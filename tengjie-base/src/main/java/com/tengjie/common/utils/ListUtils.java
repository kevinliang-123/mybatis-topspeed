package com.tengjie.common.utils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tengjie.common.persistence.TjBaseEntity;

public class ListUtils {
	public static List<String> getPosidList(String posid) {  
	    List<String> list = Lists.newArrayList();  
	    if (posid != null){  
	        for (String s : StringUtils.split(posid, ",")) {  
	            list.add(s);  
	        }  
	    }  
	    return list;  
	}  
	public static boolean sizeBigZero(List list){
		if(list!=null&&list.size()>0)return true;
		return false;
	}  
	  
	public static String setPosidList(List<String> list) {  
	    return StringUtils.join(list, ",");  
	} 
	/**
	 * list转换成map,以list中某条记录的字段keyField的值作为key值，整条记录作为value值返回
	 * 注意，这个list里面对象是一个bean或者是一个map
	 * @param list
	 * @param keyField
	 * @return 注意返回的key是字符串
	 */
	public static Map ListToMap(List list,String keyField) {  
		Map mm=new HashMap();
	    for(Object obj:list){
	    	String key="";
	    	if(obj instanceof Map){
	    		key=((Map)obj).get(keyField).toString();
	    	}else{
	    		key=Reflections.invokeGetter(obj, keyField).toString();
	    	}
	    	
	    	mm.put(key, obj);
	    }
	    return mm;
	} 
	/**
	 * list转换成map,以list中某条记录的字段keyField的值作为key值，valueField作为value值返回
	 * 注意，这个list里面对象是一个bean或者是一个map
	 * @param list
	 * @param keyField
	 * @return 注意返回的key是字符串
	 */
	public static Map ListToMap(List list,String keyField,String valueField) {  
		Map mm=new HashMap();
	    for(Object obj:list){
	    	String key="";
	    	Object value=null;
	    	if(obj instanceof Map){
	    		key=((Map)obj).get(keyField).toString();
	    		value=((Map)obj).get(valueField);
	    	}else{
	    		key=Reflections.invokeGetter(obj, keyField).toString();
	    		value=Reflections.invokeGetter(obj, valueField);
	    	}
	    	
	    	mm.put(key, value);
	    }
	    return mm;
	} 
	/**
	 * list转成list，list中为一个bean，取bean中的fieldName重组一个list
	 * @param append:fieldNames有多个参数时拼接的分隔符
	 * @param list：要提取的原始list
	 * @param fieldNames：可变参数，可以时多个字段名
	 * @return
	 */
	public static <T>List<T> ListToList(String append,List list,String ...fieldNames) {
		List result=Lists.newArrayList();
		if(fieldNames.length<1)return result;
		if(StringUtils.isEmpty(append))append="";
		for(Object bean:list) {
			
			if(fieldNames.length==1) {
				Object value=Reflections.invokeGetter(bean, fieldNames[0]);
				result.add(value);
			}else {
				Object content="-1";
				for(String fieldName:fieldNames) {
					Object value=Reflections.invokeGetter(bean, fieldNames[0]);
					if(content!=null&&"-1".equals(content.toString())) {
						content=value;
					}else {
						content=content+append+value;
					}
					result.add(content);
				}
			}
		
		}
		return result;
	}
	/**
	 * 将list内容遍历出来拼接成字符串，append是多个字符串之间的分隔符，可以为空，可以为逗号等,注意最后一个记录是不会加append
	 * @param list
	 * @param append
	 * @param ifAddSingleQuote 每个字符串是否添加单引号
	 * @return
	 */
	public static String ListToString(List list,String fieldName,String append,boolean ifAddSingleQuote) {  
		StringBuffer sb=new StringBuffer();
		if(list==null)return "";
		if(append==null)append="";
		
		for(int i=0;i<list.size();i++){
			Object obj=list.get(i);
			String content="";
			if(obj instanceof Map){
				content=((Map)obj).get(fieldName)==null?"":((Map)obj).get(fieldName).toString();
	    	}else if(obj instanceof TjBaseEntity){
	    		try {
	    			Object value=Reflections.invokeGetter(obj, fieldName);
					content=MyBeanUtils.getDestTypeValue(value, String.class);
				} catch (ParseException e) {
					e.printStackTrace();
				}
	    	}else{
	    		content=Reflections.invokeGetter(obj, fieldName)==null?"":Reflections.invokeGetter(obj, fieldName).toString();
	    	}
			if(ifAddSingleQuote)content="'" + content + "'";
			if(i==list.size()-1){
				sb.append(content);
			}else{
				sb.append(content+append);
			}
			
		}
	    return sb.toString();
	} 
	/**
	 * 将list内容遍历出来拼接成字符串，append是多个字符串之间的分隔符，可以为空，可以为逗号等,注意最后一个记录是不会加append
	 * @param list
	 * @param append
	 * @return
	 */
	public static String ListToString(List list,String append) {  
		StringBuffer sb=new StringBuffer();
		if(list==null)return "";
		if(append==null)append="";
		
		for(int i=0;i<list.size();i++){
			String obj=list.get(i).toString();
			if(i==list.size()-1){
				sb.append(obj);
			}else{
				sb.append(obj+append);
			}
			
		}
	    return sb.toString();
	} 
	/**
	 * 将list内容遍历出来拼接成字符串，append是多个字符串之间的分隔符，可以为空，可以为逗号等,注意最后一个记录是不会加append
	 * @param list
	 * @param append
	 * @param singleOrQuote 为每个字段加上单引号、双引号或者什么也不加
	 * @return
	 */
	public static String ListToString(List list,String append,String singleOrQuote) {  
		StringBuffer sb=new StringBuffer();
		if(list==null)return "";
		if(append==null)append="";
		
		for(int i=0;i<list.size();i++){
			String obj=list.get(i).toString();
			if(StringUtils.isNotEmpty(singleOrQuote)) {
				obj=singleOrQuote+obj+singleOrQuote;
			}
			if(i==list.size()-1){
				sb.append(obj);
			}else{
				sb.append(obj+append);
			}
			
		}
	    return sb.toString();
	} 
	/**
	 * 为数组添加一个元素
	 * @param array
	 * @return
	 */
	public static <T>T[]  addEleForArray(T[] origin,T ele) {  
		Collection<T> collection =new ArrayList<T>(Arrays.asList(origin));
		Collections.addAll(collection, ele);
		return collection.toArray(origin);
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
	 * 将array类型转成list类型
	 * @param array
	 * @return
	 */
	public static <T>List<T> arrayToList(T[] array) {
		
		List<T> result=Lists.newArrayList();
		if(array!=null)
			for(T temp:array){
				result.add(temp);
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
	 * list中的内容不是bean的情况下,这个方法与ListToMap区别是，这个方法的value都是null
	 * @param list
	 * @return
	 */
	public static Map ListToMapValueNull(List<String> list) {  
		Map mm=new HashMap();
		if(list!=null)
	    for(String obj:list){
	    	mm.put(obj, null);
	    }
	    return mm;
	} 
	/**
	 * list中的内容不是bean的情况下
	 * @param list
	 * @return
	 */
	public static Map ListToMap(List<String> list) {  
		Map mm=new HashMap();
		if(list!=null)
	    for(String obj:list){
	    	mm.put(obj, obj);
	    }
	    return mm;
	} 
	/**
	 * 对list按照某个sortFieldName进行排序，descOrAsc=true为升序
	 * @param source
	 * @param sortFieldName
	 * @param descOrAsc
	 */
	public static void sortList(List source,String sortFieldName,boolean descOrAsc){
		 Collections.sort(source,new ListComparator(descOrAsc,sortFieldName));
	}
	/**
	 * 注意，只能对数值类型进行比较，不包含日期，后续再加
	 * @author liangfeng
	 *
	 */
	static class ListComparator implements Comparator{
        /***
         * 是否转化为Int之后再比较
         */
        private boolean descOrAsc;
        /***
         * 对哪个列进行排序
         */
        private String comparedProperty;
        public ListComparator(boolean descOrAsc,String comparedProperty) {
            super();
            this.descOrAsc = descOrAsc;
            this.comparedProperty=comparedProperty;
        }
        public int compare(Object o1, Object o2) {
            if(null!=o1&&null!=o2)
            {
                try {
                    Object obj1=Reflections.getFieldValue(o1, comparedProperty);
                    Object obj2=Reflections.getFieldValue(o2, comparedProperty);
                    BigDecimal bd1=MyBeanUtils.getDestTypeValue(obj1,BigDecimal.class);
                    BigDecimal bd2=MyBeanUtils.getDestTypeValue(obj2,BigDecimal.class);
                    if(descOrAsc){
                       return bd1.compareTo(bd2);
                    }else{
                        return -bd1.compareTo(bd2);
                    }
                } catch (SecurityException e) {
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (ParseException e) {
					e.printStackTrace();
				} 
            }
            return 0/*等于*/;
        }
        }
}
