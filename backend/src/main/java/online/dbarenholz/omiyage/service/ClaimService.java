package online.dbarenholz.omiyage.service;

import lombok.RequiredArgsConstructor;
import online.dbarenholz.omiyage.entity.User;
import online.dbarenholz.omiyage.entity.Wish;
import online.dbarenholz.omiyage.entity.WishClaim;
import online.dbarenholz.omiyage.repository.WishClaimRepository;
import online.dbarenholz.omiyage.repository.WishRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ClaimService {

    private final WishRepository wishRepository;
    private final WishClaimRepository wishClaimRepository;

    public void claimWish(UUID wishId, User currentUser) {
        Wish wish = wishRepository.findByIdWithDetails(wishId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wish not found"));

        if (wish.getList().getOwner().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot claim a wish from your own list");
        }

        if (wishClaimRepository.existsByWishId(wishId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Wish is already claimed");
        }

        WishClaim claim = new WishClaim();
        claim.setWish(wish);
        claim.setClaimedBy(currentUser);
        wishClaimRepository.save(claim);
    }

    public void unclaimWish(UUID wishId, User currentUser) {
        WishClaim claim = wishClaimRepository.findByWishId(wishId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wish is not claimed"));

        if (!claim.getClaimedBy().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You did not claim this wish");
        }

        wishClaimRepository.delete(claim);
    }
}
