package com.lulu.core;

import com.lulu.config.Config;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;

/**
 * 锚点执行引擎：统一门面，支持多种点击方式重载 (已支持游戏缩放自适应)
 * @author 噜噜
 */
public class AutomationEngine {

    // === 新增：统一比例计算器 ===
    private static double getRatio() {
        // 将当前游戏 UI 缩放除以基准缩放 (1.5)
        return Config.Global.GAME_UI_SCALE;
    }

    // === 新增：基准坐标 -> 真实绝对屏幕坐标 转换器 ===
    private static int[] toAbsoluteAndScale(int relX, int relY) {
        HWND hwnd = User32.INSTANCE.FindWindow(null, Config.Global.APP_TITLE);
        // 如果找不到窗口，直接按原坐标比例返回，防止空指针报错
        if (hwnd == null) {
            return new int[]{(int)(relX * getRatio()), (int)(relY * getRatio())};
        }

        RECT rect = new RECT();
        User32.INSTANCE.GetWindowRect(hwnd, rect);

        double ratio = getRatio();
        // 1. 将 1.5倍的相对坐标 缩放为 真实相对坐标
        int realX = (int) (relX * ratio);
        int realY = (int) (relY * ratio);

        // 2. 加上窗口的绝对位置
        return new int[]{rect.left + realX, rect.top + realY};
    }
    public static void move(int relX, int relY) {
        // 自动将你传入的相对坐标，换算成屏幕绝对坐标
        int[] abs = toAbsoluteAndScale(relX, relY);

        // 调用底层硬件级移动
        HardwareBot.moveMouse(abs[0], abs[1]);
        System.out.println("移动鼠标 -> 相对坐标:(" + relX + "," + relY + ") 绝对坐标:(" + abs[0] + "," + abs[1] + ")");
    }

    /**
     * 方式 1：视觉驱动点击
     */
    public static boolean click(String imgName) {
        // VisionBot 返回的是 1.5倍基准坐标，传给 click(pos) 会被自动缩放，逻辑完美闭环
        int[] pos = VisionBot.getTargetPosition(Config.Global.APP_TITLE, imgName);
        return click(pos);
    }

    /**
     * 方式 2：偏移坐标点击 (核心：加入窗口换算逻辑与缩放适配)
     */
    public static boolean click(int relX, int relY) {
        int[] abs = toAbsoluteAndScale(relX, relY);

        // 调用底层硬件点击
        HardwareBot.clickAt(abs[0], abs[1]);
        System.out.println("点击坐标 -> 基准相对:(" + relX + "," + relY + ") 真实绝对:(" + abs[0] + "," + abs[1] + ")");
        return true;
    }

    /**
     * 方式 3：数组偏移点击
     */
    public static boolean click(int[] pos) {
        if (pos == null || pos.length < 2) {
            return false;
        }
        return click(pos[0], pos[1]);
    }

    /**
     * 重载 2：指定区域视觉点击
     */
    public static boolean click(String imgName, int roiX, int roiY, int roiW, int roiH) {
        int[] pos = VisionBot.getTargetPositionInRegion(Config.Global.APP_TITLE, imgName, roiX, roiY, roiW, roiH);
        return click(pos);
    }

    // ... 在 AutomationEngine 类中找个位置添加以下代码 ...

