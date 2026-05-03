package online.dbarenholz.omiyage.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import online.dbarenholz.omiyage.dto.SignupResponse;
import online.dbarenholz.omiyage.dto.UserResponse;
import online.dbarenholz.omiyage.entity.User;
import online.dbarenholz.omiyage.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AuthService {

    /** Inclusive lower bound of the Mullvad-style account number range (16 digits). */
    private static final long ACCOUNT_NUMBER_MIN = 1_000_000_000_000_000L;
    /** Exclusive upper bound (all digits 9 = 9999999999999999, +1 for nextLong). */
    private static final long ACCOUNT_NUMBER_MAX = 10_000_000_000_000_000L;

    private final UserRepository userRepository;

    /**
     * Generates a random 16-digit account number in the Mullvad range that is not yet in use.
     */
    private long generateUniqueAccountNumber() {
        long number;
        int attempts = 0;
        do {
            if (++attempts > 100) {
                throw new IllegalStateException("Unable to generate a unique account number after 100 attempts");
            }
            number = ThreadLocalRandom.current().nextLong(ACCOUNT_NUMBER_MIN, ACCOUNT_NUMBER_MAX);
        } while (userRepository.findByAccountNumber(number).isPresent());
        return number;
    }

    /**
     * Formats a raw 16-digit number as {@code XXXX-XXXX-XXXX-XXXX}.
     */
    public static String formatAccountNumber(long number) {
        String s = String.valueOf(number);
        return s.substring(0, 4) + "-" + s.substring(4, 8) + "-" + s.substring(8, 12) + "-" + s.substring(12, 16);
    }

    /**
     * Parses a formatted account number (with or without dashes/spaces) into a raw {@code long}.
     */
    public static long parseAccountNumber(String formatted) {
        return Long.parseLong(formatted.replaceAll("[^0-9]", ""));
    }

    @Transactional
    public SignupResponse signup() {
        User user = new User();
        user.setAccountNumber(generateUniqueAccountNumber());
        User saved = userRepository.save(user);
        return new SignupResponse(formatAccountNumber(saved.getAccountNumber()), saved.getDisplayName());
    }

    @Transactional
    public UserResponse login(String rawAccountNumber, HttpServletRequest request) {
        long accountNumber;
        try {
            accountNumber = parseAccountNumber(rawAccountNumber);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid account number");
        }

        User user = userRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid account number"));

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        HttpSession session = request.getSession(true);
        session.setAttribute("USER_ID", user.getId());

        return toUserResponse(user);
    }

    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    public UserResponse toUserResponse(User user) {
        return new UserResponse(
                formatAccountNumber(user.getAccountNumber()),
                user.getDisplayName(),
                user.getCreatedAt()
        );
    }
}
