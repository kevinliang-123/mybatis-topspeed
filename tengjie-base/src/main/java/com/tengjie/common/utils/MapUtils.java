package com.tengjie.common.utils;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class MapUtils {
	/**
	 *反转map
	 * @param <K>
	 * 
	 * @param map
	 * @param value
	 * @return
	 */
	public static <K,V> Map<K,V> reverseMap(Map<K, V> map) {
		Map newMap = new HashMap();
		// Map,HashMap并没有实现Iteratable接口.不能用于增强for循环.
		for (K getKey : map.keySet()) {
			newMap.put(map.get(getKey), getKey);
			
		}
		return newMap;
		// 这个key肯定是最后一个满足该条件的key.
	}
	 /**
     * 获得map的最后一个元素,返回的是entry，可以用entry.getKey\entry.getValue获得其中的值
     * @param <K>
     * @param <V>
     * @param map
     * @return
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public static <K, V> Entry<K, V> getTailByReflection(LinkedHashMap<K, V> map)
            throws NoSuchFieldException, IllegalAccessException {
        Field tail = map.getClass().getDeclaredField("tail");
        tail.setAccessible(true);
        
        return (Entry<K, V>) tail.get(map);
    }
	/**
	 * map转成list
	 * @param map
	 * @param bl:true 要的是key，false 要的是value
	 * @return
	 */
	public static Map<String,String> stringArrayToMap(String[] strArray) {  
		Map<String,String> maps=Maps.newHashMap();
		if(strArray==null)return maps;
		for(String temp:strArray) {
			maps.put(temp, temp);
		}
		return maps;
	}
	/**
	 * 获得map中的index的value或者key的值
	 * 注意，这里的map一般都是有序map
	 * @param <T>
	 * @param map：map
	 * @param index：第几条记录
	 * @param keyOrValue：要的是key还是value的值.true为key
	 * @return
	 */
	public static <T>T findValueByIndex(Map map,int index,boolean keyOrValue){
		T  result=null;
		List<T> list= mapToList(map,keyOrValue);
		if(list.size()>index) {
			result=list.get(index);
		}
		return result;
	}
	/**
	 * map转成list
	 * @param map
	 * @param bl:true 要的是key，false 要的是value
	 * @return
	 */
	public static List mapToList(Map map,boolean bl) {  
		List list=Lists.newArrayList();
	     if(map==null)return list;
	    Iterator entries = map.entrySet().iterator(); 
	    while (entries.hasNext()) { 
	      Map.Entry entry = (Map.Entry) entries.next(); 
	      Object key = entry.getKey(); 
	      Object value = entry.getValue(); 
	      if(bl){
	    	  list.add(key);
	      } else{
	    	  list.add(value);
	      }
	    }
	  
	    return list;
	} 
	/**
	 * 将array类型转成Map类型,map的key和value都一样
	 * @param <V>
	 * @param array
	 * @return
	 */
	public static <T, V>Map<T,V> arrayToMap(T[] array) {
		
		Map<T,V> result=Maps.newHashMap();
		if(array!=null)
			for(T temp:array){
				result.put(temp,null);
			}
		return result;
	}
 /**
  * 当map的value为bean对象时，提取其中的某个属性作为新的map的key
  * @param map
  * @param keyFieldName :要提取作为key的属性名,本值如果为空，则用原有map的key作为key
  * @param valueFieldName：要提取作为value的属性名，本值如果为空，则表示value设置为空
  * @return
  */
	public static Map pickUpValueProp(Map map,String keyFieldName,String valueFieldName) {  
	    Map newMap=Maps.newHashMap();
 	     if(map==null)return newMap;
	    Iterator entries = map.entrySet().iterator(); 
	    while (entries.hasNext()) { 
	      Map.Entry entry = (Map.Entry) entries.next(); 
	      Object key=entry.getKey();
	      Object value = entry.getValue(); 
	      Object newkey=key;
	      Object newvalue=null;
	      if(StringUtils.isNotEmpty(keyFieldName)){
	    	  newkey=Reflections.getFieldValue(value, keyFieldName);
	      }
	      if(StringUtils.isNotEmpty(valueFieldName)){
	    	  newvalue=Reflections.getFieldValue(value, valueFieldName);
	      }
	      newMap.put(newkey, newvalue);
	    }
	  
	    return newMap;
	} 
	/**
	 * map转成list,要求map的value是个bean对象,
	 * 将该value对象的某个属性取出作为list中内容并返回，注意，若实体中该属性的值为空，则不添加到list中
	 * @param map
	 * @param fieldName map中bean对象的某个属性名称，若为空，则使用整个bean对象
	 * @return
	 */
	public static List mapValuePropToList(Map map,String fieldName) {  
		List list=Lists.newArrayList();
	     if(map==null)return list;
	    Iterator entries = map.entrySet().iterator(); 
	    while (entries.hasNext()) { 
	      Map.Entry entry = (Map.Entry) entries.next(); 
	      Object value = entry.getValue(); 
	      Object propValue=Reflections.getFieldValue(value, fieldName);
	      if(propValue!=null)list.add(propValue);
	    }
	  
	    return list;
	} 
	/**
	 * 将map.toString后的字符串再还原回map
	 * @param str
	 * @return
	 */
	 public static Map<String,String> mapStringToMap(String str){  
		    str=str.substring(1, str.length()-1);  
		    String[] strs=str.split(",");  
		    Map<String,String> map = new HashMap<String, String>();  
		    for (String string : strs) {  
		        String key=string.split("=")[0];  
		        String value=string.split("=")[1];  
		        map.put(key.trim(), value);  
		    }  
		    return map;  
		} 
	 /**
	  * 根据value获得key
	  * @param dest
	  * @param value
	  * @return
	  */
	public static String findKeyByValue(Map dest,Object value){
		String key=null;
		Iterator entries = dest.entrySet().iterator(); 
		while (entries.hasNext()) { 
		  Map.Entry entry = (Map.Entry) entries.next(); 
		  if(entry.getValue()!=null&&entry.getValue().equals(value)){
			  key=entry.getKey()==null?null:entry.getKey().toString();
		  }
		}
		return key;
	} 
	/**
	 * 根据目标类型clazz获得对应的值
	 * @param <T>
	 * @param dest
	 * @param key
	 * @param clazz
	 * @return
	 */
	public static <T>T getValue(Map dest,String key,Class<T> clazz){
		Object obj=dest.get(key);
		if(obj!=null) {
			try {
				return MyBeanUtils.getDestTypeValue(obj, clazz);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
}
