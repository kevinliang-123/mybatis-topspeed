package com.tengjie.common.utils;

import java.math.BigDecimal;

public class BigdecimalUtils {
	// 默认除法运算精度
	private static final int DEF_DIV_SCALE = 10;

	// 这个类不能实例化
	private BigdecimalUtils() {
		
	}

	/**
	 * 提供精确的加法运算。
	 * 
	 * @param v1
	 *            被加数
	 * @param v2
	 *            加数
	 * @return 两个参数的和
	 */

	public static BigDecimal add(BigDecimal v1, BigDecimal v2) {
		return v1.add(v2);
	}
	
	/**
	 * 提供精确的减法运算。
	 * 
	 * @param v1
	 *            被减数
	 * @param v2
	 *            减数
	 * @return 两个参数的差
	 */

	public static BigDecimal sub(BigDecimal v1, BigDecimal v2) {
		return v1.subtract(v2);
	}

	/**
	 * 提供精确的乘法运算。
	 * 
	 * @param v1
	 *            被乘数
	 * @param v2
	 *            乘数
	 * @return 两个参数的积
	 */

	public static BigDecimal mul(BigDecimal v1, BigDecimal v2) {
		return v1.multiply(v2);
	}

	/**
	 * 提供（相对）精确的除法运算。当发生除不尽的情况时，由scale参数指 定精度，以后的数字四舍五入。
	 * 
	 * @param v1
	 *            被除数
	 * @param v2
	 *            除数
	 * @param scale
	 *            表示表示需要精确到小数点以后几位。
	 * @return 两个参数的商
	 */

	public static BigDecimal div(BigDecimal v1, BigDecimal v2, int scale) {
		return v1.divide(v2, scale, BigDecimal.ROUND_HALF_UP);
	}

	/**
	 * 提供精确的小数位四舍五入处理。
	 * 
	 * @param v
	 *            需要四舍五入的数字
	 * @param scale
	 *            小数点后保留几位
	 * @return 四舍五入后的结果
	 */

	public static BigDecimal round(BigDecimal v, int scale) {
		BigDecimal one = new BigDecimal("1");
		return v.divide(one, scale, BigDecimal.ROUND_HALF_UP);
	}
	
	/**
	 * 提供精确的向下取整
	 * 
	 * @param v
	 *            需要向下取整的数字
	 * @param scale
	 *            小数点后保留几位
	 * @return 向下取整后的结果
	 */

	public static BigDecimal rounDown(BigDecimal v, int scale) {
		BigDecimal one = new BigDecimal("1");
		return v.divide(one, scale, BigDecimal.ROUND_DOWN);
	}
	
	/**
	 * 提供精确的向上取整
	 * 
	 * @param v
	 *            需要向上取整的数字
	 * @param scale
	 *            小数点后保留几位
	 * @return 向上取整的结果
	 */

	public static BigDecimal rounUp(BigDecimal v, int scale) {
		BigDecimal one = new BigDecimal("1");
		return v.divide(one, scale, BigDecimal.ROUND_UP);
	}
	
	
	/**
	 * 提供精确的计算结果向下取整
	 * 
	 * @param v1
	 *            被除数
	 * @param v2
	 *            除数
	 * @param scale
	 *            小数点后保留几位
	 * @return 向下取整后的结果
	 */

	public static BigDecimal rounDown(BigDecimal v1,BigDecimal v2,int scale) {
		return v1.divide(v2, scale, BigDecimal.ROUND_DOWN);
	}
	
	/**
	 * 提供精确的向上取整
	 * 
	 * @param v1
	 *            被除数
	 * @param v2
	 *            除数
	 * @param scale
	 *            小数点后保留几位
	 * @return 向上取整的结果
	 */
	public static BigDecimal rounUp(BigDecimal v1,BigDecimal v2,int scale) {
		return v1.divide(v2, scale, BigDecimal.ROUND_UP);
	}
	
	public static void main(String[] args) {
		
		BigDecimal v = new BigDecimal("1");
		
		BigDecimal one = new BigDecimal("1");
		BigDecimal s = v.divide(one, 10, BigDecimal.ROUND_HALF_UP);
		
		System.out.println(s);
		BigDecimal q = s.multiply(new BigDecimal("2"));
		System.out.println(q);
		
		BigDecimal two = new BigDecimal("1");
		BigDecimal s1 = q.divide(two, 10, BigDecimal.ROUND_HALF_UP);
		System.out.println(s1);
		BigDecimal s2 = s1.multiply(new BigDecimal("1"));
		System.out.println(s2);
		BigDecimal s3 = s2.divide(new BigDecimal("1"), 1, BigDecimal.ROUND_DOWN);
		System.out.println(s3);
	}

}
