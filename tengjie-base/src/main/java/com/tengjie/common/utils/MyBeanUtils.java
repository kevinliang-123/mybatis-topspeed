package com.tengjie.common.utils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.DynaProperty;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.beanutils.PropertyUtilsBean;

import org.apache.poi.ss.formula.functions.T;

import org.springframework.web.multipart.MultipartFile;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tengjie.common.persistence.TjBaseEntity;
import com.tengjie.common.persistence.Page;

/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description:
 * </p>
 * 
 * @author
 * @version 2.0
 */

public class MyBeanUtils extends PropertyUtilsBean {
	private static void convert(Object dest, Object orig, String[] exclude,
			String[] include) throws IllegalAccessException,
			InvocationTargetException {

		// Validate existence of the specified beans
		if (dest == null) {
			throw new IllegalArgumentException("No destination bean specified");
		}
		if (orig == null) {
			throw new IllegalArgumentException("No origin bean specified");
		}

		// Copy the properties, converting as necessary
		if (orig instanceof DynaBean) {
			DynaProperty origDescriptors[] = ((DynaBean) orig).getDynaClass()
					.getDynaProperties();
			for (int i = 0; i < origDescriptors.length; i++) {
				String name = origDescriptors[i].getName();
				if (!includeFields(name, include)) {
					continue;
				}
				if (excludeFields(name, exclude)) {
					continue;
				}
				if (PropertyUtils.isWriteable(dest, name)) {
					Object value = ((DynaBean) orig).get(name);
					try {
						getInstance().setSimpleProperty(dest, name, value);
					} catch (Exception e) {
						; // Should not happen
					}

				}
			}
		} else if (orig instanceof Map) {
			Iterator names = ((Map) orig).keySet().iterator();
			while (names.hasNext()) {
				String name = (String) names.next();
				if (!includeFields(name, include)) {
					continue;
				}
				if (excludeFields(name, exclude)) {
					continue;
				}
				if (PropertyUtils.isWriteable(dest, name)) {
					Object value = ((Map) orig).get(name);
					try {
						getInstance().setSimpleProperty(dest, name, value);
					} catch (Exception e) {
						; // Should not happen
					}

				}
			}
		} else
		/* if (orig is a standard JavaBean) */
		{
			PropertyDescriptor origDescriptors[] = PropertyUtils
					.getPropertyDescriptors(orig);
			for (int i = 0; i < origDescriptors.length; i++) {
				String name = origDescriptors[i].getName();
				if (!includeFields(name, include)) {
					continue;
				}
				if (excludeFields(name, exclude)) {
					continue;
				}
				if ("class".equals(name)) {
					continue; // No point in trying to set an object's class
				}
				if (PropertyUtils.isReadable(orig, name)
						&& PropertyUtils.isWriteable(dest, name)) {
					try {
						Object value = PropertyUtils.getSimpleProperty(orig,
								name);
						getInstance().setSimpleProperty(dest, name, value);
					} catch (IllegalArgumentException ie) {
						; // Should not happen
					} catch (Exception e) {
						; // Should not happen
					}

				}
			}
		}

	}

	private static void convert(Object dest, Object orig) throws IllegalAccessException,
			InvocationTargetException {
		convert(dest, orig, null,null);
	}

