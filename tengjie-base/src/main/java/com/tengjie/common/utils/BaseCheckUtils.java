package com.tengjie.common.utils;


import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Maps;
import com.tengjie.common.persistence.TjBaseEntity;
import com.tengjie.common.utils.DateUtils;
import com.tengjie.common.utils.Reflections;
import com.tengjie.common.utils.StringUtils;

/**
 * 接口公共验证类
 * Created by Administrator on 2017/7/10 0010.
 */
public class BaseCheckUtils {
	
	/**
	 * 验证类型，包括手机号、日期等
	 */
	public static final String VALIDATE_EMAIL= "0";
	public static final String VALIDATE_MOBILE_PHONE = "1";
	public static final String VALIDATE_DATA_INTEGER = "2";//必须是整型数字
	public static final String VALIDATE_DATANumber = "3";//必须是数字，无论是整型还是double等
    /**
     * 分装获取到的参数，为空返回null，不为空返回实际值
     * @param param
     * @return
     */
    public static String checkParamValue(String param){
        if(!StringUtils.isNoneBlank(param) ){
            return null;
        }else{
            return param;
        }
    }

    /**
     * 判断是否必输项。
     * 是必输项返回true，不是必输项返回false
     * @param param
     * @return
     */
    public static boolean paramIsNull(Object param,boolean isType){
        if(param==null && isType){
           throw  new RuntimeException("参数不能为空");
        }else{
            return false;
        }
    }

    /**
     * 判断Object参数param是否为null，如果为null，返回瓶装后的returnStr，不为null，返回null。
     * @param param
     * @param isType
     * @param name
     * @return
     */
    public static String paramIsNull(Object param,boolean isType,String name){
        String returnStr="【"+name+"】参数不能为空";
        if(param==null && isType){
            return returnStr;
        }else{
            return null;
        }
    }
    /**
     * 判断整个参数bean中的参数如手机号、email等。
     * @param paramBean
     * @param propMap《属性名,验证类型如VALIDATE_EMAIL》
     * @return
     */
    public static String paramBeanValidateDestType(Object paramBean,Map<String,String> propMap){
    	if(paramBean==null||propMap==null||propMap.size()<1){
    		return null;
    	}
    	for (Map.Entry<String,String> entry : propMap.entrySet()) {  
			  String temp= null;
  		  if(StringUtils.isNotEmpty(entry.getKey())){
  			  Object objValue=Reflections.invokeGetter(paramBean,entry.getKey());
  			  String strValue=objValue+"";
  			  String validateType=entry.getValue();
  			  switch (validateType) {
			case VALIDATE_EMAIL:
				if(!checkEmail(strValue)){
           		 temp="邮箱格式不正确";
           	 }
				break;
             case VALIDATE_MOBILE_PHONE:
            	 if(!checkMobileNumber(strValue)){
            		 temp="手机号格式不正确";
            	 }
				break;
             case VALIDATE_DATA_INTEGER:
            	 if(!StringUtils.isNumeric(strValue)){
            		 temp=entry.getKey()+"必须是整型数字";
            	 }
				break;
             case VALIDATE_DATANumber:
            	 if(!StringUtils.isNumData(strValue)){
            		 temp=entry.getKey()+"必须是数值类型";
            	 }
				break;
			default:
				break;
			}
  		  }
  		if(StringUtils.isNotEmpty(temp))return temp;
  	 }
  	     
  	return null;
    }
    /**
     * 判断整个参数bean中的参数是否为空。
     * @param paramMap 验证map
     * @param propMap 属性map<属性英文名,中文名>
     * @param name
     * @return
     */
    public static String paramMapIsNull(Map<String,Object> paramMap,Map<String,String> propMap){
    	if(paramMap==null||propMap==null||propMap.size()<1){
    		return null;
    	}
    	for (Map.Entry<String,String> entry : propMap.entrySet()) {  
			  String temp= null;
    		  if(StringUtils.isNotEmpty(entry.getKey())){
    			  Object objValue=paramMap.get(entry.getKey());
    			  if(objValue instanceof String){
    				  temp= paramStrIsNull(objValue+"",true,entry.getValue());
    			  }else{
     				  temp= paramIsNull(objValue,true,entry.getValue());
     			  }
    		  }
    		if(StringUtils.isNotEmpty(temp))return temp;
    	 }
    	     
    	return null;
    }
   
