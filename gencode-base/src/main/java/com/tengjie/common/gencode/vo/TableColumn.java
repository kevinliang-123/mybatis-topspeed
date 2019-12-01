/**
 * Copyright &copy; 2015-2020 <a href="http://http://www.liuliangqb.com/">GenPLus</a> All rights reserved.
 */
package com.tengjie.common.gencode.vo;

import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tengjie.common.utils.StringUtils;


/**
 * 业务表字段Entity
 * @author sjjt
 * @version 2017-5-15
 */
public class TableColumn {
	public static final String area_list_querycond="area_list_querycond";//列表查询条件区
	public static final String area_list_tabledata="area_list_tabledata";//列表数据显示区
	public static final String area_form="area_form";//form页面区域
	public static final String area_defalut="area_defalut";//默认的都会有，如果某些区域不同于默认的，则在某个上面三个区域中进行单独设置
	private static final long serialVersionUID = 1L;
	private String tableName;
	private String name; 		// 列名
	private String comments;	// 描述 
	private String jdbcType;	// JDBC类型
	private Integer pricisionOne;//jdbc的精度
	private Integer pricisionTwo;
	private String javaType;	// JAVA类型
	private String javaField;	// JAVA字段名
	private String isPk;		// 是否主键（1：主键）
	private String isNull;		// 是否可为空（1：可为空；0：不为空）
	private String ifTimeStamp;//是否时间戳，也就是是否带时分秒
	private int sort;//字段在表中的顺序
	private String refTableName;//关联表表名
	private String refFieldName;//关联表字段
	private TableColumn refTableColumn;//关联表的TableColumn信息
	private Long characterMaxinumLength;//只有字符串类型，text、char、varchar才有这个，设置的长度值，如果为-1表示不限制
	private List<String> formFieldValidateList;//form编辑页面的验证类型 email\pwd等
	//扩展信息，对于jointable形式获得数据源，在使用smart的时候，要获得与其关联的List<JoinTableToolsInfoBean> ，
	//但是查找很慢，所以在数据初始化时，把数据保留起来，这样后面不用再查找了，但是要注意改动joinfinder的时候，重启程序的问题，
	//后续如果有需要，这个也可以存储其他的信息，后面应该用什么类型获取在具体程序中判断
	private Object extendInfo;
//	private String widgetType;
	//list页面的查询控件类型、form的控件的类型；下拉框、日期框等，也可以作为list页面的数据区取数据的依据,比如说，如果是下拉框类型则进行finder查找等
	//目前对原来的widgetType进行了改造，原因是，比如说xxxUserId,这如果是在from页面的时候，肯定会smart成隐藏字段，但是如果在tabledata区域，如果
	//肯定是不隐藏（因为tabledata字段不需要隐藏字段，其他地方有解释），那么他肯定是要显示用户的名字，所以同一个字段在不同的区域的控件类型是有可能不同的
	//但是，有一些控件在不同区域是相同的
	//结论：widgetTypeMap存储了不同区域的控件类型，一共有上面的4个常量区域，下面会有个getwidgetType方法，一个可变参数，即区域，不同区域按照自己的区域传入来查找，如果不传区域
	//或则没有按照你的区域单独配置widgetType，则会查找area_all作为返回，在dbtool进行数据初始化时，一定有area_all，但是其他的区域自定义，按照不同的字段来定义是否在不同区域不同的控件类型
	private Map<String,String> widgetTypeMap=Maps.newHashMap();
	private String dateFormatType;//扩展参数， 日期格式化类型、
	private String comboBoxSuffixIndex;//对于下拉框来说是标准调用的后缀 ,因为同一个表同一个字段可能要返回不同结果                                         
	public TableColumn() {
		super();
	}


	public TableColumn(String tableName){
		this.tableName = tableName;
	}

	@Length(min=1, max=200)
	public String getName() {
		return StringUtils.lowerCase(name);
	}

	public void setName(String name) {
		this.name = name;
	}

	

