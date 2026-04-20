package com.server.constant;

/**
 * 描述：FileConstant
 */
public interface FileConstant
{
    /** 文件保存目录 **/
    String FILE_SAVE_DIR = System.getProperty("user.dir") + "/tmp";

    /** 聊天记录文件目录 **/
    String CHAT_MEMORY_FILE = FILE_SAVE_DIR+"/chat-memory";

    /** 系统提示词文件名 **/
    String SYSTEM_PROMPT = "SystemPrompt.txt";
}
