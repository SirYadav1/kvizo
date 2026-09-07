<p align="center">
  <img src="docs/kvizo-logo.png" width="120" alt="Kvizo logo" />
</p>

<h1 align="center">Kvizo</h1>

<p align="center">
  <b>Community quizzes, XP and deep stats — no ads, no accounts, no tracking.</b><br />
  <i>Built alone, from scratch, for the quiz community.</i>
</p>

<p align="center">
  <a href="https://github.com/SirYadav1/kvizo-1.5.0/releases/latest"><img alt="Release" src="https://img.shields.io/badge/download-latest%20APK-7C3AED" /></a>
  <a href="LICENSE"><img alt="License" src="https://img.shields.io/badge/license-MIT-blue" /></a>
</p>

---

## About

Kvizo is an Android quiz app (Kotlin + Jetpack Compose, single universal APK)
with daily quizzes, XP levels, streaks, deep statistics and community
announcements — delivered right to your notification bar with no account and
no Firebase.

- **No ads, no trackers, no accounts** — just open and play.
- **Offline-first** — quizzes are bundled with the app; community content is
  served as signed JSON from GitHub Pages.
- **Community announcements** arrive as system notifications even when the
  app is closed (WorkManager based background checks — no push provider).

## Features

- 🎬 **Animated splash** — looping WebP with sound, light & dark theme variants
- 🧠 Curated quizzes with instant feedback, explanations and flagging
- 🌐 **Community quizzes** — Ed25519-signed `community.json`, import in one tap
- ⚡ **XP, 6 levels, streaks** — first-quiz bonus, weak-area focus card
- 📊 **Stats tab** — activity heatmap (weekday + month labels), weekly accuracy,
  difficulty breakdown, all-time / week / month filters
- 👤 **24 illustrated avatars** — flat design, picker in setup and profile
- 📤 **Share your score** — one-tap score card sharing (share sheet)
- 🔥 Streak fire state at 3+ days, XP progress bar with level titles
- 🏆 Badges, confetti celebration + speed-meter score ring on results
- 🔔 Community announcements via system notifications (no Firebase)
- ⏸️ Quiz auto-pauses in background, countdown timer, answer navigator
- 🎨 Violet brand theme, dark mode, Space Grotesk typography
- 🌙 Telegram-style dark mode — buttons stay dark
- 📦 Backup & restore with JSON, in-app updater, changelog & about screens
- 🛠️ Built-in quiz builder — create and share your own quizzes
- 📱 Single universal APK (arm64-v8a, armeabi-v7a, x86, x86_64)

## Download

| Version | Size | Notes |
|---|---|---|
| **1.5.0** (latest) | 4.5 MB | Community quizzes + announcements from GitHub Pages — [release](https://github.com/SirYadav1/kvizo-1.5.0/releases/latest) |

## Tech Stack

| Layer       | Tech                                                                 |
| ----------- | -------------------------------------------------------------------- |
| App         | Kotlin, Jetpack Compose, Material 3, Navigation, DataStore, WorkManager |
| Community   | GitHub Pages + Ed25519 signed JSON (`kvizo-community` repo)           |
| Updates     | In-app updater reading GitHub Releases                                |

## Build

```bash
./gradlew assembleRelease          # requires Android SDK (local.properties or ANDROID_HOME)
./gradlew testDebugUnitTest        # unit tests
```

Output: `app/build/outputs/apk/release/app-release.apk`

## Repository Layout

```
kvizo-1.5.0/
├── app/                 Android app (com.kvizo.app)
│   └── src/main/
│       ├── assets/splash/      animated WebP splash (light + dark)
│       ├── res/raw/            sound effects (correct, wrong, win, bell, splash)
│       └── res/drawable-nodpi/ logo, launcher icon, 24 avatar WebPs
├── docs/                Assets (logo)
└── README.md
```

## Community content

Quizzes and announcements are published from the public
[`kvizo-community`](https://github.com/SirYadav1/kvizo-community) repo as
plain JSON files signed with Ed25519. The app ships the public key and
verifies every download — a bad signature means the content never reaches
the quiz list. Files already fetched stay available offline (verified cache).

## License

MIT — see [LICENSE](LICENSE).

<!-- kvizo quiz app android kotlin jetpack compose community quizzes offline stats xp streaks siryadav -->
<span style="display:none">kvizo quiz app android kotlin jetpack compose community quizzes offline stats xp streaks siryadav</span>
