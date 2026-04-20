package com.server.config;

import com.server.rag.AnswerDocumentLoader;
import com.server.rag.AnswerKeywordEnricher;
import com.server.rag.AnswerTokenSplitter;
import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class VectorStoreConfig {
    //文档加载器
    @Resource
    private AnswerDocumentLoader answerDocumentLoader;

    //文档切割器
    @Resource
    private AnswerTokenSplitter answerTokenSplitter;

    //文档关键词补充器
    @Resource
    private AnswerKeywordEnricher answerKeywordEnricher;

    //向量存储器--用于将用户输入的信息转为向量已用于ai进行检索匹配
    @Bean
    VectorStore answerVectorStore(EmbeddingModel dashscopeEmbeddingModel)
    {
        // 创建向量存储器
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel).build();

        // 加载文档
        List<Document> documentList = answerDocumentLoader.loadMarkdowns();

        // 自主切分文档
        List<Document> splitDocuments = answerTokenSplitter.splitCustomized(documentList);

        // 自动补充关键词元信息
        List<Document> enrichedDocuments = answerKeywordEnricher.enrichDocuments(splitDocuments);
        simpleVectorStore.add(enrichedDocuments);

        return simpleVectorStore;
    }
}
