package com.tengjie.common.persistence;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.tengjie.common.persistence.util.QueryParamInfoVO;
import com.tengjie.common.service.CrudService;
import com.tengjie.common.utils.ListUtils;
import com.tengjie.common.utils.MyStringBuffer;
import com.tengjie.common.utils.Reflections;
import com.tengjie.common.utils.SpringContextHolder;
import com.tengjie.common.utils.StringUtils;


public class JoinTableBean implements Serializable  {
	private static final long serialVersionUID = 1L;
	public static String LEFT_JOIN = " left join ";
	public static String RIGHT_JOIN = " right join ";
	public static String INNER_JOIN = " inner join ";
	public static String ASC = "ASC";
	public static String DESC = "DESC";
	private  String mainTableName;//默认是不需要填写的，即关联的主表名，默认用mapper中的当前dao中的表名，但是有的如 main left join a on main.id=a.id left join b on a.id=b.id时，即实际在关联时，字表a作为了字表b的主表时，需要指定，指定的值为别名即刻，调用getTableAlias获得之前设置的
    private String tableName;//可以是一个表名，也可以是一个子查询，如果是表名，是tb开头的驼峰后的表名
    private String tableAlias;//可自动，不需要填写，但是一旦遇到冲突时需要手工设置，默认为表明名小写+10以下随机数，子查询情况的别名系统自动定义
    private String joinKind=LEFT_JOIN;//可自动，不需要填写，默认是left join
    private List<JoinOnBean> onConditions=new ArrayList();//key
    private Map<String,WhereValueBean> whereMap=new LinkedHashMap();//key为字段名，value为运算符 = ！=等
    private Map<String,String> selectMap=new HashMap();//key为字段名，value为别名，可以不填，默认为字段名
    private Map<String,String> orderyByMap=new LinkedHashMap<String, String>();//key为字段名，value为ASC或DESC，可以不填，默认为ASC
    private boolean ifSubQuery;
 
    
    
	public JoinTableBean(String tableName, List<JoinOnBean> onConditions,
			Map<String, String> selectMap) {
		super();
		setTableName(tableName);
		this.onConditions = onConditions;
		this.selectMap = selectMap;
	}
	
	
	
	public JoinTableBean() {
		super();
		// TODO Auto-generated constructor stub
	}



	public JoinTableBean(String tableName, List<JoinOnBean> onConditions,
			Map<String, WhereValueBean> whereMap, Map<String, String> selectMap) {
		super();
		setTableName(tableName);
		this.onConditions = onConditions;
		this.whereMap = whereMap;
		this.selectMap = selectMap;
	}



	public JoinTableBean(String tableName, String tableAlias, String joinKind,
			List<JoinOnBean> onConditions, Map<String, WhereValueBean> whereMap,
			Map<String, String> selectMap) {
		super();
		setTableName(tableName);
		this.tableAlias = tableAlias;
		this.joinKind = joinKind;
		this.onConditions = onConditions;
		this.whereMap = whereMap;
		this.selectMap = selectMap;
	}

	public JoinTableBean joinInto(TjBaseEntity be) {
		be.putJoinTable(this);
		return this;
	}

