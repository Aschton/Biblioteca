package cl.duoc.museo.museos.dto;

/** Respuesta al resolver un código QR. type = MUSEUM. */
public record QrResolveDto(String code, String type, Long id, String title, String description) {}
