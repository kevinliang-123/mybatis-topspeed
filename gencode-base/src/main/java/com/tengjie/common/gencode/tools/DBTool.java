package com.tengjie.common.gencode.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;

import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ibatis.type.JdbcType;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mysql.jdbc.authentication.MysqlClearPasswordPlugin;

import com.tengjie.common.config.Global;
import com.tengjie.common.gencode.vo.TableColumn;

import com.tengjie.common.persistence.util.JspElement;
import com.tengjie.common.utils.PackageUtil;
import com.tengjie.common.utils.Reflections;
import com.tengjie.common.utils.SpringContextHolder;
import com.tengjie.common.utils.StringUtils;


public class DBTool  implements ServletContextAware{
	private static Log logger = LogFactory.getLog(DBTool.class);
    private static String schemaName="ry";

    public static Connection conn=null;
    private static  Map<String,String> existDelFlagFieldTable=Maps.newHashMap();//存在del_flag字段的表，key为tbTableBame
    private static Set<Class<?>> allClass=PackageUtil.getClasses("com.ruoyi");//所有tengjie的类，以便后续查找时方便使用
    private static Map<String,List<String>> fieldToTableMap=Maps.newHashMap();//字段与表的映射关系，即使一个字段可能在多个表中重名；key为字段名，value为带有全路径的entity名
    private static Map<String,String> allTable=Maps.newHashMap();
    public static Map<String,Map<String,TableColumn>>allTableField=Maps.newHashMap();//全部表和字段的列表数据,第一层key为表名（存储的是tbTableName，即tbTeacherInfo，对于新类就是_TB_TABLE_NAME_）第二层key为字段名，第二层value为字段描述

	public static Connection getConn() {
		if(conn==null) {
			conn=SpringContextHolder.getConnection();
		}
		return conn;
	}
	public static void initAllData(){
		
		try {
			allTable=getTables();
			for (Map.Entry<String,String> entry : allTable.entrySet()) { 
				 String dbTableName=entry.getKey();
//				 if(dbTableName.toLowerCase().equals("tb_report_admin")) {
//					 System.out.println("---");
//				 }
				 Map<String,TableColumn> cols=getFieldList(dbTableName);
				 if(cols.containsKey("delFlag")) {
					 existDelFlagFieldTable.put(StringUtils.toCamelCase(dbTableName), "");
				 }
				 allTableField.put(StringUtils.toCamelCase(dbTableName), cols);
				 
			}
		} catch (Exception e) {
			//SpringContextHolder.releaseConnection(conn);
			e.printStackTrace();
		}
		//初始化TableColumn的关联字段信息
		for (Map.Entry<String,Map<String,TableColumn>> entry : allTableField.entrySet()) { 
			Map<String,TableColumn> tablecolumn=entry.getValue();
			for(TableColumn  tc: tablecolumn.values()){
				String refTable=tc.getRefTableName();
			    if(StringUtils.isNotEmpty(refTable)) {
			    	TableColumn destreftc=findTableColumn(refTable,StringUtils.toCamelCase(tc.getRefFieldName()));
			    	tc.setRefTableColumn(destreftc);
			    }
			}
		}
		
	}
	public static boolean isExistDelFlagField(String tbTableName) {
		return existDelFlagFieldTable.containsKey(tbTableName);
	}
	/**
	 * 根据数据表名和java字段名获得TableColumn信息
	 * @param dbTableName
	 * @param javaFieldName
	 * @return
	 */
	public static TableColumn findTableColumn(String dbTableName,String javaFieldName) {
		Map<String,TableColumn> refTableColumns=allTableField.get(StringUtils.toCamelCase(dbTableName));
		if(refTableColumns!=null) {
    		String refField=javaFieldName;
    		TableColumn destreftc=refTableColumns.get(refField);
    		return destreftc;
    	}
		return null;
	}
	/**获取某个conn下的所有表
	 * @return
	 */
	public static Map<String,String> getTables() throws Exception{
		if(StringUtils.isEmpty(schemaName)||schemaName.contains("must define")) {
			throw new Exception("必须在config.properties配置文件中配置jdbc.schemaName=库名称！否则无法加载数据库信息");
		}
		String sql = "SELECT table_name AS name,TABLE_COMMENT AS comment FROM INFORMATION_SCHEMA.TABLES WHERE table_schema = '"+schemaName+"'";
		PreparedStatement pstmt = (PreparedStatement)getConn().prepareStatement(sql);
		Map<String,String> tableMap = Maps.newHashMap();	//存放字段
		ResultSet rs = pstmt.executeQuery();
		while (rs.next()){
			String a=rs.getString(1);// 表名
			String b=rs.getString(2);// 说明
			if(b.equals("")){
				b = a.toUpperCase();
			}
			tableMap.put(a, b);
		}
		//SpringContextHolder.releaseConnection(conn);
		return tableMap;
	}
	
	
	
