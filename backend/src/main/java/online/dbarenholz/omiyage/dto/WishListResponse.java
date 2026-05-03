package online.dbarenholz.omiyage.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WishListResponse(
        UUID id,
        String title,
        String description,
        /** Public share identifier; construct the share URL as {@code /shared/<shareId>}. */
        UUID shareId,
        Instant createdAt,
        List<WishResponse> wishes
) {
}
