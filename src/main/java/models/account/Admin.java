package models.account;

/**
 * Represents an Admin account in the system, extending the base Account class.
 * An Admin can manage artist requests and lyrics edit requests.
 */
public class Admin extends Account {

    /**
     * Constructs a new Admin with the specified email, nickname, and password.
     *
     * @param email    The admin's email address.
     * @param nickName The admin's nickname.
     * @param password The admin's password.
     */
    public Admin(String email, String nickName, String password) {
        super(email, nickName, password);
    }

    /**
     * Gets the role of the account.
     *
     * @return The role "Admin".
     */
    @Override
    public final String getRole() {
        return "Admin";
    }
}