	public String getTableName() {
		return tableName;
	}
	public void setTableName(String tableName) {
		ifSubQuery=ifSubQuery(tableName);
		if(!ifSubQuery){
			if(tableName.contains("_")){
				tableName=StringUtils.toCamelCase(tableName);
			}
		}
		this.tableName = tableName;
	}
	private boolean ifSubQuery(String joinName){
		boolean bl;
		bl=joinName.toLowerCase().contains("select")&&joinName.toLowerCase().contains("from");
		//union all select '1' as keycode ,'文章' as type union all select '2' as keycode ,'音频' as type
		//这种造数据的，是没有from的
		if(!bl&&joinName.contains(" ")&&joinName.length()>30) {
			bl=joinName.toLowerCase().contains("select")&&joinName.toLowerCase().contains("union");
		}
		return bl;
	}
	/**
	 * 别名加上小数点 如：tableName.
	 * @return
	 */
	public String getTableAliasDot() {
		return getTableAlias()+".";
	}
	public String getTableAlias() {
		if(StringUtils.isEmpty(tableAlias)){
			if(StringUtils.isNotEmpty(tableName)){
				if(ifSubQuery(tableName)){
					tableAlias="subtable"+( new Random().nextInt(9)+1);
				}else{
					tableAlias=tableName.toLowerCase()+( new Random().nextInt(9)+1)+( new Random().nextInt(9)+1);
				}
			}
		}
	
		return tableAlias;
	}
	/**
	 * 对于在joinTableFinder中的配置的关联条件，有时两张表中配置了多个关联条件，但是
	 * 有时是关联需要这多个字段关联，有时又只想用其中的一个字段关联，这是需要移除不需要的字段
	 * @param mainFieldName:主表的关联字段名,驼峰后的字段名即可，即bean中的字段名
	 * @param childFieldName:字表的关联字段名,驼峰后的字段名即可，即bean中的字段名
	 * @return 为true表示移除成功
	 */
    public boolean removeOnCondition(String mainFieldName,String childFieldName) {
        boolean bl=false;
        mainFieldName=StringUtils.toUnderScoreCase(mainFieldName);
        childFieldName=StringUtils.toUnderScoreCase(childFieldName);
        Iterator<JoinOnBean> it = this.onConditions.iterator();
        while(it.hasNext()){
        	JoinOnBean job = it.next();
            if(mainFieldName.equals(job.getMainFieldName())&&childFieldName.equals(job.getChildFieldName())) {
            	it.remove();
            	bl=true;
            	break;
            }    
        }
        return bl;
    }
    /**
	 * 对于在joinTableFinder中的配置的关联条件，有时两张表中配置了多个关联条件，但是
	 * 有时是关联需要这多个字段关联，有时又只想用其中的一个字段关联
	 * 本方法只是保留mainFieldNames的字段的关联条件，其他的都移除
	 * @param mainFieldNames:只保留mainFieldNames这些关联条件，其他的要移除，本字段内容主表的关联字段名,驼峰后的字段名即可，即bean中的字段名
	 * @return 为移除数量
	 */
    public int removeOtherOnCondition(String ...mainFieldNames) {
        int removeNum=0;//移除数量
        if(mainFieldNames.length<1)return removeNum;
        Map<String,String> mainFieldNamesMap=ListUtils.arrayToMap(mainFieldNames);
        Iterator<JoinOnBean> it = this.onConditions.iterator();
        while(it.hasNext()){
        	JoinOnBean job = it.next();
            if(!mainFieldNamesMap.containsKey(StringUtils.toCamelCase(job.getMainFieldName()))) {//不在mainFieldNamesMap中的关联字段要移除
            	if(job.getMainFieldName()!=null) {//不删除查询条件类，只删除关联条件类，job.getMainFieldName()表示是查询条件，不是关联条件
            		it.remove();
                	removeNum++;
            	}
            	
            }    
        }
       
        return removeNum;
    }
	public void setTableAlias(String tableAlias) {
		this.tableAlias = tableAlias;
	}
	public String getJoinKind() {
		return joinKind;
	}
	public void setJoinKind(String joinKind) {
		this.joinKind = joinKind;
	}
	public List<JoinOnBean> getOnConditions() {
		return onConditions;
	}
	public void setOnConditions(List<JoinOnBean> onConditions) {
		this.onConditions = onConditions;
	}
	
	public Map<String, WhereValueBean> getWhereMap() {
		return whereMap;
	}



	public void setWhereMap(Map<String, WhereValueBean> whereMap) {
		this.whereMap = whereMap;
	}
	
