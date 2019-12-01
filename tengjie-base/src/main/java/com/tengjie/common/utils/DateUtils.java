package com.tengjie.common.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.time.DateFormatUtils;

/**
 * 日期工具类, 继承org.apache.commons.lang.time.DateUtils类
 * @author
 * @version 2014-4-15
 */
public class DateUtils extends org.apache.commons.lang3.time.DateUtils {
	
	private static String[] parsePatterns = {
		"yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM", 
		"yyyy/MM/dd", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm", "yyyy/MM",
		"yyyy.MM.dd", "yyyy.MM.dd HH:mm:ss", "yyyy.MM.dd HH:mm", "yyyy.MM"};

	/**
	 * 得到当前日期字符串 格式（yyyy-MM-dd）
	 */
	public static String getDate() {
		return getDate("yyyy-MM-dd");
	}
	/**
	 * 得到当前日期字符串 格式（yyyyMMdd）
	 */
	public static String getDates() {
		return getDate("yyyyMMdd");
	}
	
	/**
	 * 得到当前日期字符串 格式（yyyy-MM-dd） pattern可以为："yyyy-MM-dd" "HH:mm:ss" "E"
	 */
	public static String getDate(String pattern) {
		return DateFormatUtils.format(new Date(), pattern);
	}
	/**
	 * 得到格式化后的日期 默认格式（yyyy-MM-dd） pattern可以为："yyyy-MM-dd" "HH:mm:ss" "E"
	 */
	public static Date formatDateToDate(Date date, Object... pattern) {
		String formatDate = null;
		String custPatttern="yyyy-MM-dd";
		if (pattern != null && pattern.length > 0) {
			custPatttern=pattern[0].toString();
		} 
		formatDate = DateFormatUtils.format(date, custPatttern);
		try {
			return DateUtils.parseDate(formatDate, custPatttern);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * 得到日期字符串 默认格式（yyyy-MM-dd） pattern可以为："yyyy-MM-dd" "HH:mm:ss" "E"
	 */
	public static String formatDate(Date date, String... pattern) {
		String formatDate = null;
		if(date==null)return null;
		if (pattern != null && pattern.length > 0) {
			formatDate = DateFormatUtils.format(date, pattern[0].toString());
		} else {
			formatDate = DateFormatUtils.format(date, "yyyy-MM-dd");
		}
		return formatDate;
	}

	/**
	 * 得到日期字符串 默认格式（yyyy-MM-dd） pattern可以为："yyyy-MM-dd" "HH:mm:ss" "E"
	 * 主要是为了动态jsp页面的tableData区域使用，遇到日期的时分秒都是0的情况，不显示
	 */
	public static String formatDateSmart(Date date, String... pattern) {
		String formatDate = null;
		if(date==null)return null;
		if (pattern != null && pattern.length > 0) {
			String tempPattern=pattern[0].toString();
			if(tempPattern.length()>10&&!ifContainHourMinSec(date)) {
				tempPattern="yyyy-MM-dd";
			}
			formatDate = DateFormatUtils.format(date, tempPattern);
		} else {
			formatDate = DateFormatUtils.format(date, "yyyy-MM-dd");
		}
		return formatDate;
	}
	
	/**
	 * 得到日期时间字符串，转换格式（yyyy-MM-dd）,日期可能为空，若为空则返回 空
	 */
	public static String formatDateTimeMaybeNull(Date date) {
		if(date==null)return null;
		return formatDate(date, "yyyy-MM-dd");
	}
	/**
	 * 得到日期时间字符串，转换格式（yyyy-MM-dd）,日期可能为空，若为空则返回 空
	 */
	public static String formatDateTimeMaybeNullNoline(Date date) {
		if(date==null)return null;
		return formatDate(date, "yyyyMMdd");
	}
	/**
	 * 得到日期时间字符串，转换格式（yyyy-MM-dd HH:mm:ss）
	 */
	public static String formatDateTime(Date date) {
		return formatDate(date, "yyyy-MM-dd HH:mm:ss");
	}

	/**
	 * 得到当前时间字符串 格式（HH:mm:ss）
	 */
	public static String getTime() {
		return formatDate(new Date(), "HH:mm:ss");
	}

	/**
	 * 得到当前日期和时间字符串 格式（yyyy-MM-dd HH:mm:ss）
	 */
	public static String getDateTime() {
		return formatDate(new Date(), "yyyy-MM-dd HH:mm:ss");
	}
	/**
	 * 得到当前日期和时间字符串 格式（yyyy-MM-dd HH:mm:ss）
	 */
	public static String getSdfTime() {
		return formatDate(new Date(), "yyyyMMddHHmmss");
	}

	/**
	 * 得到当前年份字符串 格式（yyyy）
	 */
	public static String getYear() {
		return formatDate(new Date(), "yyyy");
	}

	/**
	 * 得到当前月份字符串 格式（MM）
	 */
	public static String getMonth() {
		return formatDate(new Date(), "MM");
	}

	/**
	 * 得到当天字符串 格式（dd）
	 */
	public static String getDay() {
		return formatDate(new Date(), "dd");
	}

	/**
	 * 得到当前星期字符串 格式（E）星期几
	 */
	public static String getWeek() {
		return formatDate(new Date(), "E");
	}
	
	/**
	 * 日期型字符串转化为日期 格式
	 * { "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", 
	 *   "yyyy/MM/dd", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm",
	 *   "yyyy.MM.dd", "yyyy.MM.dd HH:mm:ss", "yyyy.MM.dd HH:mm" }
	 */
	public static Date parseDate(Object str) {
		if (str == null){
			return null;
		}
		try {
			return parseDate(str.toString(), parsePatterns);
		} catch (ParseException e) {
			return null;
		}
	}

	/**
	 * 获取过去的天数
	 * @param date
	 * @return
	 */
	public static long pastDays(Date date) {
		long t = new Date().getTime()-date.getTime();
		return t/(24*60*60*1000);
	}

	/**
	 * 获取过去的小时
	 * @param date
	 * @return
	 */
	public static long pastHour(Date date) {
		long t = new Date().getTime()-date.getTime();
		return t/(60*60*1000);
	}
	
	/**
	 * 获取过去的分钟
	 * @param date
	 * @return
	 */
	public static long pastMinutes(Date date) {
		long t = new Date().getTime()-date.getTime();
		return t/(60*1000);
	}
	
	/**
	 * 转换为时间（天,时:分:秒.毫秒）
	 * @param timeMillis
	 * @return
	 */
    public static String formatDateTime(long timeMillis){
		long day = timeMillis/(24*60*60*1000);
		long hour = (timeMillis/(60*60*1000)-day*24);
		long min = ((timeMillis/(60*1000))-day*24*60-hour*60);
		long s = (timeMillis/1000-day*24*60*60-hour*60*60-min*60);
		long sss = (timeMillis-day*24*60*60*1000-hour*60*60*1000-min*60*1000-s*1000);
		return (day>0?day+",":"")+hour+":"+min+":"+s+"."+sss;
    }
	
	/**
	 * 获取两个日期之间的天数
	 * 
	 * @param before
	 * @param after
	 * @return
	 */
	public static double getDistanceOfTwoDate(Date before, Date after) {
		long beforeTime = before.getTime();
		long afterTime = after.getTime();
		return (afterTime - beforeTime) / (1000 * 60 * 60 * 24);
	}
	/**
	 * 两个时间段，相差多少天，上面的方法由于有可能带时分秒，所以是double类型
	 * 
	 * @param startDate
	 * @param endDate
	 * @return
	 * @throws ParseException
	 */
	public static int daysBetween(Date startDate, Date endDate)
			throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

		startDate = sdf.parse(sdf.format(startDate));
		endDate = sdf.parse(sdf.format(endDate));

		Calendar cal = Calendar.getInstance();
		cal.setTime(startDate);
		long startTime = cal.getTimeInMillis();
		cal.setTime(endDate);
		long endTime = cal.getTimeInMillis();
		long between_days = (long) Math.ceil((endTime - startTime)
				/ (1000 * 3600 * 24.0));

//		return Integer.parseInt(String.valueOf(between_days)) == 0 ? 1 : Integer.parseInt(String.valueOf(between_days))+1;
		return Integer.parseInt(String.valueOf(between_days)) == 0 ? 1 : Integer.parseInt(String.valueOf(between_days));//  去掉  +1

	}
	/**
	 * 根据日期是否带有时分秒，自动格式化日期
	 * @param d
	 * @return
	 */
	public static String autoFormateDate(Date d){
		String temp=null;
		if(ifContainHourMinSec(d)){
			temp=formatDate(d,"yyyy-MM-dd HH:mm:ss");
		}else{
			temp=formatDate(d,"yyyy-MM-dd");
		}
		return temp;
	}
	/**
	 * 一个字符串形式的日期类型，是否包含时分秒,这里指的是原始格式中是否包含
	 * @param dateStr
	 * @return
	 */
	public static boolean ifDateStrContainHourMinSec(String dateStr){
		boolean bl=false;
		if(StringUtils.isEmpty(dateStr))return bl;
		if(dateStr.length()==10)return false;
		if(dateStr.length()>10)return true;
		return bl;
	}
	/**
	 * 判断一个日期类型是否包含时分秒，如果时分秒都是零默认认为是不需要的
	 * @param d
	 */
	public static boolean ifContainHourMinSec(Date d){
		GregorianCalendar gc = new GregorianCalendar();
		gc.setTime(d);
		if( (gc.get(Calendar.HOUR)==0) && (gc.get(Calendar.MINUTE)==0) && (gc.get(Calendar.SECOND)==0) ) {
			return false;
		}
		return true;
	}
	/**
	 * 获取当前时间
	 * @since: 2017年10月13日 下午4:35:13
	 */
	public static Date getNow(){
		return new Date();
	}
	/**
	 * 加减分钟数
	 * 
	 * @param num
	 * @param Date
	 * @return
	 */
	public static Date addMin(int num, Date Date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(Date);
		calendar.add(Calendar.MINUTE, num);// 把日期往后增加 num 分钟.整数往后推,负数往前移动
		return calendar.getTime(); //
	}
	
	/**
     * 判断time是否在from，to之内
     *
     * @param time 指定日期
     * @param from 开始日期
     * @param to   结束日期
     * @return
     */
    public static boolean belongCalendar(Date time, Date from, Date to) {
        Calendar date = Calendar.getInstance();
        date.setTime(time);

        Calendar after = Calendar.getInstance();
        after.setTime(from);

        Calendar before = Calendar.getInstance();
        before.setTime(to);

        if (date.after(after) && date.before(before)) {
            return true;
        } else {
            return false;
        }
    }
    /**
     * 判断一个给定的字符串，是否是日期形态的，比如你说如一个abc，肯定不是，如果是2018/09/12就是等
     * @param str
     * @return
     */
    public static boolean isDateTypeStr(String str){
    	boolean bl=false;
    	Pattern pattern = Pattern.compile("^((\\d{2}(([02468][048])|([13579][26]))[\\-\\/\\s]?((((0?[13578])|(1[02]))[\\-\\/\\s]?((0?[1-9])|([1-2][0-9])|(3[01])))|(((0?[469])|(11))[\\-\\/\\s]?((0?[1-9])|([1-2][0-9])|(30)))|(0?2[\\-\\/\\s]?((0?[1-9])|([1-2][0-9])))))|(\\d{2}(([02468][1235679])|([13579][01345789]))[\\-\\/\\s]?((((0?[13578])|(1[02]))[\\-\\/\\s]?((0?[1-9])|([1-2][0-9])|(3[01])))|(((0?[469])|(11))[\\-\\/\\s]?((0?[1-9])|([1-2][0-9])|(30)))|(0?2[\\-\\/\\s]?((0?[1-9])|(1[0-9])|(2[0-8]))))))(\\s(((0?[0-9])|([1][0-9])|([2][0-4]))\\:([0-5]?[0-9])((\\s)|(\\:([0-5]?[0-9])))))?$");
    	  
    Matcher matcher = pattern.matcher(str);
    	 while(matcher.find()){
    		 bl=true;
             System.out.println(matcher.group());
         }
    	 return bl;
    }
    /**
     * 判断给定时间与当前时间相差多少天
     * 正数表示在当前时间之后，负数表示在当前时间之前
     * @param date
     * @return
     */
    public static long getDistanceDays(Date date) {
    	return getDistanceDays( formatDate(date, "yyyy-MM-dd"));
    }
    /**
     * 判断给定时间与当前时间相差多少天
     * 正数表示在当前时间之后，负数表示在当前时间之前
     * @param date
     * @return
     */
    public static long getDistanceDays(String date) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        long days = 0;
        try {
            Date time = df.parse(date);//String转Date
            Date now = formatDateToDate(new Date(), "yyyy-MM-dd");//获取当前时间
            long time1 = time.getTime();
            long time2 = now.getTime();
            long diff = time1 - time2;
            days = diff / (1000 * 60 * 60 * 24);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return days;//正数表示在当前时间之后，负数表示在当前时间之前
    }
   /**
    * 为前端，有时需要将日期转化为几分钟前、刚刚等形态使用，不在配置内的当年的，只显示月和日，不在当前年显示具体的年月日
    * @param destTime
    * @param transConfigMap:key为分钟数，value为该分钟数的描述；注意，必须用LinkedHashMap按照分钟数从小到大排列，可以为空，
    * 为空时：小于1分钟的为刚刚，小于1小时的按照多少分钟前，大于1小时且还在当天的，显示几小时前，大于当天的显示且日期在本年的，显示月和
    * 举例transConfigMap内容：10:刚刚
    * @return
    */
    public static String transferDateForFront(Date destTime,LinkedHashMap<Integer,String> transConfigMap){
    	  String result="";
    	  if(transConfigMap==null||transConfigMap.size()<1) {
    		  transConfigMap=new LinkedHashMap();
    		  transConfigMap.put(1, "刚刚");
    		  transConfigMap.put(60, "分钟前");
    		  transConfigMap.put(60*24, "小时前");
    	  }
    
    	  long dist=DateUtils.getDistanceDays(destTime);
    	  boolean sameYear= DateUtils.getYear().equals(DateUtils.formatDate(destTime, "yyyy"));
    	  //先判断大于1天的
    	  if(dist<0) {
    		  if(sameYear) {
    			  result=DateUtils.formatDate(destTime, "MM月dd月");
    		  }else {
    			  result=DateUtils.formatDate(destTime, "yyyy年MM月dd月");
    		  }
    		  return result;
    	  }
    	  for(Map.Entry<Integer, String> entry : transConfigMap.entrySet()){
    		    Integer minute = entry.getKey();
    		    String desc = entry.getValue();
    		    Long minuteCha=DateUtils.pastMinutes(destTime);
    		    if(minuteCha<=minute) {
    		    	if(desc.contains("分钟")) {
    		    		result=minuteCha.intValue()+desc;
    		    	}else if(desc.contains("小时")){
    		    		result=new Long(minuteCha/60).intValue()+desc;
    		    	}else {
    		    		result=desc;
    		    	}
    		    	break;
    		    }
    		}
    	  
    	  return result;
    }
	/**
	 * @param args
	 * @throws ParseException
	 */
	public static void main(String[] args) throws ParseException {
//		System.out.println(formatDate(parseDate("2010/3/6")));
//		System.out.println(getDate("yyyy年MM月dd日 E"));
//		long time = new Date().getTime()-parseDate("2012-11-19").getTime();
//		System.out.println(time/(24*60*60*1000));
		System.out.println(isDateTypeStr("2010-03-06 23:52:53"));
	}
}
