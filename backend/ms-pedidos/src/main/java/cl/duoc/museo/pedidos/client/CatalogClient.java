package cl.duoc.museo.pedidos.client;

import cl.duoc.museo.pedidos.dto.ProductDto;
import cl.duoc.museo.pedidos.dto.StockChangeRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

/**
 * Cliente HTTP hacia ms-catalogo. Así ms-pedidos no accede a la base de datos del catálogo:
 * cada microservicio es dueño de sus propios datos y se comunican por REST.
 */
@Component
public class CatalogClient {

    private final RestClient http;

    public CatalogClient(@Value("${catalogo.url}") String baseUrl) {
        this.http = RestClient.builder().baseUrl(baseUrl).build();
    }

    /** @throws ResponseStatusException 400 si el producto no existe, 503 si ms-catalogo está apagado. */
    public ProductDto getProduct(Long id) {
        try {
            return http.get().uri("/api/products/{id}", id).retrieve().body(ProductDto.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Producto " + id + " no existe");
        } catch (ResourceAccessException | HttpServerErrorException e) {
            // ms-catalogo apagado, o encendido pero con su base de datos caída (503)
            throw unavailable();
        }
    }

    /** @throws ResponseStatusException 409 si no hay stock suficiente. */
    public void decreaseStock(Long id, int quantity) {
        try {
            http.post().uri("/api/products/{id}/decrease-stock", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new StockChangeRequest(quantity))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.Conflict e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sin stock suficiente para el producto " + id);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Producto " + id + " no existe");
        } catch (ResourceAccessException | HttpServerErrorException e) {
            // ms-catalogo apagado, o encendido pero con su base de datos caída (503)
            throw unavailable();
        }
    }

    private static ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ms-catalogo no está disponible (puerto 8082) o su base de datos está apagada");
    }
}
