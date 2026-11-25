package cl.duoc.museo.pedidos.dto;

/** Resultado de la cotización de envío. */
public record ShippingQuoteResponse(double subtotal, double shipping, double total, String estimatedDays) {}
