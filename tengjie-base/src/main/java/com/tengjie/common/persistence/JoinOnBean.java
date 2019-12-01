package com.tengjie.common.persistence;

import java.io.Serializable;
import java.util.Date;

import com.tengjie.common.utils.DateUtils;
import com.tengjie.common.utils.Reflections;
import com.tengjie.common.utils.StringUtils;


public class JoinOnBean extends ConditionBean implements Serializable  {
	private static final long serialVersionUID = 1L;
    private String mainFieldName;//主表的关联字段名，当主表字段名为空时，表示是on后面的查询条件，如果mainFieldName不为空，则说明是关联条件
    private String childFieldName;//字表的关联字段名
    private Object onValue1;
    private Object onValue2;//只有between时才会 用到onValue2
    private boolean keepIt;//保持原样，当keepIt=true时，表示保持原来，不加别名不做任何转换，如 to_days(tbdailyplan84.production_fill_time) = to_days(NOW())
	private String onSql;//与上面对应，当上面为true时，则直接在on后面拼接这个sql
    private String andOr="and";
	public JoinOnBean(String mainFieldName, String childFieldName) {
		super();
		this.mainFieldName = StringUtils.toUnderScoreCase(mainFieldName);
		this.childFieldName = StringUtils.toUnderScoreCase(childFieldName);
	}
	public JoinOnBean(String mainFieldName, String childFieldName,String sign) {
		super();
		this.mainFieldName = StringUtils.toUnderScoreCase(mainFieldName);
		this.childFieldName = StringUtils.toUnderScoreCase(childFieldName);
		this.sign=sign;
	}
	
	public JoinOnBean(String mainFieldName, String childFieldName,
			Object onValue1,String sign) {
		super();
		this.mainFieldName = StringUtils.toUnderScoreCase(mainFieldName);
		this.childFieldName = StringUtils.toUnderScoreCase(childFieldName);
		this.onValue1 = onValue1;
		this.sign=sign;
	}
	public JoinOnBean(String mainFieldName, String childFieldName,
			Object onValue1, Object onValue2,String sign) {
		super();
		this.mainFieldName = StringUtils.toUnderScoreCase(mainFieldName);
		this.childFieldName = StringUtils.toUnderScoreCase(childFieldName);
		this.onValue1 = onValue1;
		this.onValue2 = onValue2;
		this.sign=sign;
	}
	public Object getOnValue1() {
		return onValue1;
	}
	public void setOnValue1(Object onValue1) {
		this.onValue1 = onValue1;
	}
	public Object getOnValue2() {
		return onValue2;
	}
	public void setOnValue2(Object onValue2) {
		this.onValue2 = onValue2;
	}
	public JoinOnBean() {
		super();
		// TODO Auto-generated constructor stub
	}
	public String getMainFieldName() {
		return mainFieldName;
	}
	public void setMainFieldName(String mainFieldName) {
		this.mainFieldName =StringUtils.toUnderScoreCase(mainFieldName);
	}
	public String getChildFieldName() {
		return childFieldName;
	}
	public void setChildFieldName(String childFieldName) {
		this.childFieldName = StringUtils.toUnderScoreCase(childFieldName);
	}

