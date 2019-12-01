package com.tengjie.common.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.tengjie.common.persistence.DataEntityFieldCallBackBean;

public class CallBackBean implements Serializable {
	private static final long serialVersionUID = 1L;
	 private String className;//对于在spring环境中的类，可以只是一个类名，否则必须带全路径的包名
     private String methodName;//方法 名,对于导入
     private Object[] otherParam;//其他传入的参数
     
     public CallBackBean(
 			Object currObj, String methodName,Object[] otherParam) {
 		super();
 		this.className = StringUtils.firstToLower(currObj.getClass().getSimpleName());
 		this.methodName = methodName;
 		this.otherParam=otherParam;
 	}
	public CallBackBean(String className, String methodName, Object[] otherParam) {
		super();
		this.className = className;
		this.methodName = methodName;
		this.otherParam = otherParam;
	}
	public String getClassName() {
		return className;
	}
	public void setClassName(String className) {
		this.className = className;
	}
	public String getMethodName() {
		return methodName;
	}
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}
	public Object[] getOtherParam() {
		return otherParam;
	}
	public void setOtherParam(Object[] otherParam) {
		this.otherParam = otherParam;
	}
	public static Object genCallBackBean(CallBackBean cbb) throws Exception{
		Object obj=findCallBackClass(cbb);
		return Reflections.invokeMethodByName(obj, cbb.getMethodName(),cbb.getOtherParam());
	}
	private static  Object  findCallBackClass(CallBackBean defcbb)throws Exception{
		String className=defcbb.getClassName();
		Object obj=null;
		try{
			obj=SpringContextHolder.getBean(className);
		}catch(Exception ex){
			char[] ch = className.toCharArray();
			if (ch[0] >= 'A' && ch[0] <= 'Z') {
				System.out.println("在当前spring环境中找不到" + className+ "对应的类，请将首字母小写！");
			} else {
				System.out.println("在当前spring环境中找不到" + className+ "对应的类，若该类不在spring环境中，请输入完整的包名+类名！");
			}
		}
		if(obj==null){
			obj=Class.forName(className).newInstance();
		}
		if (obj == null)
			throw new ClassNotFoundException("找不到]" + className+ "]对应的类,请核对是否正确！");
		return obj;
	}
}
