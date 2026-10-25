package web;

import model.User;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active web session tokens with TTL and user lookups.
 */
public class SessionManager {

    private static SessionManager instance;

    public static class Session {
        public final String token;
        public final User user;
        public long lastAccessed;

        public Session(String token, User user) {
            this.token = token;
            this.user = user;
            this.lastAccessed = System.currentTimeMillis();
        }
    }

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private static final long SESSION_TIMEOUT_MS = 24 * 60 * 60 * 1000L; // 24 hours

    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public synchronized String createSession(User user) {
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        StringBuilder sb = new StringBuilder("cfs_");
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        String token = sb.toString();
        sessions.put(token, new Session(token, user));
        cleanExpiredSessions();
        return token;
    }

    public synchronized User getUserByToken(String token) {
        if (token == null) return null;
        Session session = sessions.get(token);
        if (session == null) return null;

        if (System.currentTimeMillis() - session.lastAccessed > SESSION_TIMEOUT_MS) {
            sessions.remove(token);
            return null;
        }

        session.lastAccessed = System.currentTimeMillis();
        return session.user;
    }

    public synchronized void invalidateToken(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    private void cleanExpiredSessions() {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(e -> (now - e.getValue().lastAccessed) > SESSION_TIMEOUT_MS);
    }
}