    /**
     * 方式 4：偏移坐标双击 (用于双击舰队聚焦)
     */
    public static boolean doubleClick(int relX, int relY) {
        int[] abs = toAbsoluteAndScale(relX, relY);
        HardwareBot.moveMouse(abs[0], abs[1]);
        try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        HardwareBot.clickLeftMouse();
        try { Thread.sleep(60); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        HardwareBot.clickLeftMouse();

        System.out.println("双击坐标 -> 基准相对:(" + relX + "," + relY + ") 真实绝对:(" + abs[0] + "," + abs[1] + ")");
        return true;
    }

    public static boolean doubleClick(int[] pos) {
        if (pos == null || pos.length < 2) return false;
        return doubleClick(pos[0], pos[1]);
    }

    /**
     * 纯状态检查 (视觉识别)
     */
    public static boolean exists(String imgName) {
        return VisionBot.getTargetPosition(Config.Global.APP_TITLE, imgName) != null;
    }

    public static boolean exists(String imgName, int roiX, int roiY, int roiW, int roiH) {
        return VisionBot.getTargetPositionInRegion(Config.Global.APP_TITLE, imgName, roiX, roiY, roiW, roiH) != null;
    }

    // ==================== 键盘指令 ====================
    public static void press(int vKey) {
        HardwareBot.pressKey(vKey);
    }

    // ==================== 拖拽指令 ====================

    /**
     * 核心拖拽逻辑：支持相对坐标与缩放换算
     */
    public static void drag(int relX1, int relY1, int relX2, int relY2) {
        int[] start = toAbsoluteAndScale(relX1, relY1);
        int[] end = toAbsoluteAndScale(relX2, relY2);

        // 调用硬件驱动的平滑拖拽
        HardwareBot.dragTo(start[0], start[1], end[0], end[1]);
        System.out.println("拖拽动作 -> 基准起点:(" + relX1 + "," + relY1 + ") 基准终点:(" + relX2 + "," + relY2 + ")");
    }

    /**
     * 【修复】通过坐标数组进行拖拽
     */
    public static void drag(int[] startPos, int[] endPos) {
        if (startPos == null || endPos == null || startPos.length < 2 || endPos.length < 2) {
            System.err.println("❌ 拖拽坐标数组为空或长度不足！");
            return;
        }
        drag(startPos[0], startPos[1], endPos[0], endPos[1]);
    }

    public static void scroll(int delta) {
        HardwareBot.mouseWheel(delta);
    }

    /**
     * 获取当前屏幕上所有该图片的相对坐标列表
     */
    public static java.util.List<int[]> findAll(String imgName) {
        // 修复：使用系统设置中的全局动态阈值
        return VisionBot.getAllTargetPositions(
                Config.Global.APP_TITLE,
                imgName,
                Config.Global.MATCH_THRESHOLD
        );
    }

    /**
     * 在指定 ROI 区域内获取所有匹配目标的坐标列表
     */
    public static java.util.List<int[]> findAll(String imgName, int roiX, int roiY, int roiW, int roiH) {
        // 修复：使用系统设置中的全局动态阈值
        return VisionBot.getAllTargetPositionsInRegion(
                Config.Global.APP_TITLE,
                imgName,
                Config.Global.MATCH_THRESHOLD,
                roiX, roiY, roiW, roiH
        );
    }

    /**
     * 【新增】获取指定区域内模板图片的匹配度得分 (已自动进行缩放适配)
     */
    public static double getMatchScore(String imgName, int roiX, int roiY, int roiW, int roiH) {
        // 1. 调用 VisionBot 截取指定区域。
        // VisionBot 内部已经处理好了缩放适配，返回的是强行 Resize 回 1.5倍 的 Mat 图片
        org.opencv.core.Mat sceneMat = VisionBot.getSceneMatInRegion(Config.Global.APP_TITLE, roiX, roiY, roiW, roiH);

        if (sceneMat == null) {
            return 0.0;
        }

        // 2. 交给底层的 OpenCV 进行打分匹配
        return com.lulu.vision.OpenCVVision.getMatchScore(sceneMat, imgName);
    }

    /**
     * 【新增】统一门面：获取指定区域的原始图像矩阵 (已自动进行缩放适配)，供防爆仓等高级校验使用
     */
    public static org.opencv.core.Mat getRawMat(int roiX, int roiY, int roiW, int roiH) {
        return VisionBot.getSceneMatInRegion(Config.Global.APP_TITLE, roiX, roiY, roiW, roiH);
    }

    /**
     * 【调试利器】在屏幕上画出一个红色的 ROI 区域框 (加入缩放适配)
     */
    public static void drawDebugROI(int roiX, int roiY, int roiW, int roiH, int durationMs) {
        // 🌟 核心拦截：如果调试模式关闭，直接跳过，什么都不画！
        if (!Config.Global.DEBUG_MODE) {
            return;
        }
        HWND hwnd = User32.INSTANCE.FindWindow(null, Config.Global.APP_TITLE);
        if (hwnd == null) return;
        RECT rect = new RECT();
        User32.INSTANCE.GetWindowRect(hwnd, rect);

        double ratio = getRatio();

        // 画在屏幕上，所以宽、高、X、Y 统统要根据比例缩放到真实的物理大小
        int realX = (int) (roiX * ratio);
        int realY = (int) (roiY * ratio);
        int realW = (int) (roiW * ratio);
        int realH = (int) (roiH * ratio);

        int absX = rect.left + realX;
        int absY = rect.top + realY;

        javax.swing.SwingUtilities.invokeLater(() -> {
            javax.swing.JWindow window = new javax.swing.JWindow();
            window.setBounds(absX, absY, realW, realH); // 使用真实物理宽高
            window.setAlwaysOnTop(true);
            window.setBackground(new java.awt.Color(0, 0, 0, 0));

            javax.swing.JPanel panel = new javax.swing.JPanel() {
                @Override
                protected void paintComponent(java.awt.Graphics g) {
                    super.paintComponent(g);
                    java.awt.Graphics2D g2d = (java.awt.Graphics2D) g;
                    g2d.setColor(java.awt.Color.RED);
                    g2d.setStroke(new java.awt.BasicStroke(4));
                    g2d.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                }
            };
            panel.setOpaque(false);
            window.add(panel);
            window.setVisible(true);

            new Thread(() -> {
                try { Thread.sleep(durationMs); } catch (InterruptedException ignored) {}
                window.dispose();
            }).start();
        });
    }

    /**
     * 【调试利器】在屏幕上画出一个红色的圆形/椭圆形边框
     */
    public static void drawDebugOval(int roiX, int roiY, int roiW, int roiH, int durationMs) {
        // 🌟 核心拦截：如果调试模式关闭，直接跳过，什么都不画！
        if (!Config.Global.DEBUG_MODE) {
            return;
        }
        HWND hwnd = User32.INSTANCE.FindWindow(null, Config.Global.APP_TITLE);
        if (hwnd == null) return;
        RECT rect = new RECT();
        User32.INSTANCE.GetWindowRect(hwnd, rect);

        double ratio = getRatio();

        int realX = (int) (roiX * ratio);
        int realY = (int) (roiY * ratio);
        int realW = (int) (roiW * ratio);
        int realH = (int) (roiH * ratio);

        int absX = rect.left + realX;
        int absY = rect.top + realY;

        javax.swing.SwingUtilities.invokeLater(() -> {
            javax.swing.JWindow window = new javax.swing.JWindow();
            window.setBounds(absX, absY, realW, realH);
            window.setAlwaysOnTop(true);
            window.setBackground(new java.awt.Color(0, 0, 0, 0));

            javax.swing.JPanel panel = new javax.swing.JPanel() {
                @Override
                protected void paintComponent(java.awt.Graphics g) {
                    super.paintComponent(g);
                    java.awt.Graphics2D g2d = (java.awt.Graphics2D) g;
                    g2d.setColor(java.awt.Color.RED);
                    g2d.setStroke(new java.awt.BasicStroke(4));
                    // 🌟 这里使用的是 drawOval，专门用来画圆圈
                    g2d.drawOval(0, 0, getWidth() - 1, getHeight() - 1);
                }
            };
            panel.setOpaque(false);
            window.add(panel);
            window.setVisible(true);

            new Thread(() -> {
                try { Thread.sleep(durationMs); } catch (InterruptedException ignored) {}
                window.dispose();
            }).start();
        });
    }
    /**
     * 动态轮询检测某个图像目标，直到出现或超时
     */
    public static int[] pollForTarget(String templateName, int x, int y, int w, int h, double threshold, int timeoutMs, int intervalMs) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            int[] pos = VisionBot.getTargetPositionInRegion(Config.Global.APP_TITLE, templateName, x, y, w, h);
            double score = getMatchScore(templateName, x, y, w, h);
            if (pos != null && score >= threshold) {
                return pos;
            }
            Thread.sleep(intervalMs);
        }
        return null;
    }

    /**
     * 【新增】获取指定相对坐标处的像素颜色 (已自动适配缩放)
     */
    public static java.awt.Color getPixelColor(int relX, int relY) {
        int[] abs = toAbsoluteAndScale(relX, relY);
        try {
            java.awt.Robot robot = new java.awt.Robot();
            return robot.getPixelColor(abs[0], abs[1]);
        } catch (Exception e) {
            e.printStackTrace();
            return java.awt.Color.BLACK;
        }
    }
}