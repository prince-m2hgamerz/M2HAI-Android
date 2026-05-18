import { createClient } from '@supabase/supabase-js';

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL;
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY;
const supabaseServiceRole = import.meta.env.VITE_SUPABASE_SERVICE_ROLE;

// Admin panel uses the service role key to bypass RLS for management
export const supabase = createClient(supabaseUrl, supabaseServiceRole || supabaseAnonKey);

// Public website users must use the anon client so RLS protects per-user data.
export const userSupabase = createClient(supabaseUrl, supabaseAnonKey);
