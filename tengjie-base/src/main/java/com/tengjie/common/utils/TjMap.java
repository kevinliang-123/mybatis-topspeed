package com.tengjie.common.utils;
/**
 *  tjMap的get方法有问题，如果遇到为空则放回=，但是不能动，因为在mapper处理中要使用，因此通用的用这个TjNewmap，tjMap不再维护
 */
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
/**
 * 为创建动态属性和mapper中处理运算符时专用map
 * @author liangfeng
 *
 * @param <K>
 * @param <V>
 */
public class TjMap<K, V> extends HashMap<K, V> implements Serializable {
	private static final long serialVersionUID = 1L;
	/**
	 * 通用添加方法
	 * @param key
	 * @param value
	 * @return 返回当前map对象
	 */
	 public TjMap<K, V> putCon(K key, V value) {
		super.put(key, value);
    	return this;
    }
	 /**
		 * 添加el表达式形式的value，会自动对value进行添加${}
		 * @param key
		 * @param value
		 * @return 返回当前map对象
		 */
		 public TjMap<K, V> putConEl(String key, String value) {
			super.put((K)key, (V)("${"+value+"}"));
		    return this;
	    }

	 /**
		 * 通用添加方法
		 * @param key
		 * @param value
		 * @return 返回当前map对象
		 */
		 public TjMap<K,V> putCon(K ...keys) {
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
	 public TjMap<K, V> put(K key) {
			super.put(key,  (V) java.lang.String.class);
	    	return this;
	    }
	 /**
	  * 本方法为动态添加属性时使用，key为属性名，value自动为String.class
	  * @param key 属性名
	  * @return
	  */
	 public TjMap<K, V> removeCon(K ...key) {
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
		  if(value==null||StringUtils.isEmpty(value+"")){
			  value=(V) "=";
		  }
		 
		return value;
		  
	  }
	  /**
		  * 为mapper中处理运算符时使用，若get不到，默认返回=
		  */
		  public V get(Object key,V defaultValue) {
			  V value=null;
			  value=super.get(key);
			  if(value==null||StringUtils.isEmpty(value+"")){
				  value=(V) "=";
			  }
			 
			return value;
			  
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
	  public static <K, V>TjMap newInstance(){
		  return new TjMap<K, V>();
	  }
}
