package com.auction.shared.session;

import com.app.common.dto.LoginResponseDTO;
import lombok.Getter;
import lombok.Setter;

public class SessionManager {
    @Setter
    @Getter
    private static LoginResponseDTO currentUser;

    private SessionManager() {
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static void clear() {
        currentUser = null;
    }

    public static String getCurrentUserId() {
        return currentUser == null ? null : currentUser.getUserId();
    }

    public static String getCurrentUsername() {
        return currentUser == null ? null : currentUser.getUsername();
    }

    public static String getCurrentUserRole() {
        return currentUser == null ? null : currentUser.getRole();
    }

    public static double getCurrentUserBalance() {
        return currentUser == null ? 0.0 : currentUser.getBalance();
    }

    public static boolean hasRole(String role) {
        return role != null && role.equalsIgnoreCase(getCurrentUserRole());
    }

    public static void updateCurrentUserBalance(double balance){
        if (currentUser != null){
            currentUser.setBalance(balance);
        }
    }
}

