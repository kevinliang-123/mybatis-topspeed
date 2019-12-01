package com.tengjie.common.utils;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.LocaleResolver;

import com.google.common.collect.Lists;

/**
 * 字符串工具类, 继承org.apache.commons.lang3.StringUtils类
 * @author
 * @version 2013-05-22
 */
public class StringUtils extends org.apache.commons.lang3.StringUtils {
	
    private static final char SEPARATOR = '_';
    private static final String CHARSET_NAME = "UTF-8";
    
    /**
     * 转换为字节数组
     * @param str
     * @return
     */
    public static byte[] getBytes(String str){
    	if (str != null){
    		try {
				return str.getBytes(CHARSET_NAME);
			} catch (UnsupportedEncodingException e) {
				return null;
			}
    	}else{
    		return null;
    	}
    }
    
    /**
     * 转换为字节数组
     * @param str
     * @return
     */
    public static String toString(byte[] bytes){
    	try {
			return new String(bytes, CHARSET_NAME);
		} catch (UnsupportedEncodingException e) {
			return EMPTY;
		}
    }
    
    /**
     * 判断某个bean中的某些字段是否为空，注意基本类型 int double\bean等无效
     * 返回list，直接拼接了为空的字段的list
     */
    public static List<String> judgeFieldIfNull(Object bean,List<String> fieldNames){
    	List<String> list=new ArrayList();
    	for(String fieldName:fieldNames){
    		Object value=Reflections.invokeGetter(bean, fieldName);
    		if(judgeObjClassTypeAndValidateNull(Reflections.getFieldValue(bean, fieldName).getClass(),value)){
    			list.add(fieldName);
    		}
    	}
    	return list;
    }
    /**
     * 判断字段类型，并且判断是否为空
     * 返回true则表示为空
     * @param valType
     * @param bl
     * @return
     */
    private static boolean judgeObjClassTypeAndValidateNull(Class<?> valType,Object value){
    	boolean result=false;
    	if (valType == String.class){
			if(StringUtils.isEmpty(value+""))result=true;
		}else if (valType == Integer.class){
			if(value==null)result=true;
		}else if (valType == Long.class){
			if(value==null)result=true;
		}else if (valType == Double.class){
			if(value==null)result=true;
		}else if (valType == Float.class){
			if(value==null)result=true;
		}else if (valType == Date.class){
			if(value==null)result=true;
		}else if (valType == BigDecimal.class){
			if(value==null)result=true;
		}else{
			if(value==null)result=true;
		}
    	return result;
    }
    /**
     * 判断字符串中是否包含中文
     * @param str
     * 待校验字符串
     * @return 是否为中文
     * @warn 不能校验是否为中文标点符号 
     */
    public static boolean isContainChinese(String str) {
     Pattern p = Pattern.compile("[\u4e00-\u9fa5]");
     Matcher m = p.matcher(str);
     if (m.find()) {
      return true;
     }
     return false;
    }

    /**
     * 判断某个bean中的某些字段是否为空，注意基本类型 int double等无效
     * 返回字符串，直接拼接好要返回的中文提示,fieldNamesMap 的keyvalue分别为字段名称，字段中文名
     */
    public static String judgeFieldIfNull(Object bean,Map<String,String> fieldNotNullMap){
    	StringBuilder sb=new StringBuilder();
    	for (String getKey : fieldNotNullMap.keySet()) {
    		Object value=Reflections.invokeGetter(bean, getKey);
    		if(Reflections.getFieldValue(bean, getKey)==null||judgeObjClassTypeAndValidateNull(Reflections.getFieldValue(bean, getKey).getClass(),value)){
    			sb.append("["+fieldNotNullMap.get(getKey)+"]内容不能为空！\n");
    		}
		}
    	return sb.toString();
    }
    /**
     * 将数据库的表名，转换成驼峰后的java实体名字，字段实际不需要，
     * 字段直接调用驼峰方法即可，表名一般有tb开头的
     * @param str,数据库的表名，注意是带有下划线的那种数据库表名
     * @param ifFirstUp 是否首字母大写，true为大写，false为小写
     * @return
     */
    public static String convertDbTableNameToJavaName(String str,boolean ifFirstUp){
    	if(isEmpty(str))return str;
    	str=str.toLowerCase().replace("tb", "");
    	str=StringUtils.toCamelCase(str);
    	if(ifFirstUp){str=firstToUpper(str);}else{str=firstToLower(str);};
    	return str;
    }
    /**
     * 获得一个类的SimpleName，正常是用getSimpleName即可，但是实际不行，对于baseEntity类型，后面会包含$$
     * @param clazz
     * @return
     */
    public static String findClassSimpleName(Class clazz,boolean ...iffirstup) {

    	String simpleName=clazz.getSimpleName();
    	//如果是带有动态属性的bean，名字会不一样，是这样的teacherInfo$$BeanGeneratorByCGLIB$$33b233a6，这样无法覆盖原来的
    	if(simpleName.contains("$$"))simpleName=simpleName.substring(0, simpleName.indexOf("$$"));
    	boolean firstup=iffirstup.length>0?iffirstup[0]:false;
    	if(!firstup){
    		simpleName=StringUtils.firstToLower(simpleName);
    	}
    	return simpleName;
    }
   /**
    * 比较两个字符串是否相等，会对字符串是否为空等等多种条件进行校验
    * @param str1
    * @param str2
    * @return
    */
    public static boolean compareTwoStrEqual(String str1,String str2) {
    	if(str1==null&&str2==null)return true;//都为空也算相同
    	if(str1==null&&str2!=null)return false;
    	if(str1!=null&&str2==null)return false;
    	return str1.equals(str2);
    }
    
