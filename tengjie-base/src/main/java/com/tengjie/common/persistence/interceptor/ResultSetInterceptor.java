package com.tengjie.common.persistence.interceptor;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.HtmlUtils;

import com.tengjie.common.config.Global;
import com.tengjie.common.persistence.TjBaseEntity;
import com.tengjie.common.persistence.DataEntityFieldCallBackBean;
import com.tengjie.common.persistence.JoinTableBean;
import com.tengjie.common.persistence.util.ReturnTypeChangeUtil;
import com.tengjie.common.service.ServiceUtils;
import com.tengjie.common.utils.EmojiUtil;
import com.tengjie.common.utils.MyBeanUtils;
import com.tengjie.common.utils.NewInstanceUtil;
import com.tengjie.common.utils.Reflections;
import com.tengjie.common.utils.SpringContextHolder;
import com.tengjie.common.utils.StringUtils;
import com.tengjie.common.utils.TjNewMap;

@Intercepts({ @Signature(type = ResultSetHandler.class, method = "handleResultSets", args = { Statement.class }) })
public class ResultSetInterceptor extends BaseInterceptor {
	private static Logger logger = LoggerFactory.getLogger(ResultSetInterceptor.class);
	private static Map<String,String> str2EmojiFieldsMap=null;
	boolean ifAllStr2EmojiFields=true;//所有表情是否过滤
	private  Map<String,String> getStr2EmojiFieldsMap(){
		if(str2EmojiFieldsMap==null){
			str2EmojiFieldsMap=new HashMap();
			String temp=Global.getConfig("str2EmojiFields") ;
			if(StringUtils.isNotEmpty(temp)){
				String[] fieldsArray=temp.split(",");
				if(fieldsArray!=null&&fieldsArray.length>0){
					for(String s:fieldsArray){
						str2EmojiFieldsMap.put(s, s);
					}
				}
			}
		}
		return str2EmojiFieldsMap;
	}
 
