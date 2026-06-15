package online.dbarenholz.omiyage.repository;

import online.dbarenholz.omiyage.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByAccountNumber(Long accountNumber);

    /**
     * Used by the cleanup task to find never-used accounts older than the given
     * cutoff.
     */
    List<User> findByLastLoginAtIsNullAndCreatedAtBefore(Instant cutoff);
}
