package com.tengjie.common.persistence.interceptor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.property.PropertyTokenizer;
import org.apache.ibatis.scripting.xmltags.ForEachSqlNode;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLSelectQuery;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.parser.SQLParserUtils;
import com.alibaba.druid.sql.parser.SQLStatementParser;
import com.alibaba.druid.sql.visitor.SQLASTOutputVisitor;
import com.alibaba.druid.util.JdbcUtils;
import com.tengjie.common.persistence.Page;
import com.tengjie.common.persistence.PageMap;
import com.tengjie.common.persistence.dialect.Dialect;
import com.tengjie.common.utils.Reflections;
import com.tengjie.common.utils.StringUtils;


/**
 * SQL工具类
 * @author
 * @version 2013-8-28
 */
public class SQLHelper {
	
    /**
     * 对SQL参数(?)设值,参考org.apache.ibatis.executor.parameter.DefaultParameterHandler
     *
     * @param ps              表示预编译的 SQL 语句的对象。
     * @param mappedStatement MappedStatement
     * @param boundSql        SQL
     * @param parameterObject 参数对象
     * @throws SQLException 数据库异常
     */
    @SuppressWarnings("unchecked")
    public static void setParameters(PreparedStatement ps, MappedStatement mappedStatement, BoundSql boundSql, Object parameterObject) throws SQLException {
        ErrorContext.instance().activity("setting parameters").object(mappedStatement.getParameterMap().getId());
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        if (parameterMappings != null) {
            Configuration configuration = mappedStatement.getConfiguration();
            TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
            MetaObject metaObject = parameterObject == null ? null :
                    configuration.newMetaObject(parameterObject);
            for (int i = 0; i < parameterMappings.size(); i++) {
                ParameterMapping parameterMapping = parameterMappings.get(i);
                if (parameterMapping.getMode() != ParameterMode.OUT) {
                    Object value;
                    String propertyName = parameterMapping.getProperty();
                    PropertyTokenizer prop = new PropertyTokenizer(propertyName);
                    if (parameterObject == null) {
                        value = null;
                    } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                        value = parameterObject;
                    } else if (boundSql.hasAdditionalParameter(propertyName)) {
                        value = boundSql.getAdditionalParameter(propertyName);
                    } else if (propertyName.startsWith(ForEachSqlNode.ITEM_PREFIX) && boundSql.hasAdditionalParameter(prop.getName())) {
                        value = boundSql.getAdditionalParameter(prop.getName());
                        if (value != null) {
                            value = configuration.newMetaObject(value).getValue(propertyName.substring(prop.getName().length()));
                        }
                    } else {
                        value = metaObject == null ? null : metaObject.getValue(propertyName);
                    }
                    @SuppressWarnings("rawtypes")
					TypeHandler typeHandler = parameterMapping.getTypeHandler();
                    if (typeHandler == null) {
                        throw new ExecutorException("There was no TypeHandler found for parameter " + propertyName + " of statement " + mappedStatement.getId());
                    }
                    typeHandler.setParameter(ps, i + 1, value, parameterMapping.getJdbcType());
                }
            }
        }
    }

    private static String parseCountSql(String originSql){
    	StringBuffer out = new StringBuffer() ;
    	try{
  	    SQLStatementParser parser=SQLParserUtils.createSQLStatementParser(originSql,JdbcUtils.MYSQL);  
        //解析select查询  
        SQLSelectStatement sqlStatement = (SQLSelectStatement) parser.parseSelect() ;  
        SQLSelect sqlSelect = sqlStatement.getSelect() ;  
        if(!(sqlSelect.getQuery() instanceof SQLSelectQueryBlock)){
        	 return originSql;
        }
        //获取sql查询块  
        SQLSelectQueryBlock sqlSelectQuery = (SQLSelectQueryBlock)sqlSelect.getQuery() ; 
        if(sqlSelectQuery.getGroupBy()!=null){//如果最外层含有group by，则不能做以下的修改，直接返回原语句；但是没有测试，是否如果子查询有group这个也取出来？
      	  return originSql;
        }
       
        
        //创建sql解析的标准化输出  
        SQLASTOutputVisitor sqlastOutputVisitor = SQLUtils.createFormatOutputVisitor(out , null , JdbcUtils.MYSQL) ;
        
        out.append("select count(1) ");
        if(sqlSelectQuery.getFrom()!=null){
      	  out.append(" from ");
            sqlSelectQuery.getFrom().accept(sqlastOutputVisitor) ; 
        }
       
        if(sqlSelectQuery.getWhere()!=null){
      	  out.append(" where ");
      	  sqlSelectQuery.getWhere().accept(sqlastOutputVisitor) ;  
        }
    	}catch(Exception e){
    		System.out.println("分页解析错误，错误sql："+originSql);
    	}
        return out.toString();
  }
    /**
     * 查询总纪录数
     * @param sql             SQL语句
     * @param connection      数据库连接
     * @param mappedStatement mapped
     * @param parameterObject 参数
     * @param boundSql        boundSql
     * @return 总记录数
     * @throws SQLException sql查询错误
     */
    public static int getCount(final String sql, final Connection connection,
    							final MappedStatement mappedStatement, final Object parameterObject,
    							final BoundSql boundSql, Log log) throws SQLException {
    	String countSql =parseCountSql(sql);
        if(countSql.equals(sql)){//说明源语句中含有group by，parseCountSql方法原样返回
          countSql = "select count(1) from (" + sql + ") tmp_count";;
        }
        Connection conn = connection;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
        	if (log.isDebugEnabled()) {
                log.debug("COUNT SQL: " + StringUtils.replaceEach(countSql, new String[]{"\n","\t"}, new String[]{" "," "}));
            }
        	if (conn == null){
        		conn = mappedStatement.getConfiguration().getEnvironment().getDataSource().getConnection();
            }
        	ps = conn.prepareStatement(countSql);
            BoundSql countBS = new BoundSql(mappedStatement.getConfiguration(), countSql,
                    boundSql.getParameterMappings(), parameterObject);
            //解决MyBatis 分页foreach 参数失效 start
			if (Reflections.getFieldValue(boundSql, "metaParameters") != null) {
				MetaObject mo = (MetaObject) Reflections.getFieldValue(boundSql, "metaParameters");
				Reflections.setFieldValue(countBS, "metaParameters", mo);
			}
			//解决MyBatis 分页foreach 参数失效 end 
            SQLHelper.setParameters(ps, mappedStatement, countBS, parameterObject);
            rs = ps.executeQuery();
            int count = 0;
            if (rs.next()) {
                count = rs.getInt(1);
            }
            return count;
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (ps != null) {
            	ps.close();
            }
            if (conn != null) {
            	conn.close();
            }
        }
    }


    /**
     * 根据数据库方言，生成特定的分页sql
     * @param sql     Mapper中的Sql语句
     * @param page    分页对象
     * @param dialect 方言类型
     * @return 分页SQL
     */
    public static String generatePageSql(String sql, Page<Object> page, Dialect dialect) {
        if (dialect.supportsLimit()) {
            return dialect.getLimitString(sql, page.getFirstResult(), page.getMaxResults());
        } else {
            return sql;
        }
    }
    /**
     * 根据数据库方言，生成特定的分页sql
     * @param sql     Mapper中的Sql语句
     * @param page    分页对象
     * @param dialect 方言类型
     * @return 分页SQL
     */
    public static String generatePageSql(String sql, PageMap<Map<String,Object>> page, Dialect dialect) {
        if (dialect.supportsLimit()) {
            return dialect.getLimitString(sql, page.getFirstResult(), page.getMaxResults());
        } else {
            return sql;
        }
    }
    
    /** 
     * 去除hqlString的select子句。
     * @param hqlString
     * @return 
     */  
    @SuppressWarnings("unused")
	private static String removeSelect(String hqlString){
        int beginPos = hqlString.toLowerCase().indexOf("from");
        return hqlString.substring(beginPos);
    }  
      
    /** 
     * 去除hql的orderBy子句。 
     * @param hqlString
     * @return 
     */  
    @SuppressWarnings("unused")
	private static String removeOrders(String hqlString) {
        Pattern p = Pattern.compile("order\\s*by[\\w|\\W|\\s|\\S]*", Pattern.CASE_INSENSITIVE);  
        Matcher m = p.matcher(hqlString);
        StringBuffer sb = new StringBuffer();  
        while (m.find()) {  
            m.appendReplacement(sb, "");  
        }
        m.appendTail(sb);
        return sb.toString();  
    }
    
}
