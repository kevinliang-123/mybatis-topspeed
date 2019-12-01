/**
 * Copyright &copy; 2015-2020 <a href="http://http://www.liuliangqb.com/">jujienet</a> All rights reserved.
 */
package com.tengjie.common.gencode;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


import com.tengjie.common.config.Global;
import com.tengjie.common.gencode.tools.BackstageGenCode;
import com.tengjie.common.gencode.tools.DBTool;
import com.tengjie.common.gencode.vo.Dict;
import com.tengjie.common.gencode.vo.GenConfig;
import com.tengjie.common.gencode.vo.GenTable;
import com.tengjie.common.gencode.vo.TableColumn;
import com.tengjie.common.gencode.vo.TableMetadata;
import com.tengjie.common.persistence.util.DbTablePrimaryKeyFieldConfig;

import com.tengjie.common.utils.DateUtils;
import com.tengjie.common.utils.FileUtils;
import com.tengjie.common.utils.FreeMarkers;
import com.tengjie.common.utils.MapUtils;
import com.tengjie.common.utils.MyStringBuffer;
import com.tengjie.common.utils.PackageUtil;
import com.tengjie.common.utils.SpringContextHolder;
import com.tengjie.common.utils.StringUtils;
import com.tengjie.common.utils.freemarker.GenTemplate;
import com.tengjie.common.utils.freemarker.JaxbMapper;

import freemarker.template.TemplateException;


/**
 * 代码生成工具类
 * @author lizw
 * @version 2017-5-15
 */
public class GenUtils {

	private static Logger logger = LoggerFactory.getLogger(GenUtils.class);
	
	/**
	 * 获取模板路径
	 * @return
	 */
	public static String getTemplatePath(){
		try{
			File file = new DefaultResourceLoader().getResource("").getFile();
			if(file != null){
				return file.getAbsolutePath() + File.separator + StringUtils.replaceEach(GenUtils.class.getName(), 
						new String[]{"util."+GenUtils.class.getSimpleName(), "."}, new String[]{"template", File.separator});
			}			
		}catch(Exception e){
			logger.error("{}", e);
		}

		return "";
	}
	/**
	 * 获取代码生成配置对象
	 * @return
	 */
	public static GenConfig getConfig(){
		return fileToObject("config.xml", GenConfig.class);
	}

