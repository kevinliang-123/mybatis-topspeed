package com.tengjie.common.utils;

public class MyStringBuffer   {
	StringBuffer sb=new StringBuffer();
	public MyStringBuffer append(String ...args) {
		for(String arg:args){
			sb.append(arg);
		}
		return this;
	}
	public MyStringBuffer appendH(String ...args) {
		for(String arg:args){
			sb.append(arg);
		}
		sb.append(");\n");
		return this;
	}
	public MyStringBuffer appendK(String ...args) {
		for(String arg:args){
			sb.append(arg);
		}
		sb.append("\n");
		return this;
	}
	public MyStringBuffer appendM(String ...args) {
		for(String arg:args){
			sb.append(arg);
		}
		sb.append(";\n");
		return this;
	}
	public boolean isEmpty(){
		boolean empty=false;
		if(StringUtils.isEmpty(sb.toString())){
			empty=true;
		}
		return empty;
	}
	public void insertBlankLine(){
		sb.append("\n");
	}
	public void printInfo(String info){
		sb.append("System.out.println(\""+info+"\");"+"\n");
	}
	public static String addQuote(String var){
		return "\""+var+"\"";
	}
	public static String addSingleQuote(String var){
		return "'"+var+"'";
	}
	public void clear() {
		sb.delete(0,sb.length());
	}
	
	public static final String COMMA=",";//逗号
	public static final String SEMICOLON=";";//分号
	public String toString() {
		return sb.toString();
	}
	public static MyStringBuffer newInstance(){
		return new MyStringBuffer();
	}
}
