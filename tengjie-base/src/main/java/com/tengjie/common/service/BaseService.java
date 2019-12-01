package com.tengjie.common.service;


import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import com.tengjie.common.config.Global;
import com.tengjie.common.persistence.TjBaseEntity;
import com.tengjie.common.persistence.JoinTableBean;
import com.tengjie.common.utils.Reflections;
import com.tengjie.common.utils.StringUtils;


/**
 * Service基类
 * @author
 * @version 2014-05-16
 */
@Transactional(readOnly = true)
public abstract class BaseService {
	
	/**
	 * 日志对象
	 */
	protected Logger logger = LoggerFactory.getLogger(getClass());

	
	/**
	 * 快速获得关联表bean，此方法不支持需要主表别名情况 
	 * @param tableName 关联表名，注意加tb
	 * @param mainRelFieldName 主表关联字段
	 * @param childRelFieldName 字表关联字段
	 * @param selectMap 字表select的字段列表
	 * @return
	 */
	public static JoinTableBean findJoinTableBean(String tableName,String mainRelFieldName,String childRelFieldName,Map<String,String> selectMap){
		 return findJoinTableBean(null,tableName,mainRelFieldName, childRelFieldName, selectMap);
	}
	/**
	 * 快速获得关联表bean，此方法支持需要主表别名情况 
	 * @param mainTableAlias 主表的别名
	 * @param tableName 关联表名，注意加tb
	 * @param mainRelFieldName 主表关联字段
	 * @param childRelFieldName 字表关联字段
	 * @param selectMap 字表select的字段列表
	 * @return
	 */
	public static JoinTableBean findJoinTableBean(String mainTableAlias,String tableName,String mainRelFieldName,String childRelFieldName,Map<String,String> selectMap){
		 JoinTableBean jtb=new JoinTableBean();
		 if(StringUtils.isNotEmpty(mainTableAlias))
		 jtb.setMainTableName(mainTableAlias);
		 jtb.setTableName(tableName);
		 jtb.putOnCond(mainRelFieldName, childRelFieldName);
		 if(selectMap!=null)
		 for (Map.Entry<String, String> entry : selectMap.entrySet()) {
			 jtb.putSelect(entry.getKey(), StringUtils.isEmpty(entry.getValue())?entry.getKey():entry.getValue());
		 }
		 return jtb;
	}
	
}