	/**字段名列表，这个拿不到表的备注。这个是调用的db的元数据，会不会更快
	 * @param conn
	 * @param table
	 * @return
	 * @throws SQLException
	 */
	public static Map<String,TableColumn> getFieldListByMetada1(String dbTableName) throws SQLException{
		PreparedStatement pstmt = (PreparedStatement) getConn().prepareStatement("select * from "+dbTableName);
		//pstmt.execute(); //这点特别要注意:如果是Oracle必须要加，而对于mysql可以不用加.
		Map<String,TableColumn> columnMap=Maps.newLinkedHashMap();	//存放字段
		ResultSetMetaData rsmd = (ResultSetMetaData) pstmt.getMetaData();
//getColumnLabel实际是字段的别名，getColumnName是字段的原始值，只不过在本次查询中他俩是不一样，但是一旦加上别名，就有可能不一样。
		 for (int i = 1; i < rsmd.getColumnCount() + 1; i++) {
			 TableColumn gtc=new TableColumn();
				gtc.setName(rsmd.getColumnName(i));                    //字段名
				gtc.setJavaField(StringUtils.toCamelCase(gtc.getName()));
				gtc.setIsNull(rsmd.isNullable(i)+"");                  //是否可以为空
				gtc.setSort(i);  //字段在表中排序号
				gtc.setComments(rsmd.getColumnLabel(i));                //字段别名列名
				gtc.setJdbcType(rsmd.getColumnTypeName(i));                //字段数据类型
				columnMap.put(gtc.getJavaField(), gtc);		//把字段名放list里
          }
		// SpringContextHolder.releaseConnection(conn);
		return columnMap;
	}
	
