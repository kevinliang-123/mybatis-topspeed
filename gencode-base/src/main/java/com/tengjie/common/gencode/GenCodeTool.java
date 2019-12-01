package com.tengjie.common.gencode;

import java.util.List;
import java.util.Map;

import com.tengjie.common.gencode.tools.BackstageGenCode;
import com.tengjie.common.gencode.vo.GenConfig;
import com.tengjie.common.gencode.vo.TableMetadata;
import com.tengjie.common.utils.FileUtils;
import com.tengjie.common.utils.PackageUtil;
import com.tengjie.common.utils.SpringContextHolder;
import com.tengjie.common.utils.StringUtils;
import com.tengjie.common.utils.freemarker.GenTemplate;





public class GenCodeTool {
	private String projectBasePath;
	private List<String> tableNameList;
	private Map<String,String> includeType;
	private String packagePathUrl;//包名的前缀，会在这个报名后面加上entity、dao、controller、service等
	private String custModelName;//标准包名后面，对不同类可以再加自定义的子包名
	private boolean ifOverWrite=true;
	private String toCode(String templateType,List<GenTemplate> templateList,Map<String, Object> modelMap ,String tableName) throws Exception {
		StringBuffer sb=new StringBuffer();
		if(StringUtils.isEmpty(templateType)){
//			for (GenTemplate tpl : templateList){
//				sb.append("start*******************************************************************");
//				sb.append("\n");
//				sb.append(GenUtils.generateFileContent(tpl, modelMap));
//				//System.out.println(GenUtils.generateFileContent(tpl, modelMap));
//				sb.append("\n");
//				sb.append("end*******************************************************************");
//				sb.append("\n");
//			}
			
		}else{
		    if("1".equals(templateType)){
		    	String className=PackageUtil.findEntityNameByDbTableName(tableName, false);
		    	String bigClassName=PackageUtil.findEntityNameByDbTableName(tableName, true);
		    	String entityfinalPath=findJavaFinalPath("entity", packagePathUrl , custModelName, bigClassName, className);
				String extendsEntity=readExtendsEntityName(entityfinalPath);
				modelMap.put("extendEntity", extendsEntity);//只是给entity使用，到底是继承baseentity、dataentity或者extentity
		    	for (GenTemplate tpl : templateList){
		    		if(tpl.getName().equals("entity")){
		    			sb.append(GenUtils.generateFileContent(tpl, modelMap));
		    		}
				}
		    }
            if("2".equals(templateType)){
            	for (GenTemplate tpl : templateList){
		    		if(tpl.getName().equals("mapper")){
		    			sb.append(GenUtils.generateFileContent(tpl, modelMap));
		    		}
				}
            }
            if("3".equals(templateType)){
            	for (GenTemplate tpl : templateList){
		    		if(tpl.getName().equals("controller")){
		    			GenUtils.genController(packagePathUrl, projectBasePath,modelMap,tableName);
		    			sb.append(GenUtils.generateFileContent(tpl, modelMap));
		    		}
				}
            }
            if("4".equals(templateType)){
            	for (GenTemplate tpl : templateList){
		    		if(tpl.getName().equals("service")){
                          sb.append(GenUtils.generateFileContent(tpl, modelMap));
		    		}
				}
            }
            if("5".equals(templateType)){
            	for (GenTemplate tpl : templateList){
		    		if(tpl.getName().equals("extDao")){
                          sb.append(GenUtils.generateFileContent(tpl, modelMap));
		    		}
				}
            }
            if("6".equals(templateType)){
            	for (GenTemplate tpl : templateList){
		    		if(tpl.getName().equals("dao")){
                          sb.append(GenUtils.generateFileContent(tpl, modelMap));
		    		}
				}
            }
            if("7".equals(templateType)){
            	for (GenTemplate tpl : templateList){
		    		if(tpl.getName().equals("extMapper")){
		    			sb.append(GenUtils.generateFileContent(tpl, modelMap));
		    		}
				}
            }
           
		}
		return sb.toString();
	}
	public void startGen() throws Exception {
		GenConfig config = GenUtils.getConfig();
		for(String dbTableName:tableNameList) {
			dbTableName=dbTableName.toLowerCase();
			TableMetadata tm=GenUtils.buildTableMetadataBydbTableName(dbTableName);
			
			List<GenTemplate> templateList =GenUtils.getTemplateList(config, null, false);
			Map<String, Object> modelMap = GenUtils.getDataModel(tm,custModelName,"",packagePathUrl);
			modelMap.put("table", GenUtils.getGenTable(dbTableName));
			for(String templateType : includeType.keySet()){
				System.out.println("********************");
				String content=toCode( templateType,templateList, modelMap , dbTableName);
				writeToFile(templateType,content,dbTableName,ifOverWrite);
			}
			
			//modelMap.put("table", GenUtils.getGenTable(tableId));
		}
	}
	private void writeToFile(String templateType,String content,String dbTableName,boolean ifOverWrite) {
		String finalPath=null;
		String className=PackageUtil.findEntityNameByDbTableName(dbTableName, false);
    	String bigClassName=PackageUtil.findEntityNameByDbTableName(dbTableName, true);
		switch (templateType) {
		case "1":
			finalPath=findJavaFinalPath("entity", packagePathUrl , custModelName, bigClassName, className);
			if(ifOverWrite){
				FileUtils.writeToFile(finalPath, content, "utf-8", false);
			}else{
				System.out.println(content);
			}
			break;
		case "2":
			finalPath=projectBasePath+"/src/main/resources/mappings/modules/"+(StringUtils.isEmpty(custModelName)?className:custModelName)+"/"+bigClassName+"MainDao.xml";
			if(ifOverWrite){
				FileUtils.writeToFile(finalPath, content, "utf-8", false);
			}else{
				System.out.println(content);
			}
			break;
		case "3":
			finalPath=findJavaFinalPath("web", packagePathUrl , custModelName, bigClassName+"Controller", className);
    		if(ifOverWrite){
    			FileUtils.writeToFile(finalPath, content, "utf-8", false);
			}else{
				System.out.println(content);
			}
			break;
		case "4":
			finalPath=findJavaFinalPath("service", packagePathUrl , custModelName, bigClassName+"Service", className);
			if(ifOverWrite){
				FileUtils.writeToFile(finalPath, content, "utf-8", false);
			}else{
				System.out.println(content);
			}
			break;
		case "5":
			finalPath=findJavaFinalPath("dao", packagePathUrl , custModelName, bigClassName+"Dao", className);
			if(ifOverWrite){
				FileUtils.writeToFile(finalPath, content, "utf-8", false);
			}else{
				System.out.println(content);
			}
			break;
		case "6":
			finalPath=findJavaFinalPath("dao", packagePathUrl , custModelName, bigClassName+"MainDao", className);
			if(ifOverWrite){
				FileUtils.writeToFile(finalPath, content, "utf-8", false);
			}else{
				System.out.println(content);
			}
			break;
		case "7":
			finalPath=projectBasePath+"/src/main/resources/mappings/modules/"+(StringUtils.isEmpty(custModelName)?className:custModelName)+"/"+bigClassName+"Dao.xml";
			if(ifOverWrite){
				FileUtils.writeToFile(finalPath, content, "utf-8", false);
			}else{
				System.out.println(content);
			}
			break;
		default:
			break;
		}
	}
	/**
	 * 覆盖java的 时候，有时到底是继承baseentity、dataentity或者extentity，需要读取一下
	 * @param finalPath
	 * @return
	 */
	private String readExtendsEntityName(String finalPath) {
		String oldContent=FileUtils.readFileToString(finalPath);
		String extendsEntity=null;
		if(StringUtils.isNotEmpty(oldContent)&&oldContent.contains("ExtEntity")){
			extendsEntity="ExtEntity";
		}
		return extendsEntity;
	}
	public GenCodeTool(String projectBasePath, List<String> tableNameList, Map<String, String> includeType) {
		super();
		this.projectBasePath = projectBasePath;
		this.tableNameList = tableNameList;
		this.includeType = includeType;
	}
	 /**
     * 获得在生成java类时的最终路径，包括entity、controller、service、dao
     * @param prefix
     * @param packagePath
     * @param moduleName
     * @param bigClassName
     * @param smallClassName
     * @return
     */
    private String findJavaFinalPath(String prefix,String packagePath ,String moduleName,String className,String smallClassName){
		String tempPath=dealPackagePath(packagePath,moduleName,prefix,smallClassName);
		String finalPath=projectBasePath+"/src/main/java/"+tempPath+className+".java";
		return finalPath;
    }
    private String dealPackagePath(String packagePath,String moduleName,String prefix,String smallClassName){
    	String tempPath=packagePath+"."+(StringUtils.isEmpty(moduleName)?smallClassName:moduleName);
    	tempPath=tempPath.replace(".", "/");
    	tempPath=tempPath+"/"+prefix+"/";
    	return tempPath;
    }

	public List<String> getTableNameList() {
		return tableNameList;
	}
	public void setTableNameList(List<String> tableNameList) {
		this.tableNameList = tableNameList;
	}
	public Map<String, String> getIncludeType() {
		return includeType;
	}
	public void setIncludeType(Map<String, String> includeType) {
		this.includeType = includeType;
	}
	public String getProjectBasePath() {
		return projectBasePath;
	}
	public void setProjectBasePath(String projectBasePath) {
		this.projectBasePath = projectBasePath;
	}
	public String getPackagePathUrl() {
		return packagePathUrl;
	}
	public void setPackagePathUrl(String packagePathUrl) {
		this.packagePathUrl = packagePathUrl;
	}
	public String getCustModelName() {
		return custModelName;
	}
	public void setCustModelName(String custModelName) {
		this.custModelName = custModelName;
	}
	public boolean isIfOverWrite() {
		return ifOverWrite;
	}
	public void setIfOverWrite(boolean ifOverWrite) {
		this.ifOverWrite = ifOverWrite;
	}
	
}
