package com.tengjie.cglib.cust;

import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;

import net.sf.cglib.core.ClassEmitter;
import net.sf.cglib.core.Constants;
import net.sf.cglib.core.EmitUtils;
import net.sf.cglib.core.KeyFactory;
import net.sf.cglib.core.ReflectUtils;

public class BeanGenerator extends AbstractClassGenerator {

	 private static final Source SOURCE = new Source(BeanGenerator.class.getName());
	    private static final BeanGeneratorKey KEY_FACTORY =
	      (BeanGeneratorKey)KeyFactory.create(BeanGeneratorKey.class);
	    
	    interface BeanGeneratorKey {
	        public Object newInstance(String superclass, Map props);
	    }

	    private Class superclass;
	    private Map props = new HashMap();
	    private boolean classOnly;

	    public BeanGenerator() {
	        super(SOURCE);
	    }

	    /**
	     * Set the class which the generated class will extend. The class
	     * must not be declared as final, and must have a non-private
	     * no-argument constructor.
	     * @param superclass class to extend, or null to extend Object
	     */
	    public void setSuperclass(Class superclass) {
	        if (superclass != null && superclass.equals(Object.class)) {
	            superclass = null;
	        }
	        this.superclass = superclass;
	    }

	    public void addProperty(String name, Class type) {
	        if (props.containsKey(name)) {
	            throw new IllegalArgumentException("Duplicate property name \"" + name + "\"");
	        }
	        props.put(name, Type.getType(type));
	    }

	    protected ClassLoader getDefaultClassLoader() {
	        if (superclass != null) {
	            return superclass.getClassLoader();
	        } else {
	            return null;
	        }
	    }

	    public Object create() {
	        classOnly = false;
	        return createHelper();
	    }

	    public Object createClass() {
	        classOnly = true;
	        return createHelper();
	    }

	    private Object createHelper() {
	        if (superclass != null) {
	            setNamePrefix(superclass.getName());
	        }
	        String superName = (superclass != null) ? superclass.getName() : "java.lang.Object";
	        Object key = KEY_FACTORY.newInstance(superName, props);
	        return super.create(key);
	    }

	    public void generateClass(ClassVisitor v) throws Exception {
	        int size = props.size();
	        String[] names = (String[])props.keySet().toArray(new String[size]);
	        Type[] types = new Type[size];
	        for (int i = 0; i < size; i++) {
	            types[i] = (Type)props.get(names[i]);
	        }
	        ClassEmitter ce = new ClassEmitter(v);
	        ce.begin_class(Constants.V1_2,
	                       Constants.ACC_PUBLIC,
	                       getClassName(),
	                       superclass != null ? Type.getType(superclass) : Constants.TYPE_OBJECT,
	                       null,
	                       null);
	        EmitUtils.null_constructor(ce);
	        EmitUtils.add_properties(ce, names, types);
	        ce.end_class();
	    }

	    protected Object firstInstance(Class type) {
	        if (classOnly) {
	            return type;
	        } else {
	            return ReflectUtils.newInstance(type);
	        }
	    }

	    protected Object nextInstance(Object instance) {
	        Class protoclass = (instance instanceof Class) ? (Class)instance : instance.getClass();
	        if (classOnly) {
	            return protoclass;
	        } else {
	            return ReflectUtils.newInstance(protoclass);
	        }
	    }

	    public static void addProperties(BeanGenerator gen, Map props) {
	        for (Iterator it = props.keySet().iterator(); it.hasNext();) {
	            String name = (String)it.next();
	            gen.addProperty(name, (Class)props.get(name));
	        }
	    }

	    public static void addProperties(BeanGenerator gen, Class type) {
	        addProperties(gen, ReflectUtils.getBeanProperties(type));
	    }

	    public static void addProperties(BeanGenerator gen, PropertyDescriptor[] descriptors) {
	        for (int i = 0; i < descriptors.length; i++) {
	            gen.addProperty(descriptors[i].getName(), descriptors[i].getPropertyType());
	        }
	    }
}
