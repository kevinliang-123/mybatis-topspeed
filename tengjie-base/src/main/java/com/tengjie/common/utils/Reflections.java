package com.tengjie.common.utils;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javassist.ClassPool;
import javassist.Loader;


import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.Assert;


import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tengjie.common.persistence.TjBaseEntity;

/**
 * 反射工具类.
 * 提供调用getter/setter方法, 访问私有变量, 调用私有方法, 获取泛型类型Class, 被AOP过的真实类等工具函数.
 * @author
 * @version 2013-01-15
 */
@SuppressWarnings("rawtypes")
public class Reflections {
	
	private static final String SETTER_PREFIX = "set";

	private static final String GETTER_PREFIX = "get";

	private static final String CGLIB_CLASS_SEPARATOR = "$$";
	
	private static Logger logger = LoggerFactory.getLogger(Reflections.class);
	/**
	 * 根据一个带路径的类名，获得类的实例对象
	 * 获取规则为，先到SpringContextHolder中获取该对象，若不存在（即不在spring环境中），则会创建一个新的实例对象返回
	 * @param className
	 * @return
	 * @throws Exception 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
    public static Object getInstanceByClassName(String className,String ...packagePath) throws Exception {
		Object obj=null;
		try{
			obj=SpringContextHolder.getBean(Class.forName(className).newInstance().getClass());
		}catch(Exception ex){
			try {
				obj=Class.forName(className).newInstance();
			} catch (InstantiationException | IllegalAccessException
					| ClassNotFoundException e) {
				String path=packagePath.length>0?packagePath[0]:"com.tengjie";
				List<String> dest=ClassScaner.scan(path, className);
				if(dest!=null&&dest.size()>0)
				obj=Class.forName(dest.get(0)).newInstance();
			}
		}
		if(obj==null)
		throw new Exception("找不到类"+className);
		return obj;
    }
	/**
	 * 调用Getter方法.
	 * 支持多级，如：对象名.对象名.方法
	 */
	public static Object invokeGetter(Object obj, String propertyName) {
		Object object = obj;
		for (String name : StringUtils.split(propertyName, ".")){
			String getterMethodName = GETTER_PREFIX + StringUtils.capitalize(name);
			Class clazz=getObjPropClass(obj, propertyName);
			if(clazz.getSimpleName().equals("boolean")) { 
				getterMethodName = "is" + StringUtils.capitalize(name);
			}
			object = invokeMethod(object, getterMethodName, new Class[] {}, new Object[] {});
		}
		return object;
	}
	/**
	 * 获得一个bean中的全部属性，
	 * @param obj
	 * @param bl true，表示全部属性字段，false:只是当前类的属性字段，包括动态属性
	 * @return
	 */
	public static List<String> findAllField(Object obj,boolean bl) {
		List<String> fieldNameList=Lists.newArrayList();
		if(bl){
			PropertyDescriptor[] pds = PropertyUtils.getPropertyDescriptors(obj);
			for(PropertyDescriptor pd:pds){
				fieldNameList.add(pd.getName());
			}
		}else{
			String dynaClassName=obj.getClass().getSimpleName();
			if(dynaClassName.contains("$$BeanGeneratorByCGLIB")) {//是动态bean
				String originClassName=dynaClassName.substring(0,dynaClassName.indexOf("$$"));//原始的类名
				Class<?> currentClass = obj.getClass();
			    while (currentClass != null&&currentClass.getSimpleName().contains(originClassName)) {
			         final Field[] declaredFields = currentClass.getDeclaredFields();
			         for (final Field field : declaredFields) {
			        	 String fieldName=field.getName();
			        	 if(fieldName.contains("$cglib_prop_"))fieldName=fieldName.replace("$cglib_prop_", "");
			        	 fieldNameList.add(fieldName);
			         }
			         currentClass = currentClass.getSuperclass();
			    }
			}else {
				Field[] ffArray=obj.getClass().getDeclaredFields();
				for(Field ff:ffArray){
					fieldNameList.add(ff.getName());
				}
			}
			
		}
		return fieldNameList;
	}
	/**
	 * 这个是获得一个cglib动态bean中的动态字段，如果不是动态bean什么也取不到
	 * @param obj
	 * @param ifNeedValue 是否需要value值
	 * @return
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	//说明：QuestionLibrary$$BeanGeneratorByCGLIB$$4d55e793$$BeanGeneratorByCGLIB$$ff6cdf6a@1f72a0e,注意看这个类，
	//这是调用了多次generateBean或者说initDynaMap，每次创建他都会继承上一个类，所以obj.getClass().getDeclaredFields()只能拿到最后一次的动态属性（注意，都不包括原生属性字段），
	//obj.getClass().getSuperclass().getDeclaredFields()能拿到上一次的动态属性，再次obj.getClass().getSuperclass().getSuperclass().getDeclaredFields()
	//才能够拿到QuestionLibrary的属性，因为getDeclaredFields本身就只是拿当前子类的属性。%%%
	//class.getDeclaredFields()能获取所有属性（public、protected、default、private），但不包括父类属性，相对的class.getFields() 获取类的属性（public），包括父类,但是注意，只能是public的
	public static Map<String,Object> findAllDynaFieldForCglib(Object obj,boolean ifNeedValue) throws IllegalArgumentException, IllegalAccessException {
		Map<String,Object> fieldNameMap=Maps.newHashMap();
		String dynaClassName=obj.getClass().getSimpleName();
		if(!dynaClassName.contains("$$BeanGeneratorByCGLIB"))return fieldNameMap;
		String originClassName=dynaClassName.substring(0,dynaClassName.indexOf("$$"));//原始的类名
		
		Class<?> currentClass = obj.getClass();
	    while (currentClass != null&&!currentClass.getSimpleName().equals(originClassName)) {
	         final Field[] declaredFields = currentClass.getDeclaredFields();
	         for (final Field field : declaredFields) {
	        	 fieldNameMap.put(field.getName(),ifNeedValue?FieldUtils.readField(field, obj,true):field.getName());
	         }
	         currentClass = currentClass.getSuperclass();
	    }
		return fieldNameMap;
	}
	
	/**
	 * 与上面方法不同，这个是获得动态属性以及其对应的class类型
	 * @param obj
	 * @param ifNeedValue
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public static Map<String,Class> findAllDynaFieldForCglib(Object obj) throws IllegalArgumentException, IllegalAccessException {
		Map<String,Class> fieldNameMap=Maps.newHashMap();
		String dynaClassName=obj.getClass().getSimpleName();
		if(!dynaClassName.contains("$$BeanGeneratorByCGLIB"))return fieldNameMap;
		String originClassName=dynaClassName.substring(0,dynaClassName.indexOf("$$"));//原始的类名
		
		Class<?> currentClass = obj.getClass();
	    while (currentClass != null&&!currentClass.getSimpleName().equals(originClassName)) {
	         final Field[] declaredFields = currentClass.getDeclaredFields();
	         for (final Field field : declaredFields) {
	        	 fieldNameMap.put(field.getName(),field.getClass());
	         }
	         currentClass = currentClass.getSuperclass();
	    }
		return fieldNameMap;
	}
	/**
	 * 调用Setter方法, 仅匹配方法名。
	 * 支持多级，如：对象名.对象名.方法
	 */
	public static void invokeSetter(Object obj, String propertyName, Object value) {
		Object object = obj;
		String[] names = StringUtils.split(propertyName, ".");
		for (int i=0; i<names.length; i++){
			if(i<names.length-1){
				String getterMethodName = GETTER_PREFIX + StringUtils.capitalize(names[i]);
				object = invokeMethod(object, getterMethodName, new Class[] {}, new Object[] {});
			}else{
				String setterMethodName = SETTER_PREFIX + StringUtils.capitalize(names[i]);
				invokeMethodByName(object, setterMethodName, new Object[] { value });
			}
		}
	}

