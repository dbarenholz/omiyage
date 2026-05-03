package online.dbarenholz.omiyage.repository;

import online.dbarenholz.omiyage.entity.WishClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WishClaimRepository extends JpaRepository<WishClaim, UUID> {

    Optional<WishClaim> findByWishId(UUID wishId);

    boolean existsByWishId(UUID wishId);
}
