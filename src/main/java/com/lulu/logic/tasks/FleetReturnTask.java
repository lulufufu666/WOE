package com.lulu.logic.tasks;

import com.lulu.config.Config;
import com.lulu.core.AutomationEngine;
import com.lulu.logic.BotTask;
import com.lulu.tool.LogManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FleetReturnTask implements BotTask {

    private final String dispatchSpeed;
    private final List<Integer> targetFleetIndices;
    private final int totalFleets;

    public FleetReturnTask(String dispatchSpeed, List<Integer> targetFleetIndices) {
        this.dispatchSpeed = dispatchSpeed;
        this.targetFleetIndices = targetFleetIndices;
        this.totalFleets = Config.FleetConfig.TASK_MAP.size();
    }

    @Override
    public void execute() throws InterruptedException {
        LogManager.print("🚚 [清仓发货] 开始执行指定舰队的一键配送回城任务...");
        LogManager.print("📦 [配置读取] 当前选择的配送速度为: " + dispatchSpeed);

        if (targetFleetIndices.isEmpty()) {
            LogManager.print("❌ [清仓中止] 未指定任何目标舰队！");
            return;
        }

        LogManager.print("⚙️ [前置初始化] 正在点击固定坐标...");
        int initX1 = 2141;
        int initY1 = 203;
        AutomationEngine.click(initX1, initY1);
        Thread.sleep(Config.CruiseConfig.enableLowEndMode ? 1500 : 800);

        int initX2 = 2226;
        int initY2 = 204;
        AutomationEngine.click(initX2, initY2);
        Thread.sleep(Config.CruiseConfig.enableLowEndMode ? 1500 : 1000);

        LogManager.print("🧹 [初始清理] 正在检测并关闭残留弹窗...");
        for (int i = 0; i < 3; i++) {
            if (AutomationEngine.exists("关闭弹窗.png")) {
                AutomationEngine.click("关闭弹窗.png");
                Thread.sleep(Config.CruiseConfig.enableLowEndMode ? 1200 : 800);
            } else {
                break;
            }
        }

        int panelX = 35, panelY = 249, panelW = 436, panelH = 792;
        int roiX = 664, roiY = 188, roiW = 1072, roiH = 1010;

        List<String> fleetNames = new ArrayList<>(Config.FleetConfig.TASK_MAP.keySet());
        int processedCount = 0;

        while (processedCount < totalFleets) {
            List<int[]> visibleFleets = AutomationEngine.findAll("舰队图标.png", panelX, panelY, panelW, panelH);
            if (visibleFleets.isEmpty()) {
                LogManager.print("❌ 未在左侧列表找到舰队图标，清仓任务中止。");
                break;
            }

            visibleFleets.sort(Comparator.comparingInt(pos -> pos[1]));
            int lastFleetY = visibleFleets.get(visibleFleets.size() - 1)[1];
            int fleetX = visibleFleets.get(0)[0];

            for (int i = 0; i < visibleFleets.size(); i++) {
                if (processedCount >= totalFleets) break;

                int[] fleetPos = visibleFleets.get(i);
                String currentFleetName = fleetNames.get(processedCount);

                if (targetFleetIndices.contains(processedCount)) {
                    LogManager.print("\n👉 正在对目标舰队 [" + currentFleetName + "] 的库存进行清仓...");

                    AutomationEngine.click(fleetPos);
                    // 🌟 动态延迟
                    Thread.sleep(Config.CruiseConfig.enableLowEndMode ? 2000 : 1200);

                    if (clickWithRetry("库存.png", 4)) {
                        if (clickWithRetry("发起配送.png", 4)) {
                            AutomationEngine.drawDebugROI(roiX, roiY, roiW, roiH, 2000);

                            boolean speedSelected = true;
                            if ("中速".equals(dispatchSpeed)) {
                                speedSelected = clickWithRetryInRoi("中速.png", roiX, roiY, roiW, roiH, 4);
                            } else if ("高速".equals(dispatchSpeed)) {
                                speedSelected = clickWithRetryInRoi("高速.png", roiX, roiY, roiW, roiH, 4);
                            } else {
                                LogManager.print("     ℹ️ 默认标准速度，跳过速度选择。");
                            }

                            if (speedSelected) {
                                if (clickWithRetryInRoi("全选.png", roiX, roiY, roiW, roiH, 4)) {
                                    if (clickWithRetryInRoi("确认配送.png", roiX, roiY, roiW, roiH, 4)) {
                                        LogManager.print("✅ 舰队 [" + currentFleetName + "] 物资发货完成！");
                                    }
                                }
                            }
                        }
                    }

                    LogManager.print("🔄 正在关闭面板...");
                    clickWithRetryInRoi("关闭弹窗.png", roiX, roiY, roiW, roiH, 4);
                    Thread.sleep(Config.CruiseConfig.enableLowEndMode ? 1500 : 1000);

                } else {
                    LogManager.print("\n⏩ 跳过舰队 [" + currentFleetName + "] (未勾选)。");
                }

                processedCount++;
            }

            if (processedCount < totalFleets) {
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

        LogManager.print("🎉 [清仓发货] 所有指定舰队物资配送任务圆满完成！");
    }

    private boolean clickWithRetry(String imgName, int maxRetries) throws InterruptedException {
        for (int i = 0; i < maxRetries; i++) {
            if (AutomationEngine.exists(imgName)) {
                AutomationEngine.click(imgName);
                Thread.sleep(Config.CruiseConfig.enableLowEndMode ? 1500 : 1000);
                return true;
            }
            Thread.sleep(Config.CruiseConfig.enableLowEndMode ? 1000 : 500);
        }
        LogManager.print("⚠️ 超时未找到图标: [" + imgName + "]，已跳过。");
        return false;
    }

    private boolean clickWithRetryInRoi(String imgName, int x, int y, int w, int h, int maxRetries) throws InterruptedException {
        for (int i = 0; i < maxRetries; i++) {
            if (AutomationEngine.exists(imgName, x, y, w, h)) {
                AutomationEngine.click(imgName, x, y, w, h);
                Thread.sleep(Config.CruiseConfig.enableLowEndMode ? 1500 : 1000);
                return true;
            }
            Thread.sleep(Config.CruiseConfig.enableLowEndMode ? 1000 : 500);
        }
        LogManager.print("⚠️ 区域内超时未找到图标: [" + imgName + "]，已跳过。");
        return false;
    }
}