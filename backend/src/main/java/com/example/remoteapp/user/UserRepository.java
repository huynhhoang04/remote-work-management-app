package com.example.remoteapp.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository cho {@link User}.
 *
 * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Tìm user theo email (không phân biệt hoa/thường).
     *
     * @param email email cần tìm
     * @return user nếu tồn tại
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Kiểm tra email đã được đăng ký chưa.
     *
     * @param email email cần kiểm tra
     * @return true nếu đã tồn tại
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A1
     */
    boolean existsByEmailIgnoreCase(String email);
}
