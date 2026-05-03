package online.dbarenholz.omiyage.dto;

import java.math.BigDecimal;
import java.util.List;

public record CreateWishRequest(
        String title,
        String description,
        BigDecimal approximatePrice,
        String currencyCode,
        String imageUrl,
        List<WishLinkDto> links,
        List<String> tags
) {
}