	  /**
	   * 放置select字段,此方法表示select字段的名称和as的别名相同
	   * @param key 为字段名
	   * @return
	   */
	   public JoinTableBean putSelect(String fieldName){
		 
		   this.selectMap.put(fieldName, addDoubleQuota(fieldName));
		   return this;
	   }
	   /**
		   * 批量放置select字段,此方法表示select字段的名称和as的别名相同
		   * @param key 为字段名
		   * @return
		   */
		   public void putSelectMutil(String ...fieldNames){
			 for(String fieldName:fieldNames) {
				 this.selectMap.put(fieldName, addDoubleQuota(fieldName));
				
			 }
			   
		   }
  /**
   * 放置select字段
   * @param fieldName 为字段名
   * @param fieldNameAlias 为字段别名
   * @return
   */
   public JoinTableBean putSelect(String fieldName,String fieldNameAlias){
	  
	   this.selectMap.put(fieldName, addDoubleQuota(fieldNameAlias));
	   return this;
   }
 
  
   private String addDoubleQuota(String fieldNameAlias){
	   fieldNameAlias="\""+fieldNameAlias+"\"";
	   return fieldNameAlias;
   }
   /**
    * 放置select字段
    * @param key 为字段名
    * @param value 为字段别名
    * @return
    */
    public JoinTableBean putMutiSelect(String ...args){
    	for(int i=0;i<args.length;i++){
    		this.selectMap.put(args[i], addDoubleQuota(args[i]));
    	}
 	   
 	   return this;
    }
    /**
     * 放置where条件,采用此方法，表示默认用like
     * @param key 为字段属性名称，如：userName
     * @param fieldValue:字段的值
     * @param whereValueFromField:有时设置了whereValue为null，表示值从bean中按到对应字段的值，但是有时所需的字段值并不是对应字段的值，而是指定的某个字段的值
	                  ，因此这里就是当为null的时候，看看这个有没有，优先用这个字段获得值。这个主要是queryCond的时候smartJoin指定了关联字段（一个主表的多个字段关联统一张表），smart的为了自动化，需要指定putwhere的字段值来源于某个重新定义的别名，以便区分开来
	
     * @return
     */
     public JoinTableBean putWhereLike(String key,Object fieldValue,String whereValueFromField){
    	 WhereValueBean wb=new WhereValueBean( JoinOnBean.SIGN_LIKE, fieldValue);
    	 wb.setWhereValueFromField(whereValueFromField);
     	this.whereMap.put(key,wb);
  	   return this;
     }
   /**
    * 放置where条件,采用此方法，表示默认用like
    * @param key 为字段属性名称，如：userName
    * @param fieldValue:字段的值
    * @return
    */
    public JoinTableBean putWhereLike(String key,Object fieldValue){
    	this.whereMap.put(key, new WhereValueBean( JoinOnBean.SIGN_LIKE, fieldValue));
 	   return this;
    }
    /**
     * 放置where条件,采用此方法，表示默认用between
     * @param key 为字段属性名称，如：userName
     * @param fieldValue:字段的值
     * @return
     */
    public JoinTableBean putWhereBetween(String key,Object fieldValue,Object fieldValue1){
    	this.whereMap.put(key, new WhereValueBean( JoinOnBean.SIGN_BETWEEN, fieldValue,fieldValue1));
 	   return this;
    }
    
    /**
     * 放置where条件,采用此方法，表示默认用in
     * @param key 为字段属性名称，如：userName
     * @param fieldValue:in形式的值，注意不需要加括号
     * @return
     */
     public JoinTableBean putWhereIn(String key,Object fieldValue){
     	this.whereMap.put(key, new WhereValueBean( JoinOnBean.SIGN_IN, fieldValue));
  	   return this;
     }
     /**
      * 将该字段变为 is null或者is not null
      * 本方法写反了，true应该为is not null；与entity中的写法保持一直,已修正2019-10-23
      * @param key：字段名称
      *  @param key： isnot为true时表示为is not null
      * @return
      */
      public JoinTableBean putWhereIsNull(String key,boolean ...isnot){
    		boolean nullornotnull=false;
    		if(isnot!=null&&isnot.length>0&&isnot[0])nullornotnull=true;
    		key=this.getTableAlias()+"."+StringUtils.toUnderScoreCase(key);
    		
    		if(nullornotnull){
    			this.whereMap.put(key, new WhereValueBean(  JoinOnBean.SIGN_IS_NOT_NULL));
    		}else{
    			this.whereMap.put(key, new WhereValueBean(JoinOnBean.SIGN_IS_NULL));
    		}
    		
   	   return this;
      }
      
   /**
    * 放置where条件,采用此方法，表示默认用=等于
    * @param key 为字段属性名称，如：userName
    * @param fieldValue:字段的值
    * @return
    */
    public JoinTableBean putWhere(String key,Object fieldValue){
    	this.whereMap.put(key, new WhereValueBean( JoinOnBean.SIGN_EQUAL, fieldValue));
 	   return this;
    }
    
