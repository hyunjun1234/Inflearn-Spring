package com.ai.chapter1.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * TaskType
 *
 * 사용자 입력을 어떤 유형의 작업인지 분류하기 위한 Enum.
 * 각 유형에 따라 적절한 AI 모델로 라우팅된다.
 */
enum TaskType {

    CREATIVE,   // 창의적 글쓰기, 복잡한 추론, 코딩 → GPT 우선
    TECHNICAL,  // 기술 문서 분석, 대용량 데이터 처리 → Gemini 우선
    GENERAL,    // 단순 질의응답, 일반 대화 → Llama 기본
    UNKNOWN     // 분류 실패 또는 예외 발생 시 기본 처리
}


/**
 * RouterService
 *
 * ✔ 사용자 입력을 분석하여
 * ✔ 적절한 AI 모델(GPT, Gemini, Llama)로 라우팅하고
 * ✔ 예외 발생 시 fallback 처리까지 수행하는 서비스 클래스
 */
@Service
@Slf4j
public class RouterService {

    /**
     * Llama 모델용 ChatClient
     * 일반 질의 및 기본 fallback 모델
     */
    private final ChatClient llamaClient;

    /**
     * GPT 모델용 ChatClient
     * 창의적 작업 및 최종 fallback 모델
     */
    private final ChatClient gptClient;

    /**
     * Gemini 모델용 ChatClient
     * 기술 분석 작업 처리용
     */
    private final ChatClient geminiClient;


    /**
     * 생성자 주입 방식 (권장)
     *
     * @Qualifier 를 사용하여 Bean 이름으로 모델을 구분한다.
     *
     * @param llamaClient  Llama 모델 ChatClient
     * @param gptClient    GPT 모델 ChatClient
     * @param geminiClient Gemini 모델 ChatClient
     */
    public RouterService(
            @Qualifier("llama") ChatClient llamaClient,
            @Qualifier("gpt") ChatClient gptClient,
            @Qualifier("gemini") ChatClient geminiClient) {

        this.llamaClient = llamaClient;
        this.gptClient = gptClient;
        this.geminiClient = geminiClient;
    }


    /**
     * routeAndAsk
     *
     * 1️⃣ 사용자 입력을 TaskType으로 분류
     * 2️⃣ 분류 결과에 따라 적절한 모델 호출
     * 3️⃣ 모델 호출 실패 시 fallback 처리
     *
     * @param userInput 사용자 질문
     * @return 모델 응답 결과 문자열
     */
    public String routeAndAsk(String userInput) {

        // -----------------------------
        // 1단계: 입력 분류
        // -----------------------------
        TaskType taskType = classifyTaskWithFallback(userInput);

        log.info("UserInput='{}', Routed taskType={}", userInput, taskType);

        try {
            // -----------------------------
            // 2단계: 분류 결과에 따른 모델 라우팅
            // -----------------------------
            return switch (taskType) {

                // 창의적 작업 → GPT
                case CREATIVE -> tryPrompt(gptClient, "GPT", userInput);

                // 기술 분석 작업 → Gemini
                case TECHNICAL -> tryPrompt(geminiClient, "GEMINI", userInput);

                // 일반 질문 또는 분류 실패 → Llama
                case GENERAL, UNKNOWN -> tryPrompt(llamaClient, "LLAMA", userInput);
            };

        } catch (Exception e) {

            // -----------------------------
            // 3단계: 모델 호출 실패 시 fallback 처리
            // -----------------------------
            log.error("모델 호출 실패 (taskType={})", taskType, e);

            try {
                // 최종 fallback → GPT 재시도
                return tryPrompt(gptClient, "GPT Fallback", userInput);

            } catch (Exception ex) {

                // 모든 모델 실패
                log.error("모든 fallback 실패", ex);
                return "모델 호출 실패: " + ex.getMessage();
            }
        }
    }


    /**
     * classifyTaskWithFallback
     *
     * ✔ Llama를 사용하여 먼저 분류
     * ✔ Llama 실패 시 GPT로 fallback 분류
     * ✔ GPT도 실패 시 UNKNOWN 반환
     *
     * @param userInput 사용자 입력
     * @return TaskType 분류 결과
     */
    private TaskType classifyTaskWithFallback(String userInput) {

        // 분류용 프롬프트 생성
        String prompt = """
            다음 사용자 입력을 분석하여 한 가지 유형으로 분류하세요:
            - CREATIVE: 복잡한 추론, 창의적인 글쓰기 또는 코딩 관련 질문.
            - TECHNICAL: 긴 문서 분석, 기술적 명세 또는 구글 서비스 관련 작업.
            - GENERAL: 단순한 질문, 일반적인 사실 확인 또는 일상 대화.

            응답은 반드시 다른 설명 없이 단어(CREATIVE, TECHNICAL, GENERAL) 중 하나로만 대답하세요.
            입력값: %s
            """.formatted(userInput);

        try {

            // -----------------------------
            // 1차 시도: Llama 분류
            // -----------------------------
            String result = llamaClient.prompt(prompt)
                    .call()
                    .content()
                    .trim()
                    .toUpperCase();

            return mapToTaskType(result);

        } catch (Exception e) {

            log.warn("Llama 분류 실패, GPT fallback 시도", e);

            try {

                // -----------------------------
                // 2차 시도: GPT 분류
                // -----------------------------
                String result = gptClient.prompt(prompt)
                        .call()
                        .content()
                        .trim()
                        .toUpperCase();

                return mapToTaskType(result);

            } catch (Exception ex) {

                // 분류 완전 실패
                log.warn("GPT 분류도 실패, UNKNOWN 처리", ex);
                return TaskType.UNKNOWN;
            }
        }
    }


    /**
     * mapToTaskType
     *
     * 모델이 반환한 문자열을 안전하게 TaskType으로 변환한다.
     * (공백, 마침표 등 특수문자 제거 후 매핑)
     *
     * @param raw 모델 응답 원문
     * @return TaskType
     */
    private TaskType mapToTaskType(String raw) {

        return switch (raw.replaceAll("[^A-Z]", "")) {

            case "CREATIVE" -> TaskType.CREATIVE;
            case "TECHNICAL" -> TaskType.TECHNICAL;
            case "GENERAL" -> TaskType.GENERAL;
            default -> TaskType.UNKNOWN;
        };
    }


    /**
     * tryPrompt
     *
     * 실제 모델 호출을 수행하는 공통 메서드.
     *
     * @param client    호출할 ChatClient
     * @param modelName 로그 및 응답 prefix용 모델 이름
     * @param userInput 사용자 입력
     * @return 모델 응답 문자열
     * @throws Exception 모델 호출 실패 시 예외 전파
     */
    private String tryPrompt(ChatClient client, String modelName, String userInput) throws Exception {

        return modelName + ": " +
                client.prompt(userInput)
                        .call()
                        .content();
    }
}
