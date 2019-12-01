package com.tengjie.common.utils;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * FreeMarkers工具类
 * @author
 * @version 2013-01-15
 */
public class FreeMarkers {
   /**
    * 
    * @param templateString:模板文件中的内容
    * @param model：赋值的模型
    * @param includePath：对于模板中如果有include，那么这个是指定include的模板文件从classpath下的哪个路径开始
    *                    可变参数，实际只要一个，可以为空，为空的话，默认使用从class路径下的templates开始，即<#include "/apicloud/html/function/commonFunction.ftl">
    *                    apicloud是跟在templates后面的第一个目录。
    * @return
    * @throws Exception
    */
	public static String renderString(String templateString, Map<String, ?> model,String ...includePath) throws Exception {
		try{
			StringWriter result = new StringWriter(); 
			Configuration config=new Configuration();
			if(includePath.length<1) {
				config.setDirectoryForTemplateLoading(FileUtils.findClassPathFile("templates"));
			}else {
				config.setDirectoryForTemplateLoading(FileUtils.findClassPathFile(includePath[0]));
			}
			String freemarkerFileName=model.get("freemarkerFileName")==null?"name":model.get("freemarkerFileName").toString();//freemarker的配置中的name名字
		    Template t = new Template(freemarkerFileName, new StringReader(templateString), config);
			t.process(model, result);
			return result.toString();
		} catch (Exception e) {
			throw new FreeMarkerException("根据数据向freemark赋值时出错："+e.getMessage());
		}
	}

	public static String renderTemplate(Template template, Object model) {
		try {
			StringWriter result = new StringWriter();
			template.process(model, result);
			return result.toString();
		} catch (Exception e) {
			throw Exceptions.unchecked(e);
		}
	}

	public static Configuration buildConfiguration(String directory) throws IOException {
		Configuration cfg = new Configuration();
		Resource path = new DefaultResourceLoader().getResource(directory);
		cfg.setDirectoryForTemplateLoading(path.getFile());
		return cfg;
	}
	
	public static void main(String[] args) throws IOException {
//		// renderString
//		Map<String, String> model = com.google.common.collect.Maps.newHashMap();
//		model.put("userName", "calvin");
//		String result = FreeMarkers.renderString("hello ${userName}", model);
//		System.out.println(result);
//		// renderTemplate
//		Configuration cfg = FreeMarkers.buildConfiguration("classpath:/");
//		Template template = cfg.getTemplate("testTemplate.ftl");
//		String result2 = FreeMarkers.renderTemplate(template, model);
//		System.out.println(result2);
		
//		Map<String, String> model = com.google.common.collect.Maps.newHashMap();
//		model.put("userName", "calvin");
//		String result = FreeMarkers.renderString("hello ${userName} ${r'${userName}'}", model);
//		System.out.println(result);
	}
	
}
