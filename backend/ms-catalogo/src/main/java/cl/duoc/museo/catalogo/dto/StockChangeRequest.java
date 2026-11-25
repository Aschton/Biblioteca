package cl.duoc.museo.catalogo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Lo envía ms-pedidos para descontar inventario al confirmar un pedido. */
public record StockChangeRequest(@NotNull @Min(1) Integer quantity) {}
