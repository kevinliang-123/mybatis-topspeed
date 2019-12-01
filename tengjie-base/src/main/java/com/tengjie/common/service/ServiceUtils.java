package com.tengjie.common.service;
import java.util.List;

import com.tengjie.common.persistence.TjBaseEntity;
import com.tengjie.common.utils.Reflections;
import com.tengjie.common.utils.SpringContextHolder;
import com.tengjie.common.utils.StringUtils;

public class ServiceUtils {
	/**
	 * 此方法创建的初衷是由于很两个表关联，比如B表中存储了A表中的id，查询出B表时，也要根据这个ID获得A表记录，请取得A表记录总的某个字段的值
	 * 注意，传入的queryPropName和queryPropValue取得的结果集无论是多少条，只返回第一条对应的resultPropName所对应的值
	 * 示例：实际sql为：select a.resultPropName from tableName  a where a.queryPropName=queryPropValue;
	 * @param tableName:要查询的表名，实际为实体bean名，如projectPlan
	 * @param queryPropName 要查询的属性名
	 * @param queryPropValue 要查询的属性名对应的值
	 * @param resultPropName 要返回结果的属性
	 * @return//
	 */
    public static Object callTableByCondPorp(String tableName,String queryPropName,Object queryPropValue,String resultPropName){
    	Object resultPropValue=null; 

    	TjBaseEntity de=callTableByCondBean(tableName,queryPropName,queryPropValue);
    		if(de!=null){ 
    			resultPropValue=Reflections.getFieldValue(de,resultPropName);
    		}
	
    	return resultPropValue;
    }
    /**
     * 此方法与callTableByCondPorp区别是，获得属性列表，传入的值应该是a,b,c,最后拼接成in('a','b','c',)
	 * @return//
	 */
    public static Object callTableByCondPorpIn(String tableName,String queryPropName,Object queryPropValue,String resultPropName){
    	Object resultPropValue=null; 

    	TjBaseEntity de=callTableByCondBean(tableName,queryPropName,queryPropValue);
    		if(de!=null){ 
    			resultPropValue=Reflections.getFieldValue(de,resultPropName);
    		}
	
    	return resultPropValue;
    }
    /**
	 * 此方法创建的初衷是由于很两个表关联，比如B表中存储了A表中的id，查询出B表时，也要根据这个ID获得A表记录，请取得A表记录总的某个字段的值
	 * 注意，传入的queryPropName和queryPropValue取得的结果集无论是多少条，只返回第一条记录
	 * 示例：实际sql为：select * from tableName  a where a.queryPropName=queryPropValue;
	 * @param tableName:要查询的表名，实际为实体bean名，如projectPlan
	 * @param queryPropName 要查询的属性名
	 * @param queryPropValue 要查询的属性名对应的值
	 * @return DataEntity
	 */
    public static TjBaseEntity callTableByCondBean(String tableName,String queryPropName,Object queryPropValue){
    	TjBaseEntity oneRecord=null;

    		//tableName=tableName.substring(0, 1).toUpperCase() + tableName.substring(1);
    	    if(queryPropValue==null||StringUtils.isEmpty(queryPropValue+""))return null;
    		CrudService cService = (CrudService) SpringContextHolder.getBean(tableName+"Service");
    		oneRecord=cService.findUniqueByProperty(queryPropName, queryPropValue);
    
    	return oneRecord;
    }
    /**
   	 * 此方法创建的初衷是由于很两个表关联，比如B表中存储了A表中的id，查询出B表时，也要根据这个ID获得A表记录，请取得A表记录总的某个字段的值
   	 * 注意，传入的queryPropName和queryPropValue取得的结果集无论是多少条，只返回第一条记录
   	 * 示例：实际sql为：select * from tableName  a where a.queryPropName=queryPropValue;
   	 * @param tableName:要查询的表名，实际为实体bean名，如projectPlan
   	 * @param queryPropName 要查询的属性名
   	 * @param queryPropValue 要查询的属性名对应的值
   	 * @return DataEntity
   	 */
       public static List<TjBaseEntity> callTableByCondList(String tableName,String queryPropName,Object queryPropValue){
    	   List<TjBaseEntity> record=null;

       		//tableName=tableName.substring(0, 1).toUpperCase() + tableName.substring(1);
       	    if(queryPropValue==null||StringUtils.isEmpty(queryPropValue+""))return null;
       		CrudService cService = (CrudService) SpringContextHolder.getBean(tableName+"Service");
       		record=cService.findListByProperty(queryPropName, queryPropValue);
       	return record;
       }
       
       /**
      	 * 此方法无任何条件，直接查询全部
      	 * @param tableName:要查询的表名，实际为实体bean名，如projectPlan
      	 * @return List<BaseEntity>
      	 */
          public static List<TjBaseEntity> callTableAllByCondList(String tableName){
       	        List<TjBaseEntity> record=null;
          		CrudService cService = (CrudService) SpringContextHolder.getBean(tableName+"Service");
          		//DataEntity be=SpringContextHolder.getBean(tableName);
          		record=cService.findListByProperty("1", 1);
          	return record;
          }
}