	@Override
    public Object intercept(Invocation invocation) throws Throwable {
    	Object target = invocation.getTarget();
		ResultSetHandler  resultSetHandler = (ResultSetHandler) target;  
		ParameterHandler parameterHandler=(ParameterHandler)Reflections.getFieldValue(resultSetHandler,"parameterHandler");
		Object parameter =parameterHandler.getParameterObject() ;
		  if(parameter==null)return invocation.proceed();
		//还有加一个，只对查询结果进行处理，其他的不处理。
        Object[] args = invocation.getArgs();
   
    
        String returnRsTypeName=findMappersResultTypeName(invocation);//stmt.getResultSetType();  1是bean 2是map 3是其他
        String returnRsType=findMappersResultType(returnRsTypeName);//stmt.getResultSetType();  1是bean 2是map 3是其他
      
      if(!"1".equals(returnRsType))return invocation.proceed();//如果mapper查询返回的是map或其他，新增的字段属性会自动加到map里面，不需要处理
        boolean ifReturnMap=false;//是否返回修改为map类型
        if("1".equals(returnRsType)){
        	if((parameter instanceof TjBaseEntity)&&"2".equals(((TjBaseEntity)parameter).getResultType())){
                changeMapperStatement(invocation, 	ReturnTypeChangeUtil.newMappedStatement(getMapperStatement(invocation), java.util.Map.class));
            	ifReturnMap=true;
        	}
        	
        }
        if(parameter instanceof Map) {//ruoyi项目的分页信息，或将查询实体转换为bean，因此需要再还原回来，或者使用我们的分页查询
        	Object newparam=Class.forName(returnRsTypeName).newInstance();
        	parameter=MyBeanUtils.copyMap2BeanNotNull(newparam, (Map)parameter,true);
   
        }
        if(parameter instanceof TjBaseEntity){     	
            // 获取到当前的Statement
            Statement stmt =  (Statement) args[0];
            // 通过Statement获得当前结果集
            ResultSet resultSet = stmt.getResultSet();
        	List jtbs=((TjBaseEntity)parameter).getJoinTableBeanList();
        	Map<String,Class>addFieldMap=findAddFieldMap(jtbs,((TjBaseEntity)parameter).getMainTableAppendPropMap());
        	addFieldMap.putAll(findGroupbyFieldMap((TjBaseEntity)parameter));
        	addFieldMap.putAll(findAddSelectFieldMap((TjBaseEntity)parameter));
        	addFieldMap.putAll(findAnyQueryFieldMap(resultSet,((TjBaseEntity)parameter)));
        	if((!ifReturnMap)&&!ifAllStr2EmojiFields&&((TjBaseEntity)parameter).getFieldCallBackMap().size()<1&&((TjBaseEntity)parameter).getRowBeanCallBack()==null)return invocation.proceed();
           
        	if(!ifReturnMap){
            	 ResultSetMetaData metadata = resultSet.getMetaData();
             	 int count = metadata.getColumnCount(); 
            	 for (int i = 1; i <= count; i++) {//重新匹配新增字段的java类型,主要是jointable部分
            		 String columnLabel= metadata.getColumnLabel(i);
            		 if(addFieldMap.containsKey(columnLabel)){
                             String className=metadata.getColumnClassName(i);
            				 if(className.equalsIgnoreCase("java.sql.Timestamp")){//时间戳自动转成日期
            					 addFieldMap.put(columnLabel, java.util.Date.class);
            				 }else{
            					 addFieldMap.put(columnLabel, Class.forName(className));
            				 }
            		 }
            	 }
            	 List<TjBaseEntity> all=new ArrayList();
            	 dealBeanResultSet(all,resultSet,returnRsTypeName,addFieldMap,parameter);
            	 return all;
             }else{
            	 List<Map<String,Object>> alldata=new ArrayList();
            	 dealMapResultSet(alldata,resultSet,returnRsTypeName, ((TjBaseEntity)parameter).getMainTableAppendPropMap(),parameter);
            	 return alldata;
             }
//        	// List<BaseEntity> all=(List<BaseEntity>) invocation.proceed();
//             for(int i=0;i<all.size();i++){
//            	 BaseEntity be=all.get(i);
//            	 be= (BaseEntity) be.initDynaMap(addFieldMap);//加入对应新增的属性字段
//            	 Map<String,Object> rowdata= alldata.get(i);
//            	 for (Map.Entry<String, Class> entry :addFieldMap.entrySet()) { 
//            		 be.setDynaValue(entry.getKey(), rowdata.get(entry.getKey()));
//            	 }
//            	
//             }

    	}else{
    		return invocation.proceed();
    	}
        // handleResultSets返回结果一定是一个List
        // size为1时，Mybatis会取第一个元素作为接口的返回值。  

    }
	public void dealMapResultSet( List<Map<String,Object>> all,  ResultSet resultSet,   String returnRsTypeName,	Map<String,Class>addFieldMap,Object parameter ) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, Exception{
		 while(resultSet != null && resultSet.next()) {
		 TjNewMap<String,Object> rowData=TjNewMap.newInstance();
   		 for (int i = 1; i <=  resultSet.getMetaData().getColumnCount(); i++) {
   			 convertJdbcFieldToMap( rowData, resultSet, resultSet.getMetaData(),i,(TjBaseEntity)parameter);
   		 }
   		 //处理行回调
   		 if(((TjBaseEntity)parameter).getRowBeanCallBack()!=null){
   			 DataEntityFieldCallBackBean dfcb=((TjBaseEntity)parameter).getRowBeanCallBack();
   			 rowCallBackDeal(dfcb,rowData);
   		 }
   		Map<String,String> dontShowMap=((TjBaseEntity)parameter).getIncludeSelectFieldMapDotShow();
   		 for (Map.Entry<String, String> entry : dontShowMap.entrySet()) {
   			 if(rowData.containsKey(entry.getKey())){
   				rowData.remove(entry.getKey());
   			 }
   		 }
   		 all.add(rowData);
        }
	}
	public void dealBeanResultSet(List<TjBaseEntity> all,  ResultSet resultSet,   String returnRsTypeName,	Map<String,Class>addFieldMap,Object parameter ) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, Exception{
		 while(resultSet != null && resultSet.next()) {
    		 TjBaseEntity rowData=(TjBaseEntity) Class.forName(returnRsTypeName).newInstance();
    		 rowData=(TjBaseEntity) rowData.initDynaMap(addFieldMap);
    		 for (int i = 1; i <=  resultSet.getMetaData().getColumnCount(); i++) {
//    			 String columnLabel= metadata.getColumnLabel(i);
//    			 Object value=resultSet.getObject(columnLabel);
    			 convertJdbcFieldToBean( rowData, resultSet, resultSet.getMetaData(),i,(TjBaseEntity)parameter);
    		 }
    		
    		
//    		 Map<String,Object> rsRowDataMap=new HashMap();
//     		for (Map.Entry<String, Class> entry :addFieldMap.entrySet()) { 
//     			Object value=resultSet.getObject(entry.getKey());//后面还有一个参数，指定返回类型，看看后面是否需要
//     			rsRowDataMap.put(entry.getKey(), value);
//     			
//     		}
    		 //处理行回调
    		 if(((TjBaseEntity)parameter).getRowBeanCallBack()!=null){
    			 DataEntityFieldCallBackBean dfcb=((TjBaseEntity)parameter).getRowBeanCallBack();
    			 rowCallBackDeal(dfcb,rowData);
    		 }
    		 all.add(rowData);
         }
	}

