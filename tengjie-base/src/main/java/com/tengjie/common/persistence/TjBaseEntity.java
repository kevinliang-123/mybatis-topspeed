package com.tengjie.common.persistence;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.annotation.XmlTransient;

import net.sf.cglib.beans.BeanMap;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.springframework.core.GenericTypeResolver;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tengjie.cglib.cust.BeanGenerator;
import com.tengjie.common.config.Global;
import com.tengjie.common.persistence.util.DbTablePrimaryKeyFieldConfig;
import com.tengjie.common.persistence.util.JspElement;
import com.tengjie.common.persistence.util.MybatisSqlGetTool;
import com.tengjie.common.persistence.util.RelationOperationTool;
import com.tengjie.common.utils.CustomArrayList;
import com.tengjie.common.utils.DateUtils;
import com.tengjie.common.utils.IdGen;
import com.tengjie.common.utils.MyBeanUtils;
import com.tengjie.common.utils.MyStringBuffer;
import com.tengjie.common.utils.NewInstanceUtil;
import com.tengjie.common.utils.Reflections;
import com.tengjie.common.utils.SpringContextHolder;
import com.tengjie.common.utils.StringUtils;
import com.tengjie.common.utils.TjMap;

/**
 * Entity支持类
 * @author
 * @version 2014-05-16
 */
//说明：QuestionLibrary$$BeanGeneratorByCGLIB$$4d55e793$$BeanGeneratorByCGLIB$$ff6cdf6a@1f72a0e,注意看这个类，
//这是调用了多次generateBean或者说initdynaMap，每次创建他都会继承上一个类，所以obj.getClass().getDeclaredFields()只能拿到最后一次的动态属性（注意，都不包括原生属性字段），
//obj.getClass().getSuperclass().getDeclaredFields()能拿到上一次的动态属性，再次obj.getClass().getSuperclass().getSuperclass().getDeclaredFields()
//才能够拿到QuestionLibrary的属性，因为getDeclaredFields本身就只是拿当前子类的属性。%%%
//class.getDeclaredFields()能获取所有属性（public、protected、default、private），但不包括父类属性，相对的class.getFields() 获取类的属性（public），包括父类,但是注意，只能是public的
/****************************************/
/**
 * 关于@JsonIgnoreProperties：这个属性是配置调用springboot的jackson（某些方法页面通过ajax调用，直接返回listbean或者map的时候，spring会自动转成json格式，
 * 比如：@ResponseBody
	public List<BannerConfig> callRelationType(BannerConfig bannerConfig... 直接返回的是一个listbean，这时spring会自动转换的）
 *但是由于BaseEntity继承自BeanGenerator，因此也会把BeanGenerator转换到json中，因此加@JsonIgnoreProperties的作用是转换的时候，忽略这些属性不转换成json
 *另外，还需要在springboot的启动java类中添加
 *  @Bean
    public Jackson2ObjectMapperBuilderCustomizer addCustomBigDecimalDeserialization(） 和jacksonObjectMapperBuilder.failOnEmptyBeans(false);
    否则转换json也会出问题，因为BeanGenerator无法序列化
 * 
 */
@JsonIgnoreProperties(value = {"classLoader","namingPolicy","strategy","useCache","mappingToExportExcel","mappingToExportExcel"})
public abstract class TjBaseEntity<T> extends BeanGenerator implements Serializable  {
	public final Class<T> entityGenericType;
	@SuppressWarnings("unchecked")
	public TjBaseEntity()
    {
		//this.genericType=(Class<T>)TypeToken.of(CrudService.class).resolveType(CrudService.class.getTypeParameters()[1]).getRawType();
		// Map<TypeVariable, Type> geneMap=GenericTypeResolver.getTypeVariableMap(getClass());此方法已经被禁用
		//GenericTypeResolver.resolveTypeArguments 和GenericTypeResolver.resolveTypeArgument，第一个是在这种有两个泛型时使用。
		entityGenericType=  (Class<T>)GenericTypeResolver.resolveTypeArgument(getClass(), TjBaseEntity.class);


    }
	private static final long serialVersionUID = 1L;

	/**
	 * 实体编号（唯一标识）
	 */
	protected String id;
	public static  String _id="id";	
	
	/**
	 * 当前实体分页对象
	 */
	@JsonIgnore
	protected Page<T> page;
	/**
	 * 当前页码
	 */
	protected Integer pageNo = 1;
	/**
	 * 当前行数
	 */
	protected Integer pageSize = 10;
	/**
	 * 自定义SQL（SQL标识，SQL内容）
	 */
	protected Map<String, String> sqlMap;
	
	protected String rowErrorInfo;//专门为导入时准备的存储每行记录导入错误的项，一般不会用
	/**
	 * 是否是新记录（默认：false），调用setIsNewRecord()设置新记录，使用自定义ID。
	 * 设置为true后强制执行插入语句，ID不会自动生成，需从手动传入。
	 */
	protected boolean isNewRecord = false;
	/**
	 * 操作类型 true:新增 false:修改
	 */
	protected boolean operationType = true;

	/**
	 * 修改原生mapper文件中select字段的别名，将某个字段key的别名修改为value
	 */
	protected  Map<String,String> modifyAlias =new  HashMap();
	/**
	 * 将mapper文件中select某个字段移除掉，不适用返回map形态的，因为返回map是由手工添加
	 */
	protected  Map<String,String> removeSelectField =new  HashMap();
	/**
	 * 为findbean类型添加一个自己定义的字段，不适用返回map形态的，因为返回map是由手工添加
	 */
	protected  Map<String,String> addSelectField =new  HashMap();
	/**
	 * 修改原生mapper文件中select字段的的名字，将某个字段key的名字修改为value，如a.name 修改为 format(...
	 */
	protected  Map<String,String> modifySelectField =new  HashMap();
	/**
	 * 修改原生mapper文件中where字段的的名字，将某个字段key的名字修改为value，如a.date > 修改为 date_format(...a.date
	 */
	protected  Map<String,String> modifyWhereField =new  HashMap();
	/**
	 *原生mapper文件中的select部分，其他都去除，值包含includeSelectField中内容，主要为接口部分使用，对应的转换成map结果集
	 *key为字段名，value为别名，value为空则与key相同
	 */
	protected  Map<String,String> includeSelectFieldMap =new  HashMap();
	/**
	 * 当调用findincludeSelectMap时，由于某些字段需要在回调中用到作为关联，所以必须包含进去，但是又不想显示，所以就放到这里，
	 * 转换成map结果集的时候，会把这些字段过滤掉，即select中有，但是显示的时候没有
	 */
	protected  Map<String,String> includeSelectFieldMapDotShow =new  HashMap();
	/**
	 * 清除原有写死在mapper里的orderBy字段
	 */
	protected boolean whetherClearFixOrderBy=false;
    
	/**
	 * 主表和子表都会附加orderby，这个字段是指定他俩谁在前面，目前只能做到这一步，否则太复杂的设置，就是交叉
	 * true表示主表在前，false表示子表在前，默认是true
	 */
	protected boolean childAndMainOrderBySort=true;
	
	protected boolean  appendDistinct =false;
	//默认是bean类型，当调用查询是findlistMap等，自动修改为2；
	protected String resultType="1" ;//1:bean 2map 
	//protected String appendWhereCondSql;
    protected StringBuffer appendWhereCondSql=new StringBuffer();//这个是在原有查询条件上附加的where sql，注意是只针对主表的，比如某字段 is not null；注意这是固定的where条件，比如a>1，a is null，不能够传递动态参数
    protected Map<String,String> orderyByMap=new LinkedHashMap<String, String>();//这个是在原有查询条件上附加的orderby sql，注意是只针对主表的,key为字段名，value为ASC或DESC，可以不填，默认为ASC
    /**以下为查询时候的程序控制leftjoin使用*/
    protected List<JoinTableBean> joinTableBeanList=new ArrayList() ;
     public void clearJoinTableBeanList(){
		if(joinTableBeanList!=null&&joinTableBeanList.size()>0){
			joinTableBeanList.clear();
		}
	}
     protected  Map<String,Object> updateWhereConditionMap=new HashMap();//针对updateByWhere时使用，在这里的属性名如果传入的bean中有值将会出现在where条件中
      	/**
 	 * 接口分页
 	 */
 	protected PageMap<Map<String, Object>> pageMap;
  	protected Map<String,String> whereLikeMap=new HashMap();//如果想要某个字段执行like操作，则将该字段名称放入这里,注意这里只针对主表
  	protected Map<String,String> whereInMap=new HashMap();//如果想要某个字段执行in操作，则将该字段名称放入这里,注意这里只针对主表
  	protected Map<String,String> relationOperMap=new TjMap();//关系运算符 大于、小于等，key为字段名，value为运算符（大于、小于、between等） 注意只针对Interger\Bigdecimal\Double\Short\Long\Float 
  	protected Map<String,Class>  mainTableAppendPropMap=new  HashMap();//主表的附加属性字段，注意，只在主表查询结果集中附加该属性集合，有时主表字段不满足需求，需要新加一个数据库不存在的字段，以便存储根据其他字段处理的结果！！
  	
  	protected List<String> groupBySelectField=new CustomArrayList();//groupby以后显示的count、max等字段,本list不用包含groupByField中的字段,会自动添加
  	protected List<String> groupByField=new CustomArrayList(true,groupBySelectField);//要groupby的字段列表

  	protected  Map<String,String> queryTimeStampMap=new HashMap();//日期部分使用，默认日期查询会都过滤掉时分秒，如果需要时分秒，则将该需要的字段放到这里
  	
      protected String commonInfoDesc;//补充所有描述信息，对于有些需要在某行上描述的例如导入错误、验证错误等信息，可以使用也可以不使用
      //key为当前查询中所有回调处理所用的别名
      protected Map<String,DataEntityFieldCallBackBean> fieldCallBackMap=new HashMap();
      //列回调有时效率低，此字段为行行回调函数
      protected DataEntityFieldCallBackBean rowBeanCallBack;
     //对于某个字段，在数据库update时无论是否为空均会进入set a=...
      protected Map<String,String> originValueCopyMap= new HashMap();
    
