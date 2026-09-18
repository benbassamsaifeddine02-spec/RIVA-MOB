import {Alert,Image,ScrollView,StyleSheet,Text,View} from 'react-native';
import {useLocalSearchParams,useRouter} from 'expo-router';
import * as Print from 'expo-print';
import * as Sharing from 'expo-sharing';
import {C} from '../../src/theme';
import {Badge,Button,Card} from '../../src/ui';
import {useDocuments} from '../../src/store';

export default function Detail(){
  const{id}=useLocalSearchParams(); const r=useRouter(); const{getDocument}=useDocuments(); const d=getDocument(id);
  if(!d)return <View style={s.empty}><Text>Document introuvable.</Text></View>;

  async function pdf(){
    try{
      const{uri}=await Print.printToFileAsync({html:buildHtml(d)});
      if(await Sharing.isAvailableAsync()) await Sharing.shareAsync(uri,{mimeType:'application/pdf',dialogTitle:`MO-${d.code}`});
      else Alert.alert('PDF créé',uri);
    }catch(e){Alert.alert('Erreur','Impossible de générer le PDF.');}
  }

  return <ScrollView style={s.p} contentContainerStyle={s.c}>
    <View style={s.top}><Button title="← Retour" onPress={()=>r.back()} style={{paddingVertical:8}}/><Badge ok={d.status==='Validé'}>{d.status}</Badge></View>
    <Card><Text style={s.code}>{d.code}</Text><Text style={s.title}>{d.title}</Text><Text style={s.meta}>Version {d.version}.0 · {d.date}</Text><Button primary title="Exporter PDF" onPress={pdf} style={{marginTop:12}}/></Card>
    <Card><Text style={s.sec}>Identification</Text><Row l="Complexe" v={d.complexe}/><Row l="Zone / Machine" v={d.zoneIndex}/><Row l="Serial Number" v={d.serialNumber}/><Row l="Equipement / sous-equipement" v={d.scope}/><Row l="Responsable" v={d.responsible}/></Card>
    <Card><Text style={s.sec}>Sécurité</Text><Text style={s.text}>{d.safety.join(' · ')}</Text>{d.hazards?<Text style={s.meta}>{d.hazards}</Text>:null}</Card>
    <Card><Text style={s.sec}>5M</Text><Text style={s.sub}>Main-d’œuvre</Text>{d.workforce.map((x,i)=><Row key={i} l={x.qualification} v={x.nombre}/>)}<Text style={s.sub}>Matériel</Text>{d.equipment.map((x,i)=><Row key={i} l={x.designation} v={x.nombre}/>)}<Text style={s.sub}>Matériaux</Text>{d.materials.map((x,i)=><Row key={i} l={`${x.article} · ${x.piece}`} v={`${x.quantite} · ${x.magasin}`}/>)}</Card>
    <Text style={s.sec}>Étapes de réalisation</Text>
    {d.steps.map((x,i)=><Card key={i}><Text style={s.stepTitle}>{i+1}. {x.title}</Text><Text style={s.text}>{x.action}</Text><Text style={s.meta}>Temps : {x.duration||'—'}</Text>{x.image?<Image source={{uri:x.image}} style={s.photo}/>:null}</Card>)}
  </ScrollView>;
}

