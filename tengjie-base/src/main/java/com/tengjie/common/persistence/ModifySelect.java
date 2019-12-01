package com.tengjie.common.persistence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.expression.spel.ast.Operator;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLExprImpl;
import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLAggregateExpr;
import com.alibaba.druid.sql.ast.expr.SQLBetweenExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
import com.alibaba.druid.sql.ast.expr.SQLCaseExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLInListExpr;
import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.expr.SQLQueryExpr;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLSelectGroupByClause;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlSchemaStatVisitor;
import com.alibaba.druid.sql.dialect.postgresql.visitor.PGSchemaStatVisitor;
import com.alibaba.druid.sql.parser.SQLExprParser;
import com.alibaba.druid.sql.parser.SQLParserUtils;
import com.alibaba.druid.sql.parser.SQLStatementParser;
import com.alibaba.druid.sql.visitor.SQLASTOutputVisitor;
import com.alibaba.druid.sql.visitor.SchemaStatVisitor;
import com.alibaba.druid.stat.TableStat;
import com.alibaba.druid.stat.TableStat.Column;
import com.alibaba.druid.stat.TableStat.Condition;
import com.alibaba.druid.stat.TableStat.Name;
import com.alibaba.druid.util.JdbcUtils;
import com.google.common.collect.Lists;
import com.tengjie.common.utils.DateUtils;
import com.tengjie.common.utils.MyBeanUtils;
import com.tengjie.common.utils.MyStringBuffer;
import com.tengjie.common.utils.Reflections;
import com.tengjie.common.utils.StringUtils;
import com.tengjie.common.utils.TjMap;

//OutputVisitor用来把AST输出为字符串
//WallVisitor 来分析SQL语意来防御SQL注入攻击
//ParameterizedOutputVisitor用来合并未参数化的SQL进行统计
//EvalVisitor 用来对SQL表达式求值
//ExportParameterVisitor用来提取SQL中的变量参数
//SchemaStatVisitor 用来统计SQL中使用的表、字段、过滤条件、排序表达式、分组表达式
//SQL格式化 Druid内置了基于语义的SQL格式化功能
public class ModifySelect {
	protected String originSql;
	protected List<JoinTableBean> jtbs;
	protected  List<SQLStatement> stmtList;
	protected StringBuffer allSql=new StringBuffer();
	protected SQLStatement stmt;
	protected SQLSelectStatement sstmt;
	protected SQLSelectQueryBlock sqb;
	protected  SQLSelect sqlselect;  
	protected String blank=" ";
	protected String currEntityTableName;//当前实体表明,即主表名
	protected CustMySqlSchemaStatVisitor sqbSsVisitor;//sqbSsVisitor非常有用，

	protected Object parameter;
	protected Map<String,String> notInOr;//对应baseentity中的notInOr
  	//  SQLASTOutputVisitor visitor=SQLUtils.createFormatOutputVisitor(orderSql,stmtList,JdbcUtils.MYSQL);  
  	 
	public ModifySelect(String originSql, List<JoinTableBean> jtbs,Object parameter ) {
		super();
		try {
		
		this.originSql = originSql;
		this.jtbs = jtbs;
		SQLStatementParser parser=SQLParserUtils.createSQLStatementParser(originSql,JdbcUtils.MYSQL);  
		List<SQLStatement> stmtList=parser.parseStatementList();
		
	    stmt=stmtList.iterator().next();  
	    sstmt=(SQLSelectStatement)stmt;  
	    sqlselect=sstmt.getSelect(); 
	    sqb=(SQLSelectQueryBlock)sqlselect.getQuery();  
	    sqbSsVisitor=new CustMySqlSchemaStatVisitor();
	    stmt.accept(sqbSsVisitor);
	    this.parameter=parameter;
	    this.notInOr=((TjBaseEntity)parameter).getNotInOr();
	    sqb.accept(sqbSsVisitor);
//	    Map<Name,TableStat> tables=sqbSsVisitor.getTables();
	    currEntityTableName= StringUtils.toCamelCase(sqb.getFrom().toString());
		}catch (Exception e) {
			e.printStackTrace();
			System.err.println("sql:【"+originSql+"】出现错误！");
		}
	}
	
	private String getAliasByExistTableName(String tableName){
		Map<String,String> aliasMap=   sqbSsVisitor.getAliasMap();
		String tableAliasName="";
		 for (Map.Entry<String, String> entry : aliasMap.entrySet()) { 
			 //子查询时，value是null
			 if(entry.getValue()!=null&&entry.getValue().equals(entry.getKey()))continue;
			 if(entry.getValue()!=null&&entry.getValue().equals(tableName)){
				 tableAliasName=entry.getKey();
				 break;
			 }
		 }
		 return tableAliasName;
	}
	//从mapper配置文件中取出的被动过的任何sql，如果是from中是子查询时，getAliasByExistTableName是取不出表别名的，因为子查询的aliasMap的value是空，所以这个方法是
	//从取value为空的作为主表的别名，但是如果有多个子查询呢？目前就先抛错，要求在关联的时候手动指定
	private String getAliasForSubQuery() throws Exception{
		Map<String,String> aliasMap=   sqbSsVisitor.getAliasMap();
		String tableAliasName="";
	
		 for (Map.Entry<String, String> entry : aliasMap.entrySet()) { 
			 //子查询时，value是null
			 if(entry.getValue()==null){
				 tableAliasName=entry.getKey();
			
//				 if(i>1){
//					// throw new Exception("从mapper中获得的sql有多个子查询，系统无法获得对应别名，请手工指定！");
//				 }
			 }
		 }
		 return tableAliasName;
	}
	
