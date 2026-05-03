package online.dbarenholz.omiyage.dto;

import java.time.Instant;

/**
 * Public representation of a user; never exposes the internal UUID.
 * {@code accountNumber} is formatted as XXXX-XXXX-XXXX-XXXX.
 */
public record UserResponse(String accountNumber, String displayName, Instant createdAt) {
}
