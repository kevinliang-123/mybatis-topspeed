package com.tengjie.common.utils;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import javax.imageio.ImageIO;

import com.swetake.util.Qrcode;

/**
 * 生成二维码工具
 * 
 */
public class QrCodeUtil {
	
	public static void main(String[] args) {
		byte[] bb = createQRCode("www.baidu.com", null);
		System.out.println(bb);
	}
	/**
	 * @Description： 生成二维码
	 * @param:qrUrl二维码url
	 * @param:logoUrl 
	 * @author: lijiafu
	 * @since: 2016年3月2日 下午6:25:15
	 */
	public static byte[] createQRCode(String qrUrl,String logo) {
		byte[] data = null;
		try {
			Qrcode qrcodeHandler = new Qrcode();
			qrcodeHandler.setQrcodeErrorCorrect('M');
			qrcodeHandler.setQrcodeEncodeMode('B');
			qrcodeHandler.setQrcodeVersion(7);
			byte[] contentBytes = qrUrl.getBytes("gb2312");
			// 构造一个BufferedImage对象 设置宽、高
			BufferedImage bufImg = new BufferedImage(140, 140,
					BufferedImage.TYPE_INT_RGB);
			Graphics2D gs = bufImg.createGraphics();
			gs.setBackground(Color.white);
			gs.clearRect(0, 0, 140, 140);
			// 设定图像颜色 > BLACK
			gs.setColor(Color.BLACK);
			// 设置偏移量 不设置可能导致解析出错
			int pixoff = 2;
			// 输出内容 > 二维码
			if (contentBytes.length > 0 && contentBytes.length < 120) {
				boolean[][] codeOut = qrcodeHandler.calQrcode(contentBytes);
				for (int i = 0; i < codeOut.length; i++) {
					for (int j = 0; j < codeOut.length; j++) {
						if (codeOut[j][i]) {
							gs.fillRect(j * 3 + pixoff, i * 3 + pixoff, 3, 3);
						}
					}
				}

			} else {
				System.err.println("QRCode content bytes length = "
						+ contentBytes.length + " not in [ 0,120 ]. ");
				return null;
			}
			if(logo !=null){
				Image img = ImageIO.read(new File(logo));// 实例化一个Image对象。
				gs.drawImage(img, 55, 55, 30, 30, null);//logo位置
			}
		
			gs.dispose();
			bufImg.flush();
			// 生成二维码QRCode图片
			ByteArrayOutputStream bsout = new ByteArrayOutputStream();
			ImageIO.write(bufImg, "png", bsout);
			data = bsout.toByteArray();
			// ImageIO.write(bufImg, "png", imgFile);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return data;
	}
}
