package com.tengjie.common.utils;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * 在大量运用反射，特别是传入一个class,调用class.newInstance(),有些类比如INTGER、map等实际没有空构建器，所以会报错
 * 本方法进行处理
 * 
 * @author liangfeng
 *
 */
public class NewInstanceUtil {
	/**
	 * 判断一个类型是否有空构建器
	 * @param typeClass
	 * @return
	 */
	public static boolean ifHasNullConstructor(Class typeClass) {
		boolean hasinis = true;
		try {
			typeClass.getConstructor(null);// Integer等没有空参构建器,在instanceof 时调用typeClass.newInstance()会报错
		} catch (Exception e) {
			hasinis = false;
		}
		return hasinis;
	}
	/**
	 * 注意，此方法目前创建的初衷是为了instanceof比较时使用，不能当做值类型使用，因为对于new Integer，实际是随便赋值了一个0
	 * @param typeClass
	 * @return
	 */
	public static Object newInstance(Class typeClass){
        Object dest=null;
		 if(!ifHasNullConstructor(typeClass)){//没有空构建器
			 if(typeClass.getCanonicalName().equals("java.util.Map")){
				 dest=new HashMap();
			 }else if(typeClass.getCanonicalName().equals("java.lang.Integer")){
				 dest=new Integer(0);//随便一个值，因为此方法主要是用来与 instanceof与其他类型进行比较
			 }else if(typeClass.getCanonicalName().equals("java.lang.Double")){
				 dest= new Double(0);//随便一个值，因为此方法主要是用来与 instanceof与其他类型进行比较
			 }else if(typeClass.getCanonicalName().equals("java.lang.Long")){
				 dest=new Long(0);//随便一个值，因为此方法主要是用来与 instanceof与其他类型进行比较
			 }else if(typeClass.getCanonicalName().equals("java.lang.Short")){
				 dest=new Short("0");//随便一个值，因为此方法主要是用来与 instanceof与其他类型进行比较
			 }else if(typeClass.getCanonicalName().equals("java.math.BigDecimal")){
				 dest= new BigDecimal(0);//随便一个值，因为此方法主要是用来与 instanceof与其他类型进行比较
			 }else if(typeClass.getCanonicalName().contains("java.util.List")){
				 dest=new ArrayList();//随便一个值，因为此方法主要是用来与 instanceof与其他类型进行比较
			 }
		 }else{
			 try {
				dest=typeClass.newInstance();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		 }
		return dest;
	}

}
