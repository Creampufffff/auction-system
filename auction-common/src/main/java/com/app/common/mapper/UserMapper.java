package com.app.common.mapper;

import com.app.common.dto.*;
import com.app.common.entity.*;

public class UserMapper {

    public static LoginResponseDTO toLoginResponse(User user) {
        if (user == null) return null;
        
        return new LoginResponseDTO(
            user.getId(),
            user.getUsername(),
            user.getClass().getSimpleName(),
            user.getBalance()
        );
    }

    public static User toEntity(RegisterRequestDTO dto) {
        if (dto == null) return null;

        return new Bidder(dto.getUsername(), dto.getPassword(), dto.getEmail());
    }

    public static Seller toSeller(RegisterRequestDTO dto) {
        if (dto == null) return null;
        return new Seller(dto.getUsername(), dto.getPassword(), dto.getEmail());
    }

    public static Bidder toBidder(RegisterRequestDTO dto) {
        if (dto == null) return null;
        return new Bidder(dto.getUsername(), dto.getPassword(), dto.getEmail());
    }
    public static BalanceResponseDTO toBalanceResponse(User user) {
        if (user == null) return null;
        
        return new BalanceResponseDTO(
            user.getId(),
            user.getBalance(),
            "Số dư hiện tại của " + user.getUsername()
        );
    }

    public static LoginResponseDTO toUserDetails(User user) {
        return toLoginResponse(user);
    }
}
