Build a production-ready Android AI chatbot application inspired by the UI/UX of OpenAI and Google.
The app must look and feel modern, smooth, minimal, responsive, and premium — almost identical to ChatGPT mobile UI experience.

Tech Stack:

* Frontend: React Native Expo + TypeScript
* Backend: Supabase
* Database: Supabase PostgreSQL
* Authentication: Supabase Auth
* Storage: Supabase Storage Buckets
* Realtime: Supabase Realtime
* AI Models API: NVIDIA NIM API (free models only)
* State Management: Zustand
* UI: NativeWind/TailwindCSS
* Markdown Rendering: react-native-markdown-display
* Syntax Highlighting for code blocks
* Streaming Responses support
* Dark/Light Mode
* Android-first optimized app

Project Goal:
Create a fully functional AI chatbot app where users can:

* Sign up/login
* Chat with multiple AI models
* Save conversation history
* Upload files/images
* Stream AI responses in real-time
* Regenerate/edit prompts
* Search chats
* Delete/archive chats
* Use markdown/code rendering
* Switch between AI models
* Manage profile/settings
* Sync chats in realtime across devices

The design must be almost identical to ChatGPT Android app:

* Sidebar drawer
* Chat list history
* New Chat button
* Typing animation
* Streaming token effect
* Smooth animations
* Floating input bar
* Rounded modern UI
* Chat bubbles
* Markdown rendering
* Syntax-highlighted code blocks
* Copy buttons for code/messages
* Message reactions
* Long press actions
* Auto scroll during stream
* Token loading animation
* Premium polished experience

Required Features:

AUTH SYSTEM:

* Email/password auth
* Google login
* Guest mode
* Session persistence
* JWT handling
* Secure authentication flow

CHAT FEATURES:

* Create unlimited chats
* Rename chats
* Delete chats
* Archive chats
* Pin chats
* Search conversations
* Streaming AI responses
* Stop generating button
* Retry response button
* Continue generation button
* Message editing
* Conversation context memory
* Token usage counter
* Typing indicator
* Markdown rendering
* LaTeX math rendering
* Code syntax highlighting
* Copy/share/export chats
* Voice input
* Text-to-speech responses

AI FEATURES:
Use NVIDIA free models only through API abstraction layer.
Implement model selector with:

* Llama models
* Mistral models
* DeepSeek models
* Gemma models
* Qwen models

Backend must support:

* Dynamic model switching
* Temperature control
* Max tokens control
* Context window handling
* Streaming responses
* Retry/fallback models
* Rate limiting
* Error handling
* Token estimation
* Conversation memory

SUPABASE DATABASE STRUCTURE:
Create complete SQL schema for:

* users
* profiles
* chats
* messages
* attachments
* AI models
* subscriptions
* settings
* usage logs
* API logs
* reports

Include:

* Proper foreign keys
* RLS policies
* Secure access rules
* Optimized indexes
* Realtime subscriptions
* Row ownership security

SUPABASE STORAGE:
Create buckets for:

* profile-images
* chat-attachments
* generated-files
* voice-notes

Implement:

* Secure uploads
* Signed URLs
* File validation
* File compression
* Upload progress
* Multi-file upload

ANDROID FEATURES:

* APK build support
* Push notifications
* Deep linking
* Offline cache
* Keyboard optimization
* Android permissions
* Share intent support
* Image picker
* Camera upload
* Voice recording
* Splash screen
* Adaptive icon
* Background sync
* Performance optimized

ADMIN PANEL:
Build separate web admin dashboard with:

* User management
* Chat analytics
* Token usage analytics
* Model usage analytics
* Ban users
* Delete chats
* Monitor API usage
* Error logs
* Broadcast notifications
* App settings
* Model management
* Rate limit settings

API LAYER:
Create secure backend API wrapper between app and NVIDIA APIs.
Requirements:

* Never expose API keys in frontend
* Edge Functions on Supabase
* Streaming proxy
* Rate limiting
* Abuse protection
* Request logging
* Model fallback system
* Error retry logic
* Queue system
* Usage tracking

UI/UX REQUIREMENTS:
The app must feel exactly like premium AI apps:

* Smooth 60fps animations
* Glassmorphism effects
* Blur effects
* Skeleton loaders
* Typing animations
* Gesture support
* Haptic feedback
* Smooth navigation transitions
* Optimized keyboard handling
* Infinite scroll chat history
* Responsive layouts

SETTINGS PAGE:
Include:

* Theme switcher
* Language selector
* AI model selector
* Clear chats
* Export data
* Delete account
* Notification settings
* Voice settings
* Privacy settings

SECURITY:

* Full RLS protection
* API encryption
* Secure auth handling
* Abuse prevention
* Input sanitization
* SQL injection protection
* XSS protection
* Device session management

DELIVERABLES:
Generate:

* Complete frontend source code
* Complete Supabase backend setup
* SQL schema
* Supabase Edge Functions
* Android build configuration
* Environment setup
* API integration layer
* Deployment guide
* README documentation
* Production-ready architecture

Folder Structure:
Create scalable enterprise-grade architecture with:

* components/
* screens/
* hooks/
* services/
* api/
* store/
* lib/
* utils/
* types/
* database/
* edge-functions/
* assets/

IMPORTANT:

* Use clean scalable architecture
* Use TypeScript everywhere
* Use reusable components
* Production-ready coding standards
* Proper error handling
* Proper loading states
* Modern animations
* Optimized performance
* Modular codebase
* Realtime streaming support
* ChatGPT-like experience

The final app should feel like a real commercial AI product similar to ChatGPT mobile app with a polished production-grade Android experience.
