package com.paywave.auth.client;

import java.util.UUID;

public record CreateWalletRequest(UUID ownerId, String ownerType, String currency, UUID notifyUserId) {
}
