package online.dbarenholz.omiyage.service;

import lombok.RequiredArgsConstructor;
import online.dbarenholz.omiyage.dto.CreateWishRequest;
import online.dbarenholz.omiyage.dto.UpdateWishRequest;
import online.dbarenholz.omiyage.dto.WishResponse;
import online.dbarenholz.omiyage.entity.User;
import online.dbarenholz.omiyage.entity.Wish;
import online.dbarenholz.omiyage.entity.WishLink;
import online.dbarenholz.omiyage.entity.WishList;
import online.dbarenholz.omiyage.entity.WishTag;
import online.dbarenholz.omiyage.repository.WishListRepository;
import online.dbarenholz.omiyage.repository.WishRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class WishService {

    private final WishRepository wishRepository;
    private final WishListRepository wishListRepository;
    private final WishMapper wishMapper;

    @Transactional(readOnly = true)
    public List<WishResponse> getWishes(UUID listId, User currentUser) {
        WishList list = requireOwner(listId, currentUser);
        return wishRepository.findByListIdWithDetails(list.getId()).stream()
                .map(wish -> wishMapper.toResponse(wish, currentUser, list.getOwner().getId()))
                .toList();
    }

    public WishResponse createWish(UUID listId, User currentUser, CreateWishRequest request) {
        WishList list = requireOwner(listId, currentUser);

        Wish wish = new Wish();
        wish.setList(list);
        wish.setTitle(request.title());
        wish.setDescription(request.description());
        wish.setApproximatePrice(request.approximatePrice());
        wish.setCurrencyCode(normalizeCurrency(request.currencyCode()));
        wish.setImageUrl(normalizeOptionalText(request.imageUrl()));

        if (request.links() != null) {
            request.links().forEach(dto -> {
                WishLink link = new WishLink();
                link.setWish(wish);
                link.setUrl(dto.url());
                link.setLabel(dto.label());
                wish.getLinks().add(link);
            });
        }

        if (request.tags() != null) {
            request.tags().forEach(tagStr -> {
                WishTag tag = new WishTag();
                tag.setWish(wish);
                tag.setTag(tagStr);
                wish.getTags().add(tag);
            });
        }

        Wish saved = wishRepository.save(wish);
        touchList(list);
        return wishMapper.toResponse(saved, currentUser, list.getOwner().getId());
    }

    public WishResponse updateWish(UUID listId, UUID wishId, User currentUser, UpdateWishRequest request) {
        WishList list = requireOwner(listId, currentUser);
        Wish wish = requireWishInList(wishId, listId);

        if (request.title() != null) wish.setTitle(request.title());
        if (request.description() != null) wish.setDescription(request.description());
        if (request.approximatePrice() != null) wish.setApproximatePrice(request.approximatePrice());
        if (request.currencyCode() != null) wish.setCurrencyCode(normalizeCurrency(request.currencyCode()));
        if (request.imageUrl() != null) wish.setImageUrl(normalizeOptionalText(request.imageUrl()));

        if (request.links() != null) {
            wish.getLinks().clear();
            request.links().forEach(dto -> {
                WishLink link = new WishLink();
                link.setWish(wish);
                link.setUrl(dto.url());
                link.setLabel(dto.label());
                wish.getLinks().add(link);
            });
        }

        if (request.tags() != null) {
            wish.getTags().clear();
            request.tags().forEach(tagStr -> {
                WishTag tag = new WishTag();
                tag.setWish(wish);
                tag.setTag(tagStr);
                wish.getTags().add(tag);
            });
        }

        Wish savedWish = wishRepository.save(wish);
        touchList(list);
        return wishMapper.toResponse(savedWish, currentUser, list.getOwner().getId());
    }

    public void deleteWish(UUID listId, UUID wishId, User currentUser) {
        WishList list = requireOwner(listId, currentUser);
        Wish wish = requireWishInList(wishId, listId);
        wishRepository.delete(wish);
        touchList(list);
    }

    private void touchList(WishList list) {
        list.setUpdatedAt(Instant.now());
    }

    private WishList requireOwner(UUID listId, User user) {
        WishList list = wishListRepository.findById(listId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "List not found"));
        if (!list.getOwner().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return list;
    }

    private Wish requireWishInList(UUID wishId, UUID listId) {
        Wish wish = wishRepository.findByIdWithDetails(wishId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wish not found"));
        if (!wish.getList().getId().equals(listId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Wish not found in this list");
        }
        return wish;
    }

    private String normalizeCurrency(String currencyCode) {
        String normalized = normalizeOptionalText(currencyCode);
        return normalized == null ? null : normalized.toUpperCase();
    }

    private String normalizeOptionalText(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
