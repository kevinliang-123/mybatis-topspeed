package com.tengjie.common.persistence;

import java.io.Serializable;

import com.tengjie.common.utils.StringUtils;

public class DataEntityFieldCallBackBean implements Serializable {
	 private static final long serialVersionUID = 1L;
	 private String className;//必须带包名
     private String methodName;//方法 名,对于导入
     private Object[] otherParam;//其他传入的参数
     private boolean quick=false;//新增属性，供快速回调使用
     private boolean ifToolMethod=false;//是否调用的是如dateUtils等工具类，如果是的话，则只返回值，不包含resultset
     private String tableName;//新增属性，供快速回调使用
     private  String resultPropName;//新增属性，供快速回调使用
     private String queryPropName;// 要查询的属性名，供快速回调使用
     private String valuePropName;// 要查询的属性名，值属性名称，可以是当前查询中任何一个其他查询字段的值
     
	public DataEntityFieldCallBackBean() {
		super();
	}
	public DataEntityFieldCallBackBean(boolean quick, String tableName,
			String resultPropName,String queryPropName,String valuePropName) {
		super();
		this.quick = quick;
		this.tableName = tableName;
		this.resultPropName = resultPropName;
		this.queryPropName = queryPropName;
		this.valuePropName = valuePropName;
	}
	public DataEntityFieldCallBackBean(
			String className, String methodName) {
		super();

		this.className = className;
		this.methodName = methodName;
	}
	/**
	 * 
	 * @param className:若在spring环境中的类，则可以直接用小写开头的类名，否则的话要用全路径
	 * @param methodName
	 * @param otherParam
	 */
	public DataEntityFieldCallBackBean(
			String className, String methodName,Object[] otherParam) {
		super();

		this.className = className;
		this.methodName = methodName;
		this.otherParam=otherParam;
	}
	public DataEntityFieldCallBackBean(
			Object currObj, String methodName,Object[] otherParam) {
		super();

		this.className = StringUtils.firstToLower(currObj.getClass().getSimpleName());
		this.methodName = methodName;
		this.otherParam=otherParam;
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
	public boolean isQuick() {
		return quick;
	}
	public void setQuick(boolean quick) {
		this.quick = quick;
	}
	public String getTableName() {
		return tableName;
	}
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	public String getResultPropName() {
		return resultPropName;
	}
	public void setResultPropName(String resultPropName) {
		this.resultPropName = resultPropName;
	}
	public String getQueryPropName() {
		return queryPropName;
	}
	public void setQueryPropName(String queryPropName) {
		this.queryPropName = queryPropName;
	}
	public String getValuePropName() {
		return valuePropName;
	}
	public void setValuePropName(String valuePropName) {
		this.valuePropName = valuePropName;
	}
	public boolean isIfToolMethod() {
		return ifToolMethod;
	}
	public void setIfToolMethod(boolean ifToolMethod) {
		this.ifToolMethod = ifToolMethod;
	}
     
     
}
