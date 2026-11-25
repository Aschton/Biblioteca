package cl.duoc.museo.catalogo.service;

import cl.duoc.museo.catalogo.dto.QrResolveDto;
import cl.duoc.museo.catalogo.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Resuelve códigos QR de productos ("PROD:<id>"). Los "MUSEO:<id>" los resuelve ms-museos. */
@Service
public class QrService {

    private final ProductRepository products;

    public QrService(ProductRepository products) { this.products = products; }

    public QrResolveDto resolve(String code) {
        if (!code.startsWith("PROD:"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este servicio solo resuelve códigos PROD:<id>");
        return products.findByQrCode(code)
                .map(p -> new QrResolveDto(code, "PRODUCT", p.getId(), p.getName(), p.getDescription()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Código desconocido"));
    }
}
