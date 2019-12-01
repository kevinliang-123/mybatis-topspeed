package com.tengjie.common.persistence;

import java.io.Serializable;
/**
 * 在rpc时，保存动态属性的类型和值
 * @author liangfeng
 *
 */
public class DynaPropertyBeanForRpc implements Serializable {
    private String fieldName;
    private Class fieldClassType;
    private Object fieldValue;
	public String getFieldName() {
		return fieldName;
	}
	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}
	public Class getFieldClassType() {
		return fieldClassType;
	}
	public void setFieldClassType(Class fieldClassType) {
		this.fieldClassType = fieldClassType;
	}
	public Object getFieldValue() {
		return fieldValue;
	}
	public void setFieldValue(Object fieldValue) {
		this.fieldValue = fieldValue;
	}
    
}
