package com.tengjie.common.utils;

/**
 * tjMap的get方法有问题，如果遇到为空则放回=，但是不能动，因为在mapper处理中要使用，因此通用的用这个map，tjMap不再维护
 */
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * 为创建动态属性和mapper中处理运算符时专用map
 * 
 * @author liangfeng
 *
 * @param <K>
 * @param <V>
 */
public class TjNewMap<K, V> extends HashMap<K, V> implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 通用添加方法
	 * 
	 * @param key
	 * @param value
	 * @return 返回当前map对象
	 */
	public TjNewMap<K, V> putCon(K key, V value) {
		super.put(key, value);
		return this;
	}

	/**
	 * 添加el表达式形式的value，会自动对value进行添加${}
	 * 
	 * @param key
	 * @param value
	 * @return 返回当前map对象
	 */
	public TjNewMap<K, V> putConEl(String key, String value) {
		super.put((K) key, (V)("${"+value+"}"));
		return this;
	}
	/**
	 * 通用添加方法
	 * 
	 * @param key
	 * @param value
	 * @return 返回当前map对象
	 */
	public TjNewMap<K, V> putConMutil(K... keys) {
		for (K key : keys) {
			super.put(key, (V) key);
		}
		return this;
	}
	/**
	 * 通用添加方法
	 * 
	 * @param key
	 * @param value
	 * @return 返回当前map对象
	 */
	public TjNewMap<K, V> putCon(K... keys) {
		for (K key : keys) {
			super.put(key, (V) key);
		}
		return this;
	}

	/**
	 * 本方法为动态添加属性时使用，key为属性名，value自动为String.class
	 * 
	 * @param key 属性名
	 * @return
	 */
	public TjNewMap<K, V> put(K key) {
		super.put(key, (V) java.lang.String.class);
		return this;
	}

	/**
	 * 本方法为动态添加属性时使用，key为属性名，value自动为String.class
	 * 
	 * @param key 属性名
	 * @return
	 */
	public TjNewMap<K, V> removeCon(K... key) {
		for (K t : key) {
			super.remove(t);
		}
		return this;
	}

	/**
	 * 为mapper中处理运算符时使用，若get不到，默认返回=
	 */
	public V get(Object key) {
		V value = null;
		value = super.get(key);
		if (value == null || StringUtils.isEmpty(value + "")) {
			return null;
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
		a = (K[]) set.toArray();
		for (int i = 0; i < a.length; i++) {
			if (a[i].toString().indexOf(key.toString()) == -1) {
				continue;
			} else {
				list.add(this.get(a[i]));
			}
		}
		return list;

	}
	/**
	 * 传入多个key，只要有一个包含，就返回true
	 * @param keys
	 * @return
	 */
    public boolean containsMutiKey(K ...keys) {
    	boolean bl=false;
    	for(K key:keys) {
    		if(super.containsKey(key)) {
    			bl=true;
    			break;
    		}
    	}
    	return bl;
    }
    /**
	 * 根据目标类型clazz获得对应的值
	 * 
	 * @param <T>
	 * @param key
	 * @param clazz
	 * @return
	 */
	public String getStringValue(String key,String ...defaultValues) {
		Object obj=this.get(key);
		if(obj==null) {
			if(defaultValues.length>0) {
				return defaultValues[0];
			}else {
				return null;
			}
		}
		return obj.toString();
	}
	/**
	 * 获取Double类型的值
	 * 
	 * @param <T>
	 * @param key
	 * @param clazz
	 * @param defaultValue:为空时返回的默认值
	 * @return
	 */
	public Double getDoubleValue(String key,Double ...defaultValues) {
		Object obj=this.get(key);
		if(obj==null) {
			if(defaultValues.length>0) {
				return defaultValues[0];
			}else {
				return null;
			}
		}else if(obj.toString().equals("null")) {
			return defaultValues[0];
		}
		return new Double(obj.toString());
	}
	/**
	 * 获得T范型类型的值，请注意实际数据的类型，否则会出现强制转换异常
	 * 如果要实现目标类型的字段转换，请调用getValue(String key, Class<T> clazz, Object defaultValue) 方法
	 * @param <T>
	 * @param key
	 * @param clazz
	 * @param defaultValue:为空时返回的默认值
	 * @return
	 */
	public <T> T getValue(String key, T defaultValue) {
		T result = null;
		Object obj = super.get(key);
		if (obj != null) {
			return (T)obj;
		} else {
		    result = defaultValue;
		}
		return result;
	}
	/**
	 * 根据目标类型clazz获得对应的值
	 * 本方法会调用MyBeanUtils.getDestTypeValue进行类型转换
	 * @param <T>
	 * @param key
	 * @param clazz
	 * @param defaultValue:为空时返回的默认值
	 * @return
	 */
	public <T> T getValue(String key, Class<T> clazz, T defaultValue) {
		T result = null;
		Object obj = super.get(key);
		if (obj != null) {
			try {
				return MyBeanUtils.getDestTypeValue(obj, clazz);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		} else {
			if (defaultValue != null) {
				result =  defaultValue;
			}

		}
		return result;
	}

	public static <K, V> TjNewMap newInstance() {
		return new TjNewMap<K, V>();
	}
}
