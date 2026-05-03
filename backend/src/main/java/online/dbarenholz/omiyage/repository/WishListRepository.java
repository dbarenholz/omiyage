package online.dbarenholz.omiyage.repository;

import online.dbarenholz.omiyage.entity.WishList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WishListRepository extends JpaRepository<WishList, UUID> {

    List<WishList> findByOwnerId(UUID ownerId);

    Optional<WishList> findByShareId(UUID shareId);

    /**
     * Loads a list and eagerly fetches its wishes in a single query.
     * Wish child collections (links, tags, claim) are loaded lazily with @BatchSize.
     */
    @Query("SELECT wl FROM WishList wl LEFT JOIN FETCH wl.wishes WHERE wl.id = :id")
    Optional<WishList> findByIdWithWishes(UUID id);

    /**
     * Loads a list by its public share ID and eagerly fetches its wishes.
     */
    @Query("SELECT wl FROM WishList wl LEFT JOIN FETCH wl.wishes WHERE wl.shareId = :shareId")
    Optional<WishList> findByShareIdWithWishes(UUID shareId);
}
