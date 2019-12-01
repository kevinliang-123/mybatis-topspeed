package com.tengjie.common.persistence;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlASTVisitorAdapter;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlSchemaStatVisitor;

public class CustMySqlSchemaStatVisitor extends MySqlSchemaStatVisitor {
	 private Map<String, String> aliasMap = new HashMap<String, String>();
	    public boolean visit(SQLExprTableSource x) {
	        String alias = x.getAlias();
	        aliasMap.put(alias, x.getName().getSimpleName());
	        return true;
	    }

	    public Map<String, String> getAliasMap() {
	        return aliasMap;
	    }
}