    public String appendJoin() throws Exception{
    	StringBuffer joinSql=new StringBuffer(); 
        SQLTableSource fromx=sqb.getFrom(); 
        SQLASTOutputVisitor visitor=SQLUtils.createFormatOutputVisitor(joinSql,stmtList,JdbcUtils.MYSQL);  
        fromx.accept(visitor);  
       // joinSql.append(visitor.toString());
        SchemaStatVisitor ssVisitor=SQLUtils.createSchemaStatVisitor(stmtList, JdbcUtils.MYSQL);
        fromx.accept(ssVisitor);
        String currEntityTableNameAlias=getAliasByExistTableName(currEntityTableName);
       if(StringUtils.isEmpty(currEntityTableNameAlias))currEntityTableNameAlias=getAliasForSubQuery();
        	for(int i=0;i<jtbs.size();i++){
        	 	JoinTableBean jtb=null ;
        		if(jtbs.get(i) instanceof JoinTableBean){
        			jtb=jtbs.get(i); 
  
            	}else{
          			jtb=new JoinTableBean();
            		MyBeanUtils.copyBean2Bean(jtb, jtbs.get(i));
            	}
        		joinSql.append(jtb.getJoinKind()+blank);
        		if(jtb.isIfSubQuery()){
        			joinSql.append("("+jtb.getTableName()+")"+blank);
        		}else{
        			joinSql.append(StringUtils.toUnderScoreCase(jtb.getTableName())+blank);
        		}
        		joinSql.append(jtb.getTableAlias()+blank);
        		List<JoinOnBean> jobList=jtb.getOnConditions();
        		if(jobList.size()>0){
        				for(int t=0;t<jobList.size();t++){
        					JoinOnBean jobean=null;
        					if(jobList.get(t) instanceof JoinOnBean ){
        						jobean=jobList.get(t) ;
        					}else{
        						jobean=new JoinOnBean();
        						MyBeanUtils.copyBean2Bean(jobean, jobList.get(t));
        					}
        						 
        				String customRelaTableNameAlias=currEntityTableNameAlias;
        				if(StringUtils.isNotEmpty(jtb.getMainTableName())){
        					customRelaTableNameAlias=jtb.getMainTableName();
       				    }
        				if(t==0){
        					joinSql.append("on"+blank);
        					joinSql.append(jobean.findExpress(customRelaTableNameAlias,jtb.getTableAlias(),jtb,parameter));
        				}else{
        					String expressCond=jobean.findExpress(customRelaTableNameAlias,jtb.getTableAlias(),jtb,parameter);
        					if(StringUtils.isNotEmpty(expressCond)){
        						joinSql.append(blank+jobean.getAndOr()+blank);
        						joinSql.append(expressCond);
        					}
        				
        				}
        				 
        			
        			
        			}
        		}
        		
        		
        	}
        	joinSql.insert(0, " from ");
    	return joinSql.toString();
    }
    public String appendSelect() throws Exception{
    	StringBuffer selSql=new StringBuffer(); 
    	selSql.append(" select ");
    	if(((TjBaseEntity)parameter).isAppendDistinct()){
    		selSql.append("DISTINCT ");
    	}
    	List<SQLSelectItem> sL=new ArrayList<SQLSelectItem>();  
    	 sL=sqb.getSelectList(); 
 		if(!((TjBaseEntity)parameter).isClearSelectField()){
 			if(((TjBaseEntity)parameter).getIncludeSelectFieldMap().size()<1){
 				 Map<String,String> modifyAliasMap=((TjBaseEntity)parameter).getModifyAlias();
 	 			 Map<String,String> modifySelectMap=((TjBaseEntity)parameter).getModifySelectField();
 	 			Map<String,String> removeSelectFieldMap=((TjBaseEntity)parameter).getRemoveSelectField();
 	 			
 	 			 if(!"2".equals(((TjBaseEntity)parameter).getResultType())){//如果调用的不是返回map的方法 		
 	 				  for (  SQLSelectItem sqi : sL) {  
 	 	 		        	
 	 	 		            if (sqi.getExpr() instanceof SQLCaseExpr) {  
 	 	 		            	String appSql=SQLUtils.toSQLString(sqi.getExpr())+" as "+sqi.getAlias();
 	 	 		            	 appSql=dealModifySelectField(appSql,modifySelectMap, removeSelectFieldMap);
 	 	 		            	 if(StringUtils.isEmpty(appSql))continue;
 	 	 		            	if(modifyAliasMap.size()<1){
 	 	 		            		selSql.append(appSql).append(",");  
 	 	 		            	}else{
 	 	 		            		dealModifyAlias(selSql,appSql,modifyAliasMap);
 	 	 		            	}
 	 	 		            	
 	 	 		            } else if(sqi.getExpr() instanceof SQLAggregateExpr) {
 	 	 		            	String appSql=SQLUtils.toSQLString(sqi.getExpr())+" as "+sqi.getAlias();
 	 	 		            	 appSql=dealModifySelectField(appSql,modifySelectMap, removeSelectFieldMap);
 	 	 		            	if(modifyAliasMap.size()<1){
 	 	 		            		selSql.append(appSql).append(",");  //对于如count(1)这种情况
 	 	 		            	}else{
 	 	 		            		dealModifyAlias(selSql,appSql,modifyAliasMap);
 	 	 		            	}
 	 	 		            	
 	 	 		            } else if(sqi.getExpr() instanceof SQLBinaryOpExpr){
 	 	 		            	String appSql=SQLUtils.toSQLString(sqi.getExpr())+" as "+sqi.getAlias();
 	 	 		            	 appSql=dealModifySelectField(appSql,modifySelectMap, removeSelectFieldMap);
 	 	 		             	if(modifyAliasMap.size()<1){
 	 			            		selSql.append(appSql).append(",");  //对于如count(1)这种情况
 	 			            	}else{
 	 			            		dealModifyAlias(selSql,appSql,modifyAliasMap);
 	 			            	}
// 	 	 		            	SQLBinaryOpExpr  expr=(SQLBinaryOpExpr)sqi.getExpr() ;
// 	 	 		            	System.out.println("-----");
 	 	 		            } else if(sqi.getExpr() instanceof SQLQueryExpr) { 
 	 	 		            	String appSql=SQLUtils.toSQLString(sqi.getExpr())+" as "+sqi.getAlias();
 	 	 		            	 appSql=dealModifySelectField(appSql,modifySelectMap, removeSelectFieldMap);
 	 	 		            	if(modifyAliasMap.size()<1){
 	 			            		selSql.append(appSql).append(",");  //对于如子查询
 	 			            	}else{
 	 			            		dealModifyAlias(selSql,appSql,modifyAliasMap);
 	 			            	}
 	 	 		            }else if(sqi.getExpr() instanceof SQLMethodInvokeExpr) { //嵌套复合型的
 	 	 		            	String as="";
 	 	 		            	if(StringUtils.isNotEmpty(sqi.getAlias()))as=" as "+sqi.getAlias();
 	 	 		            	selSql.append(SQLUtils.toSQLString(sqi.getExpr())+as).append(",");
 	 	 		            } else{
 	 	 		            	String appSql=sqi.toString();
 	 	 		            	 appSql=dealModifySelectField(appSql,modifySelectMap, removeSelectFieldMap);
 	 	 		            	if(modifyAliasMap.size()<1){
 	 	 		            		selSql.append(appSql).append(",");  
 	 	 		            	}else{
 	 	 		            		dealModifyAlias(selSql,appSql,modifyAliasMap);
 	 	 		            	}
 	 	 		            	
 	 	 		            }  
 	 	 		        }  
 	 			 }else{//说明是要map 类型的groupby 他的IncludeSelectFieldMap肯定是小于1的
 	 			      if(((TjBaseEntity)parameter).getGroupBySelectField().size()>0){
 	 			    	for (  SQLSelectItem sqi : sL) {  
 	 				 		selSql.append(sqi.toString()+",");
 	 				    }
 	 			      }
 	 			 }
 			}else{
 				Map<String,String> temp=((TjBaseEntity)parameter).getIncludeSelectFieldMap();
 				for (Map.Entry<String, String> entry : temp.entrySet()) { 
 				  // String currEntityTableNameAlias=getAliasByExistTableName(currEntityTableName);
 					String key=entry.getKey();
 					if(ifBaseSelectField(key)){
 						selSql.append(blank+"a."+entry.getKey()+blank);
 					}else{
 						selSql.append(blank+key+blank);
 					}
 					selSql.append("as"+blank+entry.getValue()+blank+",");
 				}

 			}
 			
 		}
 		
 		Map<String,String> addSelectFieldMap=((TjBaseEntity)parameter).getAddSelectField();
 		for(Map.Entry<String, String> entry : addSelectFieldMap.entrySet()){
 		    String mapKey = entry.getKey();
 		    String mapValue = entry.getValue();
 		    if(StringUtils.isNotEmpty(mapValue)) {
 		    	 selSql.append(mapKey+" as "+mapValue+blank+",");
 		    }else {
 		    	 selSql.append(mapKey+blank+",");
 		    }
 		   
 		}
 		
        for(int i=0;i< jtbs.size();i++){
        	JoinTableBean jtb=null ;
        	//本段代码主要是为了服务自动生成接口部分的在线生成代码并执行部分，因为用javassist执行的代码到这里即使是
        	//相同的JoinTableBean类型，也会存在转换异常，即使用instanceof，可能是两个jvm的原因。
        	if (!(jtbs.get(i)  instanceof JoinTableBean)) {
        		
        		jtb=new JoinTableBean();
        		MyBeanUtils.copyBean2Bean(jtb, jtbs.get(i));
        	}else{
        		jtb=jtbs.get(i); 
        	}
        	for (Map.Entry<String, String> entry :( jtb.getSelectMap()).entrySet()) { 
        		  String fieldName=StringUtils.toUnderScoreCase(entry.getKey())+blank;
        		  selSql.append(parseTableAliasAndField(jtb.getTableAlias(), fieldName));
        		  if(StringUtils.isEmpty(entry.getValue())){
        			  selSql.append("as"+blank+entry.getKey()+blank+",");
        		  }else{
        			  selSql.append("as"+blank+entry.getValue()+blank+",");
        		  }
        		}
        }
        
    	return selSql.toString().substring(0, selSql.toString().length() - 1);
    }
    /**
     * 判断select中语句，比如a.id，就是标准的类型，其他的比如count(a.id)等就不是标准类型，目前只在IncludeSelectFieldMap中判断，其他还没有应用
     * @param selectFieldWord
     * @return
     */
    private boolean ifBaseSelectField(String selectFieldWord){
    	//SQLExprParser sep=new SQLExprParser(selectFieldWord,"mysql");
    	boolean bl=true;
    	if(selectFieldWord.contains("(")&&selectFieldWord.contains(")"))bl=false;
    	if(selectFieldWord.contains("case")&&selectFieldWord.contains("when"))bl=false;
//    	if (sep.expr() instanceof SQLCaseExpr||sep.expr() instanceof SQLAggregateExpr||sep.expr() instanceof SQLBinaryOpExpr||sep.expr() instanceof SQLQueryExpr) { 
//    		bl=false;
//    	}
    	return bl;
    }
    /**
     * 替换固化在mapper文件中的select  a.name  as name 为 select cast (a.name等等
     * 新加入了处理removeSelectFieldMap，对于指定字段进行移除
     * @param selSql
     * @param sqlExprValue
     * @param includeFiled
     */
    private String dealModifySelectField(String sqlExprValue, Map<String,String> modifySelectMap,Map<String,String> removeSelectFieldMap){
    	if(removeSelectFieldMap.size()<1&&modifySelectMap.size()<1)return sqlExprValue;
    	String[] temp=null;
    	StringBuffer tempSb=new StringBuffer();
    	if(sqlExprValue.contains(" AS ")){
    		temp=sqlExprValue.split(" AS ");
    	}
    	if(sqlExprValue.contains(" as ")){
    		temp=sqlExprValue.split(" as ");
    	}
    	String fieldname=parseSelectFieldName(temp[0]);//.replace("\"", "");
    	if(removeSelectFieldMap.containsKey(fieldname)){
    		return "";
    	}
    	if(modifySelectMap.containsKey(fieldname)){
			tempSb.append(modifySelectMap.get(fieldname));//替换成所需的
			if(temp.length>1){
				tempSb.append(" AS "+temp[1]);
			}
        }else{
			tempSb.append(sqlExprValue);
		}	
 		
 		return tempSb.toString();
    }
    /**
     * 替换固化在mapper文件中的where a.name 变为 dateFormat(a.name.....
     * @param selSql
     * @param sqlExprValue
     * @param includeFiled
     */
    private void dealModifyWhereField(StringBuffer selSql, SQLExpr sqle,Map<String,String> includeFiled){
    	//String kk=cond.getColumn().getTable()
    	//ifHasFieldInExpr()
		if(sqle instanceof SQLBinaryOpExpr){
			SQLBinaryOpExpr dest=(SQLBinaryOpExpr)sqle;
			if(dest.getLeft() instanceof SQLBinaryOpExpr){
				selSql.append("( "+SQLUtils.toMySqlString(sqle)+" )");
				
			}else{
				selSql.append(SQLUtils.toMySqlString(sqle));
			}
		}else{
			selSql.append(SQLUtils.toMySqlString(sqle));
		}
	}