	public String getIsPk() {
		return isPk;
	}


	public void setIsPk(String isPk) {
		this.isPk = isPk;
	}



	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public String getJdbcType() {
		return StringUtils.lowerCase(jdbcType);
	}

	public void setJdbcType(String jdbcType) {
		this.jdbcType = jdbcType;
	}

	public String getJavaType() {
		return javaType;
	}

	public void setJavaType(String javaType) {
		this.javaType = javaType;
	}

	public String getJavaField() {
		return javaField;
	}

	public void setJavaField(String javaField) {
		this.javaField = javaField;
	}

	public String getIsNull() {
		return isNull;
	}

	public void setIsNull(String isNull) {
		this.isNull = isNull;
	}


	/**
	 * 获取列名和说明
	 * @return
	 */
	public String getNameAndComments() {
		return getName() + (comments == null ? "" : "  :  " + comments);
	}
	
	/**
	 * 获取最大长度,实际在页面被调用
	 * @return
	 */
	public String getMaxLength() {
		String ml="0";
		if(pricisionOne!=null)
		if(pricisionOne.intValue()>0){
			ml=pricisionOne.intValue()+"";
		}
		return ml;
	}
	/**
	 * 设置字段的最大长度
	 * @return
	 */
	public void setMaxLength(Integer maxLength) {
		pricisionOne=maxLength;
	}
//	/**
//	 * 获取字符串长度
//	 * @return
//	 */
//	public String getDataLength(){
//		String[] ss = StringUtils.split(StringUtils.substringBetween(getJdbcType(), "(", ")"), ",");
//		if (ss != null && ss.length == 1){// && "String".equals(getJavaType())){
//			return ss[0];
//		}
//		return "0";
//	}
	/**
	 * 验证类型，对于数据类型，自动加上必须为数字，无需前端选择配置
	 * @return
	 */
	public String getValidateType() {
		String validateType=null;
		 if (StringUtils.startsWithIgnoreCase(getJdbcType(), "BIGINT")
					|| StringUtils.startsWithIgnoreCase(getJdbcType(), "NUMBER")){
				// 如果是浮点型
				String[] ss = StringUtils.split(StringUtils.substringBetween(getJdbcType(), "(", ")"), ",");
				if (ss != null && ss.length == 2 && Integer.parseInt(ss[1])>0){
					//setJavaType("Double");
				}
				// 如果是整形
				else if (ss != null && ss.length == 1 && Integer.parseInt(ss[0])<=10){
					//setJavaType("Integer");
				}
				// 长整形
				else{
					//setJavaType("Long");
				}
				validateType="number";
			}else if (StringUtils.startsWithIgnoreCase(getJdbcType(), "DECIMAL")
			    ||StringUtils.startsWithIgnoreCase(getJdbcType(), "NUMERIC")){
			  //  setJavaType("java.math.BigDecimal");
				validateType="number";
			}else if (StringUtils.startsWithIgnoreCase(getJdbcType(), "TINYINT")){
	           // setJavaType("Integer");
				validateType="number";
	        }else if (StringUtils.startsWithIgnoreCase(getJdbcType(), "INT")){
	            //setJavaType("Integer");
	        	validateType="number";
	        }
		 return validateType;
	}
	
	
//	/**
//	 * 获取字符串长度
//	 * @return
//	 */
//	public String getDataLength(){
//		String[] ss = StringUtils.split(StringUtils.substringBetween(getJdbcType(), "(", ")"), ",");
//		if (ss != null && ss.length == 1){// && "String".equals(getJavaType())){
//			return ss[0];
//		}
//		return "0";
//	}

	/**
	 * 获取简写Java类型
	 * @return
	 */
	public String getSimpleJavaType(){
		if ("This".equals(getJavaType())){
			return StringUtils.capitalize(tableName);
		}
		return StringUtils.indexOf(getJavaType(), ".") != -1 
				? StringUtils.substringAfterLast(getJavaType(), ".")
						: getJavaType();
	}
	
