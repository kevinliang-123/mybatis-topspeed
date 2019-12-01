package com.tengjie.common.service;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.cglib.beans.BeanGenerator;
import net.sf.cglib.beans.BeanMap;

import org.apache.ibatis.binding.MapperProxy;
import org.apache.taglibs.standard.lang.jstl.RelationalOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.ResolvableType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ClassUtils;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import com.tengjie.common.config.Global;
import com.tengjie.common.persistence.TjBaseEntity;
import com.tengjie.common.persistence.CrudDao;
import com.tengjie.common.persistence.JoinOnBean;
import com.tengjie.common.persistence.JoinTableBean;
import com.tengjie.common.persistence.Page;
import com.tengjie.common.persistence.PageMap;
import com.tengjie.common.persistence.util.DbTablePrimaryKeyFieldConfig;
import com.tengjie.common.persistence.util.MybatisSqlGetTool;
import com.tengjie.common.persistence.util.RelationOperationTool;
import com.tengjie.common.utils.ListUtils;
import com.tengjie.common.utils.MapUtils;
import com.tengjie.common.utils.MyBeanUtils;
import com.tengjie.common.utils.MyStringBuffer;
import com.tengjie.common.utils.NewInstanceUtil;
import com.tengjie.common.utils.PackageUtil;
import com.tengjie.common.utils.Reflections;
import com.tengjie.common.utils.SpringContextHolder;
import com.tengjie.common.utils.StringUtils;





/**
 * Service基类
 * @author
 * @version 2014-05-16
 */
@Transactional(readOnly = true)
public abstract class CrudService<D extends CrudDao<T>, T extends TjBaseEntity<T>> extends BaseService {
	
	private final Class<T> entityGenericType;
	@SuppressWarnings("unchecked")
	public CrudService()
    {
		//this.genericType=(Class<T>)TypeToken.of(CrudService.class).resolveType(CrudService.class.getTypeParameters()[1]).getRawType();
		// Map<TypeVariable, Type> geneMap=GenericTypeResolver.getTypeVariableMap(getClass());此方法已经被禁用
		//GenericTypeResolver.resolveTypeArguments 和GenericTypeResolver.resolveTypeArgument，第一个是在这种有两个泛型时使用。
        Class<?>[] argsArray =  GenericTypeResolver.resolveTypeArguments(getClass(), CrudService.class);
        entityGenericType=(Class<T>) argsArray[1];

    }
	/**
	 * 持久层对象
	 */
	@Autowired
	protected D dao;
	
	/**
	 * 获取单条数据
	 * @param id
	 * @return
	 */
	public T get(String id) {
		return dao.get(id);
	}
	
	/**
	 * 获取单条数据
	 * @param entity
	 * @return
	 */
	public T get(T entity) {
		return dao.get(entity);
	}

	/**
	 * 	 获取单条数据,本方法是一个快捷方法，根据当前主表与子表condTableName进行innerjoin，然后获得主表符合条件的结果集
	 *查询结果集不回包含子表的任何字段
	 * @param mainJoinField 当前表要与条件表的关联字段
	 * @param condTableName 条件表的表名
	 * @param condJoinField 条件表的关联字段名
	 * @param condWhereField 条件表的查询字段名
	 * @param condWhereFieldValue 条件表的查询字段值
	 * @param mainJoinField
	 * @param condTableName
	 * @param condJoinField
	 * @param condWhereField
	 * @param condWhereFieldValue
	 * @return
	 */
	public T get(String mainJoinField,String condTableName,String  condJoinField,String condWhereField,Object condWhereFieldValue) {
		T entity =(T) getCurrEntityClassInstance();
		JoinTableBean jtb=JoinTableBean.fastGetJtb(condTableName, mainJoinField, condJoinField);
		jtb.putWhere(condWhereField, condWhereFieldValue);
		entity.putJoinTable(jtb);
		return dao.getByWhere(entity);
	}

