package com.lulu.vision;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

/**
 * @author 噜噜
 */
public class ImageUtils {
    /**
     * 将 AWT 的 BufferedImage 转为 OpenCV 的 Mat
     */
    public static Mat bufferedImageToMat(BufferedImage bi) {
        // 将图片转换为 BGR 格式 (OpenCV 默认格式)
        BufferedImage temp = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        temp.getGraphics().drawImage(bi, 0, 0, null);

        byte[] pixels = ((DataBufferByte) temp.getRaster().getDataBuffer()).getData();
        Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
        mat.put(0, 0, pixels);
        return mat;
    }
}