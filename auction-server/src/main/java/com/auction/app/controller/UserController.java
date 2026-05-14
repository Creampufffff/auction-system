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
            return new RegisterResponseDTO(true, "Registration successful", user.getId());
        } catch (Exception e) {
            return new RegisterResponseDTO(false, "Error registering: " + e.getMessage(), null);
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
            return new BalanceResponseDTO(userId, 0, "User does not exist");
        }
        return UserMapper.toBalanceResponse(user);
    }

    // ✅ Dùng DTO Request/Response
    public ApiResponseDTO deposit(DepositRequestDTO request) {
        try {
            if (request.getAmount() <= 0) {
                return new ApiResponseDTO(false, "Amount must be greater than 0");
            }
            userService.deposit(request.getUserId(), request.getAmount());
            return new ApiResponseDTO(true, "Deposit successful");
        } catch (Exception e) {
            return new ApiResponseDTO(false, "Error depositing: " + e.getMessage());
        }
    }

    // ✅ Dùng DTO Request/Response
    public ApiResponseDTO withdraw(WithdrawRequestDTO request) {
        try {
            if (request.getAmount() <= 0) {
                return new ApiResponseDTO(false, "Amount must be greater than 0");
            }
            userService.withdraw(request.getUserId(), request.getAmount());
            return new ApiResponseDTO(true, "Withdrawal successful");
        } catch (Exception e) {
            return new ApiResponseDTO(false, "Error withdrawing: " + e.getMessage());
        }
    }

    // ✅ Dùng DTO Response
    public ApiResponseDTO deleteUser(String userId) {
        try {
            userService.deleteUser(userId);
            return new ApiResponseDTO(true, "User deleted successfully");
        } catch (Exception e) {
            return new ApiResponseDTO(false, "Error deleting user: " + e.getMessage());
        }
    }
}