	/**(字段名、类型、长度)列表
	 * @param 
	 * @param dbTableName :原始的数据库表名称
	 * @return
	 * @throws SQLException
	 */
	public static Map<String,TableColumn> getFieldList( String dbTableName) throws SQLException{
//		SELECT t.COLUMN_NAME AS NAME, 
//	      (CASE WHEN t.IS_NULLABLE = 'YES' THEN '1' ELSE '0' END) AS ISNULL,
//	      (t.ORDINAL_POSITION * 10) AS sort,
//	       t.COLUMN_COMMENT AS comments,
//	       t.COLUMN_TYPE AS jdbcType,
//	       t.CHARACTER_MAXIMUM_LENGTH,
//				 t.COLUMN_KEY,
//				 pri.REFERENCED_TABLE_NAME,
//				 pri.REFERENCED_COLUMN_NAME
//	FROM information_schema.`COLUMNS` t 
//	left join(
//	select  k.COLUMN_NAME,k.REFERENCED_TABLE_NAME,k.REFERENCED_COLUMN_NAME
//	  from INFORMATION_SCHEMA.KEY_COLUMN_USAGE k where 
//	    k.TABLE_SCHEMA = (SELECT DATABASE()) AND k.TABLE_NAME = 'tb_assess_project' and  k.constraint_name !='PRIMARY'
//	) pri on pri.COLUMN_NAME=t.COLUMN_NAME
//	WHERE t.TABLE_SCHEMA = (SELECT DATABASE()) AND t.TABLE_NAME = 'tb_assess_project'
//
//	ORDER BY t.ORDINAL_POSITION
        String sql="SELECT t.COLUMN_NAME AS NAME, \n" + 
        		"      (CASE WHEN t.IS_NULLABLE = 'YES' THEN '1' ELSE '0' END) AS ISNULL,\n" + 
        		"      (t.ORDINAL_POSITION * 10) AS sort,\n" + 
        		"       t.COLUMN_COMMENT AS comments,\n" + 
        		"       t.COLUMN_TYPE AS jdbcType,\n" + 
        		"       (CASE WHEN t.COLUMN_KEY = 'PRI' THEN '1' ELSE '0' END) AS isPk, \n" +
        		"       t.CHARACTER_MAXIMUM_LENGTH,\n" + 
        		"			 pri.REFERENCED_TABLE_NAME,\n" + 
        		"			 pri.REFERENCED_COLUMN_NAME \n" + 
        		"FROM information_schema.`COLUMNS` t \n" + 
        		"left join(\n" + 
        		"select  k.COLUMN_NAME,k.REFERENCED_TABLE_NAME,k.REFERENCED_COLUMN_NAME\n" + 
        		"  from INFORMATION_SCHEMA.KEY_COLUMN_USAGE k where \n" + 
        		"    k.TABLE_SCHEMA = (SELECT DATABASE()) AND k.TABLE_NAME = '"+ dbTableName +"' and  k.constraint_name !='PRIMARY' \n" + 
        		") pri on pri.COLUMN_NAME=t.COLUMN_NAME \n" + 
        		"WHERE t.TABLE_SCHEMA = (SELECT DATABASE()) AND t.TABLE_NAME = '"+ dbTableName +"' \n" + 
        		"ORDER BY t.ORDINAL_POSITION\r\n";
        		
//		String sql = "SELECT t.COLUMN_NAME AS NAME, \n" +
//				"      (CASE WHEN t.IS_NULLABLE = 'YES' THEN '1' ELSE '0' END) AS ISNULL,\n" +
//				"      (t.ORDINAL_POSITION * 10) AS sort,\n" +
//				"       t.COLUMN_COMMENT AS comments,\n" +
//				"       t.COLUMN_TYPE AS jdbcType, \n" +
//				"       (CASE WHEN t.COLUMN_KEY = 'PRI' THEN '1' ELSE '0' END) AS isPk \n" +
//				"FROM information_schema.`COLUMNS` t \n" +
//				"WHERE t.TABLE_SCHEMA = (SELECT DATABASE()) AND t.TABLE_NAME = '"+ dbTableName +"'\n" +
//				"ORDER BY t.ORDINAL_POSITION";

		PreparedStatement pstmt = (PreparedStatement) getConn().prepareStatement(sql);
		Map<String,TableColumn> columnMap=Maps.newLinkedHashMap();	//存放字段
		ResultSet rs = pstmt.executeQuery();
		
		
		while (rs.next()){
			TableColumn gtc = new TableColumn();
			gtc.setTableName(dbTableName);
			gtc.setName(rs.getString(1));                    //字段名
			String javaField=StringUtils.toCamelCase(gtc.getName());
			gtc.setJavaField(javaField);
			gtc.setIsNull(rs.getString(2));                  //是否可以为空
			gtc.setSort(Integer.parseInt(rs.getString(3)));  //排序
			gtc.setComments(rs.getString(4));                //字段备注
			String orign=rs.getString(5); //字段数据类型
			gtc.setCharacterMaxinumLength(rs.getLong(7));
			parseJdbcType(gtc,orign);
			gtc.setIsPk(rs.getString(6));// 是否是主键 1:主键 0:不是主键
			gtc.setRefTableName(rs.getString(8));//关联表表名
			gtc.setRefFieldName(rs.getString(9));//关联表字段名
			
			initColumnJavaType(gtc);//必须放到初始化jdbctype之后，因为是依赖jdbctype来判断的
			
			columnMap.put(gtc.getJavaField(), gtc);
			//处理字段与表对应关联的map
			String entityName=StringUtils.toCamelCase(dbTableName);
			if(entityName.indexOf("tb")==0){
				entityName=entityName.replace("tb", "");
			}else{
				entityName=StringUtils.firstToUpper(entityName);
			}
			 for(Class clazz:allClass){
				 if(clazz.getSimpleName().equals(entityName)){
					 entityName=clazz.getName();
				 }
			 }
			if(fieldToTableMap.containsKey(javaField)){
				fieldToTableMap.get(javaField).add(entityName);
			}else{
				fieldToTableMap.put(javaField, Lists.newArrayList(entityName));
			}
			
		}
		//SpringContextHolder.releaseConnection(conn);
		return columnMap;
	}
	private static void parseJdbcType(TableColumn gtc ,String orign){
		 String regex = "(?<=\\()(\\S+)(?=\\))";
         Pattern pattern = Pattern.compile (regex);
         Matcher matcher = pattern.matcher (orign);
         String precision="";
         while(matcher.find()){  
        	 precision=matcher.group(1);
         }
         if(gtc.getJavaField().equals("context")) {
        	 System.out.println();
         }
         if(StringUtils.isEmpty(precision)){
        	 gtc.setJdbcType(orign);
        	 if(gtc.getCharacterMaxinumLength().longValue()>0) {
        		 int po=Integer.MAX_VALUE;
        		 if(gtc.getCharacterMaxinumLength()<po) {
        			 po=gtc.getCharacterMaxinumLength().intValue();
        		 }
 				 gtc.setPricisionOne(po);
 			 }       	
         }else{
        	 gtc.setJdbcType(orign.replace("("+precision+")", ""));
        	 if(precision.contains(",")){
        		 String[] strarr=precision.split(",");
        		 gtc.setPricisionOne(Integer.valueOf(strarr[0]));
        		 gtc.setPricisionTwo(Integer.valueOf(strarr[1]));
        	 }else{
        		
        		//powerdesign如果int类型（不设置位数），直接到mysql中会出现11为，而java的integer只能有10位，
        		 //这就导致用户如果输入11位spring转换到bean会直接报错，因为页面也是11位,但是10位也不行，最大值位2147483647，因此直接用9位
        		 if(gtc.getJdbcType().equals("int")&&Integer.valueOf(precision)>9) {
        			 gtc.setPricisionOne(Integer.valueOf(9));
        		 }else {
        			 gtc.setPricisionOne(Integer.valueOf(precision));
        		 }
        		
        	 }
        	
         }
		
	}
	
    
    /**
     * 信用卡
     * @param str
     * @return
     */
    private boolean creditcardTypeJudge(String ...str){
    	Map<String,String> map=new CaseInsensitiveMap();
    	map.put("creditcard", "");
    	map.put("credit", "");	
    	map.put("信用卡", "");
    	return commonJudge(map,str);
    }
    /**
     * 密码
     * @param str
     * @return
     */
    private boolean pwdpTypeJudge(String ...str){
    	Map<String,String> map=new CaseInsensitiveMap();
    	map.put("pwd", "");
    	map.put("password", "");	
    	map.put("密码", "");
    	return commonJudge(map,str);
    }
    /**
     * ip地址
     * @param str
     * @return
     */
    private boolean ipTypeJudge(String ...str){
    	Map<String,String> map=new CaseInsensitiveMap();
    	map.put("ip地址", "");
    	map.put("ipAddress", "");	
    	map.put("ipNum", "");
    	return commonJudge(map,str);
    }
    
