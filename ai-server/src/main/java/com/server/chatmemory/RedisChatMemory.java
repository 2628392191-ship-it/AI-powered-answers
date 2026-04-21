package com.server.chatmemory;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Collections;
import java.util.List;


public class RedisChatMemory implements ChatMemory {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String PREFIX = "chat:";

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
                
        stringRedisTemplate.opsForList().rightPushAll(PREFIX + conversationId, list);
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