	/**
	 * 判断name是否在忽略列表中
	 * 
	 * 
	 * @param name
	 * @param omit
	 * @return
	 */
	private static boolean excludeFields(String name, String[] excludeF) {
		if (excludeF == null)
			return false;
		for (int i = 0; i < excludeF.length; i++) {
			if (name.equals(excludeF[i])) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 判断name是否在包含列表中
	 * 
	 * 
	 * @param name
	 * @param omit
	 * @return
	 */
	private static boolean includeFields(String name, String[] includeF) {
		if (includeF == null)
			return true;
		for (int i = 0; i < includeF.length; i++) {
			if (name.equals(includeF[i])) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 对象拷贝 数据对象空值不拷贝到目标对象
	 * 需要特别注意的是，如果有动态属性的copy，必须要再接收回来如：	detail=MyBeanUtils.copyBeanNotNull2Bean(assessResultExplain, detail);
	 * @param dataObject:origin
	 * @param toObject:dest
	 * @throws NoSuchMethodException
	 *             copy
	 */
	public static <T>T copyBeanNotNull2Bean(Object databean, Object tobean)
			throws Exception {
		PropertyDescriptor origDescriptors[] = PropertyUtils
				.getPropertyDescriptors(databean);
		for (int i = 0; i < origDescriptors.length; i++) {
			String name = origDescriptors[i].getName();
			// String type = origDescriptors[i].getPropertyType().toString();
			if ("class".equals(name)) {
				continue; // No point in trying to set an object's class
			}
			if (PropertyUtils.isReadable(databean, name)
					&& PropertyUtils.isWriteable(tobean, name)) {
				try {
					Object value = PropertyUtils.getSimpleProperty(databean,
							name);
//					System.out.println("name:"+name+"----value"+value);
					if (value != null) {
						if(value instanceof String) {
							if(StringUtils.isNotEmpty(value.toString())) {
								getInstance().setSimpleProperty(tobean, name, value);
							}
						}else {
							getInstance().setSimpleProperty(tobean, name, value);
						}
						
					}else{
						if(databean instanceof TjBaseEntity){
							if(((TjBaseEntity)databean).getOriginValueCopyMap().containsKey(name)){
								getInstance().setSimpleProperty(tobean, name, value);
							}
						}
					}
				} catch (IllegalArgumentException ie) {
					; // Should not happen
				} catch (Exception e) {
					; // Should not happen
				}

			}
		}
		TjBaseEntity dest=(TjBaseEntity)tobean;
		if(databean instanceof TjBaseEntity){
			TjBaseEntity be=(TjBaseEntity)databean;
			Map<String,Object> map=Reflections.findAllDynaFieldForCglib(be, true);
			Map<String,Class> propertyMap=Maps.newHashMap();
			for (Map.Entry<String,Object> entry : map.entrySet()) { 
				  Object value=entry.getValue();
				  String key=entry.getKey().replace("$cglib_prop_", "");
				  if(value!=null&&value instanceof MultipartFile){
					 
					  propertyMap.put(key, MultipartFile.class);
				  }
				  else if(value!=null&&value instanceof List&&((List)value).size()>0&&((List)value).get(0) instanceof MultipartFile){
					  
					  propertyMap.put(key, List.class);
				  }
				   else if(entry.getKey().equals("$cglib_prop_delUrlsForMutil")){
					
					  propertyMap.put(key, String.class);
				  }
				   else if(entry.getKey().equals("$cglib_prop_needToDeleteMultipeFieldName")){//有时在编辑页面时多文件上传，不上传文件只是删除某个文件，也要进行字段更新
					  
					  propertyMap.put(key, String.class);
				  }else {
					  if(value!=null)
					  propertyMap.put(key, value.getClass());
				  }
				  
			}
			dest=(TjBaseEntity) dest.initDynaMap(propertyMap);
			for (Map.Entry<String,Object> entry : map.entrySet()) { 
				 String key=entry.getKey().replace("$cglib_prop_", "");
				 if(entry.getValue()!=null) {//propertyMap.containsKey(key)&&
					 Object value=getDestObjectFieldValue(dest,key,entry.getValue());
					 PropertyUtils.setSimpleProperty(dest, key, value);
				 }
			}
			tobean=dest;
		}
		return (T)dest;
	}

	/**
	 * 把orig和dest相同属性的value复制到dest中
	 * 
	 * @param dest
	 * @param orig
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public static void copyBean2Bean(Object dest, Object orig) throws Exception {
		convert(dest, orig);
	}
	/**
	 * 把orig和dest相同属性的value复制到dest中
	 * exclude\include 分别为不包含字段列表和包含字段列表，通常二选一
	 * @param dest
	 * @param orig
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public static void copyBean2Bean(Object dest, Object orig,String[] exclude,
			String[] include) throws Exception {
		convert(dest, orig,exclude,include);
	}
	
	/**
	 * 把orig和dest相同属性的value复制到dest中，map2bean ,复制的属性类型不能是bean类型，后续可补充bean类型
	 * fieldMapper,两个bean的对应关系，key为源表字段，value为目标表字段，根据对应关系进行复制
	 * @param dest
	 * @param orig
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public static void copyMap2Bean(Object dest, Map orig,Map<String,String> fieldMapper) throws Exception {
	   if(dest==null||orig==null)return;
		Iterator names =orig.keySet().iterator();
		while (names.hasNext()) {
			String name = (String) names.next();
			Object value =orig.get(name);
			 if(value==null)continue;
			String tempName=null;
			if(fieldMapper!=null)
			 tempName=fieldMapper.get(name);
			if(StringUtils.isNotEmpty(tempName)){
				name=tempName;
			}
			 Field destField=Reflections.getAccessibleField(dest, name);
			 if(destField!=null){
				 Object finalValue=getDestObjectFieldValue(destField,value+"");
				 Reflections.invokeSetter(dest, name, finalValue);
				
			 }
			
		}
	}
	
	/**
	 * 把orig和dest相同属性的value复制到dest中，注意这里只支持两个bean，暂时没有考虑map2bean或bean2map,复制类型也不能是bean类型，后续可补充bean类型
	 * fieldMapper,两个bean的对应关系，key为源表字段，value为目标表字段，根据对应关系进行复制
	 * @param dest
	 * @param orig
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public static void copyBean2Bean(Object dest, Object orig,Map<String,String> fieldMapper) throws Exception {
		if(fieldMapper==null)return;
		for (String key : fieldMapper.keySet()) {
			 Object origValue=Reflections.invokeGetter(orig, key);
			 if(origValue==null)continue;
			 //String strOrigValue=origValue+"";
			 Field destField=Reflections.getAccessibleField(dest, fieldMapper.get(key));
			 if(destField==null){
				 System.out.println("找不到目标表对应的字段"+fieldMapper.get(key));
			 }
			 Object finalValue=getDestObjectFieldValue(orig,key,origValue);
			 Reflections.invokeSetter(dest, fieldMapper.get(key), finalValue);
		}
	}
	/**
	 * 防止copyBean2Bean(Object dest, Object orig,Map<String,String> fieldMapper) 中由于字段类型不一致导致的报错，如源字段是integer，目标是string等
	 * @param field
	 * @param valueStr
	 * @return
	 * @throws Exception
	 */
	public static Object getDestObjectFieldValue(Field destfield,String valueStr) throws Exception{  
	    Class typeClass = destfield.getType(); 
	    Object obj =null;
	    Constructor con = typeClass.getConstructor(valueStr.getClass());  
	 	obj = con.newInstance(valueStr);  
	    return obj;  
	}  


	//调用的时候定义什么类型就返回什么类型，但是是简单类型
	public static <V>V getDestTypeValue(Object obj,Class<V> type) throws ParseException {
	//	ParameterizedType type = (ParameterizedType)new MyBeanUtils().getClass().getGenericSuperclass();
//	 Class typeClass = type.;
//	 
		Object value =null;
	    Constructor con=null;
	    if(obj==null)return null; 
	    if(javaBasicTypeMap.containsKey(type)) {//说明是基本类型
	    	type=javaBasicTypeMap.get(type);
	    }
	    if(type.isAssignableFrom(obj.getClass()))return (V) obj;
	    if(type.getName().equals("java.util.Date")){
	    	if(obj instanceof Timestamp){
	    		 SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	    		if(obj.toString().indexOf(".0")>-1){
	    			 
	    			  value=DateUtils.parseDate( fmt.format(obj));
	    		}else{
	    			value=DateUtils.parseDate( fmt.format(obj));
	    		}
	    		
	    	}else if(obj instanceof Date){
	    		value=DateUtils.parseDate(DateUtils.formatDate((Date)obj, "yyyy-MM-dd HH:mm:ss"));
	    	}else{//字符串或其他类型
	    		if(DateUtils.ifDateStrContainHourMinSec(obj+"")){
	    			value=DateUtils.parseDate(obj+"", "yyyy-MM-dd HH:mm:ss");
	    		}else{
	    			value=DateUtils.parseDate(obj+"", "yyyy-MM-dd");
	    		}
	    		
	    	}
			
			 return (V)value;
		}else  if(type.getName().equals("java.util.List")||type.getName().equals("java.util.Map")){
			return (V)obj;
		}
		try {
			Constructor<?>[] cst=type.getConstructors();
			boolean ifhascon=false;//是否有构建器
			for(Constructor<?> dd:cst){
				Class<?>[] typea=dd.getParameterTypes();
				if(typea.length!=1)continue;
				if(typea[0].getName().equals(obj.getClass().getName())){
					ifhascon=true;
				}
			}
			
			if(!ifhascon){
				con = type.getConstructor(String.class);
				value = con.newInstance(obj+"");  
			}else{
					con = type.getConstructor(obj.getClass());
					if(con!=null){
						if(obj instanceof String && StringUtils.isNotEmpty(obj+"") )
			    		value = con.newInstance(obj);  
				}
			}
		
		} catch (Exception e) {
			e.printStackTrace();
		} 
		 return (V)value;
	}
	private static Map<Class,Class> javaBasicTypeMap=initJavaBasicType();//基本数据类型Map，key为基本数据类型，value为其对应的对象类型
	private static Map<Class,Class> initJavaBasicType(){
		Map<Class,Class> tempMap=Maps.newHashMap();
		tempMap.put(boolean.class, Boolean.class);
		tempMap.put(byte.class, Byte.class);
		tempMap.put(short.class, Short.class);
		tempMap.put(char.class, Character.class);
		tempMap.put(int.class, Integer.class);
		tempMap.put(float.class, Float.class);
		tempMap.put(long.class, Long.class);
		tempMap.put(double.class, Double.class);
       return tempMap;
	}
	/**
	 * 根据目标bean对应字段的属性类型，将要转换的valueObj转换成对应的值
	 * @param field
	 * @param valueStr
	 * @return
	 * @throws Exception
	 */
	public static Object getDestObjectFieldValue(Object destBean,String porpName,Object valueObj) throws Exception{  
		
		 if(!Reflections.isContainFieldDyna(destBean, porpName))return valueObj;
	    Class typeClass = Reflections.getObjPropClass(destBean, porpName); 
	    Object obj =null; 
	    boolean hasinis=true;
	   try{
		   typeClass.getConstructor(null);//Integer等没有空参构建器typeClass.newInstance()会报错
	     }catch(Exception e){
	    	 hasinis=false;
	     }
	    if(!hasinis){
	    	obj =getDestTypeValue(valueObj,typeClass);
	    }else{
	    	  if(typeClass.newInstance() instanceof TjBaseEntity){
	  	    	TjBaseEntity temp=	(TjBaseEntity)typeClass.newInstance();
	  	    	temp.setId(valueObj.toString());
	  	    	obj=temp;
	  	    }else{
	  	    	obj =getDestTypeValue(valueObj,typeClass);
	  	    }
	  	  
	    }
    
	    return obj;  
	}  
	/**
	 * 将list转换成map
	 * @param propsCn
	 * @return
	 */
	  public static Map listToMap(List propsCn){
		  if(propsCn==null||propsCn.size()<1)return new HashMap();
		  Map mm=new HashMap();
		  for(Object propcn:propsCn){
			
				  mm.put(propcn, propcn);
			  }
	 return mm;
	  }
	  /**
		 * 将List<bean>中的include所包含属性赋值到map中
		 * @param listMap
		 * @param bean
		 * @param include
		 */
		public static void copyListBean2ListMap(List<Map> listMap, Page page,List<String> include) {
			if(include==null||include.size()<1)return;
			List listBean=page.getList();
			if(listBean==null||listBean.size()<1)return;
			if(listMap==null)listMap=new ArrayList<Map>();
			Map<String,String> includeMap=listToMap(include);
			PropertyDescriptor[] pds = PropertyUtils.getPropertyDescriptors(listBean.get(0));
			for (Object result:listBean) {
	               Map mm=new HashMap();
					copyBean2Map(mm,result,include);
					mm.put("pageSize", page.getPageSize());
					mm.put("pageNo", page.getPageNo());
					listMap.add(mm);
			
			}
		}
		
	  /**
		 * 将List<bean>中的include所包含属性赋值到map中
		 * @param listMap
		 * @param bean
		 * @param include
		 */
		public static void copyListBean2ListMap(List<Map> listMap, List listBean,List<String> include) {
			if(include==null||include.size()<1)return;
			if(listBean==null||listBean.size()<1)return;
			if(listMap==null)listMap=new ArrayList<Map>();
			Map<String,String> includeMap=listToMap(include);
			PropertyDescriptor[] pds = PropertyUtils.getPropertyDescriptors(listBean.get(0));
			for (Object result:listBean) {
	               Map mm=new HashMap();
					copyBean2Map(mm,result,include);
					listMap.add(mm);
			
			}
		}
		
		  /**
			 * 将List<Map>中的include所包含属性赋值到listBean中
			 * @param listMap
			 * @param bean
			 * @param includes：如果传入哪怕一个，都会对includes内容进行值copy，如果不传，则全部copy
			 */
			public static List<T>  convertListMap2ListBean(List<Map> listMap,Class<T> beanClass,String ...includes) {
				if(listMap==null)listMap=new ArrayList<Map>();
				List<T>  result=Lists.newArrayList();
				Map<String,String> includeMap=Maps.newHashMap();
				if(includes.length>1)includeMap=ListUtils.arrayToMap(includes);
				try {
					for(Map data:listMap) {
						T dest = beanClass.newInstance();
						for(Object key : data.keySet()){
						    Object value = data.get(key);
						    if(includeMap.size()>0) {//说明需要对include中内容进行转换
						    	if(!includeMap.containsKey(key.toString()))continue;
						    }
						    if(Reflections.isContainField(dest, key.toString())) {
						    	value=MyBeanUtils.getDestObjectFieldValue((Object)dest, key.toString(), value);
						    	Reflections.invokeSetter((Object)dest, key.toString(), value);
						    }
						   
						}
						result.add(dest);
					}
				} catch ( Exception e) {
					e.printStackTrace();
				}
				return result;
			}
	/**
	 * 将bean中的include所包含属性赋值到map中
	 * @param map
	 * @param bean
	 * @param include
	 */
	public static void copyBean2Map(Map map, Object bean,List<String> include) {
		if(include==null||include.size()<1)return;
		Map<String,String> includeMap=listToMap(include);
		PropertyDescriptor[] pds = PropertyUtils.getPropertyDescriptors(bean);
		for (int i = 0; i < pds.length; i++) {
			PropertyDescriptor pd = pds[i];
			String propname = pd.getName();
			try {
				if(!includeMap.containsKey(propname))continue;
				if(!PropertyUtils.isReadable(bean, propname))continue;
				Object propvalue = PropertyUtils.getSimpleProperty(bean,
						propname);
				map.put(propname, propvalue);
			} catch (IllegalAccessException e) {
				// e.printStackTrace();
			} catch (InvocationTargetException e) {
				// e.printStackTrace();
			} catch (NoSuchMethodException e) {
				// e.printStackTrace();
			}
		}
	}
	/**
	 * 这个调用的不是PropertyUtils.getPropertyDescriptors(bean)，因此是将一个bean当前的属性全部copy，不包含父类的
	 * @param map
	 * @param bean
	 */
	public static void copyBean2MapCurrClassField(Map map, Object bean) {
		Field[] ffArray=bean.getClass().getDeclaredFields();
		for(Field temp:ffArray){
			try {
				if(PropertyUtils.isReadable(bean, temp.getName())){
					map.put(temp.getName(), PropertyUtils.getSimpleProperty(bean,
							temp.getName()));
				}
			} catch (IllegalAccessException | InvocationTargetException
					| NoSuchMethodException e) {
				e.printStackTrace();
			}
		}
		
	}
	public static void copyBean2Map(Map map, Object bean) {
		PropertyDescriptor[] pds = PropertyUtils.getPropertyDescriptors(bean);
		for (int i = 0; i < pds.length; i++) {
			PropertyDescriptor pd = pds[i];
			String propname = pd.getName();
			try {
				if(PropertyUtils.isReadable(bean, propname)){
					Object propvalue = PropertyUtils.getSimpleProperty(bean,
							propname);
					map.put(propname, propvalue);
				}
			} catch (IllegalAccessException e) {
				// e.printStackTrace();
			} catch (InvocationTargetException e) {
				// e.printStackTrace();
			} catch (NoSuchMethodException e) {
				// e.printStackTrace();
			}
		}
	}
	/**
	 * 将Map内的key与Bean中属性相同的内容复制到BEAN中,注意将map字段驼峰后转换
	 * 
	 * @param bean
	 *            Object
	 * @param properties
	 *            Map
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public static void copyMap2BeanCamel(Object bean, Map properties)
			throws IllegalAccessException, InvocationTargetException {
		// Do nothing unless both arguments have been specified
		if ((bean == null) || (properties == null)) {
			return;
		}
		// Loop through the property name/value pairs to be set
		Iterator names = properties.keySet().iterator();
		while (names.hasNext()) {
			String name = (String) names.next();
			// Identify the property name and value(s) to be assigned
			if (name == null) {
				continue;
			}
			String camelName=StringUtils.toCamelCase(name);
			try {
				 Object value =getDestObjectFieldValue(bean,camelName, properties.get(name));
				 BeanUtils.setProperty(bean, camelName, value);
			} catch (Exception e) {
				continue;
			}
		}
	}
	/**
	 * 将Map内的key与Bean中属性相同的内容复制到BEAN中
	 * 
	 * @param bean
	 *            Object
	 * @param properties
	 *            Map
	 * @param ifInitProp
	 *        是否对再bean中的不存在的属性创建虚拟字段，true为是
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 */
	public static Object copyMap2BeanNotNull(Object bean, Map properties,boolean ifInitProp) throws IllegalAccessException, InvocationTargetException{
		if(!ifInitProp) {
			copyMap2BeanNotNull(bean,properties);
			return bean;
		}else {
			if ((bean == null) || (properties == null)) {
				return bean;
			}
			Map<String,Class> initPropMap=Maps.newHashMap();
			Iterator names = properties.keySet().iterator();
			while (names.hasNext()) {
				String name = (String) names.next();
				// Identify the property name and value(s) to be assigned
				if (name == null) {
					continue;
				}
				try {
					 Object orginValue=properties.get(name);
					 if(!Reflections.isContainFieldDyna(bean, name)) {
						 initPropMap.put(name, Object.class);
					 }
					 if(orginValue!=null) {
						 if(!initPropMap.containsKey(name)) {
							 Object value =getDestObjectFieldValue(bean,name, properties.get(name));
							 BeanUtils.setProperty(bean, name, value);
						 }
					 }
				} catch (Exception e) {
					continue;
				}
			}
			if(initPropMap.size()>0) {
				bean=((TjBaseEntity)bean).initDynaMap(initPropMap);
				for(Map.Entry<String, Class> entry : initPropMap.entrySet()){
				    String mapKey = entry.getKey();
				
				    BeanUtils.setProperty(bean, mapKey, properties.get(mapKey));
				   
				}
			}
			return bean;
		}
		
	}
	/**
	 * 将Map内的key与Bean中属性相同的内容复制到BEAN中
	 * 
	 * @param bean
	 *            Object
	 * @param properties
	 *            Map
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public static void copyMap2BeanNotNull(Object bean, Map properties)
			throws IllegalAccessException, InvocationTargetException {
		// Do nothing unless both arguments have been specified
		if ((bean == null) || (properties == null)) {
			return;
		}
		// Loop through the property name/value pairs to be set
		Iterator names = properties.keySet().iterator();
		while (names.hasNext()) {
			String name = (String) names.next();
			// Identify the property name and value(s) to be assigned
			if (name == null) {
				continue;
			}
			
			try {
				 Object orginValue=properties.get(name);
				 if(orginValue!=null) {
					 Object value =getDestObjectFieldValue(bean,name, properties.get(name));
					 BeanUtils.setProperty(bean, name, value);
				 }
			} catch (Exception e) {
				continue;
			}
		}
	}
	/**
	 * 将Map内的key与Bean中属性相同的内容复制到BEAN中
	 * 
	 * @param bean
	 *            Object
	 * @param properties
	 *            Map
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public static void copyMap2Bean(Object bean, Map properties)
			throws IllegalAccessException, InvocationTargetException {
		// Do nothing unless both arguments have been specified
		if ((bean == null) || (properties == null)) {
			return;
		}
		// Loop through the property name/value pairs to be set
		Iterator names = properties.keySet().iterator();
		while (names.hasNext()) {
			String name = (String) names.next();
			// Identify the property name and value(s) to be assigned
			if (name == null) {
				continue;
			}
			
			try {
				 Object value =getDestObjectFieldValue(bean,name, properties.get(name));
				 BeanUtils.setProperty(bean, name, value);
			} catch (Exception e) {
				continue;
			}
		}
	}
	/**
	 * 复制map对象
	 * @explain 将paramsMap中的键值对全部拷贝到resultMap中；
	 * paramsMap中的内容不会影响到resultMap（深拷贝）
	 * @param paramsMap
	 *     被拷贝对象
	 * @param resultMap
	 *     拷贝后的对象
	 */
	public static void copyMap2Map(Map paramsMap, Map resultMap) {
	    if (resultMap == null) resultMap = new HashMap();
	    if (paramsMap == null) return;

	    Iterator it = paramsMap.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry entry = (Map.Entry) it.next();
	        Object key = entry.getKey();
	        resultMap.put(key, paramsMap.get(key));
	    }
	}
	/**
	 * 自动转Map key值大写 将Map内的key与Bean中属性相同的内容复制到BEAN中
	 * 
	 * @param bean
	 *            Object
	 * @param properties
	 *            Map
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public static void copyMap2Bean_Nobig(Object bean, Map properties)
			throws IllegalAccessException, InvocationTargetException {
		// Do nothing unless both arguments have been specified
		if ((bean == null) || (properties == null)) {
			return;
		}
		// Loop through the property name/value pairs to be set
		Iterator names = properties.keySet().iterator();
		while (names.hasNext()) {
			String name = (String) names.next();
			// Identify the property name and value(s) to be assigned
			if (name == null) {
				continue;
			}
			Object value = properties.get(name);
			// 命名应该大小写应该敏感(否则取不到对象的属性)
			// name = name.toLowerCase();
			try {
				if (value == null) { // 不光Date类型，好多类型在null时会出错
					continue; // 如果为null不用设 (对象如果有特殊初始值也可以保留？)
				}
				Class clazz = PropertyUtils.getPropertyType(bean, name);
				if (null == clazz) { // 在bean中这个属性不存在
					continue;
				}
				String className = clazz.getName();
				// 临时对策（如果不处理默认的类型转换时会出错）
				if (className.equalsIgnoreCase("java.util.Date")) {
					value = new java.util.Date(
							((java.sql.Timestamp) value).getTime());// wait to
																	// do：貌似有时区问题,
																	// 待进一步确认
				}
				// if (className.equalsIgnoreCase("java.sql.Timestamp")) {
				// if (value == null || value.equals("")) {
				// continue;
				// }
				// }
				getInstance().setSimpleProperty(bean, name, value);
			} catch (NoSuchMethodException e) {
				continue;
			}
		}
	}

	/**
	 * Map内的key与Bean中属性相同的内容复制到BEAN中 对于存在空值的取默认值
	 * 
	 * @param bean
	 *            Object
	 * @param properties
	 *            Map
	 * @param defaultValue
	 *            String
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public static void copyMap2Bean(Object bean, Map properties,
			String defaultValue) throws IllegalAccessException,
			InvocationTargetException {
		// Do nothing unless both arguments have been specified
		if ((bean == null) || (properties == null)) {
			return;
		}
		// Loop through the property name/value pairs to be set
		Iterator names = properties.keySet().iterator();
		while (names.hasNext()) {
			String name = (String) names.next();
			// Identify the property name and value(s) to be assigned
			if (name == null) {
				continue;
			}
			Object value = properties.get(name);
			try {
				Class clazz = PropertyUtils.getPropertyType(bean, name);
				if (null == clazz) {
					continue;
				}
				String className = clazz.getName();
				if (className.equalsIgnoreCase("java.sql.Timestamp")) {
					if (value == null || value.equals("")) {
						continue;
					}
				}
				if (className.equalsIgnoreCase("java.lang.String")) {
					if (value == null) {
						value = defaultValue;
					}
				}
				getInstance().setSimpleProperty(bean, name, value);
			} catch (NoSuchMethodException e) {
				continue;
			}
		}
	}
   /**
    * 筛选出来includeMap是否包含list类型的，这是对于后台的jsp页面，页面会有srcList[0].productName，这种list形式的提交，要能够把它
    * 转换到虚拟字段list中
    * @param includeMap
    * @return
    */
   private static Map<String,Object> ifIncludeMapHasListClassType(Map includeMap){
	   Map<String,Object> temp=Maps.newHashMap();
	   Iterator entries = includeMap.entrySet().iterator(); 
	   while (entries.hasNext()) { 
	     Map.Entry entry = (Map.Entry) entries.next(); 
	     String key =(String)entry.getKey(); 
	     Object value = entry.getValue(); 
		   if(((Class)value).equals(List.class)){
		     temp.put(key, value);
	       }
	   }
	 
	   return temp;
   }
	/**
	 * 处理的是cgibean
	 * 将request中传入的参数直接转入到bean，不需要request.getparameter一个个取,注意，此处为浅copy，若json中嵌套有对象，无法copy
	 * @param request
	 * @param bean
	 */
   public static Object copyRequestParamToCgiBean(HttpServletRequest request,Object bean,Map includeMap){
	   try{
		 //取出request中的所有属性
		     Enumeration<String> e = request.getParameterNames();
		     //遍历
		     if(includeMap==null)return bean;
		     Map<String,Object> listAttrName= ifIncludeMapHasListClassType(includeMap);//具有list虚拟字段的字段名列表
		     Map<String,List<ListAttrVO>> listAttrValue=Maps.newHashMap();
		     while (e.hasMoreElements()) {
		         String name = e.nextElement();
		         if(!includeMap.containsKey(name)){
		        	 if(isContainsArray(name))
		        	 for (Map.Entry<String,Object> entry : listAttrName.entrySet()) { 
		        		 String prefix=entry.getKey();
		        		 if(isMatch(name,prefix)){//如果是满足该前缀的数组
		        			 List<ListAttrVO> datacollect=null;
		        			 if(listAttrValue.containsKey(prefix)){
		        				 datacollect= listAttrValue.get(prefix);
		        			 }else{
		        				 datacollect= Lists.newArrayList();
		        				 listAttrValue.put(prefix, datacollect);
		        			 }
		        			 ListAttrVO lav=getListAttrVO(name,request.getParameter(name), prefix);
		        			 datacollect.add(lav);
		        		 }
		        	 }
		        	 continue;
		         }  
		         String value = request.getParameter(name);
		         if(StringUtils.isEmpty(value))continue;
		         if(!Reflections.isContainFieldDyna(bean, name))continue;
		         Object convertValue=getDestObjectFieldValue(bean,name,value);//"$cglib_prop_"+name
		         BeanUtils.setProperty(bean, name, convertValue); 
		     }
		     dealListDynaAttr(listAttrValue,bean);
		    
		 } catch (Exception e) {
		             throw new RuntimeException(e);
		 }
	   return bean;
   }
   private static  List<Map<String,Object>> findMaxSize(List<ListAttrVO> lavList){
	   int maxSize=-1;
	   List<Map<String,Object>> temp=Lists.newArrayList();
	   for(ListAttrVO lav:lavList){
		   if(lav.getFieldIndex().intValue()>maxSize)maxSize=lav.getFieldIndex().intValue();
	   }
	   for(int i=0;i<maxSize+1;i++){
		   Map<String,Object> t=Maps.newHashMap();
		   temp.add(t);
	   }
	   return temp;
   }
   private static void dealListDynaAttr(  Map<String,List<ListAttrVO>> listAttrValue,Object bean) throws IllegalAccessException, InvocationTargetException{
	   for (Map.Entry<String,List<ListAttrVO>> entry : listAttrValue.entrySet()) { 
		   String prefix=entry.getKey();
		   Field field=Reflections.getAccessibleField(bean, "$cglib_prop_"+prefix);
		   if(field==null)continue;

		   List<ListAttrVO> lavList=entry.getValue();
		   List<Map<String,Object>> result=findMaxSize(lavList);
		   for(ListAttrVO lav:lavList){
			   Map<String,Object> objMap=result.get(lav.getFieldIndex());
			   objMap.put(lav.getFieldName(), lav.getFieldValue());
		   }
		   //由于从页面过来的，第一条记录是伪记录，或者说从中间位置移除数据，这时的数组的下标是比如从1、3、57这种开始，而findMaxSize(lavList)是从0开始到最大下标，因此会有很多空出来的数据
		   //所以要移除这些数据。
		   Iterator<Map<String,Object>> it = result.iterator();
		   while(it.hasNext()){
			   Map<String,Object> x = it.next();
		       if(x.size()<1)it.remove();
		   }
	       BeanUtils.setProperty(bean, prefix, result); 
	   }
   }
   /**
    * 判断是否要转换字段是否是java基础类型，若不是，则视为bean类型
    * @param valType
    * @return
    */
   public static boolean isBasicJavaType(Class<?> valType  ){
	   boolean bl=true;
		if (valType == String.class){
		
		}else if (valType == Integer.class){
		
		}else if (valType == Long.class){
		
		}else if (valType == Double.class){
		
		}else if (valType == Float.class){
			
		}else if (valType == Date.class){
		
		}else{
			bl=false;
		}
		return bl;
   }
   /**
    * 此方法是为了有些时候插入添加记录时，某些字段组合不允许重复，通常是从数据库根据这些字段查询是否有记录，而对于更新情况
    */
   public static void removeEntityFromListById(List findList,TjBaseEntity removeObj){
	   if(findList==null||findList.size()==0||removeObj==null)return ;
	   for(Object be:findList){
		   TjBaseEntity dest=(TjBaseEntity)be;
		   if(dest.getId().equals(removeObj.getId())){
			   findList.remove(be);
			   break;
		   }
			  
	   }
   }
   /**
	 * 将request中传入的参数直接转入到Map，不需要request.getparameter一个个取
	 * @param request
	 * @param bean
	 */
  public static Map copyRequestParamToMap(HttpServletRequest request){
	  Map result=new HashMap();
	   try{
		 //  request.setCharacterEncoding("GBK");  //这里不设置编码会有乱码
		 //取出request中的所有属性
		     Enumeration<String> e = request.getParameterNames();
		     //遍历
		     while (e.hasMoreElements()) {
		    	 
		         String name = e.nextElement();
		         String value = request.getParameter(name);
		        // new String( request.getParameter(name).getBytes("UTF-8"),"GB2312")
		         //设置到map
		         result.put(name, value);
		        
		     }
		   
		 } catch (Exception e) {
		             throw new RuntimeException(e);
		 }
	   return result;
  }
	public MyBeanUtils() {
		super();
	}
	//*********************************以下方法为解析从页面过来的list时使用的工具类*******************************//
	 private static boolean isMatch(String arrName,String prefix){
//    	 String input = "abc[1]dd[a-b]df[3.b]";
         String regex = prefix+"\\[[^\\[\\]]*\\].[a-zA-Z0-9]*";
         Pattern pattern = Pattern.compile (regex);
         Matcher matcher = pattern.matcher (arrName);
        return matcher.find ();
     }
	 public static boolean isContainsArray(String arrName){
//    	 String input = "abc[1]dd[a-b]df[3.b]";
         String regex = "[a-zA-Z0-9]*\\[[^\\[\\]]*\\]\\.[a-zA-Z0-9]*";
         Pattern pattern = Pattern.compile (regex);
         Matcher matcher = pattern.matcher (arrName);
        return matcher.find ();
     }
	 private static ListAttrVO getListAttrVO(String key,String value,String prefix){
		 String regex = prefix+"\\[[^\\[\\]]*\\].[a-zA-Z0-9]*";
         Pattern pattern = Pattern.compile (regex);
         Matcher matcher = pattern.matcher (key);
         ListAttrVO lav=new ListAttrVO();
         lav.setFieldValue(value);
         if (matcher.find ())
         {
        	 String dest=matcher.group (0);
        	 String listName=prefix;
        	 Integer index=findListIndex(dest);
        	 String fieldName=findFieldName(dest);
        	 lav.setFieldIndex(index);
        	 lav.setFieldName(fieldName);
         }
         return lav;
	 }

     private static Integer findListIndex(String dest){
    	 Integer i=null;
    	  String regex = "\\[[^\\[\\]]*\\]";
    	   Pattern pattern = Pattern.compile (regex);
    	   Matcher matcher = pattern.matcher (dest);
    	   if (matcher.find ())
           {
    		   String curr=matcher.group (0);
    		   curr=curr.replace("[", "");
    		   curr=curr.replace("]", "");
    		   i=new Integer(curr);
           }
    	   return i;
     }
     public static String findFieldName(String dest){
     	String listName="";
     	String[] temp=dest.split("\\]\\.");
     	listName=temp[1];
     	return listName;
      }
//     private static String findListName(String dest){
//    	String listName="";
//    	String[] temp=dest.split("\\[");
//    	listName=temp[0];
//    	return listName;
//     }
     static class ListAttrVO {     //内部类
    	 private Integer fieldIndex;
    	 private String fieldName;
    	 private String fieldValue;
		 public ListAttrVO(Integer fieldIndex, String fieldName,
				String fieldValue) {
			super();
			this.fieldIndex = fieldIndex;
			this.fieldName = fieldName;
			this.fieldValue = fieldValue;
		}
		
		public ListAttrVO() {
			super();
		}

		public Integer getFieldIndex() {
			return fieldIndex;
		}
		public void setFieldIndex(Integer fieldIndex) {
			this.fieldIndex = fieldIndex;
		}
		public String getFieldName() {
			return fieldName;
		}
		public void setFieldName(String fieldName) {
			this.fieldName = fieldName;
		}
		public String getFieldValue() {
			return fieldValue;
		}
		public void setFieldValue(String fieldValue) {
			this.fieldValue = fieldValue;
		}
        
     }
}
