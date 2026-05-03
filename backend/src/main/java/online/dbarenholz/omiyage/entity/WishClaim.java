package online.dbarenholz.omiyage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wish_claims")
@Getter
@Setter
@NoArgsConstructor
public class WishClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wish_id", nullable = false, unique = true)
    private Wish wish;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claimed_by_id", nullable = false)
    private User claimedBy;

    @CreationTimestamp
    @Column(name = "claimed_at", nullable = false, updatable = false)
    private Instant claimedAt;
}