    /**
     * 身份证
     * @param str
     * @return
     */
    private boolean idCardTypeJudge(String ...str){
    	Map<String,String> map=new CaseInsensitiveMap();
    	map.put("身份证", "");
    	map.put("证件号码", "");
    	map.put("身份号", "");
    	map.put("证件号", "");
    	map.put("idcard", "");	
    	map.put("identitycard", "");
    	return commonJudge(map,str);
    }
    /**
     * 判断是否图片的英文字母采样，后续可以放到数据库，每个项目配置
     * @param str
     * @return
     */
    private boolean emailTypeJudge(String ...str){
    	Map<String,String> map=new CaseInsensitiveMap();
    	map.put("email", "");
    	map.put("mail", "");
    	map.put("邮件", "");
    	map.put("邮箱", "");
    	return commonJudge(map,str);
    }
    /**
     * 固定电话
     * @param str
     * @return
     */
    private boolean fixPhoneTypeJudge(String ...str){
    	Map<String,String> map=new CaseInsensitiveMap();
    	map.put("fixPhone", "");
    	map.put("linePhone", "");
    	map.put("telePhone", "");
    	map.put("固定电话", "");
    	map.put("固话", "");
    	return commonJudge(map,str);
    }
    /**
     * 手机号码
     * @param str
     * @return
     */
    private boolean mobilePhoneTypeJudge(String ...str){
    	Map<String,String> map=new CaseInsensitiveMap();
    	map.put("mobile", "");
    	map.put("Phone", "");
    	map.put("telePhone", "");
    	map.put("手机号", "");
    	map.put("联系电话", "");
    	map.put("电话号", "");
    	map.put("手机", "");
    	map.put("电话", "");
    	return commonJudge(map,str);
    }
    /**
     * 判断是否图片的英文字母采样，后续可以放到数据库，每个项目配置
     * @param str
     * @return
     */
    private boolean pictureTypeJudge(String ...str){
    	Map<String,String> map=new CaseInsensitiveMap();
    	map.put("pic", "");
    	map.put("photo", "");
    	map.put("avatar", "");
    	map.put("url", "");
    	map.put("picture", "");
    	map.put("图片", "");
    	map.put("banner图", "");
    	map.put("图", "");
    	return commonJudge(map,str);
    }
    /**
     * 隐藏字段类型判断
     * @param str
     * @return
     */
    private boolean hiddenTypeJudge(String ...str){
    	Map<String,String> map=new CaseInsensitiveMap();
    	map.put("_id", "");
    	boolean bl=commonJudge(map,str);
    	if(bl)return bl;
    	for(String temp:str) {
    		if(temp.endsWith("id")||temp.endsWith("Id"))return true;
    	}
    	return false;
    }
    /**
     * 判断是否音频的英文字母采样，后续可以放到数据库，每个项目配置
     * @param str
     * @return
     */
    private boolean audioTypeJudge(String ...str){
    	Map<String,String> map=new CaseInsensitiveMap();
    	map.put("audio", "");
    	map.put("音频", "");
    	map.put("音乐", "");
    	return commonJudge(map,str);
    }
    /**
     * 判断是否音频的英文字母采样，后续可以放到数据库，每个项目配置
     * @param str
     * @return
     */
    private boolean videoTypeJudge(String ...str){
    	Map<String,String> map=new CaseInsensitiveMap();
    	map.put("video", "");
    	map.put("视频", "");
    	return commonJudge(map,str);
    }
    /**
     * 下拉框判断
     * @param str
     * @return
     */
    private boolean selectTypeJudge(String ...str){
    	Map<String,String> map=new CaseInsensitiveMap();
    	map.put("kind", "");
    	map.put("type", "");	
    	map.put("status", "");
    	map.put("状态", "");
    	map.put("种类", "");
    	map.put("类型", "");
    	map.put("性别", "");
    	return commonJudge(map,str);
    }
    private boolean commonJudge(Map<String,String> map,String ...str){
    	 boolean bl=false;
     	for(final String temp:str){
     		if(judgeMapIncludeKey(map,temp)){
     			bl=true;
     			break;
     		}
     	}
     	return bl;
    }
    /**
     * 判断一个map的key是否包含temp，注意这个不是通常map的.containKey，containKey表示必须完全相等，这个是只要包含、含有就行，是子串即可
     * @param map
     * @param temp
     * @return
     */
    private boolean judgeMapIncludeKey(Map<String,String> map,final String temp){
    	Map<String,String> result=Maps.filterEntries(map, new Predicate<Map.Entry<String, String>>() {
			@Override
			public boolean apply(Entry<String, String> input) {
				if(containIgnore(temp,input.getKey()))return true;
				return false;
			}
 			});
    	if(result.size()>0){
    		return true;
    	}else{
    		return false;
    	}
    }
    private boolean containIgnore(String origin,String dest){
    	boolean bl=false;
    	if(StringUtils.isEmpty(origin)||StringUtils.isEmpty(dest))return bl;
    	if(origin.toLowerCase().contains(dest.toLowerCase()))bl=true;
    	return bl;
    }
    