	/**
	 * 根据分类获取模板列表
	 * @param config
	 * @return
	 */
	public static List<GenTemplate> getTemplateList(GenConfig config, String category, boolean ifonlyMapper){
		
		List<GenTemplate> templateList = Lists.newArrayList();
		if (config !=null && config.getCategoryList() != null && category !=  null){
			for (Dict e : config.getCategoryList()){
				if (category.equals(e.getValue())){
					templateList.add((GenTemplate) fileToObject(e.getValue(), GenTemplate.class));
				}
			}
		}else{
			for (Dict e : config.getCategoryList()){
			   templateList.add((GenTemplate) fileToObject(e.getValue(), GenTemplate.class));
			}
		}
		if(ifonlyMapper){
			List<GenTemplate> onlyMapperTempList=new ArrayList();
			for(GenTemplate gtp:templateList){
				if(gtp.getName().equals("mapper")||gtp.getName().equals("extMapper")||gtp.getName().equals("entity")){
					onlyMapperTempList.add(gtp);
				}
			}
			return onlyMapperTempList;
		}
		
		return templateList;
	}
	/**
	 * XML文件转换为对象
	 * @param fileName
	 * @param clazz
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T fileToObject(String fileName, Class<?> clazz){
		try {
			//System.out.println("*********"+new ClassPathResource("/templates/").getPath());
			String pathName = "/modules/gen/" + fileName;
		
			Resource resource = new ClassPathResource(pathName); 
			InputStream is = resource.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			StringBuilder sb = new StringBuilder();  
			while (true) {
				String line = br.readLine();
				if (line == null){ 
					break;
				}
				sb.append(line).append("\r\n");
			}
			if (is != null) {
				is.close();
			}
			if (br != null) {
				br.close();
			}
//			logger.debug("Read file content: {}", sb.toString());
			return (T) JaxbMapper.fromXml(sb.toString(), clazz);
		} catch (IOException e) {
			logger.warn("Error file convert: {}", e);
		}
//		String pathName = StringUtils.replace(getTemplatePath() + "/" + fileName, "/", File.separator);
//		logger.debug("file to object: {}", pathName);
//		String content = "";
//		try {
//			content = FileUtils.readFileToString(new File(pathName), "utf-8");
////			logger.debug("read config content: {}", content);
//			return (T) JaxbMapper.fromXml(content, clazz);
//		} catch (IOException e) {
//			logger.warn("error convert: {}", e.getMessage());
//		}
		return null;
	}
	/**
	 * 根据数据库表名获得表元数据信息
	 * @param dbTableName
	 * @return
	 */
	public static TableMetadata buildTableMetadataBydbTableName(String dbTableName) {
		TableMetadata tm=new TableMetadata();
		tm.setTableName(dbTableName);
		String tableChineseName=DBTool.getAllTable().get(dbTableName);
		tm.setTableChineseName(tableChineseName);
		tm.setPrimaryKeyName(javaPrimaryKeyName(dbTableName));
		return tm;
	}
	/**
	 * 根据数据库表名获得主键字段名，是java类型的字段，不是数据库字段
	 * @param dbTableName
	 * @return
	 */
	public static String javaPrimaryKeyName(String dbTableName) {
		dbTableName=StringUtils.toCamelCase(dbTableName);
		Map<String,TableColumn> allField=DBTool.getAllTableField().get(dbTableName);
		String primaryKey=null;
		for(TableColumn value : allField.values()){
		  if("1".equals(value.getIsPk())) {
			  primaryKey=value.getJavaField();
			  break;//只要一个，联合主键不考虑，必须有一个唯一的物理主键
		  }
		}
		return primaryKey;
	}
	/**
	 * 获得并组装genTable信息
	 * @param tpl
	 * @param model
	 * @param isReplaceFile
	 * @return
	 */
	public static GenTable getGenTable(String tableName){
	
		TableMetadata tm=buildTableMetadataBydbTableName(tableName);
			
		GenTable gt=new GenTable();
		gt.setName(tm.getTableName());
		gt.setComments(tm.getTableChineseName());
		gt.setClassName(tm.getTableName());
        gt.setDbName("mysql");
        List<TableColumn> columnList = Lists.newArrayList();
        tableName=StringUtils.toCamelCase(tableName);
        columnList=MapUtils.mapToList(DBTool.getAllTableField().get(tableName), false);
    
      gt.setColumnList(columnList);
	return gt;
	}
	/**
	 * 获取数据模型
	 * @param genScheme
	 * @return
	 */
	public static Map<String, Object> getDataModel(TableMetadata tmd,String custModuleName,String functionNameSimple,String packagePathUrl){
		Map<String, Object> model = Maps.newHashMap();
		model.put("primaryKeyName", tmd.getPrimaryKeyName());
		model.put("packageName", StringUtils.lowerCase(packagePathUrl));
		model.put("lastPackageName", StringUtils.substringAfterLast((String)model.get("packageName"),"."));
		String className=tmd.getTableName().contains("tb")?tmd.getTableName().substring(2):tmd.getTableName();
		if(StringUtils.isNotEmpty(custModuleName)){
			model.put("moduleName", custModuleName);
		}else{
			model.put("moduleName", StringUtils.uncapitalize(StringUtils.toCamelCase(className.toLowerCase())));
		}
		model.put("className", StringUtils.uncapitalize(StringUtils.toCamelCase(className)));
		model.put("ClassName", StringUtils.capitalize(StringUtils.toCamelCase(className)));
		
		model.put("functionName", tmd.getTableChineseName());
		model.put("functionNameSimple", functionNameSimple);
		model.put("functionVersion", DateUtils.getDate());
		model.put("subModuleName", "");
		model.put("functionAuthor", "自动生成");
		
		model.put("urlPrefix", model.get("moduleName").toString().toLowerCase()+"/"+model.get("className"));
		model.put("viewPrefix", //StringUtils.substringAfterLast(model.get("packageName"),".")+"/"+
				 model.get("moduleName").toString().toLowerCase()+"/"+model.get("className"));
		model.put("permissionPrefix", model.get("moduleName")+":"+model.get("className"));
		
		model.put("dbType", Global.getConfig("jdbc.type"));

		return model;
	}
	


  
//  /**
//	 * 获得并组装genTable信息
//	 * @param tpl
//	 * @param model
//	 * @param isReplaceFile
//	 * @return
//	 */
//	public static GenTable getGenTable(String tableName){
//	
//		TableMetadata tm=buildTableMetadataBydbTableName(tableName);
//			
//		GenTable gt=new GenTable();
//		gt.setName(tm.getTableName());
//		gt.setComments(tm.getTableChineseName());
//		gt.setClassName(tm.getTableName());
//        gt.setDbName("mysql");
//        List<TableColumn> columnList = Lists.newArrayList();
//        tableName=StringUtils.toCamelCase(tableName);
//        columnList=MapUtils.mapToList(DBTool.getAllTableField().get(tableName), false);
//    
//      gt.setColumnList(columnList);
//	return gt;
//	}
	private static String parseChinese(String str){
		if(StringUtils.isEmpty(str))return "数据库未命名";
		Pattern p = null;
		Matcher m = null;
		String value = null; 
		p = Pattern.compile("([\u4e00-\u9fa5]+)");
		m = p.matcher(str);
		while (m.find()) {
		value = m.group(0);
	    break;
		}
		return value;
	}
	
