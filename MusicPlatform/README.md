# WAVE MUSIC — Android app and Telegram bridge

Android project: Kotlin + Jetpack Compose + Media3. The app currently includes an Internet Archive openly licensed audio catalogue with in-app playback and Android DownloadManager downloads.

## Telegram MTProto bridge
An optional self-hosted Telethon service is in `telegram-bridge/`. It supports Telegram QR authorization, requests a song from the configured third-party bot, and relays audio replies through a protected download endpoint. See `telegram-bridge/README.md` for setup.

Important: the bridge is a backend component, not yet connected to the Android UI. Deployment requires Telegram API credentials, a private HTTPS host, a persistent protected session volume, and a strong API key. It cannot guarantee full tracks; it can only relay audio actually returned by the third-party bot. Use audio only where you have rights or permission.

## Android build
Open this directory in Android Studio or use the repository GitHub Actions workflow to build the debug APK. Package: `com.musicplatform.app`; minimum Android: API 26.
