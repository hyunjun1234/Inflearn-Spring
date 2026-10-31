package com.ai.chapter1.controller;

import com.ai.chapter1.service.FallbackService;
import com.ai.chapter1.service.MultiLLMService;
import com.ai.chapter1.service.RouterService;
import com.ai.chapter1.service.SecurityService;
import com.ai.shared.util.SecurityScanner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;


/**
 * 📌 Chapter 1. Chat Completion Controller
 *
 * - 다양한 LLM 호출 방식을 실습하기 위한 컨트롤러
 * - Zero-Shot / Few-Shot / CoT / Streaming / Fallback / Router / Security 기능 제공
 *
 * 엔드포인트 Base URL : /ch1
 */
@RestController // REST API 컨트롤러 선언 (JSON/String 반환)
@RequestMapping("/ch1") // 공통 URL Prefix
@Slf4j // Lombok Logger 자동 생성
@RequiredArgsConstructor // final 필드 생성자 자동 주입
public class Ch1Controller {
    /**
     * 💬 MultiLLMService
     * - 여러 LLM(OpenAI, Gemini 등)을 통합 호출하는 서비스
     * - type 값에 따라 모델 분기 처리
     */
    private final MultiLLMService multiLLMService;
    /**
     * 🔁 FallbackService
     * - 1차 모델 실패 시 대체 모델 호출
     * - 안정적인 AI 서비스 운영을 위한 Failover 전략
     */
    private final FallbackService fallbackService;
    /**
     * 🧭 RouterService
     * - 질문 내용을 분석하여 적절한 LLM 선택
     * - 예: 수학 → GPT-4, 번역 → Gemini 등
     */
    private final RouterService routerService;

    /**
     * 🔐 SecurityService
     * - 사용자 입력에 대한 보안 처리
     * - 프롬프트 인젝션 방어
     * - 정책 기반 필터링
     */
    private final SecurityService securityService;
    /**
     * 🛡 SecurityScanner
     * - 민감 정보 마스킹 처리
     * - 로그에 개인정보가 남지 않도록 보호
     */
    private final SecurityScanner securityScanner;
    // ================================================================
    // 1️⃣ Chat Completion (Zero-Shot Prompting)
    // ================================================================
    /**
     * 기본 Chat Completion API
     *
     * @param prompt 사용자 입력 프롬프트
     * @param type   사용할 LLM 타입 (예: openai, gemini 등)
     * @return       LLM 응답 결과 (String)
     */
    @PostMapping("/multi_llm") // POST 방식으로 변경
    public String chatCompletion(@RequestParam String prompt,
                                 @RequestParam String type) {
        // 입력 로그 출력
        log.info("prompt={}, type={}", prompt, type);
        // Multi LLM 서비스 호출
        return multiLLMService.chat(prompt, type);
    }

    // ================================================================
    // 2️⃣ Chat Streaming (Flux 기반 실시간 응답)
    // ================================================================
    /**
     * 스트리밍 방식 Chat API
     *
     * - 응답을 한 번에 반환하지 않고
     * - 토큰 단위로 실시간 전송
     *
     * @param prompt 사용자 질문
     * @param type   모델 타입
     * @return       Flux<String> (Reactive Stream)
     */
    @PostMapping(value = "/multi_llm_stream",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public Flux<String> chatStream(@RequestParam String prompt,
                                   @RequestParam String type) {
        log.info("prompt={}, type={}", prompt, type);
        return multiLLMService.chatStream(prompt, type);
    }

    // ================================================================
    // 3️⃣ Fallback 전략 적용 API
    // ================================================================
    /**
     * Fallback 처리 API
     *
     * - 1차 LLM 실패 시
     * - 자동으로 2차 LLM 호출
     *
     * @param userPrompt 사용자 질문
     * @param type       기본 모델 타입
     * @return           LLM 응답
     */
    @PostMapping("/fallback")
    public String fallback(@RequestParam("prompt") String userPrompt,
                           @RequestParam("type") String type) {
        log.info("Fallback Prompt: {}", userPrompt);
        return fallbackService.chat(userPrompt, type);
    }

    // ================================================================
    // 4️⃣ LLM Router 적용 API
    // ================================================================
    /**
     * Router 기반 모델 선택 API
     *
     * - 질문을 분석
     * - 가장 적합한 모델 자동 선택
     *
     * @param userPrompt 사용자 질문
     * @return LLM 응답
     */
    @PostMapping("/router")
    public String router(@RequestParam("prompt") String userPrompt) {
        log.info("Router Prompt: {}", userPrompt);
        return routerService.routeAndAsk(userPrompt);
    }


    // ================================================================
    // 5️⃣ Security 적용 API
    // ================================================================
    /**
     * 보안 필터링 적용 API
     *
     * 1️⃣ 민감정보 마스킹
     * 2️⃣ 프롬프트 보안 검사
     * 3️⃣ 정책 기반 요청 처리
     *
     * @param userPrompt 사용자 입력
     * @return 보안 처리 후 LLM 응답
     */
    @PostMapping("/security")
    public String security(@RequestParam("prompt") String userPrompt) {
        // 로그에는 마스킹된 정보만 남김
        log.info("Controller (Masked): {}",
                securityScanner.maskSensitive(userPrompt));
        return securityService.processRequest(userPrompt);
    }

}

