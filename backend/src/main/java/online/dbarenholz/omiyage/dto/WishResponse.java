package online.dbarenholz.omiyage.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Claim visibility rules:
 * <ul>
 *   <li>{@code claimed} and {@code claimedByMe} are {@code null} when the viewer is the list owner
 *       or is not authenticated — claim status is hidden to preserve the surprise.</li>
 *   <li>{@code claimed=true, claimedByMe=true} when the authenticated viewer is the one who claimed.</li>
 *   <li>{@code claimed=true, claimedByMe=false} when the wish is claimed by someone else.</li>
 *   <li>{@code claimed=false, claimedByMe=false} when the wish is not yet claimed.</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record WishResponse(
        UUID id,
        String title,
        String description,
        BigDecimal approximatePrice,
        String currencyCode,
        String imageUrl,
        List<WishLinkDto> links,
        List<String> tags,
        Instant createdAt,
        Instant updatedAt,
        Boolean claimed,
        Boolean claimedByMe
) {
}