    private Map<String,Integer> mybatisTextType(){
    	Map<String,Integer> maps=new CaseInsensitiveMap();
    	maps.put("BLOB", java.sql.Types.BLOB);
    	maps.put("TEXT", java.sql.Types.LONGVARCHAR);
    	maps.put("MEDIUMBLOB", java.sql.Types.BLOB);
    	maps.put("MEDIUMTEXT", java.sql.Types.LONGVARCHAR);
    	maps.put("LONGBLOB", java.sql.Types.BLOB);
    	maps.put("LONGTEXT", java.sql.Types.LONGVARCHAR);
    	return maps;
    }
    public static Map<String,Integer> mybatisDateType(){
    	Map<String,Integer> maps=new CaseInsensitiveMap();
    	maps.put("DATE", java.sql.Types.DATE);
    	maps.put("TIME", java.sql.Types.TIME);
    	maps.put("YEAR", java.sql.Types.DATE);
    	maps.put("DATETIME", java.sql.Types.TIMESTAMP);
    	maps.put("TIMESTAMP", java.sql.Types.TIMESTAMP);
    	return maps;
    }
    public static Map<String,Integer> mybatisNumType(){
    	Map<String,Integer> maps=new CaseInsensitiveMap();
    	maps.put("BIGINT", java.sql.Types.BIGINT);
    	maps.put("DECIMAL", java.sql.Types.DECIMAL);
    	maps.put("DOUBLE", java.sql.Types.DOUBLE);
    	maps.put("FLOAT", java.sql.Types.FLOAT);
    	maps.put("INTEGER", java.sql.Types.INTEGER);
    	maps.put("INT", java.sql.Types.INTEGER);
    	maps.put("NUMERIC", java.sql.Types.NUMERIC);
    	maps.put("SMALLINT", java.sql.Types.SMALLINT);
    	maps.put("TINYINT", java.sql.Types.TINYINT);
    	return maps;
    }
    /**
	 * 初始化列属性字段  JDBC和Java类型映射
	 * @param column
	 */
	public static void initColumnJavaType(TableColumn column){

	

		// 设置java类型
		if (StringUtils.startsWithIgnoreCase(column.getJdbcType(), "CHAR")
				|| StringUtils.startsWithIgnoreCase(column.getJdbcType(), "LONGTEXT")
				|| StringUtils.startsWithIgnoreCase(column.getJdbcType(), "VARCHAR")
				|| StringUtils.startsWithIgnoreCase(column.getJdbcType(), "NARCHAR")
				|| StringUtils.startsWithIgnoreCase(column.getJdbcType(), "TEXT")
				|| StringUtils.startsWithIgnoreCase(column.getJdbcType(), "MEDIUMTEXT")){
			column.setJavaType("String");
		}else if (StringUtils.startsWithIgnoreCase(column.getJdbcType(), "DATETIME")
				|| StringUtils.startsWithIgnoreCase(column.getJdbcType(), "DATE")
				|| StringUtils.startsWithIgnoreCase(column.getJdbcType(), "TIMESTAMP")){
			column.setJavaType("java.util.Date");
	
		}else if (StringUtils.startsWithIgnoreCase(column.getJdbcType(), "BIGINT")
				|| StringUtils.startsWithIgnoreCase(column.getJdbcType(), "NUMBER")){
			// 如果是浮点型
			String[] ss = StringUtils.split(StringUtils.substringBetween(column.getJdbcType(), "(", ")"), ",");
			if (ss != null && ss.length == 2 && Integer.parseInt(ss[1])>0){
				column.setJavaType("Double");
			}
			// 如果是整形
			else if (ss != null && ss.length == 1 && Integer.parseInt(ss[0])<=10){
				column.setJavaType("Integer");
			}
			// 长整形
			else{
				column.setJavaType("Long");
			}
		}else if (StringUtils.startsWithIgnoreCase(column.getJdbcType(), "DECIMAL")
		    ||StringUtils.startsWithIgnoreCase(column.getJdbcType(), "NUMERIC")){
		    column.setJavaType("java.math.BigDecimal");
		}else if (StringUtils.startsWithIgnoreCase(column.getJdbcType(), "TINYINT")){
            column.setJavaType("Integer");
        }else if (StringUtils.startsWithIgnoreCase(column.getJdbcType(), "INT")){
            column.setJavaType("Integer");
        }else if (StringUtils.startsWithIgnoreCase(column.getJdbcType(), "DOUBLE")){
            column.setJavaType("Double");
        }else if (StringUtils.startsWithIgnoreCase(column.getJdbcType(), "TIME")){
            column.setJavaType("java.sql.Time");
        }else if (StringUtils.startsWithIgnoreCase(column.getJdbcType(), "BIT")){
            column.setJavaType("Integer");
        }

	}
	
