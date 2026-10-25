package security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

/**
 * Robust salted password hashing using PBKDF2 with HMAC-SHA256.
 * Implemented exclusively using standard Java Cryptography Architecture (JCA).
 */
public final class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 12000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {}

    /**
     * Generates a cryptographically strong random salt encoded in hex.
     */
    public static String generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return toHex(salt);
    }

    /**
     * Hashes a plain password with a given hex-encoded salt.
     */
    public static String hashPassword(String password, String saltHex) {
        if (password == null) password = "";
        byte[] salt = fromHex(saltHex);
        try {
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return toHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // Fallback to SHA-256 if PBKDF2 algorithm is not supported
            return fallbackSha256(password + saltHex);
        }
    }

    /**
     * Verifies that the provided plain password matches the stored hash.
     */
    public static boolean verifyPassword(String password, String saltHex, String expectedHashHex) {
        if (password == null || saltHex == null || expectedHashHex == null) return false;
        String computed = hashPassword(password, saltHex);
        return slowEquals(computed, expectedHashHex);
    }

    /**
     * Constant-time comparison to prevent timing attacks.
     */
    private static boolean slowEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] aBytes = a.getBytes();
        byte[] bBytes = b.getBytes();
        int diff = aBytes.length ^ bBytes.length;
        for (int i = 0; i < aBytes.length && i < bBytes.length; i++) {
            diff |= aBytes[i] ^ bBytes[i];
        }
        return diff == 0;
    }

    private static String fallbackSha256(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return toHex(b);
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }

    public static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    public static byte[] fromHex(String hex) {
        if (hex == null || hex.length() % 2 != 0) {
            return new byte[SALT_BYTES];
        }
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
