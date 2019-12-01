package com.tengjie.common.persistence;

import java.io.Serializable;

public class ConditionBean implements Serializable  {
	 private static final long serialVersionUID = 1L;
	public static  final String SIGN_EQUAL = " = ";
	public static  final String SIGN_NOT_EQUAL = " != ";
	public static  final String SIGN_BETWEEN = " between ";
	public static  final String SIGN_BIG= " > ";
	public static  final String SIGN_BIG_EQUAL = " >= ";
	public static  final String SIGN_SMALL = " < ";
	public static  final String SIGN_SMALL_EQUAL = " <= ";
	public static  final String SIGN_IS_NULL = " is null ";
	public static  final String SIGN_IS_NOT_NULL = " is not null ";
	public static  final String   SIGN_IN = " in ";
	public static  final String  SIGN_LIKE = " like ";

    protected String  sign=SIGN_EQUAL;//运算符,可不写，默认是SIGN_EQUAL

	public String getSign() {
		return sign;
	}

	public void setSign(String sign) {
		this.sign = sign;
	}
    
    
}
