package com.server.chatmemory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于文件持久化的对话记忆
 * 使用kryo进行序列化来持久化对话数据
 */
public class FileBasedChatMemory implements ChatMemory
{
    private final String BASE_DIR;
    private static final Kryo kryo = new Kryo();

    static
    {
        kryo.setRegistrationRequired(false);
        // 设置实例化策略
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
        // 注册 DTO 类
        kryo.register(ChatMessageDTO.class);
        kryo.register(ArrayList.class);
    }

    // 构造对象时，指定文件保存目录
    public FileBasedChatMemory(String dir)
    {
        this.BASE_DIR = dir;
        File baseDir = new File(dir);
        if (!baseDir.exists())
        {
            baseDir.mkdirs();
        }
    }

    @Override
    public void add(String conversationId, List<Message> messages)
    {
        List<Message> conversationMessages = getOrCreateConversation(conversationId);
        conversationMessages.addAll(messages);
        saveConversation(conversationId, conversationMessages);
    }

    @Override
    public List<Message> get(String conversationId)
    {
        return getOrCreateConversation(conversationId);
    }

    @Override
    public void clear(String conversationId)
    {
        File file = getConversationFile(conversationId);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 对话摘要信息
     */
    public record ConversationSummary(String id, String title) {}

    /**
     * 列出所有对话摘要
     * @return
     */
    public List<ConversationSummary> listConversations() {
        List<ConversationSummary> summaries = new ArrayList<>();
        File baseDir = new File(BASE_DIR);
        if (baseDir.exists() && baseDir.isDirectory()) {
            File[] files = baseDir.listFiles((dir, name) -> name.endsWith(".kryo"));
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    String id = fileName.substring(0, fileName.lastIndexOf("."));
                    List<Message> messages = getOrCreateConversation(id);
                    String title = "新对话";
                    // 尝试获取第一条用户消息作为标题
                    if (messages != null && !messages.isEmpty()) {
                        for (Message m : messages) {
                            if ("USER".equalsIgnoreCase(m.getMessageType().name())) {
                                String content = m.getText();
                                if (content != null && !content.trim().isEmpty()) {
                                    title = content.length() > 20 ? content.substring(0, 20) + "..." : content;
                                    break;
                                }
                            }
                        }
                    }
                    summaries.add(new ConversationSummary(id, title));
                }
            }
        }
        return summaries;
    }

    /**
     * 列出所有对话 ID
     * @return
     */
    public List<String> listConversationIds() {
        List<String> conversationIds = new ArrayList<>();
        File baseDir = new File(BASE_DIR);
        if (baseDir.exists() && baseDir.isDirectory()) {
            File[] files = baseDir.listFiles((dir, name) -> name.endsWith(".kryo"));
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    conversationIds.add(fileName.substring(0, fileName.lastIndexOf(".")));
                }
            }
        }
        return conversationIds;
    }

    private List<Message> getOrCreateConversation(String conversationId)
    {
        File file = getConversationFile(conversationId);
        List<?> rawList = new ArrayList<>();
        if (file.exists())
        {
            try (Input input = new Input(new FileInputStream(file))) {
                rawList = kryo.readObject(input, ArrayList.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        List<Message> messages = new ArrayList<>();
        if (rawList != null) {
            for (Object item : rawList) {
                if (item instanceof ChatMessageDTO dto) {
                    if ("USER".equalsIgnoreCase(dto.getRole())) {
                        messages.add(new UserMessage(dto.getContent()));
                    } else if ("ASSISTANT".equalsIgnoreCase(dto.getRole())) {
                        messages.add(new AssistantMessage(dto.getContent()));
                    } else if ("SYSTEM".equalsIgnoreCase(dto.getRole())) {
                        messages.add(new SystemMessage(dto.getContent()));
                    } else {
                        messages.add(new UserMessage(dto.getContent()));
                    }
                } else if (item instanceof Message msg) {
                    // 如果已经是 Message 类型（旧版本数据），直接添加
                    messages.add(msg);
                }
            }
        }
        return messages;
    }

    private void saveConversation(String conversationId, List<Message> messages)
    {
        File file = getConversationFile(conversationId);
        // 转换为 DTO 进行保存
        ArrayList<ChatMessageDTO> dtos = messages.stream().map(m -> 
            new ChatMessageDTO(m.getText(), m.getMessageType().name())
        ).collect(Collectors.toCollection(ArrayList::new));
        
        try (Output output = new Output(new FileOutputStream(file))) {
            kryo.writeObject(output, dtos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private File getConversationFile(String conversationId)
    {
        return new File(BASE_DIR, conversationId + ".kryo");
    }
}
