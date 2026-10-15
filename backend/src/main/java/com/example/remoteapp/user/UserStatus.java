package com.example.remoteapp.user;

/**
 * Trạng thái tài khoản người dùng.
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A1
 */
public enum UserStatus {

    /** Tài khoản hoạt động bình thường. */
    ACTIVE,

    /** Tài khoản bị khoá (không đăng nhập được). */
    LOCKED
}
