package com.ai.shared.util;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * SecurityScanner
 *
 * 애플리케이션 내에서 전달되는 텍스트 데이터에 대해
 * 민감 정보(전화번호, 이메일, 주민등록번호, 기밀 키워드 등)를 탐지하고
 * 필요 시 마스킹 처리하는 보안 유틸 컴포넌트.
 *
 * Spring Bean으로 등록되어 서비스 계층에서 주입 받아 사용 가능.
 */
@Component
public class SecurityScanner {

    /**
     * PHONE_PATTERN
     *
     * 대한민국 전화번호 형식을 탐지하기 위한 정규식 패턴.
     *
     * - 010, 011, 016~019 (휴대폰)
     * - 02 (서울 지역번호)
     * - 03~09로 시작하는 기타 지역번호
     * - 하이픈(-), 점(.), 공백 허용
     * - 3~4자리 + 4자리 구성
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "\\b(01[016789]|02|0[3-9][0-9])[-.\\s]?(\\d{3,4})[-.\\s]?(\\d{4})\\b"
    );

    /**
     * EMAIL_PATTERN
     *
     * 일반적인 이메일 주소 형식을 탐지하기 위한 정규식 패턴.
     *
     * - 영문/숫자/._%+- 허용
     * - @ 기호 포함
     * - 도메인 + 최상위 도메인(2~6자)
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-z]{2,6}"
    );

    /**
     * SSN_PATTERN
     *
     * 대한민국 주민등록번호 형식 탐지 패턴.
     *
     * - 앞 6자리(생년월일)
     * - 하이픈(-)
     * - 뒤 7자리
     */
    private static final Pattern SSN_PATTERN = Pattern.compile(
            "\\b\\d{6}-\\d{7}\\b"
    );

    /**
     * CONFIDENTIAL_KEYWORDS
     *
     * 텍스트 내 포함 시 민감 정보로 판단할 키워드 목록.
     * (정책에 따라 확장 가능)
     */
    private static final List<String> CONFIDENTIAL_KEYWORDS = List.of(
            "대외비", "기밀", "Confidential", "설계도"
    );

    /**
     * isSensitive
     *
     * 2️⃣ 민감 정보 탐지 메서드
     *
     * 전달된 텍스트에 대해 아래 항목을 검사:
     *  - 전화번호 존재 여부
     *  - 이메일 존재 여부
     *  - 주민등록번호 존재 여부
     *  - 기밀 키워드 포함 여부
     *
     * @param text 검사 대상 문자열
     * @return 민감 정보가 하나라도 포함되면 true
     */
    public boolean isSensitive(String text) {

        // 전화번호 패턴 탐지
        boolean hasPhone = PHONE_PATTERN.matcher(text).find();

        // 이메일 패턴 탐지
        boolean hasEmail = EMAIL_PATTERN.matcher(text).find();

        // 주민등록번호 패턴 탐지
        boolean hasSsn = SSN_PATTERN.matcher(text).find();

        // 기밀 키워드 포함 여부
        boolean hasKeyword = containsKeyword(text);

        // 하나라도 존재하면 민감 정보로 판단
        return hasPhone || hasEmail || hasSsn || hasKeyword;
    }

    /**
     * containsKeyword
     *
     * 2️⃣-1 키워드 기반 민감 정보 탐지
     *
     * 사전에 정의된 기밀 키워드가
     * 텍스트에 포함되어 있는지 확인.
     *
     * @param text 검사 대상 문자열
     * @return 키워드가 하나라도 포함되면 true
     */
    private boolean containsKeyword(String text) {
        return CONFIDENTIAL_KEYWORDS
                .stream()
                .anyMatch(text::contains);
    }

    /**
     * maskSensitive
     *
     * 3️⃣ 민감 정보 마스킹 처리 (로그 출력용)
     *
     * 로그에 민감 정보가 그대로 남지 않도록
     * 탐지된 패턴을 마스킹 문자열로 치환.
     *
     * - 전화번호 → XXX-XXXX-XXXX
     * - 이메일 → ***@***.***
     * - 주민등록번호 → ******-*******
     *
     * ※ 실제 서비스에서는 부분 마스킹 전략을 적용하는 것이 더 안전함.
     *
     * @param text 마스킹할 원본 문자열
     * @return 민감 정보가 마스킹된 문자열
     */
    public String maskSensitive(String text) {

        // 1️⃣ 전화번호 마스킹
        String masked = PHONE_PATTERN
                .matcher(text)
                .replaceAll("XXX-XXXX-XXXX");

        // 2️⃣ 이메일 마스킹
        masked = EMAIL_PATTERN
                .matcher(masked)
                .replaceAll("***@***.***");

        // 3️⃣ 주민등록번호 마스킹
        masked = SSN_PATTERN
                .matcher(masked)
                .replaceAll("******-*******");

        return masked;
    }
}
