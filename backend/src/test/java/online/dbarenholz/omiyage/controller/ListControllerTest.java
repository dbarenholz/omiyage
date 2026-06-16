package online.dbarenholz.omiyage.controller;

import online.dbarenholz.omiyage.config.CorsProperties;
import online.dbarenholz.omiyage.config.SecurityConfig;
import online.dbarenholz.omiyage.dto.CreateListRequest;
import online.dbarenholz.omiyage.dto.WishListResponse;
import online.dbarenholz.omiyage.dto.WishListSummaryResponse;
import online.dbarenholz.omiyage.entity.User;
import online.dbarenholz.omiyage.repository.UserRepository;
import online.dbarenholz.omiyage.service.WishListService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ListController.class)
@Import(SecurityConfig.class)
// have to explicitly tell spring that we need cors properties
@EnableConfigurationProperties(CorsProperties.class)
class ListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WishListService wishListService;

    // Required for SecurityConfig (SessionAuthFilter)
    @MockitoBean
    private UserRepository userRepository;

    private User testUser;
    private TestingAuthenticationToken authToken;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setAccountNumber(1234567890123456L);

        authToken = new TestingAuthenticationToken(testUser, null, "ROLE_USER");
    }

    @Test
    void getLists_ShouldReturn200() throws Exception {
        final String LIST_TITLE = "Test List";
        final String LIST_DESC = "Test Description";

        when(wishListService.getLists(testUser)).thenReturn(List.of(
                new WishListSummaryResponse(UUID.randomUUID(), LIST_TITLE, LIST_DESC, UUID.randomUUID(),
                        Instant.now(),
                        Instant.now(), 0)));

        mockMvc.perform(get("/api/lists")
                .with(authentication(authToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value(LIST_TITLE))
                .andExpect(jsonPath("$[0].description").value(LIST_DESC));
    }

    @Test
    void createList_ShouldReturn201() throws Exception {
        final String LIST_TITLE = "New List";
        final String LIST_DESC = "Desc";
        WishListResponse response = new WishListResponse(UUID.randomUUID(), LIST_TITLE, LIST_DESC,
                UUID.randomUUID(),
                Instant.now(), List.of());
        when(wishListService.createList(eq(testUser), any(CreateListRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/lists")
                .with(authentication(authToken))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"New List\", \"description\":\"Desc\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(LIST_TITLE))
                .andExpect(jsonPath("$.description").value(LIST_DESC));
    }

    @Test
    void getSharedList_WithoutAuth_ShouldReturn200() throws Exception {
        final String LIST_TITLE = "Shared List";
        final String LIST_DESC = "Desc";
        final UUID SHARE_ID = UUID.randomUUID();

        WishListResponse response = new WishListResponse(UUID.randomUUID(), LIST_TITLE, LIST_DESC, SHARE_ID,
                Instant.now(), List.of());
        when(wishListService.getSharedList(SHARE_ID, null)).thenReturn(response);

        mockMvc.perform(get("/api/lists/shared/" + SHARE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(LIST_TITLE))
                .andExpect(jsonPath("$.description").value(LIST_DESC));
    }

    @Test
    void getLists_WithoutAuth_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/lists"))
                .andExpect(status().isUnauthorized());
    }
}