    /**
     * 是否包含字符串
     * @param str 验证字符串
     * @param strs 字符串组
     * @return 包含返回true
     */
    public static boolean inString(String str, String... strs){
    	if (str != null){
        	for (String s : strs){
        		if (str.equals(trim(s))){
        			return true;
        		}
        	}
    	}
    	return false;
    }
    /**
     * 查找一个字符串中的数字并返回
     * @param str
     * @return 返回-999则代表没有找到数字
     */
	public static Integer findNumData(String str){
		String regEx = "[^0-9]";//匹配指定范围内的数字
		 Pattern p = Pattern.compile(regEx);
		 Matcher m = p.matcher(str);
		 if (m.find()) {
			 String result = m.replaceAll("");
			 if(isEmpty(result)){
				 return new Integer(-999);
			 }else{
				 return new Integer( m.replaceAll("").trim());
			 }
		  }
        return new Integer(-999);
	}
    /**
     * 判断一个字符串必须是数值类型，可以是带小数点
     * @param str
     * @return 返回-999则代表没有找到数字
     */
	public static boolean isNumData(String str){
		  if (isEmpty(str)) {
	            return false;
	        }
		String regEx = "-[0-9]+(.[0-9]+)?|[0-9]+(.[0-9]+)?";//匹配指定范围内的数字
		 Pattern p = Pattern.compile(regEx);
		 Matcher m = p.matcher(str);
		 if (m.matches()) {
		  return true;
		  }
        return false;
	}

	/**
	 * 替换掉HTML标签方法
	 */
	public static String replaceHtml(String html) {
		if (isBlank(html)){
			return "";
		}
		String regEx = "<.+?>";
		Pattern p = Pattern.compile(regEx);
		Matcher m = p.matcher(html);
		String s = m.replaceAll("");
		return s;
	}
	
	/**
	 * 替换为手机识别的HTML，去掉样式及属性，保留回车。
	 * @param html
	 * @return
	 */
	public static String replaceMobileHtml(String html){
		if (html == null){
			return "";
		}
		return html.replaceAll("<([a-z]+?)\\s+?.*?>", "<$1>");
	}
	
