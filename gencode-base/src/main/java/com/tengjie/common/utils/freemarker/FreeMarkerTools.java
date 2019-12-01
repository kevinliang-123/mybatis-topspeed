package com.tengjie.common.utils.freemarker;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.hibernate.validator.constraints.Length;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;

import com.tengjie.common.config.Global;
import com.tengjie.common.utils.FileUtils;
import com.tengjie.common.utils.FreeMarkers;
import com.tengjie.common.utils.ListUtils;
import com.tengjie.common.utils.MapUtils;
import com.tengjie.common.utils.StringUtils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class FreeMarkerTools {
	/**
	 * 直接到资源文件目录resource下根据传入的model获取结果内容
	 * @param model：freemarker的xml文件中所需变量
	 * @param templateFileName：文件名
	 * @param templatePath：相对于资源文件目录下的子目录
	 * 例子：String content=JspElement.findJsFragFromFreemarker(model, "methodRichTextTemplate.xml", "/templates/methodrichtext/");
	 * @return
	 */
	 public static String findContentFromFreemarker(Map<String,Object> model,String templateFileName,String templatePath) {
	    	String result="";
	    	GenTemplate tpl=FreeMarkerTools.fileToObject(templateFileName, GenTemplate.class, templatePath);
	    	try {
				result=FreeMarkers.renderString(StringUtils.trimToEmpty(tpl.getContent()), model);
			} catch (Exception e) {
				e.printStackTrace();
			}
	    	return result;
	    }
	/**
	 * XML文件转换为对象,注意：本方法是将模板中的内容放到GenTemplate中，包括name、filepath、content等内容，
	 * 但是需要注意的是，这里只是把freemaker内容原封载入，并没有使用model进行赋值，赋值是在下一步动作
	 * filepath实际是要存储文件的地址，这样做实际也就是动态化按照目录存储。
	 * @param fileName：模板文件名(也可带路径)，实际全相对路径为：templatePath+fileName
	 * @param clazz：将模板中的内容按照属性名，存储到这个clazz中
	 * @param templatePath：模板文件路径，不能为空，为空会使用/templates/pchtml/
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T fileToObject(String fileName, Class<?> clazz,String templatePath){
		try {
			if(StringUtils.isEmpty(templatePath))templatePath="/templates/pchtml/";
			String pathName = templatePath + fileName;
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
			e.printStackTrace();
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
	 * 生成到文件,但是生成路径直接使用配置在xml模板中的全路径，不会在前面加上 Global.getProjectPath() + File.separator 
	 * @param tpl
	 * @param model
	 * @param isReplaceFile
	 * @return
	 * @throws TemplateException 
	 * @throws IOException 
	 */
	public static String generateToFileAbsPath(GenTemplate tpl, Map<String, Object> model, boolean isReplaceFile) throws Exception{
		model.put("freemarkerFileName", tpl.getName());//freemarker的配置中的name名字
		// 获取生成文件
				String fileName = StringUtils.replaceEach(FreeMarkers.renderString(tpl.getFilePath() + "/", model),
								new String[]{"//", "/", "."}, new String[]{File.separator, File.separator, File.separator})
						+ FreeMarkers.renderString(tpl.getFileName(), model);
				return genToFile(fileName,tpl,model,isReplaceFile);
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
	public static String generateToFile(GenTemplate tpl, Map<String, Object> model, boolean isReplaceFile) throws Exception{
		model.put("freemarkerFileName", tpl.getName());//freemarker的配置中的name名字
		// 获取生成文件
		String fileName = Global.getProjectPath() + File.separator 
				+ StringUtils.replaceEach(FreeMarkers.renderString(tpl.getFilePath() + "/", model),
						new String[]{"//", "/", "."}, new String[]{File.separator, File.separator, File.separator})
				+ FreeMarkers.renderString(tpl.getFileName(), model);
		
		return genToFile(fileName,tpl,model,isReplaceFile);
	}
	/**
	 * generateToFileAbsPath\generateToFile只是构建路径，真正生成文件实现在这里
	 * @param fileName
	 * @param tpl
	 * @param model
	 * @param isReplaceFile
	 * @param ifFormatHtml：对于html是否格式化，如果要生成的文件不是html，或者是html但是不需要格式化，请不要传入true
	 * 
	 * @return
	 * @throws Exception
	 */
	private static String genToFile(String fileName,GenTemplate tpl, Map<String, Object> model, boolean isReplaceFile)throws Exception {
		model.put("freemarkerFileName", tpl.getName());//freemarker的配置中的name名字
		// 获取生成文件内容,FreeMarkers生成实际代码信息
		String content = FreeMarkers.renderString(StringUtils.trimToEmpty(tpl.getContent()), model);
		
		// 如果选择替换文件，则删除原文件
		if (isReplaceFile){
			FileUtils.deleteFile(fileName);
		}
		
		// 创建并写入文件
		if (FileUtils.createFile(fileName)){
			FileUtils.writeToFile(fileName, content, true);
			
			return "生成成功："+fileName+"<br/>";
		}else{
	
			return "文件已存在："+fileName+"<br/>";
		}
	}

	
}
