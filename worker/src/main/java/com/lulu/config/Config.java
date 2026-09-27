package com.lulu.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.*;

public class Config {
    private Config() {}

    public static String PROFILE_NAME = "default";

    public static String getProfileDir() {
        String dir = "profiles/" + PROFILE_NAME + "/";
        File f = new File(dir);
        if (!f.exists()) f.mkdirs();
        return dir;
    }

    public static class Global {
        public static String APP_TITLE = "WOE";
        public static String LAUNCH_CMD = "";
        public static final String APP_VERSION = "v0.0.1";
        public static double MATCH_THRESHOLD = 0.85;
        public static double GAME_UI_SCALE = 1.0;
        public static boolean DEBUG_MODE = true;
    }

    // 🚀 新增：云端中枢通信专属配置类
    public static class Remote {
        public static boolean enableCloud = false;
        public static String commanderIp = "127.0.0.1";
    }

    public static class GlobalStats {
        public static int totalFound = 0;
        public static int totalSuccess = 0;
        public static int totalFail = 0;
        public static int totalPlanB = 0;
        public static int totalIgnore = 0;
        public static Map<String, Integer> starCountMap = new LinkedHashMap<>();
        public static Map<String, Integer> eventCountMap = new LinkedHashMap<>();
    }

    public static class CruiseConfig {
        public static int targetRounds = 1;
        public static int intervalMinutes = 15;
        public static int repairThreshold = 0;
        public static int repairFilterIndex = 1;
        public static int autoRestartGame = 1;
        public static int restartRounds = 10;
        public static boolean enableSecondVerify = true;
        public static boolean enableLowEndMode = false;
        public static boolean enableAutoRepair = true;
        public static int interactRadius = 600;
        public static int offsetX = -15;
        public static int offsetY = -15;
        public static int scrollSteps = 10;
    }

    public static class Strategy {
        public static Map<String, Map<String, String>> EVENT_MAP = new HashMap<>();
        static {
            Map<String, String> oneStarDefault = new HashMap<>();
            oneStarDefault.put("一星士兵事件.png", "正常打法.png");
            oneStarDefault.put("一星炼金师事件.png", "正常打法.png");
            oneStarDefault.put("一星附魔剑事件.png", "正常打法.png");
            EVENT_MAP.put("一星", oneStarDefault);

            Map<String, String> twoStarDefault = new HashMap<>();
            twoStarDefault.put("二星珍珠事件.png", "正常打法.png");
            twoStarDefault.put("二星金钥匙事件.png", "正常打法.png");
            twoStarDefault.put("二星项链事件.png", "正常打法.png");
            EVENT_MAP.put("二星", twoStarDefault);

            Map<String,String> threeStarDefault= new HashMap<>();
            threeStarDefault.put("三星核心事件.png","正常打法.png");
            threeStarDefault.put("三星熔岩事件.png","正常打法.png");
            threeStarDefault.put("三星神圣之剑事件.png","正常打法.png");
            threeStarDefault.put("三星阿比事件.png","正常打法.png");
            EVENT_MAP.put("三星", threeStarDefault);
        }
    }

    public static class FleetConfig {
        public static Map<String, List<String>> TASK_MAP = new LinkedHashMap<>();
        static {
            TASK_MAP.put("A-骷髅-1", List.of("一星", "二星", "三星", "四星", "五星"));
            TASK_MAP.put("A-骷髅-2", List.of("一星", "二星", "三星", "四星", "五星"));
            TASK_MAP.put("A-骷髅-3", List.of("一星", "二星", "三星", "四星", "五星"));
            TASK_MAP.put("A-骷髅-4", List.of("一星", "二星", "三星", "四星", "五星"));
            TASK_MAP.put("A-骷髅-5", List.of("一星", "二星", "三星", "四星", "五星"));
            TASK_MAP.put("A-骷髅-6", List.of("一星", "二星", "三星", "四星", "五星"));
            TASK_MAP.put("A-骷髅-7", List.of("一星", "二星", "三星", "四星", "五星"));
            TASK_MAP.put("A-骷髅-8", List.of("一星", "二星", "三星", "四星", "五星"));
        }
    }

    public static class InventoryConfig {
        public static Map<String, Integer> EVENT_QUOTA = new HashMap<>();
        static {
            EVENT_QUOTA.put("修复包", 0);
            EVENT_QUOTA.put("小型修复包", 0);
            EVENT_QUOTA.put("一星士兵事件.png", 0);
            EVENT_QUOTA.put("一星炼金师事件.png", 0);
            EVENT_QUOTA.put("一星附魔剑事件.png", 0);
            EVENT_QUOTA.put("二星珍珠事件.png", 0);
            EVENT_QUOTA.put("二星金钥匙事件.png", 0);
            EVENT_QUOTA.put("二星项链事件.png", 0);
            EVENT_QUOTA.put("三星核心事件.png", 0);
            EVENT_QUOTA.put("三星熔岩事件.png", 0);
            EVENT_QUOTA.put("三星神圣之剑事件.png", 0);
            EVENT_QUOTA.put("三星阿比事件.png", 0);
        }
    }

