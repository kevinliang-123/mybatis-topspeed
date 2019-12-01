1、	引入我们的jar包。这里我们用的是druid的1.11.14，是因为该项目用的是这个版本 ，也可以用更老的版本    

2、	需要在mybatis-config.xml中增加如下几个拦截器：

    <plugins>
	
	
    <plugin interceptor="com.tengjie.common.persistence.interceptor.PaginationInterceptor"/>分页拦截器
    
    <plugin interceptor="com.tengjie.common.persistence.interceptor.MapperModifyPlugin"/>动态sql拦截器
   
    <plugin interceptor="com.tengjie.common.persistence.interceptor.ResultSetInterceptor"/>结果集拦截器
    
   
    </plugins>


3、使用我们的代码生成器 ，我们可以生成如下代码：
1:entity 2 mainMapper 4 service 5：dao 6:extdao 7:mapper
调用方式如下，在您的项目中建立junit两个类

第一个类：

@RunWith(SpringRunner.class)
@SpringBootTest(classes = RuoYiApplication.class)
//由于是Web项目，Junit需要模拟ServletContext，因此我们需要给我们的测试类加上@WebAppConfiguration。

@WebAppConfiguration

public class BaseTests {

    @Before
    
    public void init() {
        System.out.println("开始测试-----------------");
    }
 
    @After
    public void after() {
        System.out.println("测试结束-----------------");
    }

}

第二个类：

public class FastGenCode  extends BaseTests {
	
	   @Autowired
	   ApplicationContext applicationContext;
	@Test
	public void testParse() {
		
		SpringContextHolder.applicationContext=applicationContext;
		//工程路径
		String projectBasePath="/Users/admin/eclipse-workspace/projectName";
		//包路径
		String packagePathUrl="com.ruoyi.system.mapper";
        
        //包含生成文件的类型,目前包含的
      	TjNewMap<String,String> includeType=TjNewMap.newInstance();
      	//选择所需要生成的内容：1:entity 2 mainMapper 4 service 5：dao 6:extdao 7:mapper
      	//includeType.putCon("1","2","3","4","5","6","7");//1:entity 2 mainMapper  4 service 5：dao 6:extdao 7:mapper
      	includeType.putConMutil("1","2","4","5","6","7");
       //添加数据库表
      	List<String> tableList=Lists.newArrayList();
		tableList.add("sys_notice");
//		tableList.add("tb_message_info");
		//tableList.add("tb_message_type");
      	//tableList.add("tb_app_user");
		GenCodeTool gtc=new GenCodeTool( projectBasePath, tableList, includeType); 
		gtc.setPackagePathUrl(packagePathUrl);
		try {
			gtc.startGen();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
直接执行该junit，即可生成到指定项目路径下相关类。
4、如果是对现有项目的某些功能改造，如何改造，请参考csdn文章：
5、另外，我们的mapper是主子结构，即子mapper继承主mapper，主mapper就是根据数据库表生成的mapper文件，子mapper是用来手工增加方法的，这样当数据库变化时，直接覆盖主mapper即可，但是目前为止 ，我们还没有需要手工增加方法的必要，同时dao层也是与mapper文件对应的继承关系
