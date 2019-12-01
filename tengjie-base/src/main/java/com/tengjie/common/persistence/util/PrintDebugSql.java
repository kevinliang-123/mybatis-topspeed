package com.tengjie.common.persistence.util;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

import org.apache.commons.collections.CollectionUtils;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandlerRegistry;

public class PrintDebugSql {
	// 封装了一下sql语句，使得结果返回完整xml路径下的sql语句节点id + sql语句
	public static String getSql(Configuration configuration, BoundSql boundSql,
			String sqlId) {
		String sql = showSql(configuration, boundSql);
		return sql;
	}

	/**
	 * 如果参数是String，则添加单引号， 如果是日期，则转换为时间格式器并加单引号； 对参数是null和不是null的情况作了处理
	 */
	private static String getParameterValue(Object obj) {
		String value = null;
		if (obj instanceof String) {
			value = "'" + obj.toString() + "'";
		} else if (obj instanceof Date) {
			DateFormat formatter = DateFormat.getDateTimeInstance(
					DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.CHINA);
			value = "'" + formatter.format(obj) + "'";
		} else {
			if (obj != null) {
				value = obj.toString();
			} else {
				value = "";
			}

		}
		return value;
	}

	/**
	 * 进行？的替换
	 * 
	 * @param configuration
	 * @param boundSql
	 * @return
	 */
	public static String showSql(Configuration configuration, BoundSql boundSql) {
		Object parameterObject = boundSql.getParameterObject(); // 获取参数
		List<ParameterMapping> parameterMappings = boundSql
				.getParameterMappings();
		String sql = boundSql.getSql().replaceAll("[\\s]+", " "); // sql语句中多个空格都用一个空格代替
		if (CollectionUtils.isNotEmpty(parameterMappings)
				&& parameterObject != null) {
			TypeHandlerRegistry typeHandlerRegistry = configuration
					.getTypeHandlerRegistry(); // 获取类型处理器注册器，类型处理器的功能是进行java类型和数据库类型的转换<br>　　　　　　　//
												// 如果根据parameterObject.getClass(）可以找到对应的类型，则替换
			if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
				sql = sql.replaceFirst("\\?", Matcher
						.quoteReplacement(getParameterValue(parameterObject)));

			} else {
				MetaObject metaObject = configuration
						.newMetaObject(parameterObject);// MetaObject主要是封装了originalObject对象，提供了get和set的方法用于获取和设置originalObject的属性值,主要支持对JavaBean、Collection、Map三种类型对象的操作
				for (ParameterMapping parameterMapping : parameterMappings) {
					String propertyName = parameterMapping.getProperty();
					if (metaObject.hasGetter(propertyName)) {
						Object obj = metaObject.getValue(propertyName);
						sql = sql.replaceFirst("\\?", Matcher
								.quoteReplacement(getParameterValue(obj)));
					} else if (boundSql.hasAdditionalParameter(propertyName)) {
						Object obj = boundSql
								.getAdditionalParameter(propertyName); // 该分支是动态sql
						sql = sql.replaceFirst("\\?", Matcher
								.quoteReplacement(getParameterValue(obj)));
					} else {
						sql = sql.replaceFirst("\\?", "*！！！缺失值，请确认页面或者接口是否传递了该值");
					}// 打印出缺失，提醒该参数缺失并防止错位
				}
			}
		}
		return sql;
	}
}
