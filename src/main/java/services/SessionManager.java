package services;

import models.account.Account;

public class SessionManager {
    private static Account currentAccount;

    // Convert to Singleton
    private static final SessionManager INSTANCE = new SessionManager();

    private SessionManager() {
        // Prevent new instances from being created
    }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public void setCurrentAccount(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null");
        }
        if (account.getEmail() == null || account.getEmail().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (account.getNickName() == null || account.getNickName().isEmpty()) {
            throw new IllegalArgumentException("Nickname cannot be null or empty");
        }
        if (account.getRole() == null || account.getRole().isEmpty()) {
            throw new IllegalArgumentException("Role cannot be null or empty");
        }
        currentAccount = account;
    }

    public Account getCurrentAccount() {
        if (currentAccount == null) {
            throw new IllegalStateException("No user is currently logged in.");
        }
        return currentAccount;
    }

    public void clearSession() {
        currentAccount = null;
    }

    public boolean isLoggedIn() {
        return currentAccount != null;
    }

    public boolean isAdmin() {
        return isLoggedIn() && "admin".equalsIgnoreCase(currentAccount.getRole());
    }

    public boolean isArtist() {
        return isLoggedIn() && "artist".equalsIgnoreCase(currentAccount.getRole());
    }

    public boolean isUser() {
        return isLoggedIn() && "user".equalsIgnoreCase(currentAccount.getRole());
    }
}