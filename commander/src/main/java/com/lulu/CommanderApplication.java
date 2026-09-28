package com.lulu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URI;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling // 开启定时任务支持
public class CommanderApplication {

    public static void main(String[] args) {
        // 强制关闭无头模式
        System.setProperty("java.awt.headless", "false");

        // 启动 Spring Boot
        SpringApplication.run(CommanderApplication.class, args);

        // 初始化系统托盘
        initSystemTray();
    }

    private static void initSystemTray() {
        if (!SystemTray.isSupported()) {
            System.out.println("-> [系统托盘] 当前系统环境不支持托盘图标");
            return;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();
            Image image = createDefaultTrayIconImage();

            PopupMenu popup = new PopupMenu();

            // 🚀 核心优化：采用全英文菜单项，彻底根绝 Windows 下原生 AWT 菜单的方块乱码
            MenuItem openItem = new MenuItem("Open Dashboard");
            openItem.addActionListener(e -> {
                try {
                    Desktop.getDesktop().browse(new URI("http://localhost:8080"));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            MenuItem exitItem = new MenuItem("Exit Commander");
            exitItem.addActionListener(e -> {
                System.out.println("正在关闭中枢程序...");
                System.exit(0);
            });

            popup.add(openItem);
            popup.addSeparator();
            popup.add(exitItem);

            TrayIcon trayIcon = new TrayIcon(image, "WOE Commander Center", popup);
            trayIcon.setImageAutoSize(true);

            // 双击托盘图标打开网页
            trayIcon.addActionListener(e -> {
                try {
                    Desktop.getDesktop().browse(new URI("http://localhost:8080"));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            tray.add(trayIcon);
            System.out.println("-> [系统托盘] 中枢托盘图标已成功加载到右下角！");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Image createDefaultTrayIconImage() {
        int width = 16;
        int height = 16;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(new Color(79, 70, 229)); // 科技 indigo 蓝
        g2d.fillRect(0, 0, width, height);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        g2d.drawString("W", 3, 12);
        g2d.dispose();
        return image;
    }
}