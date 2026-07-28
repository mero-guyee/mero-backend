package io.mero.app.global.exception;

/** 외부 서비스 호출 실패. 클라이언트 요청이 아니라 우리 쪽/외부 인프라 문제일 때 사용한다. */
public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
