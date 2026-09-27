package com.lulu;

import com.lulu.remote.HttpServerManager;
import com.lulu.ui.ProfileLauncher;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        System.setProperty("sun.java2d.uiScale", "1.0");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            nu.pattern.OpenCV.loadLocally();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // 1. 启动账号配置管理器界面
        ProfileLauncher.showLauncher();

        // 2. 启动后台 HTTP 中控监听服务
        HttpServerManager.startServer();
        
        System.out.println("✅ WOE 自动化引擎初始化彻底完成！");
    }
}