	/**
	 * 直接读取对象属性值, 无视private/protected修饰符, 不经过getter函数.
	 */
	public static Object getFieldValue(final Object obj, final String fieldName) {
		Field field = getAccessibleField(obj, fieldName);

		if (field == null) {
			throw new IllegalArgumentException("Could not find field [" + fieldName + "] on target [" + obj + "]");
		}

		Object result = null;
		try {
			result = field.get(obj);
		} catch (IllegalAccessException e) {
			logger.error("不可能抛出的异常{}", e.getMessage());
		}
		return result;
	}

	/**
	 * 直接设置对象属性值, 无视private/protected修饰符, 不经过setter函数.
	 */
	public static void setFieldValue(final Object obj, final String fieldName, final Object value) {
		Field field = getAccessibleField(obj, fieldName);

		if (field == null) {
			throw new IllegalArgumentException("Could not find field [" + fieldName + "] on target [" + obj + "]");
		}

		try {
			field.set(obj, value);
		} catch (IllegalAccessException e) {
			logger.error("不可能抛出的异常:{}", e.getMessage());
		}
	}

	/**
	 * 直接调用对象方法, 无视private/protected修饰符.
	 * 用于一次性调用的情况，否则应使用getAccessibleMethod()函数获得Method后反复调用.
	 * 同时匹配方法名+参数类型，
	 */
	public static Object invokeMethod(final Object obj, final String methodName, final Class<?>[] parameterTypes,
			final Object[] args) {
		Method method = getAccessibleMethod(obj, methodName, parameterTypes);
		if (method == null) {
			throw new IllegalArgumentException("Could not find method [" + methodName + "] on target [" + obj + "]");
		}

		try {
			return method.invoke(obj, args);
		} catch (Exception e) {
			throw convertReflectionExceptionToUnchecked(e);
		}
	}
	/**
	 * 直接调用对象方法, 无视private/protected修饰符，
	 * 用于一次性调用的情况，否则应使用getAccessibleMethodByName()函数获得Method后反复调用.
	 * 只匹配函数名，如果有多个同名函数调用第一个。
	 */
	public static Object invokeMethodByName(final Object obj, final String methodName,final Class[] classes, final Object[] args) {
		Method method = getAccessibleMethodByName(obj, methodName);

		if (method == null) {
			try {
				method=getJavassistMethod(obj.getClass().getName(),methodName,classes,args);
			} catch (ClassNotFoundException | InstantiationException| IllegalAccessException | NoSuchMethodException| SecurityException e) {
				e.printStackTrace();
			}
			if(method==null)throw new IllegalArgumentException("Could not find method [" + methodName + "] on target [" + obj + "]");
		}

		try {
			//org.springframework.util.ReflectionUtils.invokeMethod(method, obj, args);
			return method.invoke(obj, args);
		} catch (Exception e) {
			throw convertReflectionExceptionToUnchecked(e);
		}
	}
	/**
	 * 直接调用对象方法, 无视private/protected修饰符，
	 * 用于一次性调用的情况，否则应使用getAccessibleMethodByName()函数获得Method后反复调用.
	 * 只匹配函数名，如果有多个同名函数调用第一个。
	 */
	public static Object invokeMethodByName(final Object obj, final String methodName, final Object[] args) {
		Method method = getAccessibleMethodByName(obj, methodName);

		if (method == null) {
		   throw new IllegalArgumentException("Could not find method [" + methodName + "] on target [" + obj + "]");
		}

		try {
			//org.springframework.util.ReflectionUtils.invokeMethod(method, obj, args);
			return method.invoke(obj, args);
		} catch (Exception e) {
			throw convertReflectionExceptionToUnchecked(e);
		}
	}

