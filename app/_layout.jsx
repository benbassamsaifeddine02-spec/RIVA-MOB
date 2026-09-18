import {Stack} from 'expo-router';
import {StatusBar} from 'expo-status-bar';
import {Provider} from '../src/store';

export default function Layout(){
  return <Provider><StatusBar style="dark"/><Stack screenOptions={{headerShown:false}}/></Provider>;
}
