import AsyncStorage from '@react-native-async-storage/async-storage';
import React,{createContext,useContext,useEffect,useMemo,useState} from 'react';

const KEY='riva-mob-docs-v1';

const demo={
  id:'demo-1',
  code:'RIVA2-03-014',
  version:1,
  status:'Brouillon',
  date:'2026-09-18',
  title:'Remplacement du roulement d’un moteur',
  complexe:'RIVA2',
  zoneIndex:'03',
  serialNumber:'014',
  scope:'Roulement côté entraînement',
  responsible:'Technicien de maintenance',
  objective:'Remplacer le roulement défectueux.',
  safety:['Gants de protection','Lunettes de sécurité','Consignation électrique'],
  hazards:'Ne pas intervenir sous tension.',
  workforce:[{qualification:'Technicien de maintenance',nombre:'1'}],
  equipment:[{designation:'Extracteur de roulement',nombre:'1'}],
  materials:[{article:'6205-2RS',piece:'Roulement',quantite:'1',magasin:'MAG-001'}],
  steps:[{title:'Arrêt et consignation',action:'Arrêter la machine et effectuer la consignation.',duration:'5 min',image:null}]
};

const Ctx=createContext(null);

export function Provider({children}){
  const [documents,setDocuments]=useState([demo]);

  useEffect(()=>{
    AsyncStorage.getItem(KEY).then(x=>x&&setDocuments(JSON.parse(x))).catch(()=>{});
  },[]);

  useEffect(()=>{
    AsyncStorage.setItem(KEY,JSON.stringify(documents)).catch(()=>{});
  },[documents]);

  const saveDocument=d=>{
    const x={...d,id:d.id||String(Date.now())};
    setDocuments(p=>p.some(a=>a.id===x.id)?p.map(a=>a.id===x.id?x:a):[x,...p]);
    return x;
  };

  const getDocument=id=>documents.find(x=>x.id===id);
  const value=useMemo(()=>({documents,saveDocument,getDocument}),[documents]);

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export const useDocuments=()=>{
  const x=useContext(Ctx);
  if(!x) throw Error('Provider missing');
  return x;
};
