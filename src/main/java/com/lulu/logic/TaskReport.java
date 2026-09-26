package com.lulu.logic;

import com.lulu.config.Config;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 任务统计报表 (纯文本日志版)
 */
public class TaskReport {
    public String fleetName;
    public int totalFound = 0;
    public int successCount = 0;
    public int failCount = 0;
    public int ignoreCount = 0;
    public int planBCount = 0;

    public static class EventStat {
        public int total = 0;
        public int success = 0;
        public int planB = 0;
        public int ignore = 0;
        public int fail = 0;

        @Override
        public String toString() {
            return String.format("[共遇到:%d次 | 成功收割:%d | 破财免灾:%d | 主动忽略:%d | 失败/拦截:%d]",
                    total, success, planB, ignore, fail);
        }
    }

    public Map<String, EventStat> detailedStats = new HashMap<>();

    public TaskReport(String fleetName) {
        this.fleetName = fleetName;
    }

    private EventStat getStat(String eventName) {
        return detailedStats.computeIfAbsent(eventName, k -> new EventStat());
    }

    public void recordFoundEvent(String eventName) { getStat(eventName).total++; }
    public void recordSuccess(String eventName) { successCount++; getStat(eventName).success++; }
    public void recordPlanB(String eventName) { planBCount++; getStat(eventName).planB++; }
    public void recordIgnore(String eventName) { ignoreCount++; getStat(eventName).ignore++; }
    public void recordFail(String eventName) { failCount++; getStat(eventName).fail++; }

    public static synchronized void appendLog(String text) {
        try (PrintWriter out = new PrintWriter(new FileWriter(new File(Config.getProfileDir(), "woe_bot_report.log"), true))) { // 👉 修改这行
            out.println(text);
        } catch (IOException e) {
            System.err.println("❌ 写入日志失败: " + e.getMessage());
        }
    }

    public void printSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=====================================\n");
        sb.append("📊 舰队 [").append(fleetName).append("] 单次作业结局分布:\n");
        sb.append("   - 🎯 发现总目标: ").append(totalFound).append("\n");
        sb.append("   - ✅ 成功收割(获奖励): ").append(successCount).append("\n");
        sb.append("   - 🪙 破财免灾(仅付费): ").append(planBCount).append("\n");
        sb.append("   - ⏭️ 主动忽略(未交互): ").append(ignoreCount).append("\n");
        sb.append("   - ❌ 失败拦截(无奖励): ").append(failCount).append("\n");
        sb.append("   - 📋 各星级具体任务明细统计:\n");
        if (detailedStats.isEmpty()) {
            sb.append("       (无识别到任何具体事件)\n");
        } else {
            for (Map.Entry<String, EventStat> entry : detailedStats.entrySet()) {
                sb.append("       ➡️ ").append(entry.getKey()).append("\n            ").append(entry.getValue().toString()).append("\n");
            }
        }
        sb.append("=====================================");

        String res = sb.toString();
        System.out.println(res);
        appendLog(res);
    }
}