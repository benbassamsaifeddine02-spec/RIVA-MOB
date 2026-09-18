import {useMemo,useState} from 'react';
import {FlatList,Pressable,StyleSheet,Text,TextInput,View} from 'react-native';
import {useRouter} from 'expo-router';
import {C} from '../../src/theme';
import {Badge,Button,Card} from '../../src/ui';
import {useDocuments} from '../../src/store';

export default function Documents(){
  const r=useRouter();
  const {documents}=useDocuments();
  const[q,setQ]=useState('');
  const data=useMemo(()=>{
    const x=q.toLowerCase().trim();
    return x?documents.filter(d=>`${d.code} ${d.title}`.toLowerCase().includes(x)):documents;
  },[documents,q]);

  return <View style={s.p}>
    <View style={s.top}><Text style={s.t}>Documents</Text><Button primary title="+ Nouveau" onPress={()=>r.push('/new')} style={{paddingVertical:9}}/></View>
    <TextInput value={q} onChangeText={setQ} placeholder="Rechercher…" style={s.search}/>
    <FlatList data={data} keyExtractor={x=>x.id} contentContainerStyle={{padding:18,paddingTop:2}} renderItem={({item})=>
      <Pressable onPress={()=>r.push(`/document/${item.id}`)}>
        <Card><View style={s.row}><View style={{flex:1}}>
          <Text style={s.code}>{item.code}</Text><Text style={s.dt}>{item.title}</Text><Text style={s.meta}>Version {item.version}.0 · {item.date}</Text>
        </View><Badge ok={item.status==='Validé'}>{item.status}</Badge></View></Card>
      </Pressable>}
    />
  </View>;
}

const s=StyleSheet.create({
  p:{flex:1,backgroundColor:'#F5F6F7'},
  top:{padding:18,paddingTop:28,flexDirection:'row',justifyContent:'space-between',alignItems:'center'},
  t:{fontSize:26,fontWeight:'900',color:C.ink},
  search:{marginHorizontal:18,marginBottom:10,borderWidth:1,borderColor:C.line,borderRadius:12,padding:11,backgroundColor:C.white},
  row:{flexDirection:'row',gap:10},
  code:{fontSize:11,fontWeight:'900',color:C.red},
  dt:{fontSize:14,fontWeight:'800',color:C.ink,marginTop:3},
  meta:{fontSize:11,color:C.gray,marginTop:4}
});
