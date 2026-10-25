package cl.duoc.museo.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** DTOs de entrada/salida (mismos nombres de campo que usa la app Android). */
public final class Dtos {
    private Dtos() {}

    public record OrderItemRequest(@NotNull Long productId, @NotNull @Min(1) Integer quantity) {}

    public record OrderRequest(
            @NotBlank @Email String userEmail,
            @NotBlank String userName,
            @Valid List<OrderItemRequest> items,
            @NotBlank String country,
            @NotBlank String shippingType,
            String kind
    ) {}

    public record StatusUpdate(@NotBlank String status) {}

    public record ShippingQuoteRequest(@NotBlank String country, @NotBlank String shippingType, @Valid List<OrderItemRequest> items) {}

    public record ShippingQuote(double subtotal, double shipping, double total, String estimatedDays) {}

    public record Availability(Long productId, boolean available, int stock) {}

    public record QrResolve(String code, String type, Long id, String title, String description) {}

    public record ApiError(int status, String message) {}
}