	/**
	 * 获取简写Java字段
	 * @return
	 */
	public String getSimpleJavaField(){
		return StringUtils.substringBefore(getJavaField(), ".");
	}
	
	/**
	 * 获取Java字段，如果是对象，则获取对象.附加属性1
	 * @return
	 */
	public String getJavaFieldId(){
		return StringUtils.substringBefore(getJavaField(), "|");
	}
	
	/**
	 * 获取Java字段，如果是对象，则获取对象.附加属性2
	 * @return
	 */
	public String getJavaFieldName(){
		String[][] ss = getJavaFieldAttrs();
		return ss.length>0 ? getSimpleJavaField()+"."+ss[0][0] : "";
	}
	
	public static void main(String[]args){
	    
	    System.out.println(StringUtils.substringBefore("abc_cd", "."));
	    
	}
	
	/**
	 * 获取Java字段，所有属性名
	 * @return
	 */
	public String[][] getJavaFieldAttrs(){
		String[] ss = StringUtils.split(StringUtils.substringAfter(getJavaField(), "|"), "|");
		String[][] sss = new String[ss.length][2];
		if (ss!=null){
			for (int i=0; i<ss.length; i++){
				sss[i][0] = ss[i];
				sss[i][1] = StringUtils.toUnderScoreCase(ss[i]);
			}
		}
		return sss;
	}
	
	/**
	 * 获取列注解列表
	 * @return
	 */
	public List<String> getAnnotationList(){
		List<String> list = Lists.newArrayList();
		// 导入Jackson注解
		if ("This".equals(getJavaType())){
			list.add("com.fasterxml.jackson.annotation.JsonBackReference");
		}
		if ("java.util.Date".equals(getJavaType())){
			list.add("com.fasterxml.jackson.annotation.JsonFormat(pattern = \"yyyy-MM-dd HH:mm:ss\")");
		}
		
		// 导入JSR303验证依赖包
		if (!"1".equals(getIsNull()) && !"String".equals(getJavaType())){
			//原来是有这段代码的，对于数值类型，默认在实体entity会加注解@NotNull，这样是不对的，应该是根据业务判断的
			//list.add("javax.validation.constraints.NotNull(message=\""+getComments()+"不能为空\")");
		}
		
		else if (!"1".equals(getIsNull()) && "String".equals(getJavaType()) && !"0".equals(pricisionOne)){
			list.add("org.hibernate.validator.constraints.Length(min=0, max="+pricisionOne
					+", message=\""+getComments()+"长度必须介于 0 和 "+pricisionOne+" 之间\")");
		}
		else if ("String".equals(getJavaType()) && !"0".equals(pricisionOne)){
			if(pricisionOne!=null)
			list.add("org.hibernate.validator.constraints.Length(min=0, max="+pricisionOne
					+", message=\""+getComments()+"长度必须介于 0 和 "+pricisionOne+" 之间\")");
		}
		return list;
	}
	
	/**
	 * 获取简写列注解列表
	 * @return
	 */
	public List<String> getSimpleAnnotationList(){
		List<String> list = Lists.newArrayList();
		for (String ann : getAnnotationList()) {
			list.add(StringUtils.substringAfterLast(ann, "."));
		}
		return list;
	}
	
	/**
	 * 是否是基类字段
	 * @return
	 */
	public Boolean getIsNotBaseField(){
		if ((!StringUtils.equals(getSimpleJavaField(), "id")) &&
				(!StringUtils.equals(getSimpleJavaField(), "remarks")) &&
				(!StringUtils.equals(getSimpleJavaField(), "createBy")) &&
				(!StringUtils.equals(getSimpleJavaField(), "createDate")) &&
				(!StringUtils.equals(getSimpleJavaField(), "updateBy")) &&
				(!StringUtils.equals(getSimpleJavaField(), "updateDate")) &&
				(!StringUtils.equals(getSimpleJavaField(), "delFlag"))) return Boolean.valueOf(true);
		return
				Boolean.valueOf(false);
	}


