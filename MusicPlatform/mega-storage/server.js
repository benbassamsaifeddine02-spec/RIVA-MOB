import express from 'express';
import multer from 'multer';
import crypto from 'node:crypto';
import fs from 'node:fs/promises';
import path from 'node:path';
import { Storage } from 'megajs';

const app=express(); app.use(express.json({limit:'1mb'}));
const upload=multer({dest:'/data/tmp',limits:{fileSize:100*1024*1024}});
const PORT=Number(process.env.PORT||8081), API_KEY=process.env.WAVE_BRIDGE_API_KEY||'', ENC_KEY=process.env.MEGA_SESSION_ENCRYPTION_KEY||'', SESSION_FILE='/data/mega-session.enc';
let storage=null;
if(!API_KEY||!ENC_KEY) console.warn('Set WAVE_BRIDGE_API_KEY and MEGA_SESSION_ENCRYPTION_KEY before use');
function auth(req,res,next){const got=req.header('X-Wave-Key')||'';if(!API_KEY||!crypto.timingSafeEqual(Buffer.from(got.padEnd(API_KEY.length,' ')),Buffer.from(API_KEY.padEnd(got.length,' ')))||got.length!==API_KEY.length)return res.status(401).json({error:'Unauthorized'});next();}
function key(){return crypto.createHash('sha256').update(ENC_KEY).digest();}
function encrypt(text){const iv=crypto.randomBytes(12),c=crypto.createCipheriv('aes-256-gcm',key(),iv);const data=Buffer.concat([c.update(text,'utf8'),c.final()]);return Buffer.concat([iv,c.getAuthTag(),data]);}
function decrypt(buf){const iv=buf.subarray(0,12),tag=buf.subarray(12,28),data=buf.subarray(28),d=crypto.createDecipheriv('aes-256-gcm',key(),iv);d.setAuthTag(tag);return Buffer.concat([d.update(data),d.final()]).toString('utf8');}
async function saveSession(){await fs.writeFile(SESSION_FILE,encrypt(JSON.stringify(storage.toJSON())),{mode:0o600});}
async function connectSaved(){try{const raw=decrypt(await fs.readFile(SESSION_FILE));storage=Storage.fromJSON(JSON.parse(raw));await storage.reload();return true;}catch{return false;}}
function ready(){if(!storage)throw new Error('MEGA is not connected. Sign in first.');return storage;}
app.get('/health',(_,res)=>res.json({service:'wave-mega-storage',status:'ok'}));
app.post('/mega/login',auth,async(req,res)=>{try{const {email,password,secondFactorCode}=req.body||{};if(typeof email!=='string'||typeof password!=='string'||!email||!password)return res.status(400).json({error:'Email and password are required'});const s=await new Storage({email,password,secondFactorCode,userAgent:'WAVEMUSIC/1.0'}).ready;storage=s;await saveSession();res.json({status:'connected',account:s.name||'MEGA account'});}catch(e){res.status(401).json({error:'MEGA login failed',detail:String(e.message||e).slice(0,180)});}});
app.get('/mega/status',auth,async(_,res)=>{if(!storage)await connectSaved();if(!storage)return res.json({status:'disconnected'});try{const q=await storage.getAccountInfo();res.json({status:'connected',spaceUsed:q.spaceUsed,spaceTotal:q.spaceTotal,downloadBandwidthUsed:q.downloadBandwidthUsed,downloadBandwidthTotal:q.downloadBandwidthTotal});}catch{storage=null;res.json({status:'disconnected'});}});
app.get('/mega/library',auth,async(_,res)=>{try{const s=ready();const root=s.root;const nodes=(root.children||[]).map(f=>({id:f.nodeId,name:f.name,size:f.size||0,directory:!!f.directory}));res.json({items:nodes});}catch(e){res.status(503).json({error:e.message});}});
app.post('/mega/upload',auth,upload.single('file'),async(req,res)=>{try{if(!req.file)return res.status(400).json({error:'file is required'});const s=ready();const folder=String(req.body.folder||'WAVE MUSIC').replace(/[\\/]/g,'').slice(0,80)||'WAVE MUSIC';let dir=s.root.children?.find(x=>x.directory&&x.name===folder);if(!dir)dir=await s.mkdir(folder);const name=path.basename(req.file.originalname||'audio.bin').replace(/[\\/]/g,'_').slice(0,180);const f=await dir.upload({name,size:req.file.size},await fs.readFile(req.file.path)).complete;res.json({status:'uploaded',name:f.name,size:f.size,nodeId:f.nodeId});}catch(e){res.status(500).json({error:'Upload failed',detail:String(e.message||e).slice(0,180)});}finally{if(req.file)await fs.rm(req.file.path,{force:true});}});
app.get('/mega/download/:id',auth,async(req,res)=>{try{const s=ready();const f=s.files[req.params.id];if(!f)return res.status(404).json({error:'File not found'});res.setHeader('Content-Disposition',`attachment; filename*=UTF-8''${encodeURIComponent(f.name)}`);res.setHeader('Content-Type','application/octet-stream');f.download().pipe(res);}catch(e){res.status(500).json({error:'Download failed',detail:String(e.message||e).slice(0,180)});}});
app.post('/mega/logout',auth,async(_,res)=>{try{if(storage)storage.close();storage=null;await fs.rm(SESSION_FILE,{force:true});res.json({status:'disconnected'});}catch(e){res.status(500).json({error:'Logout failed'});}});
app.listen(PORT,'0.0.0.0',()=>console.log(`WAVE MEGA storage listening on ${PORT}`));