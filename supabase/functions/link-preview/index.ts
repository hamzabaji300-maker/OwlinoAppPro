// Supabase Edge Function: server-side link preview fetcher.
//
// Why this exists: fetching link previews directly from the phone means every
// request comes from a generic mobile/residential IP with no crawler
// reputation, which a lot of news sites' bot-protection (Cloudflare, Akamai,
// PerimeterX...) blocks outright regardless of the User-Agent string sent.
// Telegram never has this problem because IT fetches previews from ITS OWN
// servers (well-known, often allow-listed crawler infrastructure) - this
// function does the same thing for Owlino: the Android app calls this
// function instead of fetching the target site directly.
//
// Deploy:
//   supabase functions deploy link-preview --no-verify-jwt
// Call:
//   GET https://<project-ref>.supabase.co/functions/v1/link-preview?url=<encoded-url>

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

function metaTag(html: string, key: string): string | null {
  const patterns = [
    new RegExp(`<meta[^>]+property=["']${key}["'][^>]*content=["']([^"']*)["']`, "i"),
    new RegExp(`<meta[^>]+content=["']([^"']*)["'][^>]*property=["']${key}["']`, "i"),
    new RegExp(`<meta[^>]+name=["']${key}["'][^>]*content=["']([^"']*)["']`, "i"),
    new RegExp(`<meta[^>]+content=["']([^"']*)["'][^>]*name=["']${key}["']`, "i"),
  ];
  for (const p of patterns) {
    const m = html.match(p);
    if (m) return decodeHtmlEntities(m[1].trim());
  }
  return null;
}

function decodeHtmlEntities(s: string): string {
  return s
    .replace(/&amp;/g, "&")
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">");
}

function resolveImageUrl(raw: string | null, pageUrl: string): string | null {
  if (!raw) return null;
  try {
    if (raw.startsWith("http://") || raw.startsWith("https://")) return raw;
    if (raw.startsWith("//")) return "https:" + raw;
    if (raw.startsWith("/")) {
      const base = new URL(pageUrl);
      return `${base.protocol}//${base.host}${raw}`;
    }
    return raw;
  } catch {
    return raw;
  }
}

interface PreviewData {
  url: string;
  siteName: string | null;
  title: string | null;
  description: string | null;
  imageUrl: string | null;
}

async function fetchOnce(url: string, userAgent: string): Promise<PreviewData | null> {
  const res = await fetch(url, {
    headers: {
      "User-Agent": userAgent,
      "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
      "Accept-Language": "en-US,en;q=0.9,ar;q=0.8",
    },
    redirect: "follow",
  });
  const html = (await res.text()).slice(0, 700_000);
  const titleMatch = html.match(/<title[^>]*>([^<]*)<\/title>/i);
  const title = metaTag(html, "og:title") ?? (titleMatch ? decodeHtmlEntities(titleMatch[1].trim()) : null);
  const description = metaTag(html, "og:description") ?? metaTag(html, "description");
  const image = resolveImageUrl(metaTag(html, "og:image") ?? metaTag(html, "twitter:image"), url);
  const siteName =
    metaTag(html, "og:site_name") ??
    (() => {
      try {
        return new URL(url).host;
      } catch {
        return null;
      }
    })();

  if (!title && !description && !image) return null;
  return { url, siteName, title, description, imageUrl: image };
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: CORS_HEADERS });
  }

  const targetUrl = new URL(req.url).searchParams.get("url");
  if (!targetUrl) {
    return new Response(JSON.stringify({ error: "missing url param" }), {
      status: 400,
      headers: { ...CORS_HEADERS, "Content-Type": "application/json" },
    });
  }

  try {
    // Same two-pass strategy as the on-device fallback: a bot-style UA first
    // (fast, and the convention most sites specifically welcome for
    // previews), then a normal desktop-browser UA if that comes back empty.
    let data = await fetchOnce(targetUrl, "TelegramBot (like TwitterBot)").catch(() => null);
    if (!data) {
      data = await fetchOnce(
        targetUrl,
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36",
      ).catch(() => null);
    }
    return new Response(JSON.stringify(data ?? { url: targetUrl }), {
      headers: { ...CORS_HEADERS, "Content-Type": "application/json" },
    });
  } catch (e) {
    return new Response(JSON.stringify({ url: targetUrl, error: String(e) }), {
      status: 200,
      headers: { ...CORS_HEADERS, "Content-Type": "application/json" },
    });
  }
});
