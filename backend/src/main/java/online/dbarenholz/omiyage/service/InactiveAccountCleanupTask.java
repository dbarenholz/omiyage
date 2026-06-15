package online.dbarenholz.omiyage.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.dbarenholz.omiyage.entity.User;
import online.dbarenholz.omiyage.repository.UserRepository;

/**
 * Periodically removes accounts that were created but never used.
 *
 * <p>
 * An account is considered "inactive" if:
 * <ul>
 * <li>Its {@code lastLoginAt} is {@code null} (the user never logged in after
 * the initial signup), and</li>
 * <li>It was created more than {@code app.cleanup.inactive-account-minutes}
 * minutes ago (default: 15).</li>
 * </ul>
 *
 * <p>
 * This prevents orphaned accounts from accumulating when a user generates an
 * account number
 * but never returns to the application.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InactiveAccountCleanupTask {

    private final UserRepository userRepository;
    private final online.dbarenholz.omiyage.config.AppCleanupProperties appCleanupProperties;

    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    public void deleteInactiveAccounts() {
        int inactiveAccountMinutes = appCleanupProperties.inactiveAccountMinutes();
        Instant cutoff = Instant.now().minus(inactiveAccountMinutes, ChronoUnit.MINUTES);
        List<User> stale = userRepository.findByLastLoginAtIsNullAndCreatedAtBefore(cutoff);
        if (!stale.isEmpty()) {
            userRepository.deleteAll(stale);
            log.info("Deleted {} inactive account(s) not logged into within {} minutes",
                    stale.size(), inactiveAccountMinutes);
        }
    }
}