	//结果集向map输出
	private void convertJdbcFieldToMap(Map<String,Object> rowData, ResultSet resultSetRow,ResultSetMetaData metadata,int columnNum,TjBaseEntity parameter) throws Exception{
		 String columnLabel= metadata.getColumnLabel(columnNum);
		 Object value=resultSetRow.getObject(columnLabel);
		 String className=metadata.getColumnClassName(columnNum);
		 if(className.equalsIgnoreCase("java.sql.Timestamp")){//时间戳自动转成日期
			value=(java.util.Date)value;
		}
		if(!(columnLabel.indexOf(".id")>-1)&&columnLabel.contains("_")){
			columnLabel=StringUtils.toCamelCase(columnLabel);
		}
		
		if(columnNum==1){//在最后一列时只处理一次附加属性,实际在columnNum是多少处理都行,这里是处理附加属性的回调
			if(parameter.getMainTableAppendPropMap().size()>0){
				Map<String, Class> append=parameter.getMainTableAppendPropMap();
				for (Map.Entry<String, Class> entry:append.entrySet()) { 
					if(parameter.getFieldCallBackMap().containsKey(entry.getKey())){
						DataEntityFieldCallBackBean callback=null;
						if( parameter.getFieldCallBackMap().get(entry.getKey()) instanceof DataEntityFieldCallBackBean){
							callback=(DataEntityFieldCallBackBean)parameter.getFieldCallBackMap().get(entry.getKey());
						}else{
							callback=new DataEntityFieldCallBackBean();
							MyBeanUtils.copyBean2Bean(callback, parameter.getFieldCallBackMap().get(entry.getKey()));
						}
						              
						Object appendValue=fieldCallBackDeal(callback,"",resultSetRow,entry.getKey());//注意，附加属性是不允许在bean已经存在的属性，比如electronicCard.putAppendField("startDate",Date.class).putFieldCallBack("startDate", this,"dealStartDate");，startDate实际在bean中是存在的
						rowData.put(entry.getKey(), appendValue);
					}else{
						rowData.put(entry.getKey(), null);
					}
					
				}
			}
		}
		if(parameter.getFieldCallBackMap().containsKey(columnLabel)){
			DataEntityFieldCallBackBean callback=(DataEntityFieldCallBackBean) parameter.getFieldCallBackMap().get(columnLabel);
			value=fieldCallBackDeal(callback,value, resultSetRow,columnLabel);
		}
		if(columnLabel.indexOf(".id")>-1){
			columnLabel=columnLabel.substring(0,columnLabel.indexOf(".id"));
		}
		if(value!=null){
			//if(getStr2EmojiFieldsMap().containsKey(columnLabel)){
			    if(value instanceof String) {
			    	//对富文本调用htmlUnescape方法，否则app端接口无法正确显示
			    	if(((String) value).contains("font-family")&&((String) value).contains("font-size")) {
			    		value=HtmlUtils.htmlUnescape(value.toString());
			    	}else {
			    		value=EmojiUtil.str2Emoji(value.toString());
			    	}
			    }

		 }
		rowData.put(columnLabel, value);
	}
	
