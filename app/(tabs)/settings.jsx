import {ScrollView,StyleSheet,Text,View} from 'react-native';
import {C} from '../../src/theme';
import {Card} from '../../src/ui';

export default function Settings(){
  return <ScrollView style={s.p} contentContainerStyle={s.c}>
    <Text style={s.t}>Paramètres</Text>
    <Card>
      <Row l="Application" v="RIVA-MOB"/>
      <Row l="Version" v="0.1.0 MVP"/>
      <Row l="Mode" v="Offline-first"/>
      <Row l="Code" v="Complexe-ZoneIndex-SerialNumber"/>
    </Card>
    <Card>
      <Text style={s.sub}>Zones</Text>
      {['01 · EASY DOWN','02 · TABLE TOURNANTE','03 · COMPACTEUSE','04 · TROLLEY','05 · CHAINE CARRIAGE'].map(x=><Text key={x} style={s.z}>{x}</Text>)}
    </Card>
  </ScrollView>;
}
function Row({l,v}){return <View style={s.row}><Text style={s.l}>{l}</Text><Text style={s.v}>{v}</Text></View>}
const s=StyleSheet.create({
  p:{flex:1,backgroundColor:'#F5F6F7'},
  c:{padding:18,paddingTop:28},
  t:{fontSize:26,fontWeight:'900',color:C.ink,marginBottom:16},
  row:{flexDirection:'row',justifyContent:'space-between',paddingVertical:10,borderBottomWidth:1,borderBottomColor:'#ECEEEF'},
  l:{fontSize:12,color:C.gray},
  v:{fontSize:12,fontWeight:'800',color:C.ink,maxWidth:'65%',textAlign:'right'},
  sub:{fontWeight:'900',fontSize:14,marginBottom:8},
  z:{fontSize:12,fontWeight:'700',paddingVertical:5,color:C.ink}
});
