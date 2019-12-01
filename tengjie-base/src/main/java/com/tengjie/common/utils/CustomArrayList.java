package com.tengjie.common.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * 专为DataEntity中max min等字段的动态查询赋值前使用
 * @author liangfeng
 *
 * @param <E>
 */
public class CustomArrayList<E> extends ArrayList<E> implements Serializable {
	private static final long serialVersionUID = 1L;
	private boolean convertType=false;//为false的时候，对count等做转换，为true时，将bean字段类型根据其中大写字符，加下划线变成数据库字段
	private List<String> groupBySelectField;
	private String tableAlias="a";
	
	 public CustomArrayList(boolean convertType,List<String> groupBySelectField) {
		super();
		this.convertType = convertType;
		this.groupBySelectField=groupBySelectField;
	}
	 public CustomArrayList() {
			super();
		}
	 /**
	  * 添加内容，同时会将该字段添加到groupBySelectField中，但是
	  * 需要注意的是：
      * 在to_date(field_name)这些情况下，即使指定为true，也不会添加 
      * a.fieldName 这种是会自动添加到select中的，别名会截取为fieldName
	  * @param e
	  * @return
	  */
	public boolean add(E e) {
		return add(e,true);
	}
	 /**
	  * 
	  * @param e
	  * @param ifaddToSelect:是否将groupBy的字段直接添加到select中，true:添加到select字段中 ，false：不添加
	  * 需要注意的是：
      * 在to_date(field_name)这些情况下，即使指定为true，也不会添加 
      * a.fieldName 这种如果指定为true，是会自动添加到select中的，别名会截取为fieldName
	  * @return
	  */
	public boolean add(E e,boolean ifaddToSelect) {
	        if(e instanceof String){
	        	String cc=e.toString();
	        	if(!convertType){
//	        		if(StringUtils.deleteWhitespace(cc).toLowerCase().contains("count(")){
//		        		 cc= cc+" as groupByCountField";
//		        	}
//		        	if(StringUtils.deleteWhitespace(cc).toLowerCase().contains("max(")){
//		        		 cc= cc+" as groupByMaxField";
//		        	}
//		        	if(StringUtils.deleteWhitespace(cc).toLowerCase().contains("min(")){
//		        		 cc= cc+" as groupByMinField";
//		        	}
//		        	if(StringUtils.deleteWhitespace(cc).toLowerCase().contains("sum(")){
//		        		 cc= cc+" as groupBySumField";
//		        	}
//		        	if(StringUtils.deleteWhitespace(cc).toLowerCase().contains("avg(")){
//		        		 cc= cc+" as groupByAvgField";
//		        	}
		        	
	        	}else{
	        		//本方法的初衷是，对于groupByField，在添加内容时，会自动加到groupBySelectField中作为select的查询字段，
	        		//即分组内容直接作为select内容，但是对于DATE_FORMAT这种函数类型时，则不添加，需要手工自己加，
	        		//原因是：正常的逻辑是groupByField的字段，会加上as，as后面跟的也是groupByField，
	        		//当加上函数时，as的后面就会带有这个函数，除非能够把里面的独立字段信息拆出来，后续再处理
	        		if(isBasicField(cc) ){
	        			String middle=StringUtils.toUnderScoreCase(cc);
		        		if(ifaddToSelect) {
		        			String destAlias=StringUtils.isEmpty(tableAlias)?"":tableAlias+".";
		        			groupBySelectField.add(destAlias+middle+" as "+cc);
		        		}
		        		cc=middle;//如果是标准字段，在group by中，是不会加前缀的，只是在select中加了
	        		}else{
	        			if(cc.contains(".")&&!cc.contains("(")) {//非标准字段，如subtable.abc;这种形态并添加到select中，其他的不会添加到select中！！
	        				String[] split=cc.split("\\.");
	        				String prefix=split[0];//前缀，如a.field的a
		        			cc=prefix+"."+StringUtils.toUnderScoreCase(split[1]);
		        			if(ifaddToSelect) {
		        				String asName=split[1];//字段名 field
		        				asName=StringUtils.toCamelCase(StringUtils.toUnderScoreCase(asName));
		        				groupBySelectField.add(cc+" as "+asName);
		        			}
		        			
	        			}
	        			
	        		}
	        	}
	        	return super.add((E) cc);
	        }
	        return super.add(e);
	    }

	    public String getTableAlias() {
		return tableAlias;
	}
	    /**
	     * 是否标准字段，即直接一个字段名，a.fieldName,to_date(field_name)等等均不算标准字段
	     * @param arrName
	     * @return
	     */
	 private static boolean isBasicField(String arrName){
	    	// String input = "abc[1]dd[a-b]df[3.b]";
	         String regex = "^[a-zA-Z0-9]+$";
	         Pattern pattern = Pattern.compile (regex);
	         Matcher matcher = pattern.matcher (arrName);
	        return matcher.find ();
	     }
	public void setTableAlias(String tableAlias) {
		this.tableAlias = tableAlias;
	}
		/**
	     * Inserts the specified element at the specified position in this
	     * list. Shifts the element currently at that position (if any) and
	     * any subsequent elements to the right (adds one to their indices).
	     *
	     * @param index index at which the specified element is to be inserted
	     * @param element element to be inserted
	     * @throws IndexOutOfBoundsException {@inheritDoc}
	     */
	    public void add(int index, E element) {
	    	  if(element instanceof String){
		        	String cc=element.toString();
		        	if(convertType){
		        		if(StringUtils.deleteWhitespace(cc).toLowerCase().contains("count(")){
			        		 cc= cc+" as groupByCountField";
			        	}
			        	if(StringUtils.deleteWhitespace(cc).toLowerCase().contains("max(")){
			        		 cc= cc+" as groupByMaxField";
			        	}
			        	if(StringUtils.deleteWhitespace(cc).toLowerCase().contains("min(")){
			        		 cc= cc+" as groupByMinField";
			        	}
			        	if(StringUtils.deleteWhitespace(cc).toLowerCase().contains("sum(")){
			        		 cc= cc+" as groupBySumField";
			        	}
			        	if(StringUtils.deleteWhitespace(cc).toLowerCase().contains("avg(")){
			        		 cc= cc+" as groupByAvgField";
			        	}
			        	
		        	}else{
		        		cc=StringUtils.toUnderScoreCase(cc);
		        	}
		        	 super.add(index,(E) cc);
		        }
	    	super.add(index, element);
	    }

		public boolean isConvertType() {
			return convertType;
		}

		public void setConvertType(boolean convertType) {
			this.convertType = convertType;
		}
	    
}