    /**
     * 判断整个参数bean中的参数是否为空。
     * @param paramBean 验证bean
     * @param propMap 属性map<属性英文名,中文名>
     * @param name
     * @return
     */
    public static String paramBeanIsNull(Object paramBean,Map<String,String> propMap){
    	if(paramBean==null||propMap==null||propMap.size()<1){
    		return null;
    	}
    	for (Map.Entry<String,String> entry : propMap.entrySet()) {  
			  String temp= null;
    		  if(StringUtils.isNotEmpty(entry.getKey())){
    			  if(paramBean instanceof TjBaseEntity) {
    				 if(!((TjBaseEntity)paramBean).hasProperty(entry.getKey())){
    					  return entry.getKey()+"属性没有传入！";
    				  }
    			  }
    			  Object objValue=Reflections.invokeGetter(paramBean,entry.getKey());
    			  if(objValue instanceof String){
    				  temp= paramStrIsNull(objValue+"",true,entry.getValue());
    			  }else{
     				  temp= paramIsNull(objValue,true,entry.getValue());
     			  }
    		  }
    		if(StringUtils.isNotEmpty(temp))return temp;
    	 }
    	     
    	return null;
    }
    
    
    /**
     * 判断字符串参数param是否为null，如果为null，返回拼装后的returnStr，不为null，返回null。
     * @param param
     * @param isType
     * @param name
     * @return
     */
    public static String paramStrIsNull(String param,boolean isType,String name){
        String returnStr="【"+name+"】参数不能为空";
        if(StringUtils.isBlank(param) && isType){
            return returnStr;
        }else{
            return null;
        }
    }

    /**
     * 判断字符串参数param是否为null，如果为null，返回name，不为null，返回null。
     * @param param
     * @param isType
     * @param name
     * @return
     */
    public static String paramStrIsNullAll(String param,boolean isType,String name){
        if(StringUtils.isBlank(param) && isType){
            return name;
        }else{
            return null;
        }
    }

    /**
     * 判断Object参数param是否为null，如果为null，返回name，不为null，返回null。
     * @param param
     * @param isType
     * @param name
     * @return
     */
    public static String paramIsNullAll(Object param,boolean isType,String name){
        if(param==null && isType){
            return name;
        }else{
            return null;
        }
    }

    /**
     *  判断字符串参数param是否为合法的日期格式，如果为合法的，返回null，不是合法的，返回相应提示信息。
     * @param param
     * @param isType
     * @param name
     * @return
     */
    public static String paramDateIsNull(String param,boolean isType,String name){
        String returnStr="【"+name+"】参数日期格式错误";
        if(DateUtils.parseDate(param)==null && isType){
            return returnStr;
        }else{
            return null;
        }
    }
    //生成numb位随机数字
//    public static final char[] CHARS={'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z'};
    public static final char[] CHARS={'0','1','2','3','4','5','6','7','8','9'};
    public static Random random=new Random();
    public static String getRandomString(int numb){
        StringBuffer buffer=new StringBuffer();
        for(int i=0;i<numb;i++){
            buffer.append(CHARS[random.nextInt(CHARS.length)]);
        }
        return buffer.toString();
    }
    /**
     * javaBean 转 Map
     * @param object 需要转换的javabean
     * @return  转换结果map
     * @throws Exception
     */
    public static Map<String, Object> beanToMap(Object object) throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        Class cls = object.getClass();
        Field[] fields = cls.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            map.put(field.getName(), field.get(object));
        }
        return map;
    }
    /**
     * 验证邮箱
    * @param email
     * @return
     */
    public static boolean checkEmail(String email){
     boolean flag = false;
     try{
       String check = "^([a-z0-9A-Z]+[-|_|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";
       Pattern regex = Pattern.compile(check);
       Matcher matcher = regex.matcher(email);
       flag = matcher.matches();
      }catch(Exception e){
       flag = false;
      }
     return flag;
    }
    /**
     * 验证手机号码
    * @param mobiles
     * @return
     */
    public static boolean checkMobileNumber(String mobileNumber){
     boolean flag = false;
     try{
       Pattern regex = Pattern.compile("^1[3|4|5|6|7|8|9][0-9]\\d{4,8}$");
       Matcher matcher = regex.matcher(mobileNumber);
       flag = matcher.matches();
      }catch(Exception e){
       flag = false;
      }
     return flag;
    }
}
