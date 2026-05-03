package online.dbarenholz.omiyage.controller;

import lombok.RequiredArgsConstructor;
import online.dbarenholz.omiyage.dto.CreateWishRequest;
import online.dbarenholz.omiyage.dto.UpdateWishRequest;
import online.dbarenholz.omiyage.dto.WishResponse;
import online.dbarenholz.omiyage.entity.User;
import online.dbarenholz.omiyage.service.WishService;
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
@RequestMapping("/api/lists/{listId}/wishes")
@RequiredArgsConstructor
public class WishController {

    private final WishService wishService;

    @GetMapping
    public ResponseEntity<List<WishResponse>> getWishes(@PathVariable UUID listId,
                                                        Authentication auth) {
        User user = (User) auth.getPrincipal();
        return ResponseEntity.ok(wishService.getWishes(listId, user));
    }

    @PostMapping
    public ResponseEntity<WishResponse> createWish(@PathVariable UUID listId,
                                                   @RequestBody CreateWishRequest request,
                                                   Authentication auth) {
        User user = (User) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(wishService.createWish(listId, user, request));
    }

    @PatchMapping("/{wishId}")
    public ResponseEntity<WishResponse> updateWish(@PathVariable UUID listId,
                                                   @PathVariable UUID wishId,
                                                   @RequestBody UpdateWishRequest request,
                                                   Authentication auth) {
        User user = (User) auth.getPrincipal();
        return ResponseEntity.ok(wishService.updateWish(listId, wishId, user, request));
    }

    @DeleteMapping("/{wishId}")
    public ResponseEntity<Void> deleteWish(@PathVariable UUID listId,
                                           @PathVariable UUID wishId,
                                           Authentication auth) {
        User user = (User) auth.getPrincipal();
        wishService.deleteWish(listId, wishId, user);
        return ResponseEntity.noContent().build();
    }
}
