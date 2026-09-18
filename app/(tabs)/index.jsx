import {Pressable,ScrollView,StyleSheet,Text,View} from 'react-native';
import {useRouter} from 'expo-router';
import {C} from '../../src/theme';
import {Badge,Button,Card} from '../../src/ui';
import {useDocuments} from '../../src/store';

export default function Home(){
  const r=useRouter();
  const {documents}=useDocuments();

  return <ScrollView style={s.page} contentContainerStyle={s.c}>
    <View style={s.head}>
      <View><Text style={s.k}>RIVA INDUSTRIES</Text><Text style={s.t}>RIVA-MOB</Text><Text style={s.sub}>Mode Opératoire Builder</Text></View>
      <View style={s.logoMark}><Text style={s.logoText}>RIVA</Text><Text style={s.logoSub}>industries</Text></View>
    </View>

    <Card style={s.hero}>
      <Text style={s.ht}>Créer un mode opératoire</Text>
      <Text style={s.hx}>Construisez une instruction claire, illustrée et prête à exporter.</Text>
      <Button primary title="+  Nouveau mode opératoire" onPress={()=>r.push('/new')} style={{marginTop:14}}/>
    </Card>

    <Text style={s.sec}>Documents récents</Text>
    {documents.slice(0,3).map(d=><Pressable key={d.id} onPress={()=>r.push(`/document/${d.id}`)}>
      <Card><View style={s.row}>
        <View style={{flex:1}}>
          <Text style={s.code}>{d.code}</Text>
          <Text style={s.dt}>{d.title}</Text>
          <Text style={s.meta}>Version {d.version}.0 · {d.date}</Text>
        </View>
        <Badge ok={d.status==='Validé'}>{d.status}</Badge>
      </View></Card>
    </Pressable>)}

    <Text style={s.sec}>Structure</Text>
    <Card><Text style={s.flow}>01  Identification  →  02  Sécurité  →  03  5M  →  04  Étapes  →  05  PDF</Text></Card>
  </ScrollView>;
}

const s=StyleSheet.create({
  page:{flex:1,backgroundColor:'#F5F6F7'},
  c:{padding:18,paddingTop:28,paddingBottom:28},
  head:{flexDirection:'row',justifyContent:'space-between',alignItems:'center',marginBottom:18},
  k:{fontSize:10,fontWeight:'900',letterSpacing:1,color:C.red},
  t:{fontSize:28,fontWeight:'900',color:C.ink},
  sub:{fontSize:12,color:C.gray},
  logoMark:{alignItems:'flex-end'},
  logoText:{fontSize:24,fontWeight:'900',color:C.red},
  logoSub:{fontSize:9,fontWeight:'800',color:C.gray},
  hero:{backgroundColor:'#FFF8F7',borderColor:'#F3D1CE'},
  ht:{fontSize:20,fontWeight:'900',color:C.ink},
  hx:{fontSize:13,lineHeight:20,color:C.gray,marginTop:5},
  sec:{fontSize:15,fontWeight:'900',color:C.ink,marginTop:7,marginBottom:8},
  row:{flexDirection:'row',gap:10},
  code:{fontSize:11,fontWeight:'900',color:C.red},
  dt:{fontSize:14,fontWeight:'800',color:C.ink,marginTop:2},
  meta:{fontSize:11,color:C.gray,marginTop:4},
  flow:{fontSize:12,fontWeight:'800',lineHeight:23,color:C.ink}
});
