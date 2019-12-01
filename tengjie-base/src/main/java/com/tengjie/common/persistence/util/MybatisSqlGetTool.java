package com.tengjie.common.persistence.util;

/**
 * 获得mysql的解析后的sql，但是并不需要执行
 * 本方法同样会截取到mappermodifyplugin处理后的sql
 * @author liangfeng
 *
 */
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.tengjie.common.persistence.TjBaseEntity;
import com.tengjie.common.persistence.JoinTableBean;
import com.tengjie.common.persistence.ModifySelect;
import com.tengjie.common.utils.DateUtils;
import com.tengjie.common.utils.SpringContextHolder;
@Service
@Lazy(false)
public class MybatisSqlGetTool  implements ApplicationContextAware, DisposableBean{

	private static SqlSessionFactory sqlSessionFactory;
	private final ObjectFactory DEFAULT_OBJECT_FACTORY = new DefaultObjectFactory();
	private final ObjectWrapperFactory DEFAULT_OBJECT_WRAPPER_FACTORY = new DefaultObjectWrapperFactory();
	
	/**
	 * 反射对象，增加对低版本Mybatis的支持
	 *
	 * @param object
	 *            反射对象
	 * @return
	 */
	public MetaObject forObject(Object object) {
		return MetaObject.forObject(object, DEFAULT_OBJECT_FACTORY,
				DEFAULT_OBJECT_WRAPPER_FACTORY);
	}

	/**
	 * 通过接口获取sql
	 *
	 * @param mapper
	 * @param methodName
	 * @param args
	 * @return
	 */
	public String getMapperSql(Object mapper, String methodName, Object... args) {
		MetaObject metaObject = forObject(mapper);
		SqlSession session = (SqlSession) metaObject.getValue("h.sqlSession");
		Class mapperInterface = (Class) metaObject
				.getValue("h.mapperInterface");
		String fullMethodName = mapperInterface.getCanonicalName() + "."
				+ methodName;
		if (args == null || args.length == 0) {
			return getNamespaceSql(fullMethodName, null);
		} else {
			return getMapperSql(mapperInterface, methodName, args);
		}
	}

	/**
	 * 通过Mapper方法名获取sql
	 *
	 * @param session
	 * @param fullMapperMethodName
	 * @param args
	 * @return
	 */
	public String getMapperSql(String fullMapperMethodName, Object... args) {

		if (args == null || args.length == 0) {
			return getNamespaceSql(fullMapperMethodName, null);
		}
		String methodName = fullMapperMethodName.substring(fullMapperMethodName
				.lastIndexOf('.') + 1);
		Class mapperInterface = null;
		try {
			mapperInterface = Class.forName(fullMapperMethodName.substring(0,
					fullMapperMethodName.lastIndexOf('.')));
			return getMapperSql(mapperInterface, methodName, args);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("参数" + fullMapperMethodName
					+ "无效！");
		}
	}

	/**
	 * 通过Mapper接口和方法名
	 *
	 * @param session
	 * @param mapperInterface
	 * @param methodName
	 * @param args
	 * @return
	 */
	public String getMapperSql(Class mapperInterface, String methodName,
			Object... args) {
		String fullMapperMethodName = mapperInterface.getCanonicalName() + "."
				+ methodName;
		if (args == null || args.length == 0) {
			return getNamespaceSql(fullMapperMethodName, null);
		}
		Method method = getDeclaredMethods(mapperInterface, methodName);
		Map params = new HashMap();
		final Class<?>[] argTypes = method.getParameterTypes();
		for (int i = 0; i < argTypes.length; i++) {
			if (!RowBounds.class.isAssignableFrom(argTypes[i])
					&& !ResultHandler.class.isAssignableFrom(argTypes[i])) {
				String paramName = "param" + String.valueOf(params.size() + 1);
				paramName = getParamNameFromAnnotation(method, i, paramName);
				params.put(paramName, i >= args.length ? null : args[i]);
			}
		}
		if (args != null && args.length == 1) {
			Object _params = wrapCollection(args[0]);
			if (_params instanceof Map) {
				params.putAll((Map) _params);
			}
		}
		return getNamespaceSql(fullMapperMethodName, params);
	}

