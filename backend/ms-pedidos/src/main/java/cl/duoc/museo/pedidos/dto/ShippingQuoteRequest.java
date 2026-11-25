package cl.duoc.museo.pedidos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/** Datos para cotizar un envío. */
public record ShippingQuoteRequest(@NotBlank String country, @NotBlank String shippingType, @Valid List<OrderItemRequest> items) {}
