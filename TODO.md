# M2HAI (BlackboxAI) Implementation Checklist

## Plan alignment (from @prd.md + @design.md)
- [ ] Fully implement Android app features per @prd.md (auth/chat/models/storage/realtime/files/voice/notifications/offline cache/markdown code rendering)
- [ ] Apply UI/UX tokens/styles per @design.md (cream/coral/dark surfaces, premium Claude-like layout)

## Engineering fixes currently in progress
- [x] Read prd.md and design.md
- [x] Wire Supabase environment variables via Gradle BuildConfig from .env
- [x] Fix Light/Dark wiring in MainActivity by using a real ThemeViewModel
- [x] Create ThemeViewModel mapping PreferenceRepository.themeMode -> darkTheme

## Build unblockers
- [ ] Fix Gradle JDK toolchain: Android Studio JBR path is broken (missing jvm.cfg)
- [ ] Add Gradle JVM override (org.gradle.java.home) or update wrapper to use a working JDK

## Next feature work (after build succeeds)
- [ ] Fix streaming parsing to match Supabase Edge Function SSE payload
- [ ] Ensure model list + switching logic matches Supabase ai_models
- [ ] Verify auth session restore and prevent logout bounce
- [ ] Implement missing UI components: sidebar, message actions (copy/regenerate/edit), attachments picker/camera, voice input/TTS
- [ ] Complete SQL schema + RLS policies + realtime subscriptions per @prd.md
- [ ] Complete Supabase Edge Functions: streaming proxy with rate limiting, fallback models, logging, abuse protection
- [ ] Add push notification integration (FCM) and deep linking


