package com.tengjie.common.persistence.interceptor;

import java.lang.reflect.Constructor;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.CachingExecutor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ReuseExecutor;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.MappedStatement.Builder;
import org.apache.ibatis.mapping.ParameterMap;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandler;
import org.hibernate.validator.internal.util.TypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alibaba.druid.sql.PagerUtils;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.SQLUtils.FormatOption;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
import com.alibaba.druid.sql.ast.expr.SQLCaseExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLJoinTableSource;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.dialect.mysql.ast.expr.MySqlExprImpl;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlSelectParser;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlSchemaStatVisitor;
import com.alibaba.druid.sql.dialect.postgresql.visitor.PGSchemaStatVisitor;
import com.alibaba.druid.sql.parser.SQLParserUtils;
import com.alibaba.druid.sql.parser.SQLStatementParser;
import com.alibaba.druid.sql.visitor.SQLASTOutputVisitor;
import com.alibaba.druid.sql.visitor.SchemaStatVisitor;
import com.alibaba.druid.stat.TableStat;
import com.alibaba.druid.stat.TableStat.Column;
import com.alibaba.druid.stat.TableStat.Condition;
import com.alibaba.druid.stat.TableStat.Name;
import com.alibaba.druid.stat.TableStat.Relationship;
import com.alibaba.druid.util.JdbcConstants;
import com.alibaba.druid.util.JdbcUtils;
import com.tengjie.common.config.Global;
import com.tengjie.common.persistence.TjBaseEntity;
import com.tengjie.common.persistence.CustMySqlSchemaStatVisitor;
import com.tengjie.common.persistence.JoinTableBean;
import com.tengjie.common.persistence.ModifySelect;
import com.tengjie.common.persistence.util.PrintDebugSql;
import com.tengjie.common.utils.MacUtils;
import com.tengjie.common.utils.MyBeanUtils;
import com.tengjie.common.utils.Reflections;
import com.tengjie.common.utils.StringUtils;


@Intercepts(
	    {
	        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
	        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
	    }
	)
/**@Signature(type = Executor.class, method = "update", args = {
    MappedStatement.class, Object.class })})**/
public class MapperModifyPlugin extends BaseInterceptor {
	  private static final long serialVersionUID = 1L;
	static int MAPPED_STATEMENT_INDEX = 0;// 这是对应上面的args的序号
	static int PARAMETER_INDEX = 1;
	static int ROWBOUNDS_INDEX = 2;
	static int RESULT_HANDLER_INDEX = 3;
	String dialectClass;// 在mybatise-config的配制属性，可多个
    public  static boolean showPrintSql=false;

	static{
		String temp=Global.getConfig("showPrintSql");
    	if("true".equals(temp)||StringUtils.isEmpty(temp))showPrintSql=true;
	}
	
	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		//final Executor executor = (Executor) invocation.getTarget();
		final Object[] queryArgs = invocation.getArgs();
		final MappedStatement ms = (MappedStatement) queryArgs[MAPPED_STATEMENT_INDEX];
		 Object parameter = queryArgs[PARAMETER_INDEX];
		  if(parameter instanceof Map) {//ruoyi项目的分页信息，或将查询实体转换为bean，因此需要再还原回来，或者使用我们的分页查询
			    if(((Map)parameter).containsKey("entityGenericType")) {
			    	Class destclass=(Class) ((Map)parameter).get("entityGenericType");
			    	String returnRsTypeName=destclass.getName();
		        	Object newparam=Class.forName(returnRsTypeName).newInstance();
		        	parameter=MyBeanUtils.copyMap2BeanNotNull(newparam, (Map)parameter,true);
			    }
			    
	        }
		 if(parameter instanceof TjBaseEntity){
	    		if(((TjBaseEntity)parameter).getJoinTableBeanList().size()<1){
	    			if(StringUtils.isNotEmpty(((TjBaseEntity)parameter).getAppendWhereCondSql().toString())||((TjBaseEntity)parameter).getOrderyByMap().size()>0||((TjBaseEntity)parameter).isClearSelectField()||((TjBaseEntity)parameter).getModifyAlias().size()>0||((TjBaseEntity)parameter).getIncludeSelectFieldMap().size()>0||((TjBaseEntity)parameter).getAddSelectField().size()>0||((TjBaseEntity)parameter).getRemoveSelectField().size()>0||((TjBaseEntity)parameter).getOnlyOr().size()>0){//还可以附加sql
	    				
	    			} else{
	    				//printSqlAppendParam("**",ms,ms.getBoundSql(parameter));
	    				return invocation.proceed();
	    			}
	    		}
	    	}else{
	    	//	printSqlAppendParam("**",ms,ms.getBoundSql(parameter));
	    		return invocation.proceed();
	    	}
		
