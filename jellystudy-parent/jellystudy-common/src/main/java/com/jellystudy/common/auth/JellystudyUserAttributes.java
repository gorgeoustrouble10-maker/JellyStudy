package com.jellystudy.common.auth;

/**
 * 鉴权成功后写入 {@link jakarta.servlet.http.HttpServletRequest} 的属性名。
 */
public final class JellystudyUserAttributes {

    public static final String USER_ID = "jellystudyUserId";

    private JellystudyUserAttributes() {
    }
}
