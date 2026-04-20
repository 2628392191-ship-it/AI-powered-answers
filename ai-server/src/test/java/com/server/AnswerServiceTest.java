package com.server;

import com.server.service.AnswerService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AnswerServiceTest {

      @Resource
      private AnswerService answerService;

       @Test
       void testByChatMemory(){
           String result = answerService.doChat("Java语言怎么样", "1");
           System.out.println(result);
       }

}
