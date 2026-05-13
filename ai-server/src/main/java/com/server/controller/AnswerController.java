package com.server.controller;


import com.server.service.AnswerService;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai/answer")
public class AnswerController {

    @Resource
    private AnswerService answerService;

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> doChatWithLoveAppSSE(
            @RequestParam("userMessage") String userMessage,
            @RequestParam String chatId) {
        return answerService.doChatWithStream(userMessage, chatId)
                .map(chunk -> ServerSentEvent.<String>builder()
                .data(chunk)
                .build());
    }

    @DeleteMapping("/delmemory/{chatId}")
    public boolean delmemory(@PathVariable String chatId) {
        return answerService.clear(chatId);
    }



}
