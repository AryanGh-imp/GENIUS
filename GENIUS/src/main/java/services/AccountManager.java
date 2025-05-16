package services;

import models.account.Account;
import models.account.User;
import models.account.Admin;
import services.file.AdminFileManager;
import services.file.ArtistFileManager;
import services.file.UserFileManager;

import java.util.regex.Pattern;

public class AccountManager {
    private static final AdminFileManager adminFileManager = new AdminFileManager();
    private static final ArtistFileManager artistFileManager = new ArtistFileManager();
    private static final UserFileManager userFileManager = new UserFileManager();

    public enum Role {
        USER, ARTIST, ADMIN
    }

    public static void registerUser(String email, String nickName, String password, String role) {
        // Role validation
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Role cannot be null or empty");
        }

        // Checking the uniqueness of email and aliases
        if (userFileManager.isEmailOrNickNameTaken(email, nickName) ||
                adminFileManager.isEmailOrNickNameTaken(email, nickName) ||
                artistFileManager.isEmailOrNickNameTaken(email, nickName)) {
            throw new IllegalStateException("This email or nickname is already registered");
        }

        // Role reversal
        Role accountRole;
        try {
            accountRole = Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }

        // User registration (validation is done in the Account builder)
        if (accountRole == Role.ARTIST) {
            adminFileManager.saveArtistRequest(email, nickName, password);
        } else if (accountRole == Role.USER) {
            Account account = new User(email, nickName, password);
            userFileManager.saveAccount(account);
        } else if (accountRole == Role.ADMIN) {
            Account account = new Admin(email, nickName, password);
            adminFileManager.saveAccount(account);
        }
    }

    // TODO: This should be further investigated for optimization in the future.
    public static Account findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        username = username.toLowerCase();
        Account account = null;
        try {
            account = userFileManager.loadAccountByNickName(username);
            if (account != null) return account;
            account = adminFileManager.loadAccountByNickName(username);
            if (account != null) return account;
            account = artistFileManager.loadAccountByNickName(username);
            if (account != null) return account;
        } catch (IllegalStateException e) {
            System.err.println("Error loading account: " + e.getMessage());
        }
        throw new IllegalStateException("Account with username " + username + " not found");
    }

    // TODO: This should be further investigated for optimization in the future.
    public static Account findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }

        email = email.toLowerCase();
        String nickName = userFileManager.findNickNameByEmail(email, null);
        if (nickName == null) {
            nickName = adminFileManager.findNickNameByEmail(email, null);
        }
        if (nickName == null) {
            nickName = artistFileManager.findNickNameByEmail(email, null);
        }
        if (nickName == null) {
            return null;
        }
        return findByUsername(nickName);
    }

    public static boolean validateEmail(String email) {
        String regex = "^[a-zA-Z0-9]+([._+-][a-zA-Z0-9]+)*@[a-zA-Z0-9-]+(\\.[a-zA-Z]{2,})+$";
        return Pattern.matches(regex, email);
    }

    public static boolean validateNickName(String nickName) {
        String regex = "^[a-zA-Z0-9_-]+$";
        return nickName.length() >= 3 && nickName.length() <= 20 && Pattern.matches(regex, nickName);
    }

    public static boolean validatePassword(String password) {
        String regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        return Pattern.matches(regex, password);
    }
}