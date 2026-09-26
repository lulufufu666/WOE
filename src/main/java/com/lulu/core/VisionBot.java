package com.lulu.core;

import com.lulu.config.Config;
import com.lulu.vision.ImageUtils;
import com.lulu.vision.OpenCVVision;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * 视觉识别引擎 (已支持游戏内 UI 动态缩放适配)
 * @author 噜噜
 */
public class VisionBot {

    // === 你的素材基准倍率 ===
    private static final double BASE_SCALE = 1.0;

    // 获取当前比例系数 (例: 用户选1.0倍, ratio为 0.666...)
    private static double getRatio() {
        return Config.Global.GAME_UI_SCALE / BASE_SCALE;
    }

    /**
     * 内部核心方法：截图并强制缩放回基准大小 (1.5倍)
     * @param realX 屏幕真实 X
     * @param realY 屏幕真实 Y
     * @param realW 屏幕真实宽度
     * @param realH 屏幕真实高度
     * @param baseW 1.5倍下的基准宽度 (传给 OpenCV 的大小)
     * @param baseH 1.5倍下的基准高度
     */
    private static Mat captureAndNormalize(int realX, int realY, int realW, int realH, int baseW, int baseH) {
        try {
            Robot robot = new Robot();
            BufferedImage screen = robot.createScreenCapture(new Rectangle(realX, realY, realW, realH));
            Mat scene = ImageUtils.bufferedImageToMat(screen);

            // 如果当前不是 1.5 倍，则强行 Resize 到 1.5 倍的尺寸，让模板图能完美匹配！
            double ratio = getRatio();
            if (Math.abs(ratio - 1.0) > 0.01) {
                Mat resizedScene = new Mat();
                Imgproc.resize(scene, resizedScene, new Size(baseW, baseH));
                return resizedScene;
            }
            return scene;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 返回目标相对于窗口左上角的相对坐标 (返回 1.5倍 基准坐标)
     */
    public static int[] getTargetPosition(String windowTitle, String imgName) {
        HWND hwnd = User32.INSTANCE.FindWindow(null, windowTitle);
        if (hwnd == null) return null;

        RECT rect = new RECT();
        User32.INSTANCE.GetWindowRect(hwnd, rect);
        int realW = rect.right - rect.left;
        int realH = rect.bottom - rect.top;

        double ratio = getRatio();
        int baseW = (int) (realW / ratio);
        int baseH = (int) (realH / ratio);

        Mat scene = captureAndNormalize(rect.left, rect.top, realW, realH, baseW, baseH);
        return OpenCVVision.findInMat(scene, imgName);
    }

    /**
     * 在指定窗口的局部区域内寻找目标，并返回相对于整个窗口的坐标 (返回 1.5倍 基准坐标)
     */
    public static int[] getTargetPositionInRegion(String windowTitle, String imgName, int roiX, int roiY, int roiW, int roiH) {
        // 这里的 roi 传入的都是 1.5 倍的基准坐标
        HWND hwnd = User32.INSTANCE.FindWindow(null, windowTitle);
        if (hwnd == null) return null;

        RECT rect = new RECT();
        User32.INSTANCE.GetWindowRect(hwnd, rect);

        double ratio = getRatio();
        // 核心：计算真实的物理截图区域
        int realX = (int) (roiX * ratio);
        int realY = (int) (roiY * ratio);
        int realW = (int) (roiW * ratio);
        int realH = (int) (roiH * ratio);

        Mat scene = captureAndNormalize(rect.left + realX, rect.top + realY, realW, realH, roiW, roiH);
        int[] pos = OpenCVVision.findInMat(scene, imgName);
        if (pos != null) {
            // 返回基于 1.5 倍的基准坐标，这样上层逻辑完全不用改
            return new int[]{roiX + pos[0], roiY + pos[1]};
        }
        return null;
    }

    /**
     * 获取屏幕上所有目标的相对坐标列表 (返回 1.5倍 基准坐标)
     */
    public static java.util.List<int[]> getAllTargetPositions(String windowTitle, String imgName, double threshold) {
        HWND hwnd = User32.INSTANCE.FindWindow(null, windowTitle);
        if (hwnd == null) return new java.util.ArrayList<>();

        RECT rect = new RECT();
        User32.INSTANCE.GetWindowRect(hwnd, rect);
        int realW = rect.right - rect.left;
        int realH = rect.bottom - rect.top;

        double ratio = getRatio();
        int baseW = (int) (realW / ratio);
        int baseH = (int) (realH / ratio);

        Mat scene = captureAndNormalize(rect.left, rect.top, realW, realH, baseW, baseH);
        return OpenCVVision.findAllInMat(scene, imgName, threshold);
    }

    /**
     * 在指定窗口的局部区域内寻找【所有】目标，并返回相对于整个窗口的坐标列表 (返回 1.5倍 基准坐标)
     */
    public static java.util.List<int[]> getAllTargetPositionsInRegion(String windowTitle, String imgName, double threshold, int roiX, int roiY, int roiW, int roiH) {
        HWND hwnd = User32.INSTANCE.FindWindow(null, windowTitle);
        if (hwnd == null) return new java.util.ArrayList<>();

        RECT rect = new RECT();
        User32.INSTANCE.GetWindowRect(hwnd, rect);

        double ratio = getRatio();
        int realX = (int) (roiX * ratio);
        int realY = (int) (roiY * ratio);
        int realW = Math.max(1, (int) (roiW * ratio));
        int realH = Math.max(1, (int) (roiH * ratio));

        Mat scene = captureAndNormalize(rect.left + realX, rect.top + realY, realW, realH, roiW, roiH);
        java.util.List<int[]> posList = OpenCVVision.findAllInMat(scene, imgName, threshold);

        for (int[] pos : posList) {
            pos[0] += roiX; // 加上的也是 1.5 倍的偏移量
            pos[1] += roiY;
        }
        return posList;
    }

    /**
     * 获取指定区域的截图 Mat (返回的是缩放到 1.5倍 基准尺寸的 Mat)
     */
    public static Mat getSceneMatInRegion(String windowTitle, int roiX, int roiY, int roiW, int roiH) {
        HWND hwnd = User32.INSTANCE.FindWindow(null, windowTitle);
        if (hwnd == null) return null;
        RECT rect = new RECT();
        User32.INSTANCE.GetWindowRect(hwnd, rect);

        double ratio = getRatio();
        int realX = (int) (roiX * ratio);
        int realY = (int) (roiY * ratio);
        // 【核心修复】防止 1x51 这种极限采样切片在缩小后变成 0 像素宽导致截图崩溃
        int realW = Math.max(1, (int) (roiW * ratio));
        int realH = Math.max(1, (int) (roiH * ratio));

        return captureAndNormalize(rect.left + realX, rect.top + realY, realW, realH, roiW, roiH);
    }
}