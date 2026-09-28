package com.lulu.remote;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lulu.config.Config;
import com.lulu.logic.TaskManager;
import com.sun.jna.platform.win32.User32;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpServerManager {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static HttpServer server;
    private static Thread heartbeatThread;

    public static synchronized void startServer() {
        if (!Config.Remote.enableCloud) return;
        if (server != null) return;

        try {
            server = HttpServer.create(new InetSocketAddress(0), 0);
            int myPort = server.getAddress().getPort();

            server.createContext("/api/status", exchange -> {
                String response = "WOE Bot " + TaskManager.currentStatus;
                sendResponse(exchange, 200, response);
            });

            server.createContext("/api/command", exchange -> {
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = parseQuery(query);
                String action = params.getOrDefault("action", "");
                String response;
                int statusCode = 200;
                boolean isStopCommand = action.equals("stop");

                if (!TaskManager.isIdle() && !isStopCommand) {
                    statusCode = 409;
                    response = "任务拒绝：当前正在 " + TaskManager.currentStatus;
                } else if (User32.INSTANCE.FindWindow(null, Config.Global.APP_TITLE) == null) {
                    statusCode = 400;
                    response = "错误：未检测到游戏窗口 [" + Config.Global.APP_TITLE + "]";
                } else if (action.equals("start_cruise")) {
                    response = "已接收到远端巡航指令，准备启动...";
                    TaskManager.startCruise(true);
                } else if (action.equals("clear")) {
                    String speed = params.getOrDefault("speed", "极速");
                    List<Integer> fleets = parseFleets(params.getOrDefault("fleets", "0"));
                    response = "已接收到清包指令";
                    TaskManager.startClearInventory(speed, fleets);
                } else if (action.equals("dispatch")) {
                    String speed = params.getOrDefault("speed", "极速");
                    List<Integer> fleets = parseFleets(params.getOrDefault("fleets", "0"));
                    Map<String, Integer> materials = parseMaterials(params.getOrDefault("materials", ""));
                    if (materials.isEmpty()) {
                        statusCode = 400;
                        response = "错误：派发材料为空";
                    } else {
                        response = "已接收到远端派发指令";
                        TaskManager.startDispatch(materials, fleets, speed);
                    }
                } else if (isStopCommand) {
                    response = "紧急停止指令已执行";
                    TaskManager.stopAllTasks(false);
                } else {
                    statusCode = 400;
                    response = "未知指令: " + action;
                }
                sendResponse(exchange, statusCode, response);
            });

            server.createContext("/api/config", exchange -> {
                try {
                    if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                        Map<String, Object> allConfigs = new HashMap<>();

                        Map<String, Object> cruiseData = new HashMap<>();
                        cruiseData.put("targetRounds", Config.CruiseConfig.targetRounds);
                        cruiseData.put("intervalMinutes", Config.CruiseConfig.intervalMinutes);
                        cruiseData.put("repairThreshold", Config.CruiseConfig.repairThreshold);
                        cruiseData.put("autoRestartGame", Config.CruiseConfig.autoRestartGame);
                        cruiseData.put("restartRounds", Config.CruiseConfig.restartRounds);
                        cruiseData.put("enableLowEndMode", Config.CruiseConfig.enableLowEndMode);
                        cruiseData.put("enableAutoRepair", Config.CruiseConfig.enableAutoRepair);
                        cruiseData.put("repairFilterIndex", Config.CruiseConfig.repairFilterIndex);
                        cruiseData.put("enableSecondVerify", Config.CruiseConfig.enableSecondVerify);
                        cruiseData.put("interactRadius", Config.CruiseConfig.interactRadius);
                        cruiseData.put("offsetX", Config.CruiseConfig.offsetX);
                        cruiseData.put("offsetY", Config.CruiseConfig.offsetY);
                        cruiseData.put("scrollSteps", Config.CruiseConfig.scrollSteps);
                        allConfigs.put("cruise", cruiseData);

                        Map<String, Object> statsData = new HashMap<>();
                        statsData.put("totalFound", Config.GlobalStats.totalFound);
                        statsData.put("totalSuccess", Config.GlobalStats.totalSuccess);
                        statsData.put("totalPlanB", Config.GlobalStats.totalPlanB);
                        statsData.put("totalIgnore", Config.GlobalStats.totalIgnore);
                        statsData.put("totalFail", Config.GlobalStats.totalFail);
                        // 🚀 核心新增：把详细的星级与事件统计字典传给网页
                        statsData.put("starCountMap", Config.GlobalStats.starCountMap);
                        statsData.put("eventCountMap", Config.GlobalStats.eventCountMap);
                        allConfigs.put("stats", statsData);

                        allConfigs.put("dispatchTemplates", Config.DispatchConfig.TEMPLATES);
                        allConfigs.put("fleetNames", Config.FleetConfig.TASK_MAP.keySet());
                        allConfigs.put("bomTable", com.lulu.ui.MainGUI.BOM_TABLE);

                        String jsonResponse = mapper.writeValueAsString(allConfigs);
                        sendResponse(exchange, 200, jsonResponse);

                    } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                        byte[] bytes = exchange.getRequestBody().readAllBytes();
                        String jsonString = new String(bytes, StandardCharsets.UTF_8);
                        JsonNode rootNode = mapper.readTree(jsonString);

                        if (rootNode.has("cruise")) {
                            JsonNode c = rootNode.get("cruise");
                            if(c.has("targetRounds")) Config.CruiseConfig.targetRounds = c.get("targetRounds").asInt();
                            if(c.has("intervalMinutes")) Config.CruiseConfig.intervalMinutes = c.get("intervalMinutes").asInt();
                            if(c.has("repairThreshold")) Config.CruiseConfig.repairThreshold = c.get("repairThreshold").asInt();
                            if(c.has("autoRestartGame")) Config.CruiseConfig.autoRestartGame = c.get("autoRestartGame").asInt();
                            if(c.has("restartRounds")) Config.CruiseConfig.restartRounds = c.get("restartRounds").asInt();
                            if(c.has("enableLowEndMode")) Config.CruiseConfig.enableLowEndMode = c.get("enableLowEndMode").asBoolean();
                            if(c.has("enableAutoRepair")) Config.CruiseConfig.enableAutoRepair = c.get("enableAutoRepair").asBoolean();
                            if(c.has("repairFilterIndex")) Config.CruiseConfig.repairFilterIndex = c.get("repairFilterIndex").asInt();
                            if(c.has("enableSecondVerify")) Config.CruiseConfig.enableSecondVerify = c.get("enableSecondVerify").asBoolean();
                            if(c.has("interactRadius")) Config.CruiseConfig.interactRadius = c.get("interactRadius").asInt();
                            if(c.has("offsetX")) Config.CruiseConfig.offsetX = c.get("offsetX").asInt();
                            if(c.has("offsetY")) Config.CruiseConfig.offsetY = c.get("offsetY").asInt();
                            if(c.has("scrollSteps")) Config.CruiseConfig.scrollSteps = c.get("scrollSteps").asInt();

                            Config.saveCruiseConfig();

                            if (com.lulu.ui.MainGUI.instance != null) {
                                com.lulu.ui.MainGUI.instance.refreshConfigUI();
                            }
                        }

                        if (rootNode.has("dispatchTemplates")) {
                            Map<String, Map<String, Integer>> newTemplates = mapper.convertValue(
                                    rootNode.get("dispatchTemplates"),
                                    new TypeReference<Map<String, Map<String, Integer>>>() {}
                            );
                            Config.DispatchConfig.TEMPLATES.clear();
                            Config.DispatchConfig.TEMPLATES.putAll(newTemplates);
                            Config.saveDispatchTemplates();
                        }

                        sendResponse(exchange, 200, "节点配置热更新成功并已写入本地");
                    }
                } catch (Exception e) {
                    sendResponse(exchange, 500, "配置同步异常: " + e.getMessage());
                }
            });

            server.setExecutor(null);
            server.start();
            System.out.println("-> [云端通信] HTTP挂机节点已启动，分配端口: " + myPort);
            startHeartbeatThread(myPort);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized void stopServer() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (heartbeatThread != null) {
            heartbeatThread.interrupt();
            heartbeatThread = null;
        }
        System.out.println("-> [云端通信] 集群远控已关闭，底层端口与心跳均已完全释放");
    }

    private static void startHeartbeatThread(int myPort) {
        heartbeatThread = new Thread(() -> {
            HttpClient client = HttpClient.newHttpClient();
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (Config.Remote.enableCloud && Config.Remote.commanderIp != null && !Config.Remote.commanderIp.isEmpty()) {

                        // 🚀 核心修改：动态拼接大状态与细分动作，发送给 Web 端
                        String displayStatus = TaskManager.currentStatus;
                        if (!TaskManager.isIdle() && TaskManager.latestAction != null && !TaskManager.latestAction.isEmpty()) {
                            displayStatus += " | " + TaskManager.latestAction;
                        }

                        // 清洗文本中的双引号和换行符，防止 JSON 崩塌
                        String safeStatus = displayStatus.replace("\"", "'").replace("\n", " ");

                        String json = String.format("{\"accountId\":\"%s\", \"ip\":\"\", \"port\":%d, \"status\":\"%s\"}",
                                Config.PROFILE_NAME, myPort, safeStatus);

                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create("http://" + Config.Remote.commanderIp + ":8080/api/heartbeat"))
                                // 优化：增加 3 秒强制超时，防止网络黑洞导致心跳线程永久阻塞
                                .timeout(java.time.Duration.ofSeconds(3))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(json))
                                .build();
                        client.send(request, HttpResponse.BodyHandlers.discarding());
                    }
                } catch (Exception ignored) {}
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        heartbeatThread.start();
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length > 1) {
                map.put(pair[0], URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
            }
        }
        return map;
    }

    private static List<Integer> parseFleets(String fleetsStr) {
        List<Integer> fleets = new ArrayList<>();
        for (String f : fleetsStr.split(",")) {
            try { fleets.add(Integer.parseInt(f.trim())); } catch (NumberFormatException ignored) {}
        }
        if (fleets.isEmpty()) fleets.add(0);
        return fleets;
    }

    private static Map<String, Integer> parseMaterials(String matsStr) {
        Map<String, Integer> materials = new HashMap<>();
        if (matsStr == null || matsStr.isEmpty()) return materials;
        for (String matPair : matsStr.split(",")) {
            String[] kv = matPair.split(":");
            if (kv.length == 2) {
                try { materials.put(kv[0].trim(), Integer.parseInt(kv[1].trim())); } catch (NumberFormatException ignored) {}
            }
        }
        return materials;
    }

    private static void sendResponse(com.sun.net.httpserver.HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        byte[] responseBytes = response.getBytes("UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }
}