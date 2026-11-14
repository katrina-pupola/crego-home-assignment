package com.crego.backend.auth;

import com.crego.backend.common.MaxUtf8Bytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 255) String fullName,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank
        @Size(min = 12, message = "Password must be at least 12 characters long")
        @MaxUtf8Bytes(value = 72, message = "Password is too long") // BCrypt uses at most 72 bytes
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).*$",
                message = "Password must contain an uppercase letter, a lowercase letter, a digit and a special character")
        String password
) {
}
