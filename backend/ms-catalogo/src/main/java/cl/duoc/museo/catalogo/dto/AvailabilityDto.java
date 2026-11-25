package cl.duoc.museo.catalogo.dto;

/** Respuesta de disponibilidad de un producto. */
public record AvailabilityDto(Long productId, boolean available, int stock) {}
