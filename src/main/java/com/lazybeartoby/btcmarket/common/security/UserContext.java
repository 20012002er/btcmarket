package com.lazybeartoby.btcmarket.common.security;

public final class UserContext {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private UserContext() {}

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static CurrentUser require() {
        CurrentUser u = HOLDER.get();
        if (u == null) {
            throw new IllegalStateException("未登录");
        }
        return u;
    }

    public static void clear() {
        HOLDER.remove();
    }

    public record CurrentUser(Long userId, String username, String role) {}
}
