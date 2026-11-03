import asyncio, os, re, secrets
from pathlib import Path
from typing import Optional
from fastapi import FastAPI, Header, HTTPException
from fastapi.responses import FileResponse
from pydantic import BaseModel, Field
from telethon import TelegramClient
from telethon.errors import SessionPasswordNeededError

APP_ID=int(os.environ.get('TELEGRAM_API_ID','0')); APP_HASH=os.environ.get('TELEGRAM_API_HASH','')
BOT=os.environ.get('MUSIC_BOT_USERNAME','spotifybot').lstrip('@'); API_KEY=os.environ.get('WAVE_BRIDGE_API_KEY','')
DATA=Path(os.environ.get('SESSION_DIR','/data')); DATA.mkdir(parents=True,exist_ok=True)
OUT=Path(os.environ.get('DOWNLOAD_DIR','/data/downloads')); OUT.mkdir(parents=True,exist_ok=True)
app=FastAPI(title='WAVE MUSIC Telegram Bridge',version='0.1.0')
client=None; qr_task=None; qr_url=None; auth_state='disconnected'; files={}; lock=asyncio.Lock()

def guard(key):
    if not API_KEY: raise HTTPException(503,'Bridge API key is not configured')
    if not key or not secrets.compare_digest(key,API_KEY): raise HTTPException(401,'Invalid bridge API key')

async def tg():
    global client
    if APP_ID<=0 or not APP_HASH: raise HTTPException(503,'Set TELEGRAM_API_ID and TELEGRAM_API_HASH')
    if client is None: client=TelegramClient(str(DATA/'wave_user'),APP_ID,APP_HASH)
    if not client.is_connected(): await client.connect()
    return client

async def qr_worker():
    global auth_state,qr_url
    try:
        c=await tg()
        if await c.is_user_authorized(): auth_state='authorized'; qr_url=None; return
        auth_state='waiting_for_scan'; login=await c.qr_login(); qr_url=login.url
        try: await login.wait(timeout=180); auth_state='authorized'; qr_url=None
        except asyncio.TimeoutError: auth_state='expired'; qr_url=None
        except SessionPasswordNeededError: auth_state='password_required'; qr_url=None
    except Exception: auth_state='error'; qr_url=None; raise

@app.get('/health')
async def health(): return {'service':'wave-telegram-bridge','status':'ok'}

@app.post('/auth/qr')
async def start_qr(x_wave_key:Optional[str]=Header(default=None)):
    guard(x_wave_key)
    global qr_task,auth_state
    c=await tg()
    if await c.is_user_authorized(): auth_state='authorized'; return {'state':auth_state}
    if qr_task is None or qr_task.done():
        qr_task=asyncio.create_task(qr_worker())
        for _ in range(40):
            if qr_url or auth_state in ('error','password_required'): break
            await asyncio.sleep(.1)
    return {'state':auth_state,'qr_url':qr_url}

@app.get('/auth/status')
async def status(x_wave_key:Optional[str]=Header(default=None)):
    guard(x_wave_key); return {'state':auth_state,'qr_url':qr_url}

class Song(BaseModel): title:str=Field(min_length=1,max_length=200)

@app.post('/music/request')
async def request_song(body:Song,x_wave_key:Optional[str]=Header(default=None)):
    guard(x_wave_key); c=await tg()
    if not await c.is_user_authorized(): raise HTTPException(401,'Connect Telegram with QR login first')
    async with lock:
        bot=await c.get_entity(BOT); sent=await c.send_message(bot,body.title.strip()); found=None
        end=asyncio.get_running_loop().time()+90
        while asyncio.get_running_loop().time()<end:
            for m in await c.get_messages(bot,limit=15):
                if m.id>sent.id and (m.audio or m.voice or (m.document and (m.file.mime_type or '').startswith('audio/'))): found=m; break
                if m.id>sent.id and m.message and any(s in m.message.lower() for s in ('not found','no results')): raise HTTPException(404,m.message[:250])
            if found: break
            await asyncio.sleep(1.5)
        if not found: raise HTTPException(504,'No audio response received from the configured Telegram bot')
        name=found.file.name or f'wave-{found.id}.audio'; safe=re.sub(r'[^A-Za-z0-9._-]','_',name)[:120]
        saved=await c.download_media(found,file=str(OUT/f'{found.id}-{safe}'))
        if not saved: raise HTTPException(502,'Could not download Telegram media')
        token=secrets.token_urlsafe(24); files[token]=Path(saved)
        return {'status':'ready','title':name,'size':Path(saved).stat().st_size,'download_path':f'/music/file/{token}','source_bot':'@'+BOT}

@app.get('/music/file/{token}')
async def media(token:str,x_wave_key:Optional[str]=Header(default=None)):
    guard(x_wave_key); p=files.get(token)
    if not p or not p.is_file(): raise HTTPException(404,'File expired or not found')
    return FileResponse(p,filename=p.name,media_type='application/octet-stream')

@app.post('/auth/logout')
async def logout(x_wave_key:Optional[str]=Header(default=None)):
    guard(x_wave_key); global auth_state,qr_url
    if client: await client.log_out()
    auth_state='disconnected'; qr_url=None; return {'state':auth_state}