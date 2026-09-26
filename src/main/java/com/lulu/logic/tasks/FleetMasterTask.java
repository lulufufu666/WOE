package com.lulu.logic.tasks;

import com.lulu.config.Config;
import com.lulu.core.AutomationEngine;
import com.lulu.core.HardwareBot;
import com.lulu.logic.BotTask;
import com.lulu.logic.TaskReport;
import com.lulu.tool.LogManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.io.PrintWriter;
import java.util.*;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;

public class FleetMasterTask implements BotTask {

    private final LinkedHashMap<String, List<String>> fleetTaskConfig;
    private final int totalRounds;
    private final int intervalMinutes;
    private final int repairThreshold;

    private final String repairFilterImg;
    private final boolean autoRestart;
    // 👉 新增：接收从界面传来的重启周期参数
    private final int restartRounds;
    private final boolean enableAutoRepair;

    public FleetMasterTask(int rounds, int intervalMinutes, int repairThreshold, int repairFilterIndex, boolean autoRestart, int restartRounds, boolean enableAutoRepair) {
        this.totalRounds = rounds;
        this.intervalMinutes = intervalMinutes;
        this.repairThreshold = repairThreshold;
        this.fleetTaskConfig = new LinkedHashMap<>(Config.FleetConfig.TASK_MAP);
        this.autoRestart = autoRestart;
        this.restartRounds = restartRounds; // 👉 赋值保存
        this.enableAutoRepair = enableAutoRepair;

        if (repairFilterIndex == 0) {
            this.repairFilterImg = "低于20.png";
        } else if (repairFilterIndex == 2) {
            this.repairFilterImg = "低于70.png";
        } else {
            this.repairFilterImg = "低于50.png";
        }
    }