    public static class DispatchConfig {
        public static Map<String, Map<String, Integer>> TEMPLATES = new LinkedHashMap<>();
        static {
            Map<String, Integer> defaultTpl = new HashMap<>();
            defaultTpl.put("一星士兵事件.png", 5);
            defaultTpl.put("二星珍珠事件.png", 5);
            TEMPLATES.put("默认模板(测试)", defaultTpl);
        }
    }

    private static final String STRATEGY_FILE = "strategy_config.json";
    private static final String FLEET_FILE = "fleet_config.json";
    private static final String INVENTORY_FILE = "inventory_config.json";
    private static final String DISPATCH_TPL_FILE = "dispatch_templates.json";
    private static final String CRUISE_FILE = "cruise_config.json";
    private static final String STATS_FILE = "global_stats.json";
    private static final String REMOTE_FILE = "remote_config.json";

    private static final ObjectMapper mapper = new ObjectMapper();

    public static void loadProfileSettings(String profileName) {
        File f = new File("profiles/" + profileName + "/profile_settings.json");
        if (f.exists()) {
            try {
                Map<String, String> map = mapper.readValue(f, new TypeReference<Map<String, String>>(){});
                Global.APP_TITLE = map.getOrDefault("windowTitle", "WOE");
                Global.LAUNCH_CMD = map.getOrDefault("launchCmd", "");
            } catch(Exception ignored) {}
        } else {
            Global.APP_TITLE = "WOE";
            Global.LAUNCH_CMD = "";
        }
    }

    public static void saveProfileSettings(String profileName, String title, String cmd) {
        File dir = new File("profiles/" + profileName);
        if (!dir.exists()) dir.mkdirs();
        File f = new File(dir, "profile_settings.json");
        try {
            Map<String, String> map = new HashMap<>();
            map.put("windowTitle", title);
            map.put("launchCmd", cmd);
            mapper.writerWithDefaultPrettyPrinter().writeValue(f, map);
        } catch(Exception ignored) {}
    }

    public static void load() {
        try {
            String baseDir = getProfileDir();

            // 🚀 加载云端远控配置
            File remoteFile = new File(baseDir + REMOTE_FILE);
            if (remoteFile.exists()) {
                Map<String, Object> rData = mapper.readValue(remoteFile, new TypeReference<Map<String, Object>>(){});
                Remote.enableCloud = (Boolean) rData.getOrDefault("enableCloud", false);
                Remote.commanderIp = (String) rData.getOrDefault("commanderIp", "127.0.0.1");
            } else {
                saveRemoteConfig();
            }

            File file = new File(baseDir + STRATEGY_FILE);
            if (file.exists()) { Strategy.EVENT_MAP = mapper.readValue(file, new TypeReference<Map<String, Map<String, String>>>(){}); } else { save(); }

            File fleetFile = new File(baseDir + FLEET_FILE);
            if (fleetFile.exists()) { FleetConfig.TASK_MAP = mapper.readValue(fleetFile, new TypeReference<Map<String, List<String>>>(){}); } else { saveFleetConfig(); }

            File invFile = new File(baseDir + INVENTORY_FILE);
            if (invFile.exists()) { InventoryConfig.EVENT_QUOTA = mapper.readValue(invFile, new TypeReference<Map<String, Integer>>(){}); } else { saveInventory(); }

            File tplFile = new File(baseDir + DISPATCH_TPL_FILE);
            if (tplFile.exists()) {
                DispatchConfig.TEMPLATES = mapper.readValue(tplFile, new TypeReference<Map<String, Map<String, Integer>>>(){});
            } else {
                saveDispatchTemplates();
            }

            File cruiseFile = new File(baseDir + CRUISE_FILE);
            if (cruiseFile.exists()) {
                Map<String, Integer> cruiseData = mapper.readValue(cruiseFile, new TypeReference<Map<String, Integer>>(){});
                CruiseConfig.targetRounds = cruiseData.getOrDefault("targetRounds", 1);
                CruiseConfig.intervalMinutes = cruiseData.getOrDefault("intervalMinutes", 15);
                CruiseConfig.repairThreshold = cruiseData.getOrDefault("repairThreshold", 0);
                CruiseConfig.repairFilterIndex = cruiseData.getOrDefault("repairFilterIndex", 1);
                CruiseConfig.autoRestartGame = cruiseData.getOrDefault("autoRestartGame", 1);
                CruiseConfig.restartRounds = cruiseData.getOrDefault("restartRounds", 10);
                CruiseConfig.enableSecondVerify = cruiseData.getOrDefault("enableSecondVerify", 1) == 1;
                CruiseConfig.enableLowEndMode = cruiseData.getOrDefault("enableLowEndMode", 0) == 1;
                CruiseConfig.enableAutoRepair = cruiseData.getOrDefault("enableAutoRepair", 1) == 1;
                CruiseConfig.interactRadius = cruiseData.getOrDefault("interactRadius", 600);
                CruiseConfig.offsetX = cruiseData.getOrDefault("offsetX", -20);
                CruiseConfig.offsetY = cruiseData.getOrDefault("offsetY", -18);
                CruiseConfig.scrollSteps = cruiseData.getOrDefault("scrollSteps", 10);
            } else {
                saveCruiseConfig();
            }

            File statsFile = new File(baseDir + STATS_FILE);
            if (statsFile.exists()) {
                Map<String, Object> statsData = mapper.readValue(statsFile, new TypeReference<Map<String, Object>>(){});
                GlobalStats.totalFound = ((Number) statsData.getOrDefault("totalFound", 0)).intValue();
                GlobalStats.totalSuccess = ((Number) statsData.getOrDefault("totalSuccess", 0)).intValue();
                GlobalStats.totalFail = ((Number) statsData.getOrDefault("totalFail", 0)).intValue();
                GlobalStats.totalPlanB = ((Number) statsData.getOrDefault("totalPlanB", 0)).intValue();
                GlobalStats.totalIgnore = ((Number) statsData.getOrDefault("totalIgnore", 0)).intValue();
                Object mapObj = statsData.get("starCountMap");
                if (mapObj instanceof Map) {
                    GlobalStats.starCountMap.clear();
                    ((Map<?, ?>) mapObj).forEach((k, v) -> GlobalStats.starCountMap.put(String.valueOf(k), ((Number) v).intValue()));
                }
                Object eventMapObj = statsData.get("eventCountMap");
                if (eventMapObj instanceof Map) {
                    GlobalStats.eventCountMap.clear();
                    ((Map<?, ?>) eventMapObj).forEach((k, v) -> GlobalStats.eventCountMap.put(String.valueOf(k), ((Number) v).intValue()));
                }
            } else {
                saveStatsConfig();
            }
        } catch (Exception e) { System.err.println("配置加载异常: " + e.getMessage()); }
    }