    /**
     * 数据库出来的a.user_name,去掉a并驼峰返回
     * @param originDbFieldName
     */
    private String parseSelectFieldName(String originDbFieldName){
    	originDbFieldName=originDbFieldName.substring(originDbFieldName.indexOf(".")+1);
    	originDbFieldName=originDbFieldName.replace("\"", "");
    	if(originDbFieldName.contains("_")){
    		originDbFieldName=StringUtils.toCamelCase(originDbFieldName);
			}
    	return originDbFieldName;
    }
    private void dealModifyAlias(StringBuffer selSql,String sqlExprValue, Map<String,String> modifyAliasMap){
    	String[] temp=null;
    	if(sqlExprValue.contains(" AS ")){
    		temp=sqlExprValue.split(" AS ");
    	}else
    	if(sqlExprValue.contains(" as ")){
    		temp=sqlExprValue.split(" as ");
    	}else{
    		temp=new String[]{sqlExprValue};
    	}

 		if(temp.length<2){//没有as
 			String fieldname=parseSelectFieldName(temp[0]);
 			
 			if(modifyAliasMap.containsKey(fieldname)){
 				selSql.append(temp[0]);
 				if(StringUtils.isNotEmpty(modifyAliasMap.get(fieldname))){
 					selSql.append(" AS "+modifyAliasMap.get(fieldname));
 				}
 				selSql.append(",");  
 			}else{
 				selSql.append(sqlExprValue).append(",");
 			}
 		}else{
 			String fieldname=parseSelectFieldName(temp[0]);//.replace("\"", "");
 			if(modifyAliasMap.containsKey(fieldname)){
 				selSql.append(temp[0]);
 				if(StringUtils.isNotEmpty(modifyAliasMap.get(fieldname))){
 					selSql.append(" AS "+modifyAliasMap.get(fieldname));
 				}else{
 					selSql.append(" AS "+temp[1]);
 				}
 				selSql.append(",");  
 			}else{
 				selSql.append(sqlExprValue).append(",");
 			}
 		}
    }
//    public void dealOrParseWhere(SQLExpr se,List<String> astList){
//    	if (se instanceof SQLBinaryOpExpr ){
//    		SQLBinaryOpExpr  allwhere = (SQLBinaryOpExpr ) se;
//    		SQLExpr left=allwhere.getLeft();
//    		SQLExpr right=allwhere.getRight();
//    		if(left instanceof SQLPropertyExpr||right instanceof SQLPropertyExpr){
//    			if(left instanceof SQLBinaryOpExpr){
//                	dealOrParseWhere(left,astList);
//                	
//                }
//                if(right instanceof SQLBinaryOpExpr){
//                	dealOrParseWhere(right,astList);
//                	
//                }
//    		}else{
//    			StringBuffer propSB=new StringBuffer();
//        		SQLASTOutputVisitor propVistor=new SQLASTOutputVisitor(propSB);
//        		allwhere.accept(propVistor);
//        		astList.add(propSB.toString());
//    		}
//    		
//            
//            
//    		
//    	}else if(se instanceof SQLBetweenExpr ){
//    		SQLBetweenExpr  be = (SQLBetweenExpr ) se;
//    		System.out.println(be.getTestExpr()+"----"+be.getBeginExpr()+"---"+be.getEndExpr());
//    	}else {
//    		StringBuffer propSB=new StringBuffer();
//    		SQLASTOutputVisitor propVistor=new SQLASTOutputVisitor(propSB);
//    		se.accept(propVistor);
//    		astList.add(propSB.toString());
//    		System.out.println("未知"+se.toString());
//    	}
//    }
    public String appendWhere() throws Exception{
    	StringBuffer whereSql=new StringBuffer(); 
    	if(sqb.getWhere()!=null){	
    		SQLASTOutputVisitor visitor=SQLUtils.createFormatOutputVisitor(whereSql,stmtList,JdbcUtils.MYSQL);  
        	sqb.getWhere().accept(visitor);
//    	    if(true){//((BaseEntity)parameter).getModifyWhereField().size()<1
//    	    
//    	    }else{
//    	    	List<SQLExpr> sqllist= Lists.newArrayList();
//    	    	if(sqb.getWhere()  instanceof SQLBinaryOpExpr){
//    	    		for (SQLExpr sqle:sqllist) {   
//    	    			
//    	    		}
//    	    	}
//    	    }
//    	
//        
//        	
        	//whereSql.append(visitor.toString());
    	}

       // for(JoinTableBean jtb:jtbs){
        for(int i=0;i<jtbs.size();i++){
        	JoinTableBean jtb=null;
        	if(jtbs.get(i) instanceof JoinTableBean){
        		jtb=jtbs.get(i);
        	}else{
        		jtb=new JoinTableBean();
        		MyBeanUtils.copyBean2Bean(jtb, jtbs.get(i));
        	}
        	for (Map.Entry<String, WhereValueBean> entry :( jtb.getWhereMap()).entrySet()) { 
        		 String fieldName=StringUtils.toUnderScoreCase(entry.getKey()+blank);
        		 WhereValueBean temp=new WhereValueBean("","");
        		 if(entry.getValue() instanceof WhereValueBean){
        			 temp=entry.getValue() ;
        		 }else{
        			 MyBeanUtils.copyBean2Bean(temp, entry.getValue());
        		 }
        		 String sign=temp.getSign();
        		 Object paramValue=null;
        		 Object paramValue1=null;
        		 //先从参数中获得值
        		 String aliasKey=jtb.getSelectMap().get(entry.getKey());//先从select里面看是否有这个字段 名，并获取别名
        		 if(StringUtils.isEmpty(aliasKey)){
        			 aliasKey=entry.getKey();//如果select里面没有，说明动态bean里面不会有这个值，则使用原生名，实际下面是取不到值的
        		 }
        		if(StringUtils.isNotEmpty(temp.getWhereValueFromField())) {//WhereValueFromField参见WhereValueBean中说明，如果这个指定了，以这个为主.2019090添加
        			aliasKey=temp.getWhereValueFromField();
        		}
        		//如果aliasKey是通过这段话获得的 String aliasKey=jtb.getSelectMap().get(entry.getKey());//先从select里面看是否有这个字段 名，并获取别名
				//那么由于别名外部都会套一个双引号，所以这个必须去掉（注意：只有在putwhereLike的时候不赋值，且该字段又在select中存在时，才会出现这个bug）
				//所以，需要判断最外层是否又双引号，如果有，则去掉这个双引号后，再去获得值，否则拿不到值 2019-07-24发现的bug
				if(aliasKey.contains("\"")) {
					aliasKey=aliasKey.replaceAll("\"", "");
				}
    			
    			 //如果手工设置的也有值，则以手工的为主
    			if( temp.getWhereValue()!=null&&StringUtils.isNotEmpty(temp.getWhereValue()+"")){
    				paramValue=temp.getWhereValue();
    			}
    			if( temp.getWhereValue1()!=null&&StringUtils.isNotEmpty(temp.getWhereValue1()+"")){
    				paramValue1=temp.getWhereValue1();
    			}
    			if(paramValue==null){
    				if(Reflections.getAccessibleField(parameter, aliasKey)!=null)
        			    paramValue=Reflections.invokeGetter(parameter,aliasKey);//使用别名，因为bean的附加属性用的是别名，统一！
        			if(paramValue==null){
        				if(Reflections.getAccessibleField(parameter, "$cglib_prop_"+aliasKey)!=null){
        					 paramValue=Reflections.invokeGetter(parameter,aliasKey);//使用别名，因为bean的附加属性用的是别名，统一！
        				}
        			}
            	}
    			if(JoinOnBean.SIGN_BETWEEN.equals(sign)&&paramValue1==null){
    				String endAliasKey="end"+StringUtils.firstToUpper(aliasKey);
    				if(Reflections.getAccessibleField(parameter, endAliasKey)!=null)
    					paramValue1=Reflections.invokeGetter(parameter,endAliasKey);//使用别名，因为bean的附加属性用的是别名，统一！
        			if(paramValue1==null){
        				if(Reflections.getAccessibleField(parameter, "$cglib_prop_"+endAliasKey)!=null){
        					paramValue1=Reflections.invokeGetter(parameter,endAliasKey);//使用别名，因为bean的附加属性用的是别名，统一！
        				}
        			}
            	}
    			
        		 if(paramValue==null&&paramValue1==null&&!JoinOnBean.SIGN_IS_NOT_NULL.equals(sign)&&!JoinOnBean.SIGN_IS_NULL.equals(sign)){
        			 continue;        		 
        		 }
        		 if(paramValue instanceof String){
        			 if(StringUtils.isEmpty(paramValue.toString()))continue;
        		 }
        		 if(StringUtils.isEmpty(sign))sign=JoinOnBean.SIGN_EQUAL;
        		 if(StringUtils.isNotEmpty(whereSql.toString())){
        			 whereSql.append(blank+"and"+blank);
        		 }

        		 whereSql.append(parseTableAliasAndField(jtb.getTableAlias(), fieldName)+blank);
    			
    			 if(sign.equals(JoinOnBean.SIGN_LIKE)){
    				 whereSql.append(sign+blank);
    				 whereSql.append("'%"+paramValue+"%'");
    			 }else if (JoinOnBean.SIGN_IS_NOT_NULL.equals(sign)||JoinOnBean.SIGN_IS_NULL.equals(sign)){
    				 whereSql.append(sign+blank);
    			 }else if (JoinOnBean.SIGN_IN.equals(sign)){
    				 whereSql.append(sign+blank);
    				 whereSql.append("("+paramValue+")");
    			 }else if (JoinOnBean.SIGN_BETWEEN.equals(sign)){
    				 if(paramValue instanceof Date||paramValue1 instanceof Date) {//后续不管在jtb还是entity中（entity已经有了，是在mapper中调用），都要增加queryTimeStampMap这个方法，以便在这里能够判断是否需要格式化成带时分秒形式，目前先不管
    					 String pdate=formatDate(paramValue);
    					 String pdate1=formatDate(paramValue1);
    					 if(paramValue!=null&&paramValue1!=null) {
    						 whereSql.append(sign+blank);
    						 whereSql.append(pdate+" and "+pdate1);
    					 }else  if(paramValue!=null&&paramValue1==null){
    						 whereSql.append(">="+blank);
    						 whereSql.append(pdate);
    					 }else  if(paramValue==null&&paramValue1!=null){
    						 whereSql.append("<="+blank);
    						 whereSql.append(pdate1);
    					 }
    				 }else {
    					 if(paramValue!=null&&paramValue1!=null) {
    						 whereSql.append(sign+blank);
    						 whereSql.append(judgeObjIsNumData(paramValue)+" and "+judgeObjIsNumData(paramValue1));
    					 }else  if(paramValue!=null&&paramValue1==null){
    						 whereSql.append(">="+blank);
    						 whereSql.append(judgeObjIsNumData(paramValue));
    					 }else  if(paramValue==null&&paramValue1!=null){
    						 whereSql.append("<="+blank);
    						 whereSql.append(judgeObjIsNumData(paramValue1));
    					 }
    				 }
    			
    			 }else{
    				 whereSql.append(sign+blank);
    				 Class cl=paramValue.getClass();
    				 if(cl.getName().equals("java.lang.String")){
    					 whereSql.append("'"+paramValue+"'");
    				 }else if(cl.getName().equals("java.util.Date")){
    					 whereSql.append(formatDate(paramValue));
    				 }else{
    					 whereSql.append(paramValue);
    				 }
    				
    			 }
        		 
    			
    			
        	} 
        }
       
   
        String appendFixSql=((TjBaseEntity)parameter).getAppendWhereCondSql().toString();
        if(StringUtils.isNotEmpty(appendFixSql)){
        	if(StringUtils.isNotEmpty(whereSql.toString())){
        		whereSql.append(blank+"and"+blank);
         	}
        	whereSql.append(appendFixSql);
        }
       String tempSql=parseWhereForOnlyOr(whereSql.toString(),((TjBaseEntity)parameter).getOnlyOr());
       String finalSql= parseWhereForOrArray(tempSql,((TjBaseEntity)parameter).getOrArray());
      // System.out.println("finalSql::::"+finalSql);
        if(StringUtils.isNotEmpty(finalSql)){
        	finalSql=" where "+finalSql;
        }
    	return finalSql;
    }
    /**
     * 判断一个值是否为数值类型，如果是则加上单引号，如果obj为空，请不要传进来，在外围判断
     * @param obj
     * @return
     */
    private String judgeObjIsNumData(Object obj) {
    	boolean isnum=StringUtils.isNumData(obj.toString());
    	if(isnum) {
    		return obj.toString();
    	}else {
    		return MyStringBuffer.addSingleQuote(obj.toString());
    	} 
    }
    private String formatDate(Object date) {
    	String result=date+"";
    	if(date instanceof Date) {
    		if(DateUtils.ifContainHourMinSec((Date)date)) {
    			result="'" + DateUtils.formatDate((Date)date, "yyyy-MM-dd HH:mm:ss") + "'";
    		}else {
    			result="'" + DateUtils.formatDate((Date)date, "yyyy-MM-dd")+" 00:00:00" + "'";
    		}
    		
    	}
    	return result;
    }
    /**
     * 这个只是把该属性对应之前的AND条件变成OR而已
     * @param sql
     * @param onlyOr
     * @return
     */
    private String parseWhereForOnlyOr(String sql,List<String> onlyOr){
    	if(onlyOr==null||onlyOr.size()<1){
    		return sql;
    	}
     	if(StringUtils.isNotEmpty(sql)){
     		SQLExpr sqlExpr=SQLUtils.toMySqlExpr(sql);
    		List<SQLExpr> sqllist= Lists.newArrayList();
    		if(sqlExpr  instanceof SQLBinaryOpExpr){
    			SQLBinaryOpExpr opX=(SQLBinaryOpExpr) sqlExpr;
    			//如果where条件后只有一个条件，且是比如 user_id is null;在调用SQLUtils.split(opX)druid的解析会有问题，如果多个就不会有问题，这种就不解析
    			if(SQLBinaryOperator.Is.name.equalsIgnoreCase(opX.getOperator().name)||SQLBinaryOperator.IsNot.name.equalsIgnoreCase(opX.getOperator().name)) {
    				sqllist.add(opX);
    			}else {
    				sqllist=SQLUtils.split(opX);
    			}
    		}else{
    			System.out.println("无法处理or类型，请联系管理员核查具体原因！");
    			return sql;
    		}
    		StringBuffer finalSql=new StringBuffer();
    		sortList(sqllist,onlyOr);
    		for (SQLExpr sqle:sqllist) {  
    		
    			if(StringUtils.isNotEmpty(finalSql.toString())){
    				boolean hasCatch=false;
    				for(String olor:onlyOr){
    					String field=StringUtils.toUnderScoreCase(olor);
    					hasCatch=hasCatch||ifHasFieldInExpr(sqle,field);
    				}
    				if(hasCatch){
    					finalSql.append(" OR ");
    				}else{
    					finalSql.append(" AND ");
    				}
    			}
    			if(sqle instanceof SQLBinaryOpExpr){
    				SQLBinaryOpExpr dest=(SQLBinaryOpExpr)sqle;
    				if(dest.getLeft() instanceof SQLBinaryOpExpr){
    					finalSql.append("( "+SQLUtils.toMySqlString(sqle)+" )");
    				}else{
    					finalSql.append(SQLUtils.toMySqlString(sqle));
    				}
    			}else{
    				finalSql.append(SQLUtils.toMySqlString(sqle));
    			}
    		}
    		return finalSql.toString();
     	}
     	return "";
    }
    /**
     * 有时要加OR的where中的某个条件在第一个，这时没法加，所以对于需要变化加OR的，尽量向后排
     * @param sortList
     */
    public void sortList(List<SQLExpr> sortList,final List<String> onlyOr){
    	 Collections.sort(sortList, new Comparator<SQLExpr>() {  
             @Override  
             public int compare(SQLExpr o1, SQLExpr o2) {  
            	 boolean hasCatch=false;
 				for(String olor:onlyOr){
 					String field=StringUtils.toUnderScoreCase(olor);
 					hasCatch=hasCatch||ifHasFieldInExpr(o1,field);
 				}
                 int i = 1;  
                 if(hasCatch){
                	 i=-1;
                 }
                 return i;  
             }  
         });  
    }
    /**
     * 为查询条件处理or 如原来是a=? and b=? 合并成 (a=? or b=?);
     * @param sql
     * @param orArray
     * @return
     */
    private String parseWhereForOrArray(String sql,List<String[]> orArray){
    	if(orArray==null||orArray.size()<1){
    		return sql;
    	}
    	if(StringUtils.isNotEmpty(sql)){
    		SQLExpr sqlExpr=SQLUtils.toMySqlExpr(sql);
    		List<SQLExpr> sqllist= Lists.newArrayList();
    		
    		if(sqlExpr  instanceof SQLBinaryOpExpr){
    			if(((SQLBinaryOpExpr) sqlExpr).getOperator().equals(SQLBinaryOperator.Equality))return sql;
    			 sqllist=SQLUtils.split((SQLBinaryOpExpr) sqlExpr);//如果只有一个a.del_flag=? 会把这个表达式拆分了
    			 Collections.reverse(sqllist);
    			 
    		}else{
    			System.out.println("无法处理or类型，请联系管理员核查具体原因！");
    			return sql;
    		}
    		
    	
    		Map<String,List<SQLExpr>> orColl=new HashMap();
    		
    		Iterator<SQLExpr> it = sqllist.iterator();
    		while(it.hasNext()){
    			SQLExpr x = it.next();
    		    String groupNum=ifHas(x,orArray);
    		    if(Integer.parseInt(groupNum)>-1){
    		    	List<SQLExpr> seList=findStringBuffer(orColl,groupNum);
    		    	seList.add(x);
    		    	it.remove();
    		    }
    		}
    		StringBuffer finalSql=new StringBuffer();
    		for (SQLExpr sqle:sqllist) {  
    			if(StringUtils.isNotEmpty(finalSql.toString())){
    				finalSql.append(" AND ");
    			}
    			if(sqle instanceof SQLBinaryOpExpr){
    				SQLBinaryOpExpr dest=(SQLBinaryOpExpr)sqle;
    				if(dest.getLeft() instanceof SQLBinaryOpExpr){
    					finalSql.append("( "+SQLUtils.toMySqlString(sqle)+" )");
    				}else{
    					finalSql.append(SQLUtils.toMySqlString(sqle));
    				}
    			}else{
    				finalSql.append(SQLUtils.toMySqlString(sqle));
    			}
    			
    		}  
    		
    		for (List<SQLExpr> value : orColl.values()) {  
    			 if(StringUtils.isNotEmpty(finalSql.toString()))finalSql.append(" AND ");
    			 if(value.size()>1){
    				 finalSql.append("(");
    				 int i=0;
    				 for(SQLExpr sse:value){
    					 if(i>0)finalSql.append(" OR ");
    					 finalSql.append(SQLUtils.toMySqlString(sse));
    					 i++;
    				 }
    				 finalSql.append(")");
    			 }else{
    				 finalSql.append(SQLUtils.toMySqlString(value.get(0)));
    			 }
    		  
    		}  
    		
    		return finalSql.toString();
    	}
    	return "";
    }
    
