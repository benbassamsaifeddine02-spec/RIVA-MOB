import {Tabs} from 'expo-router';
import {C} from '../../src/theme';

export default function TabsLayout(){
  return <Tabs screenOptions={{
    headerShown:false,
    tabBarActiveTintColor:C.red,
    tabBarInactiveTintColor:C.gray,
    tabBarLabelStyle:{fontSize:11,fontWeight:'700'},
    tabBarStyle:{height:64,paddingBottom:8,paddingTop:6,borderTopColor:'#E6E8EA'}
  }}>
    <Tabs.Screen name="index" options={{title:'Accueil'}}/>
    <Tabs.Screen name="documents" options={{title:'Documents'}}/>
    <Tabs.Screen name="settings" options={{title:'Paramètres'}}/>
  </Tabs>;
}
