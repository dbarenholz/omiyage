package online.dbarenholz.omiyage.controller;

import lombok.RequiredArgsConstructor;
import online.dbarenholz.omiyage.entity.User;
import online.dbarenholz.omiyage.service.ClaimService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/wishes/{wishId}/claim")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;

    @PostMapping
    public ResponseEntity<Void> claimWish(@PathVariable UUID wishId,
                                          Authentication auth) {
        User user = (User) auth.getPrincipal();
        claimService.claimWish(wishId, user);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> unclaimWish(@PathVariable UUID wishId,
                                            Authentication auth) {
        User user = (User) auth.getPrincipal();
        claimService.unclaimWish(wishId, user);
        return ResponseEntity.noContent().build();
    }
}
