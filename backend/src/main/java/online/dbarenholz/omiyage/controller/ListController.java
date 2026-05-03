package online.dbarenholz.omiyage.controller;

import lombok.RequiredArgsConstructor;
import online.dbarenholz.omiyage.dto.CreateListRequest;
import online.dbarenholz.omiyage.dto.UpdateListRequest;
import online.dbarenholz.omiyage.dto.WishListResponse;
import online.dbarenholz.omiyage.dto.WishListSummaryResponse;
import online.dbarenholz.omiyage.entity.User;
import online.dbarenholz.omiyage.service.WishListService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lists")
@RequiredArgsConstructor
public class ListController {

    private final WishListService wishListService;

    @GetMapping
    public ResponseEntity<List<WishListSummaryResponse>> getLists(Authentication auth) {
        User user = (User) auth.getPrincipal();
        return ResponseEntity.ok(wishListService.getLists(user));
    }

    @PostMapping
    public ResponseEntity<WishListResponse> createList(@RequestBody CreateListRequest request,
                                                       Authentication auth) {
        User user = (User) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(wishListService.createList(user, request));
    }

    @GetMapping("/{listId}")
    public ResponseEntity<WishListResponse> getList(@PathVariable UUID listId,
                                                    Authentication auth) {
        User user = (User) auth.getPrincipal();
        return ResponseEntity.ok(wishListService.getList(listId, user));
    }

    @PatchMapping("/{listId}")
    public ResponseEntity<WishListResponse> updateList(@PathVariable UUID listId,
                                                       @RequestBody UpdateListRequest request,
                                                       Authentication auth) {
        User user = (User) auth.getPrincipal();
        return ResponseEntity.ok(wishListService.updateList(listId, user, request));
    }

    @DeleteMapping("/{listId}")
    public ResponseEntity<Void> deleteList(@PathVariable UUID listId,
                                           Authentication auth) {
        User user = (User) auth.getPrincipal();
        wishListService.deleteList(listId, user);
        return ResponseEntity.noContent().build();
    }

    /**
     * Public endpoint: no authentication required, but if a session exists the user is resolved
     * so that the owner can be identified and claim info is hidden from them.
     */
    @GetMapping("/shared/{shareId}")
    public ResponseEntity<WishListResponse> getSharedList(@PathVariable UUID shareId,
                                                          Authentication auth) {
        User viewer = (auth != null && auth.isAuthenticated()) ? (User) auth.getPrincipal() : null;
        return ResponseEntity.ok(wishListService.getSharedList(shareId, viewer));
    }
}