   /**
    * 放置where条件,采用此方法，表示默认用=等于，且值能够在mapper中自动匹配
    * @param key 为字段属性名称，如：userName
    * @return
    */
    public JoinTableBean putWhere(String key){
    	this.whereMap.put(key, new WhereValueBean( JoinOnBean.SIGN_EQUAL, null));
 	   return this;
    }
    /**
     * 放置where条件,采用此方法，运算符根据传入的控制，运算符来自于JoinOnBean中，且值能够在mapper中自动匹配
     * @param key 为字段属性名称，如：userName
     * @param sign 运算符根据传入的控制，运算符来自于JoinOnBean中
     * @return
     */
     public JoinTableBean putWhereSign(String key,String sign){
     	this.whereMap.put(key, new WhereValueBean( sign, null));
  	   return this;
     }
     /**
      * 放置where条件,采用此方法，运算符根据传入的控制，运算符来自于JoinOnBean中，值为fieldValue，当fieldValue为空时，值能够在mapper中自动匹配
      * @param key 为字段属性名称，如：userName
      * @param sign 运算符根据传入的控制，运算符来自于JoinOnBean中
      * @param fieldValue:字段的值，当fieldValue为空时，值能够在mapper中自动匹配
      * @return
      */
      public JoinTableBean putWhereSign(String key,String sign,Object fieldValue){
      	this.whereMap.put(key, new WhereValueBean( sign, fieldValue));
   	   return this;
      }
   /**
    * 放置where条件,key为字段名，value类型为：WhereValueBean为运算符 = ！=等
    * @param key 为字段属性名称，如：userName
    * @param value，类型为：WhereValueBean，里面包含了运算符= !=等，并且可以直接传值进来,如：new WhereValueBean( JoinOnBean.SIGN_EQUAL, "abc")
    * @return
    */
    public JoinTableBean putWhere(String key,WhereValueBean value){
 	   this.whereMap.put(key, value);
 	   return this;
    }
    /**
     * 放置orderby字段
     * @param key 字段名
     * @param value 排序的顺序 JoinTableBean.ASC\JoinTableBean.DESC
     * @return
     */
     public JoinTableBean putOrderby(String key,String value){
  	   this.orderyByMap.put(key, value);
  	   return this;
     }
    /**
     * 放置on后面的关联及查询条件
     * @param job
     * @return
     */
     public JoinTableBean putOnCond(JoinOnBean job){
  	   this.onConditions.add(job);
  	   return this;
     }
     /**
      * 放置on后面的查询条件,保持原样
      * @param childFieldName 子表的字段名
      * @param childFieldValue 子表的字段的值
      * @return
      */
      public JoinTableBean putOnCondAppendSql(String onSql){
    	  JoinOnBean job= new JoinOnBean();
    	  job.setOnSql(onSql);
    	  job.setKeepIt(true);
   	     this.onConditions.add(job);
   	   return this;
      }
      /**
       * 放置on后面的查询条件
       * @param childFieldName 子表的字段名
       * @param childFieldValue 子表的字段的值
       * @return
       */
       public JoinTableBean putOnCondValue(String childFieldName,Object childFieldValue){
     	  JoinOnBean job= new JoinOnBean();
     	  job.setChildFieldName(childFieldName);
     	  job.setOnValue1(childFieldValue);
    	  this.onConditions.add(job);
    	   return this;
       }  
       /**
        * 放置on后面的between查询条件
        * @param childFieldName 子表的字段名
        * @param childFieldValue between第一个值
        * @param childFieldValue1 between第二个值
        * @param andOr 与上一个条件之间的关系是and还是or，默认and
        * @return
        */
        public JoinTableBean putOnCondValueBetween(String childFieldName,Object childFieldValue,Object childFieldValue1,String andOr){
          JoinOnBean job= new JoinOnBean();
          job.setChildFieldName(childFieldName);
          
          job.setOnValue1(childFieldValue);
          job.setOnValue2(childFieldValue1);
          if(StringUtils.isNotEmpty(andOr))job.setAndOr(andOr);
      	  job.setSign(ConditionBean.SIGN_BETWEEN);
     	  this.onConditions.add(job);
     	   return this;
        }  
       /**
        * 放置on后面的查询条件
        * @param childFieldName 子表的字段名
        * @param childFieldValue 子表的字段的值
        * @param operationSign 默认是等于
        * @param andOr 与上一个条件之间的关系是and还是or，默认and
        * @return
        */
        public JoinTableBean putOnCondValue(String childFieldName,Object childFieldValue,String operationSign,String andOr){
          JoinOnBean job= new JoinOnBean();
          job.setChildFieldName(childFieldName);
          job.setOnValue1(childFieldValue);
          if(StringUtils.isNotEmpty(andOr))job.setAndOr(andOr);
      	  if(StringUtils.isNotEmpty(operationSign))job.setSign(operationSign);
     	  this.onConditions.add(job);
     	   return this;
        }  
     /**
      * 放置on后面的查询条件
      * @param childFieldName 子表的字段名
      * @param childFieldValue 子表的字段的值
      * @param andOr 与上一个条件之间的关系是and还是or，默认and
      * @return
      */
      public JoinTableBean putOnCondValue(String childFieldName,Object childFieldValue,String andOr){
    	  JoinOnBean job= new JoinOnBean();
    	  job.setChildFieldName(childFieldName);
    	  job.setOnValue1(childFieldValue);
    	  if(StringUtils.isNotEmpty(andOr))job.setAndOr(andOr);
   	      this.onConditions.add(job);
   	   return this;
      }
     /**
      * 放置on后面的关联及查询条件
      * @param mainRelationFieldName 主表的关联字段名称
      * @param childRelationFieldName 子表的关联字段名称
      * @return
      */
      public JoinTableBean putOnCond(String mainRelationFieldName,String childRelationFieldName){
     
   	   this.onConditions.add(new JoinOnBean(mainRelationFieldName,childRelationFieldName));
   	   return this;
      }
      /**
       * 放置on后面的关联及查询条件
       * @param mainRelationFieldName 主表的关联字段名称
       * @param childRelationFieldName 子表的关联字段名称
       * @return
       */
       public JoinTableBean putOnCond(String mainRelationFieldName,String childRelationFieldName,String andOr){
    	   JoinOnBean temp=new JoinOnBean(mainRelationFieldName,childRelationFieldName);
    	   if(StringUtils.isNotEmpty(andOr))temp.setAndOr(andOr);
    	   this.onConditions.add(temp);
    	   return this;
       }
     /**
      * 快速获得关联的JoinTableBean
      * @param tableName :字表名称，注意如果数据库是tb开头的，如tbClientUser
      * @param mainRelationFieldName：主表的关联字段名称
      * @param childRelationFieldName：子表的关联字段名称
      * @return
      */
     public static JoinTableBean fastGetJtb(String tableName,String mainRelationFieldName,String childRelationFieldName){
    	 JoinTableBean jtb=new JoinTableBean();
    	  jtb.setTableName(tableName);
    	  jtb.putOnCond(mainRelationFieldName,childRelationFieldName);
    	  return jtb;
     }
     /**
      * 快速获得关联的JoinTableBean
      * @param tableName :字表名称，注意如果数据库是tb开头的，如tbClientUser
      * @param mainRelationFieldName：主表的关联字段名称
      * @param childRelationFieldName：子表的关联字段名称
      * @return
      */
     public static JoinTableBean fastGetJtb(String tableName,String mainRelationFieldName,String childRelationFieldName,String joinType,String andOr){
    	 JoinTableBean jtb=new JoinTableBean();
    	  jtb.setTableName(tableName);
    	  if(StringUtils.isNotEmpty(joinType))
    	  jtb.setJoinKind(joinType);
    	  jtb.putOnCond(mainRelationFieldName,childRelationFieldName,andOr);
    	 
    	  return jtb;
     }
     /**
  	 * 快速获得关联表bean，此方法支持需要主表别名情况 
  	 * @param mainTableAlias 主表的别名
  	 * @param tableName 关联表名，注意加tb
  	 * @param mainRelFieldName 主表关联字段
  	 * @param childRelFieldName 字表关联字段
  	 * @param selectMap 字表select的字段列表
  	 * @return
  	 */
  	public static JoinTableBean fastGetJtb(String mainTableAlias,String tableName,String mainRelFieldName,String childRelFieldName,String joinType,String andOr){
  		 JoinTableBean jtb=new JoinTableBean();
  		 if(StringUtils.isNotEmpty(mainTableAlias))
  		 jtb.setMainTableName(mainTableAlias);
  		 jtb.setTableName(tableName);
  		  if(StringUtils.isNotEmpty(joinType))
  	    	  jtb.setJoinKind(joinType);
  		 jtb.putOnCond(mainRelFieldName, childRelFieldName,andOr);
  		
  		 return jtb;
  	}
     /**
 	 * 快速获得关联表bean，此方法支持需要主表别名情况 
 	 * @param mainTableAlias 主表的别名
 	 * @param tableName 关联表名，注意加tb
 	 * @param mainRelFieldName 主表关联字段
 	 * @param childRelFieldName 字表关联字段
 	 * @param selectMap 字表select的字段列表
 	 * @return
 	 */
 	public static JoinTableBean fastGetJtb(String mainTableAlias,String tableName,String mainRelFieldName,String childRelFieldName){
 		 JoinTableBean jtb=new JoinTableBean();
 		 if(StringUtils.isNotEmpty(mainTableAlias))
 		 jtb.setMainTableName(mainTableAlias);
 		 jtb.setTableName(tableName);
 		 jtb.putOnCond(mainRelFieldName, childRelFieldName);
 		
 		 return jtb;
 	}
	
 	
	public Map<String, String> getSelectMap() {
		return selectMap;
	}
	public void setSelectMap(Map<String, String> selectMap) {
		this.selectMap = selectMap;
	}



	public  String getMainTableName() {
		return mainTableName;
	}



	public  void setMainTableName(String mainTableName) {
		this.mainTableName = mainTableName;
	}



	public boolean isIfSubQuery() {
		return ifSubQuery;
	}



	public Map<String, String> getOrderyByMap() {
		return orderyByMap;
	}



	


	public void setOrderyByMap(Map<String, String> orderyByMap) {
		this.orderyByMap = orderyByMap;
	}
    
    
}