	public String getIfTimeStamp() {
		return ifTimeStamp;
	}


	public void setIfTimeStamp(String ifTimeStamp) {
		this.ifTimeStamp = ifTimeStamp;
	}


	public String getTableName() {
		return tableName;
	}


	public void setTableName(String tableName) {
		this.tableName = tableName;
	}


	public int getSort() {
		return sort;
	}


	public void setSort(int sort) {
		this.sort = sort;
	}







	public List<String> getFormFieldValidateList() {
		return formFieldValidateList;
	}


	public void setFormFieldValidateList(List<String> formFieldValidateList) {
		this.formFieldValidateList = formFieldValidateList;
	}
	public void addIntoFormFieldValidateList(String formFieldValidate) {
		if(this.formFieldValidateList==null){
			this.formFieldValidateList=Lists.newArrayList();
		}
		formFieldValidateList.add(formFieldValidate);
	}

    /**
     * 根据区域获得widgetType，不指定区域则从默认区域获得
     * @param belongArea
     * @return
     */
	public String findWidgetType(String belongArea) {
		String widgetType=null;
		if(StringUtils.isNotEmpty(belongArea)) {
			widgetType=widgetTypeMap.get(belongArea);
		}
		if(StringUtils.isEmpty(widgetType)) {
			widgetType=widgetTypeMap.get(area_defalut);
		}
		return widgetType;
	}

    /**
     * 向指定区域放置widgetType，若不指定区域，则默认放到area_defalut中，注意area_defalut必须要设置有内容
     * @param widgetType
     * @param belongArea
     */
	public void putIntoWidgetTypeMap(String widgetType,String ...belongArea) {
		if(belongArea.length<1||StringUtils.isEmpty(belongArea[0])) {
			widgetTypeMap.put(area_defalut, widgetType);
		}else {
			widgetTypeMap.put(belongArea[0], widgetType);
		}
		
	}


	public String getDateFormatType() {
		return dateFormatType;
	}


	public void setDateFormatType(String dateFormatType) {
		this.dateFormatType = dateFormatType;
	}


	public String getComboBoxSuffixIndex() {
		return comboBoxSuffixIndex;
	}


	public void setComboBoxSuffixIndex(String comboBoxSuffixIndex) {
		this.comboBoxSuffixIndex = comboBoxSuffixIndex;
	}


	public Integer getPricisionOne() {
		return pricisionOne;
	}


	public void setPricisionOne(Integer pricisionOne) {
		this.pricisionOne = pricisionOne;
	}


	public Integer getPricisionTwo() {
		return pricisionTwo;
	}


	public void setPricisionTwo(Integer pricisionTwo) {
		this.pricisionTwo = pricisionTwo;
	}


	public Object getExtendInfo() {
		return extendInfo;
	}


	public void setExtendInfo(Object extendInfo) {
		this.extendInfo = extendInfo;
	}


	public Map<String, String> getWidgetTypeMap() {
		return widgetTypeMap;
	}


	public void setWidgetTypeMap(Map<String, String> widgetTypeMap) {
		this.widgetTypeMap = widgetTypeMap;
	}


	public String getRefTableName() {
		return refTableName;
	}


	public void setRefTableName(String refTableName) {
		this.refTableName = refTableName;
	}


	


	public String getRefFieldName() {
		return refFieldName;
	}


	public void setRefFieldName(String refFieldName) {
		this.refFieldName = refFieldName;
	}


	public TableColumn getRefTableColumn() {
		return refTableColumn;
	}


	public void setRefTableColumn(TableColumn refTableColumn) {
		this.refTableColumn = refTableColumn;
	}


	public Long getCharacterMaxinumLength() {
		return characterMaxinumLength;
	}


	public void setCharacterMaxinumLength(Long characterMaxinumLength) {
		this.characterMaxinumLength = characterMaxinumLength;
	}




}