      protected List<String[]> orArray=new ArrayList();//注意，这个属性是将现有条件中的几个原来是and的条件合并起来变成加上括号和or，如 a=? and b=? 变为 (a=? or b=?)
      protected Map<String,String> notInOr=new HashMap();//是否包含在or条件中，有时查询条件中有重名的情况，而我又用重名的名字指定了or条件，导致都叫name的都会在or条件中，而我其实只想用其中的某个，
      //这是在这里指定则不会包含在or条件中，请注意，这个排除的key必须是表别名(动态表取别名)+字段，value可以为空
      protected List<String> onlyOr=new ArrayList();//注意，这个属性列表里的字段，只是将该字段之前的AND改为OR而已
      
      protected boolean clearSelectField=false;//清空select中的内容，注意只是清除findlistbean这种返回类型中在mapper文件中的，其他类型如groupbyselectfield、includeselectmap、findlistbean的关联表由于都是后续手工控制的，因此不在清除范围
      protected boolean ifRpc=false;//默认是false，调用initDynaMapForRpc时会变成true，以便后续使用
      protected Map<String,DynaPropertyBeanForRpc> dynaPropertyForRpcMap=new HashMap();
 
	public TjBaseEntity(String id) {
		this();
		this.id = id;
	}


	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	

	public Map<String, String> getOriginValueCopyMap() {
		return originValueCopyMap;
	}

	public void setOriginValueCopyMap(Map<String, String> originValueCopyMap) {
		this.originValueCopyMap = originValueCopyMap;
	}



	public boolean isAppendDistinct() {
		return appendDistinct;
	}

	public void setAppendDistinct(boolean appendDistinct) {
		this.appendDistinct = appendDistinct;
	}

	@JsonIgnore
	@XmlTransient
	public Page<T> getPage() {
		if (page == null){
			page = new Page<T>();
		}
		return page;
	}
	
	public Page<T> setPage(Page<T> page) {
		this.page = page;
		return page;
	}

	@JsonIgnore
	@XmlTransient
	public Map<String, String> getSqlMap() {
		if (sqlMap == null){
			sqlMap = Maps.newHashMap();
		}
		return sqlMap;
	}

	public void setSqlMap(Map<String, String> sqlMap) {
		this.sqlMap = sqlMap;
	}
	/**
	 * 插入之前执行方法，需要手动调用
	 */

	public void preInsert(){
		// 不限制ID为UUID，调用setIsNewRecord()使用自定义ID
		if (!this.isNewRecord){
			String priky=findPriKeyIdName();
			if(priky.toLowerCase().equals("id")) {
				setId(IdGen.uuid());
			}else {
				Reflections.setFieldValue(this, priky, IdGen.uuid());
			}
			
		}
		
	}
	/**
	 * 
	 * @param entity
	 * @return
	 */
	private String findPriKeyIdName() {
		return DbTablePrimaryKeyFieldConfig.findKeyField(this);
	}
	/**
	 * 更新之前执行方法，需要手动调用
	 */

	public void preUpdate(){

	}
	
    /**
	 * 是否是新记录（默认：false），调用setIsNewRecord()设置新记录，使用自定义ID。
	 * 设置为true后强制执行插入语句，ID不会自动生成，需从手动传入。
     * @return
     */
	public boolean getIsNewRecord() {
        return isNewRecord || StringUtils.isBlank(getIdValue());
    }
	/**
	 * 不能直接使用getId()获得id，因为有些表允许不使用id字段作为id，比如用sid
	 * @return
	 */
    private String getIdValue() {
    	String destId=DbTablePrimaryKeyFieldConfig.findKeyField(this);
		if(StringUtils.isNotEmpty(destId)) {
			if(destId.toLowerCase().equals("id")) {
				return getId();
			}else {
				Object temp=Reflections.getFieldValue(this, destId);
				return temp==null?null:temp.toString();
			}
		}
		return null;
    }
	/**
	 * 是否是新记录（默认：false），调用setIsNewRecord()设置新记录，使用自定义ID。
	 * 设置为true后强制执行插入语句，ID不会自动生成，需从手动传入。
	 */
	public void setIsNewRecord(boolean isNewRecord) {
		this.isNewRecord = isNewRecord;
	}

	public PageMap<Map<String, Object>> getPageMap() {
		return pageMap;
	}

	public void setPageMap(PageMap<Map<String, Object>> pageMap) {
		this.pageMap = pageMap;
	}

	/**
	 * 全局变量对象
	 */
	@JsonIgnore
	public Global getGlobal() {
		return Global.getInstance();
	}
	
	/**
	 * 获取数据库名称
	 */
	@JsonIgnore
	public String getDbName(){
		return Global.getConfig("jdbc.type");
	}
	