	/**
	 * 获取单条数据
	 * @param entity
	 * @param fieldName，从entity中取值作为查询条件的字段名,有时从前端过来的entity中会有很多属性有值，但是只想用其中的几个字段作为查询条件
	 *        fieldName只面向快捷简单查询，因此,注意关联信息等不会copy，如果有关联信息，说明已经是复杂查询。
	 * @return
	 */
	public Map getMap(T entity,String ...fieldName) {
		entity=copyNewEntity(entity,fieldName);
		entity.setResultType("2");

		return (Map) dao.getByWhere(entity);
	}
	/**
	 * 获取单条数据,entity中的有值数据会作为查询条件，若返回多条则会抛出业务异常
	 * @param entity
	 * @param fieldName，从entity中取值作为查询条件的字段名,有时从前端过来的entity中会有很多属性有值，但是只想用其中的几个字段作为查询条件,注意关联信息等不会copy
	 * @return
	 */
	public T getByWhere(T entity,String ...fieldName) {
		entity=copyNewEntity(entity,fieldName);

		return  dao.getByWhere(entity);
	}
	/**
	 * 获取单条数据
	 * @param entity
	 * @return
	 */
	public T getNew(T entity) {
		return dao.getNew(entity);
	}
	/**
	 * 专门为接口自动生成执行代码时使用，调用任何serverce方法均以本方法为入口，否则javassist因为泛型会无法调用方法
	 * @param methodName
	 * @param params
	 * @return
	 */
	public Object anyCallForAutoCode(String methodName,Object entity){
		
		Object newentity=null;
		try {
			newentity = getCurrEntityClassInstance();
			MyBeanUtils.copyBean2Bean(newentity, entity, new String[]{"namingPolicy"}, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	
		Object result=Reflections.invokeMethodByName(this, methodName,new Class[]{newentity.getClass()}, new Object[]{newentity});
		return result;
	}
	private Object getCurrEntityClassInstance(){
		ParameterizedType parameterizedType = (ParameterizedType) this.getClass().getGenericSuperclass();
		int index=1;
		Class clazz = (Class<T>) parameterizedType.getActualTypeArguments()[index];
		try {
			return  clazz.newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * 查询列表数据
	 * @param entity
	 * @param fieldName，从entity中取值作为查询条件的字段名,有时从前端过来的entity中会有很多属性有值，但是只想用其中的几个字段作为查询条件,注意关联信息等不会copy
	 * @return
	 */
	public List<T> findList(T entity,String ...fieldName) {
		entity=copyNewEntity(entity,fieldName);
		
		return dao.findList(entity);
	}
	
	/**
	 * 根据传入属性，将entity中的多个fieldName值copy到一个新的entity中
	 * @param entity
	 * @param fieldName
	 * @return
	 */
	public T copyNewEntity(T entity,String ...fieldName){
		if(fieldName.length<1)return entity;
		T newentity=entity;
		try {
			newentity=(T) entity.getClass().newInstance();
			Map<String,String> fieldMap=ListUtils.ListToMap(ListUtils.arrayToList(fieldName));
			fieldMap.put("page", "page");
			fieldMap.put("pageNo", "pageNo");
			fieldMap.put("pageSize", "pageSize");
			fieldMap.put("sqlMap", "sqlMap");
			fieldMap.put("rowErrorInfo", "rowErrorInfo");
			fieldMap.put("operationType", "operationType");
			fieldMap.put("modifyAlias", "modifyAlias");
			fieldMap.put("removeSelectField", "removeSelectField");
			fieldMap.put("addSelectField", "addSelectField");
			fieldMap.put("modifySelectField", "modifySelectField");
			fieldMap.put("modifyWhereField", "modifyWhereField");
			fieldMap.put("includeSelectFieldMap", "includeSelectFieldMap");
			fieldMap.put("includeSelectFieldMapDotShow", "includeSelectFieldMapDotShow");
			fieldMap.put("whetherClearFixOrderBy", "whetherClearFixOrderBy");
			fieldMap.put("childAndMainOrderBySort", "childAndMainOrderBySort");
			fieldMap.put("appendDistinct", "appendDistinct");
			fieldMap.put("resultType", "resultType");
			fieldMap.put("appendWhereCondSql", "appendWhereCondSql");
			fieldMap.put("orderyByMap", "orderyByMap");
			fieldMap.put("joinTableBeanList", "joinTableBeanList");
			fieldMap.put("updateWhereConditionMap", "updateWhereConditionMap");
			fieldMap.put("pageMap", "pageMap");
			fieldMap.put("whereLikeMap", "whereLikeMap");
			fieldMap.put("whereInMap", "whereInMap");
			fieldMap.put("relationOperMap", "relationOperMap");
			fieldMap.put("mainTableAppendPropMap", "mainTableAppendPropMap");
			fieldMap.put("groupBySelectField", "groupBySelectField");
			fieldMap.put("groupByField", "groupByField");
			
			fieldMap.put("queryTimeStampMap", "queryTimeStampMap");
			fieldMap.put("commonInfoDesc", "commonInfoDesc");
			fieldMap.put("fieldCallBackMap", "fieldCallBackMap");
			fieldMap.put("rowBeanCallBack", "rowBeanCallBack");
			fieldMap.put("originValueCopyMap", "originValueCopyMap");
			
			fieldMap.put("orArray", "orArray");
			fieldMap.put("notInOr", "notInOr");
			fieldMap.put("onlyOr", "onlyOr");
			fieldMap.put("ifRpc", "ifRpc");

			MyBeanUtils.copyBean2Bean(newentity, entity, fieldMap);
			newentity.setClearSelectField(entity.isClearSelectField());//boolean类型的invoke有问题，还是用的get
		} catch (Exception e) {
			e.printStackTrace();
		}
		return newentity;
	}
	/**
	 *本方法是一个快捷方法，根据当前主表与子表condTableName进行innerjoin，然后获得主表符合条件的结果集
	 *查询结果集不回包含子表的任何字段
	 * @param mainJoinField 当前表要与条件表的关联字段
	 * @param condTableName 条件表的表名
	 * @param condJoinField 条件表的关联字段名
	 * @param condWhereField 条件表的查询字段名
	 * @param condWhereFieldValue 条件表的查询字段值
	 * @return
	 */
	public List<T> findListFast(String mainJoinField,String condTableName,String  condJoinField,String condWhereField,Object condWhereFieldValue) {
		T entity =(T) getCurrEntityClassInstance();
		JoinTableBean jtb=JoinTableBean.fastGetJtb(condTableName, mainJoinField, condJoinField);
		jtb.putWhere(condWhereField, condWhereFieldValue);
		entity.putJoinTable(jtb);
		
		return dao.findList(entity);
	}
	/**
	 * 查询列表数据,结果集为map类型不是bean类型,本方法不需要设置IncludeSelectFieldMap，默认用全部字段
	 * @param entity
	 * @return
	 */
	public  List<Map<String,Object>> findListMapAllField(T entity) {
			Field[] fields=entity.getClass().getDeclaredFields();
			for(Field f:fields){
				 boolean isStatic = Modifier.isStatic(f.getModifiers());
				 if(!isStatic)
				entity.putIncludeSelectField(f.getName());
			}
			entity.putIncludeSelectField("id");
			return findListMap(entity);
		}

	/**
	 * 查询列表数据,结果集为map类型不是bean类型
	 * @param entity
	 * @param fieldName，从entity中取值作为查询条件的字段名,有时从前端过来的entity中会有很多属性有值，但是只想用其中的几个字段作为查询条件,注意关联信息等不会copy
	 * @return
	 */
	public  List<Map<String,Object>> findListMap(T entity,String ...fieldName) {
		entity=copyNewEntity(entity,fieldName);
		entity.setResultType("2");
		
		return (List<Map<String, Object>>) dao.findList(entity);
	}
	
	/**
	 * 查询列表数据
	 * @param entity
	 * @return
	 */
	public List<T> findAllList(T entity) {
		
		return dao.findAllList(entity);
	}
	/**
	 * 根据CollectFunctionConstantTool中的avg、max、min、sum函数，对字段进行求值运算
	 * 注意：如果数据库没有记录，则会返回null，因此接收本值一定不能用简单类型，比如int，一定要用Integer
	 * @param entity
	 * @param fieldName
	 * @param functionName 从CollectFunctionConstantTool中选择
	 * @return
	 */
	public <V>V findCollectFunction(T entity,String fieldName,String functionName){
		Object result=null;
		String alias=fieldName+StringUtils.firstToUpper(functionName.toLowerCase());
		entity.setClearSelectField(true);
	
		String functionSql=functionName+"("+StringUtils.toUnderScoreCase(fieldName)+")";
		String caseWhen="case when "+functionSql+" is null then 0 else "+functionSql+" end";
		entity.putAddSelectField(caseWhen, alias);
		
		List<T> rr=dao.findList(entity);
		T t=rr.get(0);
		if(t!=null)
		result=rr.get(0).getDynaValue(alias);
		return result==null?null:(V)result;
		
	}
	/**
	 *查询当前sql的count(1)只是返回一个整型数量值 select count(1) from table a left join.......
	 * @param entity
	 *  @param fieldName，从entity中取值作为查询条件的字段名,有时从前端过来的entity中会有很多属性有值，但是只想用其中的几个字段作为查询条件,注意关联信息等不会copy
	 * @return
	 */
	public int findCount(T entity,String ...fieldName){
		int result=0;
		entity=copyNewEntity(entity,fieldName);
		entity.setClearSelectField(true);
		entity.putAddSelectField("case when count(1) is null then 0 else count(1) end", "recordCount");
		
		List<T> rr=dao.findList(entity);
		T t=rr.get(0);
		if(t!=null)
		result=rr.get(0).getDynaIntegerValue("recordCount");
		return result;
	}
	
	/**
	 * 查询分页数据
	 * @param page 分页对象
	 * @param entity
	 * @return
	 */
	public Page<T> findPage(Page<T> page, T entity) {
		entity.setPage(page);
		
		page.setList(dao.findList(entity));
		return page;
	}
	/**
	 * 查询分页数据
	 * @param page 分页对象
	 * @param entity
	 * @return 
	 */
	public PageMap<Map<String,Object>> findMapPage(TjBaseEntity entity,PageMap<Map<String,Object>>... temppageMap){
		PageMap<Map<String,Object>> pageMap=null;
		if(temppageMap==null||temppageMap.length<1){
			pageMap=new PageMap<Map<String, Object>>(entity.getPageNo(), entity.getPageSize());
			entity.setResultType("2");
		}else{
			pageMap=temppageMap[0];
			entity.setResultType("2");
		}

		entity.setPageMap(pageMap);
		pageMap.setIfCount(false);
		
		pageMap.setList((List<Map<String, Object>>) dao.findList((T)entity));
		return pageMap;
	}
	/**
	 * 判断当前实体的主键字段名是否叫做id
	 * @param entity
	 * @return
	 */
	private boolean ifPriKeyIsId(T entity) {
		boolean bl=false;
		String destId=DbTablePrimaryKeyFieldConfig.findKeyField(entity);
		if(StringUtils.isNotEmpty(destId)) {
			if(destId.toLowerCase().equals("id")) {
				bl=true;
			}
		}
		return bl;
	}
	/**
	 * 
	 * @param entity
	 * @return
	 */
	private String findPriKeyIdName(T entity) {
		return DbTablePrimaryKeyFieldConfig.findKeyField(entity);
	}
	/**
	 * 保存数据（插入或更新）
	 * @param entity
	 * @param fieldName，需要保存或则更新的字段名，有时从前端过来的entity中会有很多属性有值，但是只想用其中的几个字段进行保存或者更新
	 * @return 如果插入前过滤器发现有问题，则返回问题内容描述
	 */
	@Transactional(readOnly = false)
	public void save(T entity,String ...fieldNames) {
		
		if(fieldNames.length>0) {
			T newentity=copyNewEntity(entity,fieldNames);
			entity=newentity;
		}

		if (entity.getIsNewRecord()){
			entity.preInsert();
			dao.insert(entity);
		}else{
			entity.preUpdate();
			dao.update(entity);
		}

	}
	
	/**
	 * 更新数据，实体bean中的updateWhereConditionMap若包含该属性，并且该属性不为空则会拼接到update的where条件中
	 * @param entity
	 * @return 如果插入前过滤器发现有问题，则返回问题内容描述
	 */
	@Transactional(readOnly = false)
	public void updateByWhere(T entity) {
		   
			entity.preUpdate();
			dao.updateByWhere(entity);
		   
	}
	
	
	/**
	 * 比对entity中原有的url与删除的url，返回剩余的url信息，多个以逗号分割
	 * @param entity
	 * @param delUrlsForMutil
	 * @return
	 */
	public String compareDelUrls(String saveUrlField, T entity,String delUrlsForMutil) {
		Object rel=null;
		if(Reflections.isContainFieldDyna(entity, saveUrlField)) {
			rel=Reflections.getFieldValue(entity, saveUrlField);
		}
		String oldUrl=rel==null?"":rel.toString();
		MyStringBuffer sb=MyStringBuffer.newInstance();
		if(StringUtils.isNotEmpty(oldUrl)) {//原来有存过，才有比较的意义
			if(StringUtils.isNotEmpty(delUrlsForMutil)) {//有删除的
				String[] oldArray=oldUrl.split(",");
				Map<String,String> delMap=MapUtils.stringArrayToMap(delUrlsForMutil.split(","));
				for(String temp:oldArray) {
					if(!delMap.containsKey(temp)) {
						if(StringUtils.isEmpty(sb.toString())) {
							sb.append(temp);
						}else {
							sb.append(",",temp);
						}
					}
				}
			}else {//没有删除的
				return oldUrl;//原来的原样返回
			}
		}
		return sb.toString();
	}
	
	/**
	 * 删除数据
	 * @param entity
	 */
	@Transactional(readOnly = false)
	public void delete(T entity) {
		String priKeyName=findPriKeyIdName(entity);
		String idValue=entity.getId();
		if(!priKeyName.toLowerCase().equals("id")) {
			Object obj=Reflections.getFieldValue(entity, priKeyName);
			if(obj!=null)idValue=obj.toString();
		}
		if(StringUtils.isNotEmpty(idValue)&&!entity.getWhereInMap().containsKey(priKeyName)){//entity.getWhereInMap().containsKey("id")必须加，否则会无法用in形式的id删除，因为之前的设置会被覆盖
			T bt=null;
			try {
				bt = (T) entity.getClass().newInstance();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			
			if(priKeyName.toLowerCase().equals("id")) {
				((TjBaseEntity)bt).setId(entity.getId()); 
			}else {
				Reflections.setFieldValue(bt, priKeyName, Reflections.getFieldValue(entity, priKeyName));
			}
			
			dao.delete(bt);
		}else{
			
			dao.delete(entity);
		}
		
	}
	
	/**
	 * 逻辑删除数据
	 * @param entity
	 */
	@Transactional(readOnly = false)
	public void deleteByLogic(T entity) {
		
		dao.deleteByLogic(entity);
	}
	
	
	/**
	 * 删除全部数据
	 * @param entitys
	 */
	@Transactional(readOnly = false)
	public void deleteAll(Collection<T> entitys) {
		for (T entity : entitys) {
			
			dao.delete(entity);
		}
	}

	/**
	 * 分组查询的List map形式的分页，为接口使用
	 * @param entity
	 * @return
	 */
	public List<Map<String,Object>> findMapGroupByList(T entity) {
		entity.setResultType("2");
		
		List<Map<String, Object>> result=(List<Map<String, Object>>) dao.findGroupByList(entity);
		if(result!=null&&result.size()==1){
			if(result.get(0)==null){//对于group by的结果集，在mysql中很奇怪，如果得出的结果集是空，dao.findGroupByList(entity)返回的list中有一条记录，但是如果你get(0),却是空的，所以这里处理一下，避免混淆
				result=new ArrayList();
			}
		}
		return result;
	}
	/**
	 * 分组查询的PageMap分页，为接口使用
	 * @param entity
	 * @param tempPageMap
	 * @return
	 */
	public PageMap<Map<String,Object>>  findPageMapByGroupBy(T entity,PageMap<Map<String,Object>>... tempPageMap) {
		PageMap<Map<String,Object>> pageMap=null;
		if(tempPageMap==null||tempPageMap.length<1){
			 pageMap=new PageMap<Map<String, Object>>(entity.getPageNo(), entity.getPageSize());
			 pageMap.setIfCount(false);
		}else{
			pageMap=tempPageMap[0];
		}
		entity.setPageMap(pageMap);
		entity.setResultType("2");
		pageMap.setList(findMapGroupByList(entity));
		return pageMap;
	}
	
	public List<T> findGroupByList(T entity) {
		
		List<T> result=dao.findGroupByList(entity);
		if(result!=null&&result.size()==1){
			if(result.get(0)==null){//对于group by的结果集，在mysql中很奇怪，如果得出的结果集是空，dao.findGroupByList(entity)返回的list中有一条记录，但是如果你get(0),却是空的，所以这里处理一下，避免混淆
				result=new ArrayList();
			}
		}
		return result;
	}

	public Page<T>  findPageByGroupBy(Page<T> page ,T entity) {
		entity.setPage(page);
		page.setList(findGroupByList(entity));

		return page;
	}
	/**
	 * 获取单条数据，此方法会将propertyName字段加上下划线来翻译为数据库字段
	 * @param propertyName
	 * @return
	 */
	public T findUniqueByPropertyCamel(String propertyName, Object value){
		propertyName=StringUtils.toUnderScoreCase(propertyName);
		return findUniqueByProperty(propertyName, value);
	}
	/**
	 * 获取单条数据
	 * @param propertyName
	 * @return
	 */
	public T findUniqueByProperty(String propertyName, Object value){
		return dao.findUniqueByProperty(propertyName, value);
	}
	/**
	 * 获取List多条数据,此方法会将propertyName字段加上下划线来翻译为数据库字段
	 * @param propertyName
	 * @return
	 */
	public List<T> findListByPropertyCamel(String propertyName, Object value){
		propertyName=StringUtils.toUnderScoreCase(propertyName);
		return findListByProperty(propertyName, value);
	}
	/**
	 * 获取List多条数据
	 * @param propertyName
	 * @return
	 */
	public List<T> findListByProperty(String propertyName, Object value){
		return dao.findListByProperty(propertyName, value);
	}
	/**
	 * 获取单条数据
	 * @param propertyName
	 * @return
	 */
	@Transactional(readOnly = false)
	public int updateForOneProperty(String updatePropertyName, Object updateValue,String wherePropertyName, Object whereValue){
		return dao.updateForOneProperty(updatePropertyName, updateValue, wherePropertyName, whereValue);
	}
	/**
	 * 根据单个属性，删除记录
	 * @param propertyName
	 * @return
	 */
	@Transactional(readOnly = false)
	public int deleteByOneProperty(String propertyName, Object value){
		if(!propertyName.contains("_")){
			propertyName=StringUtils.toUnderScoreCase(propertyName);
		}
		return dao.deleteByOneProperty(propertyName, value);
	}
	/**
	 * 批量插入
	 * @param list
	 * @return
	 */
	@Transactional(readOnly = false)
	public int insertBatch(List<T> list){
		if(list.size()<1)return 0;
		for(T entity:list){
			
			entity.preInsert();
		}
		return dao.insertBatch(list);
	}
	/**
	 * 批量插入,由于是将insert语句拼接成而成，因此如果条数太多会报错，因此这里提供一个参数，进行最大多少个的分割
	 * @param list
	 * @return
	 */
	@Transactional(readOnly = false)
	public int insertBatch(List<T> list,int splitNum){
		if(list.size()<1)return 0;
		if(splitNum<1)splitNum=300;
		int start=0;
		if(list.size()<=splitNum) {
			insertBatch(list);
		}else {
			while(start<list.size()) {
				int toIndex=start+splitNum+1;
				if(toIndex>list.size())toIndex=list.size();
				insertBatch(list.subList(start, toIndex));
				start=start+splitNum+1;
			}
			
		}
	
		for(T entity:list){
			entity.preInsert();
		}
		return dao.insertBatch(list);
	}
	/**
	 *  完全手写查询，任意的sql语句，完全手写sql查询列表
	 * @param entity:查询参数，若本entity中不含有属性，则调用动态属性添加方式
	 * @return
	 */
	public List<T> findAnyQueryList(T entity) {
		return dao.findAnyQuery(entity);//？？
	}
	/**
	 *  完全手写查询，任意的sql语句，完全手写sql查询列表
	 * @param entity:查询参数，若本entity中不含有属性，则调用动态属性添加方式
	 * @return
	 */
	public List<Map<String,Object>> findAnyQueryListMap(T entity) {
		entity.setResultType("2");
		return (List<Map<String, Object>>)dao.findAnyQuery(entity);
	}
	/**
	 * 完全手写查询，任意的sql语句，完全手写sql查询分页
	 * @param page 分页对象
	 * @param entity:查询参数，若本entity中不含有属性，则调用动态属性添加方式
	 * @return
	 */
	public PageMap<Map<String,Object>> findAnyQueryPageMap( T entity) {
		
		PageMap<Map<String, Object>> pageMap=new PageMap<Map<String, Object>>(entity.getPageNo(), entity.getPageSize());
		entity.setPageMap(pageMap);
		entity.setResultType("2");
		pageMap.setList(dao.findAnyQuery(entity));
		return pageMap;
	}
	/**
	 *  完全手写查询，任意的sql语句，完全手写sql查询列表
	 * @param entity:查询参数，若本entity中不含有属性，则调用动态属性添加方式
	 * @return
	 */
	@Transactional(readOnly = false)
	public int updateAnySql(T entity) {
		return dao.updateAnySql(entity);
	}

	/**
	 * 完全手写查询，任意的sql语句，完全手写sql查询分页
	 * @param page 分页对象
	 * @param entity:查询参数，若本entity中不含有属性，则调用动态属性添加方式
	 * @return
	 */
	public Page<T> findAnyQueryPage(Page<T> page, T entity) {
		entity.setPage(page);
		page.setList(dao.findAnyQuery(entity));
		return page;
	}
	/**
	 *  获取sequence的值
	 * @param seqName:数据库中sequence表的的seq名称
	 * @param entity:任意一个在mapper文件中具有findAnyQuery方法的实体
	 * @return 如果数据库无记录，会返回null
	 */
	@Transactional(readOnly = false)
	public Long findSequence(String seqName, T entity) {
		String sql=" select nextval('"+seqName+"')  as seqValue";
		entity.setAnySql(sql);
		List<T> result= dao.findAnyQuery(entity);
	
		if(result.size()<1){
			return null;
		}else{
			if(result.get(0)==null)return null;
			return result.get(0).getDynaLongValue("seqValue");
		}
	}
	/**
	 * 批量更新，但是实际上仍然是循环调用
	 * @param problemList
	 */
	@Transactional(readOnly = false)
	public void batchUpdate(List<T> tList) {
         for( T p:tList){
        	this.save(p);
         }
	}
	/**
	 * 不执行sql，只是获得mysql解析完成后的sql语句内容
	 * @param entity
	 * @param methodName：findList、findGroupByList、update、insert等方法
	 * @return
	 */
	public String findMybatisSql(T entity,String methodName){
		String sql=MybatisSqlGetTool.getNamespaceSql(entity,methodName);
		return sql;
	}
	/**
	 * 获得某个字段的比如avg、sum等聚合函数的sql，如果涉及多个字段，可以在传入entity的外部自行拼装也可以，
	 * 这里只是为了方便一个字段
	 * 不执行sql，只是获得mysql解析完成后的sql语句内容
	 * @param entity
	 * @param functionName：avg、sum、min等
	 * @param fieldName：要计算的字段
	 * @param roundNum：字段的保留小数点位数，可以不传入，则不进行控制
	 * @return
	 */
	public String findMybatisSqlCollectFunction(T entity,String functionName,String fieldName,Integer roundNum){
		String alias=fieldName+StringUtils.firstToUpper(functionName.toLowerCase());
		alias=StringUtils.toUnderScoreCase(alias);
		entity.setClearSelectField(true);
		String addField=functionName+"("+StringUtils.toUnderScoreCase(fieldName)+")";
		if(roundNum!=null) {
			addField="round("+addField+","+roundNum.intValue()+")";
		}
		entity.putAddSelectField(addField, alias);
		String sql=null;
		if(entity.getGroupByField()!=null&&entity.getGroupByField().size()>0) {
			sql=MybatisSqlGetTool.getNamespaceSql(entity,"findGroupByList");//这里使用的方法名，一定是要在dao即mapper中存在的
		}else {
			sql=MybatisSqlGetTool.getNamespaceSql(entity,"findList");//这里使用的方法名，一定是要在dao即mapper中存在的
		}
		
		
		return sql;
	}
	/**
	 * 比较给定的区间数据，在目标实体的指定区间段是否重复，destEntity里面的查询条件自行组成，
	 * 这里只是拼接比较部分的sql
	 * 注意：本方法主要应用在：所查字段就在当前主表中
	 * 如：有一个需求，表中有两个字段，分别是起始年龄和截止年龄组成的年龄段，在新增或者编辑的时候，要求该年龄段不能重复。
	 * @param minFieldName：实体中最小值字段名
	 * @param maxFieldName：实体中最大值字段名
	 * @return
	 */
	public boolean numberSectionRepeat(T destEntity,String minFieldName,String maxFieldName) {
		  boolean bl=false;
		  Object minValueObj=Reflections.getFieldValue(destEntity, minFieldName);
		  Object maxValueObj=Reflections.getFieldValue(destEntity, maxFieldName);
		  if(minValueObj==null||maxValueObj==null) {
			  return bl;//如果这两个值都为空，说明不需要比较，直接返回false
			// throw new ServiceException("在实体["+destEntity.getClass().getSimpleName()+"]中的字段"+minFieldName+","+maxFieldName+"两个字段的值为空！无法比较是否重复！");
		  }
		  MyStringBuffer sb=MyStringBuffer.newInstance();
		  String underMinValue=StringUtils.toUnderScoreCase(minFieldName);
		  String underMaxValue=StringUtils.toUnderScoreCase(maxFieldName);
		  String alias="a";
		  String minValue=Reflections.getFieldValue(destEntity, minFieldName).toString();
		  String maxValue=Reflections.getFieldValue(destEntity, maxFieldName).toString();
		  sb.append("((",minValue.toString(),">=",alias,".",underMinValue," and ");
		  sb.append(minValue.toString(),"<",alias,".",underMaxValue,") or ");
		   
		  sb.append("(",maxValue.toString(),">=",alias,".",underMinValue," and ");
		  sb.append(maxValue.toString(),"<",alias,".",underMaxValue,"))");
		  destEntity.putRelationOper("id", JoinOnBean.SIGN_NOT_EQUAL);
		  Reflections.setFieldValue(destEntity, minFieldName, null);//防止作为查询条件，清空 
		  Reflections.setFieldValue(destEntity, maxFieldName, null);//防止作为查询条件，清空
		  destEntity.appendWhereCondSql(sb.toString());
		  List result= findList(destEntity);
		  Reflections.setFieldValue(destEntity, minFieldName, minValueObj);//在还原回来，否则会影响到外围获取这两个值
		  Reflections.setFieldValue(destEntity, maxFieldName, maxValueObj);//在还原回来，否则会影响到外围获取这两个值
		  if(result.size()>0) {bl=true;}
		  return bl;
	}
	/**
	 * 比较给定的区间数据，在目标实体的指定区间段是否重复，destEntity里面的查询条件自行组成，
	 * 这里只是拼接比较部分的sql
	 * 注意：本方法主要应用在：所比较字段不在当前主表中，而是在关联表中，如果就只是一个表，调用上面的简单方法
	 * 如：有一个需求，表中有两个字段，分别是起始年龄和截止年龄组成的年龄段，在新增或者编辑的时候，要求该年龄段不能重复。
	 * @param destEntity：目标要查询的实体，自行组成，如果里面包含了关联表，且minFieldName、和maxFieldName是该关联表的，那么要告诉我jtbAlias
	 * @param minFieldName：实体中最小值字段名、
	 * @param maxFieldName：实体中最大值字段名
	 * @param minValue：要作为是判断否重叠区间的最小的值
	 * @param maxValue：要作为是判断否重叠区间的最大的值
	 * @param jtbAlias：如果minFieldName、和maxFieldName不是主表的字段，而是在目标实体中某个关联表jtb的字段，那么要告诉我jtbAlias，否则这里可以为空
	 * @return
	 */
	public boolean numberSectionRepeat(T destEntity,String minFieldName,String maxFieldName,Double minValue,Double maxValue,String jtbAlias) {
		  boolean bl=false;
		  MyStringBuffer sb=MyStringBuffer.newInstance();
		  String underMinValue=StringUtils.toUnderScoreCase(minFieldName);
		  String underMaxValue=StringUtils.toUnderScoreCase(maxFieldName);
		  String alias="a";
		  if(StringUtils.isNotEmpty(jtbAlias))alias=jtbAlias;
		  sb.append("((",minValue.toString(),">=",alias,".",underMinValue," and ");
		  sb.append(minValue.toString(),"<",alias,".",underMaxValue,") or ");
		   
		  sb.append("(",maxValue.toString(),">=",alias,".",underMinValue," and ");
		  sb.append(maxValue.toString(),"<",alias,".",underMaxValue,"))");
		  destEntity.putRelationOper("id", JoinOnBean.SIGN_NOT_EQUAL);
		  destEntity.appendWhereCondSql(sb.toString());
		  List result= findList(destEntity);
		  if(result.size()>0) {bl=true;}
		  return bl;
	}
}
