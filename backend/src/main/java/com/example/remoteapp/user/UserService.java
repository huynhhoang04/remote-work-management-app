package com.example.remoteapp.user;

import com.example.remoteapp.common.AppException;
import com.example.remoteapp.common.ErrorCode;
import com.example.remoteapp.user.dto.RegisterDeviceRequest;
import com.example.remoteapp.user.dto.UpdateProfileRequest;
import com.example.remoteapp.user.dto.UserResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiệp vụ hồ sơ người dùng + đăng ký thiết bị nhận push (plan 03 §A4, §A5).
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A4, §A5
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;

    /**
     * Khởi tạo service (constructor injection tường minh).
     *
     * @param userRepository   repository người dùng
     * @param deviceRepository repository thiết bị FCM
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1
     */
    public UserService(UserRepository userRepository, DeviceRepository deviceRepository) {
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
    }

    /**
     * Xem hồ sơ người dùng hiện tại (FR-A04).
     *
     * @param userId người dùng đang đăng nhập
     * @return hồ sơ DTO
     * @throws AppException USER_NOT_FOUND nếu không tìm thấy
     * @reference docs/plan-backend/02-danh-sach-api.md §3
     */
    @Transactional(readOnly = true)
    public UserResponse getProfile(UUID userId) {
        return UserResponse.from(findUser(userId));
    }

    /**
     * Cập nhật hồ sơ — chỉ đổi trường được gửi lên (FR-A04).
     *
     * @param userId  người dùng đang đăng nhập
     * @param request các trường cần sửa (null = giữ nguyên)
     * @return hồ sơ sau khi cập nhật
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A4
     */
    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName().trim());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }
        userRepository.save(user);
        return UserResponse.from(user);
    }

    /**
     * Đăng ký FCM token thiết bị — upsert theo token (mỗi token thuộc 1 user).
     *
     * @param userId  chủ thiết bị
     * @param request FCM token + nền tảng
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A5
     */
    @Transactional
    public void registerDevice(UUID userId, RegisterDeviceRequest request) {
        Device device = deviceRepository.findByFcmToken(request.fcmToken())
                .orElseGet(Device::new);
        device.setUserId(userId);
        device.setFcmToken(request.fcmToken());
        device.setPlatform(request.platform().trim().toUpperCase());
        device.setLastActiveAt(Instant.now());
        deviceRepository.save(device);
    }

    /**
     * Huỷ đăng ký FCM token (khi logout / token hỏng).
     *
     * @param userId   người dùng yêu cầu
     * @param fcmToken token cần huỷ
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A5
     */
    @Transactional
    public void removeDevice(UUID userId, String fcmToken) {
        deviceRepository.findByFcmToken(fcmToken)
                .filter(device -> device.getUserId().equals(userId))
                .ifPresent(deviceRepository::delete);
    }

    /**
     * Tìm user theo id hoặc ném lỗi thống nhất.
     *
     * @param userId id người dùng
     * @return entity {@link User}
     * @throws AppException USER_NOT_FOUND nếu không tồn tại
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.3
     */
    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng"));
    }
}
