package com.app.common.mapper;

import com.app.common.dto.*;
import com.app.common.entity.*;

/**
 * UserMapper - Chuyển đổi giữa User Entity và các DTO liên quan
 */
public class UserMapper {

    /**
     * Chuyển User Entity thành LoginResponseDTO
     * Chỉ trả về các thông tin cần thiết, không trả về password
     */
    public static LoginResponseDTO toLoginResponse(User user) {
        if (user == null) return null;
        
        return new LoginResponseDTO(
            user.getId(),
            user.getUsername(),
            user.getClass().getSimpleName(), // SELLER, BIDDER, ADMIN
            user.getBalance()
        );
    }

    /**
     * Chuyển RegisterRequestDTO thành User Entity (Bidder)
     */
    public static User toEntity(RegisterRequestDTO dto) {
        if (dto == null) return null;
        
        // Mặc định tạo Bidder, có thể mở rộng để chọn type
        return new Bidder(dto.getUsername(), dto.getPassword(), dto.getEmail());
    }

    /**
     * Chuyển RegisterRequestDTO thành Seller Entity
     */
    public static Seller toSeller(RegisterRequestDTO dto) {
        if (dto == null) return null;
        return new Seller(dto.getUsername(), dto.getPassword(), dto.getEmail());
    }

    /**
     * Chuyển RegisterRequestDTO thành Bidder Entity
     */
    public static Bidder toBidder(RegisterRequestDTO dto) {
        if (dto == null) return null;
        return new Bidder(dto.getUsername(), dto.getPassword(), dto.getEmail());
    }

    /**
     * Chuyển User Entity sang BalanceResponseDTO
     */
    public static BalanceResponseDTO toBalanceResponse(User user) {
        if (user == null) return null;
        
        return new BalanceResponseDTO(
            user.getId(),
            user.getBalance(),
            "Số dư hiện tại của " + user.getUsername()
        );
    }

    /**
     * Chuyển User Entity sang UserDetailsDTO (thông tin chi tiết người dùng)
     */
    public static LoginResponseDTO toUserDetails(User user) {
        return toLoginResponse(user);
    }
}

