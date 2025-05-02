package models.account;

import static services.AccountManager.validateEmail;
import static services.AccountManager.validatePassword;
import static services.AccountManager.validateNickName;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public abstract class Account {

    private final String email;
    private String nickName;
    private String password;


    protected Account(String email, String nickName, String password) {
        email = email != null ? email.toLowerCase() : null;
        nickName = nickName != null ? nickName.toLowerCase() : null;
        validateParameters(email, nickName, password);
        this.email = email;
        this.nickName = nickName;
        this.password = password;
    }

    private void validateParameters(String email, String nickName, String password) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (!validateEmail(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (nickName == null || nickName.trim().isEmpty()) {
            throw new IllegalArgumentException("Nickname cannot be null or empty");
        }
        if (!validateNickName(nickName)) {
            throw new IllegalArgumentException("Nickname must be 3-20 characters long and contain only letters, numbers, underscores, or hyphens");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        if (!validatePassword(password)) {
            throw new IllegalArgumentException("Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one number, and one special character");
        }
    }

    public String getEmail() {
        return email;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        nickName = nickName != null ? nickName.toLowerCase() : null;
        if (nickName == null || nickName.trim().isEmpty()) {
            throw new IllegalArgumentException("Nickname cannot be null or empty");
        }
        if (!validateNickName(nickName)) {
            throw new IllegalArgumentException("Nickname must be 3-20 characters long and contain only letters, numbers, underscores, or hyphens");
        }
        this.nickName = nickName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        if (!validatePassword(password)) {
            throw new IllegalArgumentException("Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one number, and one special character");
        }
        this.password = password;
    }


    public abstract String getRole();

    public final List<String> toFileString() {
        List<String> data = new ArrayList<>();
        data.add("Email: " + email);
        data.add("Nickname: " + nickName);
        data.add("Password: " + password);
        data.add("Role: " + getRole());
        addAdditionalData(data);
        return data;
    }

    protected void addAdditionalData(List<String> data) {
        // Subclasses can override this to add their specific data
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account account)) return false;
        return Objects.equals(email, account.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }

    @Override
    public String toString() {
        return getRole() + ": " + nickName + " (" + email + ")";
    }
}