   public String findExpress(String mainTableAlias,String childTableAlias,JoinTableBean jtb,Object parameter) throws Exception{
	   StringBuilder sb=new StringBuilder();
	   if(StringUtils.isEmpty(childTableAlias)){
		   throw new Exception("子表别名不能为空！");
	   }
	   if(this.keepIt){//保持原样，不解析
		   return this.getOnSql();
	   }
	   
       if(StringUtils.isEmpty(mainFieldName)){//是查询条件
    	   if(this.onValue1==null||StringUtils.isEmpty(this.onValue1+"")) {
    		   String aliasKey=jtb.getSelectMap().get(StringUtils.toCamelCase(childFieldName));//aliasKey是别名
    		   //aliasKey不空说明在bean中存在，说明bean中也会有这个值,aliasKey是别名,可以自动进行获取值并复制
    		   if(StringUtils.isNotEmpty(aliasKey)) {
        		   this.onValue1=Reflections.invokeGetter(parameter,aliasKey);
        		   if(StringUtils.isEmpty(this.onValue1+"")) this.onValue1=null;//怕获得空字符串，再判断一下
    		   }
    	   }
    	   if(this.onValue2==null||StringUtils.isEmpty(this.onValue2+"")) {
    		   String aliasKey=jtb.getSelectMap().get("end"+StringUtils.firstToUpper(StringUtils.toCamelCase(childFieldName)));//aliasKey是别名
    		   //aliasKey不空说明在bean中存在，说明bean中也会有这个值,aliasKey是别名,可以自动进行获取值并复制
    		   if(StringUtils.isNotEmpty(aliasKey)) {
    			   this.onValue2=Reflections.invokeGetter(parameter,aliasKey);
        		   if(StringUtils.isEmpty(this.onValue2+"")) this.onValue2=null;//怕获得空字符串，再判断一下 
    		   }
    		  
    	   }
    	   
    	 
    	   if(sign.equals(this.SIGN_BETWEEN)){
    		   if(this.onValue1==null&&this.onValue2==null)return "";
    		   if(this.onValue1 instanceof Date)this.onValue1=formatDate(this.onValue1);
    		   if(this.onValue2 instanceof Date)this.onValue2=formatDate(this.onValue2);
    		   if(this.onValue1!=null&&this.onValue2!=null) {
    			   sb.append(childTableAlias+"."+childFieldName+" "+sign+this.onValue1+" and "+this.onValue2);
    		   }else  if(this.onValue1!=null) {
    			   sb.append(childTableAlias+"."+childFieldName+" "+this.SIGN_BIG_EQUAL+this.onValue1);
    		   }else {
    			   sb.append(childTableAlias+"."+childFieldName+" "+this.SIGN_SMALL_EQUAL+this.onValue2);
    		   }
    		   
    	   }else {
    		   if(this.onValue1==null)return "";
    		   
    		   if(sign.equals(this.SIGN_LIKE)){
    	    	   sb.append(childTableAlias+"."+childFieldName+" "+sign+" '%"+this.onValue1+"%'");
    	       }else{
    	    	   if(this.onValue1 instanceof  String){
    	    		   sb.append(childTableAlias+"."+childFieldName+" "+sign+" '"+this.onValue1+"'");
    	    	   }else if(this.onValue1 instanceof  Date){
    	    		   sb.append(childTableAlias+"."+childFieldName+" "+sign+" "+formatDate(this.onValue1)+"");
    	    	   }else{
    	    		   sb.append(childTableAlias+"."+childFieldName+" "+sign+this.onValue1);
    	    	   }
    	    	   
    	       }
    	   } 
		}else{//是关联条件
			   if(StringUtils.isNotEmpty(mainTableAlias)){
				   if(mainFieldName.contains(".")){//对于从mapper中出来的sql，如果多个子查询，需要手工指定别名，即如果mainFieldName含有.，则视为手工指定
					   sb.append(mainFieldName+" "+sign+" "+childTableAlias+"."+childFieldName);
				   }else{
					   sb.append(mainTableAlias+"."+mainFieldName+" "+sign+" "+childTableAlias+"."+childFieldName);
				   }
				 
			   }else{
				   sb.append(mainFieldName+" "+sign+" "+childTableAlias+"."+childFieldName);
			   }
			 
		}
	
	   return sb.toString();
   }
   private String formatDate(Object date) {
   	String result=date+"";
   	if(date instanceof Date) {
   		result="'" + DateUtils.formatDate((Date)date, "yyyy-MM-dd")+" 00:00:00" + "'";
   	}
   	return result;
   }
public boolean isKeepIt() {
	return keepIt;
}
public void setKeepIt(boolean keepIt) {
	this.keepIt = keepIt;
}
public String getOnSql() {
	return onSql;
}
public void setOnSql(String onSql) {
	this.onSql = onSql;
}
public String getAndOr() {
	return andOr;
}
public void setAndOr(String andOr) {
	this.andOr = andOr;
}
   
}
