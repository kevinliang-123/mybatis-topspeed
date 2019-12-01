package com.tengjie.common.utils;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;



/*import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
*/
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class XmlUtils {

	/**
	 * @description: map转XML
	 * @author: hanshichao
	 * @date: 2017年8月24日 下午3:07:21
	 */
	public static String map2Xml(String alias, Map<?, ?> map) {

		StringBuffer sb = new StringBuffer();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><" + alias + ">");
		map2Xml(map, sb);
		sb.append("</" + alias + ">");

		return sb.toString();
	}

	/**
	 * @description: map转XML
	 * @author: hanshichao
	 * @date: 2017年8月24日 下午3:08:48
	 */
	public static String map2Xml(Map<?, ?> map) {

		StringBuffer sb = new StringBuffer();

		map2Xml(map, sb);

		return sb.toString();
	}

	
	/**
	* @description: XML转换成map dom4j
	* @author: hanshichao
	* @date: 2017年8月24日 下午4:00:41
	*/
/*	public static Map<String, Object> Dom2Map(String xml) {
		Map<String, Object> map = new HashMap<String, Object>();
		Document doc = null;
		try {
			doc = DocumentHelper.parseText(xml);
		} catch (DocumentException e) {
			
			e.printStackTrace();
		}
		if (doc == null)
			return null;
		Element root = doc.getRootElement();
		for (Iterator<?> iterator = root.elementIterator(); iterator.hasNext();) {
			Element e = (Element) iterator.next();
			map.put(e.getName(), e.isTextOnly() ? e.getText() : Dom2Map(e.asXML()));

		}
		return map;
	}*/

	public static Map<String, Object> Dom2Map(String xml){
		XStream magicApi = new XStream();
        magicApi.registerConverter(new MapEntryConverter());
        magicApi.alias("message", Map.class);

        @SuppressWarnings("unchecked")
		Map<String, Object> extractedMap = (Map<String, Object>) magicApi.fromXML(xml);

        return extractedMap;
	}
	
	private static void map2Xml(Map<?, ?> map, StringBuffer sb) {
		Set<?> set = map.keySet();
		for (Iterator<?> it = set.iterator(); it.hasNext();) {
			String key = (String) it.next();
			Object value = map.get(key);
			if (null == value)
				value = "";
			if (value.getClass().getName().equals("java.util.ArrayList")) {
				ArrayList<?> list = (ArrayList<?>) map.get(key);
				sb.append("<" + key + ">");
				for (int i = 0; i < list.size(); i++) {
					HashMap<?, ?> hm = (HashMap<?, ?>) list.get(i);
					map2Xml(hm, sb);
				}
				sb.append("</" + key + ">");

			} else {
				if (value instanceof HashMap) {
					sb.append("<" + key + ">");
					map2Xml((HashMap<?, ?>) value, sb);
					sb.append("</" + key + ">");
				} else {
					sb.append("<" + key + ">" + value + "</" + key + ">");
				}

			}

		}
	}
	
	
	
	 public static class MapEntryConverter implements Converter {

	        @SuppressWarnings("rawtypes")
			public boolean canConvert(Class clazz) {
	            return AbstractMap.class.isAssignableFrom(clazz);
	        }

	        @SuppressWarnings("rawtypes")
			public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {

	            AbstractMap map = (AbstractMap) value;
	            for (Object obj : map.entrySet()) {
	                Map.Entry entry = (Map.Entry) obj;
	                writer.startNode(entry.getKey().toString());
	                Object val = entry.getValue();
	                if (val instanceof Map) {
	                    marshal(val, writer, context);
	                } else if (null != val) {
	                    writer.setValue(val.toString());
	                }
	                writer.endNode();
	            }

	        }

	        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {

	            Map<String, Object> map = new HashMap<String, Object>();

	            while(reader.hasMoreChildren()) {
	                reader.moveDown();

	                String key = reader.getNodeName(); // nodeName aka element's name
	                String value = reader.getValue().replaceAll("\\n|\\t", "");
	                if (null == value || value.trim().length()==0) {

	                	map.put(key, unmarshal(reader, context));
	                } else {
	                	map.put(key, value);
	                }

	                reader.moveUp();
	            }

	            return map;
	        }
	    }


	public static void main(String[] args) {
		String xml = "<message><head><version>1.0</version><partnerId>2000010</partnerId><msgId>b4b4d3030a7f4f189033bffbcd092deb</msgId><msgTime>20170828123431</msgTime></head><body><orderId>GKPT20170828123258</orderId><ycOrderId>201708281001002465438</ycOrderId><busiCode>1</busiCode><code>0000</code><msg>订单提交成功</msg></body><signTag>bf98b2526cd9c406000a5e5ee93acec7</signTag></message>\r\n";
		Map<String, Object> map = (Map<String, Object>) Dom2Map(xml);
		if (null != map) {
			System.out.println("===");

		} else {

		}

	}

}
