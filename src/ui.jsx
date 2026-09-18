import React from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { C, R } from './theme';

export const Card = ({children, style}) => <View style={[s.card, style]}>{children}</View>;

export function Button({title, onPress, primary=false, style}) {
  return (
    <Pressable onPress={onPress} style={({pressed}) => [primary ? s.primary : s.secondary, pressed && {opacity:.72}, style]}>
      <Text style={primary ? s.primaryText : s.secondaryText}>{title}</Text>
    </Pressable>
  );
}

export const Badge = ({children, ok=false}) => (
  <View style={[s.badge, {backgroundColor: ok ? '#E8F6EF' : '#F2F3F4'}]}>
    <Text style={[s.badgeText, {color: ok ? C.green : C.gray}]}>{children}</Text>
  </View>
);

const s = StyleSheet.create({
  card:{backgroundColor:C.white,borderWidth:1,borderColor:C.line,borderRadius:R.md,padding:14,marginBottom:12},
  primary:{backgroundColor:C.red,borderRadius:R.md,paddingVertical:13,paddingHorizontal:15,alignItems:'center'},
  secondary:{backgroundColor:C.white,borderWidth:1,borderColor:C.line,borderRadius:R.md,paddingVertical:12,paddingHorizontal:15,alignItems:'center'},
  primaryText:{color:C.white,fontWeight:'800'},
  secondaryText:{color:C.ink,fontWeight:'800'},
  badge:{paddingHorizontal:8,paddingVertical:4,borderRadius:99},
  badgeText:{fontSize:10,fontWeight:'800'}
});
