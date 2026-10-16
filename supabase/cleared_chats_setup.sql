CREATE TABLE IF NOT EXISTS public.cleared_chats (
    user_id UUID REFERENCES auth.users NOT NULL,
    chat_id UUID REFERENCES public.chats NOT NULL,
    cleared_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    PRIMARY KEY (user_id, chat_id)
);
ALTER TABLE public.cleared_chats ENABLE ROW LEVEL SECURITY;