    @Override
    public void execute() throws InterruptedException {
        List<String> fleetNames = new ArrayList<>(fleetTaskConfig.keySet());
        List<TaskReport> globalReports = new ArrayList<>();

        System.out.println("🚀 开启全舰队无状态巡航模式 | 目标舰队总数: " + fleetNames.size() + " | 计划轮次: " + totalRounds + " | 轮次间隔: " + intervalMinutes + "分钟");
        String startTimeStr = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
        TaskReport.appendLog("\n\n>>>>>>>>>> [新一轮自动巡航开始: " + startTimeStr + " | 计划轮次: " + totalRounds + "] <<<<<<<<<<");

        int panelX = 35, panelY = 249, panelW = 436, panelH = 792;

        long totalTaskStartTime = System.currentTimeMillis();
        long totalActiveWorkTime = 0;
        int completedRounds = 0;

        String customLaunch = Config.Global.LAUNCH_CMD;
        String gameExePath = null;

        if (customLaunch != null && !customLaunch.trim().isEmpty()) {
            gameExePath = customLaunch.trim();
            LogManager.print("✅ 已读取自定义游戏启动快捷方式: " + gameExePath);
            if (autoRestart) LogManager.print("   -> 已开启【轮次间自动重启】，将使用沙盒快捷方式拉起游戏！");
        } else {
            HWND hwndForPath = User32.INSTANCE.FindWindow(null, Config.Global.APP_TITLE);
            if (hwndForPath != null) {
                gameExePath = HardwareBot.getGameExecutablePath(hwndForPath);
            }
            if (gameExePath != null) {
                LogManager.print("✅ 自动抓取到游戏本体路径: " + gameExePath);
                if (autoRestart) LogManager.print("   -> 已开启【轮次间自动重启】，将使用物理机直连方式拉起游戏！");
            } else {
                LogManager.print("⚠️ 未能自动获取游戏路径，冷却期间将强制保持挂机。");
            }
        }

        try {
            for (int currentRound = 1; currentRound <= totalRounds; currentRound++) {
                long roundStartTime = System.currentTimeMillis();

                System.out.println("\n=============================================");
                System.out.println("⭐⭐⭐ 正在执行第 [" + currentRound + " / " + totalRounds + "] 轮巡航 ⭐⭐⭐");
                System.out.println("=============================================");

                System.out.println("⚙️ [前置初始化] 正在点击固定坐标以重置视角...");
                int initX1 = 2141;
                int initY1 = 203;
                AutomationEngine.click(initX1, initY1);
                Thread.sleep(800);

                int initX2 = 2226;
                int initY2 = 204;
                AutomationEngine.click(initX2, initY2);
                Thread.sleep(1000);

                System.out.println("🧹 [初始清理] 正在检测并关闭残留弹窗...");
                for (int i = 0; i < 3; i++) {
                    if (AutomationEngine.exists("关闭弹窗.png")) {
                        AutomationEngine.click("关闭弹窗.png");
                        Thread.sleep(800);
                    } else {
                        break;
                    }
                }

                int globalIndex = 0;

                while (globalIndex < fleetNames.size()) {
                    System.out.println(">>> 正在扫描当前可视区域内的舰队...");
                    AutomationEngine.drawDebugROI(panelX, panelY, panelW, panelH, 1500);
                    List<int[]> visibleFleets = AutomationEngine.findAll("舰队图标.png", panelX, panelY, panelW, panelH);

                    if (visibleFleets.isEmpty()) {
                        LogManager.print("❌ 严重异常：未检测到舰队图标，请确认左侧【舰队面板】是否已展开！");
                        LogManager.print("🛑 巡航任务已紧急中止，游戏保持运行，请手动展开面板后重新启动脚本。");
                        return;
                    }

                    visibleFleets.sort(Comparator.comparingInt(pos -> pos[1]));
                    int lastFleetY = visibleFleets.get(visibleFleets.size() - 1)[1];
                    int fleetX = visibleFleets.get(0)[0];

                    for (int i = 0; i < visibleFleets.size(); i++) {
                        if (globalIndex >= fleetNames.size()) break;

                        int[] fleetPos = visibleFleets.get(i);
                        String currentFleetName = fleetNames.get(globalIndex);
                        List<String> assignedStars = fleetTaskConfig.get(currentFleetName);

                        System.out.println("\n=============================================");
                        System.out.println(">>> 正在调度 [" + currentFleetName + "] | 专属任务: " + assignedStars + " (进度: " + (globalIndex + 1) + "/" + fleetNames.size() + ")");

                        AutomationEngine.doubleClick(fleetPos);
                        Thread.sleep(2000);

                        if (enableAutoRepair && repairThreshold > 0) {
                            // 👉 换成最新的测算结果
                            int hpStartX = 1713;
                            int hpStartY = 537;
                            int hpLength = 606;

                            int hpCheckX = hpStartX + (int)(hpLength * (repairThreshold / 100.0));
                            int hpCheckY = hpStartY;

                            AutomationEngine.drawDebugOval(hpCheckX - 10, hpCheckY - 10, 20, 20, 2000);
                            java.awt.Color hpColor = AutomationEngine.getPixelColor(hpCheckX, hpCheckY);

                            boolean isHealthy = hpColor.getGreen() > hpColor.getRed() + 30 && hpColor.getGreen() > 100;

                            if (!isHealthy) {
                                String msg = "     ⚠️ 检测到 [" + currentFleetName + "] 耐久度低于设定阈值 (" + repairThreshold + "%)，正在执行进坞急修...";
                                TaskReport.appendLog(msg);
                                LogManager.print(msg);

                                performRepair(this.repairFilterImg);
                            } else {
                                LogManager.print("     ✅ 舰队耐久度健康 (高于 " + repairThreshold + "%)。");
                            }
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

                            int xOffset = Config.CruiseConfig.offsetX;
                            int yOffset = Config.CruiseConfig.offsetY;
                            AutomationEngine.move(centerRelX + xOffset, centerRelY + yOffset);
                        }
                        Thread.sleep(500);

                        for(int j = 0; j < Config.CruiseConfig.scrollSteps; j++){
                            AutomationEngine.scroll(30);
                            Thread.sleep(100);
                        }
                        Thread.sleep(1000);

                        TaskReport currentReport = new TaskReport(currentFleetName);
                        globalReports.add(currentReport);

                        for (String star : assignedStars) {
                            Map<String, String> strategyMap = Config.Strategy.EVENT_MAP.get(star);
                            if (strategyMap == null || strategyMap.isEmpty()) continue;

                            System.out.println("\n--- 开始执行舰队 [" + currentFleetName + "] 的 [" + star + "] 清剿 ---");
                            SeaQuestTask questTask = new SeaQuestTask(star, strategyMap, currentReport);
                            questTask.execute();
                            Thread.sleep(1000);
                        }

                        currentReport.printSummary();
                        globalIndex++;
                        Thread.sleep(1000);
                    }

                    if (globalIndex < fleetNames.size()) {
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

                long roundEndTime = System.currentTimeMillis();
                totalActiveWorkTime += (roundEndTime - roundStartTime);
                completedRounds++;

                if (currentRound < totalRounds) {
                    System.out.println(">>> 第 " + currentRound + " 轮(纯工作耗时: " + formatDuration(roundEndTime - roundStartTime) + ") 结束...");

                    int waitSeconds = intervalMinutes * 60;

                    // 👉 核心修改：使用 currentRound % restartRounds == 0 来判断是否到达了设定的重启周期
                    boolean shouldRestart = (autoRestart && gameExePath != null && (currentRound % restartRounds == 0));

                    if (shouldRestart) {
                        LogManager.print("   🛑 正在自动关闭游戏进程以释放电脑内存...");
                        HardwareBot.killGameProcess(Config.Global.APP_TITLE);
                        Thread.sleep(3000);
                    } else if (autoRestart) {
                        LogManager.print("   ℹ️ 尚未达到设定的重启周期（设为每" + restartRounds + "轮重启），本轮间隔将保持游戏挂机。");
                    }

                    LogManager.print("   ⏳ 任务已挂起，进入轮次冷却倒计时，请查看日志面板底部的进度条...");
                    for (int w = waitSeconds; w > 0; w--) {
                        if (Thread.currentThread().isInterrupted()) {
                            LogManager.updateProgress(-1, "");
                            throw new InterruptedException("任务被手动中断");
                        }
                        int percent = (int) (((waitSeconds - w) / (double) waitSeconds) * 100);
                        int m = w / 60;
                        int s = w % 60;
                        LogManager.updateProgress(percent, String.format("冷却中: 还剩 %02d 分 %02d 秒 (已过 %d%%)", m, s, percent));
                        Thread.sleep(1000);
                    }
                    LogManager.updateProgress(-1, "");

                    if (shouldRestart) {
                        boolean isGameLoaded = false;
                        int maxRestartAttempts = 100;

                        for (int startAttempt = 1; startAttempt <= maxRestartAttempts; startAttempt++) {
                            if (startAttempt == 1) {
                                LogManager.print("   🚀 冷却结束，正在重新启动游戏...");
                            } else {
                                LogManager.print("   🚀 [第 " + startAttempt + " 次尝试] 正在重新启动游戏...");
                            }

                            try {
                                if (gameExePath.toLowerCase().endsWith(".lnk")) {
                                    Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", "", gameExePath});
                                } else {
                                    Runtime.getRuntime().exec(new String[]{gameExePath});
                                }
                            } catch(Exception e) {
                                LogManager.print("   ❌ 启动游戏失败: " + e.getMessage());
                            }

                            LogManager.print("   ⏳ 等待游戏启动并进入主界面 (限时5分钟，每20秒低频扫描)...");
                            isGameLoaded = false;

                            for (int retry = 0; retry < 7; retry++) {
                                if (Thread.currentThread().isInterrupted()) throw new InterruptedException("任务被手动中断");

                                HWND gameHwnd = User32.INSTANCE.FindWindow(null, Config.Global.APP_TITLE);
                                if (gameHwnd != null) {
                                    User32.INSTANCE.SetForegroundWindow(gameHwnd);

                                    if (AutomationEngine.exists("关闭弹窗.png") || AutomationEngine.exists("设置.png")) {
                                        isGameLoaded = true;
                                        break;
                                    }
                                }
                                Thread.sleep(15000);
                            }

                            if (isGameLoaded) {
                                break;
                            } else {
                                if (startAttempt < maxRestartAttempts) {
                                    LogManager.print("   ⚠️ 游戏未进入主界面，疑似卡死！正在强制杀进程准备重试...");
                                    HardwareBot.killGameProcess(Config.Global.APP_TITLE);
                                    Thread.sleep(5000);
                                }
                            }
                        }

                        if (!isGameLoaded) {
                            LogManager.print("   ❌ 连续 " + maxRestartAttempts + " 次启动游戏均超时卡死，巡航任务被迫终止！");
                            break;
                        }

                        LogManager.print("   🧹 游戏加载完毕！正在自动清理登录公告/离线收益等弹窗...");
                        for (int i = 0; i < 1; i++) {
                            if (AutomationEngine.exists("关闭弹窗.png")) {
                                AutomationEngine.click("关闭弹窗.png");
                                Thread.sleep(1500);
                            } else {
                                break;
                            }
                        }
                        LogManager.print("   ✅ 游戏环境恢复完成，准备进入下一轮巡航...");

                    } else {
                        System.out.println(">>> 正在将舰队列表滑回顶部，准备新一轮...");
                        AutomationEngine.move(panelX + (panelW / 2), panelY + (panelH / 2));
                        Thread.sleep(300);
                        for (int k = 0; k < 25; k++) {
                            AutomationEngine.scroll(120);
                            Thread.sleep(80);
                        }
                        System.out.println(">>> 舰队列表已复位！");
                    }
                }
            }
        } finally {
            LogManager.updateProgress(-1, "");

            long totalTaskEndTime = System.currentTimeMillis();
            long totalDuration = totalTaskEndTime - totalTaskStartTime;
            long avgRoundDuration = completedRounds > 0 ? (totalActiveWorkTime / completedRounds) : 0;

            generateGlobalSummary(globalReports, totalDuration, totalActiveWorkTime, avgRoundDuration, completedRounds);
        }
    }

    private void performRepair(String filterImgName) throws InterruptedException {
        int roiX = 1504;
        int roiY = 510;
        int roiW = 66;
        int roiH = 74;

        int roiX2 = 571;
        int roiY2 = 151;
        int roiW2 = 1264;
        int roiH2 = 1080;

        AutomationEngine.drawDebugROI(roiX, roiY, roiW, roiH, 2000);

        if (clickWithRetryInRoi("进入维修.png", roiX, roiY, roiW, roiH, 3)) {
            LogManager.print("     🔧 成功点击【进入维修】，等待大面板展开...");
            Thread.sleep(1200);

            AutomationEngine.drawDebugROI(roiX2, roiY2, roiW2, roiH2, 2000);

            LogManager.print("     🔧 正在操作面板：取消【维修全部】勾选...");
            if (clickWithRetryInRoi("维修全部取消.png", roiX2, roiY2, roiW2, roiH2, 3)) {
                Thread.sleep(600);

                LogManager.print("     🔧 正在操作面板：尝试过滤勾选【" + filterImgName.replace(".png", "") + "】船只...");
                if (clickWithRetryInRoi(filterImgName, roiX2, roiY2, roiW2, roiH2, 3)) {
                    Thread.sleep(600);

                    LogManager.print("     🔧 下达修理指令：点击【确认维修】...");
                    if (clickWithRetryInRoi("确认维修.png", roiX2, roiY2, roiW2, roiH2, 3)) {
                        LogManager.print("     ✅ 维修动作执行完毕！等待服务器结算...");
                        Thread.sleep(1500);
                    } else {
                        LogManager.print("     ❌ 失败：未能在面板中找到并点击【确认维修】按钮。");
                    }
                } else {
                    LogManager.print("     ❌ 失败：未能找到过滤按钮【" + filterImgName + "】。");
                }
            } else {
                LogManager.print("     ❌ 失败：未能找到【维修全部取消.png】。");
            }

            LogManager.print("     🔄 维修流程完结，系统休眠缓冲中...");
            Thread.sleep(1000);
        } else {
            LogManager.print("     ❌ 严重异常：未能点击进入维修页面，放弃该轮维修。");
        }
        clickWithRetry("关闭弹窗.png",2);
    }

    private boolean clickWithRetryInRoi(String imgName, int x, int y, int w, int h, int maxRetries) throws InterruptedException {
        for (int i = 0; i < maxRetries; i++) {
            if (AutomationEngine.exists(imgName, x, y, w, h)) {
                AutomationEngine.click(imgName, x, y, w, h);
                Thread.sleep(600);
                return true;
            }
            Thread.sleep(400);
        }
        return false;
    }

    private boolean clickWithRetry(String imgName, int maxRetries) throws InterruptedException {
        for (int i = 0; i < maxRetries; i++) {
            if (AutomationEngine.exists(imgName)) {
                AutomationEngine.click(imgName);
                Thread.sleep(600);
                return true;
            }
            Thread.sleep(400);
        }
        return false;
    }

    private String formatDuration(long millis) {
        if (millis <= 0) return "0秒";
        long seconds = (millis / 1000) % 60;
        long minutes = (millis / (1000 * 60)) % 60;
        long hours = (millis / (1000 * 60 * 60));
        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("小时 ");
        if (minutes > 0) sb.append(minutes).append("分钟 ");
        sb.append(seconds).append("秒");
        return sb.toString();
    }

    private void generateGlobalSummary(List<TaskReport> globalReports, long totalDuration, long totalActiveTime, long avgRoundDuration, int completedRounds) {
        if (globalReports.isEmpty()) {
            System.out.println(">>>>>>>>>> [巡航结束 (无有效舰队数据)] <<<<<<<<<<\n");
            return;
        }

        int totalFound = 0;
        Map<String, TaskReport.EventStat> globalEventStats = new HashMap<>();

        for (TaskReport r : globalReports) {
            totalFound += r.totalFound;
            for (Map.Entry<String, TaskReport.EventStat> entry : r.detailedStats.entrySet()) {
                String eventName = entry.getKey();
                TaskReport.EventStat localStat = entry.getValue();
                TaskReport.EventStat globalStat = globalEventStats.computeIfAbsent(eventName, k -> new TaskReport.EventStat());
                globalStat.total += localStat.total;
                globalStat.success += localStat.success;
                globalStat.planB += localStat.planB;
                globalStat.ignore += localStat.ignore;
                globalStat.fail += localStat.fail;
            }
        }

        int totalSuccess = 0, totalPlanB = 0, totalFail = 0, totalIgnore = 0;
        for (TaskReport.EventStat stat : globalEventStats.values()) {
            totalSuccess += stat.success;
            totalPlanB += stat.planB;
            totalFail += stat.fail;
            totalIgnore += stat.ignore;
        }

        Config.GlobalStats.totalFound += totalFound;
        Config.GlobalStats.totalSuccess += totalSuccess;
        Config.GlobalStats.totalPlanB += totalPlanB;
        Config.GlobalStats.totalIgnore += totalIgnore;
        Config.GlobalStats.totalFail += totalFail;

        for (Map.Entry<String, TaskReport.EventStat> entry : globalEventStats.entrySet()) {
            String cleanName = entry.getKey().replace(".png", "");
            if (cleanName.length() >= 2) {
                String starLevel = cleanName.substring(0, 2);

                int currentStarCount = Config.GlobalStats.starCountMap.getOrDefault(starLevel, 0);
                Config.GlobalStats.starCountMap.put(starLevel, currentStarCount + entry.getValue().total);

                int currentEventCount = Config.GlobalStats.eventCountMap.getOrDefault(cleanName, 0);
                Config.GlobalStats.eventCountMap.put(cleanName, currentEventCount + entry.getValue().total);
            }
        }
        Config.saveStatsConfig();

        String endTimeStr = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
        StringBuilder sb = new StringBuilder();
        sb.append("🌟🌟🌟 全局巡航汇总终极报告 🌟🌟🌟\n");
        sb.append("结束时间: ").append(endTimeStr).append("\n");
        sb.append("计划轮次: ").append(totalRounds).append(" | 实际完成轮次: ").append(completedRounds).append("\n");
        sb.append("挂机总时长: ").append(formatDuration(totalDuration)).append("\n");
        sb.append("实际工作时长(刨除等待): ").append(formatDuration(totalActiveTime)).append("\n");
        sb.append("单轮平均纯工作耗时: ").append(formatDuration(avgRoundDuration)).append("\n");
        sb.append("--------------------------------------\n");
        sb.append("参与舰队总数 (所有轮次累计): ").append(globalReports.size()).append("\n");
        sb.append("🎯 发现总目标: ").append(totalFound).append("\n");
        sb.append("✅ 成功收割(获奖励): ").append(totalSuccess).append("\n");
        sb.append("🪙 破财免灾(仅付费): ").append(totalPlanB).append("\n");
        sb.append("⏭️ 主动忽略(未交互): ").append(totalIgnore).append("\n");
        sb.append("❌ 失败拦截(无奖励): ").append(totalFail).append("\n");
        sb.append("--- 📋 全舰队事件明细结局分布大盘 ---\n");

        if (globalEventStats.isEmpty()) {
            sb.append("    (本次巡航未识别到任何有效事件)\n");
        } else {
            for (Map.Entry<String, TaskReport.EventStat> entry : globalEventStats.entrySet()) {
                String eventCleanName = entry.getKey().replace(".png", "");
                sb.append("    ➡️ 【").append(eventCleanName).append("】\n         ")
                        .append(entry.getValue().toString()).append("\n");
            }
        }
        sb.append(">>>>>>>>>> [巡航数据落盘完毕] <<<<<<<<<<\n");

        String res = sb.toString();
        System.out.println("\n" + res);

        try {
            File reportDir = new File(Config.getProfileDir() + "巡航数据报表");
            if (!reportDir.exists()) {
                reportDir.mkdirs();
            }
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            File txtFile = new File(reportDir, "巡航终极报告_" + timestamp + ".txt");

            try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(txtFile), StandardCharsets.UTF_8))) {
                out.println(res);
            }
            System.out.println("📄 [终极报告] 直观文字版已保存至: " + txtFile.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("❌ 保存终极报告文本失败: " + e.getMessage());
        }
    }
}