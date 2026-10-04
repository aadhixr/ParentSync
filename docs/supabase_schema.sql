-- Supabase SQL Schema for ParentSync
-- Ensures device_id is set as the PRIMARY KEY so upserting with ?on_conflict=device_id works seamlessly without PGRST105 errors.

CREATE TABLE IF NOT EXISTS public.device_reports (
    device_id TEXT PRIMARY KEY,
    battery_level INTEGER,
    is_charging BOOLEAN,
    battery_health TEXT,
    active_foreground_package TEXT,
    device_locked BOOLEAN,
    is_online BOOLEAN,
    sync_timestamp BIGINT,
    app_usage_reports JSONB,
    device_status JSONB,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

-- Enable Row Level Security (RLS)
ALTER TABLE public.device_reports ENABLE ROW LEVEL SECURITY;

-- Policy to allow inserts/upserts/selects (adjust permissions based on app auth model)
CREATE POLICY "Enable all access for device_reports" ON public.device_reports
    FOR ALL
    USING (true)
    WITH CHECK (true);
