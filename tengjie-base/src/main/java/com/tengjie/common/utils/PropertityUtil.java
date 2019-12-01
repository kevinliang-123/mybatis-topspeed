package com.tengjie.common.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


	/**描述：获取邮件配置文件中信息<br>
	 */
	public class PropertityUtil {
		private static HashMap<String,Properties> map = null;
		
		/**
		 * 方法名称: get<br>
		 * 描述：通过key获取资源文件中的value
		 */
		public static String get(String key,String fileName){
			String propertyPath = fileName;
			if(map == null){
				map = new HashMap<String,Properties>();
			}
			if(map.get(fileName) != null) {
				return map.get(fileName).get(key)==null?null:map.get(fileName).get(key).toString();
			}
			InputStream is = PropertityUtil.class.getClassLoader().getResourceAsStream(propertyPath);
			Properties propertie = new Properties();
			try {
				propertie.load(is);
				map.put(fileName, propertie);
			} catch (IOException e) {
				e.printStackTrace();
			}finally{
				if(is != null){
					try {
						is.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				} 
			}
			return map.get(fileName).get(key)==null?null:map.get(fileName).get(key).toString();
		}

		/**
		 * 方法名称：reload<br>
		 * 描述：重新加载配置文件<br>
		 * @param fileNames 如果为空，则reload全部配置文件
		 */
		public static void reload(String... fileNames){
			if(fileNames.length == 0){
				fileNames =  map.keySet().toArray(new String[0]);
			}
			String propertyPath;
			for (String fileName : fileNames) {
				propertyPath = fileName;
				if(map == null){
					map = new HashMap<String,Properties>();
				}
				map.remove(fileName);
				InputStream is = PropertityUtil.class.getClassLoader().getResourceAsStream(propertyPath);
				Properties propertie = new Properties();
				try {
					propertie.load(is);
					map.put(fileName, propertie);
				} catch (IOException e) {
					e.printStackTrace();
				}finally{
					if(is != null){
						try {
							is.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					} 
				}
			}
		}
		

		public static Map<String, String> getMap(String fileName){
			String propertyPath = fileName;
			if(map == null){
				map = new HashMap<String,Properties>();
			}
			if(map.get(fileName) != null) {
				return new HashMap<String, String>((Map) map.get(fileName));
			}
			InputStream is = PropertityUtil.class.getClassLoader().getResourceAsStream(propertyPath);
			Properties propertie = new Properties();
			try {
				propertie.load(is);
				map.put(fileName, propertie);
			} catch (IOException e) {
				e.printStackTrace();
			}finally{
				if(is != null){
					try {
						is.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				} 
			}
			return new HashMap<String, String>((Map) map.get(fileName));
		
		}
	} 