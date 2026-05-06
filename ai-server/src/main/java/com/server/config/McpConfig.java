package com.server.config;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 手动接入需要自定义 Header 的标准 MCP Server
 * 适用场景：MCP Server 需要 Authorization 等认证头
 *
 * 注意：对端必须是标准 MCP 协议的服务（支持 initialize 握手）
 *       智谱 web_search/sse 不是标准 MCP Server，不能用此方式
 */

public class McpConfig {

    /**
     * 示例：接入一个需要 Bearer Token 认证的标准 MCP Server
     * 实际使用时替换 url 和 token
     */

    @Value("${zhipu.api-key}")
    private String ApiKey;


    public ToolCallbackProvider ZhiPuMcpProvider() {
        var transport = HttpClientSseClientTransport
                .builder("https://open.bigmodel.cn/api/mcp/web_search/sse?")
                // 添加认证 Header
                .customizeRequest(req -> req.header("Authorization", ApiKey))
                .build();

        var mcpClient = McpClient.sync(transport)
                .build();
        mcpClient.initialize(); // 标准 MCP 握手

        return new SyncMcpToolCallbackProvider(mcpClient);
    }
}
