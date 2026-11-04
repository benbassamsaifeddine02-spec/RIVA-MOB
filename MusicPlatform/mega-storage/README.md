# WAVE MUSIC MEGA storage service

This is a self-hosted cloud-library/backup adapter. It uses the community `megajs` library (unofficial; not MEGA's official SDK). Review its license, maintenance status and MEGA Terms before connecting a personal account. For an officially supported integration, replace this adapter with MEGA's official SDK/MEGAcmd.

## Security model
- MEGA email/password are submitted only to this HTTPS backend and are not written to disk or logged by this service.
- The MEGA session JSON is encrypted using AES-256-GCM and `MEGA_SESSION_ENCRYPTION_KEY`, then stored on the persistent `/data` volume.
- `WAVE_BRIDGE_API_KEY` protects all endpoints. Use a different long random value from the Telegram service if desired.
- Keep the backend private behind HTTPS. Do not expose it without a strong key and network access controls. Never commit `.env`, session files, or keys.
- This first adapter is single-user/single-account. It does not implement OAuth; MEGA account login uses email/password and optional 2FA code.

## Environment
`WAVE_BRIDGE_API_KEY`, `MEGA_SESSION_ENCRYPTION_KEY` (random 32+ byte secret), `PORT` (default 8081). Persistent disk mounted at `/data`.

## Docker
Build from this directory and run with persistent `/data`. Expose port 8081 behind HTTPS. `npm start` runs the service.

## API
All endpoints except `/health` require `X-Wave-Key`.
- `POST /mega/login` JSON `{email,password,secondFactorCode?}`
- `GET /mega/status`
- `GET /mega/library`
- `POST /mega/upload` multipart field `file`, optional `folder`
- `GET /mega/download/{nodeId}`
- `POST /mega/logout`

The Android app is not yet wired to these endpoints. Uploads currently have a 100 MB request limit. The initial library listing is root-only; folder navigation, deduplication, quota-aware sync scheduling and backup rules are subsequent integration work. Use only files you have rights to store.
