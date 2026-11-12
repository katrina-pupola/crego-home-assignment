package com.crego.backend.changerequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectRequest(
        @NotBlank(message = "A reason is required when rejecting") @Size(max = 1000) String reason
) {
}
