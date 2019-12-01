package com.tengjie.common.gencode.tools;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tengjie.common.gencode.vo.TableMetadata;
import com.tengjie.common.utils.DateUtils;
import com.tengjie.common.utils.MyStringBuffer;
import com.tengjie.common.utils.Reflections;
import com.tengjie.common.utils.StringUtils;

public abstract class InterfaceAndBackstageFactory {

	
	
	private String projectPath;
	private String packagePath;
	protected String HUANHANG="\n";
	protected String QUOTATION="\"";
	protected String BLANK1=" ";
	protected String BLANK2="  ";
	protected String BLANK3="   ";
	protected String BLANK4="    ";
	protected List<String> importList=new ArrayList<String>();
	protected List<String> autoWireServiceList=Lists.newArrayList();
	protected List<String> callBackMethodList=Lists.newArrayList();
	protected List<String> jspListJsList=Lists.newArrayList();//list jsp页面的js函数列表
	protected List<String> jspFormJsList=Lists.newArrayList();//form jsp页面的js函数列表
	protected List<String> jspFormDocumentReadyJsList=Lists.newArrayList();//form jsp页面的documentReady部分的js函数列表
	protected Map<String,String> jspFormTrIdMap=Maps.newHashMap();//form jsp页面的tr idmap，有些依赖显示，需要tr有id
	
	protected List<String> frontEditPageJsList=Lists.newArrayList();//前端配置页面JS列表
    protected final String COMMA=",";//逗号
	
	   /**
	    * 获得变量定义
	    * @param tm
	    * @return
	    */
	   protected String  findVarDefine_code(TableMetadata tm,String _varName) {
		   String varName=StringUtils.isEmpty(_varName)?"param":_varName;
		 
		  String s= tm.getTableNameFirstUp(true)+" "+varName+"= new "+tm.getTableNameFirstUp(true)+"();\n";
		  return s;
	}
	 
	   
	   /**
	    * 生成方法注解
	    * @param sb
	    * @param methodName
	    * @param methodDesc
	    */
	   protected void  genMethodAnnotate(MyStringBuffer sb,String methodDesc) {
		   sb.append("/**",HUANHANG);
		   sb.append("*@Description：",methodDesc,HUANHANG);
		   sb.append("*@author:自动生成",HUANHANG);
		   sb.append("*@since:生成时间",DateUtils.formatDate(new Date(), "yyyy年MM月dd日 HH:mm:ss"),HUANHANG);
		   sb.append("*/",HUANHANG);
	   }
	   /**
	    * 生成Controller方法的依赖注入信息
	    * @param sb
	    * @param methodName
	    * @param methodDesc
	    */
	   protected void  genMethodControllerIOC(MyStringBuffer sb,String methodName) {
		   sb.append("@RequestMapping(value = ",QUOTATION,methodName,QUOTATION,", "," method = { RequestMethod.POST })",HUANHANG);
		   sb.append("@ResponseBody",HUANHANG);
		   sb.append("*/",HUANHANG);
	   }
	   /**
	    * 根据java类型，返回需要的值的包装类型
	    * @param typeCode
	    * @param receiveJavaType 接收的javatype类型，如果相同，实际不需要转换
	    * @return
	    */
	   protected String findValuePackByJavaType( String javaType,String value,boolean ifaddQuote,String receiveJavaType){
		   String returnType="";
		   if(ifaddQuote){
			   returnType=addQuote(value);
		   }else{
			   returnType=value;
		   }
		   if(javaType.equals(receiveJavaType)){
			   return returnType;
		   }
		   if("java.util.Date".equals(javaType)){
			   returnType=" DateUtils.parseDate("+returnType+")";
		   }
		   if("java.math.BigDecimal".equals(javaType)){
			   importList.add("import java.math.BigDecimal;"+HUANHANG);
			   returnType="new BigDecimal("+returnType+")";
		   }
		   if("Long".equals(javaType)){
			   returnType="new Long("+returnType+")";
		   }
		   if("Integer".equals(javaType)){
			   returnType="new Integer("+returnType+")";
		   }
		   if("int".equals(javaType)||"short".equals(javaType)||"long".equals(javaType)||"double".equals(javaType)||"char".equals(javaType)){
			   returnType=value;
		   }
		   if("String".equals(javaType)||"java.lang.String".equals(javaType)){
			   returnType=returnType+".toString()";
		   }
		   return returnType;
	   }
	   
		
		protected void addAutoWireServiceList(String beanClassName){
			MyStringBuffer msb=new MyStringBuffer();
			msb.appendK("@Autowired");
			msb.append("protected ",beanClassName,"Service ",StringUtils.firstToLower(beanClassName),"Service;");
			if(!autoWireServiceList.contains(msb.toString())){
				autoWireServiceList.add(msb.toString());
			}
		}
		/**
		 * 导入的类型
		 * @param className:bean名称即可
		 * @param type:1、bean、2：service 3：controller 4：其他,即原样导入
		 */
		protected void addImportList(String className,String type){
			String path=this.getPackagePath()+"."+StringUtils.firstToLower(className)+".";//.toLowerCase()
		
			switch (type) {
			case "1":
				path=path+"entity."+className;
				break;
            case "2":
            	path=path+"service."+className+"Service";
            	addAutoWireServiceList(className);
				break;
            case "3":
            	path=path+"web."+className+"Controller";
				break;
            case "4":
            	path=className;
				break;
			default:
				break;
			}
			MyStringBuffer sb=new MyStringBuffer();
			sb.append("import ",path,";");
			if(!importList.contains(sb.toString())){
				importList.add(sb.toString());
			}
		}
	   /**	
	    * 获得方法定义
	    * @param tm
	    * @return
	    */
	   protected String  findControllerMethodDefine_code(TableMetadata tm,String _functionName,String ...params) {
		   StringBuffer sb=new StringBuffer();
		   sb.append("public "+tm.getTableNameFirstUp(true)+" "+_functionName+"("+tm.getTableNameFirstUp(true)+" "+tm.getTableNameFirstUp(false));
		  if(params!=null&&params.length>0){
			  for(String pp:params){
				  sb.append(","+pp);
			  }
		  }
		  sb.append(")");
		  return sb.toString();
	}
	   
