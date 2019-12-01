package com.tengjie.common.utils;

import org.springframework.util.SerializationUtils;
 

public class MySerializationUtils extends SerializationUtils {

 
	/**
	 * 将字节数组直接转成想要的对象
	 * @param T  想转换的对象有类
	 * @param obj 对象序列化数组
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T deserialezeNative(Class<T> T, byte[] obj) {
 
		T dp = (T) deserialize(obj);
		 
		return dp;
	}
	
	/**
	 * 将某对象序列化
	 * @param obj
	 * @return 
	 */	 
	public static byte[] serializeByteArray(Object obj) {
 
		byte[] b=serialize(obj);
		
		return b;
	}
	
	
	
	
	
	
	
	public static void main(String []args){
		
//		 ErrorCode error=new ErrorCode();
//		 error.setChannelErrorCode("你好啊，我的对象有中的属性111111111");
//		 byte[] b=SerializationUtils.serialize(error);
//		 
//		 ErrorCode nativea =MySerializationUtils.deserialezeNative(ErrorCode.class, b);
//		 System.out.println(nativea.getChannelErrorCode());  
	}

}
