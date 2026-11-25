package cl.duoc.museo.pedidos.dto;

/** Datos del producto que entrega ms-catalogo (solo los campos que necesita este servicio). */
public record ProductDto(Long id, String name, String category, Double price, Integer stock, boolean active) {
    public boolean isCourse() { return "CURSO".equals(category); }
}
