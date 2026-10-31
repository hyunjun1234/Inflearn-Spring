package com.ai.chapter1.service;

import com.ai.shared.util.SecurityScanner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
/**
 * SecurityService
 *
 * 사용자 입력을 받아 민감 정보 여부를 판별한 후
 * 적절한 LLM(Local or External)으로 라우팅하는 보안 기반 AI 서비스 클래스.
 *
 * - 민감 정보 포함 → Local LLM (Llama)
 * - 일반 정보 → 외부 GPT 모델
 */
@Service // Spring Service Bean 등록
@Slf4j   // Lombok: Logger 자동 생성
public class SecurityService {

    /**
     * 민감 정보 탐지를 수행하는 보안 스캐너 컴포넌트
     */
    private final SecurityScanner securityScanner;

    /**
     * 내부(Local) LLM 클라이언트 (예: Ollama + Llama3)
     */
    private final ChatClient llamaClient;

    /**
     * 외부 고성능 LLM 클라이언트 (예: GPT-4)
     */
    private final ChatClient gptClient;


    /**
     * 생성자 주입(Constructor Injection)
     *
     * @param securityScanner 민감 정보 판별기
     * @param llamaClient     내부 LLM 클라이언트 (Qualifier = "llama")
     * @param gptClient       외부 GPT 클라이언트 (Qualifier = "gpt")
     */
    public SecurityService(
            SecurityScanner securityScanner,
            @Qualifier("llama") ChatClient llamaClient,
            @Qualifier("gpt") ChatClient gptClient) {

        this.securityScanner = securityScanner;
        this.llamaClient = llamaClient;
        this.gptClient = gptClient;
    }


    /**
     * 사용자 요청을 처리하는 메인 메서드
     *
     * 처리 흐름:
     * 1️⃣ 민감 정보 검사
     * 2️⃣ 민감하면 → Local LLM 호출
     * 3️⃣ 아니면 → GPT 호출
     * 4️⃣ 각 모델 호출 실패 시 예외 처리
     *
     * @param userInput 사용자 입력 텍스트
     * @return LLM 응답 결과 문자열
     */
    public String processRequest(String userInput) {

        // ============================
        // [Step 1] 민감 정보 스캔 단계
        // ============================
        if (securityScanner.isSensitive(userInput)) {

            // 로그에 민감 데이터가 그대로 남지 않도록 마스킹 처리
            log.warn("Sensitive data detected: {}",
                    securityScanner.maskSensitive(userInput));

            try {
                // =========================================
                // [Step 2] 민감 데이터 → Local LLM 호출
                // =========================================

                return "LLAMA: " + llamaClient.prompt()

                        // 시스템 프롬프트: 보안 모드 강제
                        .system("귀하는 보안 모드에서 동작 중입니다. 외부 유출 없이 내부 데이터만으로 답변하세요.")

                        // 사용자 입력 전달
                        .user(userInput)

                        // LLM 호출 실행
                        .call()

                        // 응답 본문 추출
                        .content();

            } catch (Exception e) {

                // Ollama 서버가 죽었거나 네트워크 오류 발생 시 처리
                log.error("LLAMA 호출 실패: {}", e.getMessage());

                // 민감 데이터이므로 외부 GPT로 절대 fallback 하지 않음
                return "LLAMA 모델 호출 실패: 내부 데이터 처리 불가.";
            }
        }

        // ====================================
        // [Step 3] 일반 데이터 → GPT 라우팅
        // ====================================
        log.info("Safe data. Routing to GPT-4.");

        try {
            return "GPT: " +

                    // 단일 메시지 프롬프트 호출
                    gptClient.prompt(userInput)
                            .call()
                            .content();

        } catch (Exception e) {

            // GPT 서버 오류, API 장애 등
            log.error("GPT 호출 실패: {}", e.getMessage());

            return "GPT 모델 호출 실패: 잠시 후 다시 시도하세요.";
        }
    }
}

