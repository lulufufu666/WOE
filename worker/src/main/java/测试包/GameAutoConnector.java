package 测试包;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GameAutoConnector {

    // 💡 在这里填入你的长期 JWT (`flutter.access_token`)
    private static final String LONG_TERM_JWT = "";

    private static final String AUTH_API_URL = "https://woe-idle.com/api/egg.client.v1.PlayerService/GetRealtimeConnection";

    public static void main(String[] args) {
        try {
            System.out.println("🔄 正在自动请求服务器换取短期 Token...");

            // 让 Java 自动寻找并使用 Windows 当前系统的代理配置（和 Apifox 逻辑一致）
            HttpClient httpClient = HttpClient.newBuilder()
                    .proxy(HttpClient.Builder.NO_PROXY) // 先清空，或者直接用系统默认
                    .build();

            // 1. 发送 gRPC-web POST 请求获取底层传输配置
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AUTH_API_URL))
                    .header("Authorization", "Bearer " + LONG_TERM_JWT)
                    .header("Content-Type", "application/proto")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36")
                    .header("Origin", "https://woe-idle.com")
                    .header("Referer", "https://woe-idle.com/main/village")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() != 200) {
                System.err.println("❌ 换票失败，HTTP 状态码: " + response.statusCode());
                return;
            }

            // 2. 从返回的二进制流中解析出 WebSocket 完整的带 Token 路径
            String rawBody = new String(response.body(), StandardCharsets.UTF_8);
            String wsUrl = parseWebSocketUrl(rawBody);

            if (wsUrl == null) {
                System.err.println("❌ 未能从响应中解析出有效的 WebSocket 链接！");
                return;
            }

            System.out.println("✅ 成功获取动态 WebSocket 地址: " + wsUrl);

            // 3. 立即使用获取到的凭证建立 WebSocket 长连接
            connectWebSocket(httpClient, wsUrl);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 直接精准提取 64 位十六进制 Token 并拼接
     */
    private static String parseWebSocketUrl(String rawBody) {
        // 匹配 64 位的十六进制哈希串（游戏返回的 token 格式）
        Pattern pattern = Pattern.compile("[a-f0-9]{64}");
        Matcher matcher = pattern.matcher(rawBody);

        if (matcher.find()) {
            String token = matcher.group();
            return "wss://ws.woe-idle.com/ws?token=" + token;
        }

        return null;
    }

    /**
     * 建立 WebSocket 客户端并监听消息
     */
    private static void connectWebSocket(HttpClient client, String wsUrl) {
        System.out.println("🚀 正在连接 WebSocket 长连接...");

        CompletableFuture<WebSocket> wsFuture = client.newWebSocketBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Origin", "https://woe-idle.com")
                .header("Referer", "https://woe-idle.com/main/village")
                .buildAsync(URI.create(wsUrl), new WebSocket.Listener() {

                    @Override
                    public void onOpen(WebSocket webSocket) {
                        System.out.println("🎉 完美！Java 客户端已成功连入游戏 WebSocket！");
                    }

                    @Override
                    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
                        byte[] bytes = new byte[data.remaining()];
                        data.get(bytes);
                        System.out.println("📥 收到游戏下行二进制封包，大小: " + bytes.length + " 字节");
                        return WebSocket.Listener.super.onBinary(webSocket, data, last);
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                        System.out.println("❌ WebSocket 连接关闭: " + reason + " (状态码: " + statusCode + ")");
                        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        System.err.println("⚠️ WebSocket 发生错误: " + error.getMessage());
                    }
                });

        try {
            wsFuture.join();
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}