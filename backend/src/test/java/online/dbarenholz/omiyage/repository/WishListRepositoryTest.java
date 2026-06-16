package online.dbarenholz.omiyage.repository;

import online.dbarenholz.omiyage.TestOmiyageApplication;
import online.dbarenholz.omiyage.entity.User;
import online.dbarenholz.omiyage.entity.WishList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestOmiyageApplication.class)
class WishListRepositoryTest {

    @Autowired
    private WishListRepository wishListRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private WishList testList;

    private static final String LIST_TITLE = "My Wishlist";
    private static final String LIST_DESCRIPTION = "Things I want";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setAccountNumber(1111222233334444L);
        userRepository.save(testUser);

        testList = new WishList();
        testList.setOwner(testUser);
        testList.setTitle(LIST_TITLE);
        testList.setDescription(LIST_DESCRIPTION);
        wishListRepository.save(testList);
    }

    @Test
    void findByIdWithWishes_ShouldReturnList() {
        Optional<WishList> found = wishListRepository.findByIdWithWishes(testList.getId());

        assertTrue(found.isPresent());
        assertEquals(LIST_TITLE, found.get().getTitle());
        assertEquals(LIST_DESCRIPTION, found.get().getDescription());
        assertEquals(testUser.getId(), found.get().getOwner().getId());
    }

    @Test
    void findByShareIdWithWishes_ShouldReturnList() {
        Optional<WishList> found = wishListRepository.findByShareIdWithWishes(testList.getShareId());

        assertTrue(found.isPresent());
        assertEquals(LIST_TITLE, found.get().getTitle());
        assertEquals(LIST_DESCRIPTION, found.get().getDescription());
        assertEquals(testList.getShareId(), found.get().getShareId());
    }
}
