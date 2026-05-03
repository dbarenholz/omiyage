package online.dbarenholz.omiyage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    /** Internal primary key — never exposed to the user. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Mullvad-style 16-digit account number (range 1000000000000000–9999999999999999).
     * This is the credential the user receives at signup and uses to log in.
     * Displayed as XXXX-XXXX-XXXX-XXXX.
     */
    @Column(name = "account_number", nullable = false, unique = true)
    private Long accountNumber;

    @Column(name = "display_name")
    private String displayName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Set on every successful login; null means the account has never been used. */
    @Column(name = "last_login_at")
    private Instant lastLoginAt;
}
