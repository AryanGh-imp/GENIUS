package models.account;

import java.util.ArrayList;
import java.util.List;

public abstract class Account {
    protected String email;
    protected String nickName;
    protected String password;

    public Account(String email, String nickName, String password) {
        this.email = email;
        this.nickName = nickName;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public String getNickName() {
        return nickName;
    }

    public String getPassword() {
        return password;
    }

    public abstract String getRole();

    public List<String> toFileString() {
        List<String> data = new ArrayList<>();
        data.add("Email: " + email);
        data.add("Nickname: " + nickName);
        data.add("Password: " + password);
        data.add("Role: " + getRole());
        return data;
    }

    @Override
    public String toString() {
        return "Username: " + nickName + ", Role: " + getRole();
    }
}
