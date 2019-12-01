package com.tengjie.common.utils;

/**
 * Created by hzl on 2017/7/6.
 */
public final class GlobalUtils {
    //失败
    public static final String fail = "00";
    //成功
    public static final String  suc = "01";
    //提示消息
    public static final String  prompt = "02";
    //token失效
    public static final String  tokenExp = "03";
    //签名错误
    public static final String signExp = "04";
    //时间戳超时
    public static final String timeExp = "05";
    //不合法参数
    public static final String validExp = "06";
    //验证码已失效
    public static final String  codeExp = "07";
    //未注册
    public static final String  regExp = "08";
    //已注册
    public static final String  regedExp = "09";
    public static final String  codeError = "10";



    //系统内部错误
    public static final String failMsg = "系统内部错误!";
    public static final String  promptMsg = "报文参数错误!";
    public static final String  sucMsg = "请求成功!";
    public static final String  tokenMsg = "Token失效,重新登录!";
    public static final String  signMsg = "访问签名错误!";
    public static final String  timestampMsg = "时间戳超过30分钟!";
    public static final String   validMsg = "不合法参数";
    public static final String  codeMsg = "验证码失效!";
    public static final String  regMsg= "该帐号未注册!";
    public static final String  regedMsg= "该帐号已经注册!";
    public static final String  codeErrorMsg= "验证码错误!";
    public static final String  pwdMsg= "房间密码错误!";
    public static final String  typeMsg = "操作类型不正确!";

    /**************************************************************************/

    /*返回status状态码 start*/
    //成功返回状态。
    public static final String status200 = "200";
    //用户发出的请求有误，服务器没有进行数据的操作。
    public static final String status400 = "400";
    //用户发出的请求针对的是不存在的记录，服务器没有进行操作。
    public static final String status404 = "404";
    //服务器发生错误，操作失败。
    public static final String status500 = "500";
    //手机号未注册
    public static final String status501 = "501";
    //密码错误
    public static final String status502 = "502";
    //验证码发送频繁
    public static final String status503 = "503";
    //验证码错误
    public static final String status504 = "504";
    //用户未注册
    public static final String status505 = "505";
    //密码错误
    public static final String status506 = "506";
    //用户权限验证失败
    public static final String status4001 = "4001";
    //用户权限不足
    public static final String status4002= "4002";
    //账号另一处登录
    public static final String status5001= "5001";
    /*返回status状态码 end*/

    /*短信类型 start*/
    //01：用户注册
    public static final String messType01 = "01";
    //02：用户验证码登录
    public static final String messType02 = "02";
    //03：修改密码
    public static final String messType03 = "03";
    //04：添加银行卡
    public static final String messType04 = "04";
    //05：第三方绑定手机号
    public static final String messType05 = "05";
    /*短信类型 end*/

    /*用户类型 01：房客02：房主start*/
    public static final String userType01="01";
    public static final String userType02="02";
    /*用户类型 end*/

    /*公共是否 1：是2：否start*/
    public static final String isType1="1";
    public static final String isType2="2";
    /*公共是否 end*/

    /*原来ErrorCodeConstant中的内容，具体错误的细项，现有statuscode，然后这个是详细的错误码描述*/
    public static final String ERROR_CODE = "16";
	public static final String ERROR_CODEPhone = "1";
	public static final String ERROR_CODEEmail = "2";
	public static final String ERROR_CODENumber = "3";
	public static final String ERROR_CODEFindIMEI = "5";
	public static final String ERROR_CODEFindUser = "4";
	public static final String ERROR_CODEFindOldIMEI = "6";
	public static final String ERROR_CODEFindPhoneUser = "7";
	public static final String ERROR_CODEFindCity = "8";
	public static final String ERROR_CODEFindPoint = "9";
	public static final String ERROR_CODEExitIMEI = "10";
	public static final String ERROR_CODEFeedback = "11";
	public static final String ERROR_CODEOldPhone = "12";
	public static final String ERROR_CODENewPhone = "13";
	public static final String ERROR_CODEOldEmail = "14";
	public static final String ERROR_CODENewEmail = "15";
	public static final String ERROR_CODEErrorPhone = "17";
	public static final String ERROR_CODEInformation= "18";

}