	/**
	 * 循环向上转型, 获取对象的DeclaredField, 并强制设置为可访问.
	 * 
	 * 如向上转型到Object仍无法找到, 返回null.
	 */
	public static Field getAccessibleField(final Object obj, final String fieldName) {
		Validate.notNull(obj, "object can't be null");
		Validate.notBlank(fieldName, "fieldName can't be blank");
		for (Class<?> superClass = obj.getClass(); superClass != Object.class; superClass = superClass.getSuperclass()) {
			try {
				Field field = superClass.getDeclaredField(fieldName);
				makeAccessible(field);
				return field;
			} catch (NoSuchFieldException e) {//NOSONAR
				// Field不在当前类定义,继续向上转型
				continue;// new add
			}
		}
		return null;
	}

	/**
	 * 循环向上转型, 获取对象的DeclaredField, 并强制设置为可访问.
	 * 本方法的传入参数为类而不是对象
	 * 如向上转型到Object仍无法找到, 返回null.
	 */
	public static Field getAccessibleField(final Class obj, final String fieldName) {
		Validate.notNull(obj, "object can't be null");
		Validate.notBlank(fieldName, "fieldName can't be blank");
		for (Class<?> superClass = obj; superClass != Object.class; superClass = superClass.getSuperclass()) {
			try {
				Field field = superClass.getDeclaredField(fieldName);
				makeAccessible(field);
				return field;
			} catch (NoSuchFieldException e) {//NOSONAR
				// Field不在当前类定义,继续向上转型
				continue;// new add
			}
		}
		return null;
	}
	/**
	 * 循环向上转型, 获取对象的DeclaredMethod,并强制设置为可访问.
	 * 如向上转型到Object仍无法找到, 返回null.
	 * 匹配函数名+参数类型。
	 * 
	 * 用于方法需要被多次调用的情况. 先使用本函数先取得Method,然后调用Method.invoke(Object obj, Object... args)
	 */
	public static Method getAccessibleMethod(final Object obj, final String methodName,
			final Class<?>... parameterTypes) {
		Validate.notNull(obj, "object can't be null");
		Validate.notBlank(methodName, "methodName can't be blank");

		for (Class<?> searchType = obj.getClass(); searchType != Object.class; searchType = searchType.getSuperclass()) {
			try {
				Method method = searchType.getDeclaredMethod(methodName, parameterTypes);
				makeAccessible(method);
				return method;
			} catch (NoSuchMethodException e) {
				// Method不在当前类定义,继续向上转型
				continue;// new add
			}
		}
		return null;
	}
	/**
	 * 循环向上转型, 获取对象的DeclaredMethod,并强制设置为可访问.
	 * 如向上转型到Object仍无法找到, 返回null.
	 * 只匹配函数名。
	 * 
	 * 用于方法需要被多次调用的情况. 先使用本函数先取得Method,然后调用Method.invoke(Object obj, Object... args)
	 */
	public static List<Method> getAccessibleMethodsByName(final Object obj, final String methodName) {
		Validate.notNull(obj, "object can't be null");
		Validate.notBlank(methodName, "methodName can't be blank");
		List<Method> list=new ArrayList();
		for (Class<?> searchType = obj.getClass(); searchType != Object.class; searchType = searchType.getSuperclass()) {
			Method[] methods = searchType.getDeclaredMethods();
			for (Method method : methods) {
				if (method.getName().equals(methodName)) {
					makeAccessible(method);
					list.add(method) ;
				}
			}
		}
		return list;
	}
	/**
	 * 循环向上转型, 获取对象的DeclaredMethod,并强制设置为可访问.
	 * 如向上转型到Object仍无法找到, 返回null.
	 * 只匹配函数名。
	 * 
	 * 用于方法需要被多次调用的情况. 先使用本函数先取得Method,然后调用Method.invoke(Object obj, Object... args)
	 */
	public static Method getAccessibleMethodByName(final Object obj, final String methodName) {
		Validate.notNull(obj, "object can't be null");
		Validate.notBlank(methodName, "methodName can't be blank");

		for (Class<?> searchType = obj.getClass(); searchType != Object.class; searchType = searchType.getSuperclass()) {
			Method[] methods = searchType.getDeclaredMethods();
			for (Method method : methods) {
				if (method.getName().equals(methodName)) {
					makeAccessible(method);
					return method;
				}
			}
		}
		return null;
	}

