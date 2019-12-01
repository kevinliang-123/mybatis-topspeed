package com.tengjie.common.utils;


import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.tengjie.common.config.Global;

/** 
 * 3DES加密 
 *  
 */  
public class ThreeDES {  

	private static final String Algorithm = "DESede"; // 定义 加密算法,可用DES,DESede,Blowfish  


	// 加密入口,外部调用此方法
	public static String encryptMode(String pass, String str) {
		try {
			byte[] resultByte = encryptMode(pass.getBytes(), str.getBytes());
			// 为了防止解密时报javax.crypto.IllegalBlockSizeException: Input length must be multiple of 8 when decrypting with padded cipher异常，  
			// 不能把加密后的字节数组直接转换成字符串
			return Base64Utils.encode(resultByte);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	// 解密入口,外部调用此方法
	public static String decryptMode(String pass, String str) {
		try {
			byte[] resultByte = decryptMode(pass.getBytes(), Base64Utils.decode(str.toCharArray()));
			return new String(resultByte,"UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/** 
	 * 加密方法 
	 *  
	 * @param keybyte 
	 *            加密密钥，长度为24字节 
	 * @param src 
	 *            被加密的数据缓冲区（源） 
	 * @return 
	 * @author SHANHY 
	 * @date 2015-8-18 
	 */  
	public static byte[] encryptMode(byte[] keybyte, byte[] src) {  
		try {  
			// 生成密钥  
			SecretKey deskey = new SecretKeySpec(keybyte, Algorithm);  

			// 加密  
			Cipher c1 = Cipher.getInstance(Algorithm);  
			c1.init(Cipher.ENCRYPT_MODE, deskey);  
			return c1.doFinal(src);  
		} catch (java.security.NoSuchAlgorithmException e1) {  
			e1.printStackTrace();  
		} catch (javax.crypto.NoSuchPaddingException e2) {  
			e2.printStackTrace();  
		} catch (java.lang.Exception e3) {  
			e3.printStackTrace();  
		}  
		return null;  
	}  

	/** 
	 * 解密 
	 *  
	 * @param keybyte 
	 *            加密密钥，长度为24字节 
	 * @param src 
	 *            加密后的缓冲区 
	 * @return 
	 * @author SHANHY 
	 * @date 2015-8-18 
	 */  
	public static byte[] decryptMode(byte[] keybyte, byte[] src) {  
		try {  
			// 生成密钥  
			SecretKey deskey = new SecretKeySpec(keybyte, Algorithm);  

			// 解密  
			Cipher c1 = Cipher.getInstance(Algorithm);  
			c1.init(Cipher.DECRYPT_MODE, deskey);  
			return c1.doFinal(src);  
		} catch (java.security.NoSuchAlgorithmException e1) {  
			e1.printStackTrace();  
		} catch (javax.crypto.NoSuchPaddingException e2) {  
			e2.printStackTrace();  
		} catch (java.lang.Exception e3) {  
			e3.printStackTrace();  
		}  
		return null;  
	}  

	/** 
	 * 转换成十六进制字符串 
	 *  
	 * @param b 
	 * @return 
	 * @author SHANHY 
	 * @date 2015-8-18 
	 */  
	public static String byte2hex(byte[] b) {  
		String hs = "";  
		String stmp = "";  

		for (int n = 0; n < b.length; n++) {  
			stmp = (java.lang.Integer.toHexString(b[n] & 0XFF));  
			if (stmp.length() == 1)  
				hs = hs + "0" + stmp;  
			else  
				hs = hs + stmp;  
			if (n < b.length - 1)  
				hs = hs + ":";  
		}  
		return hs.toUpperCase();  
	}  

	/**
	 * 测试以及调用
	 * @Description： TODO
	 * @author: 高泽 
	 * @since: 2017年8月30日 下午5:36:14
	 */
	public static void main(String[] args) {  

		String secret = "33WJ9AGXogm2TOiMKy^LREg*";// Global.getConfig("trdDesKey");
		String szSrc2 = "1005036";
		String result = encryptMode(secret, szSrc2);
		System.out.println("加密后的结果:"+result);
		String result2 = decryptMode(secret, result);
		System.out.println("解密后的结果:"+result2);
	}  
}  