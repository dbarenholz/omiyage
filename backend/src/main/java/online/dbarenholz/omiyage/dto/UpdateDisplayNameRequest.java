package online.dbarenholz.omiyage.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateDisplayNameRequest(
        @NotBlank(message = "Display name is required") String displayName
) {
}