	   /**	
	    * 获得方法定义
	    * @param tm
	    * @return
	    */
	   protected String  findBaseMethodDefine_code(String publicOrPrivate,String staticStr,String returnType,String _functionName,String desc,List<String> throwsList, String ...params) {
		   MyStringBuffer sb=new MyStringBuffer();
		 sb.append(publicOrPrivate,BLANK1,staticStr,BLANK1,returnType,BLANK1,_functionName,"(");
		  if(params!=null&&params.length>0){
			  int i=0;
			  for(String pp:params){
				  sb.append(i>0?","+pp:""+pp);
				  i++;
			  }
		  }
		  sb.append(")");
		  if(throwsList!=null)
		  for(String throwC:throwsList){
			  sb.append(throwC);
		  }
		  sb.appendK("{");
		  return sb.toString();
	}
	   /**
	    * 通过反射获得一个对象中某属性的值
	    * @param obj
	    * @param fieldName
	    * @return
	    */
	   private String findValue(Object obj,String fieldName){
		   Object value=Reflections.invokeGetter(obj, fieldName);
		   return value==null?null:value.toString();
	   }
	   /**
	    * 通过反射获得一个对象中某属性的值
	    * @param obj
	    * @param fieldName
	    * @return
	    */
	   private Object findObjectValue(Object obj,String fieldName){
		   return Reflections.invokeGetter(obj, fieldName);
	   }
	  
	    
	    protected void getElse(String fieldName,String value,MyStringBuffer sb){
	    	sb.appendK("}else{");
	    	sb.appendK("   $(",addQuote("#"+fieldName+"Tr"),")",getHideOrShowReverse(value));
	    	sb.appendK("}");
	    }
	    protected String getHideOrShowReverse(String value){
	    	String result="";
	    	if("0".equals(value)){
	    		result=".hide()";
	    	}
	    	if("1".equals(value)){
	    		result=".show()";
	    	}
	    	return result;
	    }
	    protected String getHideOrShow(String value){
	    	String result=".hide()";
	    	if("0".equals(value)){
	    		result=".show()";
	    	}
	    	return result;
	    }
	    protected String getAndOr(String value){
	    	String result="&&";
	    	if("1".equals(value)){
	    		result="||";
	    	}
	    	return result;
	    }
	    protected String getOperCond(String value){
	    	String result="==";
	    	switch (value) {

	        case "2":
	        	result=">";
				break;
	        case "3":
	        	result="<";
	        case "4":
	        	result=">=";
				break;
	        case "5":
	        	result="<=";
				break;
			default:
				break;
			}
	    	return result;
	    }
	    public String addQuote(String var){
			return "\""+var+"\"";
		}
		public String addSingleQuote(String var){
			return "'"+var+"'";
		}
		public String getProjectPath() {
			return projectPath;
		}
		public void setProjectPath(String projectPath) {
			this.projectPath = projectPath;
		}
		public String getPackagePath() {
			return packagePath;
		}
		public void setPackagePath(String packagePath) {
			this.packagePath = packagePath; 
		}
		public List<String> getImportList() {
			return importList;
		}
		public void setImportList(List<String> importList) {
			this.importList = importList;
		}
		public List<String> getAutoWireServiceList() {
			return autoWireServiceList;
		}
		public void setAutoWireServiceList(List<String> autoWireServiceList) {
			this.autoWireServiceList = autoWireServiceList;
		}
		public List<String> getCallBackMethodList() {
			return callBackMethodList;
		}
		public void setCallBackMethodList(List<String> callBackMethodList) {
			this.callBackMethodList = callBackMethodList;
		}
		public List<String> getJspListJsList() {
			return jspListJsList;
		}
		public void setJspListJsList(List<String> jspListJsList) {
			this.jspListJsList = jspListJsList;
		}
		public List<String> getJspFormJsList() {
			return jspFormJsList;
		}
		public void setJspFormJsList(List<String> jspFormJsList) {
			this.jspFormJsList = jspFormJsList;
		}
		public List<String> getJspFormDocumentReadyJsList() {
			return jspFormDocumentReadyJsList;
		}
		public void setJspFormDocumentReadyJsList(
				List<String> jspFormDocumentReadyJsList) {
			this.jspFormDocumentReadyJsList = jspFormDocumentReadyJsList;
		}
		public Map<String, String> getJspFormTrIdMap() {
			return jspFormTrIdMap;
		}
		public void setJspFormTrIdMap(Map<String, String> jspFormTrIdMap) {
			this.jspFormTrIdMap = jspFormTrIdMap;
		}
		
		 public List<String> getFrontEditPageJsList() {
			return frontEditPageJsList;
		}
		public void setFrontEditPageJsList(List<String> frontEditPageJsList) {
			this.frontEditPageJsList = frontEditPageJsList;
		}
		
}
