package cl.duoc.museo.pedidos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/** Pedido o solicitud de información que envía la app. */
public record OrderRequest(
        @NotBlank @Email String userEmail,
        @NotBlank String userName,
        @Valid List<OrderItemRequest> items,
        @NotBlank String country,
        @NotBlank String shippingType,
        String kind
) {}
