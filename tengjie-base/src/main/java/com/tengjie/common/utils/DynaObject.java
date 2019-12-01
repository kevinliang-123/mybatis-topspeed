package com.tengjie.common.utils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.restlet.engine.util.DateUtils;

import com.google.common.collect.Maps;

import net.sf.cglib.beans.BeanGenerator;
import net.sf.cglib.beans.BeanMap;

public class DynaObject extends BeanGenerator implements Serializable {
	
	/**
	 * 属性map
	 */
	private BeanMap dynaBeanMap = null;
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unchecked")
	private DynaObject generateBean(Map propertyMap) {

		Set keySet = propertyMap.keySet();
		for (Iterator i = keySet.iterator(); i.hasNext();) {
			String key = (String) i.next();
			this.addProperty(key, (Class) propertyMap.get(key));
		}
		return (DynaObject) create();
	}
	/**
	 * 添加一个属性
	 * @param key
	 * @param classType
	 * @return
	 */
	public DynaObject addField(String key,Class classType){
		Map propertyMap =Maps.newHashMap();
		propertyMap.put(key, classType);
		return initDynaMap(propertyMap);
	}
	public DynaObject addField(String key){
		return addField(key,String.class);
	}
	public DynaObject   initDynaMap(Map propertyMap){
		if(propertyMap!=null){
			Iterator entries = propertyMap.entrySet().iterator(); 
			//如果已经存在属性，则不需要再添加了，主要针对页面的查询条件二次进入;实际测试中不可能，虽然可以进入页面时识别附加属性，但是再次传回controller时，
			//动态代理bean被转换为标准bean，动态属性已经消失，传入的值实际在getparamter中，此时应该从paramter取值放入
			while (entries.hasNext()) { 				
				Map.Entry entry = (Map.Entry) entries.next();  
			    Object key = entry.getKey();  
			    Object value = entry.getValue();
			    Field ff=Reflections.getAccessibleField(this, key+"");
			    if(ff!=null){
			    	propertyMap.remove(key);
			    }
			}
			if(propertyMap.size()<1){
				return this;
			}
		}
		 this.setSuperclass(this.getClass());
		 DynaObject  rr=generateBean(propertyMap);
		 rr.dynaBeanMap = BeanMap.create(rr);
		 try {
			MyBeanUtils.copyBean2Bean(rr,this);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	      return rr;
	}
	
	/**
	 * 给bean属性赋值
	 * 
	 * @param property
	 *            属性名
	 * @param value
	 *            值
	 */
	public void setDynaValue(String property, Object value) {
		dynaBeanMap.put(property, value);
	}

	/**
	 * 通过属性名得到属性值
	 * 
	 * @param property
	 *            属性名
	 * @return 值
	 */
	// public Object getDynaValue(String property) {
	// return dynaBeanMap.get(property);
	// }
	/**
	 * 通过属性名得到属性值
	 * 
	 * @param <E>
	 * @param property
	 *            属性名
	 * @return 值
	 */
	public <V> V getDynaValue(String property) {
		Object obj = dynaBeanMap.get(property);
		if (obj == null)
			return null;
		return (V) obj;
	}

	/**
	 * 通过属性名得到属性值
	 * 
	 * @param property
	 *            属性名
	 * @return 值
	 */
	public String getDynaStringValue(String property) {
		if (dynaBeanMap.get(property) == null)
			return null;
		return dynaBeanMap.get(property) + "";
	}

	/**
	 * 通过属性名得到属性值
	 * 
	 * @param property
	 *            属性名
	 * @return 值
	 */
	public Double getDynaDoubleValue(String property) {
		if (dynaBeanMap.get(property) == null)
			return null;
		return Double.valueOf(dynaBeanMap.get(property) + "");
	}
	  /** 
	   * 通过属性名得到属性值 
	   * @param property 属性名 
	   * @return 值 
	   */  
	 public Integer getDynaIntegerValue(String property) {  
		Object obj = Reflections.invokeGetter(this, property);
		  if( obj==null)return null;
	   return Integer.valueOf(obj+"");  
	 }  
	/**
	 * 通过属性名得到属性值
	 * 
	 * @param property
	 *            属性名
	 * @return 值
	 */
	public BigDecimal getDynaBigDecimalValue(String property) {
		if (dynaBeanMap.get(property) == null)
			return null;
		return BigDecimal
				.valueOf(Double.valueOf(dynaBeanMap.get(property) + ""));
	}

	/**
	 * 通过属性名得到属性值
	 * 
	 * @param property
	 *            属性名
	 * @return 值
	 */
	public Date getDynaDateValue(String property) {
		Object obj = dynaBeanMap.get(property);
		if (obj == null)
			return null;
		if (obj instanceof Date) {
			return (Date) obj;
		}
		return DateUtils.parse(obj + "");
	}
	/** 
	  * 是否含有属性名,特指是否包含动态属性
	  * @param property 属性名 
	  * @return 值 
	  */  
	 public boolean hasProperty(String property) {  
		 if(dynaBeanMap!=null){
			  return dynaBeanMap.containsKey(property);
		 }else{
			   Field ffdyna=Reflections.getAccessibleField(this, "$cglib_prop_"+property+"");
			    if(ffdyna!=null){
			    	return true;
			    }
		 }
	 	return false;
	 	
	 }  
}
