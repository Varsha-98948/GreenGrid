package com.greengrid.dto.friend;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SendFriendRequest(
        @NotNull(message = "Recipient user ID is required")
        UUID toUserId
) {
}
