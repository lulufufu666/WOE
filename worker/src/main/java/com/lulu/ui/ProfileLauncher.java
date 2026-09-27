package com.lulu.ui;

import com.lulu.config.Config;
import javax.swing.*;
import java.awt.*;
import java.io.File;

public class ProfileLauncher {

    public static void showLauncher() {
        JFrame frame = new JFrame("WOE 多账号配置管理器");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 320);
        frame.setLayout(new BorderLayout(10, 10));
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(5, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 30, 15, 30));

        JPanel row1 = new JPanel(new BorderLayout(10, 0));
        JLabel l1 = new JLabel("选择账号档案:");
        l1.setFont(new Font("微软雅黑", Font.BOLD, 14));
        row1.add(l1, BorderLayout.WEST);
        JComboBox<String> profileBox = new JComboBox<>();
        profileBox.setFont(new Font("微软雅黑", Font.PLAIN, 14));

        File profilesDir = new File("profiles");
        if (!profilesDir.exists()) profilesDir.mkdirs();
        File[] dirs = profilesDir.listFiles(File::isDirectory);
        if (dirs != null) {
            for (File d : dirs) profileBox.addItem(d.getName());
        }
        if (profileBox.getItemCount() == 0) profileBox.addItem("默认大号");

        if (!"default".equals(Config.PROFILE_NAME)) {
            profileBox.setSelectedItem(Config.PROFILE_NAME);
        }
        row1.add(profileBox, BorderLayout.CENTER);

        JButton newProfileBtn = new JButton("新建档案");
        newProfileBtn.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        row1.add(newProfileBtn, BorderLayout.EAST);

        JPanel row2 = new JPanel(new BorderLayout(10, 0));
        JLabel l2 = new JLabel("游戏窗口名称:");
        l2.setFont(new Font("微软雅黑", Font.BOLD, 14));
        row2.add(l2, BorderLayout.WEST);
        JTextField titleField = new JTextField();
        titleField.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        row2.add(titleField, BorderLayout.CENTER);

        JPanel row3 = new JPanel(new BorderLayout(10, 0));
        JLabel l3 = new JLabel("启动快捷方式:");
        l3.setFont(new Font("微软雅黑", Font.BOLD, 14));
        row3.add(l3, BorderLayout.WEST);
        JTextField cmdField = new JTextField();
        cmdField.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        row3.add(cmdField, BorderLayout.CENTER);

        JButton browseBtn = new JButton("浏览...");
        browseBtn.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        browseBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("选择快捷方式 (.lnk) 或本体 (.exe)");
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home") + "/Desktop"));
            int result = fileChooser.showOpenDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                cmdField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        });
        row3.add(browseBtn, BorderLayout.EAST);

        JLabel hintLabel = new JLabel("<html><font color='gray'>沙盒多开玩家请浏览选择沙盒快捷方式 (.lnk) 或填入完整路径</font></html>");
        hintLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        hintLabel.setHorizontalAlignment(SwingConstants.CENTER);

        profileBox.addActionListener(e -> {
            String pName = (String) profileBox.getSelectedItem();
            if (pName != null) {
                Config.loadProfileSettings(pName);
                titleField.setText(Config.Global.APP_TITLE);
                cmdField.setText(Config.Global.LAUNCH_CMD);
            }
        });
        
        if (profileBox.getSelectedItem() != null) {
            Config.loadProfileSettings((String) profileBox.getSelectedItem());
            titleField.setText(Config.Global.APP_TITLE);
            cmdField.setText(Config.Global.LAUNCH_CMD);
        }

        newProfileBtn.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(frame, "请输入新档案名称：");
            if (name != null && !name.trim().isEmpty()) {
                String cleanName = name.trim();
                profileBox.addItem(cleanName);
                Config.saveProfileSettings(cleanName, "WOE", "");
                profileBox.setSelectedItem(cleanName);
            }
        });

        JButton launchBtn = new JButton("加载独立配置并启动");
        launchBtn.setFont(new Font("微软雅黑", Font.BOLD, 16));
        launchBtn.setBackground(new Color(220, 255, 220));
        launchBtn.addActionListener(e -> {
            String selectedProfile = (String) profileBox.getSelectedItem();
            String windowTitle = titleField.getText().trim();
            String launchCmd = cmdField.getText().trim();

            if (windowTitle.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "窗口名称不能为空！");
                return;
            }

            Config.PROFILE_NAME = selectedProfile;
            Config.Global.APP_TITLE = windowTitle;
            Config.Global.LAUNCH_CMD = launchCmd;
            Config.saveProfileSettings(selectedProfile, windowTitle, launchCmd);

            frame.dispose();
            SwingUtilities.invokeLater(() -> new MainGUI().setVisible(true));
        });

        panel.add(row1);
        panel.add(row2);
        panel.add(row3);
        panel.add(hintLabel);
        panel.add(launchBtn);

        frame.add(panel, BorderLayout.CENTER);
        frame.setVisible(true);
    }
}