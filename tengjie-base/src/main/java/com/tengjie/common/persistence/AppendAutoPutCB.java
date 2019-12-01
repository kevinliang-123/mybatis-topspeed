package com.tengjie.common.persistence;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.tengjie.common.persistence.util.MybatisSqlGetTool;
import com.tengjie.common.utils.Reflections;

/**
 *  queryDataMappedFieldList.putAppendField("childList", List.class).putFieldCallBack("childList", this, "dealFindChildList");
 *  正常这样写比较麻烦，可以直接变为 queryDataMappedFieldList.putAppendField("childList", List.class).putFieldCallBack("dealFindChildList");
 * @author liangfeng
 *
 */
public class AppendAutoPutCB<T> {
    private TjBaseEntity<T> be;
    private String callBackFieldName;
    private Object currObj;
    public AppendAutoPutCB(TjBaseEntity<T> be, String callBackFieldName) {
		super();
		this.be = be;
		this.callBackFieldName = callBackFieldName;
	}

    public AppendAutoPutCB(TjBaseEntity<T> be, Object currObj,String callBackFieldName) {
		super();
		this.be = be;
		this.currObj = currObj;
		this.callBackFieldName = callBackFieldName;
	}
    /**
     * 添加处理该字段的回调
     * @param methodName
     * @return
     */
	public T putFieldCallBack(String methodName){
    	return be.putFieldCallBack(callBackFieldName, currObj, methodName);
    	
    }
	
	public TjBaseEntity<T> getBe() {
		return be;
	}
	public void setBe(TjBaseEntity<T> be) {
		this.be = be;
	}
	public String getCallBackFieldName() {
		return callBackFieldName;
	}
	public void setCallBackFieldName(String callBackFieldName) {
		this.callBackFieldName = callBackFieldName;
	}
    
}