	/**
	 * 生成controller部分
	 * @param packagePath
	 * @param projectPath
	 * @param model
	 * @param mainTableName:数据库的表名，类似tb_user
	 * @throws Exception 
	 */
  public static void genController(String packagePath,String projectPath,Map<String,Object> model,String mainTableName) throws Exception{
		BackstageGenCode bgc=new BackstageGenCode();
    	bgc.getImportList().clear();
    	bgc.getAutoWireServiceList().clear();
    	bgc.getCallBackMethodList().clear();
    	bgc.setPackagePath(packagePath);
    	bgc.setProjectPath(projectPath+"/src/main/java");//留给生成项目中的GlobalStaticDict使用
    	String dynaQueryMethod="";
    	dynaQueryMethod=bgc.genListController(mainTableName);
//    	TableMetadata tm=bgc.getTableMetadataByTableName(projectId,mainTableName);
//    	Map<String,String> allField=bgc.findFieldListByTmatadataId(tm.getId());
    	/**************以下处理smart需要的字段*********/
    	String tbTableName=StringUtils.toCamelCase(mainTableName);
    	List<TableColumn> allcolumn=MapUtils.mapToList(DBTool.getAllTableField().get(tbTableName), false);
    	List<String> tempList=Lists.newArrayList();
    	for(TableColumn gtc:allcolumn){
    		if(gtc.getJavaField().equals("createBy.id")||gtc.getJavaField().equals("updateBy.id")||gtc.getJavaField().equals("delFlag")||gtc.getJavaField().equals("updateDate")||gtc.getJavaField().equals("createDate")) {
    			continue;
    		}
    		
    		tempList.add(PackageUtil.findEntityNameByDbTableName(mainTableName, true)+"._"+gtc.getJavaField());
    	}
    	List<String> smartFieldList=Lists.newArrayList();
    	List<String> smartFormFieldList=Lists.newArrayList();
    	
    	MyStringBuffer sb=MyStringBuffer.newInstance();
    	for(int i=0;i<tempList.size();i++){
    		String str=tempList.get(i);
    		if(i%3==0){
    			smartFieldList.add(sb.toString());
    			sb=MyStringBuffer.newInstance();
    			sb.append("dvo.tableDataSmart(",str);
    		}else if(i%3==2){
    			sb.append(",",str,");"+"\n");
    		}else{
    			sb.append(",",str);
    		}
    	}
    	if((tempList.size()-1)%3!=2){
    		sb.append(");");
    	
    	}
    	smartFieldList.add(sb.toString());
    	StringBuffer[] fieldConvertJoinTable=new StringBuffer[2];
    	fieldConvertJoinTable[0]=new StringBuffer();//fieldconvert信息
    	fieldConvertJoinTable[1]=new StringBuffer();//jointable信息
    	
    	
    	/**************以上处理smart需要的字段*********/
    	
    	model.put("dynaQueryMethod", dynaQueryMethod);
    	model.put("importList", bgc.getImportList());
    	model.put("autoWireServiceList", bgc.getAutoWireServiceList());
    	model.put("callBackMethodList", bgc.getCallBackMethodList());
    	model.put("projectPath", projectPath);
  }
	/**
	 * 生成到文件文件内容并返回
	 * @param tpl
	 * @param model
	 * @param isReplaceFile
	 * @return
	 * @throws TemplateException 
	 * @throws IOException 
	 */
	public static String generateFileContent(GenTemplate tpl, Map<String, Object> model) throws Exception{
		return FreeMarkers.renderString(StringUtils.trimToEmpty(tpl.getContent()), model);
	}
	
