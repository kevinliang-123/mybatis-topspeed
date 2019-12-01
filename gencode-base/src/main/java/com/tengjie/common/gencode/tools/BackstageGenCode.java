package com.tengjie.common.gencode.tools;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.tengjie.common.gencode.GenUtils;
import com.tengjie.common.gencode.vo.TableColumn;
import com.tengjie.common.gencode.vo.TableMetadata;
import com.tengjie.common.persistence.ConditionBean;
import com.tengjie.common.persistence.JoinTableBean;

import com.tengjie.common.utils.ListUtils;
import com.tengjie.common.utils.MapUtils;
import com.tengjie.common.utils.MyStringBuffer;
import com.tengjie.common.utils.Reflections;
import com.tengjie.common.utils.StringUtils;

/**
 * 生成代码部分
 * @author liangfeng
 *
 */

public class BackstageGenCode extends InterfaceAndBackstageFactory {
	
	
	
    /**
     * 根据 业务表生成动态共用查询部分,busiinterId实际就是tb_management_backstage表的id
     * @throws Exception 
     */
    public String genDynaQuery(List<TableColumn> queryAreaList,List<TableColumn> listDataShowList,String mainTableName) throws Exception{
    	MyStringBuffer sb=new MyStringBuffer();
    
    	TableMetadata actualTm=GenUtils.buildTableMetadataBydbTableName(mainTableName);

		sb.append(findControllerMethodDefine_code(actualTm,"genDynaQuery","HttpServletRequest request"),"{");
		sb.insertBlankLine();
	//	sb.appendK(actualTm.getTableNameFirstUp(false),"=",actualTm.getTableNameFirstUp(false),".initDynaMap(request);");
		String paramName=actualTm.getTableNameFirstUp(false);//主变量名称
		sb.append(paramName+"="+paramName+".initDynaMap(request);"+HUANHANG);
		sb.insertBlankLine();
		
		//处理显示列表区
	
		
		sb.appendK("return ",paramName,";");
		sb.append("}");
		sb.insertBlankLine();

		return sb.toString();
    }


    /**
     * 根据业务表id生成controller，只有busiinterId为空的时候，说明是调用的fastgenMapperAndBean执行，这时需要projectId\mainTableName不为空
     * 否则如果busiinterId不为空，则实际不需要projectId\mainTableName
     * @param busiinterId：tb_management_backstage表的id
     * @throws Exception 
     */
    public String genListController(String mainTableName) throws Exception{
    	String tbTableName=StringUtils.toCamelCase(mainTableName);
    	List<TableColumn> queryAreaList=MapUtils.mapToList(DBTool.getAllTableField().get(tbTableName), false);
		List<TableColumn> outPutList=MapUtils.mapToList(DBTool.getAllTableField().get(tbTableName), false);
    	
		return genDynaQuery(queryAreaList,outPutList, mainTableName);
    }



}
