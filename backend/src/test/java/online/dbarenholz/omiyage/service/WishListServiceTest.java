package online.dbarenholz.omiyage.service;

import online.dbarenholz.omiyage.dto.CreateListRequest;
import online.dbarenholz.omiyage.dto.WishListResponse;
import online.dbarenholz.omiyage.dto.WishListSummaryResponse;
import online.dbarenholz.omiyage.entity.User;
import online.dbarenholz.omiyage.entity.WishList;
import online.dbarenholz.omiyage.repository.WishListRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import online.dbarenholz.omiyage.exception.UnauthorizedAccessException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WishListServiceTest {

    @Mock
    private WishListRepository wishListRepository;

    @Mock
    private WishMapper wishMapper;

    @InjectMocks
    private WishListService wishListService;

    private User currentUser;
    private User otherUser;
    private WishList wishList;

    private static final String LIST_TITLE = "My Wishlist";
    private static final String LIST_DESCRIPTION = "Things I want";

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(UUID.randomUUID());
        currentUser.setAccountNumber(1234567890123456L);

        otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        otherUser.setAccountNumber(9876543210987654L);

        wishList = new WishList();
        wishList.setId(UUID.randomUUID());
        wishList.setOwner(currentUser);
        wishList.setTitle(LIST_TITLE);
        wishList.setDescription(LIST_DESCRIPTION);
        wishList.setShareId(UUID.randomUUID());
        wishList.setCreatedAt(Instant.now());
        wishList.setUpdatedAt(Instant.now());
    }

    @Test
    void getLists_ShouldReturnSummaryList() {
        when(wishListRepository.findByOwnerId(currentUser.getId())).thenReturn(List.of(wishList));

        List<WishListSummaryResponse> result = wishListService.getLists(currentUser);

        assertEquals(1, result.size());
        assertEquals(LIST_TITLE, result.get(0).title());
        assertEquals(LIST_DESCRIPTION, result.get(0).description());
    }

    @Test
    void createList_ShouldReturnWishListResponse() {
        CreateListRequest request = new CreateListRequest("New List", "Description");
        when(wishListRepository.save(any(WishList.class))).thenAnswer(invocation -> {
            WishList saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setShareId(UUID.randomUUID());
            return saved;
        });

        WishListResponse response = wishListService.createList(currentUser, request);

        assertNotNull(response);
        assertEquals("New List", response.title());
        assertEquals("Description", response.description());
        verify(wishListRepository).save(any(WishList.class));
    }

    @Test
    void getList_WhenOwner_ShouldReturnResponse() {
        when(wishListRepository.findByIdWithWishes(wishList.getId())).thenReturn(Optional.of(wishList));

        WishListResponse response = wishListService.getList(wishList.getId(), currentUser);

        assertNotNull(response);
        assertEquals(wishList.getId(), response.id());
    }

    @Test
    void getList_WhenNotOwner_ShouldThrowForbidden() {
        when(wishListRepository.findByIdWithWishes(wishList.getId())).thenReturn(Optional.of(wishList));

        assertThrows(UnauthorizedAccessException.class, () -> wishListService.getList(wishList.getId(), otherUser));
    }
}
