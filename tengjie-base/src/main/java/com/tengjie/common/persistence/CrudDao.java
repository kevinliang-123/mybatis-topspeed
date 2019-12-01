package com.tengjie.common.persistence;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

/**
 * DAO支持类实现
 * @author
 * @version 2014-05-16
 * @param <T>
 */
public interface CrudDao<T> extends BaseDao {

	/**
	 * 获取单条数据
	 * @param id
	 * @return
	 */
	public T get(String id);
	
	/**
	 * 获取单条数据
	 * @param entity
	 * @return
	 */
	public T get(T entity);
	/**
	 * 获取单条数据,entity中的有值数据会作为查询条件，若返回多条则会抛出业务异常
	 * @param entity
	 * @return
	 */
	public T getByWhere(T entity);

	/**
	 * 获取单条数据
	 * @param entity
	 * @return
	 */
	public T getNew(T entity);
	/**
	 * 根据实体名称和字段名称和字段值获取唯一记录
	 * 
	 * @param <T>
	 * @param propertyName
	 * @param value
	 * @return
	 */
	public  T findUniqueByProperty(@Param(value = "propertyName") String propertyName, @Param(value = "value") Object value);
	/**
	 * 根据实体名称和字段名称和字段值获取list记录
	 * 
	 * @param <T>
	 * @param propertyName
	 * @param value
	 * @return
	 */
	public  List<T> findListByProperty(@Param(value = "propertyName") String propertyName, @Param(value = "value") Object value);
	/**
	 * 更新实体中某一个字段的值（目前还不知道是否能更新日期类型，待测试）
	 * @param updatePropertyName
	 * @param updateValue
	 * @param wherePropertyName
	 * @return whereValue
	 */
	public  int updateForOneProperty(@Param(value = "updatePropertyName") String updatePropertyName, @Param(value = "updateValue") Object updateValue,@Param(value = "wherePropertyName") String wherePropertyName,@Param(value = "whereValue") Object whereValue);
	
	/**
	 * 根据一个属性名，删除记录
	 * 
	 * @param <T>
	 * @param propertyName
	 * @param value
	 * @return
	 */
	public  int deleteByOneProperty(@Param(value = "propertyName") String propertyName, @Param(value = "value") Object value);
	
	/**
	 * 查询数据列表，如果需要分页，请设置分页对象，如：entity.setPage(new Page<T>());
	 * @param entity
	 * @return
	 */
	public List findList(T entity);
	

	
	
	/**
	 * 查询所有数据列表
	 * @param entity
	 * @return
	 */
	public List<T> findAllList(T entity);
	
	/**
	 * 查询所有数据列表
	 * @see public List<T> findAllList(T entity)
	 * @return
	 */
	@Deprecated
	public List<T> findAllList();
	
	/**
	 * 插入数据
	 * @param entity
	 * @return
	 */
	public int insert(T entity);
	
	/**
	 * 更新数据,注意直接调用dao，不会更新updateDate字段，若调用service则会更新
	 * @param entity
	 * @return
	 */
	public int update(T entity);
	/**
	 * 更新数据，实体bean中的updateWhereConditionMap若包含该属性，并且该属性不为空则会拼接到update的where条件中
	 * @param entity
	 * @return
	 */
	public int updateByWhere(T entity);
	
	/**
	 * 删除数据（物理删除，从数据库中彻底删除）
	 * @param id
	 * @see public int delete(T entity)
	 * @return
	 */
	@Deprecated
	public int delete(String id);
	
	/**
	 * 删除数据（逻辑删除，更新del_flag字段为1,在表包含字段del_flag时，可以调用此方法，将数据隐藏）
	 * @param id
	 * @see public int delete(T entity)
	 * @return
	 */
	@Deprecated
	public int deleteByLogic(String id);
	
	/**
	 * 删除数据（物理删除，从数据库中彻底删除）
	 * @param entity
	 * @return
	 */
	public int delete(T entity);
	
	/**
	 * 删除数据（逻辑删除，更新del_flag字段为1,在表包含字段del_flag时，可以调用此方法，将数据隐藏）
	 * @param entity
	 * @return
	 */
	public int deleteByLogic(T entity);
	/**
	 * 分组查询
	 * @param entity
	 * @return
	 */
	public List<T> findGroupByList(T entity);
	/**
	 * 批量删除
	 * @param list
	 * @return
	 */
	public int insertBatch(List<T> list);
	
	/**
	 * 完全手写查询，任意的sql语句，可以传参也可以不传
	 * @param entity
	 * @return
	 */
	public List findAnyQuery(T entity);
	/**
	 * 完全手写查询，任意的sql语句，可以传参也可以不传
	 * @param entity
	 * @return
	 */
	public int updateAnySql(T entity);
}