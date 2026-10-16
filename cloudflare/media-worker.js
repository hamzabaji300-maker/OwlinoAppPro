// =====================================================================
// Owlino Media Worker  —  خادم ملفات الدردشة على Cloudflare (R2)
// =====================================================================
// ماذا يفعل (بكلام بسيط):
//   1) الرفع  (PUT /upload):  يسأل Supabase: هل المستخدم عضو؟ ما نوع المحادثة؟ هل تجاوز حصته؟
//                             ثم يحفظ الملف في R2 ويرجع رابطًا موقّعًا.
//   2) التحميل (GET /m/...):  لا يكفي الرابط! يلزم أيضًا تسجيل دخول مستخدم له حق القراءة
//                             (عضو في المحادثة، أو قناة عامة). فالرابط المسرّب لا يفتح لغير الأعضاء،
//                             ومن غادر المحادثة يفقد الوصول خلال دقيقة.
//   3) الحذف:                 تلقائي من R2 بقواعد Lifecycle (انظر README-ar.md)، والرابط ينتهي في الموعد نفسه.
//
// الإعدادات المطلوبة (من لوحة Cloudflare، لا تكتبها هنا):
//   Binding (R2 bucket) باسم:  MEDIA
//   Variable:  SUPABASE_URL        مثال: https://xxxx.supabase.co
//   Variable:  SUPABASE_ANON_KEY   المفتاح العام (publishable/anon) لمشروع Supabase
//   Secret:    URL_SECRET          نص عشوائي طويل (40 حرفًا أو أكثر) تخترعه أنت
// =====================================================================

const ENC = new TextEncoder();

// مدة بقاء الملفات بالأيام حسب نوع المحادثة (يقررها الخادم)
const DAYS = { direct: 30, group: 180 };

// أقصى حجم لكل نوع (بالبايت). التطبيق يفحصها أيضًا، وهذه الحماية من جهة الخادم.
const LIMITS = {
  image: 12 * 1024 * 1024,
  thumb: 1 * 1024 * 1024,
  voice: 12 * 1024 * 1024,
  video: 55 * 1024 * 1024,
  file: 27 * 1024 * 1024,
};

const ID_RE = /^[A-Za-z0-9_-]{6,80}$/;

// أنواع المستندات المسموح حفظها بنوعها الحقيقي (تُقدَّم دائمًا كتنزيل attachment، لا تُعرض داخل الصفحة).
// لا نقبل أبدًا text/html ولا image/svg+xml ولا أي سكربت.
const SAFE_DOC_TYPES = /^(application\/(pdf|zip|x-zip-compressed|msword|rtf|x-7z-compressed|x-rar-compressed|vnd\.rar|vnd\.openxmlformats-officedocument\.[a-z0-9.+-]{1,80}|vnd\.ms-[a-z0-9.+-]{1,40}|vnd\.oasis\.opendocument\.[a-z0-9.+-]{1,40})|text\/(plain|csv))$/;
const MEDIA_TYPE = /^(image|video|audio)\/[a-z0-9.+-]{1,60}$/;