	private void convertJdbcFieldToBean(TjBaseEntity rowData, ResultSet resultSetRow,ResultSetMetaData metadata,int columnNum,TjBaseEntity parameter) throws Exception{
 		 String columnLabel= metadata.getColumnLabel(columnNum);
 		
 		 Object value=resultSetRow.getObject(columnLabel);
//		 Class<?> dbType =value.getClass(); 
//		 Field field = Reflections.getAccessibleField(rowData,columnLabel); 
//		 Class<?> beanType =field.getClass();
//		 if(beanType!=dbType){  
//             value = resultSetRow.getString(columnLabel);  
//         }
 		if(!(columnLabel.indexOf(".id")>-1)&&columnLabel.contains("_")){
 			columnLabel=StringUtils.toCamelCase(columnLabel);
 		}
 		
 		if(columnNum==1){//在最后一列时只处理一次附加属性,实际在columnNum是多少处理都行,这里是处理附加属性的回调
 			if(parameter.getMainTableAppendPropMap().size()>0){
 				Map<String, Class> append=parameter.getMainTableAppendPropMap();
 				for (Map.Entry<String, Class> entry:append.entrySet()) { 
 					if(parameter.getFieldCallBackMap().containsKey(entry.getKey())){
 						DataEntityFieldCallBackBean callback=(DataEntityFieldCallBackBean) parameter.getFieldCallBackMap().get(entry.getKey());
                      
 						Object appendValue=fieldCallBackDeal(callback,"",resultSetRow,entry.getKey());//注意，附加属性是不允许在bean已经存在的属性，比如electronicCard.putAppendField("startDate",Date.class).putFieldCallBack("startDate", this,"dealStartDate");，startDate实际在bean中是存在的
 						if(appendValue!=null){
 							appendValue=MyBeanUtils.getDestObjectFieldValue(rowData,entry.getKey(),appendValue);
 				 			 if(appendValue!=null){
 				 				Method hasexist=Reflections.getAccessibleMethodByName(rowData,StringUtils.findSetGetByProp(entry.getKey(), false));
 				 				
 				 				if(hasexist!=null){
 				 					 Reflections.invokeSetter(rowData, entry.getKey(),appendValue);
 				 					//hasexist.invoke(rowData, appendValue);
 				 				}
// 				 				 Reflections.invokeSetter(rowData, entry.getKey(),appendValue);
 				 			//	org.apache.commons.beanutils.BeanUtils.setProperty(rowData, entry.getKey(), appendValue);
 				 			 }
 				 			
 				 		 }
 					
 						
 					
 					}
 					
 				}
 			}
 		}
 		if(parameter.getFieldCallBackMap().containsKey(columnLabel)){
 			DataEntityFieldCallBackBean callback=(DataEntityFieldCallBackBean) parameter.getFieldCallBackMap().get(columnLabel);
 			value=fieldCallBackDeal(callback,value, resultSetRow,columnLabel);
 		}
 		if(columnLabel.indexOf(".id")>-1){
 			columnLabel=columnLabel.substring(0,columnLabel.indexOf(".id"));
		}
 		str2Emoji(value,rowData,columnLabel);
		

	}
	private void str2Emoji(Object value,TjBaseEntity rowData,String columnLabel) throws Exception{
		if(value!=null){
			 value=MyBeanUtils.getDestObjectFieldValue(rowData,columnLabel,value);
			 if(value!=null){
				Method hasexist=Reflections.getAccessibleMethodByName(rowData,StringUtils.findSetGetByProp(columnLabel, false));
				if(hasexist!=null){
					if(getStr2EmojiFieldsMap().size()<1){//如果不配置，那么就是全部转化，效率会低一些
						if(value!=null&&value instanceof String)
	 					 value=EmojiUtil.str2Emoji(value.toString());
					}else{//如果配置了，则以配置为主
						 
						 if(getStr2EmojiFieldsMap().containsKey(columnLabel)){
							if(value!=null&&value instanceof String)
	 						value=EmojiUtil.str2Emoji(value.toString());
					     }
					}
					
				 Reflections.invokeSetter(rowData, columnLabel,value);
				}
				// org.apache.commons.beanutils.BeanUtils.setProperty(rowData, columnLabel, value);
			 }
		 }
		
	}
	private void rowCallBackDeal(DataEntityFieldCallBackBean defcbb,Object rowData) throws InstantiationException, IllegalAccessException, ClassNotFoundException{
		Object obj=findCallBackClass(defcbb);
		if(defcbb.getOtherParam()==null||defcbb.getOtherParam().length<1){
			 Reflections.invokeMethodByName(obj, defcbb.getMethodName(), new Object[]{rowData});
		}else{
			Object[] params=defcbb.getOtherParam();
			List newParam= new ArrayList(Arrays.asList(params));
			newParam.add(0, rowData);				
			Reflections.invokeMethodByName(obj, defcbb.getMethodName(), newParam.toArray());
		}
		
	}
	private Object  findCallBackClass(DataEntityFieldCallBackBean defcbb) throws InstantiationException, IllegalAccessException, ClassNotFoundException{
		String className=defcbb.getClassName();
		if(className.contains("$$")){//说明该类已经被动态添加过属性了
			className=className.substring(0, className.indexOf("$$"));
		}
		Object obj=null;
		try{
			if(className.contains(".")){//说明传入的是个全路径
				className=StringUtils.substringAfterLast(className,"." );
			}
			className=StringUtils.firstToLower(className);
			obj=SpringContextHolder.getBean(className);
		}catch(Exception ex){
			char[] ch = className.toCharArray();
			if (ch[0] >= 'A' && ch[0] <= 'Z') {
				logger.debug("在当前spring环境中找不到" + className+ "对应的类，请将首字母小写！");
			} else {
				logger.debug("在当前spring环境中找不到" + className+ "对应的类，若该类不在spring环境中，请输入完整的包名+类名！");
			}
		}
		if(obj==null){
			obj=Class.forName(className).newInstance();
		}
		if (obj == null)
			throw new ClassNotFoundException("找不到]" + className+ "]对应的类,请核对是否正确！");
		return obj;
	}

