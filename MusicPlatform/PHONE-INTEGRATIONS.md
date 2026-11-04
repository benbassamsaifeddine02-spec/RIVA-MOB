# WAVE MUSIC phone integrations

## Telegram import flow
1. In WAVE MUSIC, enter a song or artist in the search field.
2. Tap **Open Telegram**. Android opens the configured Telegram bot link with the search text prefilled where supported.
3. Request the audio in Telegram. The bot must actually provide an audio file; WAVE MUSIC cannot convert previews into full tracks.
4. In Telegram, use Share on the returned audio and select WAVE MUSIC.
5. WAVE MUSIC copies the shared audio into its private `filesDir/imports` directory and adds it to the Telegram Imports list for local playback.

Telegram account authentication remains inside the official Telegram app. This implementation does not read Telegram messages, automate a user session, or store Telegram credentials.

## MEGA backup flow
1. Install and sign in to the official MEGA Android app.
2. In WAVE MUSIC, tap **Link MEGA folder**.
3. In the Android system folder picker, select the target cloud folder if the installed MEGA app exposes it as a Storage Access Framework document provider.
4. WAVE MUSIC stores the granted folder URI permission locally.
5. New Telegram imports enqueue a WorkManager backup. **Back up now** queues the same operation manually.
6. The worker uses `NetworkType.CONNECTED`, so it may run over Wi-Fi or mobile data. Android decides the exact execution time based on background-work scheduling.

The MEGA app must expose a writable DocumentsProvider for this route to work. If MEGA does not appear in the picker, direct upload is not available through this SAF integration on that device. Do not treat a generic local folder as cloud backup. The old Node.js/megajs service is a separate server adapter and is not used by this phone-hosted path.

## Scope and safeguards
- Automatic backup currently covers audio imported through Telegram into WAVE MUSIC's private imports folder.
- Internet Archive downloads managed by Android DownloadManager are not yet included in this backup queue.
- Files with an existing matching name in the destination are not uploaded again.
- The app does not request MEGA passwords or Telegram login codes.
- Only upload and retain audio that you have permission to store.
