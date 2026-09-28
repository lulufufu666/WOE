package com.lulu.ui;

import com.lulu.config.Config;
import com.lulu.core.HardwareBot;
import com.lulu.logic.BotTask;
import com.lulu.logic.tasks.FleetMasterTask;
import com.lulu.logic.tasks.MaterialDispatchTask;
import com.lulu.logic.tasks.FleetReturnTask;
import com.lulu.tool.LogManager;
import com.lulu.logic.TaskManager;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainGUI extends JFrame {

    public static MainGUI instance;

    private JFrame logFrame;
    private JTextArea logArea;

    private JSpinner radiusSpinner, offsetXSpinner, offsetYSpinner, scrollSpinner;
    private JSpinner roundSpinner, intervalSpinner, repairSpinner, restartRoundSpinner;
    private JCheckBox autoRepairCheckBox, restartCheckBox, secondVerifyCheckBox, lowEndCheckBox;
    private JComboBox<String> filterCombo;

    public static final Map<String, Map<String, Integer>> BOM_TABLE = new HashMap<>();
    static {
        BOM_TABLE.put("一星士兵事件.png", Map.of("陆行舟", 4, "绳索", 30, "藏宝图", 1));
        BOM_TABLE.put("一星炼金师事件.png", Map.of("酸液", 4, "麦酒", 4, "藏宝图", 1));
        BOM_TABLE.put("一星附魔剑事件.png", Map.of("铁匠", 2, "铁钉", 12, "陆行舟", 2, "钢剑", 8, "藏宝图", 1));

        BOM_TABLE.put("二星项链事件.png", Map.of("霜核", 2, "钢铲", 6, "伐木工 等级 2", 6, "藏宝图", 1));
        BOM_TABLE.put("二星珍珠事件.png", Map.of("珍珠", 10, "藏宝图", 1));
        BOM_TABLE.put("二星金钥匙事件.png", Map.of("士兵 lv.2", 2, "钢剑", 2, "藏宝图", 1));

        BOM_TABLE.put("三星核心事件.png", Map.of("炼金术士", 4, "酸液", 12, "维京", 3, "附魔之剑", 3, "麦酒", 12, "沉没宝藏地图", 1));
        BOM_TABLE.put("三星阿比事件.png", Map.of("麦酒", 20, "维京", 4, "钢剑", 12, "治疗师", 3, "治疗药水", 3, "沉没宝藏地图", 1));
        BOM_TABLE.put("三星神圣之剑事件.png", Map.of("神圣宝石", 3, "工程师", 3, "晶体", 1500, "附魔之剑", 1, "沉没宝藏地图", 1));
    }

    public MainGUI() {
        instance = this;
        Config.load();

        com.lulu.remote.HttpServerManager.startServer();

        setTitle("WOE 自动化引擎 Pro - [" + Config.PROFILE_NAME + "]");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        setupLogWindow();

        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.LEFT);
        tabbedPane.setFont(new Font("微软雅黑", Font.BOLD, 16));
        UIManager.put("TabbedPane.tabInsets", new Insets(10, 12, 10, 12));

        tabbedPane.addTab(" 🚀 核心控制台 ", createConsolePanel());
        tabbedPane.addTab(" 🚢 舰队星级设置 ", createFleetConfigPanel());
        tabbedPane.addTab(" ⚙️ 事件策略配置 ", createStrategyConfigPanel());

        JPanel statsWrapper = new JPanel(new BorderLayout());
        statsWrapper.add(createStatsPanel(statsWrapper), BorderLayout.CENTER);
        tabbedPane.addTab(" 📊 历史数据看板 ", statsWrapper);

        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedIndex() == 3) {
                statsWrapper.removeAll();
                statsWrapper.add(createStatsPanel(statsWrapper), BorderLayout.CENTER);
                statsWrapper.revalidate();
                statsWrapper.repaint();
            }
        });

        add(tabbedPane, BorderLayout.CENTER);

        setSize(900, 680);
        setLocationRelativeTo(null);

        // 🚀 核心接入：将全局快捷键交给 TaskManager 处理
        TaskManager.startGlobalHotkeyListener(this);
    }

    public void refreshConfigUI() {
        SwingUtilities.invokeLater(() -> {
            if (roundSpinner != null) roundSpinner.setValue(Config.CruiseConfig.targetRounds);
            if (intervalSpinner != null) intervalSpinner.setValue(Config.CruiseConfig.intervalMinutes);
            if (repairSpinner != null) repairSpinner.setValue(Config.CruiseConfig.repairThreshold);
            if (filterCombo != null) filterCombo.setSelectedIndex(Config.CruiseConfig.repairFilterIndex);
            if (restartCheckBox != null) restartCheckBox.setSelected(Config.CruiseConfig.autoRestartGame == 1);
            if (restartRoundSpinner != null) restartRoundSpinner.setValue(Config.CruiseConfig.restartRounds);
            if (secondVerifyCheckBox != null) secondVerifyCheckBox.setSelected(Config.CruiseConfig.enableSecondVerify);
            if (lowEndCheckBox != null) lowEndCheckBox.setSelected(Config.CruiseConfig.enableLowEndMode);
            if (autoRepairCheckBox != null) autoRepairCheckBox.setSelected(Config.CruiseConfig.enableAutoRepair);
            if (radiusSpinner != null) radiusSpinner.setValue(Config.CruiseConfig.interactRadius);
            if (offsetXSpinner != null) offsetXSpinner.setValue(Config.CruiseConfig.offsetX);
            if (offsetYSpinner != null) offsetYSpinner.setValue(Config.CruiseConfig.offsetY);
            if (scrollSpinner != null) scrollSpinner.setValue(Config.CruiseConfig.scrollSteps);
            this.revalidate();
            this.repaint();
        });
    }

    private JPanel createTitledPanel(String title) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                title, TitledBorder.LEFT, TitledBorder.TOP,
                new Font("微软雅黑", Font.BOLD, 14), new Color(80, 80, 80)
        );
        panel.setBorder(border);
        return panel;
    }

    private JPanel createConsolePanel() {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        radiusSpinner = new JSpinner(new SpinnerNumberModel(Config.CruiseConfig.interactRadius, 50, 2000, 10));
        offsetXSpinner = new JSpinner(new SpinnerNumberModel(Config.CruiseConfig.offsetX, -1000, 1000, 1));
        offsetYSpinner = new JSpinner(new SpinnerNumberModel(Config.CruiseConfig.offsetY, -1000, 1000, 1));
        scrollSpinner = new JSpinner(new SpinnerNumberModel(Config.CruiseConfig.scrollSteps, 0, 50, 1));

        JPanel paramPanel = new JPanel();
        paramPanel.setLayout(new BoxLayout(paramPanel, BoxLayout.Y_AXIS));
        paramPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "⚙️ 巡航参数设置", TitledBorder.LEFT, TitledBorder.TOP,
                new Font("微软雅黑", Font.BOLD, 14), new Color(80, 80, 80)
        ));

        JPanel paramRow1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        JLabel roundLabel = new JLabel("目标巡航轮次:");
        roundLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        roundSpinner = new JSpinner(new SpinnerNumberModel(Config.CruiseConfig.targetRounds, 1, 999, 1));
        roundSpinner.setFont(new Font("微软雅黑", Font.BOLD, 14));

        JLabel intervalLabel = new JLabel("轮次间隔(分):");
        intervalLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        intervalSpinner = new JSpinner(new SpinnerNumberModel(Config.CruiseConfig.intervalMinutes, 0, 999, 1));
        intervalSpinner.setFont(new Font("微软雅黑", Font.BOLD, 14));

        JLabel repairLabel = new JLabel("舰队报警阈值(%):");
        repairLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        repairLabel.setForeground(new Color(200, 50, 50));
        repairSpinner = new JSpinner(new SpinnerNumberModel(Config.CruiseConfig.repairThreshold, 0, 100, 1));
        repairSpinner.setFont(new Font("微软雅黑", Font.BOLD, 14));

        paramRow1.add(roundLabel);
        paramRow1.add(roundSpinner);
        paramRow1.add(Box.createHorizontalStrut(6));
        paramRow1.add(intervalLabel);
        paramRow1.add(intervalSpinner);
        paramRow1.add(Box.createHorizontalStrut(6));
        paramRow1.add(repairLabel);
        paramRow1.add(repairSpinner);

        JPanel paramRow2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        autoRepairCheckBox = new JCheckBox("启用自动维修", Config.CruiseConfig.enableAutoRepair);
        autoRepairCheckBox.setFont(new Font("微软雅黑", Font.BOLD, 14));
        autoRepairCheckBox.setForeground(new Color(200, 50, 50));

        JLabel filterLabel = new JLabel("单船修理档位:");
        filterLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        filterLabel.setForeground(new Color(200, 50, 50));
        filterCombo = new JComboBox<>(new String[]{"低于20%", "低于50%", "低于70%"});
        filterCombo.setFont(new Font("微软雅黑", Font.BOLD, 14));
        filterCombo.setSelectedIndex(Config.CruiseConfig.repairFilterIndex);

        paramRow2.add(autoRepairCheckBox);
        paramRow2.add(Box.createHorizontalStrut(15));
        paramRow2.add(filterLabel);
        paramRow2.add(filterCombo);

        JPanel paramRow3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));

        restartCheckBox = new JCheckBox("自动重启游戏, 每(轮):", Config.CruiseConfig.autoRestartGame == 1);
        restartCheckBox.setFont(new Font("微软雅黑", Font.BOLD, 14));
        restartCheckBox.setForeground(new Color(60, 120, 200));

        restartRoundSpinner = new JSpinner(new SpinnerNumberModel(Config.CruiseConfig.restartRounds, 1, 999, 1));
        restartRoundSpinner.setFont(new Font("微软雅黑", Font.BOLD, 14));

        secondVerifyCheckBox = new JCheckBox("开启星级二次校验", Config.CruiseConfig.enableSecondVerify);
        secondVerifyCheckBox.setFont(new Font("微软雅黑", Font.BOLD, 14));
        secondVerifyCheckBox.setForeground(new Color(60, 120, 200));

        lowEndCheckBox = new JCheckBox("低配云端适配 (防卡顿)", Config.CruiseConfig.enableLowEndMode);
        lowEndCheckBox.setFont(new Font("微软雅黑", Font.BOLD, 14));
        lowEndCheckBox.setForeground(new Color(200, 100, 50));

        paramRow3.add(restartCheckBox);
        paramRow3.add(restartRoundSpinner);
        paramRow3.add(Box.createHorizontalStrut(15));
        paramRow3.add(secondVerifyCheckBox);
        paramRow3.add(Box.createHorizontalStrut(15));
        paramRow3.add(lowEndCheckBox);

        paramPanel.add(paramRow1);
        paramPanel.add(paramRow2);
        paramPanel.add(paramRow3);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "🚀 核心作业控制", TitledBorder.LEFT, TitledBorder.TOP,
                new Font("微软雅黑", Font.BOLD, 14), new Color(80, 80, 80)
        ));

        JButton startBtn = new JButton("▶ 启动自动巡航");
        startBtn.setFont(new Font("微软雅黑", Font.BOLD, 14));
        startBtn.setBackground(new Color(220, 255, 220));
        startBtn.addActionListener(e -> {
            try {
                roundSpinner.commitEdit(); intervalSpinner.commitEdit(); repairSpinner.commitEdit();
                radiusSpinner.commitEdit(); offsetXSpinner.commitEdit(); offsetYSpinner.commitEdit();
                scrollSpinner.commitEdit(); restartRoundSpinner.commitEdit();
            } catch (ParseException ex) {}

            Config.CruiseConfig.targetRounds = (Integer) roundSpinner.getValue();
            Config.CruiseConfig.intervalMinutes = (Integer) intervalSpinner.getValue();
            Config.CruiseConfig.repairThreshold = (Integer) repairSpinner.getValue();
            Config.CruiseConfig.repairFilterIndex = filterCombo.getSelectedIndex();
            Config.CruiseConfig.autoRestartGame = restartCheckBox.isSelected() ? 1 : 0;
            Config.CruiseConfig.restartRounds = (Integer) restartRoundSpinner.getValue();
            Config.CruiseConfig.enableSecondVerify = secondVerifyCheckBox.isSelected();
            Config.CruiseConfig.enableLowEndMode = lowEndCheckBox.isSelected();
            Config.CruiseConfig.enableAutoRepair = autoRepairCheckBox.isSelected();
            Config.saveCruiseConfig();

            // 🚀 核心修复：移交 TaskManager
            TaskManager.startCruise(false);
        });

        JButton stopBtn = new JButton("⏹ 紧急停止 (ESC)");
        stopBtn.setFont(new Font("微软雅黑", Font.BOLD, 14));
        stopBtn.setBackground(new Color(255, 220, 220));
        stopBtn.setForeground(Color.RED);
        // 🚀 核心修复：移交 TaskManager
        stopBtn.addActionListener(e -> TaskManager.stopAllTasks(true));

        JButton dispatchBtn = new JButton("📦 一键物资补货");
        dispatchBtn.setFont(new Font("微软雅黑", Font.BOLD, 14));
        dispatchBtn.setBackground(new Color(230, 240, 255));
        dispatchBtn.addActionListener(e -> openDispatchDialog());

        JButton clearInvBtn = new JButton("🚚 一键指定清仓");
        clearInvBtn.setFont(new Font("微软雅黑", Font.BOLD, 14));
        clearInvBtn.setBackground(new Color(255, 235, 205));
        clearInvBtn.addActionListener(e -> openClearInventoryDialog());

        JButton switchProfileBtn = new JButton("🔄 切换账号");
        switchProfileBtn.setFont(new Font("微软雅黑", Font.BOLD, 14));
        switchProfileBtn.setBackground(new Color(240, 230, 255));
        switchProfileBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "确认要返回账号配置界面吗？\n(⚠️ 如果有正在运行的任务将会被紧急停止)",
                    "切换账号", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                // 🚀 核心修复：移交 TaskManager
                if (TaskManager.isBusy()) TaskManager.stopAllTasks(false);
                if (logFrame != null) logFrame.dispose();
                this.dispose();
                MainGUI.showProfileLauncher();
            }
        });

        JPanel cRow1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        cRow1.add(startBtn);
        cRow1.add(stopBtn);
        cRow1.add(switchProfileBtn);

        JPanel cRow2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        cRow2.add(dispatchBtn);
        cRow2.add(clearInvBtn);

        controlPanel.add(cRow1);
        controlPanel.add(cRow2);

        // 🚀 新增：云端中枢通信配置面板
        JPanel remotePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        remotePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "🌐 云端中枢通信配置 (SD-WAN / Tailscale)", TitledBorder.LEFT, TitledBorder.TOP,
                new Font("微软雅黑", Font.BOLD, 14), new Color(80, 80, 80)
        ));

        JCheckBox cloudEnableBox = new JCheckBox("开启集群远控", Config.Remote.enableCloud);
        cloudEnableBox.setFont(new Font("微软雅黑", Font.BOLD, 14));
        cloudEnableBox.setForeground(new Color(40, 100, 180));

        JLabel ipLabel = new JLabel("中枢网络IP:");
        ipLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        JTextField ipField = new JTextField(Config.Remote.commanderIp, 12);
        ipField.setFont(new Font("Consolas", Font.PLAIN, 14));

        JButton saveRemoteBtn = new JButton("保存网络设置");
        saveRemoteBtn.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        saveRemoteBtn.addActionListener(e -> {
            Config.Remote.enableCloud = cloudEnableBox.isSelected();
            Config.Remote.commanderIp = ipField.getText().trim();
            Config.saveRemoteConfig();

            // 🚀 核心修改：动态控制后台服务的生死
            if (Config.Remote.enableCloud) {
                com.lulu.remote.HttpServerManager.startServer();
                JOptionPane.showMessageDialog(this, "云端通信配置已保存！\n微服务端口已分配，心跳包已自动生效。");
            } else {
                com.lulu.remote.HttpServerManager.stopServer();
                JOptionPane.showMessageDialog(this, "云端通信已关闭！\n后台微服务已销毁，端口完全释放。");
            }
        });

        remotePanel.add(cloudEnableBox);
        remotePanel.add(Box.createHorizontalStrut(15));
        remotePanel.add(ipLabel);
        remotePanel.add(ipField);
        remotePanel.add(Box.createHorizontalStrut(5));
        remotePanel.add(saveRemoteBtn);


        JPanel debugPanel = new JPanel();
        debugPanel.setLayout(new BoxLayout(debugPanel, BoxLayout.Y_AXIS));
        TitledBorder debugBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "🔧 高级调试与参数微调", TitledBorder.LEFT, TitledBorder.TOP,
                new Font("微软雅黑", Font.BOLD, 14), new Color(80, 80, 80)
        );
        debugPanel.setBorder(debugBorder);

        JPanel debugRow1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        debugRow1.add(new JLabel("圆圈半径:"));
        debugRow1.add(radiusSpinner);
        debugRow1.add(new JLabel("偏移X:"));
        debugRow1.add(offsetXSpinner);
        debugRow1.add(new JLabel("偏移Y:"));
        debugRow1.add(offsetYSpinner);
        debugRow1.add(new JLabel("缩放滚轮(次):"));
        debugRow1.add(scrollSpinner);

        radiusSpinner.addChangeListener(e -> { Config.CruiseConfig.interactRadius = (Integer) radiusSpinner.getValue(); Config.saveCruiseConfig(); });
        offsetXSpinner.addChangeListener(e -> { Config.CruiseConfig.offsetX = (Integer) offsetXSpinner.getValue(); Config.saveCruiseConfig(); });
        offsetYSpinner.addChangeListener(e -> { Config.CruiseConfig.offsetY = (Integer) offsetYSpinner.getValue(); Config.saveCruiseConfig(); });
        scrollSpinner.addChangeListener(e -> { Config.CruiseConfig.scrollSteps = (Integer) scrollSpinner.getValue(); Config.saveCruiseConfig(); });

        JPanel debugRow2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        JButton testCircleBtn = new JButton("探测交互边界 (测圆)");
        testCircleBtn.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        testCircleBtn.addActionListener(e -> {
            try {
                radiusSpinner.commitEdit(); offsetXSpinner.commitEdit(); offsetYSpinner.commitEdit(); scrollSpinner.commitEdit();
            } catch (ParseException ex) {}
            // 🚀 核心修复：移交 TaskManager
            TaskManager.testInteractionCircle();
        });

        JCheckBox debugCheckBox = new JCheckBox("启用视觉锚点红框", Config.Global.DEBUG_MODE);
        debugCheckBox.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        debugCheckBox.addActionListener(e -> {
            Config.Global.DEBUG_MODE = debugCheckBox.isSelected();
        });

        JButton showLogBtn = new JButton("唤起/隐藏日志台");
        showLogBtn.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        showLogBtn.addActionListener(e -> logFrame.setVisible(!logFrame.isVisible()));

        debugRow2.add(testCircleBtn);
        debugRow2.add(Box.createHorizontalStrut(8));
        debugRow2.add(debugCheckBox);
        debugRow2.add(Box.createHorizontalStrut(8));
        debugRow2.add(showLogBtn);

        debugPanel.add(debugRow1);
        debugPanel.add(debugRow2);

        contentPanel.add(paramPanel);
        contentPanel.add(Box.createVerticalStrut(8));
        contentPanel.add(controlPanel);
        contentPanel.add(Box.createVerticalStrut(8));
        contentPanel.add(remotePanel);
        contentPanel.add(Box.createVerticalStrut(8));
        contentPanel.add(debugPanel);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(contentPanel, BorderLayout.NORTH);
        return wrapper;
    }

    // =========================================================================
    // 🌟 舰队星级设置面板
    // =========================================================================
    private JPanel createFleetConfigPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        String[] columnNames = {"舰队名称", "一星", "二星", "三星", "四星", "五星"};
        String[] allStars = {"一星", "二星", "三星", "四星", "五星"};

        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                return column == 0 ? String.class : Boolean.class;
            }
        };

        for (Map.Entry<String, List<String>> entry : Config.FleetConfig.TASK_MAP.entrySet()) {
            String fleetName = entry.getKey();
            List<String> assignedStars = entry.getValue();
            Object[] rowData = new Object[6];
            rowData[0] = fleetName;
            for (int s = 0; s < allStars.length; s++) {
                rowData[s + 1] = assignedStars.contains(allStars[s]);
            }
            tableModel.addRow(rowData);
        }

        JTable table = new JTable(tableModel);
        setupTableStyle(table);
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));

        JPanel topToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 10));
        JButton addFleetBtn = new JButton("➕ 添加新舰队");
        JButton delFleetBtn = new JButton("➖ 删除选中舰队");
        delFleetBtn.setForeground(Color.RED);

        addFleetBtn.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        delFleetBtn.setFont(new Font("微软雅黑", Font.PLAIN, 13));

        addFleetBtn.addActionListener(e -> {
            String newName = JOptionPane.showInputDialog(this, "请输入新舰队名称（例如：A-巡航组-9）：");
            if (newName != null && !newName.trim().isEmpty()) {
                tableModel.addRow(new Object[]{newName.trim(), true, true, true, true, true});
            }
        });

        delFleetBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1) {
                tableModel.removeRow(selectedRow);
            } else {
                JOptionPane.showMessageDialog(this, "请先在表格中点击选中要删除的舰队行！");
            }
        });

        topToolbar.add(addFleetBtn);
        topToolbar.add(Box.createHorizontalStrut(10));
        topToolbar.add(delFleetBtn);

        JPanel quickTogglePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
        JLabel quickLabel = new JLabel("批量全选/清空: ");
        quickLabel.setFont(new Font("微软雅黑", Font.BOLD, 13));
        quickTogglePanel.add(quickLabel);
        quickTogglePanel.add(Box.createHorizontalStrut(10));

        for (int i = 0; i < allStars.length; i++) {
            int colIndex = i + 1;
            JButton toggleBtn = new JButton(allStars[i]);
            toggleBtn.setFont(new Font("微软雅黑", Font.PLAIN, 12));
            toggleBtn.setBackground(new Color(240, 248, 255));

            toggleBtn.addActionListener(e -> {
                if (table.isEditing()) {
                    table.getCellEditor().stopCellEditing();
                }
                boolean anyUnselected = false;
                for (int row = 0; row < tableModel.getRowCount(); row++) {
                    Boolean val = (Boolean) tableModel.getValueAt(row, colIndex);
                    if (val == null || !val) {
                        anyUnselected = true;
                        break;
                    }
                }
                for (int row = 0; row < tableModel.getRowCount(); row++) {
                    tableModel.setValueAt(anyUnselected, row, colIndex);
                }
            });
            quickTogglePanel.add(toggleBtn);
            quickTogglePanel.add(Box.createHorizontalStrut(5));
        }

        northPanel.add(topToolbar);
        northPanel.add(quickTogglePanel);
        panel.add(northPanel, BorderLayout.NORTH);

        JButton saveBtn = new JButton("💾 保存星级配置");
        saveBtn.setFont(new Font("微软雅黑", Font.BOLD, 15));
        saveBtn.setPreferredSize(new Dimension(0, 45));
        saveBtn.setBackground(new Color(220, 255, 220));

        saveBtn.addActionListener(e -> {
            if (table.isEditing()) {
                table.getCellEditor().stopCellEditing();
            }
            Map<String, List<String>> newMap = new LinkedHashMap<>();

            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String fleetName = (String) tableModel.getValueAt(i, 0);
                if (fleetName != null && !fleetName.trim().isEmpty()) {
                    List<String> activeStars = new ArrayList<>();
                    for (int s = 0; s < allStars.length; s++) {
                        Boolean isChecked = (Boolean) tableModel.getValueAt(i, s + 1);
                        if (isChecked != null && isChecked) {
                            activeStars.add(allStars[s]);
                        }
                    }
                    newMap.put(fleetName.trim(), activeStars);
                }
            }
            Config.FleetConfig.TASK_MAP = newMap;
            Config.saveFleetConfig();
            JOptionPane.showMessageDialog(this, "✅ 舰队动态配置保存成功！");
        });

        panel.add(saveBtn, BorderLayout.SOUTH);
        return panel;
    }

    // =========================================================================
    // 🌟 事件策略配置面板
    // =========================================================================
    private JPanel createStrategyConfigPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        Map<String, Map<String, JComboBox<String>>> starComboMap = new LinkedHashMap<>();

        for (Map.Entry<String, Map<String, String>> starEntry : Config.Strategy.EVENT_MAP.entrySet()) {
            String starName = starEntry.getKey();
            Map<String, String> eventMap = starEntry.getValue();

            JPanel starPanel = new JPanel(new GridLayout(0, 2, 10, 8));
            starPanel.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(new Color(180, 180, 180)),
                    " " + starName + "星级事件策略 ", TitledBorder.LEFT, TitledBorder.TOP,
                    new Font("微软雅黑", Font.BOLD, 14), new Color(50, 50, 50)
            ));

            Map<String, JComboBox<String>> comboRow = new HashMap<>();
            for (Map.Entry<String, String> eventEntry : eventMap.entrySet()) {
                String eventName = eventEntry.getKey();
                String currentAction = eventEntry.getValue();

                JLabel label = new JLabel(eventName.replace(".png", ""));
                label.setFont(new Font("微软雅黑", Font.PLAIN, 13));
                label.setHorizontalAlignment(SwingConstants.RIGHT);

                String[] choices = new String[]{"冒险.png", "忽略.png"};
                if (currentAction.equals("绕过.png") || currentAction.equals("等待.png")) {
                    choices = new String[]{"绕过.png", "等待.png"};
                } else if (currentAction.equals("战斗.png") || currentAction.equals("付费.png")) {
                    choices = new String[]{"战斗.png", "付费.png"};
                }

                JComboBox<String> combo = new JComboBox<>(choices);
                combo.setFont(new Font("微软雅黑", Font.PLAIN, 13));
                combo.setSelectedItem(currentAction);

                comboRow.put(eventName, combo);
                starPanel.add(label);
                starPanel.add(combo);
            }
            starComboMap.put(starName, comboRow);
            mainPanel.add(starPanel);
            mainPanel.add(Box.createVerticalStrut(10));
        }

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(null);
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton saveBtn = new JButton("💾 保存策略并更新配置");
        saveBtn.setFont(new Font("微软雅黑", Font.BOLD, 15));
        saveBtn.setPreferredSize(new Dimension(0, 45));
        saveBtn.setBackground(new Color(220, 255, 220));

        saveBtn.addActionListener(e -> {
            for (Map.Entry<String, Map<String, JComboBox<String>>> starEntry : starComboMap.entrySet()) {
                String starName = starEntry.getKey();
                Map<String, JComboBox<String>> comboRow = starEntry.getValue();
                Map<String, String> targetEventMap = Config.Strategy.EVENT_MAP.get(starName);

                if (targetEventMap != null) {
                    for (Map.Entry<String, JComboBox<String>> comboEntry : comboRow.entrySet()) {
                        String eventName = comboEntry.getKey();
                        String selectedAction = (String) comboEntry.getValue().getSelectedItem();
                        targetEventMap.put(eventName, selectedAction);
                    }
                }
            }
            Config.save();
            JOptionPane.showMessageDialog(this, "✅ 海域事件策略保存成功，立即生效！");
        });

        panel.add(saveBtn, BorderLayout.SOUTH);
        return panel;
    }

    // =========================================================================
    // 🌟 历史数据看板面板 (专业结局分布版)
    // =========================================================================
    private JPanel createStatsPanel(JPanel wrapper) {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        int totalFound = Config.GlobalStats.totalFound;
        int totalSuccess = Config.GlobalStats.totalSuccess;
        int totalFail = Config.GlobalStats.totalFail;
        int totalPlanB = Config.GlobalStats.totalPlanB;
        int totalIgnore = Config.GlobalStats.totalIgnore;

        JPanel topWrapper = new JPanel(new BorderLayout());
        JLabel totalLabel = new JLabel(" 🎯 历史总计遇到合法事件: " + totalFound + " 次");
        totalLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
        totalLabel.setForeground(new Color(50, 50, 50));
        totalLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 10, 0));

        JPanel overviewPanel = new JPanel(new GridLayout(1, 4, 10, 10));

        int percentSuccess = totalFound > 0 ? (int)(totalSuccess * 100.0 / totalFound) : 0;
        int percentPlanB = totalFound > 0 ? (int)(totalPlanB * 100.0 / totalFound) : 0;
        int percentIgnore = totalFound > 0 ? (int)(totalIgnore * 100.0 / totalFound) : 0;
        int percentFail = totalFound > 0 ? (int)(totalFail * 100.0 / totalFound) : 0;

        overviewPanel.add(createStatCard("✅ 成功收割", String.format("%d 次 (%d%%)", totalSuccess, percentSuccess), new Color(46, 139, 87)));
        overviewPanel.add(createStatCard("🪙 破财免灾", String.format("%d 次 (%d%%)", totalPlanB, percentPlanB), new Color(255, 140, 0)));
        overviewPanel.add(createStatCard("⏭️ 主动忽略", String.format("%d 次 (%d%%)", totalIgnore, percentIgnore), new Color(128, 128, 128)));
        overviewPanel.add(createStatCard("❌ 失败/拦截", String.format("%d 次 (%d%%)", totalFail, percentFail), new Color(205, 92, 92)));

        topWrapper.add(totalLabel, BorderLayout.NORTH);
        topWrapper.add(overviewPanel, BorderLayout.CENTER);

        JPanel chartPanel = new JPanel();
        chartPanel.setLayout(new BoxLayout(chartPanel, BoxLayout.Y_AXIS));
        chartPanel.setBackground(Color.WHITE);

        Map<String, Integer> starsMap = Config.GlobalStats.starCountMap;
        Map<String, Integer> eventsMap = Config.GlobalStats.eventCountMap;

        String[] starLevels = {"一星", "二星", "三星", "四星", "五星"};
        for (String star : starLevels) {
            int starTotal = starsMap.getOrDefault(star, 0);
            if (starTotal > 0) {
                int percentOfAll = totalFound > 0 ? (int)(starTotal * 100.0 / totalFound) : 0;
                JLabel header = new JLabel(String.format(" %s事件总计: %d 次 (占大盘总比例 %d%%)", star, starTotal, percentOfAll));
                header.setFont(new Font("微软雅黑", Font.BOLD, 15));
                header.setForeground(new Color(60, 60, 60));
                header.setBorder(BorderFactory.createEmptyBorder(15, 10, 5, 10));
                chartPanel.add(header);

                for (Map.Entry<String, Integer> entry : eventsMap.entrySet()) {
                    String eventName = entry.getKey();
                    if (eventName.startsWith(star)) {
                        int eCount = entry.getValue();
                        int percentInStar = (int)(eCount * 100.0 / starTotal);
                        int percentInGlobal = totalFound > 0 ? (int)(eCount * 100.0 / totalFound) : 0;

                        JPanel row = new JPanel(new BorderLayout(15, 0));
                        row.setBorder(BorderFactory.createEmptyBorder(5, 25, 5, 25));
                        row.setBackground(Color.WHITE);

                        JLabel label = new JLabel(eventName.replace(star, ""));
                        label.setFont(new Font("微软雅黑", Font.PLAIN, 13));
                        label.setPreferredSize(new Dimension(80, 0));
                        label.setHorizontalAlignment(SwingConstants.RIGHT);

                        JProgressBar bar = new JProgressBar(0, 100);
                        bar.setValue(percentInStar);
                        bar.setStringPainted(true);
                        bar.setString(String.format("%d 次 (本星级内: %d%% | 全局总占比: %d%%)", eCount, percentInStar, percentInGlobal));
                        bar.setFont(new Font("微软雅黑", Font.BOLD, 12));

                        if (percentInStar > 50) bar.setForeground(new Color(255, 165, 0));
                        else if (percentInStar > 20) bar.setForeground(new Color(135, 206, 235));
                        else bar.setForeground(new Color(180, 220, 180));

                        row.add(label, BorderLayout.WEST);
                        row.add(bar, BorderLayout.CENTER);
                        chartPanel.add(row);
                    }
                }

                JSeparator separator = new JSeparator();
                separator.setForeground(new Color(230, 230, 230));
                chartPanel.add(separator);
            }
        }

        JScrollPane scrollChart = new JScrollPane(chartPanel);
        scrollChart.getVerticalScrollBar().setUnitIncrement(16);
        scrollChart.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                " 🌟 详细事件嵌套占比 ", TitledBorder.LEFT, TitledBorder.TOP,
                new Font("微软雅黑", Font.BOLD, 14), new Color(80, 80, 80)
        ));

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        JButton openHistoryBtn = new JButton("📜 查阅历史巡航报告");
        openHistoryBtn.setFont(new Font("微软雅黑", Font.BOLD, 13));
        openHistoryBtn.setForeground(new Color(40, 100, 180));
        openHistoryBtn.addActionListener(e -> openHistoryRecordsDialog());

        JButton clearBtn = new JButton("清除历史数据");
        clearBtn.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        clearBtn.setForeground(Color.RED);
        clearBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "确定要清空所有挂机历史数据吗？此操作不可逆！", "警告", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                Config.GlobalStats.totalFound = 0;
                Config.GlobalStats.totalSuccess = 0;
                Config.GlobalStats.totalFail = 0;
                Config.GlobalStats.totalPlanB = 0;
                Config.GlobalStats.totalIgnore = 0;
                Config.GlobalStats.starCountMap.clear();
                Config.GlobalStats.eventCountMap.clear();
                Config.saveStatsConfig();

                if (wrapper != null) {
                    wrapper.removeAll();
                    wrapper.add(createStatsPanel(wrapper), BorderLayout.CENTER);
                    wrapper.revalidate();
                    wrapper.repaint();
                }
            }
        });

        bottomPanel.add(openHistoryBtn);
        bottomPanel.add(clearBtn);

        panel.add(topWrapper, BorderLayout.NORTH);
        panel.add(scrollChart, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void openHistoryRecordsDialog() {
        JDialog dialog = new JDialog(this, "📜 历史巡航记录与报表阅览", true);
        dialog.setSize(850, 600);
        dialog.setLayout(new BorderLayout(10, 10));

        File dir = new File(Config.getProfileDir() + "巡航数据报表");
        if (!dir.exists()) dir.mkdirs();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt") || name.endsWith(".log"));

        if (files != null) {
            Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
        }

        DefaultListModel<String> listModel = new DefaultListModel<>();
        if (files != null) {
            for (File f : files) {
                listModel.addElement(f.getName());
            }
        }

        JList<String> fileList = new JList<>(listModel);
        fileList.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane listScroll = new JScrollPane(fileList);
        listScroll.setPreferredSize(new Dimension(280, 0));
        listScroll.setBorder(BorderFactory.createTitledBorder("文件列表 (按时间降序)"));

        JTextArea contentArea = new JTextArea("👈 请在左侧选择一个日志文件查看详细内容...");
        contentArea.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        contentArea.setBackground(new Color(40, 44, 52));
        contentArea.setForeground(new Color(171, 178, 191));
        contentArea.setEditable(false);
        JScrollPane contentScroll = new JScrollPane(contentArea);
        contentScroll.setBorder(BorderFactory.createTitledBorder("报告内容阅览区"));

        fileList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = fileList.getSelectedValue();
                if (selected != null) {
                    File selectedFile = new File(dir, selected);
                    if (selected.endsWith(".txt") || selected.endsWith(".log")) {
                        try {
                            String content = new String(Files.readAllBytes(selectedFile.toPath()), StandardCharsets.UTF_8);
                            contentArea.setText(content);
                            contentArea.setCaretPosition(0);
                        } catch (Exception ex) {
                            contentArea.setText("❌ 读取文件失败: " + ex.getMessage());
                        }
                    }
                }
            }
        });

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton openDirBtn = new JButton("📂 打开所在文件夹");
        openDirBtn.addActionListener(e -> {
            try { Desktop.getDesktop().open(dir); } catch (Exception ex) {}
        });

        JButton openExternalBtn = new JButton("📄 用外部程序打开选中文件");
        openExternalBtn.setBackground(new Color(230, 240, 255));
        openExternalBtn.addActionListener(e -> {
            String selected = fileList.getSelectedValue();
            if (selected != null) {
                try { Desktop.getDesktop().open(new File(dir, selected)); } catch (Exception ex) {}
            } else {
                JOptionPane.showMessageDialog(dialog, "请先在左侧选择一个文件！");
            }
        });

        bottomPanel.add(openDirBtn);
        bottomPanel.add(openExternalBtn);

        dialog.add(listScroll, BorderLayout.WEST);
        dialog.add(contentScroll, BorderLayout.CENTER);
        dialog.add(bottomPanel, BorderLayout.SOUTH);

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JPanel createStatCard(String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(color, 2, true));
        card.setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        JLabel valueLabel = new JLabel(value, SwingConstants.CENTER);
        valueLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
        valueLabel.setForeground(color);
        valueLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 15, 0));

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private void setupLogWindow() {
        logFrame = new JFrame("📜 WOE 运行日志 (实时监听)");
        logFrame.setSize(500, 380);
        logFrame.setAlwaysOnTop(true);
        logFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        logFrame.setLayout(new BorderLayout());

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(new Color(220, 220, 220));
        logArea.setLineWrap(true);

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        logFrame.add(scrollPane, BorderLayout.CENTER);

        JProgressBar waitProgressBar = new JProgressBar(0, 100);
        waitProgressBar.setStringPainted(true);
        waitProgressBar.setFont(new Font("微软雅黑", Font.BOLD, 14));
        waitProgressBar.setVisible(false);
        waitProgressBar.setPreferredSize(new Dimension(500, 30));
        logFrame.add(waitProgressBar, BorderLayout.SOUTH);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Insets scnMax = Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration());
        int taskBarHeight = scnMax.bottom;

        int x = screenSize.width - logFrame.getWidth() - 20;
        int y = screenSize.height - logFrame.getHeight() - taskBarHeight - 20;
        logFrame.setLocation(x, y);

        LogManager.setTargetTextArea(logArea);
        LogManager.setProgressBar(waitProgressBar);
    }

    private void setupTableStyle(JTable table) {
        table.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        table.setRowHeight(35);
        table.getTableHeader().setFont(new Font("微软雅黑", Font.BOLD, 15));
        table.getTableHeader().setPreferredSize(new Dimension(0, 40));
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.setDefaultRenderer(String.class, centerRenderer);
        table.setDefaultRenderer(Integer.class, centerRenderer);
    }

    private int getStarRank(String eventName) {
        if (eventName.startsWith("一星")) return 1;
        if (eventName.startsWith("二星")) return 2;
        if (eventName.startsWith("三星")) return 3;
        if (eventName.startsWith("四星")) return 4;
        if (eventName.startsWith("五星")) return 5;
        return 99;
    }

    private void openDispatchDialog() {
        // 🚀 核心接入 TaskManager
        if (TaskManager.isBusy()) {
            JOptionPane.showMessageDialog(this, "⚠️ 当前已有任务正在运行，请先停止！");
            return;
        }

        JDialog dialog = new JDialog(this, "自动化物资补给 (目标舰队与表单管理)", true);
        dialog.setSize(850, 600);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topPanel.setBorder(BorderFactory.createTitledBorder("预设模板选择"));

        JComboBox<String> tplCombo = new JComboBox<>();
        tplCombo.addItem("【新建空白表单】");
        for (String tplName : Config.DispatchConfig.TEMPLATES.keySet()) {
            tplCombo.addItem(tplName);
        }

        JButton saveTplBtn = new JButton("保存当前填写的为新模板");
        JButton delTplBtn = new JButton("删除选中模板");
        delTplBtn.setForeground(Color.RED);

        topPanel.add(new JLabel("模板："));
        topPanel.add(tplCombo);
        topPanel.add(saveTplBtn);
        topPanel.add(delTplBtn);
        dialog.add(topPanel, BorderLayout.NORTH);

        JPanel fleetPanel = new JPanel();
        fleetPanel.setLayout(new BoxLayout(fleetPanel, BoxLayout.Y_AXIS));
        fleetPanel.setBorder(BorderFactory.createTitledBorder("指定补货舰队"));

        List<JCheckBox> fleetCheckBoxes = new ArrayList<>();
        int index = 0;
        for (String fleetName : Config.FleetConfig.TASK_MAP.keySet()) {
            JCheckBox cb = new JCheckBox((index + 1) + ". " + fleetName);
            cb.setSelected(true);
            cb.setFont(new Font("微软雅黑", Font.PLAIN, 14));
            fleetCheckBoxes.add(cb);
            fleetPanel.add(cb);
            index++;
        }

        JButton selectAllBtn = new JButton("全选 / 全不选");
        selectAllBtn.addActionListener(e -> {
            boolean anyUnselected = fleetCheckBoxes.stream().anyMatch(cb -> !cb.isSelected());
            for (JCheckBox cb : fleetCheckBoxes) {
                cb.setSelected(anyUnselected);
            }
        });
        fleetPanel.add(Box.createVerticalStrut(10));
        fleetPanel.add(selectAllBtn);

        JScrollPane fleetScroll = new JScrollPane(fleetPanel);
        fleetScroll.setPreferredSize(new Dimension(200, 0));

        List<String> sortedKeys = new ArrayList<>(BOM_TABLE.keySet());
        if (!sortedKeys.contains("修复包")) sortedKeys.add("修复包");
        if (!sortedKeys.contains("小型修复包")) sortedKeys.add("小型修复包");

        sortedKeys.sort((a, b) -> {
            boolean aIsItem = a.equals("修复包") || a.equals("小型修复包");
            boolean bIsItem = b.equals("修复包") || b.equals("小型修复包");
            if (aIsItem && !bIsItem) return -1;
            if (!aIsItem && bIsItem) return 1;
            if (aIsItem && bIsItem) return a.equals("修复包") ? -1 : 1;

            int rankA = getStarRank(a);
            int rankB = getStarRank(b);
            if (rankA != rankB) return Integer.compare(rankA, rankB);
            return a.compareTo(b);
        });

        JPanel formPanel = new JPanel(new GridLayout(0, 2, 20, 15));
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 40, 15, 40));

        Map<String, JTextField> inputFieldsMap = new LinkedHashMap<>();
        Font labelFont = new Font("微软雅黑", Font.BOLD, 15);
        Font inputFont = new Font("微软雅黑", Font.PLAIN, 16);

        for (String key : sortedKeys) {
            JLabel label = new JLabel(key.replace(".png", ""));
            label.setFont(labelFont);
            if (key.equals("修复包") || key.equals("小型修复包")) {
                label.setForeground(new Color(220, 50, 50));
            }
            label.setHorizontalAlignment(SwingConstants.RIGHT);

            JTextField tf = new JTextField();
            tf.setFont(inputFont);
            tf.setHorizontalAlignment(JTextField.CENTER);
            tf.setText("");

            inputFieldsMap.put(key, tf);
            formPanel.add(label);
            formPanel.add(tf);
        }

        JScrollPane formScroll = new JScrollPane(formPanel);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel mainContentPanel = new JPanel(new BorderLayout(10, 10));
        mainContentPanel.add(fleetScroll, BorderLayout.WEST);
        mainContentPanel.add(formScroll, BorderLayout.CENTER);
        dialog.add(mainContentPanel, BorderLayout.CENTER);

        tplCombo.addActionListener(e -> {
            String selected = (String) tplCombo.getSelectedItem();
            if (selected != null && Config.DispatchConfig.TEMPLATES.containsKey(selected)) {
                Map<String, Integer> tplData = Config.DispatchConfig.TEMPLATES.get(selected);
                for (Map.Entry<String, JTextField> entry : inputFieldsMap.entrySet()) {
                    int val = tplData.getOrDefault(entry.getKey(), 0);
                    entry.getValue().setText(val == 0 ? "" : String.valueOf(val));
                }
            } else {
                for (JTextField tf : inputFieldsMap.values()) {
                    tf.setText("");
                }
            }
        });

        saveTplBtn.addActionListener(e -> {
            String tplName = JOptionPane.showInputDialog(dialog, "请为这个配置起个名字：");
            if (tplName != null && !tplName.trim().isEmpty()) {
                Map<String, Integer> currentData = new HashMap<>();
                for (Map.Entry<String, JTextField> entry : inputFieldsMap.entrySet()) {
                    String text = entry.getValue().getText().trim();
                    if (!text.isEmpty()) {
                        try {
                            int val = Integer.parseInt(text);
                            if (val > 0) currentData.put(entry.getKey(), val);
                        } catch (NumberFormatException ignored) {}
                    }
                }

                if (currentData.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "表单是空的，无需保存！");
                    return;
                }

                Config.DispatchConfig.TEMPLATES.put(tplName.trim(), currentData);
                Config.saveDispatchTemplates();

                boolean exists = false;
                for (int i = 0; i < tplCombo.getItemCount(); i++) {
                    if (tplCombo.getItemAt(i).equals(tplName.trim())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) tplCombo.addItem(tplName.trim());
                tplCombo.setSelectedItem(tplName.trim());
                JOptionPane.showMessageDialog(dialog, "✅ 模板 [" + tplName + "] 保存成功！");
            }
        });

        delTplBtn.addActionListener(e -> {
            String selected = (String) tplCombo.getSelectedItem();
            if (selected != null && Config.DispatchConfig.TEMPLATES.containsKey(selected)) {
                Config.DispatchConfig.TEMPLATES.remove(selected);
                Config.saveDispatchTemplates();
                tplCombo.removeItem(selected);
                JOptionPane.showMessageDialog(dialog, "✅ 模板已删除！");
            }
        });

        JButton startDispatchBtn = new JButton("计算汇总");
        startDispatchBtn.setFont(new Font("微软雅黑", Font.BOLD, 16));
        startDispatchBtn.setPreferredSize(new Dimension(0, 50));
        startDispatchBtn.setBackground(new Color(230, 240, 255));

        startDispatchBtn.addActionListener(e -> {
            List<Integer> selectedIndices = new ArrayList<>();
            for (int i = 0; i < fleetCheckBoxes.size(); i++) {
                if (fleetCheckBoxes.get(i).isSelected()) {
                    selectedIndices.add(i);
                }
            }
            if (selectedIndices.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "❌ 请至少在左侧勾选一个要补货的舰队！", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Map<String, Integer> dispatchMap = new LinkedHashMap<>();
            int totalSets = 0;

            for (Map.Entry<String, JTextField> entry : inputFieldsMap.entrySet()) {
                String text = entry.getValue().getText().trim();
                if (!text.isEmpty()) {
                    try {
                        int sets = Integer.parseInt(text);
                        if (sets > 0) {
                            dispatchMap.put(entry.getKey(), sets);
                            totalSets += sets;
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(dialog, "❌ 错误：输入框内必须是纯数字！", "格式错误", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
            }

            if (totalSets == 0) {
                int choice = JOptionPane.showConfirmDialog(dialog,
                        "您没有填写任何需要配送的事件套数，系统将不计算配方物资。\n\n是否直接跳过事件计算，进入下一步单独发送独立物资？",
                        "提示", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (choice != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            Map<String, Integer> calculatedMaterials = new HashMap<>();
            for (Map.Entry<String, Integer> entry : dispatchMap.entrySet()) {
                String eventName = entry.getKey();
                int sets = entry.getValue();

                if (eventName.equals("修复包") || eventName.equals("小型修复包")) {
                    calculatedMaterials.put(eventName, calculatedMaterials.getOrDefault(eventName, 0) + sets);
                } else {
                    Map<String, Integer> bom = BOM_TABLE.get(eventName);
                    if (bom != null) {
                        for (Map.Entry<String, Integer> mat : bom.entrySet()) {
                            calculatedMaterials.put(mat.getKey(), calculatedMaterials.getOrDefault(mat.getKey(), 0) + (mat.getValue() * sets));
                        }
                    }
                }
            }

            if (calculatedMaterials.isEmpty() && totalSets > 0) {
                JOptionPane.showMessageDialog(dialog, "❌ 错误：所选事件没有配置对应的消耗配方(BOM)！");
                return;
            }

            openMaterialReviewDialog(calculatedMaterials, selectedIndices, dialog);
        });

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(startDispatchBtn, BorderLayout.CENTER);
        dialog.add(bottomPanel, BorderLayout.SOUTH);

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void openMaterialReviewDialog(Map<String, Integer> calculatedMaterials, List<Integer> selectedIndices, JDialog parentDialog) {
        JDialog reviewDialog = new JDialog(this, "📝 实际装车清单核对与调整", true);
        reviewDialog.setSize(450, 580);
        reviewDialog.setLayout(new BorderLayout(10, 10));

        if (!calculatedMaterials.containsKey("修复包")) {
            calculatedMaterials.put("修复包", 0);
        }
        if (!calculatedMaterials.containsKey("小型修复包")) {
            calculatedMaterials.put("小型修复包", 0);
        }

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 5, 15));

        JLabel hintLabel1 = new JLabel("系统已算出这批任务理论所需物资总量。");
        hintLabel1.setFont(new Font("微软雅黑", Font.BOLD, 14));
        JLabel hintLabel2 = new JLabel("<html><font color='gray'>您可以在此手动修改(扣减)实际要携带的数量<br>🌟 游戏更新：可直接在下方单独设置【修复包】和【小型修复包】！</font></html>");
        hintLabel2.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        infoPanel.add(hintLabel1);
        infoPanel.add(hintLabel2);
        reviewDialog.add(infoPanel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridLayout(0, 2, 10, 15));
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 30, 15, 30));

        Map<String, JTextField> inputFieldsMap = new LinkedHashMap<>();

        List<String> matKeys = new ArrayList<>(calculatedMaterials.keySet());
        matKeys.sort((a, b) -> {
            boolean aIsItem = a.equals("修复包") || a.equals("小型修复包");
            boolean bIsItem = b.equals("修复包") || b.equals("小型修复包");
            if (aIsItem && !bIsItem) return -1;
            if (!aIsItem && bIsItem) return 1;
            if (aIsItem && bIsItem) return a.equals("修复包") ? -1 : 1;
            return a.compareTo(b);
        });

        for (String matName : matKeys) {
            Integer amount = calculatedMaterials.get(matName);

            JLabel label = new JLabel(matName + " :");
            label.setFont(new Font("微软雅黑", Font.BOLD, 15));
            if (matName.equals("修复包") || matName.equals("小型修复包")) {
                label.setForeground(new Color(220, 50, 50));
            }
            label.setHorizontalAlignment(SwingConstants.RIGHT);

            JTextField tf = new JTextField(String.valueOf(amount));
            tf.setFont(new Font("微软雅黑", Font.PLAIN, 16));
            tf.setHorizontalAlignment(JTextField.CENTER);

            inputFieldsMap.put(matName, tf);
            formPanel.add(label);
            formPanel.add(tf);
        }

        JScrollPane scrollPane = new JScrollPane(formPanel);
        reviewDialog.add(scrollPane, BorderLayout.CENTER);

        JPanel bottomActionPanel = new JPanel(new BorderLayout(5, 5));
        bottomActionPanel.setBorder(BorderFactory.createEmptyBorder(5, 15, 10, 15));

        JPanel speedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        speedPanel.add(new JLabel("发货速度: "));
        JComboBox<String> costCombo = new JComboBox<>(new String[]{"标准 (默认)", "中速", "高速"});
        costCombo.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        speedPanel.add(costCombo);

        JButton confirmBtn = new JButton("确认数量无误，全自动填单！");
        confirmBtn.setFont(new Font("微软雅黑", Font.BOLD, 16));
        confirmBtn.setPreferredSize(new Dimension(0, 45));
        confirmBtn.setBackground(new Color(220, 255, 220));

        confirmBtn.addActionListener(e -> {
            Map<String, Integer> finalMaterials = new LinkedHashMap<>();
            for (Map.Entry<String, JTextField> entry : inputFieldsMap.entrySet()) {
                try {
                    int val = Integer.parseInt(entry.getValue().getText().trim());
                    if (val > 0) finalMaterials.put(entry.getKey(), val);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(reviewDialog, "❌ 包含非数字字符，请检查修改内容！");
                    return;
                }
            }

            String speed = "标准";
            if (costCombo.getSelectedIndex() == 1) speed = "中速";
            if (costCombo.getSelectedIndex() == 2) speed = "高速";

            reviewDialog.dispose();
            parentDialog.dispose();
            logFrame.setVisible(true);

            // 🚀 核心接入 TaskManager
            TaskManager.startDispatch(finalMaterials, selectedIndices, speed);
        });

        bottomActionPanel.add(speedPanel, BorderLayout.NORTH);
        bottomActionPanel.add(confirmBtn, BorderLayout.CENTER);
        reviewDialog.add(bottomActionPanel, BorderLayout.SOUTH);

        reviewDialog.setLocationRelativeTo(parentDialog);
        reviewDialog.setVisible(true);
    }

    private void openClearInventoryDialog() {
        // 🚀 核心接入 TaskManager
        if (TaskManager.isBusy()) {
            JOptionPane.showMessageDialog(this, "⚠️ 当前已有任务正在运行，请先停止！");
            return;
        }

        JDialog dialog = new JDialog(this, "自动化清仓 (目标舰队与速度选择)", true);
        dialog.setSize(400, 500);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel fleetPanel = new JPanel();
        fleetPanel.setLayout(new BoxLayout(fleetPanel, BoxLayout.Y_AXIS));
        fleetPanel.setBorder(BorderFactory.createTitledBorder("指定清仓舰队"));

        List<JCheckBox> fleetCheckBoxes = new ArrayList<>();
        int index = 0;
        for (String fleetName : Config.FleetConfig.TASK_MAP.keySet()) {
            JCheckBox cb = new JCheckBox((index + 1) + ". " + fleetName);
            cb.setSelected(true);
            cb.setFont(new Font("微软雅黑", Font.PLAIN, 14));
            fleetCheckBoxes.add(cb);
            fleetPanel.add(cb);
            index++;
        }

        JButton selectAllBtn = new JButton("全选 / 全不选");
        selectAllBtn.addActionListener(e -> {
            boolean anyUnselected = fleetCheckBoxes.stream().anyMatch(cb -> !cb.isSelected());
            for (JCheckBox cb : fleetCheckBoxes) {
                cb.setSelected(anyUnselected);
            }
        });
        fleetPanel.add(Box.createVerticalStrut(10));
        fleetPanel.add(selectAllBtn);

        JScrollPane fleetScroll = new JScrollPane(fleetPanel);
        dialog.add(fleetScroll, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel speedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        speedPanel.add(new JLabel("发货速度: "));
        JComboBox<String> costCombo = new JComboBox<>(new String[]{"标准 (默认)", "中速", "高速"});
        costCombo.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        speedPanel.add(costCombo);

        JButton startBtn = new JButton("确认选好，开始清仓！");
        startBtn.setFont(new Font("微软雅黑", Font.BOLD, 16));
        startBtn.setBackground(new Color(255, 235, 205));
        startBtn.setPreferredSize(new Dimension(0, 50));

        startBtn.addActionListener(e -> {
            List<Integer> selectedIndices = new ArrayList<>();
            for (int i = 0; i < fleetCheckBoxes.size(); i++) {
                if (fleetCheckBoxes.get(i).isSelected()) {
                    selectedIndices.add(i);
                }
            }
            if (selectedIndices.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "❌ 请至少勾选一个要清仓的舰队！", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String speed = "标准";
            if (costCombo.getSelectedIndex() == 1) speed = "中速";
            if (costCombo.getSelectedIndex() == 2) speed = "高速";

            dialog.dispose();

            // 🚀 核心接入 TaskManager
            TaskManager.startClearInventory(speed, selectedIndices);
        });

        bottomPanel.add(speedPanel, BorderLayout.NORTH);
        bottomPanel.add(startBtn, BorderLayout.CENTER);
        dialog.add(bottomPanel, BorderLayout.SOUTH);

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    public static void showProfileLauncher() {
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
            fileChooser.setDialogTitle("选择沙盒快捷方式 (.lnk) 或游戏本体 (.exe)");
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home") + "/Desktop"));
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("快捷方式/程序 (*.lnk, *.exe)", "lnk", "exe"));

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
            String name = JOptionPane.showInputDialog(frame, "请输入新档案名称（如：沙盒小号）：");
            if (name != null && !name.trim().isEmpty()) {
                String cleanName = name.trim();
                boolean exists = false;
                for (int i = 0; i < profileBox.getItemCount(); i++) {
                    if (profileBox.getItemAt(i).equals(cleanName)) exists = true;
                }
                if (!exists) {
                    profileBox.addItem(cleanName);
                    Config.saveProfileSettings(cleanName, "WOE", "");
                }
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