    @Override
    public boolean equals(Object obj) {
        if (null == obj) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!getClass().equals(obj.getClass())) {
            return false;
        }
        TjBaseEntity<?> that = (TjBaseEntity<?>) obj;
        return null == this.getIdValue() ? false : this.getIdValue().equals(that.getIdValue());
    }
    
    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
    public List<JoinTableBean> getJoinTableBeanList() {
		return joinTableBeanList;
	}

	public void setJoinTableBeanList(List<JoinTableBean> joinTableBeanList) {
		this.joinTableBeanList = joinTableBeanList;
	}
	/**
	 * 获得所有有用的属性，什么叫做有用的属性，当前entity的属性、动态属性、createDate等那几个dataEntity中的属性
	 */
   public Map<String,Class> findPureUsefulProp(){
	   Map<String,Class> propsMap=Maps.newHashMap();
	   Field[] fields=this.getClass().getDeclaredFields();
	   for(Field f:fields){
			 boolean isStatic = Modifier.isStatic(f.getModifiers());
			 if(!isStatic)
				 propsMap.put(f.getName(), f.getClass());
		}
	   propsMap.put("id",String.class);

	   try {
		   propsMap.putAll(Reflections.findAllDynaFieldForCglib(this));
	} catch (IllegalArgumentException | IllegalAccessException e) {
		e.printStackTrace();
	}
       return propsMap;
   }
	/** 
     * 属性map 
     */  
   private transient  BeanMap dynaBeanMap = null;  //注意，此dynaBeanMap不能生成set、get方法，否则拿不到，不知道为什么
   
   /**
    * 在处理user的时候，明明过来的属性里面看不到$porp_的属性内容，但是在BeanGenerator的props属性中确有，不知道为甚，如果不一致吧props中的移除掉，后面用一个其他类试试
    * 我发现这个情况应该是这样的，我之前写过一个错误的用法，如teacherInfo.initDynaMap(request);，但是没有teacherInfo=teacherInfo.initDynaMap(request);
    * 这样再来接收一下，导致的结果就是看teacherInfo确实是没有这个属性，但是props里面确实是有，所以应该必须显示的接收就没有上述问题了。
    * 上面这个说法是对的，但是另外对于user，由于获得user是用的User oldUser=systemService.getUser(orgInfo.getSysUserId());，这个实际是到缓存里面拿的，就有点类似这种没接收的了
    */
   private  void removePropsDynaProp(Map propertyMap){
		Field mm=Reflections.getAccessibleField(this, "props");
		try {
			Map temp=(Map) mm.get(this);
			   for (Iterator i = propertyMap.keySet().iterator(); i.hasNext();) {  
			       String key = (String) i.next(); 
			   	if(temp.containsKey(key)){
					temp.remove(key);
					}
			   }
		}catch(Exception e){
			e.printStackTrace();
		}
   }
   
	@SuppressWarnings("unchecked")  
    private TjBaseEntity generateBean(Map propertyMap) {  
	  if(JspElement.getEntityNameByClass(this.getClass(), false).equals("user")) {
		  removePropsDynaProp(propertyMap);//只对user表这样处理，具体参见removePropsDynaProp的方法说明
	  }
	  
      Set keySet = propertyMap.keySet();  
      for (Iterator i = keySet.iterator(); i.hasNext();) {  
       String key = (String) i.next(); 
       this.addProperty(key, (Class) propertyMap.get(key)); 
     
     //  setDynaValue(key, (Class) propertyMap.get(key));,
      }  
     return  (TjBaseEntity)create();  
    } 
	public T  initDynaMapForRpc(Object ...propName){
		ifRpc=true;
		if(propName==null||propName.length<0){
			return (T) this;
		}
		
	   TjMap propertyMap = new TjMap();
		     
	   for(Object prop:propName){
			if(prop instanceof Object[]){
				Object[] temp=(Object[])prop;
				 propertyMap.put(temp[0],temp[1]);
			}else{
				if(StringUtils.isNotEmpty(prop+""))
					 propertyMap.put(prop);
			}
			
		}
		savePropertyMapForRpc(propertyMap);
		return initDynaMap(propertyMap);


	}
	/***
	 * 初始化动态属性
	 * @param request
	 * @param propName:可变参数，要求必须为string或者Object[]
	 * @return
	 * 注意：当传入参数只有一个，且是一个数组时，不能直接放置，如pedd.initDynaMap(new Object[]{"endBrowseTime",Date.class})，这时可变参数会变成2个，错误
	 * 正确写法如下，应该把数组先定义；当多个参数时，即可直接放到里面了
	 *    Object addparam=new Object[]{"endBrowseTime",Date.class};，并且注意Object addparam，一定是定义为Object而不是Object[]数组
		   pedd.initDynaMap(addparam);
	 */
	public T  initDynaMap(Object ...propName){
		if(propName==null||propName.length<0){
			return (T) this;
		}
	   TjMap propertyMap = new TjMap();
		     
	   for(Object prop:propName){
			if(prop instanceof Object[]){
				Object[] temp=(Object[])prop;
				 propertyMap.put(temp[0],temp[1]);
			}else{
				if(StringUtils.isNotEmpty(prop+""))
					 propertyMap.put(prop);
			}
			
		}
		return initDynaMap(propertyMap);
	}
	/**
	 * 从request中获得全部属性，并组成map返回，注意，值包含动态属性字段，对于实体中固有的字段是不会获取的
	 * @param request
	 * @param allFieldCopy：allFieldCopy为true表示实体中存在的属性也进行值copy，false表示不对实体中存在属性进行值copy
	 * @return
	 */
	private TjMap findPropertyMapFromRequest(HttpServletRequest request,boolean ...allFieldCopyDyna) {
		Enumeration<String> et = request.getParameterNames();
		TjMap propertyMap =TjMap.newInstance();
		boolean allFieldCopy=false;
		if(allFieldCopyDyna.length>0)allFieldCopy=allFieldCopyDyna[0];
		while (et.hasMoreElements()) {
	       String name = et.nextElement();
	       if(Reflections.isContainField(this, name)) {
	    	   if(!allFieldCopy) continue;//从这里可以看出来，如果allFieldCopy为false，那么如果这个实体本身有这个name字段，那么是不会创建虚拟字段，后续也就不会进行值copy
	    	  
	       }
	       Class typeClass=String.class;
	       if(StringUtils.isNotEmpty(request.getParameter(name))){
	    	   String ss=request.getParameter(name);
	    	   if(DateUtils.isDateTypeStr(ss)){//判断是否是日期类型，否则在页面调用dateUtil.formateDate会报错，因为默认会初始化成字符串类型
	    		   typeClass=Date.class;
	    	   }
	       }
	       if(MyBeanUtils.isContainsArray(name)){//表示是数组类型
	    	   name=name.substring(0,name.indexOf("["));
	    	   typeClass=List.class;
	       }
	       if(propertyMap.containsKey(name))continue;
	       propertyMap.put(name,typeClass);
	    }  
		return propertyMap;
	}
	/**
	 * 将request中的在实体中不存在的属性动态创建，并将值copy到对应属性中，同时也会将实体中存在的属性的值copy过去
	 * 关于本方法的应用场景为，一个新增表单，里面包含了多张表的信息，主表中的字段如果在request中存在，那么会直接映射进去，
	 * 但是对于子表，由于原有的initDynaMap方法是不包含对存在值copy的，因此子表应该调用本方法；
	 * 如：
	 * public void saveAll(OrgInfo orgInfo,HttpServletRequest request)  {
		orgInfo=orgInfo.initDynaMap(request);
		User user=new User();
		user=(User) user.initDynaMap(request,true);
	 * @param request
	 * @return
	 */
	public T  initDynaMapAllField(HttpServletRequest request){
		TjMap propertyMap =findPropertyMapFromRequest(request,true);
		return initDynaMap(propertyMap,request);
	}
	
	/***
	 * 需要注意的是：第一次从左侧菜单点击进来的，是没有任何查询条件的，因此也就不会有任何动态属性
	 * 此方法可包含String[] 数组类型，初始化动态属性，并从request中值copy到动态属性所在bean中
	 * @param request
	 * 需要注意，propName是可变参数，如果传入的是只有一个数组，如new String[]{a,b}；实际是被才分成两个变量，只有多个参数时候才可以传入数组
	 * 应该把数组先定义；当多个参数时，即可直接放到里面了
	 *    Object addparam=new Object[]{"endBrowseTime",Date.class};，并且注意Object addparam，一定是定义为Object而不是Object[]数组
	 * @param propName:可变参数，要求必须为string或者string[],如果本参数为空，则表示将所有的request中的属性propName进行复制。
	 * @return
	 */
	public T  initDynaMap(HttpServletRequest request,Object ...propName){
		if(propName==null||propName.length<1){//表示将request中的全部在bean中不存在的属性进行copy，但是不包括文件部分，文件部分在baseController中拦截，但是注意：必须使用对应的model来接收，因为出现过页面的
			//modelAttribute="materialInfo" action="${ctx}/teachermaterialmodule/teacherMaterialModule/saveMaterialInfo",这个modelAttribute跟进入的controller对应的model根本不同，而我只是对controller的model进行文件copy
			TjMap propertyMap =findPropertyMapFromRequest(request);
			T dest=initDynaMap(propertyMap,request);
			return dest;
		}else{
			TjMap propertyMap = new TjMap();
			for(Object prop:propName){
				if(prop instanceof Object[]){
					Object[] temp=(Object[])prop;
					 propertyMap.put(temp[0],temp[1]);
				}else{
					if(StringUtils.isNotEmpty(prop+""))
						 propertyMap.put(prop);
				}
				
			}
			T dest=initDynaMap(propertyMap,request);
			return dest;
		}
	}
	public T  initDynaMapForRpc(Map propertyMap,HttpServletRequest request){
		savePropertyMapForRpc(propertyMap);
		ifRpc=true;
		return initDynaMap(propertyMap,request);
	}
	
	/**
	 *     在页面回传实际测试中不可能，虽然可以进入页面时识别附加属性，但是再次传回controller时，
	//动态代理bean被转换为标准bean，动态属性已经消失，传入的值实际在getparamter中，此时应该从paramter取值放入
	 * @param propertyMap ：属性map，key为属性名，value为属性对应的类型如Sting.class
	 * @param request
	 * @return
	 */
	public T  initDynaMap(Map propertyMap,HttpServletRequest request){
		 

//		 this.setSuperclass(this.getClass());
//		 BaseEntity  rr=generateBean(propertyMap);
//		 rr.dynaBeanMap = BeanMap.create(rr);
		 TjMap requestParam =findPropertyMapFromRequest(request);
		 requestParam.putAll(propertyMap);
		 TjBaseEntity  rr=(TjBaseEntity) initDynaMap(requestParam);
		 try {
			//这里必须要 rr=接收一下，因为copyRequestParamToCgiBean里面也会由于文件上传而新加属性，新加属性必须这样显示接收，否则地址都已经变了一个新对象
			 rr=(TjBaseEntity)MyBeanUtils.copyRequestParamToCgiBean(request,rr,requestParam);
		} catch (Exception e) {
			e.printStackTrace();
		}
	      return (T) rr;
	}
	/**
	 * 由于采用rpc时，动态属性无法序列化过去，因此保存在一个map中，后续到service端重新赋值出来动态属性
	 * @param propertyMap
	 */
	private void savePropertyMapForRpc(Map propertyMap){
		if(propertyMap!=null){
			Iterator entries = propertyMap.entrySet().iterator(); 
			while (entries.hasNext()) { 				
				Map.Entry entry = (Map.Entry) entries.next();  
				String key = entry.getKey().toString();
				Class value = (Class) entry.getValue();
				DynaPropertyBeanForRpc dynaPropertyForRpc =new DynaPropertyBeanForRpc();
				dynaPropertyForRpc.setFieldName(key);
				dynaPropertyForRpc.setFieldClassType(value);
				dynaPropertyForRpcMap.put(key, dynaPropertyForRpc);
			}
		}
	}
	/**
	 * 因为rpc无法传输动态属性；根据client端的存储在dynaPropertyForRpcMap中的动态属性，进行还原成动态属性
	 */
	public T restorePropertyMapForRpc(){
		Map<String,Class> propertyMap=new HashMap();
		 for (Map.Entry<String,DynaPropertyBeanForRpc> entry : dynaPropertyForRpcMap.entrySet()) {
			   String key=entry.getKey();
			   DynaPropertyBeanForRpc value=entry.getValue();
			   propertyMap.put(key, value.getFieldClassType());
		 }
		Object obj=initDynaMap(propertyMap);
		if(propertyMap.size()>0)
		 for (Map.Entry<String,DynaPropertyBeanForRpc> entry : dynaPropertyForRpcMap.entrySet()) {
			   String key=entry.getKey();
			   DynaPropertyBeanForRpc value=entry.getValue();
			   if(value.getFieldValue()!=null){
				   
			   }
			   Reflections.invokeMethodByName(obj, "setDynaValue", new Object[]{key,value.getFieldValue()});
			  // ((BaseEntity<T>)obj).setDynaValue(key,value.getFieldValue());
		 }
		 return (T) obj;
	}
	public T  initDynaMapForRpc(Map propertyMap){
		savePropertyMapForRpc(propertyMap);
		ifRpc=true;
		return initDynaMap(propertyMap);
	}
	/**
	 * 初始化动态属性，只是添加属性，不从request中copy值
	 * 重要注意：cglib中有一个属性叫做source，因此不能出现叫做source的动态属性，会创建不进去
	 * @param propertyMap：属性map，key为属性名，value为属性对应的类型如Sting.class
	 * @return
	 */
	public T  initDynaMap(Map destPropMap){
		Map propertyMap=Maps.newHashMap();
		MyBeanUtils.copyMap2Map(destPropMap, propertyMap);
		if(propertyMap!=null){
			Iterator entries = propertyMap.entrySet().iterator(); 
			//如果已经存在属性，则不需要再添加了，主要针对页面的查询条件二次进入;实际测试中不可能，虽然可以进入页面时识别附加属性，但是再次传回controller时，
			//动态代理bean被转换为标准bean，动态属性已经消失，传入的值实际在getparamter中，此时应该从paramter取值放入
			while (entries.hasNext()) { 				
				Map.Entry entry = (Map.Entry) entries.next();  
			    Object key = entry.getKey();  
			   // Object value = entry.getValue();
			    Field ff=Reflections.getAccessibleField(this, key+"");
			    if(ff!=null){
			    	entries.remove();
			    	//propertyMap.remove(key);
			    }
			  //已经有了得动态属性也不能添加，对于表单的form和save，我是在form和save中都添加了属性才醒，但是第二次点击编辑时，会说属性重复，不知道为啥，但是对于用户管理，因为user是继承了baseuser，不是直接继承dataentity
			    //可能跟这个也没关系，不知道为何，实际再次点击编辑的时候，已经没有了该属性，但是添加的时候就会报错说是属性重复
			    Field ffdyna=Reflections.getAccessibleField(this, "$cglib_prop_"+key+"");
			    if(ffdyna!=null){
			    	entries.remove();
			    	//propertyMap.remove(key);
			    }
			}
			if(propertyMap.size()<1){
				return (T) this;
			}
		}
		 this.setSuperclass(this.getClass());
		 TjBaseEntity  rr=generateBean(propertyMap);
		 BeanMap  be=BeanMap.create(rr);
		 //将classLoader设置为空，因为在dubbo rpc时会对classLoader序列化，进而要求jdk相关类全部序列化，实际没用
		 //只是设置为空还不行，实际后台面调用AbstractClassGenerator的getClassLoader时，还会有值，所以自定义了一个CustomClassLoader，当
		 //遇到CustomClassLoader时就无需再往下走了，直接返回空即可
		 //下面这行已经注释掉了，对于非rpc项目，两次initDynaMap时报错，找不到bean的类，原因是第一次的时候classloader被改变了
		// Reflections.invokeMethodByName(be.getBean(), "setClassLoader", new Object[]{new CustomClassLoader()});
	
		 rr.dynaBeanMap = be;//实际dynaBeanMap吧当前类中的所有属性以key+class的形式全部加载一遍
		
		
		 try {
			MyBeanUtils.copyBean2Bean(rr,this,new String[]{"dynaBeanMap","classLoader","isNewRecord"},null);
			rr.setIsNewRecord(this.isNewRecord);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	      return (T) rr;
	}
	
	/**
	 * 初始化完成后，可以增量添加属性
	 * @param propertyMap
	 * @return
	 */
	public T  appendDynaMap(Map propertyMap){
		
		 this.setSuperclass(this.getClass());
		 BeanMap oldBm=this.dynaBeanMap;
		 Iterator entries = oldBm.entrySet().iterator(); 
		 while (entries.hasNext()) { 	
			 Map.Entry entry = (Map.Entry) entries.next();  
			 Object key = entry.getKey();  
			 Object value = entry.getValue();
			 propertyMap.put(key, value);
		 }
		 TjBaseEntity  rr=generateBean(propertyMap);
		 rr.dynaBeanMap = BeanMap.create(rr);
		 try {
			MyBeanUtils.copyBean2Bean(rr,this,new String[]{"dynaBeanMap"},null);//在这列的copy，会将rr中的dynaBeanMap清空，如果想继续用dynaBeanMap，那么就应该有一个忽略的方法，不能copy dynaBeanMap
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	      return (T) rr;
	}
	
	 /** 
     * 给bean属性赋值 
     * @param property 属性名 
     * @param value 值 
     */  
   public void setDynaValue(String property, Object value) {  
	   if(ifRpc){
		   DynaPropertyBeanForRpc dynaPropertyBeanForRpc= dynaPropertyForRpcMap.get(property);
		   if(dynaPropertyBeanForRpc!=null){
			   dynaPropertyBeanForRpc.setFieldValue(value);
		   }
	   }
	   dynaBeanMap.put(property, value);  
   }  
 
   /** 
     * 通过属性名得到属性值 
     * @param property 属性名 
     * @return 值 
     */  
//   public Object getDynaValue(String property) {  
//     return dynaBeanMap.get(property);  
//   }  
   /** 
    *  !!!除非明确知道接收类的类型与动态类中的类型一致，否则不要使用本方法
    *  
    * 通过属性名得到属性值 ,本方法即将弃用，或尽量不要使用
    * 因为很多调用本方法时，数据是integer却用String来接收，会出现强制转换异常
    * 若需要可以调用getDynaStringValue、getDynaLongValue等来明确需要的返回类型，则会自动转换
    * 也可以调用getDynaValue(String property,Class<V> cls) 方法，明确指明类型
    * @param <E>
    * @param property 属性名 
    * @return 值 
    */  
   @Deprecated
  public <V>V getDynaValue(String property) {  

	if(!this.hasProperty(property))return null;
	Object obj= Reflections.invokeGetter(this, property);
	if(obj==null)return null;
    return (V)obj;  
  }  
   /** 
    *  !!!除非明确知道接收类的类型与动态类中的类型一致，否则不要使用本方法
    *  
    * 通过属性名得到属性值 ,本方法即将弃用，或尽量不要使用
    * 因为很多调用本方法时，数据是integer却用String来接收，会出现强制转换异常
    * 若需要可以调用getDynaStringValue、getDynaLongValue等来明确需要的返回类型，则会自动转换
    * 也可以调用getDynaValue(String property,Class<V> cls) 方法，明确指明类型
    * @param <E>
    * @param property 属性名 
    * @param defaultValue 属性不存在或属性值为null时的默认值
    * @return 值 
    */   
   @Deprecated
   public <V>V getDynaValue(String property,V defaultValue) {  

		V dest=getDynaValue(property);
		if(dest==null){
			   return defaultValue;
		}
	    return dest;  
}  
public <V>V getDynaValue(String property,Class<V> cls) { 
	   if(!this.hasProperty(property))return null;
		Object obj=  Reflections.invokeGetter(this, property);
		if(obj==null)return null;
		try {
			obj=MyBeanUtils.getDestTypeValue(obj, cls);
		} catch (ParseException e) {
		}
	    return (V)obj;  
}  
/** 
 * 通过属性名得到属性值 ，属性不存在或属性值为null时返回defaultValue
 * @param property 属性名 
 * @param defaultValue 属性不存在或属性值为null时的默认值
 * @return 值 
 */  
public <V>V getDynaValue(String property,Class<V> cls,V defaultValue) {  
	V dest=getDynaValue(property,cls);
    if(dest==null){
	   return defaultValue;
    }
    return dest;
} 
 /**
  * 得到list类型动态属性，注意一定是动态属性
  * 注意genericCls，一定是（map、bean）其他无法处理，如果是简单对象自己再程序中转换
  * 重要，本方法只是对动态可编辑表格，即form页面中动态添加表格，接收数据时候使用,但是其他的好像也能用
  * @param property:属性名称
  * @param cls:该list对应的泛型
  * @return
  */
  public <V>List<V> getDynaListValue(String property,Class genericCls) {  
	  //对于list类型的动态属性，从request中获得的时候，其为List<map>，因为添加动态属性时不知道其所含泛型是什么类型，可能是bean也可能是map或其他
	  //但是不支持list再嵌套list了
	  if(!Reflections.isContainFieldDyna(this, property))return null;
	  List<Object> objList = (List<Object>)Reflections.invokeGetter(this, property);
	  if( objList==null)return null;
	  List<V> newObj=Lists.newArrayList(); 
	  try {
		    for(Object temp:objList){
		    	Object destclss=NewInstanceUtil.newInstance(genericCls);
		    	if(temp instanceof Map){
		    		if(destclss instanceof Map){
		    			destclss=temp;
		    			newObj.add((V)destclss);
		    		}else{
		    			if(destclss instanceof TjBaseEntity) {
		    				Map<String,Class> addPropMap=findNeedDynaProp((Map)temp,genericCls);
		    				TjBaseEntity be=(TjBaseEntity)destclss;
		    				be=(TjBaseEntity) be.initDynaMap(addPropMap);
		    				MyBeanUtils.copyMap2Bean(be, (Map)temp);
		    				newObj.add((V)be);
		    			}else {
		    				MyBeanUtils.copyMap2Bean(destclss, (Map)temp);
		    				newObj.add((V)destclss);
		    			}
		    		}
		    	}else{//是bean
		    		if(destclss instanceof Map){
		    			MyBeanUtils.copyBean2Map((Map)destclss, temp);
		    		}else{
		    			MyBeanUtils.copyBean2Bean(destclss, temp);
		    		}
		    		newObj.add((V)destclss);
		    	}
		    	
		    }
		} catch (Exception e) {
			e.printStackTrace();
		}
	return (List<V>)newObj;  
  }  
  /**
   * 获得genericCls相对temp中不存在的属性
   */
  private  Map<String,Class> findNeedDynaProp(Map temp,Class genericCls){
	  Map<String,Class> addPropMap=new HashMap();
		Iterator entries = ( (Map)temp).entrySet().iterator(); 
		while (entries.hasNext()) { 
			Map.Entry entry = (Map.Entry) entries.next();
			String key=entry.getKey().toString();
			try {
				Field field=genericCls.getDeclaredField(key);
			} catch (NoSuchFieldException e) {
				addPropMap.put(key, entry.getValue().getClass());
			}
		}
		return addPropMap;
  }
   /** 
    * 通过属性名得到属性值 
    * @param property 属性名 
    * @return 值 
    */  
  public String getDynaStringValue(String property) {  
	  if(!this.hasProperty(property))return null;
	  Object obj = Reflections.invokeGetter(this, property);
	  if( obj==null)return null;
    return obj+"";  
  }  
  /** 
   * 通过属性名得到属性值 ，属性不存在或属性值为null时返回defaultValue
   * @param property 属性名 
   * @param defaultValue 属性不存在或属性值为null时的默认值
   * @return 值 
   */  
  public String getDynaStringValue(String property,String defaultValue) {  
	  String dest=getDynaStringValue(property);
      if(dest==null){
  	   return defaultValue;
      }
      return dest;
  } 
  /** 
   * 通过属性名得到属性值 
   * @param property 属性名 
   * @return 值 
   */  
 public Long getDynaLongValue(String property) {  
	 if(!this.hasProperty(property))return null;
	Object obj = Reflections.invokeGetter(this, property);
	if( obj==null)return null;
   return Long.valueOf(obj+"");  
 }  
 /** 
  * 通过属性名得到属性值 ，属性不存在或属性值为null时返回defaultValue
  * @param property 属性名 
  * @param defaultValue 属性不存在或属性值为null时的默认值
  * @return 值 
  */  
 public Long getDynaLongValue(String property,Long defaultValue) {  
	 Long dest=getDynaLongValue(property);
     if(dest==null){
 	   return defaultValue;
     }
     return dest;
 } 
  /** 
   * 通过属性名得到属性值 
   * @param property 属性名 
   * @return 值 
   */  
 public Integer getDynaIntegerValue(String property) {  
	 if(!this.hasProperty(property))return null;
	Object obj = Reflections.invokeGetter(this, property);
	  if( obj==null)return null;
   return Integer.valueOf(obj+"");  
 }  
 /** 
  * 通过属性名得到属性值 ，属性不存在或属性值为null时返回defaultValue
  * @param property 属性名 
  * @param defaultValue 属性不存在或属性值为null时的默认值
  * @return 值 
  */  
 public Integer getDynaIntegerValue(String property,Integer defaultValue) {  
	 Integer dest=getDynaIntegerValue(property);
     if(dest==null){
 	   return defaultValue;
     }
     return dest;
 } 
  /** 
   * 通过属性名得到属性值 
   * @param property 属性名 
   * @return 值 
   */  
 public Double getDynaDoubleValue(String property) {  
	 if(!this.hasProperty(property))return null;
	Object obj = Reflections.invokeGetter(this, property);
	  if( obj==null)return null;
   return Double.valueOf(obj+"");  
 }  
 /** 
  * 通过属性名得到属性值 ，属性不存在或属性值为null时返回defaultValue
  * @param property 属性名 
  * @param defaultValue 属性不存在或属性值为null时的默认值
  * @return 值 
  */  
 public Double getDynaDoubleValue(String property,Double defaultValue) {  
	 Double dest=getDynaDoubleValue(property);
     if(dest==null){
 	   return defaultValue;
     }
     return dest;
 } 
 /** 
  * 是否含有属性名,全部属性都可判断
  * 
  * @param property 属性名 
  * @return 值 
  */  
 public boolean hasProperty(String property) {  
	 if(dynaBeanMap!=null){
		  boolean dynaBeanMapContain= dynaBeanMap.containsKey(property);
		  if(dynaBeanMapContain) {
			  return true;
		  }else {
			  return  Reflections.isContainFieldDyna(this, property);
		  }
	 }else{
		  return Reflections.isContainFieldDyna(this, property);
//		    Field ffdyna=Reflections.getAccessibleField(this, "$cglib_prop_"+property+"");
//		    if(ffdyna!=null){
//		    	return true;
//		    }
	 }
 //	return false;
 	
 }  
 /**
  * 注意，这里查找jtb只能对正常的主表、子表进行查找，对于子表是子查询的无法查询，需要自行在程序中拿到JoinTableBeanList进行查找
  * 从已经添加过的joinTableBeanList中取回JoinTableBean，主要是为了有时是用smartjoin
  * 添加的jtb而无法获得，获得jtb可能需要添加或者remove一些属性
  * 如果这两个表有多个关联，那么只返回第一个找到的
  * @param mainTableName:如果获得的jtb是跟主表关联的，mainTableName可以为空,tbAppUser这种tb开头的，即真实数据库表名驼峰后的
  * @param childTableName：子表的名称,tbAppUser这种tb开头的，即真实数据库表名驼峰后的
  * @param mainTableRelFieldName：主表的关联条件字段（注意：只是关联条件，不是where条件。因为有时一个主表有多个字段会跟同一张表关联，单独依靠表名查找不能确定找哪个） 
  *        本字段可以为空，为空则就是按照关联的表名进行查找
  * @return
  */
 public JoinTableBean findJtbFromJoinTableBeanList(String mainTableName,String childTableName,String mainTablePointJoinFiled) {
	 String TB_TABLE_NAME=Reflections.getFieldValue(this, "_TB_TABLE_NAME_")+"";
	 if(StringUtils.isEmpty(mainTableName)) {
		 mainTableName=TB_TABLE_NAME;
	 }
	 for(JoinTableBean jtb:joinTableBeanList) {
		 String mtb=jtb.getMainTableName();
		 if(StringUtils.isEmpty(mtb)) {
			 mtb=TB_TABLE_NAME;
		 }else {//mtb有可能本身就是子表的别名，因此要过滤其所含的数字部分
			 mtb= StringUtils.findEnglishFromStr(mtb, false);
		 }
		 String ctb=jtb.getTableName();
		 if(mtb.toLowerCase().equals(mainTableName.toLowerCase())&&ctb.toLowerCase().equals(childTableName.toLowerCase())) {
			 if(StringUtils.isNotEmpty(mainTablePointJoinFiled)) {
				 List<JoinOnBean> jobList=jtb.getOnConditions();
				 for(JoinOnBean job: jobList) {//遍历关联条件
					 if(job.getMainFieldName()!=null&&job.getMainFieldName().equals(StringUtils.toUnderScoreCase(mainTablePointJoinFiled))) {
						 return jtb;
					 }
				 }
			 }else {
				 return jtb;
			 }
			 
		 }
	 }
	 return null;
 }
 /** 
  * 通过属性名得到属性值 
  * @param property 属性名 
  * @return 值 
  */  
public BigDecimal getDynaBigDecimalValue(String property) {  
	if(!this.hasProperty(property))return null;
	Object obj = Reflections.invokeGetter(this, property);
	  if( obj==null)return null;
  return BigDecimal.valueOf(Double.valueOf(obj+""));  
}  
/** 
 * 通过属性名得到属性值 ，属性不存在或属性值为null时返回defaultValue
 * @param property 属性名 
 * @param defaultValue 属性不存在或属性值为null时的默认值
 * @return 值 
 */  
public BigDecimal getDynaBigDecimalValue(String property,BigDecimal defaultValue) {  
	BigDecimal dest=getDynaBigDecimalValue(property);
    if(dest==null){
	   return defaultValue;
    }
    return dest;
} 
/** 
 * 通过属性名得到属性值 
 * @param property 属性名 
 * @return 值 
 */  
public Date getDynaDateValue(String property) {  
	if(!this.hasProperty(property))return null;
	  Object obj= Reflections.invokeGetter(this, property);
	  if(obj==null)return null;
     if(obj instanceof Date){
    	 return (Date)obj;
     }
 return DateUtils.parseDate(obj);  
}  
/** 
 * 通过属性名得到属性值 ，属性不存在或属性值为null时返回defaultValue
 * @param property 属性名 
 * @param defaultValue 属性不存在或属性值为null时的默认值
 * @return 值 
 */  
public Date getDynaDateValue(String property,Date defaultValue) {  
	Date dest=getDynaDateValue(property);
    if(dest==null){
	   return defaultValue;
    }
    return dest;
} 
	public String getResultType() {
	return resultType;
}
public void setResultType(String resultType) {
	this.resultType = resultType;
}


public Map<String, String> getOrderyByMap() {
	return orderyByMap;
}

public void setOrderyByMap(Map<String, String> orderyByMap) {
	this.orderyByMap = orderyByMap;
}

public void appendWhereCondSql(String sql){
	if(StringUtils.isNotEmpty(sql)){
		if(StringUtils.isNotEmpty(appendWhereCondSql.toString())){
			appendWhereCondSql.append(" and ");
		}
		appendWhereCondSql.append(sql);
	}
}

	


	public boolean isChildAndMainOrderBySort() {
	return childAndMainOrderBySort;
}

public void setChildAndMainOrderBySort(boolean childAndMainOrderBySort) {
	this.childAndMainOrderBySort = childAndMainOrderBySort;
}

	public StringBuffer getAppendWhereCondSql() {
	return appendWhereCondSql;
}


	public Map<String, String> getWhereLikeMap() {
	return whereLikeMap;
}

public void setWhereLikeMap(Map<String, String> whereLikeMap) {
	this.whereLikeMap = whereLikeMap;
}

public Map<String, String> getWhereInMap() {
	return whereInMap;
}

public void setWhereInMap(Map<String, String> whereInMap) {
	this.whereInMap = whereInMap;
}

public List<String> getGroupBySelectField() {
	return groupBySelectField;
}

public void setGroupBySelectField(List<String> groupBySelectField) {
	this.groupBySelectField = groupBySelectField;
}

public List<String> getGroupByField() {
	return groupByField;
}

public void setGroupByField(List<String> groupByField) {
	this.groupByField = groupByField;
}
//
//public Integer getGroupByCountField() {
//	return groupByCountField;
//}
//
//public void setGroupByCountField(Integer groupByCountField) {
//	this.groupByCountField = groupByCountField;
//}
//
//public Integer getGroupByMaxField() {
//	return groupByMaxField;
//}
//
//public void setGroupByMaxField(Integer groupByMaxField) {
//	this.groupByMaxField = groupByMaxField;
//}
//
//public Integer getGroupByMinField() {
//	return groupByMinField;
//}
//
//public void setGroupByMinField(Integer groupByMinField) {
//	this.groupByMinField = groupByMinField;
//}
//
//public Integer getGroupBySumField() {
//	return groupBySumField;
//}
//
//public void setGroupBySumField(Integer groupBySumField) {
//	this.groupBySumField = groupBySumField;
//}
//
//public Integer getGroupByAvgField() {
//	return groupByAvgField;
//}
//
//public void setGroupByAvgField(Integer groupByAvgField) {
//	this.groupByAvgField = groupByAvgField;
//}

public String getCommonInfoDesc() {
	return commonInfoDesc;
}

public void setCommonInfoDesc(String commonInfoDesc) {
	this.commonInfoDesc =(this.commonInfoDesc==null?"":this.commonInfoDesc)+"\n"+ commonInfoDesc;
}


public boolean isOperationType() {
	return operationType;
}

public void setOperationType(boolean operationType) {
	this.operationType = operationType;
}

	public Map<String, DataEntityFieldCallBackBean> getFieldCallBackMap() {
	return fieldCallBackMap;
}

public void setFieldCallBackMap(
		Map<String, DataEntityFieldCallBackBean> fieldCallBackMap) {
	this.fieldCallBackMap = fieldCallBackMap;
}


	public Map<String, Class> getMainTableAppendPropMap() {
	return mainTableAppendPropMap;
}

public void setMainTableAppendPropMap(Map<String, Class> mainTableAppendPropMap) {
	this.mainTableAppendPropMap = mainTableAppendPropMap;
}


	public Integer getPageNo() {
	return pageNo;
}

public void setPageNo(Integer pageNo) {
	this.pageNo = pageNo;
}

public Integer getPageSize() {
	return pageSize;
}

public void setPageSize(Integer pageSize) {
	this.pageSize = pageSize;
}


public boolean isWhetherClearFixOrderBy() {
	return whetherClearFixOrderBy;
}
/**
 * 是否清除原有mapper文件中的orderby信息
 * @param whetherClearFixOrderBy
 */
public void setWhetherClearFixOrderBy(boolean whetherClearFixOrderBy) {
	this.whetherClearFixOrderBy = whetherClearFixOrderBy;
}
/**
 * 清除原有mapper文件中的orderby信息
 * @param whetherClearFixOrderBy
 */
public void clearFixOrderBy() {
	this.whetherClearFixOrderBy = true;
}

public String getRowErrorInfo() {
	return rowErrorInfo;
}

public void setRowErrorInfo(String rowErrorInfo) {
	this.rowErrorInfo = rowErrorInfo;
}
public T putJoinTable(JoinTableBean jtb){
	for(JoinTableBean exist:joinTableBeanList){//这个是怕有的随机别名会存在重名的情况，因为随机数会存在相同的情况。
		if(exist.getTableAlias().equals(jtb.getTableAlias())){
			jtb.setTableAlias(jtb.getTableAlias()+"1");
		}
	}
	this.joinTableBeanList.add(jtb);
	return (T) this;
}
/**
 * 查找相同的jt并返回，若没有则返回null，这个是比较jtb里面的所有的mainTableName、tableName、List<JoinOnBean>来判断是否有重复的,
 * 注意List<JoinOnBean>只是用关联条件来匹配
 * @param newjtb:要比较的jtb
 * @param mainTablePointJoinFileds:指定要比较的关联字段的列表，若传入，则目标jtb必须也是只有这些关联字段（不能多也不能少，即完全相同），若不传，两个jtb的关联字段的数量、内容也必须完全相同
 * @return
 */
public JoinTableBean findExistSameJtb(JoinTableBean newjtb,String... mainTablePointJoinFileds) {
	JoinTableBean findDest=null;
	for(JoinTableBean oldjtb:joinTableBeanList){
		//oldjtb.getMainTableName(), newjtb.getMainTableName()如果是mainTableName本身又是某个子表做主表，可能带数字，这种应该不考虑？？？
		if(StringUtils.compareTwoStrEqual(oldjtb.getMainTableName(), newjtb.getMainTableName())) {//比较主表是否相同，都为空也算相同，表示是都跟主表关联而已
			if(StringUtils.compareTwoStrEqual(oldjtb.getTableName(), newjtb.getTableName())) {//比较被关联表是否相同
				//比较关联字段是否相同
				Map<String,String> oldOnMap=filterJoinOnBeanList(oldjtb.getOnConditions());
				Map<String,String> newOnMap= filterJoinOnBeanList(newjtb.getOnConditions());
				if(mainTablePointJoinFileds.length>0) {//如果指定了mainTablePointJoinFileds，说明只要newOnMap中主表字段是指定的这些来比较是否完全一致
					Map<String,String> mainTablePointJoinFiledMap=Maps.newHashMap();
					for(String temp:mainTablePointJoinFileds) {
						mainTablePointJoinFiledMap.put(StringUtils.toUnderScoreCase(temp),"");
					}
					Iterator<Map.Entry<String, String>> it = newOnMap.entrySet().iterator();  
					 while(it.hasNext()){  
				          Map.Entry<String, String> entry=it.next();  
				          if(!mainTablePointJoinFiledMap.containsKey(entry.getKey())) {//不在指定主表关联字段中的关联都删除，以便后续比对
				        	  it.remove();
				          }  
				     }  
				}
				boolean relationFieldIsSame=false;
				if(oldOnMap.size()!=newOnMap.size()) {
					continue;
				}
				for(Map.Entry<String, String> entry : oldOnMap.entrySet()){
				    String oldMainTableField = entry.getKey();
				    String oldChildFieldName = entry.getValue();
				    if(newOnMap.containsKey(oldMainTableField)&&StringUtils.compareTwoStrEqual(newOnMap.get(oldMainTableField),oldChildFieldName)) {
				    	relationFieldIsSame=true;
				    }else {
				    	relationFieldIsSame=false;
				    	break;
				    }
				}
				
				if(relationFieldIsSame) {
					findDest=oldjtb;
					break;
				}
			}
		} 
	}
	return findDest;
}
/**
 * 将List<JoinOnBean>中的每个JoinOnBean mainFieldName（为空的不需要，表示是查询条件）、childFieldName提取出来转成map
 *  private String mainFieldName;//主表的关联字段名，当主表字段名为空时，表示是on后面的查询条件，如果mainFieldName不为空，则说明是关联条件
    private String childFieldName;//字表的关联字段名
 * @param newOnlist
 * @return key为mainFieldName value为childFieldName
 */
private Map<String,String> filterJoinOnBeanList(List<JoinOnBean> newOnlist) {
	Map<String,String> resultMap=Maps.newHashMap();
	for(JoinOnBean job:newOnlist) {
		if(StringUtils.isNotEmpty(job.getMainFieldName())) {
			resultMap.put(job.getMainFieldName(), job.getChildFieldName());
		}
	}
	return resultMap;
}
/**
 * 为当前主表添加扩展属性字段,默认为字符串类型,即String.class
 * @param key：字段名称
 * @return
 */
public T putAppendField(String key){
	this.mainTableAppendPropMap.put(key, String.class);
	return (T) this;
}
/**
 * 为当前主表添加扩展属性字段,默认为字符串类型,即String.class
 * ，可以直接添加putfieldcallback，且无需再次输入字段名
 * @param key：字段名称
 * @return
 */
public AppendAutoPutCB<T> putAppendFieldS(String key){
	this.mainTableAppendPropMap.put(key, String.class);
	String callClassName=((StackTraceElement)new Throwable().getStackTrace()[1]).getClassName();
	AppendAutoPutCB<T> aapc=null;
	try {
		aapc = new AppendAutoPutCB<T>(this,SpringContextHolder.findCallBackClass(callClassName),key);
	} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
		e.printStackTrace();
	}
	return aapc;
}
/**
 * 为当前主表添加扩展属性字段，可以直接添加putfieldcallback，且无需再次输入字段名
 * @param key：字段名称
 * @param value：属性的类型如：String.class
 * @return
 */
public AppendAutoPutCB<T> putAppendFieldS(String key, Class value){
	this.mainTableAppendPropMap.put(key, value);
	String callClassName=((StackTraceElement)new Throwable().getStackTrace()[1]).getClassName();
	AppendAutoPutCB<T> aapc=new AppendAutoPutCB<T>(this,SpringContextHolder.getBean(callClassName),key);
	return aapc;
}
/**
 * 为当前主表添加扩展属性字段
 * @param key：字段名称
 * @param value：属性的类型如：String.class
 * @return
 */
public T putAppendField(String key, Class value){
	this.mainTableAppendPropMap.put(key, value);
	return (T) this;
}
/**
 * 为属性添加回调方法
 * @param key 字段名称
 * @param value DataEntityFieldCallBackBean对象
 * @return
 */
public T putFieldCallBack(String key, DataEntityFieldCallBackBean value){
	this.fieldCallBackMap.put(key, value);
	return (T) this;
}
/**
 *  快速回调 ,不需要写回调方法
 * @param key 字段名称
 * @param tableName 调用对象 实体bean的名称即可
 * @param queryPropName 该对象的属性名称作为查询条件 
 * @param resultPropName 要返回属性值的字段名称
 * @return
 */
public T putFieldQuickCallBack(String key, String tableName,String queryPropName,String resultPropName){
	this.fieldCallBackMap.put(key, 	new DataEntityFieldCallBackBean(true,tableName,resultPropName,queryPropName,""));
	return (T) this;
}
/**
 *  快速回调 ,不需要写回调方法
 * @param key 字段名称
 * @param tableName 调用对象 实体bean的名称即可
 * @param queryPropName 该对象的属性名称作为查询条件
 * @param valuePropName 值属性名称，可以是当前查询中任何一个其他查询字段的值，上面的方法是默认为当前字段
 * @param resultPropName 要返回属性值的字段名称
 * @return
 */
public T putFieldQuickCallBack(String key, String tableName,String queryPropName,String valuePropName,String resultPropName){
	this.fieldCallBackMap.put(key, 	new DataEntityFieldCallBackBean(true,tableName,resultPropName,queryPropName,valuePropName));
	return (T) this;
}
/**
 * 为属性添加回调方法，本方法要求回调函数就在调用类当中，且不可传递扩展参数
 * @param key 字段名称
 * @param currObj 调用对象，传递为this
 * @param methodName 具体的方法名
 * @return
 */
public T putFieldCallBack(String key, Object currObj, String methodName){
	this.fieldCallBackMap.put(key, 	new DataEntityFieldCallBackBean(currObj,methodName,null));
	return (T) this;
}
/**
 * 为属性添加回调方法，本方法要求回调函数就在调用类当中，可传递扩展参数
 * @param key 字段名称
 * @param currObj 调用对象，传递为this
 * @param methodName 具体的方法名
 * @return
 */
public T putFieldCallBack(String key, Object currObj, String methodName,Object[] params){
	this.fieldCallBackMap.put(key, 	new DataEntityFieldCallBackBean(currObj,methodName,params));
	return (T) this;
}
/**
 * 为属性添加回调方法，此方法为调用工具类时使用，需要全工具类名,此方法回调时不包含resultset，只是数据库该字段
 * 原值放到函数第一个参数，其他扩展参数随后
 * @param key 字段名称
 * @param className 调用的类名，若在spring环境中则首字母小写的类名即可，若不在，则需要全路径
 * @param methodName 具体的方法名
 * @param otherParams 其他扩展参数
 * @return
 */
public T putFieldToolCallBack(String key, String className, String methodName,Object ...otherParams){
	DataEntityFieldCallBackBean db=new DataEntityFieldCallBackBean(className,methodName,otherParams);
	db.setIfToolMethod(true);
	this.fieldCallBackMap.put(key, db);
	return (T) this;
}
/**
 * 为属性添加回调方法，不可传递扩展参数
 * @param key 字段名称
 * @param className 调用的类名，若在spring环境中则首字母小写的类名即可，若不在，则需要全路径
 * @param methodName 具体的方法名
 * @return
 */
public T putFieldCallBack(String key, String className, String methodName){
	this.fieldCallBackMap.put(key, new DataEntityFieldCallBackBean(className,methodName,null));
	return (T) this;
}
/**
 * *此map为排除部分，对于字段较多时采用排除方式
 * @param excludFieldName;可变参数，不包含部分,
 * @return
 */
public T putExcludeSelectField(String ...excludFieldName){
	Map<String,String> excludeMap=null;
	if(excludFieldName!=null&&excludFieldName.length>0){
		excludeMap=new HashMap();
		for(String fn:excludFieldName){
			excludeMap.put(fn,fn );
		}
	}
	includeSelectFieldMap.putAll(findIncludeSelectMapByExclude(excludeMap));
	return (T) this;
}
private Map<String,String> findIncludeSelectMapByExclude(Map<String,String> excludeMap){
	Field[] ff=((Class)this.getClass()).getDeclaredFields();
	Map<String,String> addMap=new HashMap();
	for(Field temp:ff){
		String name=temp.getName();
		if("serialVersionUID".equals(name)||name.startsWith("_"))continue;
		if(excludeMap==null||!excludeMap.containsKey(name)){
			addMap.put(StringUtils.toUnderScoreCase(name), name);
		}
	}
	if(excludeMap==null||!excludeMap.containsKey("id")){
		addMap.put("id","id");
	}
	return addMap;
}
/**
 * 接收多个select参数
 * *原生mapper文件中的select部分，select内容即为传入的参数，主要为接口部分使用，对应的转换成map结果集
 * @param fieldName;可变参数，别名默认也是fieldName
 * @return
 */
public T putIncludeSelectFields(String ...fieldName){
	if(fieldName!=null&&fieldName.length>0){
		for(String fn:fieldName){
			String temp=StringUtils.toUnderScoreCase(fn);
			includeSelectFieldMap.put(temp, "\""+fn+"\"");
		}
		
	}
	return (T) this;
}
/**
 * 本方法后续版本将会去掉，因为与现有的方法putAddSelectField(String key,String alias)会存在冲突，当只有两个参数的时候，不知是调用可变参数的还是带别名的
 * 改为调用名称为putIncludeSelectFields。表示多个字段
 * *原生mapper文件中的select部分，select内容即为传入的参数，主要为接口部分使用，对应的转换成map结果集
 * @param fieldName;可变参数，别名默认也是fieldName
 * @return
 */
@Deprecated
public T putIncludeSelectField(String ...fieldName){
	if(fieldName!=null&&fieldName.length>0){
		for(String fn:fieldName){
			String temp=StringUtils.toUnderScoreCase(fn);
			includeSelectFieldMap.put(temp, "\""+fn+"\"");
		}
		
	}
	return (T) this;
}
/**
 * 专门对应putIncludeSelectField，因为实际有时回调需要的字段，但是又不想出现在结果集中，只是回调时使用
 * @param key
 * @return
 */
public T putIncludeSelectFieldDontShow(String key){
	String temp=StringUtils.toUnderScoreCase(key);
	includeSelectFieldMapDotShow.put(key,temp);
	return (T) this;
}
public T putIncludeSelectFieldDontShow(String ...keys){
	for(String key:keys){
		String temp=StringUtils.toUnderScoreCase(key);
		includeSelectFieldMapDotShow.put(key,temp);
	}
	return (T) this;
}

/**
 *  为返回bean类型查询时添加一个自己定义的字段，但是要注意，这个字段必须是存在在整个查询中的某个表的，因为会拼接到select中，
 *  而putAppendField是在bean中虚构一个字段
 *  不适用返回map形态的，因为返回map是由手工添加
 *  注意：如果传入的是commentCount，则会形成comment_count as commentCount,
    也可以传入commentCount as bbb，一旦带有as，则会保持原样，因为一旦带有as，那么就会保持原样拼接，但是这种带as的形式最好调用putAddSelectField(String key,String alias)方法
 * @param key:字段名
 * @return
 */
public T putAddSelectField(String key){
	putAddSelectField(key,key);
	return (T) this;
}
/**
 *  为返回bean类型查询时添加一个自己定义的字段，但是要注意，这个字段必须是存在在整个查询中的某个表的，因为会拼接到select中，
 *  而putAppendField是在bean中虚构一个字段
 *  不适用结果集是返回map形态的，因为返回map是由手工添加
 * @param key:字段名
 * @return
 */
public T putAddSelectField(String key,String alias){
	String temp=StringUtils.toUnderScoreCase(key);
	addSelectField.put(temp, alias);
	return (T) this;
}
/**
 *  为返回bean类型查询时添加一个自己定义的字段，注意，这里的字段名和别名均不会做任何处理(不会toUnderScoreCase进行转换)，原样输出
 *  因为有些子查询已经在子表中转换成驼峰后字段了，这里不能再处理
 *  不适用结果集是返回map形态的，因为返回map是由手工添加
 *  但是要注意，这个字段必须是存在在整个查询中的某个表的，因为会拼接到select中，而putAppendField是在bean中虚构一个字段
 * @param key:字段名
 * @return
 */
public T putAddSelectFieldKeep(String key,String alias){
	addSelectField.put(key, alias);
	return (T) this;
}
/**
 *  为返回bean类型查询时将mapper文件中select某个字段移除掉，
 *  不适用结果集是返回map形态的，因为返回map是由手工添加
 * @param key:字段名
 * @return
 */
public T putRemoveSelectField(String key){
	String temp=StringUtils.toUnderScoreCase(key);
	removeSelectField.put(temp, temp);
	return (T) this;
}

/**
 * 原生mapper文件中的select部分，其他都去除，select内容即为传入的参数，主要为接口部分使用，对应的转换成map结果集
 * @param key:字段名
 * @param alias：字段别名
 * @return
 */
public T putIncludeSelectField(String key,String alias){
	String temp=StringUtils.toUnderScoreCase(key);
	includeSelectFieldMap.put(temp, StringUtils.isEmpty(alias)?key:alias);
	return (T) this;
}

/**
 * 将该字段变为 is null或者is not null
 * @param key：字段名称
 *  @param key： isnot为true时表示为is not null
 * @return
 */
public T putWhereISNULL(String key,boolean ...isnot){
	boolean nullornotnull=false;
	if(isnot!=null&&isnot.length>0&&isnot[0])nullornotnull=true;
	if(key.contains(".")){
		
	}else{
		key="a."+StringUtils.toUnderScoreCase(key);
	}

	if(nullornotnull){
		this.appendWhereCondSql(key+" is not null ");
	}else{
		this.appendWhereCondSql(key+" is  null ");
	}
	
	return (T) this;
}

/**
 * 将该字段作为like条件
 * @param key：字段名称
 * @return
 */
public T putWhereLike(String key){
	this.whereLikeMap.put(key,"");
	return (T) this;
}
/**
 * 将该字段作为like条件
 * @param key：字段名称
 * @return
 */
public void putWhereLikeMutil(String ...keys){
	for(String key:keys) {
		this.whereLikeMap.put(key,"");
	}
	
	
}

/**
 * 将该字段作为in条件
 * @param key：字段名称
 * @return
 */
public T putWhereIn(String key){
	this.whereInMap.put(key,RelationOperationTool.IN);
	return (T) this;
}
/**
 * 将该字段作为in条件
 * @param key：字段名称
 * @return
 */
public void putWhereInMutil(String ...keys){
	for(String key:keys) {
		this.whereInMap.put(key,RelationOperationTool.IN);
	}

}

/**
 * 将该字段作为in条件
 * @param key：字段名称
 * @param value：,RelationOperationTool.IN 或者,RelationOperationTool.NOT_IN
 * @return
 */
public T putWhereIn(String key,String inOrNotIn){

		try {
			if(inOrNotIn==null||(!inOrNotIn.equals(RelationOperationTool.IN)&&!inOrNotIn.equals(RelationOperationTool.NOT_IN))){
				throw new Exception("运算符不正确");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	this.whereInMap.put(key,inOrNotIn);
	return (T) this;
}
/**
 * 放置查询条件中替代=号的关系 运算符，如正常为where a.name=?其中的=号可以任意替换为！=、>=等等，
 * 注意：可以看到在mapper中的配置，对于字符串类型除了in和like，其他都在relationOperMap中配置，
 * 特别注意：对于数值类型，是没有in和like的，没有in是因为bean中为数值类型，无法传参到mapper中
 * @param key key为字段名，
 * @param value value为运算符（大于、小于、between等）,调用RelationOperationTool.BIG_EQUAL
 * @return
 */
public T putRelationOper(String key,String value){
	if(RelationOperationTool.IS_NULL.equals(value)){
		putWhereISNULL(key,false);
	}else if(RelationOperationTool.IS_NOT_NULL.equals(value)) {
		putWhereISNULL(key,true);
	}else if(RelationOperationTool.LIKE.equals(value)) {
		putWhereLike(key);
	}else {
		this.relationOperMap.put(key,value);
	}
	
	return (T) this;
}
/**
 * 设置完groupby以后，通常为要计算的字段，如count(a.age) as aaa,别名自行书写
 * @param key
 * @return
 */
public T putGroupBySelectField(String key){
	this.groupBySelectField.add(key);
	return (T) this;
}
/**
 * 设置分组条件，如group by user_name等
 * @param key 要分组的字段名userName，为驼峰后的写法，在本字段中出现的groupby字段，会自动添加到groupBySelectField 添加到select中。
 * 但是一旦字段带有格式化函数，必须用数据库字段名，而非驼峰字段名，并且当为函数时 ，不会将本字段添加到groupBySelectField
 * 比如：to_date(field_name)等等均不会添加到select字段中，a.fieldName 这种是会自动添加到select中的，别名会截取为fieldName
 * @return
 */ 
public T putGroupByField(String key){
	this.groupByField.add(key);
	return (T) this;
}
/**
 * 设置分组条件，如group by user_name等
 * @param key
 * @param ifaddToSelect:是否将groupBy的字段直接添加到select中，true:添加到select字段中 ，false：不添加
 * 需要注意的是：
 * 比如：to_date(field_name)等等均不会添加到select字段中，即使指定为true；a.fieldName 这种是如果指定为true会自动添加到select中的，别名会截取为fieldName
 * @return
 */
public T putGroupByField(String key,boolean ifaddToSelect){
	((CustomArrayList)this.groupByField).add(key,ifaddToSelect);
	return (T) this;
}
/**
 * 修改别名，将某个字段key的别名修改为value
 * 需要注意的是：如果用返回beanlist类型的查询，如果修改的别名在实体中不存在该字段，实际不会创建虚拟字段，会拿不到，
 * 这时可以使用putAddSelectField来添加 一个新查询字段，这个方法会创建虚拟字段
 * ，如果用map类型的实际就无所谓了
 * @param key 要修改的字段
 * @param value  ：要重新as的新别名
 * @return
 */
public T putModifyAlias(String key,String value){
	this.modifyAlias.put(key,value);
	return (T) this;
}

/**
 *  修改原生mapper文件中where字段的的名字，将某个字段key的名字修改为value，如a.date > 修改为 date_format(...a.date
 * @param key 要修改的字段
 * @param value  ：要重新as的新别名
 * @return
 */
public T putModifyWhereField(String key,String value){
	this.modifyWhereField.put(key,value);
	return (T) this;
}
/**
 * 修改原生mapper文件中select字段的的名字，将某个字段key的名字修改为value，如a.name 修改为 format(...
 * @param key 要修改的字段
 * @param value  ：修改后的内容，注意as部分还是保留原样的
 * @return
 */
public T putModifySelectField(String key,String value){
	this.modifySelectField.put(key,value);
	return (T) this;
}


/**
 * 将现有条件中的几个原来是and的条件合并起来变成加上括号和or，如 a=? and b=? 变为 (a=? or b=?)
 * @param ors：字段列表
 * @return
 */
public T putOrArray(String ...ors){
	List<String> temp=Lists.newArrayList();
	for(String or:ors){
		temp.add(or);
	}
	this.orArray.add(temp.toArray(new String[temp.size()]));
	return (T) this;
}
/**
 * 将查询条件where中该字段之前的AND改为OR而已
 * @param ors：字段列表
 * @return
 */
public T putOnlyOr(String ...ors){
	for(String or:ors){
		this.onlyOr.add(or);
	}
	return (T) this;
}

	public List<String[]> getOrArray() {
	return orArray;
}

public void setOrArray(List<String[]> orArray) {
	this.orArray = orArray;
}
//保存时,不允许重复的字段列表， List<String>为字段组
private List<List<String>> noRepeateFieldsListForSave=Lists.newArrayList();
private String noRepeateMessage=null;
/**
 * 新增时不允许重复的字段，可多个字段放入，每个字段都不能重复
 * @param fieldName
 */
public void noRepeatFieldForSaveFilter(String ...fieldName){
	for(String temp:fieldName){
		List<String> list=Lists.newArrayList();
		list.add(temp);
		noRepeateFieldsListForSave.add(list);
	}
	
}
/**
 * 新增时不允许重复的字段组，多个字段作为一个组合条件判断是否重复
 * @param fieldName
 */
public void noRepeatFieldForSaveFilterG(String ...fieldNameComposite){
	List<String> list=Lists.newArrayList();
	for(String temp:fieldNameComposite){
		list.add(temp);
	}
	noRepeateFieldsListForSave.add(list);
}
/**
 * 默认日期的查询条件均不带时分秒，若需要带时分秒，则需要将字段名放入本方法中
 * @param fieldName
 * @return
 */
public T putQueryTimeStampField(String fieldName){
	this.queryTimeStampMap.put(fieldName, fieldName);
	return (T) this;
}
/**
 * 必须调用service中的updateByWhere时生效，必须包含属性名和对应的值，如果为空则不会进入where条件中
 * @param fieldName：要出现在where条件的属性名 如userName;
 * @return
 */
public T putUpdateWhereField(String fieldName,Object value){
	this.updateWhereConditionMap.put(fieldName+"Uwc", value);
	return (T) this;
}
/**
 * 在调用update时，无论是否传入的值是空，
 * @param fieldName
 * @return
 */
public T putSetForUpdateAny(String fieldName){
	this.originValueCopyMap.put(fieldName, "");
	return (T) this;
}
	public List<String> getOnlyOr() {
	return onlyOr;
}

	/**
	 * 多个列回调时效率较低，此方法为行回调，当为返回结果集为bean时参数baseEntity，当返回结果集为map时参数为Map
	 * @param className：类名称，当为spring环境中bean时首字母小写的类名，当为工具类时使用全路径
	 * @param methodName：方法名
	 * @param otherParam：其他扩展参数（可变参数）
	 */
public void putRowCallBack(String className,String methodName,Object ...otherParam){
		this.rowBeanCallBack=new DataEntityFieldCallBackBean(className,methodName,otherParam);
}	
/**
 * 多个列回调时效率较低，此方法为行回调，当为返回结果集为bean时参数回调参数为baseEntity，当返回结果集为map时参数为Map
 * @param currObj：调用对象，传递为this
 * @param methodName：方法名
 * @param otherParam：其他扩展参数（可变参数）
 */
public void putRowCallBack(Object currObj,String methodName,Object ...otherParam){
	this.rowBeanCallBack=new DataEntityFieldCallBackBean(currObj,methodName,otherParam);
}	
public void setOnlyOn(List<String> onlyOr) {
	this.onlyOr = onlyOr;
}
/**
 * 不执行sql，只是获得mysql解析完成后的sql语句内容
 * @param methodName：findList、findGroupByList、update、insert等方法
 * @return
 */
public String findMybatisSql(String methodName){
	String sql=MybatisSqlGetTool.getNamespaceSql(this,methodName);
	return sql;
}
//专为支持完全手写sql方式，此方法放置sql语句
private String anySql;
/**
 * 专为支持完全手写sql方式，此方法设置该sql语句
 * @return
 */
public String getAnySql() {
	return anySql;
}
public void setAnySql(String anySql) {
	this.anySql = anySql;
}

public T putOrderBy(String key,String value){
	this.orderyByMap.put(key, value);
	return (T) this;
}





	public Map<String, String> getModifyAlias() {
	return modifyAlias;
}
public void setModifyAlias(Map<String, String> modifyAlias) {
	this.modifyAlias = modifyAlias;
}


	public boolean isClearSelectField() {
	return clearSelectField;
}
/**
 * 清空select中的字段内容，注意只是清除findlistbean这种返回类型中在mapper文件中的，
 * 其他类型如groupbyselectfield、includeselectmap、findlistbean的关联表由于都是后续手工控制的，
 * 因此不在清除范围
 */
public void setClearSelectField(boolean clearSelectField) {
	this.clearSelectField = clearSelectField;
}
	public Map<String, String> getIncludeSelectFieldMap() {
	return includeSelectFieldMap;
}
public void setIncludeSelectFieldMap(Map<String, String> includeSelectFieldMap) {
	this.includeSelectFieldMap = includeSelectFieldMap;
}
	public Map<String, String> getNotInOr() {
	return notInOr;
}
public void setNotInOr(Map<String, String> notInOr) {
	this.notInOr = notInOr;
}


	public Map<String, String> getQueryTimeStampMap() {
	return queryTimeStampMap;
}
public void setQueryTimeStampMap(Map<String, String> queryTimeStampMap) {
	this.queryTimeStampMap = queryTimeStampMap;
}


	public Map<String, Object> getUpdateWhereConditionMap() {
	return updateWhereConditionMap;
}
public void setUpdateWhereConditionMap(
		Map<String, Object> updateWhereConditionMap) {
	this.updateWhereConditionMap = updateWhereConditionMap;
}

	public Map<String, String> getModifySelectField() {
	return modifySelectField;
}
public void setModifySelectField(Map<String, String> modifySelectField) {
	this.modifySelectField = modifySelectField;
}
public Map<String, String> getModifyWhereField() {
	return modifyWhereField;
}
public void setModifyWhereField(Map<String, String> modifyWhereField) {
	this.modifyWhereField = modifyWhereField;
}

	public DataEntityFieldCallBackBean getRowBeanCallBack() {
	return rowBeanCallBack;
}
public void setRowBeanCallBack(DataEntityFieldCallBackBean rowBeanCallBack) {
	this.rowBeanCallBack = rowBeanCallBack;
}


	public Map<String, String> getIncludeSelectFieldMapDotShow() {
	return includeSelectFieldMapDotShow;
}
public void setIncludeSelectFieldMapDotShow(
		Map<String, String> includeSelectFieldMapDotShow) {
	this.includeSelectFieldMapDotShow = includeSelectFieldMapDotShow;
}

	public Map<String, DynaPropertyBeanForRpc> getDynaPropertyForRpcMap() {
	return dynaPropertyForRpcMap;
}
public void setDynaPropertyForRpcMap(
		Map<String, DynaPropertyBeanForRpc> dynaPropertyForRpcMap) {
	this.dynaPropertyForRpcMap = dynaPropertyForRpcMap;
}

	public boolean isIfRpc() {
	return ifRpc;
}
public void setIfRpc(boolean ifRpc) {
	this.ifRpc = ifRpc;
}

	public List<List<String>> getNoRepeateFieldsListForSave() {
	return noRepeateFieldsListForSave;
}
public void setNoRepeateFieldsListForSave(
		List<List<String>> noRepeateFieldsListForSave) {
	this.noRepeateFieldsListForSave = noRepeateFieldsListForSave;
}
public String getNoRepeateMessage() {
	return noRepeateMessage;
}
public void setNoRepeateMessage(String noRepeateMessage) {
	this.noRepeateMessage = noRepeateMessage;
}
	public Map<String, String> getRemoveSelectField() {
	return removeSelectField;
}
public void setRemoveSelectField(Map<String, String> removeSelectField) {
	this.removeSelectField = removeSelectField;
}
public Map<String, String> getAddSelectField() {
	return addSelectField;
}
public void setAddSelectField(Map<String, String> addSelectField) {
	this.addSelectField = addSelectField;
}


	public void setOnlyOr(List<String> onlyOr) {
	this.onlyOr = onlyOr;
}

	public void setAppendWhereCondSql(StringBuffer appendWhereCondSql) {
		this.appendWhereCondSql = appendWhereCondSql;
	}


	public Map<String, String> getRelationOperMap() {
		return relationOperMap;
	}


	public void setRelationOperMap(Map<String, String> relationOperMap) {
		this.relationOperMap = relationOperMap;
	}
	/** 搜索值 */
    private String searchValue;

    /** 创建者 */
    private String createBy;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新者 */
    private String updateBy;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /** 备注 */
    private String remark;

    /** 请求参数 */
    private Map<String, Object> params;

    public String getSearchValue()
    {
        return searchValue;
    }

    public void setSearchValue(String searchValue)
    {
        this.searchValue = searchValue;
    }

    public String getCreateBy()
    {
        return createBy;
    }

    public void setCreateBy(String createBy)
    {
        this.createBy = createBy;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public String getUpdateBy()
    {
        return updateBy;
    }

    public void setUpdateBy(String updateBy)
    {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime)
    {
        this.updateTime = updateTime;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }

    public Map<String, Object> getParams()
    {
        if (params == null)
        {
            params = new HashMap<>();
        }
        return params;
    }

    public void setParams(Map<String, Object> params)
    {
        this.params = params;
    }

}
