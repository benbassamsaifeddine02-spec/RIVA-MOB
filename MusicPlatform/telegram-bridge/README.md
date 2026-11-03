# WAVE MUSIC Telegram MTProto bridge

Optional self-hosted service. It keeps the Telegram user session on your server; the Android app never receives Telegram login codes, API hashes, or session files.

## Features
- QR login for your own Telegram account using Telethon/MTProto.
- Send a song-title request to the configured third-party bot (default: @spotifybot).
- Wait for an audio/document reply, download it server-side, and expose a protected file endpoint for in-app streaming or offline saving.

## Limits and security
- This is a relay: it cannot turn a preview into a full track. It only returns media the bot actually sends.
- @spotifybot is third-party software. Its availability, automation rules, rate limits, and terms are outside this project; verify its terms before automating it.
- Only download, retain, or redistribute audio when you have rights or permission. The bridge does not determine copyright status.
- Create Telegram API credentials at https://my.telegram.org. Never put credentials, QR data, or session files in the Android app or source control.
- Deploy privately over HTTPS with a strong WAVE_BRIDGE_API_KEY and persistent protected storage mounted at /data. Run a single worker because the MTProto session is stateful.
- QR login can request Telegram 2-step verification. This initial version reports password_required; a secure password-entry recovery flow remains to be implemented.

## Run
1. Copy .env.example to .env and fill in the credentials. Generate a long random API key.
2. Build the container with Dockerfile in this folder.
3. Run with the environment file, HTTPS reverse proxy, and a persistent protected volume mounted at /data.
4. POST /auth/qr with header X-Wave-Key; scan qr_url using Telegram Settings > Devices > Link Desktop Device. Poll /auth/status until authorized.
5. POST /music/request with JSON body {"title":"Artist - Song"}. Fetch the returned download_path with the same X-Wave-Key to stream or save the file.

## Endpoints
- GET /health
- POST /auth/qr
- GET /auth/status
- POST /music/request
- GET /music/file/{token}
- POST /auth/logout

Before public deployment, add per-user authentication, file expiry/cleanup, request quotas, encrypted backups, audit logs without message contents, and a secure 2FA recovery flow.
