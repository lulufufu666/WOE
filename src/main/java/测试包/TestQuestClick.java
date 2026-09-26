package 测试包;

import com.lulu.config.Config;
import com.lulu.core.AutomationEngine;
import com.lulu.core.VisionBot;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import nu.pattern.OpenCV;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestQuestClick {

    public static void main(String[] args) throws InterruptedException {
        // 1. 初始化基础环境
        System.setProperty("sun.java2d.uiScale", "1.0");
        OpenCV.loadLocally();

        Config.Global.GAME_UI_SCALE = 1.0;
        Config.Global.MATCH_THRESHOLD = 0.8;

        System.out.println(">>> 正在探测游戏窗口: [" + Config.Global.APP_TITLE + "]");
        HWND hwnd = User32.INSTANCE.FindWindow(null, Config.Global.APP_TITLE);

        if (hwnd == null) {
            System.err.println("❌ 找不到窗口，请检查游戏是否打开。");
            return;
        }

        User32.INSTANCE.SetForegroundWindow(hwnd);
        Thread.sleep(1000);

        AutomationEngine.click(2303, 203); // 缩放为海盗图

        Thread.sleep(1000);
        // 2. 动态计算中心并初始化缩放
        RECT rect = new RECT();
        User32.INSTANCE.GetWindowRect(hwnd, rect);
        int centerX = (rect.right - rect.left) / 2;
        int centerY = (rect.bottom - rect.top) / 2;

        AutomationEngine.move(centerX, centerY);
        Thread.sleep(200);

        for (int i = 0; i < 10; i++) {
            AutomationEngine.scroll(100);
            Thread.sleep(1000);
        }

        System.out.println("✅ 地图缩放完毕，等待画面稳定...");
        Thread.sleep(1000);

        // 3. 扫描骷髅头
        int roiX = 715;
        int roiY = 276;
        int roiW = 1058;
        int roiH = 964;

        String targetQuest = "一星.png"; // 想要找的海图图标
        String genericYellowButton = "事件可交互.png";    // 黄色可交互按钮
        String specificStarButton = "一星事件激活.png";    // 一星专属激活标识

        String[] allSkulls = {"一星.png", "二星.png", "三星.png", "四星.png", "五星.png"};
        Config.Global.MATCH_THRESHOLD = 0.65;

        System.out.println(">>> 正在进行海域广角扫描...");
        List<int[]> possibleTargets = AutomationEngine.findAll(targetQuest, roiX, roiY, roiW, roiH);

        System.out.println(">>> 正在屏幕上绘制大范围 ROI (持续 3000ms)...");
        AutomationEngine.drawDebugROI(roiX, roiY, roiW, roiH, 3000);

        if (possibleTargets.isEmpty()) {
            System.err.println("❌ 扫描完毕：海域空空如也，连长得像的都没找到。");
            return;
        }

        System.out.println("✅ 粗筛发现 " + possibleTargets.size() + " 个疑似目标，进入【最高分竞选】模式...");

        List<int[]> validTargets = new ArrayList<>();
        int boxSize = 100;
        int offset = boxSize / 2;

        for (int i = 0; i < possibleTargets.size(); i++) {
            int[] pt = possibleTargets.get(i);
            int targetX = pt[0];
            int targetY = pt[1];

            int smallRoiX = targetX - offset;
            int smallRoiY = targetY - offset;

            double maxScore = 0.0;
            String realIdentity = "未知";

            for (String skullImg : allSkulls) {
                double score = AutomationEngine.getMatchScore(skullImg, smallRoiX, smallRoiY, boxSize, boxSize);
                if (score > maxScore) {
                    maxScore = score;
                    realIdentity = skullImg;
                }
            }

            if (realIdentity.equals(targetQuest)) {
                validTargets.add(pt);
            }
        }

        // ===================================================
        // 🌟 执行统一画框与交互决策
        // ===================================================
        if (!validTargets.isEmpty()) {
            System.out.println("\n🎯 鉴定完毕！本区域共找到 " + validTargets.size() + " 个真实的 [" + targetQuest + "]。");
            System.out.println(">>> 正在同时高亮所有目标，维持 5 秒...");

            for (int[] target : validTargets) {
                AutomationEngine.drawDebugROI(target[0] - offset, target[1] - offset, boxSize, boxSize, 5000);
            }
            Thread.sleep(5000);

            // ===================================================
            // 🌟 核心升级：事件策略映射表配置
            // ===================================================
            // key: 事件图片名字, value: 该事件触发后你要点击的操作按钮图片名字（例如：战斗.png、付费.png等）
            Map<String, String> eventActionMap = new HashMap<>();
            eventActionMap.put("一星士兵事件.png", "绕过.png");
            eventActionMap.put("一星治疗师事件.png", "忽略.png");
            eventActionMap.put("一星炼金师事件.png", "绕过.png");
            eventActionMap.put("一星附魔剑事件.png", "冒险.png");
            // 你后续如果还有 等待.png、忽略.png，随时可以往这里 put 增加策略！

            System.out.println(">>> 开始执行清剿与事件智能决策交互...");
            for (int i = 0; i < validTargets.size(); i++) {
                int[] target = validTargets.get(i);
                int targetX = target[0];
                int targetY = target[1];

                // 1. 点击骷髅头，唤起弹窗
                System.out.println("\n>>> [第 " + (i + 1) + " 个目标] 点击骷髅头，唤起交互面板...");
                AutomationEngine.click(targetX, targetY);
                Thread.sleep(1000);

                // 2. 检查黄色按钮与一星标识
                int squareSize = 900;
                int halfSize = squareSize / 2;
                int btnRoiX = Math.max(0, targetX - halfSize);
                int btnRoiY = Math.max(0, targetY - halfSize);
                int btnRoiW = squareSize;
                int btnRoiH = squareSize;

                AutomationEngine.drawDebugROI(btnRoiX, btnRoiY, btnRoiW, btnRoiH, 1000);

                int[] yellowBtnPos = VisionBot.getTargetPositionInRegion(Config.Global.APP_TITLE, genericYellowButton, btnRoiX, btnRoiY, btnRoiW, btnRoiH);
                double yellowScore = AutomationEngine.getMatchScore(genericYellowButton, btnRoiX, btnRoiY, btnRoiW, btnRoiH);
                double starScore = AutomationEngine.getMatchScore(specificStarButton, btnRoiX, btnRoiY, btnRoiW, btnRoiH);

                // 3. 如果是黄色可交互 且 确认为一星
                if (yellowScore >= 0.75 && starScore >= 0.75 && yellowBtnPos != null) {
                    System.out.println("     ✅ 确认是一星可交互任务，点击黄色按钮进入事件详情页！");

                    // 【核心步骤】：点击黄色按钮交互进去
                    AutomationEngine.click(yellowBtnPos);

                    // 等待 1.5 秒，让游戏页面完全切换到事件详情页
                    Thread.sleep(1500);

                    // ===================================================
                    // 🌟 统一大范围面板交互区 (ROI 覆盖整个事件弹窗)
                    // ===================================================
                    int panelRoiX = 693;
                    int panelRoiY = 370;
                    int panelRoiW = 1086;
                    int panelRoiH = 798;

                    // 绘制这个大面板的调试红框，让你看清它是不是把整个弹窗都完美框住了 (停留 1 秒)
                    AutomationEngine.drawDebugROI(panelRoiX, panelRoiY, panelRoiW, panelRoiH, 1000);

                    System.out.println("     🔍 正在大面板内识别具体的一星事件类型...");

                    String detectedEvent = "未知事件";
                    double maxEventScore = 0.0;

                    // 1. 在大面板内遍历所有一星事件图鉴，找出得分最高的那一个
                    for (String eventImg : eventActionMap.keySet()) {
                        double score = AutomationEngine.getMatchScore(eventImg, panelRoiX, panelRoiY, panelRoiW, panelRoiH);
                        if (score > maxEventScore) {
                            maxEventScore = score;
                            detectedEvent = eventImg;
                        }
                    }

                    System.out.println("     -> 事件识别结果: [" + detectedEvent + "] (匹配得分: " + String.format("%.3f", maxEventScore) + ")");

                    // 2. 策略命中后，在大面板内寻找对应的操作按钮并点击
                    if (maxEventScore >= 0.75 && eventActionMap.containsKey(detectedEvent)) {
                        String targetActionBtn = eventActionMap.get(detectedEvent);
                        System.out.println("     🎯 策略命中！该事件对应操作按钮为: [" + targetActionBtn + "]");

                        // 在同一个大面板范围内，寻找目标操作按钮（如：战斗.png、付费.png等）的精确坐标
                        int[] actionBtnPos = VisionBot.getTargetPositionInRegion(Config.Global.APP_TITLE, targetActionBtn, panelRoiX, panelRoiY, panelRoiW, panelRoiH);

                        if (actionBtnPos != null) {
                            System.out.println("     🚀 正在点击操作按钮执行: " + targetActionBtn);
                            AutomationEngine.click(actionBtnPos);

                            // ===================================================
                            // 🌟 核心升级：多轮动态轮询结果（支持：绕过失败、忽略提示、存货成功）
                            // ===================================================
                            System.out.println("     ⏳ 操作指令已下发，开始多轮检测交互结果...");

                            boolean isSuccess = false;
                            int maxRetry = 6; // 最多检测 6 轮（每轮 0.6 秒，总共约 3.5 秒）

                            for (int retry = 1; retry <= maxRetry; retry++) {
                                Thread.sleep(600); // 等待游戏响应或动画弹出

                                // 1. 检测大面板内是否有【绕过失败.png】
                                double failScore = AutomationEngine.getMatchScore("绕过失败.png", panelRoiX, panelRoiY, panelRoiW, panelRoiH);
                                if (failScore >= 0.75) {
                                    System.err.println("     ❌ 第 " + retry + " 轮检测：触发了【绕过失败.png】！正在寻找关闭按钮...");

                                    // 优先寻找“关闭弹窗.png”点击，如果没找到则点原骷髅头兜底
                                    clickCloseButtonOrFallback(panelRoiX, panelRoiY, panelRoiW, panelRoiH, targetX, targetY);
                                    break; // 结束本轮任务
                                }

                                // 2. 检测大面板内是否有【忽略提示.png】
                                double ignoreScore = AutomationEngine.getMatchScore("忽略提示.png", panelRoiX, panelRoiY, panelRoiW, panelRoiH);
                                if (ignoreScore >= 0.75) {
                                    System.out.println("     ℹ️ 第 " + retry + " 轮检测：触发了【忽略提示.png】！正在寻找关闭按钮...");

                                    // 同样寻找“关闭弹窗.png”关闭窗口
                                    clickCloseButtonOrFallback(panelRoiX, panelRoiY, panelRoiW, panelRoiH, targetX, targetY);
                                    break; // 结束本轮任务
                                }

                                // 3. 全局寻找【存货.png】判定是否成功获得奖励
                                if (AutomationEngine.exists("存货.png")) {
                                    System.out.println("     🎉 检测到【存货.png】！恭喜，成功获得奖励！");
                                    AutomationEngine.click("存货.png");
                                    isSuccess = true;
                                    break; // 成功，结束本轮任务
                                }

                                System.out.println("     🔄 第 " + retry + " 轮检测中：服务器结算中...");
                            }

                            if (!isSuccess) {
                                System.out.println("     ℹ️ 本次交互分支已完成（可能因失败/忽略已关闭弹窗）。");
                            }

                        } else {
                            System.err.println("     ⚠️ 认出了事件是 [" + detectedEvent + "]，但在面板内没找到按钮 [" + targetActionBtn + "]！");
                            // 找不到操作按钮时的兜底：直接点关闭按钮或原骷髅头
                            clickCloseButtonOrFallback(panelRoiX, panelRoiY, panelRoiW, panelRoiH, targetX, targetY);
                        }

                    } else {
                        System.err.println("     ⚠️ 未能成功识别具体一星事件类型（或得分过低），尝试关闭/返回...");
                        // 识别失败兜底关闭
                        AutomationEngine.click(targetX, targetY);
                    }

                    // 动作执行完毕，等待 2 秒进入下一个任务循环
                    Thread.sleep(2000);

                } else {
                    System.out.println("     ⚠️ 状态判定：按钮是灰色不可交互（或非一星），点击原骷髅头坐标关闭弹窗...");
                    AutomationEngine.click(targetX, targetY);
                    Thread.sleep(1000);
                }
            }

            System.out.println("\n🎉 所有目标遍历与事件智能决策完毕！");

        } else {
            System.err.println("\n⚠️ 鉴定完毕：屏幕上所有的疑似目标中，没有一个是我们要找的 [" + targetQuest + "]。");
        }

    }

    /**
     * 辅助方法：在大面板区域内寻找“关闭弹窗.png”并点击，如果找不到则点击原骷髅头坐标兜底
     */
    private static void clickCloseButtonOrFallback(int roiX, int roiY, int roiW, int roiH, int fallbackX, int fallbackY) {
        try {
            // 在大面板范围内寻找叉叉按钮
            int[] closeBtnPos = VisionBot.getTargetPositionInRegion(Config.Global.APP_TITLE, "关闭弹窗.png", roiX, roiY, roiW, roiH);
            double closeScore = AutomationEngine.getMatchScore("关闭弹窗.png", roiX, roiY, roiW, roiH);

            if (closeScore >= 0.75 && closeBtnPos != null) {
                System.out.println("     ✔️ 成功识别到【关闭弹窗.png】（叉叉），正在精准点击关闭...");
                AutomationEngine.click(closeBtnPos);
            } else {
                System.out.println("     ⚠️ 未能识别到叉叉按钮，执行原位兜底点击关闭...");
                AutomationEngine.click(fallbackX, fallbackY);
            }
            Thread.sleep(1000); // 等待窗口关闭动画
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}