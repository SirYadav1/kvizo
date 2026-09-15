# Kvizo Release Notes

## v2.1.0 (September 15, 2026)

### ✨ New Features
- **Cloudflare Worker API** — Backend now served via signed Worker API with Ed25519 verification
- **About Me section** — Click to visit [https://kvizo.indevs.in/](https://kvizo.indevs.in/)
- **Website link** in About screen

### ⚡ Performance Improvements
- **3.3MB release APK** — R8 minification + resource shrinking enabled
- Faster quiz loading via optimized resource handling
- Smoother button animations and page transitions
- Reduced startup time

### 🔧 Backend Upgrade Method
The backend now uses Cloudflare Workers:
1. Push quizzes to `kvizo-community` GitHub repo
2. GitHub webhook triggers the Worker
3. Worker updates KV namespace
4. App fetches and verifies signed JSON
5. Ed25519 signature checked locally before displaying

---

## v2.0.0 (September 9, 2026)
- 80+ badges across 15 categories
- Community quizzes with one-tap import
- Quiz share codes
- Brand-new app icon
- 5-language support

---

## Download
**Release APK**: ~3.3MB (R8-optimized, signed)

---

## About Kvizo
Kvizo is an open-source Android quiz app built by **SirYadav1**.
No ads, no accounts, no tracking. Just open and play.

🔗 [https://kvizo.indevs.in/](https://kvizo.indevs.in/)
