package com.tengjie.common.utils;

import java.security.MessageDigest;

import sun.misc.BASE64Encoder;

/**
 * 采用MD5加密解密
 * @author tfq
 * @datetime 2011-10-13
 */
public class MD5Util {
	
	
	
	/**
	* @description: MD5 UTF8
	* @author: hanshichao
	* @date: 2017年8月25日 上午11:37:50
	*/
	public static String parseStrToMd5L32(String str){
		String reStr = null;
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			byte[] bytes = md5.digest(str.getBytes("utf-8"));
			StringBuffer stringBuffer = new StringBuffer();
			for (byte b : bytes){
				int bt = b&0xff;
				if (bt < 16){
					stringBuffer.append(0);
				} 
				stringBuffer.append(Integer.toHexString(bt));
			}
			reStr = stringBuffer.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return reStr;
	}
	
	/**
	* @description: MD5 GBK
	* @author: hanshichao
	* @date: 2017年8月25日 上午11:37:50
	*/
	public static String parseStrToMd5L32GBK(String str){
		String reStr = null;
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			byte[] bytes = md5.digest(str.getBytes("gbk"));
			StringBuffer stringBuffer = new StringBuffer();
			for (byte b : bytes){
				int bt = b&0xff;
				if (bt < 16){
					stringBuffer.append(0);
				} 
				stringBuffer.append(Integer.toHexString(bt));
			}
			reStr = stringBuffer.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return reStr;
	}
	
	/**
	* @description: MD5 GB3212
	* @author: hanshichao
	* @date: 2017年8月25日 上午11:37:50
	*/
	public static String parseStrToMd5L32GB2312(String str){
		String reStr = null;
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			byte[] bytes = md5.digest(str.getBytes("gb2312"));
			StringBuffer stringBuffer = new StringBuffer();
			for (byte b : bytes){
				int bt = b&0xff;
				if (bt < 16){
					stringBuffer.append(0);
				} 
				stringBuffer.append(Integer.toHexString(bt));
			}
			reStr = stringBuffer.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return reStr;
	}
	
	
	/***
	 * MD5加码 生成32位md5码
	 */
	public static String string2MD5(String inStr){
		MessageDigest md5 = null;
		try{
			md5 = MessageDigest.getInstance("MD5");
		}catch (Exception e){
			System.out.println(e.toString());
			e.printStackTrace();
			return "";
		}
		char[] charArray = inStr.toCharArray();
		byte[] byteArray = new byte[charArray.length];

		for (int i = 0; i < charArray.length; i++)
			byteArray[i] = (byte) charArray[i];
		byte[] md5Bytes = md5.digest(byteArray);
		StringBuffer hexValue = new StringBuffer();
		for (int i = 0; i < md5Bytes.length; i++){
			int val = ((int) md5Bytes[i]) & 0xff;
			if (val < 16)
				hexValue.append("0");
			hexValue.append(Integer.toHexString(val));
		}
		return hexValue.toString();

	}

	/**
	 * 加密解密算法 执行一次加密，两次解密
	 */ 
	public static String convertMD5(String inStr){

		char[] a = inStr.toCharArray();
		for (int i = 0; i < a.length; i++){
			a[i] = (char) (a[i] ^ 't');
		}
		String s = new String(a);
		return s;

	}
	
	public static String convertMD5String(String source){

		MessageDigest md5;
		try {
			md5 = MessageDigest.getInstance("MD5");
			byte[] bts = md5.digest(source.getBytes("UTF-8"));
			BASE64Encoder encoder = new BASE64Encoder();
			String chargeSign = encoder.encode(bts);
			return chargeSign;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return "";

	}
	
	
	
	

	
	

	// 测试主函数
	public static void main(String args[]) {
		String s = new String("YRsNHuXagLo=wanda");
		System.out.println("原始：" + s);
		System.out.println("MD5后：" + string2MD5("<BusiData><CreateTime>1454308038574</CreateTime><ChargePhoneNum>13805331100</ChargePhoneNum><UserID>5338027676052</UserID><ProductCode>108711</ProductCode><ChargeNum>1</ChargeNum></BusiData>aa94e32272d24178abbdac3755e5aac1"));
		System.out.println("加密的：" + convertMD5(s));
		System.out.println("解密的：" + convertMD5(convertMD5(s)));

	}
}
