package com.example.remoteapp.chat.ws;

import java.security.Principal;

/**
 * Principal cho phiên STOMP — tên chính là userId để {@code /user/...} định tuyến đúng.
 *
 * @reference docs/plan-backend/02-danh-sach-api.md §12
 */
public class StompPrincipal implements Principal {

    private final String name;

    /**
     * Khởi tạo principal.
     *
     * @param name userId dạng chuỗi
     * @reference docs/plan-backend/02-danh-sach-api.md §12
     */
    public StompPrincipal(String name) {
        this.name = name;
    }

    /**
     * Tên principal (userId).
     *
     * @return userId dạng chuỗi
     * @reference docs/plan-backend/02-danh-sach-api.md §12
     */
    @Override
    public String getName() {
        return name;
    }
}
