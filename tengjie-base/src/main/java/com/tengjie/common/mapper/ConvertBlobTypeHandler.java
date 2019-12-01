package com.tengjie.common.mapper;


import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;

/**
 * className:ConvertBlobTypeHandler
 * 
 * 自定义typehandler，解决mybatis存储blob字段后，出现乱码的问题
 * 配置mapper.xml：
 * <result  typeHandler="cn.ffcs.drive.common.util.ConvertBlobTypeHandler"/>
 * 
 * @author
 * @version 1.0.0
 * @date 2016-05-05 11:15:23
 * 
 */
@MappedJdbcTypes(JdbcType.BLOB)
public class ConvertBlobTypeHandler extends BaseTypeHandler<String> {//指定字符集  
    private static final String DEFAULT_CHARSET = "utf-8";  

@Override  
public void setNonNullParameter(PreparedStatement ps, int i,  
        String parameter, JdbcType jdbcType) throws SQLException {  
    ByteArrayInputStream bis;  
    try {  
        bis = new ByteArrayInputStream(parameter.getBytes(DEFAULT_CHARSET));  
    } catch (UnsupportedEncodingException e) {  
        throw new RuntimeException("Blob Encoding Error!");  
    }     
    ps.setBinaryStream(i, bis, parameter.length());  
}  

@Override  
public String getNullableResult(ResultSet rs, String columnName)  
        throws SQLException {  
    Blob blob = rs.getBlob(columnName);  
    byte[] returnValue = null;  
    if (null != blob) {  
        returnValue = blob.getBytes(1, (int) blob.length());  
    }  else{
    	return rs.getString(columnName);
    }
    try {  
        return new String(returnValue, DEFAULT_CHARSET);  
    } catch (UnsupportedEncodingException e) {  
        throw new RuntimeException("Blob Encoding Error!");  
    }  
}  

@Override  
public String getNullableResult(CallableStatement cs, int columnIndex)  
        throws SQLException {  
    Blob blob = cs.getBlob(columnIndex);  
    byte[] returnValue = null;  
    if (null != blob) {  
        returnValue = blob.getBytes(1, (int) blob.length());  
    }  
    try {  
        return new String(returnValue, DEFAULT_CHARSET);  
    } catch (UnsupportedEncodingException e) {  
        throw new RuntimeException("Blob Encoding Error!");  
    }  
}

@Override
public String getNullableResult(ResultSet arg0, int arg1) throws SQLException {
	// TODO Auto-generated method stub
	return null;
}  } 