	/**
	 * 改变private/protected的方法为public，尽量不调用实际改动的语句，避免JDK的SecurityManager抱怨。
	 */
	public static void makeAccessible(Method method) {
		if ((!Modifier.isPublic(method.getModifiers()) || !Modifier.isPublic(method.getDeclaringClass().getModifiers()))
				&& !method.isAccessible()) {
			method.setAccessible(true);
		}
	}

	/**
	 * 改变private/protected的成员变量为public，尽量不调用实际改动的语句，避免JDK的SecurityManager抱怨。
	 */
	public static void makeAccessible(Field field) {
		if ((!Modifier.isPublic(field.getModifiers()) || !Modifier.isPublic(field.getDeclaringClass().getModifiers()) || Modifier
				.isFinal(field.getModifiers())) && !field.isAccessible()) {
			field.setAccessible(true);
		}
	}

	/**
	 * 通过反射, 获得Class定义中声明的泛型参数的类型, 注意泛型必须定义在父类处
	 * 如无法找到, 返回Object.class.
	 * eg.
	 * public UserDao extends HibernateDao<User>
	 *
	 * @param clazz The class to introspect
	 * @return the first generic declaration, or Object.class if cannot be determined
	 */
	@SuppressWarnings("unchecked")
	public static <T> Class<T> getClassGenricType(final Class clazz) {
		return getClassGenricType(clazz, 0);
	}

