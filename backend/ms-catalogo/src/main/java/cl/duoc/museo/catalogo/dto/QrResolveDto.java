package cl.duoc.museo.catalogo.dto;

/** Respuesta al resolver un código QR. type = PRODUCT. */
public record QrResolveDto(String code, String type, Long id, String title, String description) {}
