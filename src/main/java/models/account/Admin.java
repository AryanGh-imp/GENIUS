package models.account;

import services.file.AdminFileManager;

import java.util.List;


public class Admin extends Account {
    private AdminFileManager adminFileManager;

    public Admin(String email, String nickName, String password) {
        super(email, nickName, password);
    }

    public void setAdminFileManager(AdminFileManager adminFileManager) {
        if (adminFileManager == null) {
            throw new IllegalArgumentException("AdminFileManager cannot be null");
        }
        this.adminFileManager = adminFileManager;
    }

    public AdminFileManager getAdminFileManager() {
        return adminFileManager;
    }

    @Override
    public final String getRole() {
        return "Admin";
    }

    public String[][] getPendingArtistRequests() {
        checkAdminFileManager();
        List<String[]> requests = adminFileManager.loadPendingArtistRequests();
        return requests.toArray(new String[0][]);
    }

    public String[][] getApprovedArtistRequests() {
        checkAdminFileManager();
        List<String[]> requests = adminFileManager.loadApprovedArtistRequests();
        return requests.toArray(new String[0][]);
    }

    public String[][] getRejectedArtistRequests() {
        checkAdminFileManager();
        List<String[]> requests = adminFileManager.loadRejectedArtistRequests();
        return requests.toArray(new String[0][]);
    }

    public void approveArtist(String email, String nickName) {
        checkAdminFileManager();
        adminFileManager.approveArtistRequest(email, nickName);
    }

    public void rejectArtist(String email, String nickName) {
        checkAdminFileManager();
        adminFileManager.rejectArtistRequest(email, nickName);
    }

    private void checkAdminFileManager() {
        if (adminFileManager == null) {
            throw new IllegalStateException("AdminFileManager is not set for this Admin instance.");
        }
    }
}