# Music Platform — Android cloud build

This isolated branch builds an installable Android debug APK.

## First MVP scope
- Kotlin + Jetpack Compose dark interface
- Runtime audio permission request
- Local audio discovery through Android MediaStore
- Track list with title, artist and album
- Local playback using AndroidX Media3 ExoPlayer

## Not yet implemented
Telegram bot adapters, external music catalog, infinite recommendation feed, download manager, Room persistence, background MediaSession playback and release signing are later milestones. This is a local-player MVP, not the complete product.

Package: `com.musicplatform.app`
Minimum Android: API 26 (Android 8.0)
