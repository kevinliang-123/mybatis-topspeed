package com.tengjie.common.persistence;

import java.io.Serializable;

public class WhereValueBean extends ConditionBean  implements Serializable{
	private static final long serialVersionUID = 1L;
	private  String whereValueFromField;//有时设置了whereValue为null，表示值从bean中按到对应字段的值，但是有时所需的字段值并不是对应字段的值，而是指定的某个字段的值
	//，因此这里就是当为null的时候，看看这个有没有，优先用这个字段获得值。这个主要是queryCond的时候smartJoin指定了关联字段（一个主表的多个字段关联统一张表），smart的为了自动化，需要指定putwhere的字段值来源于某个重新定义的别名，以便区分开来
	public WhereValueBean(String sign) {// is null等情况，是没有whereValue
		super();
		this.sign=sign;
	}
	
	public WhereValueBean(String sign,Object whereValue) {
		super();
		this.whereValue = whereValue;
		this.sign=sign;
	}
	public WhereValueBean(String sign,Object whereValue,Object whereValue1) {
		super();
		this.whereValue = whereValue;
		this.whereValue1 = whereValue1;
		this.sign=sign;
	}
    private Object whereValue;
    private Object whereValue1;
	public Object getWhereValue() {
		return whereValue;
	}

	public void setWhereValue(Object whereValue) {
		this.whereValue = whereValue;
	}
	public Object getWhereValue1() {
		return whereValue1;
	}
	public void setWhereValue1(Object whereValue1) {
		this.whereValue1 = whereValue1;
	}

	public String getWhereValueFromField() {
		return whereValueFromField;
	}

	public void setWhereValueFromField(String whereValueFromField) {
		this.whereValueFromField = whereValueFromField;
	}

	

}
