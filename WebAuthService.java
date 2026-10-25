package service;

import model.Role;
import model.User;
import model.UserStatus;
import security.PasswordHasher;
import storage.WebDataManager;
import util.DateUtils;
import util.ValidationUtils;

import java.io.IOException;

/**
 * Authentication and session authorization service for Web API.
 */
public class WebAuthService {

    private final WebDataManager dataManager;

    public WebAuthService(WebDataManager dataManager) {
        this.dataManager = dataManager;
    }

    public synchronized User login(String usernameOrId, String password) throws Exception {
        if (!ValidationUtils.isNotEmpty(usernameOrId) || !ValidationUtils.isNotEmpty(password)) {
            throw new IllegalArgumentException("Username/ID and password are required.");
        }

        String query = usernameOrId.trim();
        User user = null;

        for (User u : dataManager.getUsers().values()) {
            if (u.getUsername().equalsIgnoreCase(query) || u.getUserId().equalsIgnoreCase(query)) {
                user = u;
                break;
            }
        }

        if (user == null) {
            throw new SecurityException("Invalid credentials. Account not found.");
        }

        if (!user.isActive()) {
            throw new SecurityException("This account is currently inactive. Contact your campus administrator.");
        }

        boolean match = PasswordHasher.verifyPassword(password, user.getSalt(), user.getPasswordHash());
        if (!match) {
            dataManager.logAudit(user.getUserId(), "LOGIN_FAILED", "Failed web login attempt");
            throw new SecurityException("Invalid credentials. Incorrect password.");
        }

        user.setLastLogin(DateUtils.nowString());
        try {
            dataManager.saveUsers();
        } catch (IOException ignored) {}

        dataManager.logAudit(user.getUserId(), "LOGIN_SUCCESS", "Web user logged in successfully");
        return user;
    }

    public synchronized void changePassword(String userId, String oldPassword, String newPassword) throws Exception {
        User user = dataManager.getUsers().get(userId);
        if (user == null) throw new IllegalArgumentException("User not found.");

        if (!PasswordHasher.verifyPassword(oldPassword, user.getSalt(), user.getPasswordHash())) {
            throw new SecurityException("Current password does not match.");
        }

        if (!ValidationUtils.isValidPassword(newPassword)) {
            throw new IllegalArgumentException("New password must be at least 6 characters.");
        }

        String newSalt = PasswordHasher.generateSalt();
        String newHash = PasswordHasher.hashPassword(newPassword, newSalt);

        user.setSalt(newSalt);
        user.setPasswordHash(newHash);
        user.setMustChangePassword(false);

        dataManager.saveUsers();
        dataManager.logAudit(userId, "PASSWORD_CHANGED", "User changed password via web.");
    }

    public synchronized void resetPasswordByAdmin(String adminUserId, String targetUserId, String newTempPassword) throws Exception {
        User admin = dataManager.getUsers().get(adminUserId);
        if (admin == null || admin.getRole() != Role.ADMIN) {
            throw new SecurityException("Admin authorization required.");
        }

        User target = dataManager.getUsers().get(targetUserId);
        if (target == null) throw new IllegalArgumentException("Target user not found.");

        if (!ValidationUtils.isValidPassword(newTempPassword)) {
            throw new IllegalArgumentException("Temporary password must be at least 6 characters.");
        }

        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hashPassword(newTempPassword, salt);

        target.setSalt(salt);
        target.setPasswordHash(hash);
        target.setMustChangePassword(true);

        dataManager.saveUsers();
        dataManager.logAudit(adminUserId, "PASSWORD_RESET", "Admin reset password for " + targetUserId);
    }
}
