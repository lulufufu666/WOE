package com.lulu.tool;

import com.lulu.config.Config;
import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogManager {

    private static JTextArea targetTextArea;
    private static JProgressBar targetProgressBar;

    public static void setTargetTextArea(JTextArea area) {
        targetTextArea = area;
    }

    public static void setProgressBar(JProgressBar pb) {
        targetProgressBar = pb;
    }

    public static synchronized void print(String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        String formattedMsg = "[" + timestamp + "] " + message;

        // 🚀 核心 Hook：自动捕获业务动作同步到云端
        // 过滤掉包含 >>>、---、=== 这种多余排版符号的日志，只抓取纯文字描述
        String trimmed = message.trim();
        if (!trimmed.startsWith(">") && !trimmed.startsWith("-") && !trimmed.startsWith("=") && !trimmed.isEmpty()) {
            com.lulu.logic.TaskManager.latestAction = trimmed;
        }

        System.out.println(formattedMsg);

        if (targetTextArea != null) {
            SwingUtilities.invokeLater(() -> {
                targetTextArea.append(formattedMsg + "\n");
                targetTextArea.setCaretPosition(targetTextArea.getDocument().getLength());
            });
        }

        try (PrintWriter out = new PrintWriter(new FileWriter(new File(Config.getProfileDir(), "woe_bot_report.log"), true))) {
            out.println(formattedMsg);
        } catch (IOException ignored) {}
    }

    public static void updateProgress(int percent, String text) {
        if (targetProgressBar != null) {
            SwingUtilities.invokeLater(() -> {
                if (percent >= 0) {
                    if (!targetProgressBar.isVisible()) {
                        targetProgressBar.setVisible(true);
                    }
                    targetProgressBar.setValue(percent);
                    targetProgressBar.setString(text);

                    // 🚀 核心 Hook：将轮次等待的倒计时也实时同步给云端
                    com.lulu.logic.TaskManager.latestAction = text;

                } else {
                    targetProgressBar.setVisible(false);
                }
            });
        }
    }
}