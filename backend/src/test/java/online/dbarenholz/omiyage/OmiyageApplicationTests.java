package online.dbarenholz.omiyage;

import online.dbarenholz.omiyage.dto.CreateListRequest;
import online.dbarenholz.omiyage.dto.CreateWishRequest;
import online.dbarenholz.omiyage.dto.LoginRequest;
import online.dbarenholz.omiyage.dto.SignupResponse;
import online.dbarenholz.omiyage.dto.UserResponse;
import online.dbarenholz.omiyage.dto.WishLinkDto;
import online.dbarenholz.omiyage.dto.WishListResponse;
import online.dbarenholz.omiyage.dto.WishResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import(TestOmiyageApplication.class)
class OmiyageApplicationTests {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void contextLoads() {
        // sanity check that Spring context and Testcontainers start up successfully.
    }

    @Test
    void shouldSignupAndLoginSuccessfully() {
        // do signup
        SignupResponse signupBody = webTestClient.post()
                .uri("/api/auth/signup")
                .exchange()
                .expectStatus().isCreated()
                .expectBody(SignupResponse.class)
                .returnResult().getResponseBody();

        assertThat(signupBody).isNotNull();
        assertThat(signupBody.accountNumber()).isNotNull();

        // do login with account we just made
        EntityExchangeResult<UserResponse> loginResult = webTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest(signupBody.accountNumber()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserResponse.class)
                .returnResult();

        UserResponse userResponse = loginResult.getResponseBody();
        assertThat(userResponse).isNotNull();
        assertThat(userResponse.accountNumber()).isEqualTo(signupBody.accountNumber());

        // verify login session cookie is set
        ResponseCookie jsessionId = loginResult.getResponseCookies().getFirst("JSESSIONID");
        assertThat(jsessionId).isNotNull();
        assertThat(jsessionId.getValue()).isNotEmpty();
    }

    @Test
    void shouldFailLoginWithInvalidCredentials() {
        webTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest("9999999999999999")) // non-existent account
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private record TestUserContext(String accountNumber, String jsessionId, String csrfToken) {
    }