	private Object fieldCallBackDeal(DataEntityFieldCallBackBean defcbb,Object originValue,ResultSet resultSetRow,String columnLabel) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoSuchMethodException, SecurityException, IllegalArgumentException, InvocationTargetException{
		Object value=originValue;
		if(defcbb.isQuick()){//快速回调
			if(StringUtils.isNotEmpty(defcbb.getValuePropName())){
				originValue=resultSetRow.getObject(defcbb.getValuePropName());
			}
			value=ServiceUtils.callTableByCondPorp(defcbb.getTableName(), defcbb.getQueryPropName(), originValue, defcbb.getResultPropName());
			return value;
		}
		Object obj=findCallBackClass(defcbb);
		
		if(defcbb.getOtherParam()==null||defcbb.getOtherParam().length<1){
			if(!defcbb.isIfToolMethod()){
				if(Reflections.getAccessibleMethodByName(obj, defcbb.getMethodName())==null){
					value=Reflections.invokeJavassistMethod(obj.getClass().getName(),defcbb.getMethodName(),new Class[]{Object.class,ResultSet.class},new Object[]{originValue,resultSetRow});
				}else{
					value=Reflections.invokeMethodByName(obj, defcbb.getMethodName(), new Object[]{originValue,resultSetRow});
				}
			}else{
				value=Reflections.invokeMethodByName(obj, defcbb.getMethodName(), new Object[]{originValue});
			}
			 
		}else{
			Object[] params=defcbb.getOtherParam();
			List newParam= new ArrayList(Arrays.asList(params));
			if(!defcbb.isIfToolMethod()){
				newParam.add(0, resultSetRow);	
			}
			newParam.add(0, originValue);			
			if(Reflections.getAccessibleMethodByName(obj, defcbb.getMethodName())==null){
				value=Reflections.invokeJavassistMethod(obj.getClass().getName(),defcbb.getMethodName(),new Class[]{Object.class,ResultSet.class,HttpServletRequest.class},newParam.toArray());
			}else{
				value=Reflections.invokeMethodByName(obj, defcbb.getMethodName(), newParam.toArray());
			}
		}
        
		return value;
	}
	
//	@Override 这个是想对结果集的bean或map进行转换处理，实际是不需要的，降低了效率，在程序出来以后用转换工具，无论bena还是map，都可以转换的
//    public Object intercept(Invocation invocation) throws Throwable {
//    	Object target = invocation.getTarget();
////		ResultSetHandler  resultSetHandler = (ResultSetHandler) target;  
////		ParameterHandler parameterHandler=(ParameterHandler)Reflections.getFieldValue(resultSetHandler,"parameterHandler");
////		Object parameter =parameterHandler.getParameterObject() ;
//		
//        Object[] args = invocation.getArgs();
//        final Object parameter = args[1];
//        // 获取到当前的Statement
//        Statement stmt =  (Statement) args[0];
//        // 通过Statement获得当前结果集
//        ResultSet resultSet = stmt.getResultSet();
//        String returnRsType=findMappersResultType(invocation);//stmt.getResultSetType();  1是bean 2是map 3是其他
//        if(parameter==null)return invocation.proceed();
//        if(parameter instanceof BaseEntity){
//    	   if(StringUtils.isNotEmpty(((BaseEntity)parameter).getResultType())){
//    		   if("1".equals(((BaseEntity)parameter).getResultType())){//查询要求返回是bean
//    			   if("2".equals(returnRsType)){//实际mapper中配置的返回是map
//    				   
//    			   }
//    		   }else if("2".equals(((BaseEntity)parameter).getResultType())){//查询返回要求是map
//                   if("1".equals(returnRsType)){//实际mapper中配置的返回是bean
//    				   
//    			   }
//    		   }
//    	   }else{
//    		   return invocation.proceed();
//    	   }
//    	}else{
//    		return invocation.proceed();
//    	}
//       
//        List<Object> resultList = new ArrayList<Object>();  
//        if(resultSet != null && resultSet.next()) {
//           
//            // handleResultSets返回结果一定是一个List
//            // size为1时，Mybatis会取第一个元素作为接口的返回值。  
//            return resultList;
//        }
//        return invocation.proceed();
//    }
	/**
	 * 在调用anyquery时，匹配当前bean中不存在的属性
	 * @param parameter
	 * @return
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 */
	private Map<String,Class> findAnyQueryFieldMap(ResultSet resultSet ,TjBaseEntity parameter) throws SQLException, ClassNotFoundException{
		Map<String,Class> filedMap=new HashMap();
		if(StringUtils.isEmpty(parameter.getAnySql())&&!parameter.isClearSelectField()){//20190326修改
			return filedMap;
		}
		String currClassName=parameter.getClass().getName();
		Field[] fieldArray=null;
		if(currClassName.contains("$$BeanGeneratorByCGLIB")){//说明该类已经被动态添加过属性了
			String originClassName=currClassName.substring(0, currClassName.lastIndexOf("$$BeanGeneratorByCGLIB"));
			fieldArray=Class.forName(originClassName).getDeclaredFields();
		}else{
			 fieldArray=parameter.getClass().getDeclaredFields();
		}

		Map<String,String> existMap=new HashMap();
		for(Field fi:fieldArray){
			existMap.put(fi.getName(), "");
		}
		 ResultSetMetaData metadata = resultSet.getMetaData();
    	 int count = metadata.getColumnCount(); 
    	 for (int i = 1; i <= count; i++) {//重新匹配新增字段的java类型
    		 String columnLabel= metadata.getColumnLabel(i);
    		 String convertColumnLabel=null;
    		 if(columnLabel.contains("_")){
    			 convertColumnLabel=StringUtils.toCamelCase(columnLabel);
    		 }else{
    			 convertColumnLabel=columnLabel;
    		 }
    				
    		 if(!existMap.containsKey(convertColumnLabel)){
    			  filedMap.put(convertColumnLabel, Class.forName(metadata.getColumnClassName(i)));
    		 }
    	 }
    	 return filedMap;
	}
	/**
	 * 获得addSelectFieldMap中的字段列表
	 * @param parameter
	 * @return
	 */
	private Map<String,Class> findAddSelectFieldMap(TjBaseEntity parameter){
		Map<String,String>  addSelectFieldMap=	parameter.getAddSelectField();
		Map<String,Class> addFieldMap=new HashMap();
		for(Map.Entry<String, String> entry : addSelectFieldMap.entrySet()){
		    String mapKey = entry.getKey();
		    String mapValue = entry.getValue();
		    addFieldMap.put(mapValue, String.class);//此处都用java.lang.String.class，后续会根据jdbctype重新匹配
		}
	
		return addFieldMap;
	}
	/**
	 * 获得groupby中的字段列表
	 * @param parameter
	 * @return
	 */
	private Map<String,Class> findGroupbyFieldMap(TjBaseEntity parameter){
		List<String>  selFieldList=	parameter.getGroupBySelectField();
		Map<String,Class> groupbyFieldMap=new HashMap();
		for(String selField:selFieldList){
			
			selField=selField.substring(selField.toLowerCase().indexOf("as ")+3);
			 String fieldName=StringUtils.deleteWhitespace(selField);
			 groupbyFieldMap.put(fieldName, java.lang.String.class);//此处都用java.lang.String.class，后续会根据jdbctype重新匹配
		}
		return groupbyFieldMap;
	}
	/**
	 * 获得新加字段的列表
	 * @param jtbs
	 * @param mainTableAppendPropMap
	 * @return
	 * @throws Exception 
	 */
	private 	Map<String,Class> findAddFieldMap(List<JoinTableBean> jtbs,Map<String,Class> mainTableAppendPropMap) throws Exception{
		Map<String,Class> addFieldMap=new HashMap();

		 for(int i=0;i<jtbs.size();i++){
	        	JoinTableBean jtb=null ;
	        	if(jtbs.get(i) instanceof JoinTableBean){
	        	   jtb=jtbs.get(i); 
	  
	            }else{
	          		jtb=new JoinTableBean();
	            	MyBeanUtils.copyBean2Bean(jtb, jtbs.get(i));
	      }
	        	for (Map.Entry<String, String> entry :( jtb.getSelectMap()).entrySet()) { 
	        		  String fieldName="";
	        		  if(StringUtils.isEmpty(entry.getValue())){
	        			  fieldName=entry.getKey();
	        		  }else{
	        			  fieldName=entry.getValue();
	        		  }
	        		  if(fieldName.contains("\""))fieldName=fieldName.replace("\"", "");//20190507添加，由于oracle的字段别名必须加上引号，在jointablebean中
	        		  //putselect方法中字段别名都添加了双引号，所以虚拟字段的名称都会多一个双引号，从数据库拿出字段时无法对应，因此在此添加过滤掉
	        		  addFieldMap.put(fieldName, java.lang.String.class);//此处都用java.lang.String.class，后续会根据jdbctype重新匹配
	        	}
	        }
		 addFieldMap.putAll(mainTableAppendPropMap);
		 return addFieldMap;
	}

