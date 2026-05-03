package online.dbarenholz.omiyage.dto;

/**
 * Login request body: provide the Mullvad-style account number shown at signup.
 * The number can be supplied with or without dashes (e.g. "1234-5678-9012-3456" or "1234567890123456").
 */
public record LoginRequest(String accountNumber) {
}
