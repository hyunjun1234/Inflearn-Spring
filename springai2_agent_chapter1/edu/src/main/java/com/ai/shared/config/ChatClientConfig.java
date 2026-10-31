package com.ai.shared.config;
// Spring AI의 ChatClient 인터페이스
// - 다양한 LLM(OpenAI, Gemini, Ollama 등)을 추상화하여 동일한 방식으로 호출할 수 있게 해줌

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ChatClientConfig 클래스
 *
 * - Spring 설정(Configuration) 클래스
 * - 여러 AI 모델(OpenAI, Gemini, Ollama)을 각각의 ChatClient로 구성
 * - Qualifier를 사용하여 서비스에서 원하는 모델을 선택 주입 가능하도록 구성
 */
@Configuration
@Slf4j
public class ChatClientConfig {

    /**
     * OpenAI 전용 ChatClient Bean 생성
     *
     * @param openAiChatModel
     *  - Spring AI가 자동 구성한 OpenAiChatModel
     *  - OpenAI API와 실제 통신을 담당하는 모델 객체
     *
     * @return ChatClient
     *  - OpenAI GPT 모델을 사용하는 ChatClient 인스턴스
     */
    @Bean // Spring 컨테이너에 Bean 등록
    @Qualifier("gpt") // 동일 타입(ChatClient) Bean 구분용 이름
    public ChatClient openAiChatClient(OpenAiChatModel openAiChatModel) {

        // 1️⃣ ChatClient.builder(model)
        //   - 특정 ChatModel(OpenAI)을 기반으로 ChatClient 생성 시작

        // 2️⃣ defaultSystem(...)
        //   - 모든 요청에 기본으로 포함될 System Prompt 설정
        //   - AI의 역할/성격을 정의하는 영역

        // 3️⃣ build()
        //   - ChatClient 최종 생성

        return ChatClient.builder(openAiChatModel)
                .defaultSystem("당신은 OpenAI가 제공하는 친절한 AI 조수입니다.")
                .build();
    }

    /**
     * Google Gemini 전용 ChatClient Bean 생성
     *
     * @param googleGenAiChatModel
     *  - Google Gemini API와 통신하는 모델 객체
     *
     * @return ChatClient
     *  - Gemini 모델을 사용하는 ChatClient 인스턴스
     */
    @Bean
    @Qualifier("gemini") // "gemini" 이름으로 Bean 등록
    public ChatClient geminiChatClient(GoogleGenAiChatModel googleGenAiChatModel) {

        // Gemini 모델 기반 ChatClient 생성
        // defaultSystem을 통해 Gemini 모델의 역할 정의
        return ChatClient.builder(googleGenAiChatModel)
                .defaultSystem("당신은 Google의 Gemini 모델을 기반으로 한 전문가입니다.")
                .build();
    }

    /**
     * Ollama (로컬 Llama 모델) 전용 ChatClient Bean 생성
     *
     * @param model
     *  - Ollama 서버와 연결되는 ChatModel 객체
     *  - 로컬에서 실행 중인 Llama 3.2 등의 모델과 통신
     *
     * @return ChatClient
     *  - 로컬 LLM을 사용하는 ChatClient 인스턴스
     */
    @Bean
    @Qualifier("llama") // "llama" 이름으로 Bean 등록
    public ChatClient llamaClient(OllamaChatModel model) {
        log.info("Llama Chat Client(ChatClientConfig)");
        // 로컬 LLM(Ollama) 기반 ChatClient 생성
        // 로컬 모델의 역할 정의
        return ChatClient.builder(model)
                .defaultSystem("당신은 로컬에서 구동되는 Llama 3.2 모델입니다.")
                .build();
    }

}
