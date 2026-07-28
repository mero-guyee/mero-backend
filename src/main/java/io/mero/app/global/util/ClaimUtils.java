package io.mero.app.global.util;

import io.jsonwebtoken.Claims;

/**
 * ID 토큰의 클레임을 타입에 관대하게 읽는다.
 *
 * <p>제공자마다 같은 클레임을 다른 타입으로 내려준다. Apple은 email_verified를 문자열 "true"로
 * 주기도 하고, Google도 발급 경로에 따라 문자열일 수 있다. {@code claims.get(name, Boolean.class)}로
 * 받으면 이때 RequiredTypeException이 나면서 정상 토큰의 로그인이 통째로 실패한다.
 */
public final class ClaimUtils {

    private ClaimUtils() {
    }

    /** boolean 클레임을 읽는다. boolean과 문자열 "true"를 모두 참으로 본다. 없거나 해석 불가면 false. */
    public static boolean readBoolean(Claims claims, String name) {
        Object value = claims.get(name);
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return value instanceof String stringValue && Boolean.parseBoolean(stringValue);
    }

    /** 문자열 클레임을 읽는다. 없거나 문자열이 아니면 null. */
    public static String readString(Claims claims, String name) {
        Object value = claims.get(name);
        return value instanceof String stringValue ? stringValue : null;
    }
}
