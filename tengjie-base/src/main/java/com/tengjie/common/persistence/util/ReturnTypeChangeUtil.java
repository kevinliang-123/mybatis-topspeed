package com.tengjie.common.persistence.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
/**
 * 此类作为工具类，服务于将返回值为bean的类型转换为Map类型，以便给接口使用
 * @author liangfeng
 *
 */
public class ReturnTypeChangeUtil {
	 private static final List<ResultMapping> EMPTY_RESULTMAPPING = new ArrayList<ResultMapping>(0);
	 private static Map<String,MappedStatement> statementCache=new HashMap();
	 /**
     * 根据现有的 ms 创建一个新的，使用新的返回值类型
     *
     * @param ms
     * @return
     */
    public static MappedStatement newMappedStatement(MappedStatement ms, Class resultType) {
        //下面是新建的过程，考虑效率和复用对象的情况下，这里最后生成的ms可以缓存起来，下次根据 ms.getId() + "_" + getShortName(resultType) 直接返回 ms,省去反复创建的过程
       String key=ms.getId() + "_Map";
       if(  statementCache.get(key)!=null)return   statementCache.get(key);
    	MappedStatement.Builder builder = new MappedStatement.Builder(ms.getConfiguration(), key, ms.getSqlSource(), ms.getSqlCommandType());
        builder.resource(ms.getResource());
        builder.fetchSize(ms.getFetchSize());
        builder.statementType(ms.getStatementType());
        builder.keyGenerator(ms.getKeyGenerator());
        if (ms.getKeyProperties() != null && ms.getKeyProperties().length != 0) {
            StringBuilder keyProperties = new StringBuilder();
            for (String keyProperty : ms.getKeyProperties()) {
                keyProperties.append(keyProperty).append(",");
            }
            keyProperties.delete(keyProperties.length() - 1, keyProperties.length());
            builder.keyProperty(keyProperties.toString());
        }
        builder.timeout(ms.getTimeout());
        builder.parameterMap(ms.getParameterMap());
        //count查询返回值int
        List<ResultMap> resultMaps = new ArrayList<ResultMap>();
        ResultMap resultMap = new ResultMap.Builder(ms.getConfiguration(), ms.getId(), resultType, EMPTY_RESULTMAPPING).build();
        resultMaps.add(resultMap);
        builder.resultMaps(resultMaps);
        builder.resultSetType(ms.getResultSetType());
        builder.cache(ms.getCache());
        builder.flushCacheRequired(ms.isFlushCacheRequired());
        builder.useCache(ms.isUseCache());
        MappedStatement temp=builder.build();
        statementCache.put(key, temp);
        return temp;
    }

}
