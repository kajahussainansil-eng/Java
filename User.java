package model;

import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userId;
    private String username;
    private String passwordHash;
    private String salt;
    private String fullName;
    private Role role;
    private UserStatus status;
    private boolean mustChangePassword;
    private String createdAt;
    private String lastLogin;

    public User() {}

    public User(String userId, String username, String passwordHash, String salt,
                String fullName, Role role, UserStatus status, boolean mustChangePassword,
                String createdAt, String lastLogin) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.fullName = fullName;
        this.role = role != null ? role : Role.STUDENT;
        this.status = status != null ? status : UserStatus.ACTIVE;
        this.mustChangePassword = mustChangePassword;
        this.createdAt = createdAt != null ? createdAt : "";
        this.lastLogin = lastLogin != null ? lastLogin : "";
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public boolean isMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getLastLogin() { return lastLogin; }
    public void setLastLogin(String lastLogin) { this.lastLogin = lastLogin; }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    @Override
    public String toString() {
        return fullName + " (" + userId + " - " + role.getDisplayName() + ")";
    }
}
