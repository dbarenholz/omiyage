package online.dbarenholz.omiyage.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import online.dbarenholz.omiyage.repository.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

/**
 * Reads the authenticated user's UUID from the HTTP session and populates
 * the Spring Security context so that downstream filters and controllers
 * see the user as authenticated.
 */
public class SessionAuthFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public SessionAuthFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object raw = session.getAttribute("USER_ID");
            if (raw instanceof UUID userId) {
                userRepository.findById(userId).ifPresent(user -> {
                    var auth = new UsernamePasswordAuthenticationToken(
                            user, null, Collections.emptyList());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });
            }
        }
        filterChain.doFilter(request, response);
    }
}