function safeName(raw) {
  // نزيل الأحرف التحكمية والفواصل والاقتباس ونقصّ الطول
  return (raw || '').replace(/[\u0000-\u001f\u007f\/\\"]/g, '').trim().slice(0, 150);
}

function dispositionFor(name) {
  if (!name) return 'attachment';
  const ascii = name.replace(/[^\x20-\x7e]/g, '_').replace(/[%;"]/g, '_');
  const encoded = encodeURIComponent(name).replace(/['()*]/g, (c) => '%' + c.charCodeAt(0).toString(16).toUpperCase());
  return `attachment; filename="${ascii}"; filename*=UTF-8''${encoded}`;
}

// ذاكرة مؤقتة قصيرة لقرارات القراءة (تقلّل الطلبات إلى Supabase)
const ACCESS_CACHE = new Map();
const ALLOW_TTL_MS = 60_000;
const DENY_TTL_MS = 15_000;

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    try {
      if (request.method === 'PUT' && url.pathname === '/upload') {
        return await handleUpload(request, env, url);
      }
      if ((request.method === 'GET' || request.method === 'HEAD') && url.pathname.startsWith('/m/')) {
        return await handleDownload(request, env, url);
      }
      if (url.pathname === '/' || url.pathname === '/health') {
        return json({ ok: true, service: 'owlino-media' });
      }
      return json({ error: 'not_found' }, 404);
    } catch (e) {
      return json({ error: 'server_error' }, 500);
    }
  },
};

// ---------------------------------------------------------------- أدوات Supabase
function bearer(request) {
  const auth = request.headers.get('Authorization') || '';
  return auth.startsWith('Bearer ') ? auth.slice(7).trim() : '';
}

// قرار واحد من Supabase بجلسة المستخدم: مسجّل؟ عضو؟ ما نوع المحادثة؟ هل تجاوز حصته؟
async function reserveUpload(env, token, chat, kind, size) {
  const res = await fetch(`${env.SUPABASE_URL}/rest/v1/rpc/reserve_media_upload`, {
    method: 'POST',
    headers: {
      apikey: env.SUPABASE_ANON_KEY,
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ p_chat: chat, p_kind: kind, p_size: size }),
  });
  if (res.status === 401) return { error: json({ error: 'unauthorized' }, 401) };
  if (!res.ok) return { error: json({ error: 'upstream_error' }, 502) };
  const verdict = await res.json();
  if (!verdict || verdict.allowed !== true) {
    const reason = verdict && verdict.reason;
    if (reason === 'unauthorized') return { error: json({ error: 'unauthorized' }, 401) };
    if (reason === 'quota') return { error: json({ error: 'quota_exceeded' }, 429) };
    if (reason === 'bad_request') return { error: json({ error: 'bad_request' }, 400) };
    return { error: json({ error: 'forbidden' }, 403) };
  }
  // نوع المحادثة يحدده الخادم فقط (فردية = 30 يومًا، مجموعة/قناة = 180 يومًا)
  return { scope: verdict.scope === 'group' ? 'group' : 'direct' };
}

// هل لهذا المستخدم حق قراءة ملفات هذه المحادثة؟ (عضو، أو قناة عامة)
async function canRead(env, token, chat) {
  const tokenId = await sha256Hex(token); // نعرّف الجلسة دون تخزين الرمز نفسه
  const cacheKey = `${tokenId}|${chat}`;
  const now = Date.now();
  const hit = ACCESS_CACHE.get(cacheKey);
  if (hit && hit.until > now) return hit.value;

  const res = await fetch(`${env.SUPABASE_URL}/rest/v1/rpc/can_read_media`, {
    method: 'POST',
    headers: {
      apikey: env.SUPABASE_ANON_KEY,
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ p_chat: chat }),
  });
  if (res.status === 401) return 'unauthorized';
  if (!res.ok) return 'error';
  const allowed = (await res.json()) === true;

  if (ACCESS_CACHE.size > 5000) {
    let n = 0;
    for (const k of ACCESS_CACHE.keys()) {
      ACCESS_CACHE.delete(k);
      if (++n >= 1000) break;
    }
  }
  ACCESS_CACHE.set(cacheKey, { value: allowed, until: now + (allowed ? ALLOW_TTL_MS : DENY_TTL_MS) });
  return allowed;
}

function accessError(access) {
  if (access === 'unauthorized') return new Response('Unauthorized', { status: 401 });
  if (access === 'error') return new Response('Upstream error', { status: 502 });
  return new Response('Forbidden', { status: 403 });
}

// ---------------------------------------------------------------- الرفع
async function handleUpload(request, env, url) {
  const token = bearer(request);
  if (!token) return json({ error: 'unauthorized' }, 401);

  const chat = url.searchParams.get('chat') || '';
  const kind = url.searchParams.get('kind') || '';
  if (!(kind in LIMITS) || !ID_RE.test(chat)) {
    return json({ error: 'bad_request' }, 400);
  }

  const length = parseInt(request.headers.get('Content-Length') || '0', 10);
  if (!length) return json({ error: 'length_required' }, 411);
  if (length > LIMITS[kind]) return json({ error: 'too_large' }, 413);

  const decision = await reserveUpload(env, token, chat, kind, length);
  if (decision.error) return decision.error;
  const scope = decision.scope;

  // المفتاح عشوائي لا يمكن تخمينه
  const key = `${scope}/${chat}/${crypto.randomUUID()}`;
  // نقبل صور/فيديو/صوت بنوعها، والمستندات الشائعة بنوعها الحقيقي (قائمة سماح)، وأي نوع آخر يُحفظ كملف عادي.
  // هذا يمنع رفع صفحة HTML أو SVG أو سكربت وتقديمه من عنوان الـ Worker.
  const rawType = (request.headers.get('Content-Type') || '').trim().toLowerCase();
  const isMedia = MEDIA_TYPE.test(rawType) && rawType !== 'image/svg+xml';
  const isDoc = SAFE_DOC_TYPES.test(rawType);
  const contentType = (isMedia || isDoc) ? rawType : 'application/octet-stream';
  const httpMetadata = { contentType };
  // كل ما ليس صورة/فيديو/صوت يُقدَّم كتنزيل باسمه الحقيقي (إن أُرسل)
  if (!isMedia) httpMetadata.contentDisposition = dispositionFor(safeName(url.searchParams.get('name')));
  await env.MEDIA.put(key, request.body, { httpMetadata });

  // لا نُنشئ رابطًا لملف ناقص أو فارغ أبدًا
  const head = await env.MEDIA.head(key);
  if (!head || head.size !== length) {
    try { await env.MEDIA.delete(key); } catch (e) {}
    return json({ error: 'incomplete_upload' }, 500);
  }

  return json(await signedResult(env, url.origin, key, scope));
}

async function signedResult(env, origin, key, scope) {
  // رابط موقّع ينتهي في موعد حذف الملف
  const exp = Math.floor(Date.now() / 1000) + DAYS[scope] * 86400;
  const sig = await sign(env.URL_SECRET, `${key}:${exp}`);
  return { url: `${origin}/m/${key}?e=${exp}&s=${sig}`, key, expires_at: exp };
}

// ---------------------------------------------------------------- التحميل
async function handleDownload(request, env, url) {
  let key;
  try {
    key = decodeURIComponent(url.pathname.slice(3));
  } catch (e) {
    return new Response('Bad Request', { status: 400 });
  }
  const exp = parseInt(url.searchParams.get('e') || '0', 10);
  const sig = url.searchParams.get('s') || '';
  if (!exp || !sig) return new Response('Forbidden', { status: 403 });

  // الطبقة 1: التوقيع والانتهاء (رخيصة، بلا اتصال بأحد)
  const ok = await verify(env.URL_SECRET, `${key}:${exp}`, sig);
  if (!ok) return new Response('Forbidden', { status: 403 });
  if (Date.now() / 1000 > exp) return new Response('Expired', { status: 410 });

  // الطبقة 2: لا يكفي الرابط. يلزم مستخدم مسجّل له حق القراءة.
  const token = bearer(request);
  if (!token) return new Response('Unauthorized', { status: 401 });
  const parts = key.split('/');
  if (parts.length !== 3 || !ID_RE.test(parts[1])) return new Response('Forbidden', { status: 403 });
  const access = await canRead(env, token, parts[1]);
  if (access !== true) return accessError(access);

  const object = await env.MEDIA.get(key, { range: request.headers, onlyIf: request.headers });
  if (object === null) return new Response('Not Found', { status: 404 });

  const headers = new Headers();
  object.writeHttpMetadata(headers);
  headers.set('etag', object.httpEtag);
  headers.set('accept-ranges', 'bytes');
  headers.set('cache-control', 'private, max-age=604800');
  // حماية المتصفح: لا تخمّن نوع الملف، ولا تنفّذ أي محتوى نشط
  headers.set('x-content-type-options', 'nosniff');
  headers.set('content-security-policy', "default-src 'none'; sandbox");
  // أي شيء ليس صورة/فيديو/صوت يُقدَّم كتنزيل (باسمه الحقيقي إن حُفظ عند الرفع)
  if (!headers.get('content-disposition') && !MEDIA_TYPE.test(headers.get('content-type') || '')) {
    headers.set('content-disposition', 'attachment');
  }
  if (object.range) {
    const start = object.range.offset ?? 0;
    const end = object.range.end ?? (start + (object.range.length ?? object.size) - 1);
    headers.set('content-range', `bytes ${start}-${end}/${object.size}`);
  }
  // الطول الكامل معلن للردود الكاملة، فيكشف التطبيق أي تنزيل مقطوع
  if (!object.range && object.body) headers.set('content-length', String(object.size));
  const status = object.body ? (request.headers.get('range') !== null ? 206 : 200) : 304;
  return new Response(request.method === 'HEAD' ? null : object.body, { headers, status });
}

// ---------------------------------------------------------------- التوقيع
async function hmacKey(secret) {
  return crypto.subtle.importKey('raw', ENC.encode(secret), { name: 'HMAC', hash: 'SHA-256' }, false, ['sign']);
}

async function sign(secret, message) {
  const key = await hmacKey(secret);
  const sig = await crypto.subtle.sign('HMAC', key, ENC.encode(message));
  return [...new Uint8Array(sig)].map((b) => b.toString(16).padStart(2, '0')).join('');
}

async function verify(secret, message, hex) {
  const expected = await sign(secret, message);
  if (expected.length !== hex.length) return false;
  let diff = 0;
  for (let i = 0; i < expected.length; i++) diff |= expected.charCodeAt(i) ^ hex.charCodeAt(i);
  return diff === 0;
}

async function sha256Hex(text) {
  const d = await crypto.subtle.digest('SHA-256', ENC.encode(text));
  return [...new Uint8Array(d)].slice(0, 16).map((b) => b.toString(16).padStart(2, '0')).join('');
}

function json(obj, status = 200) {
  return new Response(JSON.stringify(obj), {
    status,
    headers: { 'content-type': 'application/json; charset=utf-8', 'cache-control': 'no-store' },
  });
}
