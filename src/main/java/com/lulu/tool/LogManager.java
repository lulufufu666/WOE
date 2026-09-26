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

    // 绑定右下角的文本框
    public static void setTargetTextArea(JTextArea area) {
        targetTextArea = area;
    }

    // 🌟 新增：绑定日志面板底部的进度条
    public static void setProgressBar(JProgressBar pb) {
        targetProgressBar = pb;
    }

    public static synchronized void print(String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        String formattedMsg = "[" + timestamp + "] " + message;

        // 1. 打印到 IDEA 控制台
        System.out.println(formattedMsg);

        // 2. 实时追加到右下角日志面板
        if (targetTextArea != null) {
            SwingUtilities.invokeLater(() -> {
                targetTextArea.append(formattedMsg + "\n");
                targetTextArea.setCaretPosition(targetTextArea.getDocument().getLength());
            });
        }

        // 3. 实时写入本地 TXT 日志
        try (PrintWriter out = new PrintWriter(new FileWriter(new File(Config.getProfileDir(), "woe_bot_report.log"), true))) {
            out.println(formattedMsg);
        } catch (IOException ignored) {}
    }

    // 🌟 新增：用来实时无缝更新进度条的动画和数字
    public static void updateProgress(int percent, String text) {
        if (targetProgressBar != null) {
            SwingUtilities.invokeLater(() -> {
                if (percent >= 0) {
                    if (!targetProgressBar.isVisible()) {
                        targetProgressBar.setVisible(true); // 如果隐藏了就召唤出来
                    }
                    targetProgressBar.setValue(percent);
                    targetProgressBar.setString(text);
                } else {
                    targetProgressBar.setVisible(false); // 传负数时自动隐藏进度条
                }
            });
        }
    }
}