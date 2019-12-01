package com.tengjie.common.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.springframework.beans.factory.BeanDefinitionStoreException;

import com.google.common.collect.Sets;
import com.tengjie.common.config.Global;
import com.tengjie.common.persistence.TjBaseEntity;

/**
 * @author jiangzeyin
 * 还有一个叫ClassScaner的类，也是这个作用，抽时间看一下
 * @date 2016-9-9
 */
public class PackageUtil {
	public static Set<Class<?>> allClass;//classPathForProject下的全部类
	public static Set<Class<?>> allEntityClass;//classPathForProject下的所有baseEntity的子类
	public static String[] classPathForProject;
	
	static {
		classPathForProject=new String[] {"com.jujienet", "com.tengjie.base", "com.dongfang","com.tengjie.common"};
		if(allClass==null) {
			allClass=new LinkedHashSet<Class<?>>();
			for(String tempPath:classPathForProject) {
				allClass.addAll(getClasses(tempPath));
			}
		}
		if(allEntityClass==null) {
			allEntityClass=new LinkedHashSet<Class<?>>();
			for(Class cla:allClass){
				if(TjBaseEntity.class.isAssignableFrom(cla)) {
					allEntityClass.add(cla);
				}
			}
		}
	}
	/**
	 * 根据数据的表名获得对应的实体类名
	 * @param dbTableName：数据库的实际表名tb_aaa_aa
	 * @param firstToUp：true首字母大写
	 * @return
	 */
	public static String findEntityNameByDbTableName(String dbTableName,boolean firstToUp) {
		String camel=StringUtils.toCamelCase(dbTableName);
		String entityName=null;
		if(camel.toLowerCase().equals("sysuser")){//对于sysUser来说是数据库的名字，但是对于实体却是user
			entityName="user";
		}else {
			if(camel.startsWith("tb")) {
				entityName=camel.replace("tb", "");
			}else {
				entityName=camel;
			}
		}
		if(firstToUp) {
			entityName=StringUtils.firstToUpper(entityName);
		}else {
			entityName=StringUtils.firstToLower(entityName);
		}
		return entityName;
			
	}
	private static String filterClassName(String className) {
		if(className.equals("sysUser")||className.equals("SysUser")){//对于sysUser来说是数据库的名字，但是对于实体却是user
			className="User";
		}
		if(className.equals("tbUser")||className.equals("TbUser")){//对于sysUser来说是数据库的名字，但是对于实体却是user
			className="AppUser";
		}
		if(className.startsWith("tb"))
			className=className.replace("tb", "");
		return className;
	}
	/**
	 * 到指定包路径下寻找对应的类名，找到后返回全路径
	 * 优化：大部分是获得entity类，所以优先取得entity类，娶不到再取全局查，另外，所有的类要加载到内存中
	 * @param className
	 * @param projectPackagePath
	 * @return
	 */
	public static Class findDestClass(String className){
		
		className=filterClassName(className);
		for(Class cla:allEntityClass){
			if(cla.getSimpleName().equals(className)){
				return cla;
			}
		}
		for(Class cla:allClass){
			if(cla.getSimpleName().equals(className)){
				return cla;
			}
		}
		return null;
	}
	/**
	 * 到指定包路径下寻找对应的类名，找到后返回全路径
	 * 优化：大部分是获得entity类，所以优先取得entity类，娶不到再取全局查，另外，所有的类要加载到内存中
	 * @param className
	 * @param projectPackagePath
	 * @return
	 */
	public static String findDestClassPath(String className){
		String classPackage="";
		className=filterClassName(className);
		for(Class cla:allEntityClass){
			if(cla.getSimpleName().equals(className)){
				classPackage=cla.getName();
				return classPackage;
			}
		}
		for(Class cla:allClass){
			if(cla.getSimpleName().equals(className)){
				classPackage=cla.getName();
				return classPackage;
			}
		}
		return classPackage;
	}
	
	
	/**
	 * 从包package中获取所有的Class
	 * 
	 * @param pack
	 * @return
	 */
	public static Set<Class<?>> getClasses(String pack) {
			// 第一个class类的集合
			Set<Class<?>> classes = new LinkedHashSet<Class<?>>();
			// 是否循环迭代
			boolean recursive = true;
			// 获取包的名字 并进行替换
			String packageName = pack;
			String packageDirName = packageName.replace('.', '/');
			// 定义一个枚举的集合 并进行循环来处理这个目录下的things
			Enumeration<URL> dirs;
			try {
				dirs = Thread.currentThread().getContextClassLoader().getResources(
						packageDirName);
				// 循环迭代下去
				while (dirs.hasMoreElements()) {
					// 获取下一个元素
					URL url = dirs.nextElement();
					// 得到协议的名称
					String protocol = url.getProtocol();
					// 如果是以文件的形式保存在服务器上
					if ("file".equals(protocol)) {
						//System.err.println("file类型的扫描");
						// 获取包的物理路径
						String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
						// 以文件的方式扫描整个包下的文件 并添加到集合中
						findAndAddClassesInPackageByFile(packageName, filePath,
								recursive, classes);
					} else if ("jar".equals(protocol)) {
						// 如果是jar包文件
						// 定义一个JarFile
						//System.err.println("jar类型的扫描");
						JarFile jar;
						try {
							// 获取jar
							jar = ((JarURLConnection) url.openConnection())
									.getJarFile();
							// 从此jar包 得到一个枚举类
							Enumeration<JarEntry> entries = jar.entries();
							// 同样的进行循环迭代
							while (entries.hasMoreElements()) {
								// 获取jar里的一个实体 可以是目录 和一些jar包里的其他文件 如META-INF等文件
								JarEntry entry = entries.nextElement();
								String name = entry.getName();
								// 如果是以/开头的
								if (name.charAt(0) == '/') {
									// 获取后面的字符串
									name = name.substring(1);
								}
								// 如果前半部分和定义的包名相同
								if (name.startsWith(packageDirName)) {
									int idx = name.lastIndexOf('/');
									// 如果以"/"结尾 是一个包
									if (idx != -1) {
										// 获取包名 把"/"替换成"."
										packageName = name.substring(0, idx)
												.replace('/', '.');
									}
									// 如果可以迭代下去 并且是一个包
									if ((idx != -1) || recursive) {
										// 如果是一个.class文件 而且不是目录
										if (name.endsWith(".class")
												&& !entry.isDirectory()) {
											// 去掉后面的".class" 获取真正的类名
											String className = name.substring(
													packageName.length() + 1, name
															.length() - 6);
											
												//System.out.println("-----"+className);
												if(className.length()<2||"JavaMailUtils$1".equals(className)||"JavaMailUtils".equals(className)){//由于JavaMailUtils有匿名内部类Authenticator，会提示找不到，导致JavaMailUtils无法创建，并且也无法捕获异常信息，不知道为啥.其他的内部类都能正确的创建
													continue;
												}
												// 添加到classes
												if(className.equalsIgnoreCase("padpropertiesutil"))continue;
												if(className.equalsIgnoreCase("cacheUtils"))continue;
												if(className.equalsIgnoreCase("springContextHolder"))continue;
												if(className.equalsIgnoreCase("jedisutils"))continue;
												if(className.equalsIgnoreCase("ehcacheutils"))continue;
												if(className.equalsIgnoreCase("dictutils"))continue;
												if(className.equalsIgnoreCase("BadHanyuPinyinOutputFormatCombination"))continue;
												Class<?> cc=null;
												try {
													if("HanlpUtil".equals(className)) {//HanlpUtil是解析xmind的时候使用，有些项目没有配置这个，则认为不需要
														String crfResoursePat=Global.getConfig("crfResoursePath");
														if(StringUtils.isEmpty(crfResoursePat))continue;
													}
													cc = Class.forName(packageName + '.'+ className);
												} catch (BeanDefinitionStoreException|NoClassDefFoundError|ClassNotFoundException e1) {
													continue;
												}
												classes.add(cc);
											
										}
									}
								}
							}
						} catch (IOException e) {
							// log.error("在扫描用户定义视图时从jar包获取文件出错");
							e.printStackTrace();
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		
		return classes;
	}
	/**
	 * 以文件的形式来获取包下的所有Class
	 * 
	 * @param packageName
	 * @param packagePath
	 * @param recursive
	 * @param classes
	 */
	public static void findAndAddClassesInPackageByFile(String packageName,
			String packagePath, final boolean recursive, Set<Class<?>> classes) {
		// 获取此包的目录 建立一个File
		File dir = new File(packagePath);
		// 如果不存在或者 也不是目录就直接返回
		if (!dir.exists() || !dir.isDirectory()) {
			// log.warn("用户定义包名 " + packageName + " 下没有任何文件");
			return;
		}
		
		// 如果存在 就获取包下的所有文件 包括目录
		File[] dirfiles = dir.listFiles(new FileFilter() {
			// 自定义过滤规则 如果可以循环(包含子目录) 或则是以.class结尾的文件(编译好的java类文件)
			public boolean accept(File file) {
				return (recursive && file.isDirectory())
						|| (file.getName().endsWith(".class"));
			}
		});
		// 循环所有文件
		for (File file : dirfiles) {
			// 如果是目录 则继续扫描
			if (file.isDirectory()) {
				findAndAddClassesInPackageByFile(packageName + "."
						+ file.getName(), file.getAbsolutePath(), recursive,
						classes);
			} else {
				// 如果是java类文件 去掉后面的.class 只留下类名
				String className = file.getName().substring(0,
						file.getName().length() - 6);
				try {
					// 添加到集合中去
					//classes.add(Class.forName(packageName + '.' + className));
                                         //经过回复同学的提醒，这里用forName有一些不好，会触发static方法，没有使用classLoader的load干净
                                        classes.add(Thread.currentThread().getContextClassLoader().loadClass(packageName + '.' + className));  
                              } catch (ClassNotFoundException e) {
					// log.error("添加用户自定义视图类错误 找不到此类的.class文件");
				//	e.printStackTrace();
                            	  continue;
				}
			}
		}
	}
    /**
     * 获取某包下（包括该包的所有子包）所有类
     *
     * @param packageName 包名
     * @return 类的完整名称
     * @throws UnsupportedEncodingException
     */
    public static List<String> getClassName(String packageName) throws IOException {
        return getClassName(packageName, true);
    }

    /**
     * 获取某包下所有类
     *
     * @param packageName  包名
     * @param childPackage 是否遍历子包
     * @return 类的完整名称
     * @throws UnsupportedEncodingException
     */
    public static List<String> getClassName(String packageName, boolean childPackage) throws IOException {
        List<String> fileNames = new ArrayList<>();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        String packagePath = packageName.replace(".", "/");
        Enumeration<URL> urls = loader.getResources(packagePath);
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            if (url == null)
                continue;
            String type = url.getProtocol();
            if (type.equals("file")) {
                fileNames.addAll(getClassNameByFile(url.getPath(), childPackage));
            } else if (type.equals("jar")) {
                fileNames.addAll(getClassNameByJar(url.getPath(), childPackage));
            }
        }
        fileNames.addAll(getClassNameByJars(((URLClassLoader) loader).getURLs(), packagePath, childPackage));
        return fileNames;
    }

    /**
     * 从项目文件获取某包下所有类
     *
     * @param filePath     文件路径
     *                     类名集合
     * @param childPackage 是否遍历子包
     * @return 类的完整名称
     * @throws UnsupportedEncodingException
     */
    private static List<String> getClassNameByFile(String filePath, boolean childPackage) throws UnsupportedEncodingException {
        List<String> myClassName = new ArrayList<>();
        //filePath = UrlDecode.getURLDecode(filePath);
        File file = new File(filePath);
        File[] childFiles = file.listFiles();
        if (childFiles == null)
            return myClassName;
        for (File childFile : childFiles) {
            if (childFile.isDirectory()) {
                if (childPackage) {
                    myClassName.addAll(getClassNameByFile(childFile.getPath(), childPackage));
                }
            } else {
                String childFilePath = childFile.getPath();
                //childFilePath = FileUtil.clearPath(childFilePath);
                if (childFilePath.endsWith(".class")) {
                    childFilePath = childFilePath.substring(childFilePath.indexOf("/classes/") + 9, childFilePath.lastIndexOf("."));
                    childFilePath = childFilePath.replace("/", ".");
                    myClassName.add(childFilePath);
                }
            }
        }
        return myClassName;
    }

    /**
     * 从jar获取某包下所有类
     *
     * @param jarPath      jar文件路径
     * @param childPackage 是否遍历子包
     * @return 类的完整名称
     * @throws UnsupportedEncodingException
     */
    private static List<String> getClassNameByJar(String jarPath, boolean childPackage) throws UnsupportedEncodingException {
        List<String> myClassName = new ArrayList<String>();
        String[] jarInfo = jarPath.split("!");
        String jarFilePath = jarInfo[0].substring(jarInfo[0].indexOf("/"));
       // jarFilePath = UrlDecode.getURLDecode(jarFilePath);
        String packagePath = jarInfo[1].substring(1);
        try {
            JarFile jarFile = new JarFile(jarFilePath);
            Enumeration<JarEntry> entrys = jarFile.entries();
            while (entrys.hasMoreElements()) {
                JarEntry jarEntry = entrys.nextElement();
                String entryName = jarEntry.getName();
                if (entryName.endsWith(".class")) {
                    if (childPackage) {
                        if (entryName.startsWith(packagePath)) {
                            entryName = entryName.replace("/", ".").substring(0, entryName.lastIndexOf("."));
                            myClassName.add(entryName);
                        }
                    } else {
                        int index = entryName.lastIndexOf("/");
                        String myPackagePath;
                        if (index != -1) {
                            myPackagePath = entryName.substring(0, index);
                        } else {
                            myPackagePath = entryName;
                        }
                        if (myPackagePath.equals(packagePath)) {
                            entryName = entryName.replace("/", ".").substring(0, entryName.lastIndexOf("."));
                            myClassName.add(entryName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            //SystemLog.Log(LogType.systemInfo, e.getMessage(), e);
        }
        return myClassName;
    }

    /**
     * 从所有jar中搜索该包，并获取该包下所有类
     *
     * @param urls         URL集合
     * @param packagePath  包路径
     * @param childPackage 是否遍历子包
     * @return 类的完整名称
     * @throws UnsupportedEncodingException
     */
    private static List<String> getClassNameByJars(URL[] urls, String packagePath, boolean childPackage) throws UnsupportedEncodingException {
        List<String> myClassName = new ArrayList<String>();
        if (urls != null) {
            for (int i = 0; i < urls.length; i++) {
                URL url = urls[i];
                String urlPath = url.getPath();
                // 不必搜索classes文件夹
                if (urlPath.endsWith("classes/")) {
                    continue;
                }
                String jarPath = urlPath + "!/" + packagePath;
                myClassName.addAll(getClassNameByJar(jarPath, childPackage));
            }
        }
        return myClassName;
    }
}