    public static void saveRemoteConfig() {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("enableCloud", Remote.enableCloud);
            data.put("commanderIp", Remote.commanderIp);
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(getProfileDir() + REMOTE_FILE), data);
        } catch (Exception ignored) {}
    }

    public static void save() {
        try { mapper.writerWithDefaultPrettyPrinter().writeValue(new File(getProfileDir() + STRATEGY_FILE), Strategy.EVENT_MAP); } catch (Exception ignored) {}
    }
    public static void saveFleetConfig() {
        try { mapper.writerWithDefaultPrettyPrinter().writeValue(new File(getProfileDir() + FLEET_FILE), FleetConfig.TASK_MAP); } catch (Exception ignored) {}
    }
    public static void saveInventory() {
        try { mapper.writerWithDefaultPrettyPrinter().writeValue(new File(getProfileDir() + INVENTORY_FILE), InventoryConfig.EVENT_QUOTA); } catch (Exception ignored) {}
    }
    public static void saveDispatchTemplates() {
        try { mapper.writerWithDefaultPrettyPrinter().writeValue(new File(getProfileDir() + DISPATCH_TPL_FILE), DispatchConfig.TEMPLATES); } catch (Exception ignored) {}
    }
    public static void saveCruiseConfig() {
        try {
            Map<String, Integer> data = new HashMap<>();
            data.put("targetRounds", CruiseConfig.targetRounds);
            data.put("intervalMinutes", CruiseConfig.intervalMinutes);
            data.put("repairThreshold", CruiseConfig.repairThreshold);
            data.put("repairFilterIndex", CruiseConfig.repairFilterIndex);
            data.put("autoRestartGame", CruiseConfig.autoRestartGame);
            data.put("restartRounds", CruiseConfig.restartRounds);
            data.put("enableSecondVerify", CruiseConfig.enableSecondVerify ? 1 : 0);
            data.put("enableLowEndMode", CruiseConfig.enableLowEndMode ? 1 : 0);
            data.put("enableAutoRepair", CruiseConfig.enableAutoRepair ? 1 : 0);
            data.put("interactRadius", CruiseConfig.interactRadius);
            data.put("offsetX", CruiseConfig.offsetX);
            data.put("offsetY", CruiseConfig.offsetY);
            data.put("scrollSteps", CruiseConfig.scrollSteps);
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(getProfileDir() + CRUISE_FILE), data);
        } catch (Exception ignored) {}
    }
    public static void saveStatsConfig() {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("totalFound", GlobalStats.totalFound);
            data.put("totalSuccess", GlobalStats.totalSuccess);
            data.put("totalFail", GlobalStats.totalFail);
            data.put("totalPlanB", GlobalStats.totalPlanB);
            data.put("totalIgnore", GlobalStats.totalIgnore);
            data.put("starCountMap", GlobalStats.starCountMap);
            data.put("eventCountMap", GlobalStats.eventCountMap);
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(getProfileDir() + STATS_FILE), data);
        } catch (Exception ignored) {}
    }
}