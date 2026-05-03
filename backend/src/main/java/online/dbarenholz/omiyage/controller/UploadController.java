package online.dbarenholz.omiyage.controller;

import lombok.RequiredArgsConstructor;
import online.dbarenholz.omiyage.dto.ImageUploadResponse;
import online.dbarenholz.omiyage.service.ImageStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final ImageStorageService imageStorageService;

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageUploadResponse> uploadImage(@RequestPart("file") MultipartFile file) {
        String imageUrl = imageStorageService.uploadWishImage(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ImageUploadResponse(imageUrl));
    }
}
