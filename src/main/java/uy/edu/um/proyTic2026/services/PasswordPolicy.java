package uy.edu.um.proyTic2026.services;

public final class PasswordPolicy {
    private static final String STRONG_PASSWORD = "^(?=.*\\p{Ll})(?=.*\\p{Lu})(?=.*\\d)(?=.*[\\p{P}\\p{S}]).{8,30}$";

    private PasswordPolicy() { }

    public static void requireStrong(String password) {
        if (password == null || !password.matches(STRONG_PASSWORD)) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener entre 8 y 30 caracteres, mayúscula, minúscula, número y símbolo.");
        }
    }
}
