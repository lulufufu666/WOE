package com.lulu.tool;

import com.lulu.config.Config;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;

import javax.imageio.ImageIO;
import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 游戏内指定 ROI 区域截图提取工具
 */
public class ROICaptureTool {

    public static void main(String[] args) {
        System.setProperty("sun.java2d.uiScale", "1.0");
        // ==========================================
        // 👉 在这里填入你用 MouseTracker 测出来的坐标
        // ==========================================
        int roiX = 1124;  // 替换为你的 ROI_X
        int roiY = 565;  // 替换为你的 ROI_Y
        int roiW = 1;   // 替换为你的 ROI_W
        int roiH = 51;   // 替换为你的 ROI_H
        
        // 给你要截取的品质取个名字，比如 "Legendary" (传说)
        String qualityName = "超凡";
        
        System.out.println(">>> 准备截取目标区域...");
        captureGameROI(roiX, roiY, roiW, roiH, qualityName);
    }

    /**
     * 根据相对坐标截取游戏画面并保存为 PNG
     */
    public static void captureGameROI(int relX, int relY, int width, int height, String fileNamePrefix) {
        // 1. 寻找游戏窗口
        HWND hwnd = User32.INSTANCE.FindWindow(null, Config.Global.APP_TITLE);
        if (hwnd == null) {
            System.err.println("❌ 找不到游戏窗口，请确保游戏正在运行！");
            return;
        }

        // 2. 将窗口激活到前台，确保截不到被遮挡的画面
        User32.INSTANCE.SetForegroundWindow(hwnd);
        try {
            Thread.sleep(500); // 留出 0.5 秒让窗口弹出来
        } catch (InterruptedException ignored) {}

        // 3. 获取窗口当前的绝对位置
        RECT rect = new RECT();
        User32.INSTANCE.GetWindowRect(hwnd, rect);

        // 4. 将你传入的相对坐标，换算成屏幕绝对坐标
        int absX = rect.left + relX;
        int absY = rect.top + relY;

        try {
            // 5. 执行屏幕截图
            Robot robot = new Robot();
            Rectangle captureRect = new Rectangle(absX, absY, width, height);
            BufferedImage screenCapture = robot.createScreenCapture(captureRect);

            // 6. 生成带时间戳的文件名，防止文件被覆盖
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String finalFileName = fileNamePrefix + "_" + timestamp + ".png";
            File outputFile = new File(finalFileName);

            // 7. 保存到本地
            ImageIO.write(screenCapture, "png", outputFile);
            System.out.println("✅ 截图成功！文件已保存至: " + outputFile.getAbsolutePath());

        } catch (AWTException e) {
            System.err.println("❌ Robot 初始化失败，截图环境受限: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("❌ 图片文件保存失败: " + e.getMessage());
        }
    }
}