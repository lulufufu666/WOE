package com.lulu.logic;

import com.lulu.config.Config;
import com.lulu.core.AutomationEngine;
import com.lulu.core.HardwareBot;
import com.lulu.logic.tasks.FleetMasterTask;
import com.lulu.logic.tasks.FleetReturnTask;
import com.lulu.logic.tasks.MaterialDispatchTask;
import com.lulu.tool.LogManager;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;

import javax.swing.*;
import java.util.List;
import java.util.Map;

public class TaskManager {

    public static volatile String currentStatus = "IDLE (空闲)";
    // 🚀 新增：用于记录极细粒度的瞬间动作
    public static volatile String latestAction = "";

    private static Thread runningThread;
    private static Thread hotkeyThread;

    public static boolean isIdle() {
        return currentStatus.contains("IDLE");
    }

    public static boolean isBusy() {
        return runningThread != null && runningThread.isAlive();
    }

    private static void focusGameWindow() {
        HWND hwnd = User32.INSTANCE.FindWindow(null, Config.Global.APP_TITLE);
        if (hwnd != null) {
            User32.INSTANCE.SetForegroundWindow(hwnd);
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }
    }

    public static void startCruise(boolean isRemote) {
        if (isBusy()) {
            if (!isRemote) JOptionPane.showMessageDialog(null, "当前已有任务正在运行！");
            return;
        }
        LogManager.print(isRemote ? "收到云端指令：启动自动巡航" : "启动自动巡航...");
        focusGameWindow();

        BotTask masterTask = new FleetMasterTask(
                Config.CruiseConfig.targetRounds, Config.CruiseConfig.intervalMinutes,
                Config.CruiseConfig.repairThreshold, Config.CruiseConfig.repairFilterIndex,
                Config.CruiseConfig.autoRestartGame == 1, Config.CruiseConfig.restartRounds,
                Config.CruiseConfig.enableAutoRepair
        );
        runningThread = new Thread(() -> {
            try {
                currentStatus = isRemote ? "CRUISING (云端接管)" : "CRUISING (本地执行)";
                latestAction = "初始化巡航参数...";
                masterTask.execute();
            } catch (InterruptedException ex) {
                LogManager.print("任务被强行中断！");
                Thread.currentThread().interrupt();
            } finally {
                currentStatus = "IDLE (空闲)";
                latestAction = "";
            }
        });
        runningThread.start();
    }

    public static void stopAllTasks() {
        stopAllTasks(true);
    }

    public static void stopAllTasks(boolean showWarning) {
        if (isBusy()) {
            runningThread.interrupt();
            LogManager.print("强制终止指令已执行...");
            currentStatus = "IDLE (已停止)";
            latestAction = ""; // 🚀 清空动作
        } else if (showWarning) {
            JOptionPane.showMessageDialog(null, "当前没有运行的任务");
        }
    }

    public static void startDispatch(Map<String, Integer> finalMaterials, List<Integer> selectedIndices, String speed) {
        if (isBusy()) return;
        focusGameWindow();
        runningThread = new Thread(() -> {
            try {
                currentStatus = "DISPATCHING (物资派发)";
                latestAction = "正在准备表单...";
                new MaterialDispatchTask(finalMaterials, selectedIndices, speed).execute();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                currentStatus = "IDLE (空闲)";
                latestAction = "";
            }
        });
        runningThread.start();
    }

    public static void startClearInventory(String speed, List<Integer> selectedIndices) {
        if (isBusy()) return;
        LogManager.print("启动舰队清仓指令...");
        focusGameWindow();
        runningThread = new Thread(() -> {
            try {
                currentStatus = "CLEARING (一键清仓)";
                latestAction = "正在定位舰队...";
                new FleetReturnTask(speed, selectedIndices).execute();
            } catch (InterruptedException ex) {
                LogManager.print("清仓任务已终止！");
                Thread.currentThread().interrupt();
            } finally {
                currentStatus = "IDLE (空闲)";
                latestAction = "";
            }
        });
        runningThread.start();
    }

    public static void testInteractionCircle() {
        if (isBusy()) {
            JOptionPane.showMessageDialog(null, "当前已有任务运行中");
            return;
        }
        runningThread = new Thread(() -> {
            try {
                focusGameWindow();
                int newRadius = Config.CruiseConfig.interactRadius;
                int offsetX = Config.CruiseConfig.offsetX;
                int offsetY = Config.CruiseConfig.offsetY;
                int scrollSteps = Config.CruiseConfig.scrollSteps;

                LogManager.print(">>> [沙盒调试] 开始测试 | 半径: " + newRadius + " | 偏移X: " + offsetX + " | 偏移Y: " + offsetY);

                int panelX = 35, panelY = 249, panelW = 436, panelH = 792;
                List<int[]> visibleFleets = AutomationEngine.findAll("舰队图标.png", panelX, panelY, panelW, panelH);
                if (!visibleFleets.isEmpty()) {
                    AutomationEngine.doubleClick(visibleFleets.get(0));
                    Thread.sleep(2000);
                } else {
                    LogManager.print("未能找到舰队图标，跳过聚焦...");
                }

                AutomationEngine.click(2303, 292);
                Thread.sleep(800);
                AutomationEngine.click(2319, 201);
                Thread.sleep(1000);

                HWND hwnd = User32.INSTANCE.FindWindow(null, Config.Global.APP_TITLE);
                if (hwnd != null) {
                    RECT rect = new RECT();
                    User32.INSTANCE.GetWindowRect(hwnd, rect);
                    double ratio = Config.Global.GAME_UI_SCALE;
                    int centerRelX = (int) (((rect.right - rect.left) / 2) / ratio);
                    int centerRelY = (int) (((rect.bottom - rect.top) / 2) / ratio);
                    AutomationEngine.move(centerRelX + offsetX, centerRelY + offsetY);
                }
                Thread.sleep(500);

                for (int j = 0; j < scrollSteps; j++) {
                    AutomationEngine.scroll(30);
                    Thread.sleep(100);
                }
                Thread.sleep(1000);

                int roiX = 715, roiY = 276, roiW = 1058, roiH = 964;
                int centerX = roiX + roiW / 2 + offsetX;
                int centerY = roiY + roiH / 2 + offsetY;
                AutomationEngine.drawDebugOval(centerX - newRadius, centerY - newRadius, newRadius * 2, newRadius * 2, 6000);

            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
        runningThread.start();
    }

    public static void startGlobalHotkeyListener(JFrame mainGuiFrame) {
        hotkeyThread = new Thread(() -> {
            boolean lastF1State = false;
            while (true) {
                try {
                    if (HardwareBot.isKeyPressed(0x1B)) { // ESC
                        if (isBusy()) {
                            LogManager.print("监听到全局 ESC 键，触发紧急停止！");
                            runningThread.interrupt();
                            Thread.sleep(1000);
                        }
                    }

                    boolean currentF1State = HardwareBot.isKeyPressed(0x70); // F1
                    if (currentF1State && !lastF1State) {
                        SwingUtilities.invokeLater(() -> {
                            mainGuiFrame.setVisible(true);
                            mainGuiFrame.toFront();
                            mainGuiFrame.setState(JFrame.NORMAL);
                            mainGuiFrame.requestFocus();
                            LogManager.print("监听到全局 F1 键，菜单已激活！");
                        });
                    }
                    lastF1State = currentF1State;
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        hotkeyThread.setDaemon(true);
        hotkeyThread.start();
    }
}