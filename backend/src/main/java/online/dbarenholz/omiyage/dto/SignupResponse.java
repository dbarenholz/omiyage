package online.dbarenholz.omiyage.dto;

/**
 * Returned after successful signup.
 * {@code accountNumber} is the user-facing credential formatted as XXXX-XXXX-XXXX-XXXX.
 * The user must save this; it cannot be recovered.
 */
public record SignupResponse(String accountNumber, String displayName) {
}