		final RowBounds rowBounds = (RowBounds) queryArgs[ROWBOUNDS_INDEX];
		//final PageBounds pageBounds = new PageBounds(rowBounds);
		final BoundSql boundSql = ms.getBoundSql(parameter);// 获得查询语句对像
		String sql = boundSql.getSql();// 获得查询语句
			//appendJoinTables(sql,parameter);
		sql=appendJoinSql(sql,((TjBaseEntity)parameter).getJoinTableBeanList(),parameter);
		
		
	//	dealAddParameterMapping(boundSql,ms.getConfiguration());
		//boundSql.setAdditionalParameter("2", "woca");
		BoundSql newBoundSql = new BoundSql(ms.getConfiguration(), sql,boundSql.getParameterMappings(),boundSql.getParameterObject());// 重新new一个查询语句对像
		//newBoundSql.setAdditionalParameter("2", "woca");
		MappedStatement newMs = copyFromMappedStatement(ms,new BoundSqlSqlSource(newBoundSql));// 把新的查询放到statement里
		printSqlAppendParam("",newMs,newBoundSql);
		queryArgs[MAPPED_STATEMENT_INDEX] = newMs;
		queryArgs[5] = newBoundSql;
//		String methonName = invocation.getMethod().getName();
//		System.out.println("methodName__________:" + methonName);
		return invocation.proceed();
	}
	private String appendJoinSql(String originSql, List<JoinTableBean> jtbs,Object parameter) throws Exception{
		ModifySelect ms=new ModifySelect( originSql,  jtbs, parameter);
		StringBuilder sb=new StringBuilder();
		sb.append(ms.appendSelect());
		sb.append(ms.appendJoin());
		sb.append(ms.appendWhere());
		sb.append(ms.appendGroupBy());
		sb.append(ms.appendOrder());
		// System.out.println("修改后sql_________________:\n"+SQLUtils.format(sb.toString(), JdbcUtils.MYSQL));
		return sb.toString();
	}
	   /**
		 * 为sql拼接值，目的是告诉开发人员哪些字段没有传值进来
		 * @param modifySql
		 */
	   public void printSqlAppendParam(String prefix,final MappedStatement ms,BoundSql boundSql){
		   if(!showPrintSql)return;
		   String sqlId = ms.getId();
		   String sql = PrintDebugSql.getSql(ms.getConfiguration(), boundSql, sqlId);
		   if(sql.contains("SQLCaseExpr")) {// PrintDebugSql.getSql有未解析的表达式，后续要看看，现在先不处理，是不是要参照正式解析那里
			   return;
		   }
		   try{
		   if(StringUtils.isEmpty(prefix)){
			   System.out.println("未格式化前：["+sqlId+"]:\n"+sql);
			   String format="";
			   try {
				 //  format= SQLUtils.formatMySql(sql);
				   List<SQLStatement> statementList = SQLUtils.toStatementList(sql, JdbcUtils.MYSQL);
				   format= SQLUtils.toSQLString(statementList, JdbcUtils.MYSQL, new FormatOption());
			   }catch (com.alibaba.druid.sql.parser.ParserException e) {
				   format="无法格式化该sql:"+sql;
			}
			   System.err.println("拼接参数后sql____________：["+sqlId+"]:\n"+format);
		   }else{
			   if(sqlId.contains("dao.RoleDao")||sqlId.contains("dao.MenuDao")||sqlId.contains("dao.RoleDao")||sqlId.contains("dao.UserDao")||sqlId.contains("dao.DictDao"))
				   return;
			   try{
				  // SQLUtils.format(sql, JdbcUtils.MYSQL);
			   }catch(Exception e){
				   System.err.println("sql:有错误，无法打印："+sql);return ;
			   }
			   //System.out.println(prefix+"：["+sqlId+"]:\n"+SQLUtils.format(sql, JdbcUtils.MYSQL));
		   }
		   }catch(Exception e){
			   
			   System.err.println("sql:有错误，无法打印："+sql);
		   }
		   
	   }
	private void dealAddParameterMapping(BoundSql boundSql,Configuration configuration){
		List<ParameterMapping> pmlist= boundSql.getParameterMappings();
		Object obj=boundSql.getParameterObject();
		
		
	    TypeHandler<?> th=null;
		 for (ParameterMapping pm : boundSql.getParameterMappings()) {
			 String propertyName = pm.getProperty();
			 th=pm.getTypeHandler();
			// pm.getResultMapId()
			 //System.out.println("propertyName:::"+propertyName);
//		      if (rmId != null) {
//		       // ResultMap rm = boundSql.getResultMap(rmId);
//		      }
		 }
		// Class<?> javaTypeClass = resolveParameterJavaType(parameterType, property, javaType, jdbcType); 
		// TypeHandler<?> typeHandlerInstance = resolveTypeHandler(javaTypeClass, typeHandler); 
		 ParameterMapping.Builder builder= new ParameterMapping.Builder(configuration, dialectClass, th);
//		 builder.jdbcType(jdbcType);  
//		    builder.resultMapId(resultMap);  
//		    builder.mode(parameterMode);  
//		    builder.numericScale(numericScale);  
//		    builder.typeHandler(typeHandlerInstance);
		 ParameterMapping newpm=builder.build();
		 boundSql.getParameterMappings().add(newpm);
		
//		  if (Reflections.getFieldValue(boundSql, "metaParameters") != null) {
//             MetaObject mo = (MetaObject) Reflections.getFieldValue(boundSql, "metaParameters");
//             Reflections.setFieldValue(newBoundSql, "metaParameters", mo);
//         }

		//System.out.println("");
	}
  
    public static String appendJoinTables(String sql,Object parameter ){
    	if(parameter instanceof TjBaseEntity){
    		
    	}else{
    		return sql;
    	}
        if(parameter instanceof Map){
    		
    	}
    	  StringBuffer from=new StringBuffer();  
          SQLStatementParser parser=SQLParserUtils.createSQLStatementParser(sql,JdbcUtils.MYSQL);  
          List<SQLStatement> stmtList=parser.parseStatementList();  
          SQLASTOutputVisitor visitor=SQLUtils.createFormatOutputVisitor(from,stmtList,JdbcUtils.MYSQL);  
          CustMySqlSchemaStatVisitor ssVisitor=new CustMySqlSchemaStatVisitor();

          List<SQLSelectItem> sL=new ArrayList<SQLSelectItem>();  
          SQLStatement stmt=stmtList.iterator().next();  
          SQLSelectStatement sstmt=(SQLSelectStatement)stmt;  
          SQLSelect sqlselect=sstmt.getSelect();  
  
          if(sqlselect.getQuery() instanceof SQLSelectStatement) {
        	  
          }else{//else if (sqlSelectQuery instanceof MySqlUnionQuery) { 
        	  
          }
          SQLSelectQueryBlock sqb=(SQLSelectQueryBlock)sqlselect.getQuery();  
          SQLTableSource fromx=sqb.getFrom();  
//         SQLJoinTableSource sjts=new SQLJoinTableSource();
//         sjts.setJoinType(SQLJoinTableSource.JoinType.LEFT_OUTER_JOIN);
//         sjts.setLeft(fromx);
//         SQLExprTableSource sets=new SQLExprTableSource();
//         sets.setAlias("wocao");
//         SQLIdentifierExpr sle=new SQLIdentifierExpr();
//         sle.setName("mytable");
//         sets.setExpr(sle);
//         sjts.setRight(sets);
//         SQLBinaryOpExpr condition=new SQLBinaryOpExpr();
//         SQLExpr left;
//    
//		condition.setLeft(left);
////         condition.setOperator(new SQLBinaryOperator());
////         condition.setRight(right);
//         sjts.setCondition(condition);
//          fromx.get
//          fromx.accept(ssVisitor);  
          TableStat ts=new TableStat();
          Name nn=new Name("mytable");
          ssVisitor.getTables().put(nn, ts);
        
         ssVisitor.getAliasMap().put("tbalias", "mytable");
          
        
         Column leftC=new Column("a","id");
         Column rightC=new Column("tbalias","kk");
         Relationship e=new Relationship(leftC,rightC,SQLBinaryOperator.Equality.name);
         
          ssVisitor.getRelationships().add(e);
          StringBuffer test=new StringBuffer();
         
//          fromx.output(");
        fromx.accept(visitor);  
          fromx.output(test);
          fromx.accept(ssVisitor);  
        //  fromx.getAttributes()
          SQLExprTableSource sets;
       //   MySqlExprImpl expr;expr.
          SQLUtils.toSQLExpr(sql);
        //  PagerUtils.count(sql, dbType)
          sL=sqb.getSelectList();  
        
    
//          ssVisitor.
          StringBuffer sb=new StringBuffer();  
          for (  SQLSelectItem sqi : sL) {  
              if (sqi.getExpr() instanceof SQLCaseExpr) {  
                  sb.append(sqi.getAlias()).append(",");  
              }  
              else {  
                  sb.append(sqi.toString()).append(",");  
              }  
          }  
         // JoinParser  joinParser=new JoinParser(sqb,sql);
         // System.out.println("* ???? : " + sb.toString().substring(0, sb.toString().length() - 1));  
          //sourceAnalysis(fromx,apMap);  
          return "";
    }
	@Override
	public Object plugin(Object target) {
		return Plugin.wrap(target, this);// 这些莫认调用
	}