	@Override
	public void setServletContext(ServletContext servletContext) {
		DBTool.initAllData();
		
	}
	public static Set<Class<?>> getAllClass() {
		return allClass;
	}
	public static void setAllClass(Set<Class<?>> allClass) {
		DBTool.allClass = allClass;
	}
	public static Map<String, String> getAllTable() {
		toInitFilter(allTable);
		return allTable;
	}
	public static void setAllTable(Map<String, String> allTable) {
		DBTool.allTable = allTable;
	}
	public static Map<String, Map<String, TableColumn>> getAllTableField() {
		toInitFilter(allTableField);
		return allTableField;
	}
	public static void setAllTableField(Map<String, Map<String, TableColumn>> allTableField) {
		DBTool.allTableField = allTableField;
	}
	public static Map<String, List<String>> getFieldToTableMap() {
		toInitFilter(fieldToTableMap);
		return fieldToTableMap;
	}
	public static void setFieldToTableMap(Map<String, List<String>> fieldToTableMap) {
		DBTool.fieldToTableMap = fieldToTableMap;
	}
	private static void toInitFilter(Object obj) {
		if(obj==null) {
			initAllData();
		}else if(obj instanceof Map) {
			if(((Map)obj).size()<1) {
				initAllData();
			}
		}
		
	}
}
