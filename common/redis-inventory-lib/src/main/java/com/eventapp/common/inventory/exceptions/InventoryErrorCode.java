package com.eventapp.common.inventory.exceptions;

import com.eventapp.sharedutils.exceptions.enums.IErrorCode;

public enum InventoryErrorCode implements IErrorCode {
    CAPACITY_TOO_LOW("EVT_400", "New capacity cannot be lower than tickets already sold", 400),
    RESERVATION_QUANTITY_MISMATCH("EVT_400", "The number of tickets being confirmed does not match the reservation.", 400),

    CANNOT_DELETE_SOLD_EVENT("EVT_403", "Cannot delete event %s: It has active sales or reservations. Please cancel the event instead.", 403),

    EVENT_NOT_OPEN_FOR_RESALE("EVT_409_NOT_PUBLISHED", "Event is not published or is cancelled", 409),
    RESERVATION_LOCK_FAILED("EVT_409_LOCK", "Failed to acquire reservation lock. Please try again.", 409),
    EVENT_ALREADY_CANCELED("EVT_409_CANCELLED", "Event is already in CANCELED status", 409),
    DUPLICATE_RESERVATION("BK_409", "You already have an active reservation for event %s. Please complete or cancel it before trying again.", 409),

    EVENT_NOT_FOUND_IN_REDIS("EVT_404", "Inventory data for event %s could not be found in Redis. It may have expired or was never initialized.", 404),
    EVENT_SOLD_OUT("EVT_410", "This event is sold out or all remaining tickets are in checkout.", 410),
    RESERVATION_EXPIRED("EVT_410", "Reservation for event %s has expired or is invalid.", 410),

    INVENTORY_INTEGRITY_FAILURE("INV_500", "Critical error: Inventory synchronization failed for event %s.", 500),
    INVALID_INVENTORY_STATE("EVT_500", "Critical error: No reserved tickets found for event %s in Redis.", 500),
    EVENT_OPERATION_FAILED("EVT_500_OPERATION_FAILED", "Unexpected inventory operation failure", 500);

    private final String code;
    private final String message;
    private final int httpStatus;

    InventoryErrorCode(String code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String formatMessage(Object... args) {
        if (args == null || args.length == 0)
            return getMessage();

        return String.format(getMessage(), args);
    }
}