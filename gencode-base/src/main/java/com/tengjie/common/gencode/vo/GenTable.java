/**
 * Copyright &copy; 2015-2020 <a href="http://http://www.liuliangqb.com/">GenPLus</a> All rights reserved.
 */
package com.tengjie.common.gencode.vo;

import java.util.List;

import org.hibernate.validator.constraints.Length;

import com.google.common.collect.Lists;

import com.tengjie.common.utils.StringUtils;


/**
 * 业务表Entity
 * @author sjjt
 * @version 2017-5-15
 */
public class GenTable  {
	
	private static final long serialVersionUID = 1L;
	private String name; 	// 名称
	private String comments;		// 描述
	private String className;		// 实体类名称
	private String parentTable;		// 关联父表
	private String parentTableFk;		// 关联父表外键
	private String tableFieldXML;          //表字段属性集
    private String dbName;//数据库名称
    private String dbType;//数据库名称
    // 表列，对于非controller，这个是全部的字段列表；对于list.jsp这个是查询条件列表，listJspShowList是list.jsp显示列表
    private List<TableColumn> columnList = Lists.newArrayList();	
    
	private List<String> pkList; // 当前表主键列表
	
	public String getDbType()
    {
        return dbType;
    }

    public void setDbType(String dbType)
    {
        this.dbType = dbType;
    }

    public String getDbName()
    {
        return dbName;
    }

    public void setDbName(String dbName)
    {
        this.dbName = dbName;
    }

	public GenTable() {
		super();
	}



	@Length(min=1, max=200)
	public String getName() {
		return StringUtils.lowerCase(name);
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}
 
	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		if(className.toLowerCase().indexOf("tb")==0){
			className=className.substring(2);
		}
		this.className = className;
	}


	public String getParentTable() {
		return StringUtils.lowerCase(parentTable);
	}

	public void setParentTable(String parentTable) {
		this.parentTable = parentTable;
	}

	public String getParentTableFk() {
		return StringUtils.lowerCase(parentTableFk);
	}

	public void setParentTableFk(String parentTableFk) {
		this.parentTableFk = parentTableFk;
	}

	public String getTableFieldXML() {
		return tableFieldXML;
	}

	public void setTableFieldXML(String tableFieldXML) {
		this.tableFieldXML = tableFieldXML;
	}

	public List<String> getPkList() {
		return pkList;
	}

	public void setPkList(List<String> pkList) {
		this.pkList = pkList;
	}

	

	public List<TableColumn> getColumnList() {
		return columnList;
	}

	public void setColumnList(List<TableColumn> columnList) {
		this.columnList = columnList;
	}

	

	/**
	 * 获取列名和说明
	 * @return
	 */
	public String getNameAndComments() {
		return getName() + (comments == null ? "" : "  :  " + comments)+ (dbName == null ? "" : "  ("+dbName+")");
	}

	/**
	 * 获取导入依赖包字符串
	 * @return
	 */
	public List<String> getImportList(){
		List<String> importList = Lists.newArrayList(); // 引用列表
		for (TableColumn column : getColumnList()){
			if (column.getIsNotBaseField() || ( ("createDate".equals(column.getSimpleJavaField()) || "updateDate".equals(column.getSimpleJavaField())))){
				// 导入类型依赖包， 如果类型中包含“.”，则需要导入引用。
				if (StringUtils.indexOf(column.getJavaType(), ".") != -1 && !importList.contains(column.getJavaType())){
					importList.add(column.getJavaType());
				}
			}
			if (column.getIsNotBaseField()){
				// 导入JSR303、Json等依赖包
				for (String ann : column.getAnnotationList()){
					String temp=StringUtils.substringBeforeLast(ann, "(");
					if(temp.contains("("))ann=StringUtils.substringBeforeLast(temp, "(");
					if (!importList.contains(StringUtils.substringBeforeLast(ann, "("))){
						importList.add(StringUtils.substringBeforeLast(ann, "("));					
					}
				}
			}
		}

		return importList;
	}
	

	/**
	 * 是否存在create_date列
	 * @return
	 */
	public Boolean getCreateDateExists(){
		for (TableColumn c : columnList){
			if ("create_date".equals(c.getName())){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 是否存在update_date列
	 * @return
	 */
	public Boolean getUpdateDateExists(){
		for (TableColumn c : columnList){
			if ("update_date".equals(c.getName())){
				return true;
			}
		}
		return false;
	}

	/**
	 * 是否存在del_flag列
	 * @return
	 */
	public Boolean getDelFlagExists(){
		for (TableColumn c : columnList){
			if ("del_flag".equals(c.getName())){
				return true;
			}
		}
		return false;
	}
}


