package com.lulu.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final Map<String, Map<String, Object>> nodes = new ConcurrentHashMap<>();
    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping(value = "/heartbeat", produces = "text/plain;charset=UTF-8")
    public String heartbeat(@RequestBody Map<String, Object> nodeInfo, HttpServletRequest request) {
        String accountId = (String) nodeInfo.get("accountId");
        nodeInfo.put("lastHeartbeat", System.currentTimeMillis());

        // 获取真实通信 IP
        String realIp = request.getHeader("X-Forwarded-For");
        if (realIp == null || realIp.isEmpty() || "unknown".equalsIgnoreCase(realIp)) {
            realIp = request.getRemoteAddr();
        }

        // 🚀 核心修复 2：如果提取到的是 IPv6 地址 (包含冒号且没有括号)，自动添加 []
        if (realIp != null && realIp.contains(":") && !realIp.startsWith("[")) {
            realIp = "[" + realIp + "]";
        }

        nodeInfo.put("ip", realIp); // 强制覆盖真实IP
        nodes.put(accountId, nodeInfo);

        return "OK";
    }

    @GetMapping(value = "/nodes", produces = "application/json;charset=UTF-8")
    public Collection<Map<String, Object>> getNodes() {
        long now = System.currentTimeMillis();
        nodes.values().removeIf(node -> now - (Long) node.get("lastHeartbeat") > 15000);
        return nodes.values();
    }

    @PostMapping(value = "/command/{accountId}", produces = "text/plain;charset=UTF-8")
    public String sendCommand(@PathVariable String accountId, HttpServletRequest request) {
        Map<String, Object> node = nodes.get(accountId);
        if (node == null) return "指令下发失败：该账号已离线";

        String ip = (String) node.get("ip");
        Integer port = (Integer) node.get("port");
        String queryString = request.getQueryString();
        String targetUrl = "http://" + ip + ":" + port + "/api/command" + (queryString != null ? "?" + queryString : "");

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(targetUrl, null, String.class);
            return response.getBody();
        } catch (Exception e) {
            return "指令下发失败：节点网络异常";
        }
    }

    @GetMapping(value = "/config/{accountId}", produces = "application/json;charset=UTF-8")
    public String getConfig(@PathVariable String accountId) {
        Map<String, Object> node = nodes.get(accountId);
        if (node == null) return "{\"error\": \"节点已离线\"}";
        try {
            String targetUrl = "http://" + node.get("ip") + ":" + node.get("port") + "/api/config";
            return restTemplate.getForObject(targetUrl, String.class);
        } catch (Exception e) {
            return "{\"error\": \"节点网络异常\"}";
        }
    }

    // 🚀 核心修复点：将 payload 的类型从 String 改为 byte[]。
    // 这将强制 Spring Boot 放弃 ISO-8859-1 转换，直接透传带有中文的 UTF-8 字节流。
    @PostMapping(value = "/config/{accountId}", produces = "text/plain;charset=UTF-8")
    public String updateConfig(@PathVariable String accountId, @RequestBody byte[] payload) {
        Map<String, Object> node = nodes.get(accountId);
        if (node == null) return "指令下发失败：该账号已离线";
        try {
            String targetUrl = "http://" + node.get("ip") + ":" + node.get("port") + "/api/config";
            return restTemplate.postForObject(targetUrl, payload, String.class);
        } catch (Exception e) {
            return "节点网络异常";
        }
    }
}