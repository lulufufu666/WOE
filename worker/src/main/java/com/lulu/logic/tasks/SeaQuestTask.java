package com.lulu.logic.tasks;

import com.lulu.tool.LogManager;
import com.lulu.config.Config;
import com.lulu.core.AutomationEngine;
import com.lulu.core.VisionBot;
import com.lulu.logic.BotTask;
import com.lulu.logic.TaskReport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SeaQuestTask implements BotTask {

    private final String starPrefix;
    private final Map<String, String> eventActionMap;
    private final TaskReport report;

    public SeaQuestTask(String starPrefix, Map<String, String> eventActionMap, TaskReport report) {
        this.starPrefix = starPrefix;
        this.eventActionMap = eventActionMap;
        this.report = report;
    }

    @Override
    public void execute() throws InterruptedException {
        String targetQuest = starPrefix + ".png";
        String specificStarButton = starPrefix + "事件激活.png";
        String genericYellowButton = "事件可交互.png";
        String[] allSkulls = {"一星.png", "二星.png", "三星.png", "四星.png", "五星.png"};

        Config.Global.MATCH_THRESHOLD = 0.65;

        int centerX = 1244 + Config.CruiseConfig.offsetX;
        int centerY = 758 + Config.CruiseConfig.offsetY;
        int radius = Config.CruiseConfig.interactRadius;

        int roiX = Math.max(0, centerX - radius);
        int roiY = Math.max(0, centerY - radius);
        int roiW = radius * 2;
        int roiH = radius * 2;

        LogManager.print("🧭 [海域扫描] 正在指定区域扫描目标星域: [" + starPrefix + "]");
        AutomationEngine.drawDebugROI(roiX, roiY, roiW, roiH, 3000);

        List<int[]> possibleTargets = AutomationEngine.findAll(targetQuest, roiX, roiY, roiW, roiH);

        if (possibleTargets.isEmpty()) {
            LogManager.print("❌ [扫描结果] 当前海域空空如也，未发现任何 [" + starPrefix + "] 目标。");
            return;
        }

        AutomationEngine.drawDebugOval(centerX - radius, centerY - radius, radius * 2, radius * 2, 3000);

        List<int[]> targetsInCircle = new ArrayList<>();
        for (int[] pt : possibleTargets) {
            double distance = Math.sqrt(Math.pow(pt[0] - centerX, 2) + Math.pow(pt[1] - centerY, 2));
            if (distance <= radius) {
                targetsInCircle.add(pt);
            }
        }

        possibleTargets = targetsInCircle;
        if (possibleTargets.isEmpty()) {
            LogManager.print("❌ [距离过滤] 发现了目标，但全都在舰队的圆形交互边界（半径: " + radius + "）之外！");
            return;
        }

        List<int[]> validTargets = new ArrayList<>();
        int boxSize = 100;
        int offset = boxSize / 2;

        for (int[] pt : possibleTargets) {
            int smallRoiX = pt[0] - offset;
            int smallRoiY = pt[1] - offset;
            double maxScore = 0.0;
            String realIdentity = "未知";

            for (String skullImg : allSkulls) {
                double score = AutomationEngine.getMatchScore(skullImg, smallRoiX, smallRoiY, boxSize, boxSize);
                if (score > maxScore) { maxScore = score; realIdentity = skullImg; }
            }
            if (realIdentity.equals(targetQuest)) {
                validTargets.add(pt);
            }
        }

        if (validTargets.isEmpty()) {
            LogManager.print("⚠️ [图像甄别] 范围内的目标星级比对未通过，没有符合要求的真实 [" + starPrefix + "] 目标。");
            return;
        }

        LogManager.print("🎯 [锁定目标] 成功过滤并锁定 " + validTargets.size() + " 个合法的真实 [" + starPrefix + "] 目标点！");
        for (int[] target : validTargets) {
            AutomationEngine.drawDebugOval(target[0] - offset, target[1] - offset, boxSize, boxSize, 1000);
        }

        report.totalFound += validTargets.size();

        for (int i = 0; i < validTargets.size(); i++) {
            int[] target = validTargets.get(i);
            int targetX = target[0], targetY = target[1];

            LogManager.print("👉 [执行交互] 正在前往第 (" + (i + 1) + "/" + validTargets.size() + ") 个目标点: [" + targetX + ", " + targetY + "]");
            AutomationEngine.click(targetX, targetY);

            Thread.sleep(500);

            int panelRoiX = 693, panelRoiY = 370, panelRoiW = 1086, panelRoiH = 798;
            int btnRoiX = Math.max(0, targetX - 450), btnRoiY = Math.max(0, targetY - 450);
            int btnRoiW = 900, btnRoiH = 900;

            int[] yellowBtnPos = AutomationEngine.pollForTarget(
                    genericYellowButton, btnRoiX, btnRoiY, btnRoiW, btnRoiH, 0.75, 3000, 300
            );

            double starScore = 1.0;
            if (Config.CruiseConfig.enableSecondVerify) {
                starScore = AutomationEngine.getMatchScore(specificStarButton, btnRoiX, btnRoiY, btnRoiW, btnRoiH);
            }

            if (yellowBtnPos != null && starScore >= 0.75) {
                if (Config.CruiseConfig.enableSecondVerify) {
                    LogManager.print("     ✨ 成功捕捉到交互按钮且星级二次校验通过，安全放行，准备点击...");
                } else {
                    LogManager.print("     ✨ 成功捕捉到交互按钮(已跳过星级二次校验)，直接放行，准备点击...");
                }

                AutomationEngine.click(yellowBtnPos);

                AutomationEngine.move(100, 100);

                if (Config.CruiseConfig.enableLowEndMode) {
                    LogManager.print("     ⏳ [低配模式] 暂停截图 1.5 秒，彻底让出 CPU 算力等待画面渲染完毕...");
                    Thread.sleep(1500);
                } else {
                    Thread.sleep(800);
                }

                String detectedEvent = "未知事件";
                double maxEventScore = 0.0;
                long eventStartTime = System.currentTimeMillis();

                while (System.currentTimeMillis() - eventStartTime < 4000) {
                    maxEventScore = 0.0;
                    for (String eventImg : eventActionMap.keySet()) {
                        double score = AutomationEngine.getMatchScore(eventImg, panelRoiX, panelRoiY, panelRoiW, panelRoiH);
                        if (score > maxEventScore) {
                            maxEventScore = score;
                            detectedEvent = eventImg;
                        }
                    }
                    if (maxEventScore >= 0.75) {
                        break;
                    }
                    Thread.sleep(400);
                }

                if (maxEventScore >= 0.75 && eventActionMap.containsKey(detectedEvent)) {
                    String eventCleanName = detectedEvent.replace(".png", "");
                    LogManager.print("✨ [事件识别成功] 精准锁定目标事件: 【" + eventCleanName + "】 (匹配度: " + String.format("%.2f", maxEventScore) + ")");

                    report.recordFoundEvent(detectedEvent);

                    String targetActionBtn = eventActionMap.get(detectedEvent);
                    int[] actionBtnPos = VisionBot.getTargetPositionInRegion(Config.Global.APP_TITLE, targetActionBtn, panelRoiX, panelRoiY, panelRoiW, panelRoiH);

                    String fallbackActionBtn = getFallbackAction(targetActionBtn);
                    int[] actionBtnPosFinal = actionBtnPos;
                    String finalActionToUse = targetActionBtn;

                    if (actionBtnPosFinal == null && fallbackActionBtn != null) {
                        LogManager.print("     ⚠️ [物资预警] 首选策略按钮 [" + targetActionBtn.replace(".png", "") + "] 不可用，触发降级，尝试备选按钮: [" + fallbackActionBtn.replace(".png", "") + "]");
                        actionBtnPosFinal = VisionBot.getTargetPositionInRegion(Config.Global.APP_TITLE, fallbackActionBtn, panelRoiX, panelRoiY, panelRoiW, panelRoiH);
                        if (actionBtnPosFinal != null) {
                            finalActionToUse = fallbackActionBtn;
                        }
                    }

                    if (actionBtnPosFinal != null) {
                        LogManager.print("     🚀 [执行决策] 点击行动按钮: [" + finalActionToUse.replace(".png", "") + "]");
                        AutomationEngine.click(actionBtnPosFinal);
                        boolean isSuccess = false;
                        boolean hasClosed = false;

                        for (int retry = 1; retry <= 5; retry++) {
                            Thread.sleep(1000);

                            if (AutomationEngine.getMatchScore("绕过失败.png", panelRoiX, panelRoiY, panelRoiW, panelRoiH) >= 0.75) {
                                LogManager.print("     ❌ [结果判定] (第 " + retry + " 秒) 遭遇拦截：检测到【绕过失败.png】");
                                report.recordFail(detectedEvent);
                                clickCloseButtonOrFallback(panelRoiX, panelRoiY, panelRoiW, panelRoiH, targetX, targetY);
                                hasClosed = true;
                                break;
                            }
                            if (AutomationEngine.getMatchScore("忽略提示.png", panelRoiX, panelRoiY, panelRoiW, panelRoiH) >= 0.75) {
                                LogManager.print("     ⚠️ [结果判定] (第 " + retry + " 秒) 任务跳过：检测到【忽略提示.png】");
                                report.recordIgnore(detectedEvent);
                                clickCloseButtonOrFallback(panelRoiX, panelRoiY, panelRoiW, panelRoiH, targetX, targetY);
                                hasClosed = true;
                                break;
                            }

                            if (AutomationEngine.getMatchScore("付费成功提示.png", panelRoiX, panelRoiY, panelRoiW, panelRoiH) >= 0.75) {
                                LogManager.print("     🛡️ [付费避险] (第 " + retry + " 秒) 成功完成付费避险，正在关闭提示弹窗...");
                                report.recordPlanB(detectedEvent);
                                clickCloseButtonOrFallback(panelRoiX, panelRoiY, panelRoiW, panelRoiH, targetX, targetY);
                                hasClosed = true;
                                break;
                            }

                            // ==========================================
                            // 👉 核心修正：局部坐标计算和画框移到这里！
                            // ==========================================
                            int lootRoiW = 700;
                            int lootRoiH = 700;
                            int lootRoiX = Math.max(0, targetX - 350);
                            int lootRoiY = Math.max(0, targetY - 350);

                            // 每次找存货前画 1 秒钟的框，刚好和循环频率完美同步
                            if(retry==1){
                                AutomationEngine.drawDebugROI(lootRoiX, lootRoiY, lootRoiW, lootRoiH, 1000);
                            }
                            if (AutomationEngine.exists("存货.png", lootRoiX, lootRoiY, lootRoiW, lootRoiH)) {
                                LogManager.print("     🎉 [大捷！] (第 " + retry + " 秒) 成功捞到奖励！检测到【存货.png】，正在局部收割战利品...");
                                report.recordSuccess(detectedEvent);
                                AutomationEngine.click("存货.png", lootRoiX, lootRoiY, lootRoiW, lootRoiH);
                                isSuccess = true;
                                break;
                            }
                        }

                        if (!hasClosed && !isSuccess) {
                            LogManager.print("     ⏰ [超时兜底] 轮询 10 秒未检测到明确结果，执行超时关闭兜底。");
                            report.recordFail(detectedEvent);
                            clickCloseButtonOrFallback(panelRoiX, panelRoiY, panelRoiW, panelRoiH, targetX, targetY);
                        }
                    } else {
                        LogManager.print("     ❌ [操作失败] 未能找到任何可用的执行按钮，放弃该事件。");
                        report.recordFail(detectedEvent);
                        clickCloseButtonOrFallback(panelRoiX, panelRoiY, panelRoiW, panelRoiH, targetX, targetY);
                    }
                } else {
                    String bestMatchName = detectedEvent.replace(".png", "");
                    LogManager.print("     ⚠️ [未知事件] 弹窗已开，但未能在面板中匹配到任何策略（最相似: [" + bestMatchName + "], 匹配度: " + String.format("%.2f", maxEventScore) + "），关闭弹窗。");
                    clickCloseButtonOrFallback(panelRoiX, panelRoiY, panelRoiW, panelRoiH, targetX, targetY);
                }

                LogManager.print("     ⏳ 目标处理完毕，缓冲停顿，等待游戏动画结束...");
                Thread.sleep(1500);

            } else if (yellowBtnPos != null) {
                LogManager.print("     ⚠️ 弹出黄色按钮，但旁边不是【" + starPrefix + "】(二次校验匹配度: " + String.format("%.2f", starScore) + ")，防进错面板机制生效，拒绝点击！");
                AutomationEngine.click(targetX, targetY);
                Thread.sleep(1000);
            } else {
                LogManager.print("     ℹ️ 点击目标后未能稳定唤起交互弹窗，跳过。");
                AutomationEngine.click(targetX, targetY);
                Thread.sleep(1000);
            }
        }
    }

    private void clickCloseButtonOrFallback(int roiX, int roiY, int roiW, int roiH, int fallbackX, int fallbackY) throws InterruptedException {
        int[] closeBtnPos = VisionBot.getTargetPositionInRegion(Config.Global.APP_TITLE, "关闭弹窗.png", roiX, roiY, roiW, roiH);
        if (closeBtnPos != null && AutomationEngine.getMatchScore("关闭弹窗.png", roiX, roiY, roiW, roiH) >= 0.75) {
            AutomationEngine.click(closeBtnPos);
        } else {
            AutomationEngine.click(fallbackX, fallbackY);
        }
        Thread.sleep(1000);
    }

    private String getFallbackAction(String primaryAction) {
        if (primaryAction == null) return null;
        switch (primaryAction) {
            case "冒险.png": return "忽略.png";
            case "绕过.png": return "等待.png";
            case "战斗.png": return "付费.png";
            default: return null;
        }
    }
}