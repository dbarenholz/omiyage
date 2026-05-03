package online.dbarenholz.omiyage.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "wishes")
@Getter
@Setter
@NoArgsConstructor
public class Wish {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "list_id", nullable = false)
    private WishList list;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "approximate_price", precision = 10, scale = 2)
    private BigDecimal approximatePrice;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @BatchSize(size = 50)
    @OneToMany(mappedBy = "wish", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WishLink> links = new ArrayList<>();

    @BatchSize(size = 50)
    @OneToMany(mappedBy = "wish", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WishTag> tags = new ArrayList<>();

    @OneToOne(mappedBy = "wish", cascade = CascadeType.ALL, orphanRemoval = true)
    private WishClaim claim;
}
