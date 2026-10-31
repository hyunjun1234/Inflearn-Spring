package com.ai.chapter1.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * MultiLLMService
 *
 * 여러 LLM(OpenAI, Gemini, LLaMA 등)을 하나의 서비스에서
 * 통합 관리하고 호출할 수 있도록 만든 서비스 클래스.
 *
 * 주요 기능:
 * 1. 모델 타입에 따라 ChatClient 선택
 * 2. 동기 호출 (CircuitBreaker 적용)
 * 3. 스트리밍 호출 (Flux 기반 비동기 처리)
 */
@Service // Spring Bean 등록
@Slf4j   // log 객체 자동 생성
public class MultiLLMService {

    /**
     * LLM 타입(String) → ChatClient 매핑 정보
     *
     * key   : "openai", "gpt", "gemini", "llama"
     * value : 각 LLM에 연결된 ChatClient Bean
     *
     * Map 기반으로 관리하여 switch/case 제거
     */
    private final Map<String, ChatClient> clients;

    /**
     * 생성자 주입
     *
     * @Qualifier를 사용하여 서로 다른 ChatClient Bean을 구분
     *
     * @param llamaClient  LLaMA 전용 ChatClient
     * @param gptClient    OpenAI GPT 전용 ChatClient
     * @param geminiClient Gemini 전용 ChatClient
     */
    public MultiLLMService(
            @Qualifier("llama") ChatClient llamaClient,
            @Qualifier("gpt") ChatClient gptClient,
            @Qualifier("gemini") ChatClient geminiClient) {

        // LLM 타입별로 ChatClient를 Map에 저장
        this.clients = Map.of(
                "openai", gptClient,
                "gpt", gptClient,
                "gemini", geminiClient,
                "llama", llamaClient
        );
    }

    /**
     * 동기 LLM 호출 메서드
     *
     * CircuitBreaker 적용:
     * - 최근 호출 실패율이 설정값을 초과하면 자동으로 fallback 호출
     *
     * @param question 사용자 질문
     * @param type     LLM 타입 (openai, gemini, llama 등)
     * @return         LLM 응답 문자열
     */
    @CircuitBreaker(name = "multiLLM", fallbackMethod = "fallbackChat")
    public String chat(String question, String type) {

        // 1️⃣ 모델 타입을 소문자로 변환 후 ChatClient 조회
        ChatClient client = clients.get(type.toLowerCase());

        // 2️⃣ 존재하지 않는 모델인 경우 예외 발생
        if (client == null) {
            throw new IllegalArgumentException("지원하지 않는 모델: " + type);
        }

        // 3️⃣ LLM 호출 실행 (동기 방식)
        return client.prompt()                 // 프롬프트 생성 시작
                .system("질문에 대한 답변을 한국어로 친절하게 답변해야 합니다.") // 시스템 메시지
                .user(question)                // 사용자 질문 입력
                .call()                        // 동기 호출 실행
                .content();                    // 응답 문자열 반환
    }

    /**
     * CircuitBreaker fallback 메서드
     *
     * chat() 호출 실패 시 자동으로 실행됨
     *
     * ⚠️ 주의:
     * - 원본 메서드와 동일한 파라미터 + 마지막에 Throwable 추가
     *
     * @param question 사용자 질문
     * @param type     LLM 타입
     * @param ex       발생한 예외
     * @return         대체 응답 메시지
     */
    public String fallbackChat(String question, String type, Throwable ex) {

        // 에러 로그 기록
        log.error("Fallback 호출됨: 모델={}, 질문={}, 오류={}",
                type, question, ex.getMessage());

        // 사용자에게 반환할 안전한 메시지
        return "현재 모델 호출이 불가능합니다. 잠시 후 다시 시도해주세요.";
    }

    /**
     * 비동기 스트리밍 LLM 호출
     *
     * Flux<String>을 반환하여
     * SSE(Server-Sent Events) 방식으로 실시간 전송 가능
     *
     * @param question 사용자 질문
     * @param type     LLM 타입
     * @return         Flux<String> (스트리밍 응답)
     */
    public Flux<String> chatStream(String question, String type) {

        // 1️⃣ 모델 조회
        ChatClient client = clients.get(type.toLowerCase());

        // 2️⃣ 지원하지 않는 모델일 경우 Flux 에러 반환
        if (client == null) {
            return Flux.error(new IllegalArgumentException("지원하지 않는 모델: " + type));
        }

        // 3️⃣ 스트리밍 호출
        return client.prompt()                     // 프롬프트 시작
                .system("질문에 대한 답변을 한국어로 친절하게 답변해야 합니다.")
                .user(question)                    // 사용자 질문
                .stream()                          // 스트리밍 모드 시작
                .content()                         // Flux<String> 반환
                .onErrorResume(e -> {              // 스트리밍 중 오류 발생 시 처리
                    log.error("스트리밍 중 오류 발생: {}", e.getMessage());
                    return Flux.just(
                            "현재 모델 호출 중 오류가 발생했습니다."
                    );
                });
    }
}

