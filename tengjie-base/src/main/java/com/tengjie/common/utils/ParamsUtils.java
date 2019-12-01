package com.tengjie.common.utils;


import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.PropertyFilter;
import com.alibaba.fastjson.serializer.SerializeFilter;
import com.alibaba.fastjson.serializer.SerializeWriter;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.serializer.SimplePropertyPreFilter;
import com.alibaba.fastjson.serializer.ValueFilter;
import com.fasterxml.jackson.databind.deser.std.CollectionDeserializer;
import com.google.common.collect.Maps;
import com.tengjie.common.persistence.TjBaseEntity;
import com.tengjie.common.persistence.Page;
import com.tengjie.common.persistence.PageMap;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by hzl on 2017/7/6.
 */
public class ParamsUtils {

    /**
     * 描述：获取请求参数
     * @param request
     * @return
     */
    public static JSONObject getReqPrams(HttpServletRequest request){
        try {
            StringBuffer buffer = new StringBuffer();
            ServletInputStream inputStream = request.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream(), "utf-8"));
            String line;
            while ((line = br.readLine()) != null) {
                buffer.append(line);
            }
            if(StringUtils.isBlank(buffer)){
                buffer.append("{}");
            }
            return JSON.parseObject(buffer.toString());
        }catch (Exception e){}
        return new JSONObject();
    }

    /**
     * 描述：request
     * @param sobj
     * @return
     */
    public static JSONObject getRequest(JSONObject sobj) {
        JSONObject requestJson = (JSONObject) sobj.get("request");
        return requestJson;
    }
    /**
     * 描述：Header
     * @param sobj
     * @return
     */
    public static JSONObject getHeader(JSONObject sobj) {
        JSONObject headerJson = (JSONObject) sobj.get("header");
        return headerJson;
    }
    /**
     * 描述：Params参数
     * @param sobj
     * @return
     */
    public static JSONObject getParams(JSONObject sobj) {
        JSONObject requestJson = (JSONObject) sobj.get("request");
        JSONObject paramsJson = (JSONObject) requestJson.get("params");
        return paramsJson;
    }

    /**
     * 获取params中的数组
     * @param params
     * @return
     */
    public static JSONArray getDataArray(JSONObject params) {
        JSONArray jsonArray=params.getJSONArray("dataArray");
        return jsonArray;
    }

    //封装header头信息
    public static Map<String,String> setHeaderValueJson() {
        Map<String,String> headerValueMap = new HashMap<String, String>();
        headerValueMap.put("device", "01");
        headerValueMap.put("platform", "01");
        headerValueMap.put("version", "1.0");
        return headerValueMap;
    }
    //封装response头信息
    public static Map<String,Object> setResponseJson(String status,String message,Map<String,Map<String,Object>> resultValueMap) {
        Map<String,Object> responseValueMap = new HashMap<String, Object>();
        responseValueMap.put("message", message);
        responseValueMap.put("status", status);
        responseValueMap.put("result", resultValueMap);
        return responseValueMap;
    }

    //封装data数据
    public static Map<String,Map<String,Object>> setDataJson(String dataName,List list,int pageCount,int recordCount) {
        Map<String,Map<String,Object>> responseValueMap = new HashMap<String, Map<String,Object>>();
        Map<String,Object> dataMap = new HashMap<String, Object>();
        dataMap.put("data", list);
        dataMap.put("pageCount", pageCount);
        dataMap.put("recordCount", recordCount);
        responseValueMap.put(dataName, dataMap);
        return responseValueMap;
    }
    //封装多个data数据
    public static Map<String,Map<String,Object>> setMoreDataJson(List<Map<String,Object>> dataList) {
        Map<String,Map<String,Object>> responseValueMap = new HashMap<String, Map<String,Object>>();
        for(int i=0;i<dataList.size();i++){
            Map<String,Object> newMap = new HashMap<String, Object>();
            newMap.put("data", dataList.get(i).get("list"));
            newMap.put("pageCount", dataList.get(i).get("pageCount"));
            newMap.put("recordCount", dataList.get(i).get("recordCount"));
            responseValueMap.put(dataList.get(i).get("dataName")+"", newMap);
        }
        return responseValueMap;
    }
    //分装返回的json参数
    public static JSONObject setReturnJson(String status,String message,Map<String,Map<String,Object>> resultValueMap) {
        Map<String,Object> returnMap = new HashMap<String, Object>();
        returnMap.put("header", setHeaderValueJson());
        returnMap.put("response", setResponseJson(status,message,resultValueMap));
        String json = JSONObject.toJSONString(returnMap,SerializerFeature.WriteMapNullValue, SerializerFeature.WriteNullNumberAsZero	); //转成JSON数据
        JSONObject jobj=JSON.parseObject(json);//将字符串转为json格式
        return jobj;
    }
    //已流的形式向客户的返回json数据
    public static void getPrint(HttpServletResponse response, JSONObject jsonObject) throws IOException {
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        out.print(jsonObject);
        out.flush();
        out.close();
    }
    public static JSONObject getReturnOneListJson(String status,String message,List list,int pageType,int pageCount) {
        if(pageCount == 999999){
            pageCount = 1;
        }
        JSONObject jsonObject=new JSONObject();
        if(status.equals(GlobalUtils.suc)){
            //判断是否有分页信息
            if(pageType == 1){
                jsonObject=setReturnJson(
                        status,message,
                        setDataJson("mainData",list,pageCount,list.size())
                );
            }else{
                jsonObject = setReturnJson(
                        status,message,
                        setDataJson("mainData",list,list.size(),list.size())
                );
            }
            //返回没有查到数据的json
        }else if(status.equals("09")){
            //返回失败信息。
            jsonObject=setReturnJson(
                    status,message,
                    new HashMap<String,Map<String,Object>>()
            );
            //返回提示信息的json
        }else{
            //返回失败信息。
            jsonObject=setReturnJson(
                    status,message,
                    new HashMap<String,Map<String,Object>>()
            );
        }
        return jsonObject;
    }

    /**
     * 描述：检测报文参数是否合法
     * @param strs
     */
    public static boolean isValidParams(String ... strs){
        for (int i = 0; i <strs.length; i++) {
            if(StringUtils.isBlank(strs[i])){
                return true;
            }
        }
        return false;
    }
    /**
     * 描述：报文参数错误
     * @return
     */
    public static JSONObject paramsIsNull(){
        JSONObject jsonObject =  ParamsUtils.setReturnJson( GlobalUtils.prompt,GlobalUtils.promptMsg,new HashMap<String, Map<String, Object>>());
        return jsonObject;
    }
    /**
     * 描述：异常消息
     * @return
     */
    public static JSONObject exceptionMsg(){
        JSONObject jsonObject =  ParamsUtils.setReturnJson( GlobalUtils.fail,GlobalUtils.failMsg,new HashMap<String, Map<String, Object>>());
        return jsonObject;
    }

    /**
     * 描述：处理分页页码
     * @param page
     * @param defValue
     * @return
     */
    public static int page(String page,int defValue){
        if(StringUtils.isBlank(page)){
            return defValue;
        }
        return Integer.valueOf(page);
    }
    /**
     * 描述：处理行号
     * @param rows
     * @param defValue
     * @return
     */
    public static int rows(String rows,int defValue){
        if(StringUtils.isBlank(rows)){
            return defValue;
        }
        return Integer.valueOf(rows);
    }
    
    /**
     * 描述：本方法只对数据库出来的list、bean page对象进行过滤截取，然后返回JSONArray，以便调用方拿回去放到自己map里面重新组装
     * @param status
     * @param includeOrExclude:true 为include false为exclude
     * @param resultValueObject
     * @return
     */
    public static String parseBeanForInOrExclude(String listDataName,String status,Object resultValueObject,boolean includeOrExclude, List<String> cludeKeys) {
        Map<String,Object> returnMap = new HashMap<String, Object>();
        returnMap.put("status", status);
        
       
        if(StringUtils.isEmpty(listDataName)){
        	listDataName="listData";
        }
        Map<String,Object> childMap=new HashMap();
        Object obj=null;
        if(resultValueObject instanceof Page){
        	Page pp=(Page)resultValueObject;
        	returnMap.put("pageNo", pp.getPageNo());
        	returnMap.put("pageSize", pp.getPageSize());
        	obj=parseBeanForInOrExclude(pp.getList(),includeOrExclude,cludeKeys);
        }else if(resultValueObject instanceof Collection){//list等集合类型
        	 obj=parseBeanForInOrExclude(resultValueObject,includeOrExclude,cludeKeys);
        }else{//单个bean类型
        	obj=parseBeanForInOrExclude(resultValueObject,includeOrExclude,cludeKeys);
             returnMap.put("data", obj);
            
        	 return JSONObject.toJSONString(returnMap,SerializerFeature.WriteMapNullValue,
                      SerializerFeature.WriteNullListAsEmpty,
                      SerializerFeature.WriteNullStringAsEmpty,
                      SerializerFeature.WriteNullNumberAsZero,
                      SerializerFeature.WriteNullBooleanAsFalse); //转成JSON数据
        }
       
        childMap.put(listDataName, obj);
        returnMap.put("data", childMap);

       String json = JSONObject.toJSONString(returnMap,filter,SerializerFeature.WriteMapNullValue,
                                                       SerializerFeature.WriteNullListAsEmpty,
                                                       SerializerFeature.WriteNullStringAsEmpty,
                                                       SerializerFeature.WriteNullNumberAsZero,
                                                       SerializerFeature.WriteNullBooleanAsFalse); //转成JSON数据
    	return  json;
    }
   
    /**
     * 描述：本方法只对数据库出来的list、bean对象进行过滤截取，然后返回JSONArray，以便调用方拿回去放到自己map里面重新组装
     * @param status
     * @param includeOrExclude:true 为include false为exclude
     * @param resultValueObject
     * @return
     */
    public static Object parseBeanForInOrExclude(Object resultValueObject,boolean includeOrExclude, List<String> cludeKeys) {
    	   SimplePropertyPreFilter propfilter = new SimplePropertyPreFilter();
           propfilter.getExcludes().add("page");
           if(cludeKeys!=null){
           	if(includeOrExclude){
               	
               	propfilter.getIncludes().addAll(cludeKeys);
               }else{
               	propfilter.getExcludes().addAll(cludeKeys);
               }
           }
         
    	String  json = JSONObject.toJSONString(resultValueObject,propfilter,SerializerFeature.WriteMapNullValue,
                  SerializerFeature.WriteNullListAsEmpty,
                  SerializerFeature.WriteNullStringAsEmpty,
                  SerializerFeature.WriteNullNumberAsZero,
                  SerializerFeature.WriteNullBooleanAsFalse); //转成JSON数据
    	if(resultValueObject instanceof Collection){
    		return  JSONObject.parseArray(json);
    	}else{
    		return  JSONObject.parseObject(json);
    	}

    }

    /**
     * 描述：返回报文通用方法
     * @param status
     * @param includeOrExclude:true 为include false为exclude
     * @param resultValueMap
     * @return
     */
    public static String reposeBodyForInOrExclude(String status,Object resultValueObject,boolean includeOrExclude, List<String> cludeKeys,String[] ...dateFormat) {
    	 Map<String,Object> returnMap = new HashMap<String, Object>();
         returnMap.put("status", status);
         returnMap.put("data", "replace");
         String json ="";
         /**
          * WriteNullNumberAsZero	数值字段如果为null,输出为0,而非null
          * WriteMapNullValue	是否输出值为null的字段,默认为false
          *///SerializeFilter

         SimplePropertyPreFilter propfilter = new SimplePropertyPreFilter();
         propfilter.getExcludes().add("page");
         if(cludeKeys!=null){
         	if(includeOrExclude){
             	
             	propfilter.getIncludes().addAll(cludeKeys);
             }else{
             	propfilter.getExcludes().addAll(cludeKeys);
             }
         }
          Map<String,String> datefmatMap=new HashMap();
         if(dateFormat!=null&&dateFormat.length>0){
        	 for(String[] df:dateFormat){
        		 datefmatMap.put(df[0], df[1]);
        	 }
         }
         final Map<String,String> innerMap=datefmatMap;
         ValueFilter dateFilter = new ValueFilter() {
             public Object process(Object obj, String name, Object v) {
                 if (v == null||StringUtils.isEmpty(v+""))
                     return "";
                 if(innerMap.containsKey(name)){
                	  if(v instanceof java.util.Date){
                     	return  DateUtils.formatDate(( java.util.Date)v, innerMap.get(name));
                      }
                      if(v instanceof String){
                     	 try {
							return DateUtils.parseDate(v+"", innerMap.get(name));
						} catch (ParseException e) {
							e.printStackTrace();
							  return v;
						}
                      }
                 }
               
                 return v;
             }
         };
         SerializeFilter[] sfArray=new SerializeFilter[2];
         sfArray[0]=propfilter;
         sfArray[1]=dateFilter;
       String temp=JSONObject.toJSONString(returnMap);//必须要先这样，然后替换，否则过滤器如果是include，会不包含把status、data都干掉，所以是分开来过滤再拼接替换

         json = JSONObject.toJSONString(resultValueObject,sfArray,SerializerFeature.WriteMapNullValue,
                 SerializerFeature.WriteNullListAsEmpty,
                 SerializerFeature.WriteNullStringAsEmpty,
                 SerializerFeature.WriteNullNumberAsZero,
                 SerializerFeature.WriteNullBooleanAsFalse); //转成JSON数据
         return temp.replace("\"replace\"", json);
    }
    /**
     * 描述：返回报文通用方法
     * @param status
     * @param includeOrExclude:true 为include false为exclude
     * @param resultValueMap
     * @return
     */
    public static String reposeBodyForInOrExclude(String status,Object resultValueObject,boolean includeOrExclude, List<String> cludeKeys) {
        Map<String,Object> returnMap = new HashMap<String, Object>();
        returnMap.put("status", status);
        returnMap.put("data", "replace");
        String json ="";
        /**
         * WriteNullNumberAsZero	数值字段如果为null,输出为0,而非null
         * WriteMapNullValue	是否输出值为null的字段,默认为false
         *///SerializeFilter
        SimplePropertyPreFilter propfilter = new SimplePropertyPreFilter();
        propfilter.getExcludes().add("page");
        if(cludeKeys!=null){
        	if(includeOrExclude){
            	
            	propfilter.getIncludes().addAll(cludeKeys);
            }else{
            	propfilter.getExcludes().addAll(cludeKeys);
            }
        }
      String temp=JSONObject.toJSONString(returnMap);//必须要先这样，然后替换，否则过滤器如果是include，会不包含把status、data都干掉，所以是分开来过滤再拼接替换

        json = JSONObject.toJSONString(resultValueObject,propfilter,SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteNullListAsEmpty,
                SerializerFeature.WriteNullStringAsEmpty,
                SerializerFeature.WriteNullNumberAsZero,
                SerializerFeature.WriteNullBooleanAsFalse); //转成JSON数据
        return temp.replace("\"replace\"", json);
    }
 
   // SimplePropertyPreFilter filter = new SimplePropertyPreFilter();
    /**
     * 描述：返回报文通用方法,本方法一般为管理后台使用，所处数据均为固定化的stats+data+list或者bean，上面方法是为接口使用可自定义data后面的参数名称
     * @param status
     * @param resultValueMap
     * @return
     */
    public static String reposeBodyForIgnoreProp(String status,Object resultValueObject) {
        Map<String,Object> returnMap = new HashMap<String, Object>();
        returnMap.put("status", status);
        returnMap.put("data", resultValueObject);
        /**
         * WriteNullNumberAsZero	数值字段如果为null,输出为0,而非null
         * WriteMapNullValue	是否输出值为null的字段,默认为false
         *///SerializeFilter
        SimplePropertyPreFilter propfilter = new SimplePropertyPreFilter();
        propfilter.getExcludes().add("page");
        SerializeWriter out = new SerializeWriter();
        JSONSerializer serializer = null;
        try {
       
//         serializer = new JSONSerializer(out);
//        serializer.getValueFilters().add(filter);
//        serializer.getPropertyPreFilters().add(propfilter);
//        serializer.write(returnMap);
        } finally {
            out.close();
        }
        String json = JSONObject.toJSONString(returnMap,propfilter,SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteNullListAsEmpty,
                SerializerFeature.WriteNullStringAsEmpty,
                SerializerFeature.WriteNullNumberAsZero,
                SerializerFeature.WriteNullBooleanAsFalse); //转成JSON数据
        return json;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * 描述：返回报文通用方法
     * @param status
     * @param resultValueMap
     * @return
     */
    public static String reposeBody(String status,Object resultValueObject) {
        Map<String,Object> returnMap = new HashMap<String, Object>();
        returnMap.put("status", status);
        returnMap.put("data", resultValueObject);
        /**
         * WriteNullNumberAsZero	数值字段如果为null,输出为0,而非null
         * WriteMapNullValue	是否输出值为null的字段,默认为false
         */
        String json = JSONObject.toJSONString(returnMap,filter,SerializerFeature.WriteMapNullValue,
                                                        SerializerFeature.WriteNullListAsEmpty,
                                                        SerializerFeature.WriteNullStringAsEmpty,
                                                        SerializerFeature.WriteNullNumberAsZero,
                                                        SerializerFeature.WriteNullBooleanAsFalse); //转成JSON数据
        return json;
    }
    /**
     * 描述：返回报文通用方法
     * @param status
     * @param resultValueMap
     * @return
     */
    public static String reposeBodyForError(String errcode,String errmsg,Map<String ,Object> resultValueMap) {
        Map<String,Object> returnMap = new HashMap<String, Object>();
        returnMap.put("errcode", errcode);
        if(StringUtils.isNoneBlank(errmsg)){
        	returnMap.put("errmsg", errmsg);
        }
        if(resultValueMap != null && resultValueMap.size() > 0){
        	returnMap.put("data", resultValueMap);
        }
        /**
         * WriteNullNumberAsZero	数值字段如果为null,输出为0,而非null
         * WriteMapNullValue	是否输出值为null的字段,默认为false
         */
        String json = JSONObject.toJSONString(returnMap,filter,SerializerFeature.WriteMapNullValue,
                                                        SerializerFeature.WriteNullListAsEmpty,
                                                        SerializerFeature.WriteNullStringAsEmpty,
                                                        SerializerFeature.WriteNullNumberAsZero,
                                                        SerializerFeature.WriteNullBooleanAsFalse); //转成JSON数据
        return json;
    }
   
    /**
     * 描述：返回报文通用方法
     * @param status
     * @param resultValueMap
     * @return
     */
    public static String reposeBody(String status,Map<String ,Object> resultValueMap) {
        Map<String,Object> returnMap = new HashMap<String, Object>();
        returnMap.put("status", status);
        returnMap.put("data", resultValueMap);
        /**
         * WriteNullNumberAsZero	数值字段如果为null,输出为0,而非null
         * WriteMapNullValue	是否输出值为null的字段,默认为false
         */
        String json = JSONObject.toJSONString(returnMap,filter,SerializerFeature.WriteMapNullValue,
                                                        SerializerFeature.WriteNullListAsEmpty,
                                                        SerializerFeature.WriteNullStringAsEmpty,
                                                        SerializerFeature.WriteNullNumberAsZero,
                                                        SerializerFeature.WriteNullBooleanAsFalse,
                                                        SerializerFeature.DisableCircularReferenceDetect); //转成JSON数据;SerializerFeature.DisableCircularReferenceDetect新加的，禁止循环检测，否则对于相同子数据会出来引用类型 $ref
        return json;
    }
    /**
     * 描述：返回报文通用方法
     * @param status
     * @param resultValueMap
     * @return
     */
    public static String reposeBodyOut(String errcode,String errmsg,Map<String ,Object> resultValueMap) {
        Map<String,Object> returnMap = new HashMap<String, Object>();
        returnMap.put("errcode", errcode);
        if(StringUtils.isNoneBlank(errmsg)){
        	returnMap.put("errmsg", errmsg);
        }
      
        if(resultValueMap != null && resultValueMap.size() > 0){
        	  returnMap.putAll(resultValueMap);
        }
        /**
         * WriteNullNumberAsZero	数值字段如果为null,输出为0,而非null
         * WriteMapNullValue	是否输出值为null的字段,默认为false
         */
        String json = JSONObject.toJSONString(returnMap,filter,SerializerFeature.WriteMapNullValue,
                                                        SerializerFeature.WriteNullListAsEmpty,
                                                        SerializerFeature.WriteNullStringAsEmpty,
                                                        SerializerFeature.WriteNullNumberAsZero,
                                                        SerializerFeature.WriteNullBooleanAsFalse,
                                                        SerializerFeature.DisableCircularReferenceDetect); //转成JSON数据
   
        return json;
    }
    private static ValueFilter filter = new ValueFilter() {

        public Object process(Object obj, String s, Object v) {
            if (v == null)
                return "";
            return v;
        }
    };
    /**
     * 描述：返回成功代码
     * @return
     */
    public static String suc200(){
        Map<String,Object> params = sucPackMap(null,null);
        return reposeBody(GlobalUtils.status200,params);
    }
    /**
     * 描述：返回成功代码,Object
     * @return
     */
    public static String suc200Object(Object obj){
    	ObjectDeserializer od=ParserConfig.getGlobalInstance().getDeserializer(obj.getClass()) ;
//    	if(od instanceof JavaBeanDeserializer){
//    		JSONObject.to
//    	}
//	    if(od instanceof MapDeserializer){
//    		
//    	}
//    	  if(od instanceof CollectionCodec){
//      	
//    	  }
//       if(od instanceof CollectionDeserializer){
//    		
//     	}
  
      //  Map<String,Object> params = sucPackMap(null,map);
        return reposeBodyForIgnoreProp(GlobalUtils.status200,obj);
    }
    /**
     * 描述：处理bean类型
     * @return
     */
    public static String suc200BeanInclude(Object obj,List<String> include){
    	return reposeBodyForInOrExclude(GlobalUtils.status200,obj,true,include);
    }
    /**
     * 描述：处理bean类型
     * @return
     */
    public static String suc200BeanInclude(Object obj,List<String> include,String[] ...dateFormat){
    	return reposeBodyForInOrExclude(GlobalUtils.status200,obj,true,include,dateFormat);
    }
    /**
     * 描述：处理bean类型
     * @return
     */
    public static String suc200BeanExclude(Object obj,List<String> exclude,String[] ...dateFormat){
    	return reposeBodyForInOrExclude(GlobalUtils.status200,obj,false,exclude,dateFormat);
    }
    /**
     * 描述：处理bean类型
     * @return
     */
    public static String suc200BeanExclude(Object obj,List<String> exclude){
    	return reposeBodyForInOrExclude(GlobalUtils.status200,obj,false,exclude);
    }
    /**
     * 描述：返回成功代码,Map集合
     * @return
     */
    public static String suc200Map(Map<String,Object> map){
        Map<String,Object> params = sucPackMap(null,map);
        return reposeBody(GlobalUtils.status200,params);
    }
    /**
     * 描述：返回成功代码,PageMap
     * @return
     */
    public static String suc200PageMap(PageMap pm){
    	Map<String,Object> params=new HashMap();
    	params.put("pageNo", pm.getPageNo());
    	params.put("pageSize", pm.getPageSize());
    	params.put("listData", pm.getList());
        return suc200Map(params);
    }
    /**
     * 描述：返回成功代码,List集合
     * @return
     */
    public static String suc200ListBean(List<?> list){
        Map<String,Object> params = sucPackMapBean(list,null);
       
        return  reposeBodyForInOrExclude(GlobalUtils.status200,params,true,null);
    }
    /**
     * 描述：返回成功代码,List集合
     * @return
     */
    public static String suc200ListBeanInclude(List<?> list,List<String> include,Map<String,String> ...fieldMapping){
        Map<String,Object> pkmap = new HashMap<String,Object>();
        if(list != null && list.size() >= 0){
            pkmap.put("listData",parseBeanForInOrExclude(list,true,include));
        }

        return  reposeBody(GlobalUtils.status200,pkmap);
    }
    /**
     * 描述：返回成功代码,List集合
     * @return
     */
    public static String suc200ListBeanExclude(List<?> list,List<String> exclude){
    	 Map<String,Object> pkmap = new HashMap<String,Object>();
         if(list != null && list.size() >= 0){
             pkmap.put("listData",parseBeanForInOrExclude(list,false,exclude));
         }
        return reposeBody(GlobalUtils.status200,pkmap);
    }
    /**
     * 描述：返回成功代码,List集合
     * @return
     */
    public static String suc200List(List<Map<String,Object>> list){
        Map<String,Object> params = sucPackMap(list,null);
        return reposeBody(GlobalUtils.status200,params);
    }
    /**
     * 描述：返回成功代码,List集合,Map集合
     * @return
     */
    public static String suc200ListMap(List<Map<String,Object>> list,Map<String,Object> map){
        Map<String,Object> params = sucPackMap(list,map);
        return reposeBody(GlobalUtils.status200,params);
    }
    /**
     * 描述：参数错误，返回400
     * @return
     */
    public static String err400(String errmsg){
        return reposeBodyForError(GlobalUtils.status400,errmsg,null);
    }

    /**
     * 描述：返回系统错误代码
     * @return
     */
    public static String err500Out(String errmsg){
        return reposeBodyOut(GlobalUtils.status500,errmsg,null);
    }
    /**
     * 描述：返回系统错误代码
     * @return
     */
    public static String err500Out(Exception e){
    	String errmsg = getExceptionDetail(e);
        return reposeBodyOut(GlobalUtils.status500,errmsg,null);
    }
    /**
     * 描述：返回系统错误代码
     * @return
     */
    public static String err500(String errmsg){
        return reposeBodyForError(GlobalUtils.status500,errmsg,null);
    }
    /**
     * 描述：返回系统错误代码
     * @return
     */
    public static String err500(Exception e){
    	String errmsg = getExceptionDetail(e);
        return reposeBodyForError(GlobalUtils.status500,errmsg,null);
    }
    /**
     * 描述：参数错误，返回400 errorstats\meassge包含在data中
     * @return
     */
    public static String err400(String errorStatus,String errorMessage){
         Map<String,Object> map = packMap(errorStatus,errorMessage,null,null);
        return reposeBody(GlobalUtils.status400,map);
    }
    /**
     * 描述：参数错误，返回400,,errorstats\meassge与status、data平级
     * err400Out与err400的区别是errorstats\meassge与status、data平级
     * @return
     */
    public static String err400Out(String errorStatus,String errorMessage){
         Map<String,Object> map = packMap(errorStatus,errorMessage,null,null);
        return reposeBodyOut(GlobalUtils.status400,map);
    }
    /**
     * 描述：参数错误，返回200,,errorstats\meassge与status、data平级
     * @return
     */
    public static String err200Out(String errorStatus,String errorMessage){
         Map<String,Object> map = packMap(errorStatus,errorMessage,null,null);
        return reposeBodyOut(GlobalUtils.status200,map);
    }
    /**
     * @Description： 异常信息捕获
     * @author: houzl 
     * @param e 异常类
     * @return
     * @since: 2017年10月12日 下午3:04:59
     */
	public static String getExceptionDetail(Exception e) {
		StringBuffer stringBuffer = new StringBuffer(e.toString() + "\n");
		StackTraceElement[] messages = e.getStackTrace();
		int length = messages.length;
		for (int i = 0; i < length; i++) {
			stringBuffer.append("\t" + messages[i].toString() + "\n");
		}
		return stringBuffer.toString();
	}
	
    /**
     * 描述：返回客户端json对象
     * @param response
     * @param str
     * @throws IOException
     */
    public static void writeStr(HttpServletResponse response,String str) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        out.print(str);
        out.flush();
        out.close();
    }

    /**
     * 描述：检测请求参数是否合法
     * @param strs
     */
    public static String isValidParamsStr(String errMsgs,String ... strs){
        String [] errs = errMsgs.split(",");
        if(0 == errs.length || errs.length < strs.length){
            throw new RuntimeException("参数不合法");
        }
        for (int i = 0; i <strs.length; i++) {
            if(StringUtils.isBlank(strs[i])){
                return "【"+errs[i]+"】参数不能为空";
            }

        }
        return "";
    }

    /**
     * 描述：封装返回数据报文
     * @param errorStatus
     * @param errorMessage
     * @return
     */
    public static Map<String,Object> packMap(String errorStatus,String errorMessage,List<Map<String,Object>> list,Map<String,Object> map,Object ... objects){
        Map<String,Object> pkmap = new HashMap<String,Object>();
        pkmap.put("errorStatus",errorStatus);
        pkmap.put("errorMessage",errorMessage);
        if(list != null && list.size() >= 0){
             pkmap.put("listData",list);
        }
        if(map != null && !map.isEmpty() && map.size() >= 0){
            pkmap.putAll(map);
        }
        return pkmap;
    }
    /**
     * 描述：封装返回数据报文
     * @return
     */
    public static Map<String,Object> sucPackMap(List<Map<String,Object>> list,Map<String,Object> map,Object ... objects){
        Map<String,Object> pkmap = new HashMap<String,Object>();
        if(list != null && list.size() >= 0){
            pkmap.put("listData",list);
        }
        if(map != null && !map.isEmpty() && map.size() >= 0){
            pkmap.putAll(map);
        }
        return pkmap;
    }

    /**
     * 描述：封装返回数据报文
     * @return
     */
    public static Map<String,Object> sucPackMapBean(List<?> list,Map<String,Object> map){
        Map<String,Object> pkmap = new HashMap<String,Object>();
        if(list != null && list.size() >= 0){
            pkmap.put("listData",list);
        }
        if(map != null && !map.isEmpty() && map.size() >= 0){
            pkmap.putAll(map);
        }
       
        return pkmap;
    }
