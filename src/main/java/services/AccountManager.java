package services;

import models.account.Account;
import models.account.User;
import models.account.Artist;
import models.account.Admin;
import services.file.AdminFileManager;
import services.file.ArtistFileManager;
import services.file.UserFileManager;

import java.util.List;
import java.util.regex.Pattern;

public class AccountManager {
    private static final AdminFileManager adminFileManager = new AdminFileManager();
    private static final ArtistFileManager artistFileManager = new ArtistFileManager();
    private static final UserFileManager userFileManager = new UserFileManager();

    public enum Role {
        USER, ARTIST, ADMIN
    }

    public static void registerUser(String email, String nickName, String password, String role) {
        email = email != null ? email.toLowerCase() : null;
        nickName = nickName != null ? nickName.toLowerCase() : null;

        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (nickName == null || nickName.trim().isEmpty()) {
            throw new IllegalArgumentException("Nickname cannot be null or empty");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Role cannot be null or empty");
        }

        if (!validateEmail(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (!validateNickName(nickName)) {
            throw new IllegalArgumentException("Nickname must be 3-20 characters long and contain only letters, numbers, underscores, or hyphens");
        }
        if (!validatePassword(password)) {
            throw new IllegalArgumentException("Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one number, and one special character");
        }

        if (userFileManager.isEmailOrNickNameTaken(email, nickName) ||
                adminFileManager.isEmailOrNickNameTaken(email, nickName) ||
                artistFileManager.isEmailOrNickNameTaken(email, nickName)) {
            throw new IllegalStateException("This email or nickname is already registered");
        }

        Role accountRole;
        try {
            accountRole = Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }

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

    public static Account findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        username = username.toLowerCase();
        try {
            Account account = userFileManager.loadAccountByNickName(username);
            if (account != null) {
                return account;
            }
        } catch (IllegalStateException e) {
            // Continue searching in other managers
        }
        try {
            Account account = adminFileManager.loadAccountByNickName(username);
            if (account != null) {
                return account;
            }
        } catch (IllegalStateException e) {
            // Continue searching in other managers
        }
        try {
            Account account = artistFileManager.loadAccountByNickName(username);
            if (account != null) {
                return account;
            }
        } catch (IllegalStateException e) {
            // Continue searching in other managers
        }
        throw new IllegalStateException("Account with username " + username + " not found");
    }

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

    public static Account login(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        username = username.toLowerCase();
        Account account = findByUsername(username);

        if (!account.getPassword().equals(password)) {
            throw new IllegalStateException("Incorrect password");
        }

        if (account instanceof Artist artist) {
            if (!artist.isApproved()) {
                throw new IllegalStateException("Your artist account is not yet approved by the admin");
            }
        }

        return account;
    }

    public static boolean validateEmail(String email) {
        String regex = "^[a-zA-Z0-9]+([._+-][a-zA-Z0-9]+)*@[a-zA-Z0-9-]+(\\.[a-zA-Z]{2,})+$";
        return Pattern.matches(regex, email);
    }

    private static boolean validateNickName(String nickName) {
        String regex = "^[a-zA-Z0-9_-]+$";
        return nickName.length() >= 3 && nickName.length() <= 20 && Pattern.matches(regex, nickName);
    }

    private static boolean validatePassword(String password) {
        String regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        return Pattern.matches(regex, password);
    }
}