package com.lulu.vision;

import nu.pattern.OpenCV;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.InputStream;

/**
 * @author 噜噜
 */
public class OpenCVVision {
    static { OpenCV.loadLocally(); }

    public static int[] findInMat(Mat sceneMat, String fileName) {
        String resourcePath = "imgs/" + fileName;

        // 1. 直接读取资源流到字节数组
        try (InputStream is = OpenCVVision.class.getClassLoader().getResourceAsStream(resourcePath)) {
            // 🚨 新增：如果文件找不到，一定要大声报错！
            if (is == null) {
                System.err.println("❌ [致命错误] 找不到图片文件: " + resourcePath + "，请检查文件名是不是拼错了！");
                return null;
            }

            byte[] bytes = is.readAllBytes();

            // 2. 核心：不存文件，直接在内存中解码为 Mat
            Mat template = Imgcodecs.imdecode(new MatOfByte(bytes), Imgcodecs.IMREAD_COLOR);

            if (template.empty()) {
                System.err.println("❌ 内存解码模板失败");
                return null;
            }

            // 3. 执行匹配
            Mat result = new Mat();
            Imgproc.matchTemplate(sceneMat, template, result, Imgproc.TM_CCOEFF_NORMED);
            Core.MinMaxLocResult mmr = Core.minMaxLoc(result);

            // 🚨 新增：打印出 OpenCV 算出来的真实相似度！
            //System.out.println("🎯 [视觉调试] 寻找图片 [" + fileName + "] 的最高相似度得分: " + mmr.maxVal);

            if (mmr.maxVal > com.lulu.config.Config.Global.MATCH_THRESHOLD) {
                return new int[]{
                        (int) (mmr.maxLoc.x + template.cols() / 2.0),
                        (int) (mmr.maxLoc.y + template.rows() / 2.0)
                };
            } else {
                //System.out.println("⚠️ [视觉拦截] 找到了 [" + fileName + "]，但得分(" + mmr.maxVal + ") 低于全局阈值(" + com.lulu.config.Config.Global.MATCH_THRESHOLD + ")，被判定为 false！");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 寻找图像中所有匹配的目标，并进行距离去重 (NMS)
     */
    public static java.util.List<int[]> findAllInMat(Mat sceneMat, String fileName, double threshold) {
        String resourcePath = "imgs/" + fileName;
        java.util.List<int[]> resultList = new java.util.ArrayList<>();

        try (InputStream is = OpenCVVision.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) return resultList;
            byte[] bytes = is.readAllBytes();
            Mat template = Imgcodecs.imdecode(new MatOfByte(bytes), Imgcodecs.IMREAD_COLOR);
            if (template.empty()) return resultList;

            Mat result = new Mat();
            Imgproc.matchTemplate(sceneMat, template, result, Imgproc.TM_CCOEFF_NORMED);

            // 1. 遍历结果矩阵，找出所有大于阈值的点
            java.util.List<int[]> rawPoints = new java.util.ArrayList<>();
            for (int y = 0; y < result.rows(); y++) {
                for (int x = 0; x < result.cols(); x++) {
                    if (result.get(y, x)[0] >= threshold) {
                        rawPoints.add(new int[]{
                                (int) (x + template.cols() / 2.0),
                                (int) (y + template.rows() / 2.0)
                        });
                    }
                }
            }

            // 2. 核心去重逻辑：OpenCV 会在同一个白点附近匹配出好几个高分坐标，必须合并
            for (int[] p : rawPoints) {
                boolean isDuplicate = false;
                for (int[] f : resultList) {
                    // 如果两个点距离小于 15 像素，认为是同一个白点
                    if (Math.abs(p[0] - f[0]) < 15 && Math.abs(p[1] - f[1]) < 15) {
                        isDuplicate = true;
                        break;
                    }
                }
                if (!isDuplicate) {
                    resultList.add(p);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultList;
    }


    // 新增：只返回相似度分数的方法
    // 在 OpenCVVision.java 中添加
    public static double getMatchScore(Mat sceneMat, String fileName) {
        String resourcePath = "imgs/" + fileName;
        try (InputStream is = OpenCVVision.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) return 0.0;
            byte[] bytes = is.readAllBytes();
            Mat template = Imgcodecs.imdecode(new MatOfByte(bytes), Imgcodecs.IMREAD_COLOR);
            if (template.empty()) return 0.0;
            Mat result = new Mat();
            Imgproc.matchTemplate(sceneMat, template, result, Imgproc.TM_CCOEFF_NORMED);
            Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
            return mmr.maxVal; // 返回最高相似度分数
        } catch (Exception e) { return 0.0; }
    }
}