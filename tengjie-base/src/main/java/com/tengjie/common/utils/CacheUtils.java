package com.tengjie.common.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

/**
 * Cache工具类
 * @author
 * @version 2013-5-29
 */
public class CacheUtils {
	
	private static CacheManager cacheManager = ((CacheManager)SpringContextHolder.getBean("cacheManager"));

	private static final String SYS_CACHE = "sysCache";

	/**
	 * 获取SYS_CACHE缓存
	 * @param key
	 * @return
	 */
	public static Object get(String key) {
		return get(SYS_CACHE, key);
	}
	
	/**
	 * 写入SYS_CACHE缓存
	 * @param key
	 * @return
	 */
	public static void put(String key, Object value) {
		put(SYS_CACHE, key, value);
	}
	
	/**
	 * 从SYS_CACHE缓存中移除
	 * @param key
	 * @return
	 */
	public static void remove(String key) {
		remove(SYS_CACHE, key);
	}
	/**
	 * 获取缓存
	 * @param cacheName
	 * @param key
	 * @return
	 */
	public static List<Object> getByProperty(String cacheName, String propertyName,Object propertyValue) {
		Cache ca=getCache(cacheName);
		List<Object> result=new ArrayList();
		if(StringUtils.isEmpty(propertyName)||StringUtils.isEmpty(propertyValue+""))return result;
		Map<Object, Element> element = ca.getAll(ca.getKeys());
		
		if(element!=null)
		for(Element e:element.values()){
			try {
				Field field=e.getClass().getDeclaredField(propertyName);
				if(field!=null){
					Object value=	field.get(e);
					 if(field.getType() == String.class){//后续应该加上其他类型的比较，这里先比较string
						 if(value!=null&&propertyValue!=null){
							 if(value.toString().equals(propertyValue)){
								 result.add(e);
							 }
						 }
					 }
				}
				
			} catch (NoSuchFieldException e1) {
			//	e1.printStackTrace();
		      break;
			} catch (SecurityException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IllegalArgumentException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IllegalAccessException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			e.getObjectValue();
		}
		return result;
	}
	
	/**
	 * 获取缓存
	 * @param cacheName
	 * @param key
	 * @return
	 */
	public static Object get(String cacheName, String key) {
		Element element = getCache(cacheName).get(key);
		return element==null?null:element.getObjectValue();
	}

	/**
	 * 写入缓存
	 * @param cacheName
	 * @param key
	 * @param value
	 */
	public static void put(String cacheName, String key, Object value) {
		Element element = new Element(key, value);
		getCache(cacheName).put(element);
	}

	/**
	 * 从缓存中移除
	 * @param cacheName
	 * @param key
	 */
	public static void remove(String cacheName, String key) {
		getCache(cacheName).remove(key);
	}
	
	/**
	 * 获得一个Cache，没有则创建一个。
	 * @param cacheName
	 * @return
	 */
	private static Cache getCache(String cacheName){
		Cache cache = cacheManager.getCache(cacheName);
		if (cache == null){
			cacheManager.addCache(cacheName);
			cache = cacheManager.getCache(cacheName);
			cache.getCacheConfiguration().setEternal(true);
		}
		return cache;
	}

	public static CacheManager getCacheManager() {
		return cacheManager;
	}
	
}
