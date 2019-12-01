package com.tengjie.common.utils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
public class HttpUtil {
	
	public static final String SOAP_CONTENT_TYPE = "application/soap+xml";
	public static final String XML_CONTENT_TYPE = "text/xml";
	public static final String CHAR_SET = "UTF-8";
	public static final int CONNECTION_TIMEOUT = 3 * 1000;
	public static final int SOCKET_TIMEOUT = 20 * 1000;
	
	//专门为了打印本地 接收 和 发送 的成对请求日志 
	//private static final Logger ioLogger = Logger.getLogger("io");
	
	public static String postSoap(String url,String body){
		return post(url, body,ContentType.create(SOAP_CONTENT_TYPE, CHAR_SET));
	}
	
	public static String postXml(String url,String body){
		return post(url, body,ContentType.create(XML_CONTENT_TYPE, CHAR_SET));
	}
	public static String getXml(String url){
		return get(url, XML_CONTENT_TYPE, CHAR_SET);
	}
	public static String getXml(String url, String charset){
		return get(url, XML_CONTENT_TYPE, charset);
	}
	
	public static String post(String url){
		CloseableHttpClient client = HttpClients.createDefault();
		HttpPost post = getPost(url);
		return execute(client, post, CHAR_SET);
	}
	
	public static String get(String url){
		CloseableHttpClient client = HttpClients.createDefault();
		HttpGet get = getGet(url);
		return execute(client, get, CHAR_SET);
	}
	public static String get(String url,String CHAR_SET){
		CloseableHttpClient client = HttpClients.createDefault();
		HttpGet get = getGet(url);
		return execute(client, get, CHAR_SET);
	}
	
	public static String post(String url,String body,int timeOut){
		ContentType ct = ContentType.create(XML_CONTENT_TYPE, CHAR_SET);
		
		return post(url,body,ct,timeOut);
	}
	public static String post(String url,String body,String contentType){
		ContentType ct = ContentType.create(XML_CONTENT_TYPE, CHAR_SET);
		if(contentType != null){
			ct = ContentType.parse(contentType);
		}
		return post(url,body,ct);
	}
	public static String post(String url, Map<String, String> params){
		ContentType ct = ContentType.create(XML_CONTENT_TYPE, CHAR_SET);
		return post(url,params,ct);
	}
	public static String post(String url, Map<String, String> params,String contentType){
		ContentType ct = ContentType.create(XML_CONTENT_TYPE, CHAR_SET);
		if(contentType != null){
			ct = ContentType.parse(contentType);
		}
		return post(url,params,ct);
	}
	private static String get(String url,String contentType,String charset){
		CloseableHttpClient client = HttpClients.createDefault();
		HttpGet get = getGet(url);
		//ioLogger.info("request-get:" + url);
		String result = execute(client, get, charset);
		//ioLogger.info("response-get:" + result);
		return result;
	}
	private static String post(String url,String body,ContentType contentType){
		CloseableHttpClient client = HttpClients.createDefault();
		HttpPost post = getPost(url);
		StringEntity se = new StringEntity(body,contentType);
		post.setEntity(se);
		//ioLogger.info("request-post:" + body);
		String result = execute(client, post, contentType.getCharset().displayName());
		//ioLogger.info("response-post:" + result);
		return result;
	}
	private static String post(String url,String body,ContentType contentType,int timeout){
		CloseableHttpClient client = HttpClients.createDefault();
		HttpPost post = getPost(url,timeout);
		StringEntity se = new StringEntity(body,contentType);
		post.setEntity(se);
		//ioLogger.info("request-post:" + body);
		String result = execute(client, post, contentType.getCharset().displayName());
		//ioLogger.info("response-post:" + result);
		return result;
	}
	
	private static String post(String url, Map<String, String> params,ContentType contentType){
		CloseableHttpClient client = HttpClients.createDefault();
		HttpPost post = getPost(url);
		
		 if (params != null) { 
			 List<NameValuePair> nvps = new ArrayList<NameValuePair>();  
        
             for (Map.Entry<String, String> entry : params.entrySet()) {
         
                 nvps.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));  
               
             } 
         	try {
				post.setEntity(new UrlEncodedFormEntity(nvps,"utf-8"));//这里修改了一下加了,"utf-8"，否则会中文乱码
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
     } 
	
		//ioLogger.info("request-post:" + body);
		String result = execute(client, post, contentType.getCharset().displayName());
		//ioLogger.info("response-post:" + result);
		return result;
	}
	private static HttpGet getGet(String url){
		HttpGet get = new HttpGet(url);
		RequestConfig config = RequestConfig.custom()
				.setConnectTimeout(CONNECTION_TIMEOUT)
				.setSocketTimeout(SOCKET_TIMEOUT)
				.build();
		get.setConfig(config);
		return get;
	}
	private static HttpPost getPost(String url){
		HttpPost post = new HttpPost(url);
		RequestConfig config = RequestConfig.custom()
				.setConnectTimeout(CONNECTION_TIMEOUT)
				.setSocketTimeout(SOCKET_TIMEOUT)
				.build();
		post.setConfig(config);
		return post;
	}
	private static HttpPost getPost(String url,int timeout){
		HttpPost post = new HttpPost(url);
		RequestConfig config = RequestConfig.custom()
				.setConnectTimeout(timeout)
				.setSocketTimeout(timeout)
				.build();
		post.setConfig(config);
		return post;
	}
    private static String execute(CloseableHttpClient client,HttpUriRequest request,String charset){
		CloseableHttpResponse response = null;
		try {
			response = client.execute(request);
			if(response.getStatusLine().getStatusCode() == 200){
				return consume(response.getEntity(),charset);
			}else{
				return null;
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}finally{
			try {
				if(response != null){
					response.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
				response = null;
			}
		}
	}
	private static String consume(HttpEntity entity,String charset){
		StringBuilder sb = new StringBuilder();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(entity.getContent(), charset));
			String line = null;
			while((line = reader.readLine())!= null){
				sb.append(line + "\n");
			}
			return sb.toString();
		} catch (UnsupportedOperationException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}finally{
			if(reader != null){
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
					reader = null;
				}
			}
		}
	}
	
	
	public static void main(String[] args) {
		String url = "http://101.200.217.149:8360/unicomAync/queryBizOrder.do";
		String str = post(url);
		System.out.println(str);
	}
	
}
