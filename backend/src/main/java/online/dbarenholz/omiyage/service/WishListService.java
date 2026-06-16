package online.dbarenholz.omiyage.service;

import lombok.RequiredArgsConstructor;
import online.dbarenholz.omiyage.dto.CreateListRequest;
import online.dbarenholz.omiyage.dto.UpdateListRequest;
import online.dbarenholz.omiyage.dto.WishListResponse;
import online.dbarenholz.omiyage.dto.WishListSummaryResponse;
import online.dbarenholz.omiyage.dto.WishResponse;
import online.dbarenholz.omiyage.entity.User;
import online.dbarenholz.omiyage.entity.WishList;
import online.dbarenholz.omiyage.repository.WishListRepository;
import online.dbarenholz.omiyage.exception.ResourceNotFoundException;
import online.dbarenholz.omiyage.exception.UnauthorizedAccessException;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class WishListService {

    private final WishListRepository wishListRepository;
    private final WishMapper wishMapper;

    @Transactional(readOnly = true)
    public List<WishListSummaryResponse> getLists(User currentUser) {
        return wishListRepository.findByOwnerId(currentUser.getId()).stream()
                .map(list -> new WishListSummaryResponse(
                        list.getId(),
                        list.getTitle(),
                        list.getDescription(),
                        list.getShareId(),
                list.getCreatedAt(),
                list.getUpdatedAt(),
                list.getWishes().size()))
                .toList();
    }

    public WishListResponse createList(User currentUser, CreateListRequest request) {
        WishList list = new WishList();
        list.setOwner(currentUser);
        list.setTitle(request.title());
        list.setDescription(request.description());
        WishList saved = wishListRepository.save(list);
        // Owner viewing their own newly created list — hide claim info
        return toDetailResponse(saved, currentUser, currentUser.getId());
    }

    @Transactional(readOnly = true)
    public WishListResponse getList(UUID listId, User currentUser) {
        WishList list = wishListRepository.findByIdWithWishes(listId)
                .orElseThrow(() -> new ResourceNotFoundException("List not found"));
        if (!list.getOwner().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("Access denied");
        }
        return toDetailResponse(list, currentUser, list.getOwner().getId());
    }

    public WishListResponse updateList(UUID listId, User currentUser, UpdateListRequest request) {
        WishList list = wishListRepository.findById(listId)
                .orElseThrow(() -> new ResourceNotFoundException("List not found"));
        if (!list.getOwner().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("Access denied");
        }
        if (request.title() != null) list.setTitle(request.title());
        if (request.description() != null) list.setDescription(request.description());
        WishList saved = wishListRepository.save(list);
        return toDetailResponse(saved, currentUser, saved.getOwner().getId());
    }

    public void deleteList(UUID listId, User currentUser) {
        WishList list = wishListRepository.findById(listId)
                .orElseThrow(() -> new ResourceNotFoundException("List not found"));
        if (!list.getOwner().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("Access denied");
        }
        wishListRepository.delete(list);
    }

    @Transactional(readOnly = true)
    public WishListResponse getSharedList(UUID shareId, @Nullable User viewer) {
        WishList list = wishListRepository.findByShareIdWithWishes(shareId)
                .orElseThrow(() -> new ResourceNotFoundException("List not found"));
        return toDetailResponse(list, viewer, list.getOwner().getId());
    }

    private WishListResponse toDetailResponse(WishList list, @Nullable User viewer, UUID listOwnerId) {
        List<WishResponse> wishes = list.getWishes().stream()
                .map(wish -> wishMapper.toResponse(wish, viewer, listOwnerId))
                .toList();
        return new WishListResponse(
                list.getId(),
                list.getTitle(),
                list.getDescription(),
                list.getShareId(),
                list.getCreatedAt(),
                wishes);
    }
}
