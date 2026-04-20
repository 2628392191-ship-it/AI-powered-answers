package com.server.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

@Configuration
@Slf4j
public class MyLoggerAdvisor implements CallAdvisor, StreamAdvisor {
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain chain) {
        //拦截并打印非流式请求和响应的日志
        chatClientRequest = before(chatClientRequest);
        ChatClientResponse chatClientResponse = chain.nextCall(chatClientRequest);
        observeAfter(chatClientResponse);
        return chatClientResponse;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain chain) {
        //拦截并打印流式请求和响应的日志
        chatClientRequest = before(chatClientRequest);
        return (new ChatClientMessageAggregator()).aggregateChatClientResponse(chain.nextStream(chatClientRequest), this::observeAfter);
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return 0;
    }

    private ChatClientRequest before(ChatClientRequest request)
    {
        log.info("AI Request: {}", request.prompt());
        return request;
    }

    private void observeAfter(ChatClientResponse chatClientResponse)
    {
        log.info("AI Response: {}", chatClientResponse.chatResponse().getResult().getOutput().getText());
    }
}
