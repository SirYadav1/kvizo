# Changelog

All notable changes to Kvizo Android will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.1.0] - 2026-09-15

### Added
- **Backend upgrade: Cloudflare Worker API** for signed quiz delivery (Ed25519 verification)
- **About Me section** — portfolio link and developer info
- **Website link** in About screen (https://kvizo.indevs.in/)
- Community quiz import via signed `community.json`

### Changed
- **Backend upgrade method**: Community repo → GitHub webhook → Worker KV → app fetch
  1. Push quizzes to `kvizo-community` GitHub repo
  2. GitHub webhook triggers Cloudflare Worker
  3. Worker updates KV namespace with quiz data
  4. App fetches signed JSON from Worker API
  5. Ed25519 signature verified locally in-app
- R8 optimization for improved startup time and smoother transitions
- APK size reduced to **3.3MB** (from 20MB debug)

### Performance
- Fixed DNS resolution: raw.githubusercontent.com → Cloudflare Worker API (kvizo-api.ray-crane.workers.dev)
- Faster quiz loading via R8 code shrinking
- Reduced APK size from 20MB (debug) → 3.3MB (release) with resource shrinking
- Smoother button animations and page transitions
- Optimized resource loading with `isShrinkResources = true`

## [2.0.0] - 2026-09-09

### Added
- 80+ badges across 15 categories with unlock celebrations
- Community quizzes with one-tap import
- Quiz share codes — share any quiz offline via clipboard
- Quiz exit confirmation & auto-pause only on true backgrounding
- Brand-new app icon
- Smoother buttons, clipped ripples & press animations
- Full 5-language support with instant switching

## [1.5.0] - 2026-08-08

### Added
- Speedometer score sweep — settles exactly on your score
- One-shot confetti celebration on finish
- Settings redesign: segmented theme, cleaner controls
- Telegram-style dark mode colors
- Updater, changelog & about screens
- Backup & Restore with JSON import
- Quiz pauses automatically when you leave the app

## [1.4.0] - 2026-08-06

### Added
- Violet redesign — new UI everywhere
- Space Grotesk display font
- Dark mode improvements & SVG icons
- Press-glow buttons

## [1.0.0] - 2026-08-03

### Added
- The first Kvizo release
- Offline quizzes & local profiles

## Backend Upgrade Method

The app now uses a **Cloudflare Worker** as the API layer between the community quiz repository and the app.

### Flow
1. **Publish**: Push quiz data to `kvizo-community` GitHub repo
2. **Trigger**: GitHub webhook fires on push
3. **Update**: Cloudflare Worker updates KV namespace
4. **Fetch**: App requests signed JSON from `https://kvizo-api.<worker>.workers.dev`
5. **Verify**: App checks Ed25519 signature before displaying quizzes
6. **Notify**: Users get system notifications for new announcements

### Security
- Ed25519 public key embedded in app — verifies all community quiz data
- No credentials exposed in the Worker or app
- Worker serves as CDN + API layer only
- GitHub webhook secrets verify push authenticity