	/**
	 * 通过反射, 获得Class定义中声明的父类的泛型参数的类型.
	 * 如无法找到, 返回Object.class.
	 * 
	 * 如public UserDao extends HibernateDao<User,Long>
	 *
	 * @param clazz clazz The class to introspect
	 * @param index the Index of the generic ddeclaration,start from 0.
	 * @return the index generic declaration, or Object.class if cannot be determined
	 */
	public static Class getClassGenricType(final Class clazz, final int index) {

		Type genType = clazz.getGenericSuperclass();

		if (!(genType instanceof ParameterizedType)) {
			logger.warn(clazz.getSimpleName() + "'s superclass not ParameterizedType");
			return Object.class;
		}

		Type[] params = ((ParameterizedType) genType).getActualTypeArguments();

		if (index >= params.length || index < 0) {
			logger.warn("Index: " + index + ", Size of " + clazz.getSimpleName() + "'s Parameterized Type: "
					+ params.length);
			return Object.class;
		}
		if (!(params[index] instanceof Class)) {
			logger.warn(clazz.getSimpleName() + " not set the actual class on superclass generic parameter");
			return Object.class;
		}

		return (Class) params[index];
	}
	/**
	 * 获得一个对象的某个属性的类型Class,注意这个方法包括动态属性判断
	 * @param instance
	 * @return
	 */
	public static Class<?> getObjPropClass(Object instance,String fieldName) {
		Field field= getAccessibleField(instance, fieldName);
		if(field==null){
			fieldName="$cglib_prop_"+fieldName+"";
		}
		field=getAccessibleField(instance, fieldName);
		return field.getType();

	}
	/**
	 * 某个对象是否含有某个属性字段,注意这个方法包含了对动态属性的判断
	 * @param instance
	 * @param fieldName
	 * @return
	 */
	public static boolean isContainFieldDyna(Object instance,String fieldName){
		if(getAccessibleField(instance, fieldName)==null){
			fieldName="$cglib_prop_"+fieldName+"";
			if(getAccessibleField(instance, fieldName)==null){
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 某个对象是否含有某个属性字段,注意这个方法不包括动态属性判断
	 * @param instance
	 * @param fieldName
	 * @return
	 */
	public static boolean isContainField(Object instance,String fieldName){
		if(getAccessibleField(instance, fieldName)==null)return false;
		return true;
	}
	/**
	 * 获得对象的所属类的类型
	 * @param instance
	 * @return
	 */
	public static Class<?> getUseClass(Object instance) {
		Assert.notNull(instance, "Instance must not be null");
		Class clazz = instance.getClass();
		if (clazz != null && clazz.getName().contains(CGLIB_CLASS_SEPARATOR)) {
			Class<?> superClass = clazz.getSuperclass();
			if (superClass != null && !Object.class.equals(superClass)) {
				return superClass;
			}
		}
		return clazz;

	}
	
	/**
	 * 将反射时的checked exception转换为unchecked exception.
	 */
	public static RuntimeException convertReflectionExceptionToUnchecked(Exception e) {
		if (e instanceof IllegalAccessException || e instanceof IllegalArgumentException
				|| e instanceof NoSuchMethodException) {
			return new IllegalArgumentException(e);
		} else if (e instanceof InvocationTargetException) {
			return new RuntimeException(((InvocationTargetException) e).getTargetException());
		} else if (e instanceof RuntimeException) {
			return (RuntimeException) e;
		}
		return new RuntimeException("Unexpected Checked Exception.", e);
	}
	/**
	 * 获得javassist方法
	 * @param classPath
	 * @param methodName
	 * @param paramsTypeClass
	 * @param params
	 * @return
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	public static Method getJavassistMethod(String classPath,String methodName,Class[] paramsTypeClass,Object[] params) throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, SecurityException{
		ClassPool pool = ClassPool.getDefault();
		//pool.insertClassPath(new ClassClassPath(this.getClass()));
	 	 Loader cl = new Loader( Thread.currentThread().getContextClassLoader(), pool);
		Class clclazz = cl.loadClass(classPath);
		Object obj=clclazz.newInstance();
		Method m=null;
		if(paramsTypeClass==null){
			 m=clclazz.getMethod(methodName,new Class[]{TjBaseEntity.class});
		}else{
			m=clclazz.getMethod(methodName,paramsTypeClass);
		}
		
		
		return m;
	}
	/**
	 * 反射调用动态生成的javassist生成的方法
	 * @param classPath
	 * @param methodName
	 * @param paramsTypeClass
	 * @param params
	 * @return
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public static Object invokeJavassistMethod(String classPath,String methodName,Class[] paramsTypeClass,Object[] params) throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, SecurityException {
	 	ClassPool pool = ClassPool.getDefault();
		//pool.insertClassPath(new ClassClassPath(this.getClass()));
	 	 Loader cl = new Loader( Thread.currentThread().getContextClassLoader(), pool);
		Class clclazz = cl.loadClass(classPath);
		Object obj=clclazz.newInstance();
		Method m=clclazz.getMethod(methodName,paramsTypeClass);
		
		Object result=null;
		try {
			result = m.invoke(obj, params);
		} catch (IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
		return result;
	}
}