function Row({l,v}){return <View style={s.row}><Text style={s.lab}>{l}</Text><Text style={s.val}>{v||'—'}</Text></View>}
function esc(v=''){return String(v).replace(/[&<>"']/g,s=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#039;'}[s]))}

function buildHtml(d){
  const w=d.workforce.map(x=>`<tr><td>${esc(x.qualification)}</td><td>${esc(x.nombre)}</td></tr>`).join('');
  const e=d.equipment.map(x=>`<tr><td>${esc(x.designation)}</td><td>${esc(x.nombre)}</td></tr>`).join('');
  const m=d.materials.map(x=>`<tr><td>${esc(x.article)}</td><td>${esc(x.piece)}</td><td>${esc(x.quantite)}</td><td>${esc(x.magasin)}</td></tr>`).join('');
  const st=d.steps.map((x,i)=>`<div class="step"><b>${i+1}. ${esc(x.title)}</b><p>${esc(x.action)}</p><p><b>Temps :</b> ${esc(x.duration||'—')}</p>${x.image?`<img src="${x.image}">`:''}</div>`).join('');
  return `<!doctype html><html><head><style>body{font-family:Arial;color:#202428;margin:24px}.head{border:1px solid #ddd;padding:14px}h1{color:#EF2B20;margin:0;font-size:22px}h2{background:#EF2B20;color:#fff;padding:6px;font-size:12px}table{width:100%;border-collapse:collapse}th,td{border:1px solid #ddd;padding:6px;font-size:10px}th{background:#f3f4f5}.meta{color:#6F767C;font-size:10px}.step{border:1px solid #ddd;padding:8px;margin:8px 0}.step img{max-width:100%;max-height:260px}</style></head><body><div class="head"><div class="meta">RIVA INDUSTRIES · RIVA-MOB</div><h1>MODE OPÉRATOIRE</h1><b>${esc(d.title)}</b><div class="meta">MO-${esc(d.code)} · Version ${esc(d.version)}.0 · ${esc(d.status)} · ${esc(d.date)}</div></div><h2>IDENTIFICATION</h2><table><tr><td>Complexe</td><td>${esc(d.complexe)}</td></tr><tr><td>Zone/Machine</td><td>${esc(d.zoneIndex)}</td></tr><tr><td>Serial Number</td><td>${esc(d.serialNumber)}</td></tr><tr><td>Equipement / sous-equipement</td><td>${esc(d.scope)}</td></tr><tr><td>Responsable</td><td>${esc(d.responsible)}</td></tr></table><h2>SÉCURITÉ</h2><p>${esc(d.safety.join(' · '))}</p><p>${esc(d.hazards||'')}</p><h2>5M — MAIN-D’ŒUVRE</h2><table><tr><th>Qualification</th><th>Nombre</th></tr>${w}</table><h2>5M — MATÉRIEL</h2><table><tr><th>Désignation</th><th>Nombre</th></tr>${e}</table><h2>5M — MATÉRIAUX</h2><table><tr><th>Code d'article</th><th>Pièce</th><th>Quantité</th><th>Code magasin</th></tr>${m}</table><h2>ÉTAPES DE RÉALISATION</h2>${st}</body></html>`;
}

const s=StyleSheet.create({
  p:{flex:1,backgroundColor:'#F5F6F7'},
  c:{padding:18,paddingTop:28},
  top:{flexDirection:'row',justifyContent:'space-between',alignItems:'center',marginBottom:12},
  code:{fontSize:11,fontWeight:'900',color:C.red},
  title:{fontSize:21,fontWeight:'900',color:C.ink,marginTop:4},
  meta:{fontSize:11,color:C.gray,marginTop:5},
  sec:{fontSize:15,fontWeight:'900',marginBottom:8,color:C.ink},
  sub:{fontSize:12,fontWeight:'900',marginTop:8,marginBottom:5,color:C.ink},
  row:{flexDirection:'row',justifyContent:'space-between',paddingVertical:7,borderBottomWidth:1,borderBottomColor:'#EEF0F1'},
  lab:{fontSize:11,color:C.gray},
  val:{fontSize:11,fontWeight:'800',color:C.ink,maxWidth:'60%',textAlign:'right'},
  text:{fontSize:12,color:C.ink,lineHeight:18},
  stepTitle:{fontSize:15,fontWeight:'900',color:C.ink,marginBottom:5},
  photo:{width:'100%',height:200,marginTop:8},
  empty:{flex:1,justifyContent:'center',alignItems:'center'}
});
