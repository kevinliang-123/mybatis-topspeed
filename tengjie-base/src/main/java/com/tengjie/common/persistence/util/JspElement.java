package com.tengjie.common.persistence.util;

import java.lang.reflect.Field;
import com.tengjie.common.utils.StringUtils;


//<spring:eval expression="new test.DateBean()" var="dateBean" scope="request" />
//<div>Date format: <spring:eval expression="dateBean.date" />
public abstract class JspElement {
	
    /**
     * 获得驼峰后的带有tb的名字，如tbTeacherInfo，但是对于user.java和老的一些程序，可能是没有_TB_TABLE_NAME_这个字段的，不能反射调用这个字段来获取
     */
    public static String findWithCamelTableName(Class tableClass,boolean iffirstup) {
    	
    	String simpleName=StringUtils.findClassSimpleName(tableClass, iffirstup);
    
    	
    	if(simpleName.toLowerCase().equals("user")) {//还有一些没有tb开头的表，这里都要加上
    		return "sysUser";
    	}else if(simpleName.toLowerCase().equals("appuser")) {//还有一些没有tb开头的表，这里都要加上
    		Field ff=null;
    		try {
    			ff=tableClass.getDeclaredField("_TB_TABLE_NAME_");
        	}catch(Exception e) {};
        	if(ff!=null) {
        		return "tbAppUser";
        	}
    		return "tbUser";
    	}else {
    		String tb=iffirstup?"Tb":"tb";
    		return tb+StringUtils.firstToUpper(simpleName);
    	}
    }
    /**
     * 根据表类或者表名，默认是获得首字小写的，iffirstup如果为true则表示要大写的
     * @param tableClass
     * @param iffirstup
     * @return
     */
    public static  String getEntityNameByClass(Class tableClass,boolean ...iffirstup){
    	String entityName=tableClass.getSimpleName();
    	//如果是带有动态属性的bean，名字会不一样，是这样的teacherInfo$$BeanGeneratorByCGLIB$$33b233a6，这样无法覆盖原来的
    	if(entityName.contains("$$"))entityName=entityName.substring(0, entityName.indexOf("$$"));
    	boolean firstup=iffirstup.length>0?iffirstup[0]:false;
    	if(!firstup){
    		entityName=StringUtils.firstToLower(entityName);
    	}
    	return entityName;
    }
   
}
