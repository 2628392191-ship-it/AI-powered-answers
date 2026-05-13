package com.server.chatmemory;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Collections;
import java.util.List;


public class RedisChatMemory implements ChatMemory {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String PREFIX = "chat:";
    private static final int MAX_HISTORY_SIZE = 20;
    private static final Duration TTL = Duration.ofDays(7);

    public RedisChatMemory(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        if (CollUtil.isEmpty(messages)) return;
        // 序列化为 DTO 再存储，避免直接序列化接口类型
        List<String> list = messages.stream()
                .map(m -> JSONUtil.toJsonStr(new ChatMessageDTO(m.getText(), m.getMessageType().name())))
                .toList();
                
        String key = PREFIX + conversationId;
        stringRedisTemplate.opsForList().rightPushAll(key, list);
        // 只保留最近 N 条消息，控制内存和 Token 消耗
        stringRedisTemplate.opsForList().trim(key, -MAX_HISTORY_SIZE, -1);
        // 设置过期时间
        stringRedisTemplate.expire(key, TTL);
    }

    @Override
    public List<Message> get(String conversationId) {
        List<String> list = stringRedisTemplate.opsForList().range(PREFIX + conversationId, 0, -1);
        if (list == null || list.isEmpty()) return Collections.emptyList();
        // 反序列化为 DTO，再按 role 还原为具体 Message 子类
        return list.stream()
                .map(str -> JSONUtil.toBean(str, ChatMessageDTO.class))
                .map(RedisChatMemory::toMessage)
                .toList();
    }

    @Override
    public void clear(String conversationId) {
        stringRedisTemplate.delete(PREFIX + conversationId);
    }

    private static Message toMessage(ChatMessageDTO dto) {
        return switch (dto.getRole().toUpperCase()) {
            case "ASSISTANT" -> new AssistantMessage(dto.getContent());
            case "SYSTEM"    -> new SystemMessage(dto.getContent());
            default          -> new UserMessage(dto.getContent());
        };
    }
}
