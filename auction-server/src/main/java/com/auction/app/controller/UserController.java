package com.auction.app.controller;

import com.app.common.dto.*;
import com.app.common.entity.User;
import com.app.common.mapper.UserMapper;
import com.auction.app.service.UserService;

public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ✅ Dùng DTO Request/Response
    public RegisterResponseDTO register(RegisterRequestDTO request) {
        try {
            User user = UserMapper.toEntity(request);
            userService.register(user);
            return new RegisterResponseDTO(true, "Đăng ký thành công", user.getId());
        } catch (Exception e) {
            return new RegisterResponseDTO(false, "Lỗi đăng ký: " + e.getMessage(), null);
        }
    }

    // ✅ Dùng DTO Request/Response
    public LoginResponseDTO login(LoginRequestDTO request) {
        User user = userService.login(request.getUsername(), request.getPassword());
        if (user == null) {
            return null;
        }
        return UserMapper.toLoginResponse(user);
    }

    // ✅ Dùng DTO Response
    public LoginResponseDTO getUserProfile(String userId) {
        User user = userService.getById(userId);
        return UserMapper.toUserDetails(user);
    }

    // ✅ Dùng DTO Response
    public BalanceResponseDTO getBalance(String userId) {
        User user = userService.getById(userId);
        if (user == null) {
            return new BalanceResponseDTO(userId, 0, "Người dùng không tồn tại");
        }
        return UserMapper.toBalanceResponse(user);
    }

    // ✅ Dùng DTO Request/Response
    public ApiResponseDTO deposit(DepositRequestDTO request) {
        try {
            if (request.getAmount() <= 0) {
                return new ApiResponseDTO(false, "Số tiền phải lớn hơn 0");
            }
            userService.deposit(request.getUserId(), request.getAmount());
            return new ApiResponseDTO(true, "Nạp tiền thành công");
        } catch (Exception e) {
            return new ApiResponseDTO(false, "Lỗi nạp tiền: " + e.getMessage());
        }
    }

    // ✅ Dùng DTO Request/Response
    public ApiResponseDTO withdraw(WithdrawRequestDTO request) {
        try {
            if (request.getAmount() <= 0) {
                return new ApiResponseDTO(false, "Số tiền phải lớn hơn 0");
            }
            userService.withdraw(request.getUserId(), request.getAmount());
            return new ApiResponseDTO(true, "Rút tiền thành công");
        } catch (Exception e) {
            return new ApiResponseDTO(false, "Lỗi rút tiền: " + e.getMessage());
        }
    }

    // ✅ Dùng DTO Response
    public ApiResponseDTO deleteUser(String userId) {
        try {
            userService.deleteUser(userId);
            return new ApiResponseDTO(true, "Xóa người dùng thành công");
        } catch (Exception e) {
            return new ApiResponseDTO(false, "Lỗi xóa người dùng: " + e.getMessage());
        }
    }
}


