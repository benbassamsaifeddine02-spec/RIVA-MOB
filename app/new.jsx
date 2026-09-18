import {Alert,Image,Pressable,ScrollView,StyleSheet,Text,TextInput,View} from 'react-native';
import {useState} from 'react';
import {useRouter} from 'expo-router';
import * as ImagePicker from 'expo-image-picker';
import {C} from '../src/theme';
import {Button,Card} from '../src/ui';
import {useDocuments} from '../src/store';

const zones=[['01','EASY DOWN'],['02','TABLE TOURNANTE'],['03','COMPACTEUSE'],['04','TROLLEY'],['05','CHAINE CARRIAGE']];
const blank={complexe:'RIVA2',zoneIndex:'03',serialNumber:'001',version:1,status:'Brouillon',date:new Date().toISOString().slice(0,10),title:'',objective:'',scope:'',responsible:'',safety:['Gants de protection','Lunettes de sécurité','Chaussures de sécurité','Consignation électrique'],hazards:'',workforce:[{qualification:'Technicien de maintenance',nombre:'1'}],equipment:[{designation:'Extracteur',nombre:'1'}],materials:[{article:'',piece:'',quantite:'1',magasin:''}],steps:[{title:'',action:'',duration:'',image:null}]};

export default function New(){
  const r=useRouter(); const{saveDocument}=useDocuments(); const[d,setD]=useState(blank); const[page,setPage]=useState(0);
  const code=`${d.complexe}-${String(d.zoneIndex).padStart(2,'0')}-${String(d.serialNumber||'001').padStart(3,'0')}`;
  const p=(k,v)=>setD(x=>({...x,[k]:v}));
  const row=(g,i,k,v)=>setD(x=>({...x,[g]:x[g].map((r,j)=>j===i?{...r,[k]:v}:r)}));
  const add=(g,x)=>setD(x0=>({...x0,[g]:[...x0[g],x]}));

  async function photo(i){
    const perm=await ImagePicker.requestMediaLibraryPermissionsAsync();
    if(!perm.granted)return Alert.alert('Permission requise','Autorisez l’accès aux photos.');
    const res=await ImagePicker.launchImageLibraryAsync({mediaTypes:['images'],quality:.75});
    if(!res.canceled)row('steps',i,'image',res.assets[0].uri);
  }

  function save(){
    if(!d.title.trim())return Alert.alert('Titre requis','Ajoutez un titre au mode opératoire.');
    const x=saveDocument({...d,id:String(Date.now()),code});
    r.replace(`/document/${x.id}`);
  }

  return <View style={s.p}>
    <View style={s.top}><View><Text style={s.k}>NOUVEAU MODE OPÉRATOIRE</Text><Text style={s.tt}>{code}</Text></View><Text style={s.counter}>{page+1}/5</Text></View>
    <View style={s.bar}><View style={[s.fill,{width:`${(page+1)*20}%`}]}/></View>
    <ScrollView contentContainerStyle={s.c}>
      {page===0&&<Identification d={d} p={p} code={code}/>}
      {page===1&&<Safety d={d} p={p}/>}
      {page===2&&<FiveM d={d} row={row} add={add}/>}
      {page===3&&<Steps d={d} row={row} add={add} photo={photo}/>}
      {page===4&&<Summary d={d} code={code}/>}
      <View style={s.nav}>
        {page>0?<Button title="← Retour" onPress={()=>setPage(x=>x-1)} style={{flex:1}}/>:<View style={{flex:1}}/>}
        {page<4?<Button primary title="Suivant →" onPress={()=>setPage(x=>x+1)} style={{flex:1}}/>:<Button primary title="Enregistrer" onPress={save} style={{flex:1}}/>}
      </View>
    </ScrollView>
  </View>
}

