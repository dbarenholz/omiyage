package online.dbarenholz.omiyage.repository;

import online.dbarenholz.omiyage.entity.Wish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WishRepository extends JpaRepository<Wish, UUID> {

    /**
     * Fetch all wishes for a list; child collections are batched by @BatchSize on
     * the entity.
     */
    @Query("SELECT w FROM Wish w WHERE w.list.id = :listId ORDER BY w.createdAt ASC")
    List<Wish> findByListIdWithDetails(UUID listId);

    /**
     * Fetch a single wish by id; child collections are batched by @BatchSize on the
     * entity.
     */
    Optional<Wish> findById(UUID id);

    default Optional<Wish> findByIdWithDetails(UUID id) {
        return findById(id);
    }
}