	@Override
	public Object plugin(Object target) {
		return Plugin.wrap(target, this);// 这些莫认调用
	}

	@Override
	public void setProperties(Properties properties) {
		 super.initProperties(properties);
	}
	private String findMappersResultType(String typeName){
		try {
			if(NewInstanceUtil.newInstance(Class.forName(typeName)) instanceof TjBaseEntity){//com.jujienet.modules.sys.entity.User
				typeName="1";
			}else  if(NewInstanceUtil.newInstance(Class.forName(typeName))instanceof Map){;
				typeName="2";
			}else{
				typeName="3";
			}
		} catch (ClassNotFoundException e) {
		
			e.printStackTrace();
		}
		return typeName;
	}
//	private MappedStatement mapperstatement=null;
	private MappedStatement getMapperStatement(Invocation invocation){
	//	if(mapperstatement!=null)return mapperstatement;
		ResultSetHandler resultSetHandler = (ResultSetHandler) invocation.getTarget();
		//通过java反射获得mappedStatement属性值
		return (MappedStatement)Reflections.getFieldValue(resultSetHandler, "mappedStatement");
		//return mapperstatement;
	}
	private void changeMapperStatement(Invocation invocation,MappedStatement mewMs){
		ResultSetHandler resultSetHandler = (ResultSetHandler) invocation.getTarget();
		//通过java反射获得mappedStatement属性值
		Reflections.setFieldValue(resultSetHandler, "mappedStatement", mewMs);
	//	Reflections.invokeSetter(resultSetHandler, "mappedStatement", mewMs);
	}
	private String findMappersResultTypeName(Invocation invocation){
	   MappedStatement ms =getMapperStatement(invocation);
		//final MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
		List<ResultMap> rms = ms.getResultMaps();
		ResultMap rm = rms != null && rms.size() > 0 ? rms.get(0) : null;
		String type = rm != null && rm.getType() != null ? rm.getType().getName() : "";
		
      
		return type;
	}
}
