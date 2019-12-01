package com.tengjie.common.utils;

/**
 * 文件描述
 * @author wangxipeng
 *
 */
public class FileDescEntity {

	String saveName;//保存名
	String originalName;//原始名
	String fileContentType;
	String fileExtend;//文件后缀.docx .txt .jpg等
	String url;
	String contentType;//内容类型 如txt为 txt/plain
	int fileSize;
	String targetType;//1本地2云服务器
	
	
	 
	public String getTargetType() {
		return targetType;
	}

	public void setTargetType(String targetType) {
		this.targetType = targetType;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getSaveName() {
		return saveName;
	}

	public void setSaveName(String saveName) {
		this.saveName = saveName;
	}

	public String getOriginalName() {
		return originalName;
	}

	public void setOriginalName(String originalName) {
		this.originalName = originalName;
	}

	public String getFileContentType() {
		return fileContentType;
	}

	public void setFileContentType(String fileContentType) {
		this.fileContentType = fileContentType;
	}

	public String getFileExtend() {
		return fileExtend;
	}

	public void setFileExtend(String fileExtend) {
		this.fileExtend = fileExtend;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public int getFileSize() {
		return fileSize;
	}

	public void setFileSize(int fileSize) {
		this.fileSize = fileSize;
	}

}
