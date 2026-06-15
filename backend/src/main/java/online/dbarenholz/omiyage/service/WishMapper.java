package online.dbarenholz.omiyage.service;

import lombok.RequiredArgsConstructor;
import online.dbarenholz.omiyage.dto.WishLinkDto;
import online.dbarenholz.omiyage.dto.WishResponse;
import online.dbarenholz.omiyage.entity.User;
import online.dbarenholz.omiyage.entity.Wish;
import online.dbarenholz.omiyage.entity.WishClaim;
import online.dbarenholz.omiyage.entity.WishTag;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Shared mapping logic for converting a {@link Wish} entity to a {@link WishResponse} DTO.
 *
 * <p>Claim visibility rules:
 * <ul>
 *   <li>If the viewer is {@code null} (unauthenticated) or is the list owner, claim info is
 *       hidden ({@code claimed} and {@code claimedByMe} are both {@code null}).</li>
 *   <li>If the viewer is the claimer, {@code claimed=true, claimedByMe=true}.</li>
 *   <li>If the wish is claimed by someone else, {@code claimed=true, claimedByMe=false}.</li>
 *   <li>If the wish is not claimed, {@code claimed=false, claimedByMe=false}.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class WishMapper {

    /**
     * Maps a wish to its response DTO.
     *
     * @param wish        the wish entity (claim/links/tags must be accessible)
     * @param viewer      the currently authenticated user, or {@code null} if unauthenticated
     * @param listOwnerId the UUID of the list owner
     */
    public WishResponse toResponse(Wish wish, @Nullable User viewer, UUID listOwnerId) {
        List<WishLinkDto> links = wish.getLinks().stream()
                .map(link -> new WishLinkDto(link.getUrl(), link.getLabel()))
                .toList();

        List<String> tags = wish.getTags().stream()
                .map(WishTag::getTag)
                .toList();

        Boolean claimed;
        Boolean claimedByMe;

        boolean viewerIsOwner = viewer != null && viewer.getId().equals(listOwnerId);

        if (viewer == null || viewerIsOwner) {
            // Unauthenticated viewers and the list owner cannot see claim status
            claimed = null;
            claimedByMe = null;
        } else {
            WishClaim wishClaim = wish.getClaim();
            if (wishClaim != null) {
                claimedByMe = wishClaim.getClaimedBy().getId().equals(viewer.getId());
                claimed = true;
            } else {
                claimedByMe = false;
                claimed = false;
            }
        }

        return new WishResponse(
                wish.getId(),
                wish.getTitle(),
                wish.getDescription(),
                wish.getApproximatePrice(),
            wish.getCurrencyCode(),
            wish.getImageUrl(),
                links,
                tags,
                wish.getCreatedAt(),
                wish.getUpdatedAt(),
                claimed,
                claimedByMe
        );
    }
}
