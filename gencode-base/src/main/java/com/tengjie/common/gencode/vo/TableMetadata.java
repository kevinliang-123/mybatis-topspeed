package com.tengjie.common.gencode.vo;

import org.hibernate.validator.constraints.Length;

import com.tengjie.common.utils.StringUtils;

/**
 * TB_TABLE_METADATAEntity
 * 
 */
public class TableMetadata {
	
	private static final long serialVersionUID = 1L;
	private String tableName;		// 工作时间
	private String tableChineseName;		// 中文名
	private String tableDesc;		// 业务描述
	private String primaryKeyName;		// 主键名
	public static  String _TB_TABLE_NAME_="tbTableMetadata";
	public static  String _TABLENAME="tableName";		// 工作时间
	public static  String _TABLECHINESENAME="tableChineseName";		// 中文名
	public static  String _TABLEDESC="tableDesc";		// 业务描述
	public static  String _PROJECTID="projectId";		// project_id

	public TableMetadata() {
		super();
	}

	@Length(min=1, max=100, message="工作时间长度必须介于 1 和 100 之间")
	public String getTableName() {
		return tableName;
	}
	
	public String getTableNameFirstUp(boolean bl) {
		String step1=tableName.toLowerCase().contains("tb_")?tableName.toLowerCase().substring(3):tableName;
		String step2="";
		if(bl){
			step2=StringUtils.toCapitalizeCamelCase(step1);
		}else{
			step2=StringUtils.toCamelCase(step1);
		}
				
		return step2;
	}
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	
	@Length(min=1, max=100, message="中文名长度必须介于 1 和 100 之间")
	public String getTableChineseName() {
		return tableChineseName;
	}

	public void setTableChineseName(String tableChineseName) {
		this.tableChineseName = tableChineseName;
	}
	
	@Length(min=1, max=300, message="业务描述长度必须介于 1 和 300 之间")
	public String getTableDesc() {
		return tableDesc;
	}

	public void setTableDesc(String tableDesc) {
		this.tableDesc = tableDesc;
	}
	
	
	
	public String getPrimaryKeyName() {
		return primaryKeyName;
	}

	public void setPrimaryKeyName(String primaryKeyName) {
		this.primaryKeyName = primaryKeyName;
	}

}