    private List<SQLExpr> findStringBuffer(Map<String,List<SQLExpr>> orColl,String groupNum){
    	if(orColl.get(groupNum)==null){
    		List<SQLExpr> temp=Lists.newArrayList();
    		orColl.put(groupNum, temp);
    		return temp;
    	}else{
    		return orColl.get(groupNum);
    	}
    }
    /**
     * 
     * @param sql
     * @param orArray
     * @return 返回的是groupnum号码，即归属到第几个要or的组里面
     */
    private String ifHas(SQLExpr x ,List<String[]> orArray){
    	int returngroupnum=-1;
    	for(int groupnum=0;groupnum<orArray.size();groupnum++){
    		String[] strA=orArray.get(groupnum);
    		for(String temp:strA){
    			String field=StringUtils.toUnderScoreCase(temp);
    			if(ifHasFieldInExpr(x,field)){
    				returngroupnum=groupnum;
					break;
    			}
    		}
    	}
    	return String.valueOf(returngroupnum);
    }
    /**
     * 在表达式中是否含有该属性
     * @param x
     * @param field
     * @return
     */
   private boolean ifHasFieldInExpr(SQLExpr x,String field){
	   boolean bl=false;
	   if (x instanceof SQLBinaryOpExpr ){
			SQLExpr left=((SQLBinaryOpExpr)x).getLeft();
			if( left instanceof SQLPropertyExpr ){//A.ID = 3 这里的A.ID是一个SQLPropertyExpr
				if(notInOr.containsKey(((SQLPropertyExpr) left).toString()))return false;//指定了不包含在or中，下面的between等没有加这句话，遇到了再加，因为不知道是怎么拿出来
				String name=((SQLPropertyExpr) left).getName();
	    				if(field.equals(name)){
	    					bl=true;
                            return bl;
	    				}
			}else if( left instanceof SQLIdentifierExpr ){// ID = 3 这里的ID是一个SQLIdentifierExpr
				if(notInOr.containsKey(((SQLIdentifierExpr) left).toString()))return false;//指定了不包含在or中，下面的between等没有加这句话，遇到了再加，因为不知道是怎么拿出来
				String name=((SQLIdentifierExpr) left).getName();
	    				if(field.equals(name)){
	    					bl=true;
                            return bl;
	    				}
			}
			
		}else if(x instanceof SQLBetweenExpr ){
			SQLExpr testExpr=((SQLBetweenExpr) x).getTestExpr() ;
			if(testExpr instanceof SQLPropertyExpr){
				String name=((SQLPropertyExpr) testExpr).getName();
				if(field.equals(name)){
					bl=true;
					return bl;
				}
			}
		}else if(x instanceof SQLInListExpr ){
			SQLExpr testExpr=((SQLInListExpr) x).getExpr();
			if(testExpr instanceof SQLPropertyExpr){
				String name=((SQLPropertyExpr) testExpr).getName();
				if(field.equals(name)){
					bl=true;
					return bl;
				}
			}
		}else{
			System.out.println("不知道的类型"+x.toString());
		}
	   return bl;
   }
//   /**
//    * 替换where中某个条件左侧的内容的方法，在表达式中是否含有该属性,并且直接替换
//    * @param x
//    * @param field
//    * @return
//    */
//  private String ifHasFieldInExprAndReplaceLeft(SQLExpr x,Map<String,String> fieldMap){
//	  StringBuffer result=new StringBuffer();
//	   if (x instanceof SQLBinaryOpExpr ){
//			SQLExpr left=((SQLBinaryOpExpr)x).getLeft();
//			if( left instanceof SQLPropertyExpr ){
//				String name=((SQLPropertyExpr) left).getName();
//	    				if(fieldMap.containsKey(name)){
//	    					result.append(name+)
//	    				}
//			}
//			
//		}else if(x instanceof SQLBetweenExpr ){
//			SQLExpr testExpr=((SQLBetweenExpr) x).getTestExpr() ;
//			if(testExpr instanceof SQLPropertyExpr){
//				String name=((SQLPropertyExpr) testExpr).getName();
//				if(field.equals(name)){
//					bl=true;
//					return bl;
//				}
//			}
//		}else if(x instanceof SQLInListExpr ){
//			SQLExpr testExpr=((SQLInListExpr) x).getExpr();
//			if(testExpr instanceof SQLPropertyExpr){
//				String name=((SQLPropertyExpr) testExpr).getName();
//				if(field.equals(name)){
//					bl=true;
//					return bl;
//				}
//			}
//		}else{
//			System.out.println("不知道的类型"+x.toString());
//		}
//	   return bl;
//  }
    public String appendGroupBy(){
    	StringBuffer groupBySql=new StringBuffer(); 
    	List<SQLExpr> sL=new ArrayList<SQLExpr>();  //SQLCaseExpr
    	if(sqb.getGroupBy()==null)return "";
    	sL=sqb.getGroupBy().getItems();
    	int t=0;
        for (  SQLExpr sqi : sL) {  
            if (sqi instanceof SQLPropertyExpr) { 
            	String groupbysql=SQLUtils.toSQLString(((SQLPropertyExpr) sqi));
            	if(t>0){
            		groupBySql.append(","+groupbysql+blank);
            	}else{
            		groupBySql.append(blank+"group by"+blank);
            		groupBySql.append(groupbysql+blank);
            	}
            	
            	t++;
            }
        }  
    	
    	  return groupBySql.toString();
    }
    public String appendOrder() throws Exception{
    	StringBuffer orderSql=new StringBuffer(); 
    	 List<Column> orderList=sqbSsVisitor.getOrderByColumns();
    	// select * from tb_comment_record where FIND_IN_SET(id, find_comment_record_tree_function('aaa')) order by FIND_IN_SET(id, find_comment_record_tree_function(9)) 
    	//上面这条sql，由于orderby是复杂的，所以在这里sqbSsVisitor.getOrderByColumns()拿不到，也就是driud没发解析这个复杂的orderby
    	 // sqlselect.getOrderBy();
    	//  sstmt.getSelect().getOrderBy().accept(visitor);
//    	  ssVisitor.getOrderByColumns();
//    	 String orderbySql= SQLUtils.toSQLString(sstmt.getSelect().getOrderBy());
    	  String bySort="";
    	  if(!((TjBaseEntity)parameter).isWhetherClearFixOrderBy())
           for (  Column sqi : orderList) {  
        	   for (Map.Entry<String, Object> entry :( sqi.getAttributes()).entrySet()) { 
        		   if(entry!=null){
        			   bySort=entry.getValue()==null?"":entry.getValue()+"";
        		   }
        	   }
        	  String tableAlias=getAliasByExistTableName(sqi.getTable());
        	   orderSql.append(parseTableAliasAndField(tableAlias,sqi.getName())+blank);  
        	   orderSql.append(bySort+",");
           }  
    	  StringBuffer tempChildSb=new StringBuffer();
    	  for(int i=0;i<jtbs.size();i++){
          	JoinTableBean jtb=null;
          	if(jtbs.get(i) instanceof JoinTableBean){
          		jtb=jtbs.get(i);
          	}else{
          		jtb=new JoinTableBean();
          		MyBeanUtils.copyBean2Bean(jtb, jtbs.get(i));
          	}
              	for (Map.Entry<String, String> entry :( jtb.getOrderyByMap()).entrySet()) { 
              		 String fieldName=StringUtils.toUnderScoreCase(entry.getKey()+blank);
              		tempChildSb.append(parseTableAliasAndField(jtb.getTableAlias(), fieldName)+blank);
              		String descOrAsc=JoinTableBean.ASC;
              		if(StringUtils.isNotEmpty(entry.getValue())){
              			descOrAsc=entry.getValue();
              		}
              		tempChildSb.append(descOrAsc+blank+",");
           	    }
           	}
           StringBuffer tempMainSb=new StringBuffer();
         Map<String, String> mainOrderbyMap= ((TjBaseEntity)parameter).getOrderyByMap();
       	for (Map.Entry<String, String> entry :mainOrderbyMap.entrySet()) { 
       		String fieldName=StringUtils.toUnderScoreCase(entry.getKey()+blank);
       	    String currEntityTableNameAlias=getAliasByExistTableName(currEntityTableName);
       	    tempMainSb.append(parseTableAliasAndField(currEntityTableNameAlias, fieldName)+blank);
       		String descOrAsc=JoinTableBean.ASC;
       		if(StringUtils.isNotEmpty(entry.getValue())){
      			descOrAsc=entry.getValue();
      		}
       		tempMainSb.append(descOrAsc+blank+",");
       	}
       	
       	if(((TjBaseEntity)parameter).isChildAndMainOrderBySort()){//true说明主表设置的orderby在前
       		orderSql.append(tempMainSb.toString());
       		orderSql.append(tempChildSb.toString());
       	}else{
       		orderSql.append(tempChildSb.toString());
       		orderSql.append(tempMainSb.toString());
       		
       	}
        String finalSql="";
           if(StringUtils.isNotEmpty(orderSql.toString())){
        	  
        	   orderSql.insert(0, " order by ");
        	   finalSql=orderSql.toString().substring(0, orderSql.toString().length() - 1);
           }
           return finalSql;
    }
    /**
     * 处理表别名+字段名方式，返回所需
     * @param tAlias
     * @param fieldName
     * @return
     */
   private String parseTableAliasAndField(String tAlias,String fieldName){
	  StringBuilder sb=new StringBuilder();
	 
	  if(StringUtils.isEmpty(tAlias)){
		  sb.append(fieldName);
	  }else{
		  SQLExpr se=SQLUtils.toSQLExpr(fieldName);
		  if(se instanceof SQLIdentifierExpr){
			  sb.append(tAlias+"."+fieldName);
		  }else{
			  sb.append(fieldName);//此时可能是max字段或者其他的case when cast等等，记得要自己加上别名了
		  }
	

		 
	  }
	  return sb.toString();
   }
   private String findTableAlias(Map<String,String> tableAliasMap,String destTableName){
	   String tableAlias="";
	   for (Map.Entry<String, String> entry : tableAliasMap.entrySet()) { 
		   if(entry.getValue().equals(entry.getKey()))continue;
		   if(destTableName.equals(entry.getValue())){ 
			   tableAlias=entry.getKey();
		   }
	   }
	   return tableAlias;
   }

}
