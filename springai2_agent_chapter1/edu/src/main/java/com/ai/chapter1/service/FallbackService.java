package com.ai.chapter1.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * FallbackService
 *
 * ▶ 역할
 * - Llama(Ollama)와 GPT 두 개의 AI 모델을 호출하는 서비스
 * - Ollama 서버 장애 발생 시 자동으로 GPT로 fallback 처리
 * - Resilience4j CircuitBreaker 적용
 *
 * ▶ 동작 흐름
 * 1. llama 호출 시 서킷 브레이커 감시
 * 2. 실패율 초과 → OPEN 상태 전환
 * 3. OPEN 상태에서는 fallback 메서드 자동 호출
 * 4. GPT가 대신 응답 반환
 */
@Service
@Slf4j
public class FallbackService {

    /**
     * GPT용 ChatClient Bean
     * - OpenAI 또는 외부 GPT 모델 호출용
     * - fallback 시 사용됨
     */
    private final ChatClient gptClient;

    /**
     * Llama(Ollama)용 ChatClient Bean
     * - 로컬 Ollama 서버 호출용
     * - 서킷 브레이커 보호 대상
     */
    private final ChatClient llamaClient;

    /**
     * 생성자 주입 (Constructor Injection)
     *
     * @Qualifier("llama") : Bean 이름이 "llama"인 ChatClient 주입
     * @Qualifier("gpt")   : Bean 이름이 "gpt"인 ChatClient 주입
     *
     * Spring이 애플리케이션 시작 시 자동으로 주입
     */
    public FallbackService(
            @Qualifier("llama") ChatClient llamaClient,
            @Qualifier("gpt") ChatClient gptClient) {

        this.gptClient = gptClient;
        this.llamaClient = llamaClient;
    }

    /**
     * chat()
     *
     * ▶ 실제 AI 호출 진입점 (Entry Point)
     * ▶ 서킷 브레이커 적용 대상 메서드
     *
     * @CircuitBreaker 설명:
     * - name = "ollamaService"
     *     → application.yml에 정의된 서킷 브레이커 설정 사용
     * - fallbackMethod = "fallbackToGpt"
     *     → 예외 발생 시 자동 호출될 메서드
     *
     * @param question 사용자 질문
     * @param type     호출할 AI 타입 ("llama" 또는 "gpt")
     * @return AI 응답 문자열
     */
    @CircuitBreaker(name = "ollamaService", fallbackMethod = "fallbackToGpt")
    public String chat(String question, String type) {

        // 1️⃣ llama 요청인 경우
        if ("llama".equalsIgnoreCase(type)) {

            // 로그 출력 (Ollama 호출 시작)
            log.info(">>> Ollama 서버로 요청을 보냅니다.");

            /**
             * 2️⃣ 실제 Ollama 서버 호출
             *
             * llamaClient.prompt(question)
             *     → 질문 설정
             *
             * .call()
             *     → 동기 방식으로 실제 API 호출 실행
             *
             * .content()
             *     → AI 응답 텍스트 추출
             *
             * ⚠️ 여기서 RuntimeException 발생 시:
             *     → CircuitBreaker가 감지
             *     → fallbackToGpt() 자동 실행
             */
            return llamaClient.prompt(question).call().content();
        }

        // 3️⃣ GPT 직접 호출 (서킷 브레이커 대상 아님)
        return callAi(gptClient, question);
    }

    /**
     * fallbackToGpt()
     *
     * ▶ 서킷 브레이커가 OPEN 상태일 때 자동 실행되는 메서드
     * ▶ chat() 메서드와 동일한 파라미터 + Throwable 필수
     *
     * @param question 원래 질문
     * @param type     요청 타입
     * @param t        발생한 예외 객체
     * @return GPT 대체 응답
     */
    public String fallbackToGpt(String question, String type, Throwable t) {

        // 1️⃣ 경고 로그 출력 (서킷 브레이커 작동 알림)
        log.warn("🚨 서킷 브레이커 작동! 타입: {}, 사유: {}", type, t.getMessage());

        // 2️⃣ GPT 호출 (대체 모델)
        String gptResponse = callAi(gptClient, question);

        // 3️⃣ 사용자에게 장애 안내 메시지 포함하여 반환
        return "[알림: 로컬 서버 장애로 GPT가 대신 답변합니다]\n\n" + gptResponse;
    }

    /**
     * callAi()
     *
     * ▶ 공통 AI 호출 메서드
     * ▶ GPT / Llama 모두 동일한 호출 구조이므로 재사용
     *
     * @param client   사용할 ChatClient
     * @param question 질문 내용
     * @return AI 응답 텍스트
     */
    private String callAi(ChatClient client, String question) {

        /**
         * prompt(question)
         *   → AI 모델에 질문 세팅
         *
         * call()
         *   → 실제 API 호출
         *
         * content()
         *   → 최종 텍스트 결과 반환
         */
        return client.prompt(question).call().content();
    }
}
