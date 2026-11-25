package cl.duoc.museo.catalogo.controller;

import cl.duoc.museo.catalogo.dto.QrResolveDto;
import cl.duoc.museo.catalogo.service.QrService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint REST para resolver códigos QR de productos. */
@RestController
@RequestMapping("/api/qr")
public class QrController {

    private final QrService service;

    public QrController(QrService service) { this.service = service; }

    @GetMapping("/{code}")
    public QrResolveDto resolve(@PathVariable String code) { return service.resolve(code); }
}