    private TestUserContext createTestUser() {
        // signup
        SignupResponse signupBody = webTestClient.post()
                .uri("/api/auth/signup")
                .exchange()
                .expectBody(SignupResponse.class)
                .returnResult().getResponseBody();
        // login
        EntityExchangeResult<UserResponse> loginResult = webTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest(signupBody.accountNumber()))
                .exchange()
                .expectBody(UserResponse.class)
                .returnResult();
        // get csrf
        ResponseCookie initialJsessionId = loginResult.getResponseCookies().getFirst("JSESSIONID");
        String currentCookieValue = initialJsessionId != null ? initialJsessionId.getValue() : "";
        EntityExchangeResult<Map<String, String>> csrfResult = webTestClient.get()
                .uri("/api/auth/csrf")
                .cookie("JSESSIONID", currentCookieValue)
                .exchange()
                .expectBody(new ParameterizedTypeReference<Map<String, String>>() {
                })
                .returnResult();
        String csrfToken = csrfResult.getResponseBody().get("token");
        ResponseCookie updatedCookie = csrfResult.getResponseCookies().getFirst("JSESSIONID");
        if (updatedCookie != null) {
            currentCookieValue = updatedCookie.getValue();
        }
        // return a test user
        return new TestUserContext(signupBody.accountNumber(), currentCookieValue, csrfToken);
    }

    @Test
    void shouldCreateAndFetchList() {
        final String LIST_NAME = "My Wishlist";
        final String LIST_DESC = "With some description";

        TestUserContext testUserCtx = createTestUser();

        // create wish list
        CreateListRequest createListRequest = new CreateListRequest(LIST_NAME, LIST_DESC);
        WishListResponse createdList = webTestClient.post()
                .uri("/api/lists")
                .cookie("JSESSIONID", testUserCtx.jsessionId)
                .header("X-XSRF-TOKEN", testUserCtx.csrfToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createListRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(WishListResponse.class)
                .returnResult().getResponseBody();

        assertThat(createdList).isNotNull();
        assertThat(createdList.title()).isEqualTo(LIST_NAME);
        assertThat(createdList.description()).isEqualTo(LIST_DESC);
        assertThat(createdList.id()).isNotNull();
        assertThat(createdList.shareId()).isNotNull();

        // fetch the list as the authenticated user
        WishListResponse fetchedList = webTestClient.get()
                .uri("/api/lists/" + createdList.id())
                .cookie("JSESSIONID", testUserCtx.jsessionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(WishListResponse.class)
                .returnResult().getResponseBody();

        assertThat(fetchedList).isNotNull();
        assertThat(fetchedList.title()).isEqualTo(LIST_NAME);
        assertThat(fetchedList.description()).isEqualTo(LIST_DESC);
    }

    @Test
    void shouldNotFetchOtherUserList() {
        final String LIST_NAME = "User 1 List";
        final String LIST_DESC = "With some description for user 1";

        TestUserContext testUserCtx1 = createTestUser();
        TestUserContext testUserCtx2 = createTestUser();

        // user 1 creates a list
        CreateListRequest createListRequest = new CreateListRequest(LIST_NAME, LIST_DESC);
        WishListResponse createdList = webTestClient.post()
                .uri("/api/lists")
                .cookie("JSESSIONID", testUserCtx1.jsessionId)
                .header("X-XSRF-TOKEN", testUserCtx1.csrfToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createListRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(WishListResponse.class)
                .returnResult().getResponseBody();

        assertThat(createdList).isNotNull();
        assertThat(createdList.title()).isEqualTo(LIST_NAME);
        assertThat(createdList.description()).isEqualTo(LIST_DESC);
        assertThat(createdList.id()).isNotNull();
        assertThat(createdList.shareId()).isNotNull();

        // fetch the list as user 2
        webTestClient.get()
                .uri("/api/lists/" + createdList.id())
                .cookie("JSESSIONID", testUserCtx2.jsessionId)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldFetchSharedListAsUnauthenticatedUser() {
        final String LIST_NAME = "Shared List";
        final String LIST_DESC = "Publicly visible";

        TestUserContext testUserCtx = createTestUser();

        // create list
        CreateListRequest createListRequest = new CreateListRequest(LIST_NAME, LIST_DESC);
        WishListResponse createdList = webTestClient.post()
                .uri("/api/lists")
                .cookie("JSESSIONID", testUserCtx.jsessionId)
                .header("X-XSRF-TOKEN", testUserCtx.csrfToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createListRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(WishListResponse.class)
                .returnResult().getResponseBody();

        // fetch shared list unauthenticated
        WishListResponse sharedFetchResponse = webTestClient.get()
                .uri("/api/lists/shared/" + createdList.shareId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(WishListResponse.class)
                .returnResult().getResponseBody();

        assertThat(sharedFetchResponse).isNotNull();
        assertThat(sharedFetchResponse.title()).isEqualTo(LIST_NAME);
        assertThat(sharedFetchResponse.description()).isEqualTo(LIST_DESC);
    }

    @Test
    void shouldFetchSharedListAsOtherUser() {
        final String LIST_NAME = "User 1 List";
        final String LIST_DESC = "With some description for user 1";

        TestUserContext testUserCtx1 = createTestUser();
        TestUserContext testUserCtx2 = createTestUser();

        // user 1 makes list
        CreateListRequest createListRequest = new CreateListRequest(LIST_NAME, LIST_DESC);
        WishListResponse createdList = webTestClient.post()
                .uri("/api/lists")
                .cookie("JSESSIONID", testUserCtx1.jsessionId)
                .header("X-XSRF-TOKEN", testUserCtx1.csrfToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createListRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(WishListResponse.class)
                .returnResult().getResponseBody();

        assertThat(createdList).isNotNull();
        assertThat(createdList.title()).isEqualTo(LIST_NAME);
        assertThat(createdList.id()).isNotNull();
        assertThat(createdList.shareId()).isNotNull();

        // fetch shared list as user 2
        WishListResponse sharedFetchResponse = webTestClient.get()
                .uri("/api/lists/shared/" + createdList.shareId())
                .cookie("JSESSIONID", testUserCtx2.jsessionId)
                .header("X-XSRF-TOKEN", testUserCtx2.csrfToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(WishListResponse.class)
                .returnResult().getResponseBody();

        assertThat(sharedFetchResponse).isNotNull();
        assertThat(sharedFetchResponse.title()).isEqualTo(LIST_NAME);
        assertThat(sharedFetchResponse.description()).isEqualTo(LIST_DESC);
    }

    @Test
    void shouldCreateAndFetchWish() {
        final String LIST_NAME = "Wish List";
        final String LIST_DESC = "Desc";
        final String WISH_NAME = "My New Wish";
        final String WISH_DESC = "Wish Description";
        final BigDecimal WISH_PRICE = new BigDecimal("19.99");
        final String WISH_CURRENCY = "EUR";
        final String WISH_IMAGE_URL = "http://example.com/image1";
        final List<WishLinkDto> WISH_LINKS = List.of(new WishLinkDto("Example", "http://example.com"));
        final List<String> WISH_TAGS = List.of("tag1", "tag2");

        TestUserContext testUserCtx = createTestUser();

        // create list
        CreateListRequest createListRequest = new CreateListRequest(LIST_NAME, LIST_DESC);
        WishListResponse createdList = webTestClient.post()
                .uri("/api/lists")
                .cookie("JSESSIONID", testUserCtx.jsessionId())
                .header("X-XSRF-TOKEN", testUserCtx.csrfToken())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createListRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(WishListResponse.class)
                .returnResult().getResponseBody();

        // create wish
        CreateWishRequest createWishRequest = new CreateWishRequest(
                WISH_NAME,
                WISH_DESC,
                WISH_PRICE,
                WISH_CURRENCY,
                WISH_IMAGE_URL,
                WISH_LINKS,
                WISH_TAGS);
        WishResponse createdWish = webTestClient.post()
                .uri("/api/lists/" + createdList.id() + "/wishes")
                .cookie("JSESSIONID", testUserCtx.jsessionId())
                .header("X-XSRF-TOKEN", testUserCtx.csrfToken())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createWishRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(WishResponse.class)
                .returnResult().getResponseBody();

        assertThat(createdWish).isNotNull();
        assertThat(createdWish.title()).isEqualTo(WISH_NAME);
        assertThat(createdWish.description()).isEqualTo(WISH_DESC);
        assertThat(createdWish.approximatePrice()).isEqualTo(WISH_PRICE);
        assertThat(createdWish.currencyCode()).isEqualTo(WISH_CURRENCY);
        assertThat(createdWish.imageUrl()).isEqualTo(WISH_IMAGE_URL);
        assertThat(createdWish.links()).isEqualTo(WISH_LINKS);
        assertThat(createdWish.tags()).isEqualTo(WISH_TAGS);

        // fetch wish
        List<WishResponse> wishes = webTestClient.get()
                .uri("/api/lists/" + createdList.id() + "/wishes")
                .cookie("JSESSIONID", testUserCtx.jsessionId())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(WishResponse.class)
                .returnResult().getResponseBody();

        assertThat(wishes).hasSize(1);
        assertThat(wishes.get(0).title()).isEqualTo(WISH_NAME);
        assertThat(wishes.get(0).description()).isEqualTo(WISH_DESC);
        assertThat(wishes.get(0).approximatePrice()).isEqualTo(WISH_PRICE);
        assertThat(wishes.get(0).currencyCode()).isEqualTo(WISH_CURRENCY);
        assertThat(wishes.get(0).imageUrl()).isEqualTo(WISH_IMAGE_URL);
        assertThat(wishes.get(0).links()).isEqualTo(WISH_LINKS);
        assertThat(wishes.get(0).tags()).isEqualTo(WISH_TAGS);
    }

    @Test
    void shouldClaimAndUnclaimSharedWishWithAppropriateVisibility() {
        final String LIST_NAME = "Wish List";
        final String LIST_DESC = "Desc";
        final String WISH_NAME = "My New Wish";
        final String WISH_DESC = "Wish Description";
        final BigDecimal WISH_PRICE = new BigDecimal("19.99");
        final String WISH_CURRENCY = "EUR";
        final String WISH_IMAGE_URL = "http://example.com/image1";
        final List<WishLinkDto> WISH_LINKS = List.of(new WishLinkDto("Example", "http://example.com"));
        final List<String> WISH_TAGS = List.of("tag1", "tag2");

        TestUserContext owner = createTestUser();
        TestUserContext claimer = createTestUser();
        TestUserContext other = createTestUser();

        // create list
        CreateListRequest createListRequest = new CreateListRequest(LIST_NAME, LIST_DESC);
        WishListResponse list = webTestClient.post()
                .uri("/api/lists")
                .cookie("JSESSIONID", owner.jsessionId())
                .header("X-XSRF-TOKEN", owner.csrfToken())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createListRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(WishListResponse.class)
                .returnResult().getResponseBody();

        // create wish
        CreateWishRequest createWishRequest = new CreateWishRequest(
                WISH_NAME,
                WISH_DESC,
                WISH_PRICE,
                WISH_CURRENCY,
                WISH_IMAGE_URL,
                WISH_LINKS,
                WISH_TAGS);
        WishResponse wish = webTestClient.post()
                .uri("/api/lists/" + list.id() + "/wishes")
                .cookie("JSESSIONID", owner.jsessionId())
                .header("X-XSRF-TOKEN", owner.csrfToken())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createWishRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(WishResponse.class)
                .returnResult().getResponseBody();

        // claim wish
        webTestClient.post()
                .uri("/api/wishes/" + wish.id() + "/claim")
                .cookie("JSESSIONID", claimer.jsessionId())
                .header("X-XSRF-TOKEN", claimer.csrfToken())
                .exchange()
                .expectStatus().isNoContent();

        // fetch list as claimer
        WishListResponse sharedListAfterClaim = webTestClient.get()
                .uri("/api/lists/shared/" + list.shareId())
                .cookie("JSESSIONID", claimer.jsessionId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(WishListResponse.class)
                .returnResult().getResponseBody();

        // verify wish is claimed by claimer
        WishResponse claimedWish = sharedListAfterClaim.wishes().get(0);
        assertThat(claimedWish.claimed()).isTrue();
        assertThat(claimedWish.claimedByMe()).isTrue();

        // fetch list as unauthenticated user
        WishListResponse sharedListUnauth = webTestClient.get()
                .uri("/api/lists/shared/" + list.shareId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(WishListResponse.class)
                .returnResult().getResponseBody();

        // verify unauthenticated user can't see anything, because if it's the original
        // user it'd ruin the surprise
        WishResponse claimedWishUnauth = sharedListUnauth.wishes().get(0);
        assertThat(claimedWishUnauth.claimed()).isNull();

        // fetch list as list owner
        WishListResponse sharedListOwner = webTestClient.get()
                .uri("/api/lists/" + list.id())
                .cookie("JSESSIONID", owner.jsessionId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(WishListResponse.class)
                .returnResult().getResponseBody();

        // verify owner doesn't see claim status
        WishResponse claimedWishOwner = sharedListOwner.wishes().get(0);
        assertThat(claimedWishOwner.claimed()).isNull();

        // fetch list as other user
        WishListResponse sharedListOther = webTestClient.get()
                .uri("/api/lists/shared/" + list.shareId())
                .cookie("JSESSIONID", other.jsessionId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(WishListResponse.class)
                .returnResult().getResponseBody();

        // verify other user sees claim status
        WishResponse claimedWishOther = sharedListOther.wishes().get(0);
        assertThat(claimedWishOther.claimedByMe()).isFalse();
        assertThat(claimedWishOther.claimed()).isTrue();

        // unclaim wish as the claimer
        webTestClient.delete()
                .uri("/api/wishes/" + wish.id() + "/claim")
                .cookie("JSESSIONID", claimer.jsessionId())
                .header("X-XSRF-TOKEN", claimer.csrfToken())
                .exchange()
                .expectStatus().isNoContent();

        // fetch as unauthenticated user after unclaim
        WishListResponse sharedListAfterUnclaimUnauth = webTestClient.get()
                .uri("/api/lists/shared/" + list.shareId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(WishListResponse.class)
                .returnResult().getResponseBody();

        // verify unauthenticated user still can't see anything
        WishResponse unclaimedWishUnauth = sharedListAfterUnclaimUnauth.wishes().get(0);
        assertThat(unclaimedWishUnauth.claimed()).isNull();

        // fetch as claimer after unclaim
        WishListResponse sharedListAfterUnclaim = webTestClient.get()
                .uri("/api/lists/shared/" + list.shareId())
                .cookie("JSESSIONID", claimer.jsessionId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(WishListResponse.class)
                .returnResult().getResponseBody();

        // verify unclaimed as claimer
        WishResponse unclaimedWish = sharedListAfterUnclaim.wishes().get(0);
        assertThat(unclaimedWish.claimed()).isFalse();
        assertThat(unclaimedWish.claimedByMe()).isFalse();

        // fetch list as other user after unclaim by claimer
        WishListResponse sharedListAfterUnclaimOther = webTestClient.get()
                .uri("/api/lists/shared/" + list.shareId())
                .cookie("JSESSIONID", other.jsessionId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(WishListResponse.class)
                .returnResult().getResponseBody();

        // verify other user sees unclaimed
        WishResponse unclaimedWishOther = sharedListAfterUnclaimOther.wishes().get(0);
        assertThat(unclaimedWishOther.claimed()).isFalse();
    }

    @Test
    void shouldNotClaimSharedWishAsUnauthenticatedUser() {
        TestUserContext owner = createTestUser();
        // create list & wish
        WishListResponse list = webTestClient.post().uri("/api/lists").cookie("JSESSIONID", owner.jsessionId())
                .header("X-XSRF-TOKEN", owner.csrfToken()).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateListRequest("List", "Desc")).exchange().expectBody(WishListResponse.class)
                .returnResult().getResponseBody();
        WishResponse wish = webTestClient.post().uri("/api/lists/" + list.id() + "/wishes")
                .cookie("JSESSIONID", owner.jsessionId()).header("X-XSRF-TOKEN", owner.csrfToken())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateWishRequest("Gift", "Desc", null, null, null, null, null)).exchange()
                .expectBody(WishResponse.class).returnResult().getResponseBody();

        // Try to claim as unauthenticated (no session cookie/csrf)
        webTestClient.post()
                .uri("/api/wishes/" + wish.id() + "/claim")
                .exchange()
                .expectStatus().is4xxClientError();
    }

}