	/**
	 * 替换为手机识别的HTML，去掉样式及属性，保留回车。
	 * @param txt
	 * @return
	 */
	public static String toHtml(String txt){
		if (txt == null){
			return "";
		}
		return replace(replace(Encodes.escapeHtml(txt), "\n", "<br/>"), "\t", "&nbsp; &nbsp; ");
	}
	private static void toNewLine(String str, int length,StringBuffer sb,String newLineSign) throws UnsupportedEncodingException{
		int currentLength = 0;
		StringBuffer tempsb=new StringBuffer();
		for (char c : str.toCharArray()) {
			currentLength += String.valueOf(c).getBytes("GBK").length;
			if (currentLength <= length - 3) {
				sb.append(c);
				
			} else {
				tempsb.append(c);
			}
		}
		sb.append(newLineSign);
		if(StringUtils.isNotEmpty(tempsb.toString())){
			toNewLine(tempsb.toString(),length,sb,newLineSign);
		}
	}
	/**
	 * 向一个url中添加参数
	 * @param originUrl:原始rul
	 * @param params：参数map
	 * @param ifContainSingleQuote 参数value是否加单引号，这是可变参数，可以不传,true表示加单引号
	 * @return
	 */
	public static String appendUrlParam(String originUrl,Map<String,String> params,boolean ...ifContainSingleQuote){
	    if(params!=null)
		for(Map.Entry<String, String> entry : params.entrySet()){
		    String mapKey = entry.getKey();
		    String mapValue = entry.getValue();
		    originUrl=appendUrlParam(originUrl,mapKey,mapValue,ifContainSingleQuote);
		}
	    return originUrl;
	}
	/**
	 * 向一个url中添加参数
	 * @param originUrl:原始rul
	 * @param paramKey：参数key
	 * @param paramValue;参数value
	 * @param ifContainSingleQuote 参数value是否加单引号，这是可变参数，可以不传,true表示加单引号
	 * @return
	 */
	public static String appendUrlParam(String originUrl,String paramKey,String paramValue,boolean ...ifContainSingleQuote){
		if(isEmpty(originUrl))return originUrl;
		boolean isNeedSingleQuote=false;
		if(ifContainSingleQuote.length>0)isNeedSingleQuote=ifContainSingleQuote[0];
		MyStringBuffer callBackUrlsb=new MyStringBuffer();
		callBackUrlsb.append(originUrl);
		if(!callBackUrlsb.toString().contains("?")){
			callBackUrlsb.append("?");
		}
		if(isNeedSingleQuote){
			paramValue="'"+paramValue+"'";
		}
		//问号后面是否有内容，即问号后面的内容
		String afterQuestionMarkContent=callBackUrlsb.toString().substring(callBackUrlsb.toString().indexOf("?")+1);
		if (StringUtils.isEmpty(afterQuestionMarkContent)) {
			callBackUrlsb.append(paramKey,"=",paramValue);
		}else{
			callBackUrlsb.append("&",paramKey,"=",paramValue);
		}
		return callBackUrlsb.toString();
	}
	/**
	 * 将过长的字符串进行换行（不区分中英文字符）
	 * @param str 目标字符串
	 * @param length 换行长度
	 * @param newLineSign 换行符，通常为br、\n等，由用户自定义
	 * @return
	 */
	public static String abbrToNewLine(String str, int length,String newLineSign) {
		if (str == null) {
			return "";
		}
		try {
			StringBuffer sb = new StringBuffer();
			String replaceHtml=replaceHtml(StringEscapeUtils.unescapeHtml4(str));
			toNewLine(replaceHtml,length,sb,newLineSign);
			return sb.toString();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return "";
	}
	/**
	 * 缩略字符串（不区分中英文字符）
	 * 注意，每个中文算作两个长度，英文算作一个长度
	 * @param str 目标字符串
	 * @param length 截取长度
	 * @return
	 */
	public static String abbr(String str, int length) {
		if (str == null) {
			return "";
		}
		try {
			StringBuilder sb = new StringBuilder();
			int currentLength = 0;
			for (char c : replaceHtml(StringEscapeUtils.unescapeHtml4(str)).toCharArray()) {
				currentLength += String.valueOf(c).getBytes("GBK").length;
				if (currentLength <= length - 3) {
					sb.append(c);
				} else {
					sb.append("...");
					break;
				}
			}
			return sb.toString();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return "";
	}
	
	public static String abbr2(String param, int length) {
		if (param == null) {
			return "";
		}
		StringBuffer result = new StringBuffer();
		int n = 0;
		char temp;
		boolean isCode = false; // 是不是HTML代码
		boolean isHTML = false; // 是不是HTML特殊字符,如&nbsp;
		for (int i = 0; i < param.length(); i++) {
			temp = param.charAt(i);
			if (temp == '<') {
				isCode = true;
			} else if (temp == '&') {
				isHTML = true;
			} else if (temp == '>' && isCode) {
				n = n - 1;
				isCode = false;
			} else if (temp == ';' && isHTML) {
				isHTML = false;
			}
			try {
				if (!isCode && !isHTML) {
					n += String.valueOf(temp).getBytes("GBK").length;
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

			if (n <= length - 3) {
				result.append(temp);
			} else {
				result.append("...");
				break;
			}
		}
		// 取出截取字符串中的HTML标记
		String temp_result = result.toString().replaceAll("(>)[^<>]*(<?)",
				"$1$2");
		// 去掉不需要结素标记的HTML标记
		temp_result = temp_result
				.replaceAll(
						"</?(AREA|BASE|BASEFONT|BODY|BR|COL|COLGROUP|DD|DT|FRAME|HEAD|HR|HTML|IMG|INPUT|ISINDEX|LI|LINK|META|OPTION|P|PARAM|TBODY|TD|TFOOT|TH|THEAD|TR|area|base|basefont|body|br|col|colgroup|dd|dt|frame|head|hr|html|img|input|isindex|li|link|meta|option|p|param|tbody|td|tfoot|th|thead|tr)[^<>]*/?>",
						"");
		// 去掉成对的HTML标记
		temp_result = temp_result.replaceAll("<([a-zA-Z]+)[^<>]*>(.*?)</\\1>",
				"$2");
		// 用正则表达式取出标记
		Pattern p = Pattern.compile("<([a-zA-Z]+)[^<>]*>");
		Matcher m = p.matcher(temp_result);
		List<String> endHTML = Lists.newArrayList();
		while (m.find()) {
			endHTML.add(m.group(1));
		}
		// 补全不成对的HTML标记
		for (int i = endHTML.size() - 1; i >= 0; i--) {
			result.append("</");
			result.append(endHTML.get(i));
			result.append(">");
		}
		return result.toString();
	}
	
	/**
	 * 转换为Double类型
	 */
	public static Double toDouble(Object val){
		if (val == null){
			return 0D;
		}
		try {
			return Double.valueOf(trim(val.toString()));
		} catch (Exception e) {
			return 0D;
		}
	}
	/**
	 * trim所有的空格
	 * @param str
	 * @return 
	 */
	public static String trimAllWhitespace(String str) {
		return org.springframework.util.StringUtils.trimAllWhitespace(str);
	}
	/**
	 * 转换为Float类型
	 */
	public static Float toFloat(Object val){
		return toDouble(val).floatValue();
	}
	/**
	 * 根据bean字段属性获得set或者get方法的字符串
	 * @param prop
	 * @param bl true获得set方法字符串，获得get方法字符串
	 * @return
	 */
	public static String findSetGetByProp(String prop,boolean bl){
		String rr;
		if(bl){
			rr="set"+firstToUpper(prop);
		}else{
			rr="get"+firstToUpper(prop);
		}
		return rr;
	}
	/**
	 * 首字母小写
	 * @param str
	 * @return
	 */
   public static String firstToLower(String str){
		    char[] ch = str.toCharArray();  
		    if (ch[0]>='A'  &&  ch[0]<='Z') {  
		        ch[0] = (char) (ch[0] + 32);  
		    }  
		    return new String(ch);  
   }
	/**
	 * 首字母大写
	 * @param str
	 * @return
	 */
   public static String firstToUpper(String str){
		    char[] ch = str.toCharArray();  
		    if (ch[0] >= 'a' && ch[0] <= 'z') {  
		        ch[0] = (char) (ch[0] - 32);  
		    }  
		    return new String(ch);  
   }
	/**
	 * 转换为Long类型
	 */
	public static Long toLong(Object val){
		return toDouble(val).longValue();
	}

	/**
	 * 转换为Integer类型
	 */
	public static Integer toInteger(Object val){
		return toLong(val).intValue();
	}
	
	/**
	 * 获得i18n字符串
	 */
	public static String getMessage(String code, Object[] args) {
		LocaleResolver localLocaleResolver = (LocaleResolver) SpringContextHolder.getBean(LocaleResolver.class);
		HttpServletRequest request = ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest();  
		Locale localLocale = localLocaleResolver.resolveLocale(request);
		return SpringContextHolder.getApplicationContext().getMessage(code, args, localLocale);
	}
	
	/**
	 * 获得用户远程地址
	 */
	public static String getRemoteAddr(HttpServletRequest request){
		String remoteAddr = request.getHeader("X-Real-IP");
        if (isNotBlank(remoteAddr)) {
        	remoteAddr = request.getHeader("X-Forwarded-For");
        }else if (isNotBlank(remoteAddr)) {
        	remoteAddr = request.getHeader("Proxy-Client-IP");
        }else if (isNotBlank(remoteAddr)) {
        	remoteAddr = request.getHeader("WL-Proxy-Client-IP");
        }
        return remoteAddr != null ? remoteAddr : request.getRemoteAddr();
	}

	/**
	 * 驼峰命名法工具
	 * @return
	 * 		toCamelCase("hello_world") == "helloWorld" 
	 * 		toCapitalizeCamelCase("hello_world") == "HelloWorld"
	 * 		toUnderScoreCase("helloWorld") = "hello_world"
	 */
    public static String toCamelCase(String s) {
        if (s == null) {
            return null;
        }

        s = s.toLowerCase();

        StringBuilder sb = new StringBuilder(s.length());
        boolean upperCase = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (c == SEPARATOR) {
                upperCase = true;
            } else if (upperCase) {
                sb.append(Character.toUpperCase(c));
                upperCase = false;
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
	 * 驼峰命名法工具
	 * @return
	 * 		toCamelCase("hello_world") == "helloWorld" 
	 * 		toCapitalizeCamelCase("hello_world") == "HelloWorld"
	 * 		toUnderScoreCase("helloWorld") = "hello_world"
	 */
    public static String toCapitalizeCamelCase(String s) {
        if (s == null) {
            return null;
        }
        s = toCamelCase(s);
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
    
    /**
	 * 驼峰命名法工具
	 * @return
	 * 		toCamelCase("hello_world") == "helloWorld" 
	 * 		toCapitalizeCamelCase("hello_world") == "HelloWorld"
	 * 		toUnderScoreCase("helloWorld") = "hello_world"
	 */
    public static String toUnderScoreCase(String s) {
        if (s == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        boolean upperCase = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            boolean nextUpperCase = true;

            if (i < (s.length() - 1)) {
                nextUpperCase = Character.isUpperCase(s.charAt(i + 1));
            }

            if ((i > 0) && Character.isUpperCase(c)) {
                if (!upperCase || !nextUpperCase) {
                    sb.append(SEPARATOR);
                }
                upperCase = true;
            } else {
                upperCase = false;
            }

            sb.append(Character.toLowerCase(c));
        }

        return sb.toString();
    }
    public static boolean contains(String target,String ...con){
    	boolean bl=false;
    	for(String c:con){
    		if(contains(target.toLowerCase(), c.toLowerCase())){
    			bl=true;
    			break;
    		}
    	}
    	return bl;
    }
    /**
     * 如果不为空，则设置值
     * @param target
     * @param source
     */
    public static void setValueIfNotBlank(String target, String source) {
		if (isNotBlank(source)){
			target = source;
		}
	}
 
    /**
     * 转换为JS获取对象值，生成三目运算返回结果
     * @param objectString 对象串
     *   例如：row.user.id
     *   返回：!row?'':!row.user?'':!row.user.id?'':row.user.id
     */
    public static String jsGetVal(String objectString){
    	StringBuilder result = new StringBuilder();
    	StringBuilder val = new StringBuilder();
    	String[] vals = split(objectString, ".");
    	for (int i=0; i<vals.length; i++){
    		val.append("." + vals[i]);
    		result.append("!"+(val.substring(1))+"?'':");
    	}
    	result.append(val.substring(1));
    	return result.toString();
    }
    /**
     * 为以逗号或其他分隔符分割的字符串，添加单引号;同时若中间有空字符串则过滤掉
     * @param str a,b,c 字符串
     * @param split 分隔符，传空默认为逗号
     */
    public static String addSingleQuoteForStr(String str,String split){
    	if(isEmpty(str))return str;
    	StringBuffer sb = new StringBuffer();
    	if(StringUtils.isEmpty(split))split=",";
    	  String[] temp = str.split(split);
    	  for (int i = 0; i < temp.length; i++) {
    	   if (StringUtils.isNotEmpty(temp[i]))
    	    sb.append("'" + temp[i] + "'"+split);
    	  }
    	  String result = sb.toString();
    	  String tp = result.substring(result.length() - 1, result.length());
    	  if (split.equals(tp))
    	   return result.substring(0, result.length() - 1);
    	  else
    	   return result;
    }
    
    /**
     * 分割后放入Map
     * @param str  要分割的字符串
     * @param a    分割符号
     * @return     Map
     */
    public static Map<String,String>  split2Map(String str,Character a){
    	String[] arr=split(str, a);
    	Map<String,String>   map=new LinkedHashMap<String, String>();
    	
    	for(String s:arr){
    		map.put(s, s);
    	}
    	return map;
    }
    /**
	 * 使用正则表达式提取小括号中的内容
	 * 小括号无论是用中文还是应为的都可以提出来
	 * @param msg
	 * @return 
	 */
	public static String extractBracketRegular(String msg){
		List<String> ptrList=Lists.newArrayList();
		ptrList.add("(\\([^\\)]*\\))");
		ptrList.add("(\\（[^\\)]*\\))");
		ptrList.add("(\\（[^\\)]*\\）)");
		ptrList.add("(\\([^\\)]*\\）)");
		String result=null;
		for(String ptr:ptrList) {
			Pattern p = Pattern.compile(ptr);
			Matcher m = p.matcher(msg);
			while(m.find()){
				result=m.group().substring(1, m.group().length()-1);
				break;
			}
		}
		
		return result;
	}
    /**
     * 默认逗号分割后放入Map
     * @param str  要分割的字符串 
     * @return     Map
     */
    public static Map<String,String>  split2Map(String str){
    	String[] arr=split(str, ',');
    	Map<String,String>   map=new LinkedHashMap<String, String>();
    	
    	for(String s:arr){
    		map.put(s, s);
    	}
    	return map;
    }
    /**
     * 字符串转int类型，若为空则返回defValue
     * @param page
     * @param defValue
     * @return
     */
    public static int strToInt(Object obj,int defValue){
    	String str=obj==null?"":obj.toString();
        if(StringUtils.isBlank(str)){
            return defValue;
        }
        return Integer.valueOf(str);
    }
    public static void main(String[] args) {
    	Map<String,String>   map=split2Map("abc,kjk,ssss", ',');
    	System.out.println(map.toString());
	}
    /**
     * 将字符串按照分隔符进行分割，并且将分隔符附加在后面的内容中，这个分隔符是多个，使用|，如，|。|；这种。
     * @param source
     * @param splitStr
     * @return
     */
    public static String[] splitAppendSelfAfter(String source,String splitStr){
		/*需要分割的文章*/  
		String str = source;  
		  
		/*正则表达式：句子结束符*/  
		String regEx=splitStr;   
		Pattern p =Pattern.compile(regEx);  
		Matcher m = p.matcher(str);  
		  
		/*按照句子结束符分割句子*/  
		String[] words = p.split(str);  
		String before="";
		/*将句子结束符连接到相应的句子后*/  
		if(words.length > 0)  
		{  
		    int count = 0;  
		    while(count < words.length)  
		    {  
		        if(m.find())  
		        {  
		            words[count] = before+words[count];  
		            before=m.group();
		        }else{
		        	 words[count] = before+words[count];  
		        }  
		        count++;  
		    }  
		}  
//		  
//		/*输出结果*/  
//		for(int index = 0; index < words.length; index++)  
//		{  
//		    String word = words[index];  
//		}  
		return words;
	}
  /**
   * 根据正则表达式到字符串中提取对应内容
   * @param regEx
   * @param str
   * @return
   */
    public static String matchResult(String regEx,String str)
	{
    	Pattern p=Pattern.compile(regEx);
		StringBuilder sb = new StringBuilder();
		Matcher m = p.matcher(str);
		while (m.find())
		for (int i = 0; i <= m.groupCount(); i++) 
		{
			sb.append(m.group());   
		}
		return sb.toString();
	}
    /**
     * 从字符串中获得其中的中文部分
     * @param chines
     * @return
     */
 	public static String findChineseFromStr(String originStr) {
 		String regEx1 = "[\\u4e00-\\u9fa5]";//\\)表示也包含括号
 		return matchResult(regEx1,originStr);
 	}
 	 /**
     * 从字符串中获得其中的中文部分
     * @param chines
     * @return
     */
 	public static String findChineseFromStrContainkuohao(String originStr) {
 		String regEx1 = "[\\(\\u4e00-\\u9fa5\\)]";//\\)表示也包含括号
 		return matchResult(regEx1,originStr);
 	}
 	/**
     * 从字符串中获得其中的英文部分
     * @param originStr
     * @param isContainNumber 是否包含数字
     * @return
     */
 	public static String findEnglishFromStr(String originStr,boolean isContainNumber) {
 		String regEx1 = "[a-zA-Z]";
 		if(isContainNumber) {
 			regEx1 = "[a-zA-Z0-9]";
 		}
 		return matchResult(regEx1,originStr);
 	}
    
 	/**
 	 * 将浮点数值类型，包括多种double、decimal等，转化为string类型时，对于x.0这种情况去掉后面的.0；
 	 * 注意：x.5，依然会保留，只是不会出现1.0这种情况而已
 	 * @param obj
 	 * @return
 	 */
 	public static String formatNumberToString(Object obj) {
 		DecimalFormat decimalFormat = new DecimalFormat("###################.###########");
 		String pxvalue=decimalFormat.format(obj);//12.0这种去掉0，如果是12.5则保留
 		return pxvalue;
 	}
 
}
