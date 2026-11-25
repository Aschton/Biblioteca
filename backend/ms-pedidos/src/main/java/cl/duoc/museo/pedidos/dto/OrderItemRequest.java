package cl.duoc.museo.pedidos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Línea de un pedido: producto y cantidad. */
public record OrderItemRequest(@NotNull Long productId, @NotNull @Min(1) Integer quantity) {}