	/**
	 * 通过命名空间方式获取sql
	 *
	 * @param session
	 * @param namespace
	 * @return
	 */
	public String getNamespaceSql(SqlSession session, String namespace) {
		return getNamespaceSql(namespace, null);
	}
	/**
	 * 通过entity对象方式获取sql
	 *
	 * @param session
	 * @param namespace
	 * @param params
	 * @return
	 */
	public static String  getNamespaceSql(TjBaseEntity  params,String methodName) {
 
		String mainDaoPath=findNamespace(methodName,params);
		return getNamespaceSql(mainDaoPath,params);
	}
	/**
	 * 获得当前方法的id
	 *
	 * @param session
	 * @param namespace
	 * @param params
	 * @return
	 */
	public static String  findNamespace(String mehtodName, Object params) {
		Configuration configuration = getSqlSessionFactory().openSession()
				.getConfiguration();
		Collection<Class<?>> allMapperClass=configuration.getMapperRegistry().getMappers();
		String simpleName=params.getClass().getSimpleName();
		if(simpleName.contains("$$BeanGeneratorByCGLIB"))simpleName=simpleName.substring(0,simpleName.indexOf("$$BeanGeneratorByCGLIB"));
		String mapperName=simpleName+"MainMapper";//视程序而定，这个项目是MainMapper，有些可能是MainDao
		String mapperName1=simpleName+"MainDao";
		String mapperName2=simpleName+"Mapper";
		Class<?> destMapperclass=null;
		for(Class<?> tempMapperclass:allMapperClass) {
			if(tempMapperclass.getSimpleName().equals(mapperName)) {
				destMapperclass=tempMapperclass;
				break;
			}
		}
		if(destMapperclass==null) {
			for(Class<?> tempMapperclass:allMapperClass) {
				if(tempMapperclass.getSimpleName().equals(mapperName1)) {
					destMapperclass=tempMapperclass;
					break;
				}
				if(tempMapperclass.getSimpleName().equals(mapperName2)) {
					destMapperclass=tempMapperclass;
					break;
				}
			}
		}
		
		return destMapperclass.getPackage().getName()+"."+destMapperclass.getSimpleName()+"."+mehtodName;
	}
	/**
	 * 通过命名空间方式获取sql
	 *
	 * @param session
	 * @param namespace
	 * @param params
	 * @return
	 */
	public static String  getNamespaceSql(String namespace, Object params) {
		params = wrapCollection(params);
		Configuration configuration = getSqlSessionFactory().openSession()
				.getConfiguration();
		MappedStatement mappedStatement = configuration
				.getMappedStatement(namespace);
		TypeHandlerRegistry typeHandlerRegistry = mappedStatement
				.getConfiguration().getTypeHandlerRegistry();
		BoundSql boundSql = mappedStatement.getBoundSql(params);

		List<ParameterMapping> parameterMappings = boundSql
				.getParameterMappings();
		String sql = boundSql.getSql();
		try {
			sql=appendJoinSql(sql,((TjBaseEntity)params).getJoinTableBeanList(),params);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		if (parameterMappings != null) {
			for (int i = 0; i < parameterMappings.size(); i++) {
				ParameterMapping parameterMapping = parameterMappings.get(i);
				if (parameterMapping.getMode() != ParameterMode.OUT) {
					Object value;
					String propertyName = parameterMapping.getProperty();
					if (boundSql.hasAdditionalParameter(propertyName)) {
						value = boundSql.getAdditionalParameter(propertyName);
					} else if (params == null) {
						value = null;
					} else if (typeHandlerRegistry.hasTypeHandler(params
							.getClass())) {
						value = params;
					} else {
						MetaObject metaObject = configuration
								.newMetaObject(params);
						
						value = metaObject.getValue(propertyName);
					}
					JdbcType jdbcType = parameterMapping.getJdbcType();
					if (value == null && jdbcType == null)
						jdbcType = configuration.getJdbcTypeForNull();
					sql = replaceParameter(sql, value, jdbcType,
							parameterMapping.getJavaType());
				}
			}
		}
		return sql;
	}
	private static String appendJoinSql(String originSql, List<JoinTableBean> jtbs,Object parameter) throws Exception{
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
	 * 根据类型替换参数 仅作为数字和字符串两种类型进行处理，需要特殊处理的可以继续完善这里
	 *
	 * @param sql
	 * @param value
	 * @param jdbcType
	 * @param javaType
	 * @return
	 */
	private static String replaceParameter(String sql, Object value,
			JdbcType jdbcType, Class javaType) {
		String strValue = String.valueOf(value);
		if (jdbcType != null) {
			switch (jdbcType) {
			// 数字

			case BIT:
			case TINYINT:
			case SMALLINT:
			case INTEGER:
			case BIGINT:
			case FLOAT:
			case REAL:
			case DOUBLE:
			case NUMERIC:
			case DECIMAL:
				break;
			// 日期
                 
			case DATE:
				strValue="'" + DateUtils.formatDate((Date)value, "yyyy-MM-dd")+" 00:00:00" + "'";
				break;
			case TIME:
			case TIMESTAMP:
				// 其他，包含字符串和其他特殊类型

			default:
				strValue = "'" + strValue + "'";

			}
		} else if (Number.class.isAssignableFrom(javaType)) {
			// 不加单引号

		} else {
			strValue = "'" + strValue + "'";
		}
		return sql.replaceFirst("\\?", strValue);
	}

	/**
	 * 获取指定的方法
	 *
	 * @param clazz
	 * @param methodName
	 * @return
	 */
	private Method getDeclaredMethods(Class clazz, String methodName) {
		Method[] methods = clazz.getDeclaredMethods();
		for (Method method : methods) {
			if (method.getName().equals(methodName)) {
				return method;
			}
		}
		throw new IllegalArgumentException("方法" + methodName + "不存在！");
	}

	/**
	 * 获取参数注解名
	 *
	 * @param method
	 * @param i
	 * @param paramName
	 * @return
	 */
	private String getParamNameFromAnnotation(Method method, int i,
			String paramName) {
		final Object[] paramAnnos = method.getParameterAnnotations()[i];
		for (Object paramAnno : paramAnnos) {
			if (paramAnno instanceof Param) {
				paramName = ((Param) paramAnno).value();
			}
		}
		return paramName;
	}

	/**
	 * 简单包装参数
	 *
	 * @param object
	 * @return
	 */
	private static Object wrapCollection(final Object object) {
		if (object instanceof List) {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("list", object);
			return map;
		} else if (object != null && object.getClass().isArray()) {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("array", object);
			return map;
		}
		return object;
	}


	@Override
	public void destroy() throws Exception {
		SpringContextHolder.clearHolder();
		
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		sqlSessionFactory=applicationContext.getBean("sqlSessionFactory", SqlSessionFactory.class);
		
	}

	public static SqlSessionFactory getSqlSessionFactory() {
		if(sqlSessionFactory==null)sqlSessionFactory=SpringContextHolder.getApplicationContext().getBean("sqlSessionFactory", SqlSessionFactory.class);
		return sqlSessionFactory;
	}

	
    
}
