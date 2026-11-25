package cl.duoc.museo.pedidos.dto;

import jakarta.validation.constraints.NotBlank;

/** Nuevo estado de un pedido. */
public record StatusUpdateRequest(@NotBlank String status) {}