function Field({label,value,onChange,multi=false,ph=''}){return <View style={{marginBottom:11}}><Text style={s.lab}>{label}</Text><TextInput value={String(value??'')} onChangeText={onChange} placeholder={ph} multiline={multi} style={[s.input,multi&&{minHeight:85,textAlignVertical:'top'}]}/></View>}
function Chip({x,on,press}){return <Pressable onPress={press} style={[s.chip,on&&s.chipOn]}><Text style={[s.chipText,on&&{color:C.red}]}>{x}</Text></Pressable>}
function Identification({d,p,code}){return <Card><Text style={s.sec}>Identification</Text><Text style={s.lab}>Complexe</Text><View style={s.wrap}>{['RIVA1','RIVA2','RIVA3'].map(x=><Chip key={x} x={x} on={d.complexe===x} press={()=>p('complexe',x)}/>)}</View><Text style={s.lab}>Zone / Machine</Text><View style={s.wrap}>{zones.map(([i,n])=><Chip key={i} x={`${i} · ${n}`} on={d.zoneIndex===i} press={()=>p('zoneIndex',i)}/>)}</View><Field label="Serial Number" value={d.serialNumber} onChange={v=>p('serialNumber',v)} ph="001"/><Field label="Titre" value={d.title} onChange={v=>p('title',v)} ph="Ex. Remplacement d’un roulement"/><Field label="Equipement / sous-equipement" value={d.scope} onChange={v=>p('scope',v)}/><Field label="Responsable" value={d.responsible} onChange={v=>p('responsible',v)}/><Text style={s.codePrev}>Code document : {code}</Text></Card>}
function Safety({d,p}){const a=['Gants de protection','Lunettes de sécurité','Chaussures de sécurité','Consignation électrique','Consignation hydraulique/pneumatique','Balisage de la zone'];const tog=x=>p('safety',d.safety.includes(x)?d.safety.filter(a=>a!==x):[...d.safety,x]);return <Card><Text style={s.sec}>Sécurité</Text><View style={s.wrap}>{a.map(x=><Chip key={x} x={`✓ ${x}`} on={d.safety.includes(x)} press={()=>tog(x)}/>)}</View><Field label="Risques / précautions spécifiques" value={d.hazards} onChange={v=>p('hazards',v)} multi/></Card>}
function Table({title,rows,group,keys,headers,row,add,newRow}){return <Card><Text style={s.sec}>{title}</Text><View style={s.thr}>{headers.map(h=><Text key={h} style={s.th}>{h}</Text>)}</View>{rows.map((r,i)=><View key={i} style={s.tr}>{keys.map(k=><TextInput key={k} value={String(r[k]??'')} onChangeText={v=>row(group,i,k,v)} style={s.cell}/>)}</View>)}<Button title="+ Ajouter" onPress={()=>add(group,newRow)}/></Card>}
function FiveM({d,row,add}){return <><Table title="Main-d’œuvre" rows={d.workforce} group="workforce" keys={['qualification','nombre']} headers={['Qualification','Nombre']} row={row} add={add} newRow={{qualification:'',nombre:'1'}}/><Table title="Matériel" rows={d.equipment} group="equipment" keys={['designation','nombre']} headers={['Désignation','Nombre']} row={row} add={add} newRow={{designation:'',nombre:'1'}}/><Table title="Matériaux" rows={d.materials} group="materials" keys={['article','piece','quantite','magasin']} headers={['Code d’article','Pièce','Quantité','Code magasin']} row={row} add={add} newRow={{article:'',piece:'',quantite:'1',magasin:''}}/></>}
function Steps({d,row,add,photo}){return <><Text style={s.sec}>Étapes de réalisation</Text>{d.steps.map((x,i)=><Card key={i}><View style={s.stepRow}><Text style={s.stepNo}>{i+1}</Text><Text style={s.sec}>Étape</Text></View><Field label="Titre" value={x.title} onChange={v=>row('steps',i,'title',v)}/><Field label="Action / instruction" value={x.action} onChange={v=>row('steps',i,'action',v)} multi/><Field label="Temps" value={x.duration} onChange={v=>row('steps',i,'duration',v)} ph="Ex. 10 min"/><Pressable style={s.photoBox} onPress={()=>photo(i)}>{x.image?<Image source={{uri:x.image}} style={s.photo}/>:<Text style={s.photoText}>＋ Ajouter une photo</Text>}</Pressable></Card>)}<Button title="+ Ajouter une étape" onPress={()=>add('steps',{title:'',action:'',duration:'',image:null})}/></>}
function Summary({d,code}){return <Card><Text style={s.sec}>Résumé</Text><Text style={s.big}>{code}</Text><Text style={s.dt}>{d.title||'Sans titre'}</Text><Text style={s.meta}>Version {d.version}.0 · {d.status} · {d.date}</Text><Text style={s.sum}>Sécurité : {d.safety.length}</Text><Text style={s.sum}>Main-d’œuvre : {d.workforce.length}</Text><Text style={s.sum}>Matériel : {d.equipment.length}</Text><Text style={s.sum}>Matériaux : {d.materials.length}</Text><Text style={s.sum}>Étapes : {d.steps.length}</Text></Card>}
const s=StyleSheet.create({p:{flex:1,backgroundColor:'#F5F6F7'},top:{padding:18,paddingTop:25,flexDirection:'row',justifyContent:'space-between',alignItems:'center'},k:{fontSize:10,fontWeight:'900',color:C.red,letterSpacing:1},tt:{fontSize:20,fontWeight:'900',color:C.ink,marginTop:3},counter:{fontWeight:'900',color:C.gray},bar:{height:4,backgroundColor:'#E5E6E8'},fill:{height:4,backgroundColor:C.red},c:{padding:18,paddingBottom:30},sec:{fontSize:16,fontWeight:'900',color:C.ink,marginBottom:9},lab:{fontSize:11,fontWeight:'800',color:C.gray,marginBottom:6},input:{borderWidth:1,borderColor:C.line,borderRadius:10,backgroundColor:C.white,padding:11,fontSize:13,color:C.ink},wrap:{flexDirection:'row',flexWrap:'wrap',gap:8,marginBottom:12},chip:{borderWidth:1,borderColor:C.line,borderRadius:99,paddingHorizontal:10,paddingVertical:8,backgroundColor:C.white},chipOn:{backgroundColor:'#FFF0EE',borderColor:'#F1AAA4'},chipText:{fontSize:11,fontWeight:'700',color:C.ink},codePrev:{fontSize:11,fontWeight:'900',color:C.red},nav:{flexDirection:'row',gap:9,marginTop:8},thr:{flexDirection:'row',borderWidth:1,borderColor:C.line,backgroundColor:'#F2F3F4',padding:7},th:{flex:1,fontSize:10,fontWeight:'900'},tr:{flexDirection:'row',gap:5,marginTop:5},cell:{flex:1,borderWidth:1,borderColor:C.line,borderRadius:7,padding:8,fontSize:11,backgroundColor:C.white},stepRow:{flexDirection:'row',alignItems:'center',gap:8,marginBottom:7},stepNo:{width:28,height:28,borderRadius:14,backgroundColor:'#FDE8E6',color:C.red,fontWeight:'900',textAlign:'center',paddingTop:6},photoBox:{minHeight:130,borderWidth:2,borderStyle:'dashed',borderColor:'#CFD4D8',borderRadius:12,alignItems:'center',justifyContent:'center',overflow:'hidden'},photo:{width:'100%',height:200},photoText:{fontSize:13,fontWeight:'800',color:C.gray},big:{fontSize:12,fontWeight:'900',color:C.red},dt:{fontSize:18,fontWeight:'900',marginTop:4,color:C.ink},meta:{fontSize:11,color:C.gray,marginTop:4},sum:{fontSize:12,paddingTop:6,color:C.ink}});
