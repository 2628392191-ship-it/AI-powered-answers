package com.server.mcp;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 智谱 AI Web Search 工具
 * 智谱的 web_search/sse 是普通 HTTP SSE 接口，不是标准 MCP Server，
 * 需要手动发送 JSON-RPC 请求并解析 SSE 响应流。
 */
@Slf4j
public class ZhiPuMcp {

    @Value("${zhipu.api-key}")
    private String apiKey;

    private static final String SEARCH_URL = "https://open.bigmodel.cn/api/mcp/web_search/sse";

    @Tool(name = "zhipuSearch", description = "Search the web using ZhiPu AI, returns latest information from the internet")
    public String searchWeb(@ToolParam(description = "Search query keyword") String question) {
        try {
            String urlWithAuth = SEARCH_URL + "?Authorization=" + apiKey;
            HttpURLConnection conn = (HttpURLConnection) new URL(urlWithAuth).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "text/event-stream");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);

            // JSON-RPC 请求体
            String body = JSONUtil.createObj()
                    .set("jsonrpc", "2.0")
                    .set("id", UUID.randomUUID().toString())
                    .set("method", "tools/call")
                    .set("params", JSONUtil.createObj()
                            .set("name", "web_search")
                            .set("arguments", JSONUtil.createObj()
                                    .set("query", question)))
                    .toString();

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            // 读取 SSE 流，拼接所有 data 行
            StringBuilder raw = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data:")) {
                        String data = line.substring(5).trim();
                        if (!data.isEmpty() && !"[DONE]".equals(data)) {
                            raw.append(data);
                        }
                    }
                }
            }

            return parseResult(raw.toString());

        } catch (Exception e) {
            log.error("ZhiPu web search failed", e);
            return "搜索失败: " + e.getMessage();
        }
    }

    /**
     * 解析 JSON-RPC 响应，提取 result.content[].text
     */
    private String parseResult(String raw) {
        if (raw == null || raw.isBlank()) return "未获取到搜索结果";
        try {
            JSONObject json = JSONUtil.parseObj(raw);
            if (json.containsKey("error")) {
                return "搜索接口错误: " + json.getJSONObject("error").getStr("message");
            }
            JSONObject result = json.getJSONObject("result");
            if (result == null) return raw;
            JSONArray content = result.getJSONArray("content");
            if (content == null || content.isEmpty()) return raw;

            StringBuilder sb = new StringBuilder();
            for (Object item : content) {
                JSONObject obj = (JSONObject) item;
                if ("text".equals(obj.getStr("type"))) {
                    sb.append(obj.getStr("text"));
                }
            }
            return sb.isEmpty() ? raw : sb.toString();
        } catch (Exception e) {
            log.warn("解析搜索结果失败，返回原始内容", e);
            return raw;
        }
    }
}
