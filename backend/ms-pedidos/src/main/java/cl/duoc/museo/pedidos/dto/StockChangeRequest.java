package cl.duoc.museo.pedidos.dto;

/** Cuerpo que se envía a ms-catalogo para descontar stock. */
public record StockChangeRequest(Integer quantity) {}
