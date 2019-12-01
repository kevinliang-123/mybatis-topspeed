package com.tengjie.common.persistence.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

import javax.servlet.ServletContext;

import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;

import com.google.common.collect.Maps;
import com.tengjie.common.config.Global;
import com.tengjie.common.persistence.TjBaseEntity;
import com.tengjie.common.utils.PackageUtil;
import com.tengjie.common.utils.SpringContextHolder;
import com.tengjie.common.utils.StringUtils;
import com.tengjie.common.utils.TjNewMap;

/**
 * 由于在程序中很多地方，比如说查询表的下拉框，关键值会直接用id字段，但是有些表比如city、province她的主键不是id，
 * 并且有些程序其他的地方也是这个原则，或者给其他人用这个封装，他们的表的主键不一定叫id，
 * 因此这里会全局配置表的id字段配置，在本类中只针对我们自己的项目中的某几张表，并且预置好了，
 * 然后在配置文件中会有dbTablePrimaryKeyFieldConfig="自定义类路径";，会优先加载这个
 * 自定义类路径，这个类也要定义一个方法public static void buildKeyFieldsMap()来覆盖这个父类方法。
 *
 * @author admin
 *
 */
@Component
public class DbTablePrimaryKeyFieldConfig  implements ServletContextAware{
	private static final String idFieldName="id";
	private static String schemaName=Global.getConfig("jdbc.schemaName");
	public static Connection conn=null;
	protected static TjNewMap<String,String> keyFieldsMap=TjNewMap.newInstance();//key为数据库表名，value为主键字段名，字段名是驼峰后的
	@Override
	public void setServletContext(ServletContext servletContext) {
		try {
			DbTablePrimaryKeyFieldConfig.buildKeyFieldsMap();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	
	/**
	 * 构建表与主键字段的对应关系
	 * @throws Exception 
	 */
	private static void buildKeyFieldsMap() throws Exception {
		if(StringUtils.isEmpty(schemaName)) {
			throw new Exception("必须在config.properties中配置当前项目的数据schemaName，或直接配置schemaName的值");
		}
		if(conn==null) {
			conn=SpringContextHolder.getConnection();
		}
		String sql="SELECT\n" + 
				"  t.TABLE_NAME ,\n" + 
				"	max(c.COLUMN_NAME) AS NAME\n" + 
				"FROM\n" + 
				"	information_schema.`COLUMNS` c\n" + 
				"	left join\n" + 
				"   INFORMATION_SCHEMA.TABLES t on t.TABLE_NAME=c.table_name \n" + 
				"WHERE\n" + 
				"c.COLUMN_KEY = 'PRI' and\n" + 
				"c.TABLE_SCHEMA = '"+schemaName+"' and\n" + 
				"t.table_schema = '"+schemaName+"'\n" + 
				"group by  t.TABLE_NAME";
		PreparedStatement pstmt = (PreparedStatement)conn.prepareStatement(sql);
		Map<String,String> tableMap = Maps.newHashMap();	//存放字段
		ResultSet rs = pstmt.executeQuery();
		while (rs.next()){
			String a=rs.getString(1);// 表名
			String b=rs.getString(2);// 主键列名
			keyFieldsMap.put(a,StringUtils.toCamelCase(b));
			
		}
	}
	/**
	 * 根据实体名获得主键字段名
	 * @param entityName
	 * @return
	 */
	public static String findKeyField(String entityName) {
		entityName=StringUtils.firstToLower(entityName);
		String dbTableName=StringUtils.toUnderScoreCase(entityName);
		String startWithTb=keyFieldsMap.get(dbTableName);
		if(StringUtils.isEmpty(startWithTb)) {//如果找不到
			startWithTb=keyFieldsMap.getStringValue("tb_"+dbTableName,idFieldName);
		}
		return startWithTb;
	}
	/**
	 * 根据实体对象获得主键字段名
	 * @param entityName
	 * @return
	 */
    public static String  findKeyField(TjBaseEntity be) {
		String entityName=JspElement.getEntityNameByClass(be.getClass(), false);
		return findKeyField(entityName);
	}
    /**
	 * 根据实体类获得主键字段名
	 * @param entityName
	 * @return
	 */
    public static String  findKeyField(Class<? extends TjBaseEntity> beClass) {
		String entityName=JspElement.getEntityNameByClass(beClass, false);
		return findKeyField(entityName);
	}
    /**
   	 * 数据库表名获得主键字段名
   	 * @param entityName：带有tb的驼峰后的表名
   	 * @return
   	 */
    public static String findKeyFieldBytbName(String tbTableName) {
    	String dbTableName=StringUtils.toUnderScoreCase(tbTableName);
    	String dest=keyFieldsMap.getStringValue(dbTableName);
    	
		return dest;
   	}
    /**
   	 * 数据库表名获得主键字段名
   	 * @param entityName
   	 * @return
   	 */
    public static String findKeyFieldByDbName(String dbTableName) {
    	String dest=keyFieldsMap.getStringValue(dbTableName);
    	
		return dest;
   	}
}
