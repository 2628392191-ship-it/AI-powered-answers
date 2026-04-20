package com.server.tools;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.stream.Collectors;

public class LovingAdviceTool {

    private final VectorStore vectorStore;

    public LovingAdviceTool(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }


    @Tool(description = "Provides loving advice based on the user's input.")
    public String provideLovingAdvice(String input) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(input)
                .topK(3) // 取相关度最高的前 3 个文档片段
                .similarityThreshold(0.75).build(); //相似度阈值
        List<Document> documents = vectorStore.similaritySearch(searchRequest);

        return documents.stream().map(doc -> "【参考片段】\n" + doc.getText() + "\n")
                .collect(Collectors.joining("\n---\n"));
    }
}
