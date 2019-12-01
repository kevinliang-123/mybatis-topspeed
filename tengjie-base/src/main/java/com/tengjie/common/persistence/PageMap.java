package com.tengjie.common.persistence; 
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.tengjie.common.config.Global;
/**
 * Created by hzl on 2017/7/11.
 */
public class PageMap<E> implements Serializable  {

    protected Integer pageNo = 1; // 当前页码
    protected Integer pageSize = Integer.valueOf(Global.getConfig("page.pageSize")); // 页面大小，设置为“-1”表示不进行分页（分页无效）

    protected long count;// 总记录数，设置为“-1”表示不查询总数

    protected  Integer totalPage; //总页码
    private List<E> list = new ArrayList<E>();
	private boolean ifCount=true;//是否查询分业count数据，如果查询会执行select count(1) from sql

    public int getPageNo() {
        return pageNo;
    }

    public void setPageNo(Integer pageNo) {
        if(pageNo == null)return;
        if(pageNo > 0){
            this.pageNo = pageNo;
        }
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        if (pageSize == null)return;
        if(pageSize > 0){
            this.pageSize = pageSize;
        }
        if(pageSize < 0){
            this.pageSize = pageSize;
        }
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public List<E> getList() {
        return list;
    }

    public void setList(List<E> list) {
        this.list = list;
    }

    /**
     * 获取 Hibernate FirstResult
     */
    public int getFirstResult(){
        int firstResult = (getPageNo() - 1) * getPageSize();
        /*if (firstResult >= getCount() || firstResult<0) {
            firstResult = 0;
        }*/
        return firstResult;
    }
    /**
     * 获取 Hibernate MaxResults
     */
    public int getMaxResults(){
        return getPageSize();
    }

    public int getTotalPage() {
        return totalPage;
    }

    public void setTotalPage(Integer totalPage) {
        this.totalPage = totalPage;
    }

    public void init(){
        int count = (int)this.getCount();
        int pageSize = this.getPageSize();
        int total = count%pageSize == 0 ? count/pageSize : (count/pageSize)+1;
        this.setTotalPage(total);
    }

    public PageMap(Integer pageNo, Integer pageSize) {
        if (pageNo == null) {
            return;
        }
        if (pageNo > 0) {
            this.pageNo = pageNo;
        }
        if (pageSize == null) {
            return;
        }
        if (pageSize > 0) {
            this.pageSize = pageSize;
        }
    }

    public boolean getIfCount() {
		return ifCount;
	}

	public void setIfCount(boolean ifCount) {
		this.ifCount = ifCount;
	}

	public PageMap() {}
}
