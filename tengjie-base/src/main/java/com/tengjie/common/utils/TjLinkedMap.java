package com.tengjie.common.utils;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
/**
 * 为创建动态属性和mapper中处理运算符时专用map
 * @author liangfeng
 *
 * @param <K>
 * @param <V>
 */
public class TjLinkedMap<K, V> extends LinkedHashMap<K, V> implements Serializable {
	private static final long serialVersionUID = 1L;
	/**
	 * 通用添加方法
	 * @param key
	 * @param value
	 * @return 返回当前map对象
	 */
	 public TjLinkedMap<K, V> putCon(K key, V value) {
		super.put(key, value);
    	return this;
    }
	 /**
		 * 通用添加方法
		 * @param key
		 * @param value
		 * @return 返回当前map对象
		 */
		 public TjLinkedMap<K,V> putCon(K ...keys) {
			 for(K key:keys){
				 super.put(key, (V) key);
			 }
	    	return this;
	    }
	 /**
	  * 本方法为动态添加属性时使用，key为属性名，value自动为String.class
	  * @param key 属性名
	  * @return
	  */
	 public TjLinkedMap<K, V> put(K key) {
			super.put(key,  (V) java.lang.String.class);
	    	return this;
	    }
	 /**
	  * 本方法为动态添加属性时使用，key为属性名，value自动为String.class
	  * @param key 属性名
	  * @return
	  */
	 public TjLinkedMap<K, V> removeCon(K ...key) {
		   for(K t:key){
			   super.remove(t);
		   }
	    	return this;
	    }
	 /**
	  * 为mapper中处理运算符时使用，若get不到，默认返回=
	  */
	  public V get(Object key) {
		  V value=null;
		  value=super.get(key);
//		  if(value==null||StringUtils.isEmpty(value+"")){
//			  value=(V) "=";
//		  }
//		 
		return value;
		  
	  }
	  /**
			 * 根据目标类型clazz获得对应的值
			 * @param <T>
			 * @param key
			 * @param clazz
			 * @param defaultValue:为空时返回的默认值
			 * @return
			 */
			public  <T>T getValue(String key,Class<T> clazz){
				return getValue(key,clazz,null);
			}
	  /**
		 * 根据目标类型clazz获得对应的值
		 * @param <T>
		 * @param key
		 * @param clazz
		 * @param defaultValue:为空时返回的默认值
		 * @return
		 */
		public  <T>T getValue(String key,Class<T> clazz,Object defaultValue){
			T result=null;
			Object obj=super.get(key);
			if(obj!=null) {
				try {
					return MyBeanUtils.getDestTypeValue(obj, clazz);
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}else {
				if(defaultValue!=null) {
					result=(T) defaultValue;	
				}
				
			}
			return result;
		}
	  public static <K, V>TjLinkedMap newInstance(){
		  return new TjLinkedMap<K, V>();
	  }
	  /**
		  * 模糊匹配key值，只要包含就行
		  */
		  public List<V> getLike(K key) {
			  List<V> list = null; 
			  list = new ArrayList<V>(); 
	            K[] a = null; 
	            Set<K> set = this.keySet(); 
	            a = (K[])set.toArray(); 
	            for (int i = 0; i < a.length; i++) { 
	                if (a[i].toString().indexOf(key.toString()) == -1) { 
	                    continue; 
	                } else { 
	                    list.add(this.get(a[i])); 
	                } 
	            } 
		      return list; 
			  
		  }
}
