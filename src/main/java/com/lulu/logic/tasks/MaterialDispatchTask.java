package com.lulu.logic.tasks;

import com.lulu.config.Config;
import com.lulu.core.AutomationEngine;
import com.lulu.logic.BotTask;
import com.lulu.tool.LogManager;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class MaterialDispatchTask implements BotTask {

    private final Map<String, Integer> finalMaterials;
    private final List<Integer> targetFleetIndices;
    private final String dispatchSpeed;
    private final int totalFleets;

    public MaterialDispatchTask(Map<String, Integer> finalMaterials, List<Integer> targetFleetIndices, String dispatchSpeed) {
        this.finalMaterials = finalMaterials;
        this.targetFleetIndices = targetFleetIndices;
        this.dispatchSpeed = dispatchSpeed;
        this.totalFleets = Config.FleetConfig.TASK_MAP.size();
    }

    @Override
    public void execute() throws InterruptedException {
        if (finalMaterials.isEmpty()) {
            LogManager.print("❌ [配送中止] 实际装车清单为空，无需配送！");
            return;
        }
        LogManager.print("📋 [最终装车清单确认]: " + finalMaterials);
        LogManager.print("📦 [配置读取] 当前选择的配送速度为: " + dispatchSpeed);

        LogManager.print("⚙️ [前置初始化] 正在点击固定坐标...");
        int initX1 = 2141, initY1 = 203;
        AutomationEngine.click(initX1, initY1);
        Thread.sleep(Config.CruiseConfig.enableLowEndMode ? 1000 : 500);

        int initX2 = 2226, initY2 = 204;
        AutomationEngine.click(initX2, initY2);
        Thread.sleep(Config.CruiseConfig.enableLowEndMode ? 1200 : 600);

        LogManager.print("🧹 [初始清理] 正在检测并关闭残留弹窗...");
        for (int i = 0; i < 3; i++) {
            if (AutomationEngine.exists("关闭弹窗.png")) {
                AutomationEngine.click("关闭弹窗.png");
                Thread.sleep(Config.CruiseConfig.enableLowEndMode ? 1000 : 500);
            } else break;
        }

        int panelX = 35, panelY = 249, panelW = 436, panelH = 792;
        int roiX = 664, roiY = 188, roiW = 1072, roiH = 1010;

        List<String> fleetNames = new ArrayList<>(Config.FleetConfig.TASK_MAP.keySet());
        int globalIndex = 0;

        while (globalIndex < totalFleets) {
            List<int[]> visibleFleets = AutomationEngine.findAll("舰队图标.png", panelX, panelY, panelW, panelH);
            if (visibleFleets.isEmpty()) {
                LogManager.print("❌ 未在左侧列表找到舰队图标，补给任务中止。");
                break;
            }

            visibleFleets.sort(Comparator.comparingInt(pos -> pos[1]));
            int lastFleetY = visibleFleets.get(visibleFleets.size() - 1)[1];
            int fleetX = visibleFleets.get(0)[0];

            for (int i = 0; i < visibleFleets.size(); i++) {
                if (globalIndex >= totalFleets) break;

                int[] fleetPos = visibleFleets.get(i);
                String currentFleetName = fleetNames.get(globalIndex);

                if (targetFleetIndices.contains(globalIndex)) {
                    LogManager.print("\n👉 正在对目标舰队 [" + currentFleetName + "] 进行自动补货...");

                    AutomationEngine.click(fleetPos);
                    // 🌟 动态延迟：打开库存需要时间
                    Thread.sleep(Config.CruiseConfig.enableLowEndMode ? 2000 : 800);

                    if (clickWithRetry("库存.png", 4)) {
                        if (clickWithRetry("发起配送.png", 4)) {
                            if (clickWithRetry("交换目的地.png", 4)) {
                                AutomationEngine.drawDebugROI(roiX, roiY, roiW, roiH, 1500);

                                boolean speedSelected = true;
                                if ("中速".equals(dispatchSpeed)) {
                                    speedSelected = clickWithRetryInRoi("中速.png", roiX, roiY, roiW, roiH, 4);
                                } else if ("高速".equals(dispatchSpeed)) {
                                    speedSelected = clickWithRetryInRoi("高速.png", roiX, roiY, roiW, roiH, 4);
                                } else {
                                    LogManager.print("     ℹ️ 默认标准速度，跳过速度选择。");
                                }

                                if (speedSelected) {
                                    int searchBoxX = 1400, searchBoxY = 720;
                                    int inputBoxX = 1206, inputBoxY = 719;
                                    int saveBtnX = 1334, saveBtnY = 856;

                                    for (Map.Entry<String, Integer> entry : finalMaterials.entrySet()) {
                                        String material = entry.getKey();
                                        int amount = entry.getValue();
                                        String materialImgName = material + ".png";

                                        LogManager.print("     👉 准备添加物资: " + material + " -> " + amount);

                                        boolean foundItem = false;

                                        // 👉 核心修改：重试次数改为 3 次
                                        for (int searchRetry = 1; searchRetry <= 3; searchRetry++) {
                                            LogManager.print("       🔄 [第" + searchRetry + "次] 尝试搜索并定位物资...");

                                            AutomationEngine.click(searchBoxX, searchBoxY);
                                            Thread.sleep(400);
                                            AutomationEngine.click(searchBoxX, searchBoxY);
                                            Thread.sleep(400);

                                            // 调用强化后的清空逻辑
                                            pressCtrlAAndBackspace();

                                            setClipboardString(material);
                                            pressCtrlV();
                                            // 🌟 动态延迟：给足云电脑刷新搜索列表的时间
                                            Thread.sleep(Config.CruiseConfig.enableLowEndMode ? 3000 : 1500);

                                            LogManager.print("       🔍 视觉识别: [" + materialImgName + "]");
                                            foundItem = clickWithRetryInRoi(materialImgName, roiX, roiY, roiW, roiH, 3);

                                            if (foundItem) {
                                                break; // 成功找到，跳出重试循环
                                            } else {
                                                LogManager.print("       ⚠️ 第 " + searchRetry + " 次未找到物资图片，准备重新输入...");
                                                Thread.sleep(Config.CruiseConfig.enableLowEndMode ? 1200 : 600);
                                            }
                                        }

                                        if (!foundItem) {
                                            String errorMsg = "❌ 严重错误！\n连续 3 次未能通过图像识别找到物资：[" + material + "]\n\n"
                                                    + "请检查：\n1. " + materialImgName + " 截图是否存在且清晰。\n"
                                                    + "2. 游戏仓库内是否真的有这个物资。\n\n为防点错，任务已强制终止！";
                                            LogManager.print(errorMsg.replace("\n", " "));

                                            SwingUtilities.invokeLater(() -> {
                                                JOptionPane.showMessageDialog(null, errorMsg, "物资定位失败，已紧急停止", JOptionPane.ERROR_MESSAGE);
                                            });
                                            return;
                                        }

                                        LogManager.print("     ⏳ 等待数量输入界面弹出...");
                                        // 🌟 动态延迟：低配机提前让出 CPU，防止后续 `exists` 扫描死锁
                                        if (Config.CruiseConfig.enableLowEndMode) Thread.sleep(1500);

                                        AutomationEngine.drawDebugROI(roiX, roiY, roiW, roiH, 2000);
                                        boolean isPanelReady = false;
                                        for (int wait = 0; wait < 15; wait++) {
                                            if (AutomationEngine.exists("输入数量界面.png", roiX, roiY, roiW, roiH)) {
                                                isPanelReady = true;
                                                LogManager.print("     ✅ 数量界面已就绪，准备填单！");
                                                break;
                                            }
                                            // 🌟 动态延迟：低频轮询
                                            Thread.sleep(Config.CruiseConfig.enableLowEndMode ? 600 : 200);
                                        }

                                        if (!isPanelReady) {
                                            LogManager.print("     ⚠️ 等待数量界面超时，尝试强制继续输入...");
                                        }
                                        Thread.sleep(400);

                                        AutomationEngine.click(inputBoxX, inputBoxY);
                                        Thread.sleep(400);
                                        AutomationEngine.click(inputBoxX, inputBoxY);
                                        Thread.sleep(400);

                                        pressCtrlAAndBackspace();

                                        setClipboardString(String.valueOf(amount));
                                        pressCtrlV();
                                        Thread.sleep(Config.CruiseConfig.enableLowEndMode ? 1200 : 800);

                                        AutomationEngine.click(saveBtnX, saveBtnY);
                                        Thread.sleep(Config.CruiseConfig.enableLowEndMode ? 1200 : 800);
                                    }

                                    if (clickWithRetryInRoi("确认配送.png", roiX, roiY, roiW, roiH, 4)) {
                                        LogManager.print("✅ 舰队 [" + currentFleetName + "] 补货发车成功！");
                                    }
                                }
                            }
                        }
                    }

                    LogManager.print("🔄 正在关闭面板...");
                    clickWithRetryInRoi("关闭弹窗.png", roiX, roiY, roiW, roiH, 4);
                    Thread.sleep(Config.CruiseConfig.enableLowEndMode ? 1200 : 600);

                } else {
                    LogManager.print("\n⏩ 跳过舰队 [" + currentFleetName + "] (未勾选)。");
                }

                globalIndex++;
            }

            if (globalIndex < totalFleets) {
                LogManager.print("     🖱️ 正在使用滚轮精准翻页加载后续舰队...");
                AutomationEngine.move(fleetX, lastFleetY);
                Thread.sleep(300);
                for (int k = 0; k < 8; k++) {
                    AutomationEngine.scroll(-120);
                    Thread.sleep(100);
                }
                Thread.sleep(1500);
            }
        }
        LogManager.print("🎉 [自动化补货] 所有指定舰队补货任务圆满完成！");
    }

    private boolean clickWithRetry(String imgName, int maxRetries) throws InterruptedException {
        for (int i = 0; i < maxRetries; i++) {
            if (AutomationEngine.exists(imgName)) {
                AutomationEngine.click(imgName);
                Thread.sleep(Config.CruiseConfig.enableLowEndMode ? 1200 : 600);
                return true;
            }
            Thread.sleep(Config.CruiseConfig.enableLowEndMode ? 800 : 400);
        }
        return false;
    }

    private boolean clickWithRetryInRoi(String imgName, int x, int y, int w, int h, int maxRetries) throws InterruptedException {
        for (int i = 0; i < maxRetries; i++) {
            if (AutomationEngine.exists(imgName, x, y, w, h)) {
                AutomationEngine.click(imgName, x, y, w, h);
                Thread.sleep(Config.CruiseConfig.enableLowEndMode ? 1200 : 600);
                return true;
            }
            Thread.sleep(Config.CruiseConfig.enableLowEndMode ? 800 : 400);
        }
        return false;
    }

    private void pressCtrlAAndBackspace() {
        try {
            Robot robot = new Robot();
            // 1. 常规的 Ctrl + A 全选删除
            robot.keyPress(KeyEvent.VK_CONTROL);
            Thread.sleep(50);
            robot.keyPress(KeyEvent.VK_A);
            Thread.sleep(100);
            robot.keyRelease(KeyEvent.VK_A);
            robot.keyRelease(KeyEvent.VK_CONTROL);

            Thread.sleep(300);

            robot.keyPress(KeyEvent.VK_BACK_SPACE);
            Thread.sleep(100);
            robot.keyRelease(KeyEvent.VK_BACK_SPACE);

            Thread.sleep(300);

            // 👉 核心修改：暴力兜底，连按 15 次退格键彻底清空残留
            for (int i = 0; i < 15; i++) {
                robot.keyPress(KeyEvent.VK_BACK_SPACE);
                Thread.sleep(20);
                robot.keyRelease(KeyEvent.VK_BACK_SPACE);
            }
            Thread.sleep(200);

        } catch (Exception ignored) {}
    }

    private void pressCtrlV() {
        try {
            Robot robot = new Robot();
            robot.keyPress(KeyEvent.VK_CONTROL);
            Thread.sleep(50);
            robot.keyPress(KeyEvent.VK_V);
            Thread.sleep(100);
            robot.keyRelease(KeyEvent.VK_V);
            robot.keyRelease(KeyEvent.VK_CONTROL);

            Thread.sleep(500);
        } catch (Exception ignored) {}
    }

    private void setClipboardString(String text) throws InterruptedException {
        StringSelection ss = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
        Thread.sleep(400);
    }
}