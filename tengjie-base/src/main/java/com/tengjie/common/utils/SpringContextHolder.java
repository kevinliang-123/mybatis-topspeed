package com.tengjie.common.utils;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.context.ContextLoader;

import com.tengjie.common.persistence.TjBaseEntity;
import com.tengjie.common.persistence.DataEntityFieldCallBackBean;

/**
 * 以静态变量保存Spring ApplicationContext, 可在任何代码任何地方任何时候取出ApplicaitonContext.
 * 
 * @author
 * @date 2013-5-29 下午1:25:40
 */
@Component
public class SpringContextHolder implements ApplicationListener<ContextRefreshedEvent>,ApplicationContextAware, DisposableBean {

	public static ApplicationContext applicationContext;

	private static Logger logger = LoggerFactory.getLogger(SpringContextHolder.class);

	/**
	 * 取得存储在静态变量中的ApplicationContext.
	 */
	public static ApplicationContext getApplicationContext() {
		assertContextInjected();
		return applicationContext;
	}
    public static Connection getConnection(){
    	DataSource dataSource = (DataSource)getApplicationContext().getBean(DataSource.class);  
    	Connection	connection =null;
    	if(dataSource!=null)
    	connection =DataSourceUtils.getConnection(dataSource);
    	return connection;
    }
    /**
     * 如果不是在事务的场景中调用getConnection，则不会自动释放connection，容易内存泄露
     * 因此在非事务场景中手工拿出getConnection后，必须调用releaseConnection进行释放。
     * @param connection
     * @return
     */
    public static Connection releaseConnection(Connection	connection){
    	DataSource dataSource = (DataSource)getApplicationContext().getBean(DataSource.class);  
    	DataSourceUtils.releaseConnection(connection, dataSource);
    	return connection;
    }
	/**
	 * 从静态变量applicationContext中取得Bean, 自动转型为所赋值对象的类型.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getBean(String name) {
		assertContextInjected();
		return (T) getApplicationContext().getBean(name);
	}

	/**
	 * 从静态变量applicationContext中取得Bean, 自动转型为所赋值对象的类型.
	 */
	public static <T> T getBean(Class<T> requiredType) {
		assertContextInjected();
		return getApplicationContext().getBean(requiredType);
	}
	/**
	 * 根据实体对象获得对应的service的名称
	 * @param be：实体对象
	 * @param iffirstup：首字母是否大写，默认是小写，true或者不传入都是小写，false是大写
	 * @return
	 */
    public static String findServiceNameByEntity(TjBaseEntity be,boolean ...iffirstLow) {
    	String entityName=findEntityNameByEntity(be,iffirstLow);
    	return entityName+"Service";
    }
    /**
	 * 根据实体对象获得对应的Dao的名称
	 * @param be：实体对象
	 * @param iffirstup：首字母是否大写，默认是小写
	 * @return
	 */
    public static String findDaoNameByEntity(TjBaseEntity be,boolean ...iffirstLow) {
    	String entityName=findEntityNameByEntity(be,iffirstLow);
    	return entityName+"Dao";
    }
	/**
	 * 根据实体对象获得其名称，注意，如果是动态bean类型，会去掉后面的后缀，最终是干净的名称
	 * @param be：实体对象
	 * @param iffirstup：首字母是否大写，默认是小写,true是小写
	 * @return
	 */
    public static String findEntityNameByEntity(TjBaseEntity be,boolean ...iffirstLow) {
    	String entityName=be.getClass().getSimpleName();
    	//如果是带有动态属性的bean，名字会不一样，是这样的teacherInfo$$BeanGeneratorByCGLIB$$33b233a6，这样无法覆盖原来的
    	if(entityName.contains("$$"))entityName=entityName.substring(0, entityName.indexOf("$$"));
    	boolean firstup=iffirstLow.length>0?iffirstLow[0]:true;
    	if(firstup){
    		entityName=StringUtils.firstToLower(entityName);
    	}
    	return entityName;
    
    }
    /**
     * 根据类名成获得对象，优先到spring中查找，如果找不到，则使用Class.forName创建类
     * @param className：可以是全包名，也可以就是一个类名，会优先到spring中查找
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     */
    public static Object  findCallBackClass(String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException{
		
		Object obj=null;
		String allPackage=null;
		try{
			if(className.contains(".")){//说明传入的是个全路径
				allPackage=className;
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
		if(obj==null&&allPackage!=null){
			obj=Class.forName(allPackage).newInstance();
		}
		if (obj == null)
			throw new ClassNotFoundException("找不到[" + className+ "]对应的类,请核对是否正确！");
		return obj;
	}
	/**
	 * 清除SpringContextHolder中的ApplicationContext为Null.
	 */
	public static void clearHolder() {
		if (logger.isDebugEnabled()){
			logger.debug("清除SpringContextHolder中的ApplicationContext:" + applicationContext);
		}
		applicationContext = null;
	}

	/**
	 * 实现ApplicationContextAware接口, 注入Context到静态变量中.
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		SpringContextHolder.applicationContext = applicationContext;
	}

	/**
	 * 实现DisposableBean接口, 在Context关闭时清理静态变量.
	 */
	@Override
	public void destroy() throws Exception {
		SpringContextHolder.clearHolder();
	}

	/**
	 * 检查ApplicationContext不为空.
	 */
	private static void assertContextInjected() {
		if(applicationContext==null){
			ContextLoader.getCurrentWebApplicationContext();
			applicationContext = new ClassPathXmlApplicationContext("spring-context.xml");
		}
		Validate.validState(applicationContext != null, "applicaitonContext属性未注入, 请在applicationContext.xml中定义SpringContextHolder.");
	}
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		applicationContext=( event).getApplicationContext();
	}
}