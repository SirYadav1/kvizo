# Kvizo

A quiz app that actually looks good. Built with Kotlin and Jetpack Compose.

## What it does

- Create custom quizzes with multiple categories
- Take quizzes in normal or timed mode
- Track your progress, streaks, and accuracy
- Compete on the leaderboard
- Import community quizzes (Ed25519 signed, tamper-proof)
- Pick from anime avatars or meme PFPs

## Tech stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Database:** SQLite (raw, no ORM)
- **Image loading:** Coil
- **Navigation:** Compose Navigation
- **Build:** Gradle 8.7, AGP 8.5

## Building

You need Android SDK 34 and JDK 17.

```bash
git clone https://github.com/SirYadav1/kvizo.git
cd kvizo
./gradlew :app:assembleDebug
```

APK will be at `app/build/outputs/apk/release/app-release.apk`.

## Community Quizzes

Community quizzes are hosted on GitHub Pages and signed with Ed25519. The app fetches them, verifies the signature, and imports them locally.

To add quizzes to the community pool:
1. Fill out the quiz request form (link in the app)
2. I review and approve it
3. It gets signed and pushed to the community repo
4. Everyone gets it on next refresh

### Signing

```bash
python3 tools/sign_quizzes.py --json community.json --key /path/to/private_key.pem --out community.sig
```

Private key stays offline. Public key is hardcoded in the app.

## Project structure

```
app/src/main/java/com/quizforge/app/
  data/           - Database, models, settings
  logic/          - Quiz engine, scoring
  ui/             - Screens, viewmodel, components, theme
  util/           - Backup manager
  MainActivity.kt - Entry point, navigation
```

## Security

- Ed25519 digital signatures for community quizzes
- Private key never leaves the author
- Public key hardcoded in app source
- HTTPS only
- Input sanitization on quiz text
- Key rotation supported

## License

MIT

## Author

Yadav - @SirYadav1