/*----------------------------------------------------------临时方法，防止旅游报社报错，后续可能要去掉----------------------------------------------------------*/
    /**
     * 描述：返回报文通用方法
     * @param status
     * @param resultValueMap
     * @return
     */
    public static String reposeBody(String errcode,String errmsg,Map<String ,Object> resultValueMap) {
        Map<String,Object> returnMap = new HashMap<String, Object>();
        returnMap.put("status", errcode);
        if(StringUtils.isNoneBlank(errmsg)){
        	returnMap.put("errmsg", errmsg);
        }
        if(resultValueMap != null && resultValueMap.size() > 0){
        	returnMap.put("data", resultValueMap);
        }
        /**
         * WriteNullNumberAsZero	数值字段如果为null,输出为0,而非null
         * WriteMapNullValue	是否输出值为null的字段,默认为false
         */
        String json = JSONObject.toJSONString(returnMap,filter,SerializerFeature.WriteMapNullValue,
                                                        SerializerFeature.WriteNullListAsEmpty,
                                                        SerializerFeature.WriteNullStringAsEmpty,
                                                        SerializerFeature.WriteNullNumberAsZero,
                                                        SerializerFeature.WriteNullBooleanAsFalse); //转成JSON数据
        
//        logger.info("======reposeBody返回参数："+json);
        return json;
    }


    /**
     * 描述：返回报文通用方法
     * @param status
     * @param resultValueMap
     * @return
     */
    public static String reposeBodyOut(String status,Map<String ,Object> resultValueMap) {
        Map<String,Object> returnMap = new HashMap<String, Object>();
        returnMap.put("status", status);
        returnMap.put("data", "");
        if(resultValueMap!=null&&resultValueMap.size()>0){
        	returnMap.putAll(resultValueMap);
        }
        /**
         * WriteNullNumberAsZero	数值字段如果为null,输出为0,而非null
         * WriteMapNullValue	是否输出值为null的字段,默认为false
         */
        String json = JSONObject.toJSONString(returnMap,filter,SerializerFeature.WriteMapNullValue,
                                                        SerializerFeature.WriteNullListAsEmpty,
                                                        SerializerFeature.WriteNullStringAsEmpty,
                                                        SerializerFeature.WriteNullNumberAsZero,
                                                        SerializerFeature.WriteNullBooleanAsFalse); //转成JSON数据
        return json;
    }

 /**
     * 描述：返回报文通用方法
     * @param status
     * @param resultValueMap
     * @return
     */
    public static String reposeBody(String status, String errcode,String errmsg,Map<String ,Object> resultValueMap) {
        Map<String,Object> returnMap = new HashMap<String, Object>();
        returnMap.put("status", status);
        returnMap.put("errcode", errcode);
        if(StringUtils.isNoneBlank(errmsg)){
        	returnMap.put("errmsg", errmsg);
        }
        if(resultValueMap != null && resultValueMap.size() > 0){
        	returnMap.put("data", resultValueMap);
        }
        /**
         * WriteNullNumberAsZero	数值字段如果为null,输出为0,而非null
         * WriteMapNullValue	是否输出值为null的字段,默认为false
         */
        String json = JSONObject.toJSONString(returnMap,filter,SerializerFeature.WriteMapNullValue,
                                                        SerializerFeature.WriteNullListAsEmpty,
                                                        SerializerFeature.WriteNullStringAsEmpty,
                                                        SerializerFeature.WriteNullNumberAsZero,
                                                        SerializerFeature.WriteNullBooleanAsFalse); //转成JSON数据
        
//        logger.info("======reposeBody返回参数："+json);
        return json;
    }


}