	/**
     * 生成到文件
     * @param tpl
     * @param model
     * @param isReplaceFile
     * @return
	 * @throws TemplateException 
	 * @throws IOException 
     */
    public static String generateToFileAndGetFileName(GenTemplate tpl, Map<String, Object> model, boolean isReplaceFile) throws Exception{
    	 String fileName ="";
    	try{
    	// 获取生成文件
         fileName = model.get("projectPath")+ File.separator 
                + StringUtils.replaceEach(FreeMarkers.renderString(tpl.getFilePath() + "/", model),
                        new String[]{"//", "/", "."}, new String[]{File.separator, File.separator, File.separator})
                + FreeMarkers.renderString(tpl.getFileName(), model);
        logger.debug(" fileName === " + fileName);
        File existFile=new File(fileName);
      
    	 if(existFile.exists()){
        	  String oldContent=FileUtils.readFileToString(existFile);
              if(StringUtils.isNotEmpty(oldContent)&&oldContent.contains("ExtEntity")){
            	  model.put("extendEntity", "ExtEntity");
      		}
        	if(tpl.getName().equals("dao")||tpl.getName().equals("extDao")||tpl.getName().equals("mapper")||tpl.getName().equals("entity")||tpl.getName().equals("extMapper")){
        		if(tpl.getName().equals("extMapper")){//如果是扩展的mapper存在，则不覆盖也不加copy，什么都不做
        			return fileName;
        		}
        	}else{
        		fileName=addCopyForFileName(fileName);
            	model.put("isCopy", "1");//表示是copy文件，不覆盖原来的
            	File copyFileName=new File(fileName);
            	if(copyFileName.exists()){
            		copyFileName.delete();
            	}
        	}
        	
        }
        // 获取生成文件内容,FreeMarkers生成实际代码信息
        String content = FreeMarkers.renderString(StringUtils.trimToEmpty(tpl.getContent()), model);
        logger.debug(" content === \r\n" + content);
   
        // 如果选择替换文件，则删除原文件
//        if (isReplaceFile){
//            FileUtils.deleteFile(fileName);
//        }
        
        // 创建并写入文件
        if (FileUtils.createFile(fileName)){
            FileUtils.writeToFile(fileName, content, true);
            logger.debug(" file create === " + fileName);
//            return "生成成功："+fileName+"<br/>";
        }else{
            logger.debug(" file extents === " + fileName);
//            return "文件已存在："+fileName+"<br/>";
        }
       }catch(Exception e){
    	   throw e;
       }
        return fileName;
    }
	private static String addCopyForFileName(String fileName){
		String start=fileName.substring(0, fileName.lastIndexOf("."));
		String middle="_copy";
		String end=fileName.substring( fileName.lastIndexOf("."));
		return start+middle+end;
	}
	/********************************以下为解析代码表到配置的部分*************************************/
	private static String printJavaCode(String dbtableName,String fieldName,String datas){
		
		String[] keyvalue=parseData(datas);
	
    	MyStringBuffer sb=MyStringBuffer.newInstance();
    	String entityName=dbtableName.replace("tb_", "");
    	entityName=StringUtils.toCamelCase(entityName);
    	if(fieldName.contains("_"))
    	fieldName=StringUtils.toCamelCase(fieldName);
    	String beanName=entityName+StringUtils.firstToUpper(fieldName);
    	sb.appendH("FieldValueConvertInfoBean ",beanName,"=new FieldValueConvertInfoBean(",addQuote(entityName),",",addQuote(fieldName));
    	
    	sb.appendH(beanName,".putMapKeys(",keyvalue[0],").putMapValues(",keyvalue[1]);
    	sb.appendH("alljtbSourceList.add(",beanName);
    	return sb.toString();
    }
    /**
     * 解析这种数据为 1：未发布 2 已发布===》"0","1"   "稍后发布","立即发布"
     * @param datas
     * @return
     */
    private static String[] parseData(String datas) {
    	String[] result=new String[2];
    	List<Integer> numList=findNumData(datas);
    	if(numList.size()<1)return result;
    	MyStringBuffer keysb=MyStringBuffer.newInstance();
    	MyStringBuffer valuesb=MyStringBuffer.newInstance();
    	for(Integer it:numList) {
    		if(StringUtils.isEmpty(keysb.toString())) {
    			keysb.append(addQuote(it+""));
    		}else {
    			keysb.append(","+addQuote(it+""));
    		}
    		datas=datas.replace(it+"", "");
    	}
    	result[0]=keysb.toString();
    	datas=datas.replace(":", " ");
    	datas=datas.replace("\\", " ");
    	datas=datas.replace("：", " ");
    	datas=datas.replace("、", " ");
    	String[] newdatas=datas.split(" ");
    	for(String newdata:newdatas) {
    		if(StringUtils.isNotEmpty(StringUtils.trimAllWhitespace(newdata))) {
    			if(StringUtils.isEmpty(valuesb.toString())) {
    				valuesb.append(addQuote(newdata));
        		}else {
        			valuesb.append(","+addQuote(newdata));
        		}
    		}
    	}
    	result[1]=valuesb.toString();
    	return result;
    }
    
    public static String addQuote(String var){
		return "\""+var+"\"";
	}
	public static String addSingleQuote(String var){
		return "'"+var+"'";
	}
	public static List<Integer> findNumData(String str){
		List<Integer> datalist=Lists.newArrayList();
		String result=null;
		String regEx = "[^0-9]";//匹配指定范围内的数字
		 Pattern p = Pattern.compile(regEx);
		 Matcher m = p.matcher(str);
		 if (m.find()) {
			 result = m.replaceAll(""); 
		  }
		 if(StringUtils.isNotEmpty(result)&&StringUtils.isNotEmpty(result.trim())) {
			 String temp=result.trim();
			 for(int i=0;i<temp.length();i++) {
				 datalist.add(new Integer(temp.substring(i, i+1)));
			 }
		 }
			 
        return datalist;
	}
	
}
