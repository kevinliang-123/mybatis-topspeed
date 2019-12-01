package com.tengjie.common.utils;

import java.util.Properties;  

import javax.mail.Authenticator;  
import javax.mail.Message.RecipientType;  
import javax.mail.PasswordAuthentication;  
import javax.mail.Session;  
import javax.mail.Transport;  
import javax.mail.internet.InternetAddress;  
import javax.mail.internet.MimeMessage;  
  

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.mail.util.MailSSLSocketFactory;  
import com.tengjie.common.utils.PropertiesLoader; 
  
public class JavaMailUtils {  
	 //log日志  
    private static Logger logger = LoggerFactory.getLogger(JavaMailUtils.class);
    
    private static PropertiesLoader loader = new PropertiesLoader("mail.properties");
	private static String mailServer;
	private static String loginAccount;
	private static String loginAuthCode;

	
	
	 private static final  String FOMATE_TYPE_HTML="text/html;charset=utf-8";
	 private static final  String FOMATE_TYPE_TEXT="text/plain;charset=utf-8";
	 
	 
	 private  static final  String CONTENT_TYPE_HTML="HTML";
	 private static final  String CONTENT_TYPE_TEXT="TEXT";
	  
	
	 static {
		
		mailServer=loader.getProperty("mailServer");
		loginAccount=loader.getProperty("loginAccount");
		loginAuthCode=loader.getProperty("loginAuthCode");
		 
		
	 }
	
	
	public static int sendMailText(String[] reciverMails,String subject,String content){  
		    
		return sendEmail(reciverMails,subject,content,CONTENT_TYPE_TEXT);
	}
	
	public static int sendMailHTML(String[] reciverMails,String subject,String content){  
	    
		return sendEmail(reciverMails,subject,content,CONTENT_TYPE_HTML);
	}
	
	/** 
     * 发送邮件工具类:通过qq邮件发送,因为具有ssl加密,采用的是smtp协议 
     * @param mailServer 邮件服务器的主机名:如 "smtp.qq.com" 
     * @param loginAccount 登录邮箱的账号:如 "XXXX@qq.com" 
     * @param loginAuthCode 登录qq邮箱时候需要的授权码:可以进入qq邮箱,账号设置那里"生成授权码" 
     * @param sender 发件人 
     * @param recipients 收件人:支持群发 
     * @param emailSubject 邮件的主题 
     * @param emailContent 邮件的内容 
     * @param emailContentType 邮件内容的类型,支持纯文本:"text/plain;charset=utf-8";,带有Html格式的内容:"text/html;charset=utf-8"  
     * @return 
     */  
    private static int sendEmail(String[] recipients, String emailSubject,String emailContent,String contentType){            
       int res=0;  
          
       try {  
            //跟smtp服务器建立一个连接  
            Properties p = new Properties();  
            //设置邮件服务器主机名  
            p.setProperty("mail.host",mailServer);  
            //发送服务器需要身份验证,要采用指定用户名密码的方式去认证  
            p.setProperty("mail.smtp.auth", "true");  
            //发送邮件协议名称  
            p.setProperty("mail.transport.protocol", "smtp");  
  
            //开启SSL加密，否则会失败  
            MailSSLSocketFactory sf = new MailSSLSocketFactory();  
            sf.setTrustAllHosts(true);  
            p.put("mail.smtp.ssl.enable", "true");  
            p.put("mail.smtp.ssl.socketFactory", sf);  
  
            // 创建session  
            Session session = Session.getDefaultInstance(p, new Authenticator() {  
                protected PasswordAuthentication getPasswordAuthentication() {  
                     
                    PasswordAuthentication pa = new PasswordAuthentication(loginAccount,loginAuthCode);  
                    return pa;  
                }  
            });  
            session.setDebug(true);  
   
            MimeMessage msg = new MimeMessage(session);   
            msg.setFrom(new InternetAddress(loginAccount));  
               
              
            InternetAddress[] receptientsEmail=new InternetAddress[recipients.length];  
            for(int i=0;i<recipients.length;i++){  
                receptientsEmail[i]=new InternetAddress(recipients[i]);  
            }  
              
            //多个收件人  
            msg.setRecipients(RecipientType.TO,receptientsEmail);  
              
            //3邮件内容:主题、内容  
            msg.setSubject(emailSubject);  
            
            
            if("HTML".equals(contentType)){
            	msg.setContent(emailContent,FOMATE_TYPE_HTML);//发html格式的文本  
            }else{
            	msg.setContent(emailContent, FOMATE_TYPE_TEXT);//纯文本  
            } 
           
            Transport.send(msg);  
            logger.debug("邮件发送成功");  
            res=1;  
              
        }catch (Exception e) {  
        	 logger.debug("邮件发送失败: ",e);  
              
             res=0;  
        }      
        return res;  
    }  
	
	
	  
}  