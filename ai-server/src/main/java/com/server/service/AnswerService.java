package com.server.service;


import com.server.advisor.MyLoggerAdvisor;
import com.server.chatmemory.RedisChatMemory;
import com.server.constant.FileConstant;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@Slf4j
public class AnswerService {
    private final ChatMemory chatMemory;

    private final ChatClient chatClient;

    private final String systemPrompt;

    public AnswerService(ChatModel dashscopeChatModel,StringRedisTemplate stringRedisTemplate) {
        //使用redis存储会话记忆
        this.chatMemory = new RedisChatMemory(stringRedisTemplate);
        //获取系统提示词
        ClassPathResource systemPromptResource = new ClassPathResource(FileConstant.SYSTEM_PROMPT);
        systemPrompt = systemPromptResource.getDescription();

        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(systemPrompt)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

//    @Resource
//    private VectorStore answerVectorStore;

    public Flux<String> doChatWithStream(String userMessage, String chatId) {
        return chatClient
                .prompt()
                .user(userMessage)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
                .stream()
                .content();
    }


    public String doChat(String userMessage, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(userMessage)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }


    public boolean clear(String chatId) {
        chatMemory.clear(chatId);
        return true;
    }


}