//	@Override
//	public void setProperties(Properties properties) {
//		// TODO Auto-generated method stub
//		PropertiesHelper propertiesHelper = new PropertiesHelper(properties);
//		String dialectClass = propertiesHelper
//				.getRequiredString("dialectClass");
//		setDialectClass(dialectClass);// 这个方法主要就是获得mybatis-config里的配制
//		// 比如：<!-- <plugin
//		// interceptor="com.teamsun.net.common.utils.MyBatisePlugin">
//		// <property name="dialectClass"
//		// value="com.github.miemiedev.mybatis.paginator.dialect.OracleDialect"
//		// />
//		// </plugin> -->
//		// 上面是在mybatise-config的配制
//	}

//	public void setDialectClass(String dialectClass) {
//	
//		this.dialectClass = dialectClass;
//	}

	private MappedStatement copyFromMappedStatement(MappedStatement ms,
			SqlSource newSqlSource) {
		Builder builder = new MappedStatement.Builder(ms.getConfiguration(),
				ms.getId(), newSqlSource, ms.getSqlCommandType());

		builder.resource(ms.getResource());
		builder.fetchSize(ms.getFetchSize());
		builder.statementType(ms.getStatementType());
		builder.keyGenerator(ms.getKeyGenerator());
		if (ms.getKeyProperties() != null && ms.getKeyProperties().length > 0) {
			builder.keyProperty(ms.getKeyProperties()[0]);
		}
		// setStatementTimeout()
		builder.timeout(ms.getTimeout());

		// setStatementResultMap()
		builder.parameterMap(ms.getParameterMap());

		// setStatementResultMap()
		builder.resultMaps(ms.getResultMaps());
		builder.resultSetType(ms.getResultSetType());

		// setStatementCache()
		builder.cache(ms.getCache());
		builder.flushCacheRequired(ms.isFlushCacheRequired());
		builder.useCache(ms.isUseCache());

		return builder.build();
	}
	 @Override
	    public void setProperties(Properties properties) {
	        super.initProperties(properties);
	    }

	public static class BoundSqlSqlSource implements SqlSource {

		private BoundSql boundSql;

		public BoundSqlSqlSource(BoundSql boundSql) {
			this.boundSql = boundSql;
		}

		public BoundSql getBoundSql(Object parameterObject) {
			return boundSql;
		}
	}

}