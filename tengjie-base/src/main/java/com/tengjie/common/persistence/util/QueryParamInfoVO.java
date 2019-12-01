package com.tengjie.common.persistence.util;

import java.util.List;

import com.google.common.collect.Lists;
import com.tengjie.common.persistence.TjBaseEntity;
import com.tengjie.common.persistence.JoinTableBean;
import com.tengjie.common.utils.Reflections;

/**
 * 主要为了向某些动态方法中传递所需参数的名字、运算符、值等信息
 * 创建时时为了JoinTableBean的buildAnyCountJtb方法使用，因为entity不确定，因此要对这个bean进行哪些参数的传递需要依靠这个来传递
 * 后续，感觉这个可以应用到好多地方来进行参数的传递
 * @author admin
 *
 */
public class QueryParamInfoVO {
	List<QueryParamInfoVOChild> paramList=Lists.newArrayList();
	
	public QueryParamInfoVO() {
		super();
	}
	public static void parseParam(TjBaseEntity dest,QueryParamInfoVO qpiv) {
		if(dest==null||qpiv==null)return;
		List<QueryParamInfoVOChild> childList=qpiv.paramList;
		for(QueryParamInfoVOChild child:childList) {
			String filedName=child.getFieldName();
			if(child.getValue()!=null)
			Reflections.invokeSetter(dest, filedName, child.getValue());
			if(RelationOperationTool.IN .equals(child.getRelationOperationSign())) {
				dest.putWhereIn(filedName, RelationOperationTool.IN);
				
			}else if(RelationOperationTool.NOT_IN.equals(child.getRelationOperationSign())) {
				dest.putWhereIn(filedName, RelationOperationTool.NOT_IN);
			}else if(RelationOperationTool.IS_NULL.equals(child.getRelationOperationSign())) {
				dest.putWhereISNULL(filedName, false);
				
			}else if(RelationOperationTool.IS_NOT_NULL.equals(child.getRelationOperationSign())) {
				dest.putWhereISNULL(filedName, true);
			}else if(RelationOperationTool.LIKE.equals(child.getRelationOperationSign())) {
				dest.putWhereLike(filedName);
			}else {
				dest.putRelationOper(filedName, child.getRelationOperationSign());
			}
			
		}
	}
	/**
	 * 添加一条参数描述描述记录
	 * @param fieldName：字段名
	 * @param relationOperationSign：运算符,来自于RelationOperationTool
	 * @param value：字段的值，可以为空
	 * @return
	 */
	public QueryParamInfoVO putInfo(String fieldName, String relationOperationSign, Object value) {
		QueryParamInfoVOChild child=new QueryParamInfoVOChild(fieldName,relationOperationSign,value);
		paramList.add(child);
		return this;
	}
	/**
	 * 添加一条参数描述描述记录
	 * @param fieldName：字段名
	 * @param value：字段的值，可以为空
	 * @return
	 */
	public QueryParamInfoVO putInfo(String fieldName,  Object value) {
		QueryParamInfoVOChild child=new QueryParamInfoVOChild(fieldName,value);
		paramList.add(child);
		return this;
	}
	
	public static QueryParamInfoVO newInstance(String fieldName, String relationOperationSign, Object value) {
    	QueryParamInfoVO qp=new QueryParamInfoVO();
    	qp.putInfo(fieldName, relationOperationSign, value);
    	return qp;
    }
	public static QueryParamInfoVO newInstance(String fieldName, Object value) {
    	QueryParamInfoVO qp=new QueryParamInfoVO();
    	qp.putInfo(fieldName, value);
    	return qp;
    }
    public static QueryParamInfoVO newInstance() {
    	QueryParamInfoVO qp=new QueryParamInfoVO();
    	return qp;
    }
	private static class QueryParamInfoVOChild{
		private String fieldName;
		private String relationOperationSign=RelationOperationTool.EQUAL;//来自于RelationOperationTool,默认时等于
		private Object value;
		private Object value1;// 为between准备，但是这里没有处理，后面需要再加
		public String getFieldName() {
			return fieldName;
		}
		public void setFieldName(String fieldName) {
			this.fieldName = fieldName;
		}
		public String getRelationOperationSign() {
			return relationOperationSign;
		}
		public void setRelationOperationSign(String relationOperationSign) {
			this.relationOperationSign = relationOperationSign;
		}
		public Object getValue() {
			return value;
		}
		public void setValue(Object value) {
			this.value = value;
		}
		public QueryParamInfoVOChild(String fieldName, String relationOperationSign, Object value) {
			super();
			this.fieldName = fieldName;
			if(relationOperationSign!=null)
			this.relationOperationSign = relationOperationSign;
			this.value = value;
		}
		public QueryParamInfoVOChild(String fieldName, Object value) {
			super();
			this.fieldName = fieldName;
			this.value = value;
		}
		
	}
}
