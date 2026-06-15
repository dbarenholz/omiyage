package online.dbarenholz.omiyage.repository;

import online.dbarenholz.omiyage.entity.WishClaim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WishClaimRepository extends JpaRepository<WishClaim, UUID> {

    Optional<WishClaim> findByWishId(UUID wishId);

    boolean existsByWishId(UUID wishId);
}
