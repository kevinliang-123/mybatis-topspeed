package com.tengjie.common.utils;

import com.fasterxml.jackson.databind.deser.Deserializers.Base;
import com.tengjie.common.persistence.TjBaseEntity;

/**
 * sql的一些工具类
 * @author admin
 *
 */
public class SqlUtils {
	/**
	 * 获得年格式化sql
	 * @param fieldName
	 */
	public static String findYearFormatSql(String fieldName) {
		fieldName=StringUtils.toUnderScoreCase(fieldName);
		String sql=" DATE_FORMAT("+fieldName+",'%Y') ";
		return sql;
	}
	/**
	 * 获得季格式化sql
	 * @param fieldName
	 */
	public static String findQuarterFormatSql(String fieldName) {
		fieldName=StringUtils.toUnderScoreCase(fieldName);
		String sql=" concat(date_format("+fieldName+", '%Y'),FLOOR((date_format("+fieldName+", '%m')+2)/3)) ";
		return sql;
	}
	/**
	 * 获得月格式化sql
	 * @param fieldName
	 */
	public static String findMonthFormatSql(String fieldName) {
		fieldName=StringUtils.toUnderScoreCase(fieldName);
		String sql=" DATE_FORMAT("+fieldName+",'%Y-%m') ";
		return sql;
	}
	
	/**
	 * 构建调用递归树查询函数的select sql语句，首先需要在数据库构建好函数，这里只是调用而已。
	 * 注意：会把拼接好的sql方到anySql中
	 * @param functionName：要调用的函数名
	 * @param entity：实体，实体中可以包含了与其他表关联的其他语句，依然遵循原来的规则
	 * @param paramValue：传入函数的参数值
	 * @param ifContianInObj：返回的列表中是否包含传入参数对应的值，true包含，false不包含
	 * @return
	 */
	public static void buildMysqlTreeFunctionSelectSql(String functionName,TjBaseEntity entity,String paramValue,boolean ifContianInObj) {
		
		String anySql="select a.* from tb_comment_record a where FIND_IN_SET(a.id, find_comment_record_tree_function("+paramValue+")) ";
		entity.setAnySql(anySql);
		entity.putOrderBy(" find_in_set(a.id, find_comment_record_tree_function("+paramValue+"))", "asc");
		if(!ifContianInObj) {
			entity.appendWhereCondSql(" a.id !="+paramValue);
		}
	}
   /**
    * 创建数据结构的存储过程代码，注意：直接创建函数会出现This function has none of DETERMINISTIC, NO SQL, or READS SQL DATA in its declaration and binary
      mysql的设置默认是不允许创建函数
∂解决办法1:
执行：
SET GLOBAL log_bin_trust_function_creators = 1;
不过 重启了 就失效了
注意： 有主从复制的时候 从机必须要设置  不然会导致主从同步失败
解决办法2：
在my.cnf里面设置
log-bin-trust-function-creators=1
不过这个需要重启服务
调用函数：select find_comment_record_tree_function(1)); 1为id
select * from tb_comment_record where FIND_IN_SET(id, find_comment_record_tree_function(9)) order by FIND_IN_SET(id, find_comment_record_tree_function(9))
    */
//	DROP FUNCTION IF EXISTS find_comment_record_tree_function;
//	CREATE  FUNCTION `find_comment_record_tree_function` (rootId VARCHAR(32)<备注：如果是varchar必须跟后面的位数>) RETURNS VARCHAR(4000) 
//	    BEGIN 
//	        DECLARE sTemp VARCHAR(4000); 
//	        DECLARE sTempChd VARCHAR(4000); 
//	 
//	        SET sTemp = '$'; 
//	        SET sTempChd =cast(rootId as CHAR); 
//	 
//	        WHILE sTempChd is not null DO 
//	            SET sTemp = concat(sTemp,',',sTempChd); 
//	            SELECT group_concat(id) INTO sTempChd FROM tb_comment_record where FIND_IN_SET(parent_id,sTempChd)>0; 
//	        END WHILE; 
//	        RETURN sTemp; 
//	END  ;
}
