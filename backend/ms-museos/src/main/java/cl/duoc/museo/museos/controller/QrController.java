package cl.duoc.museo.museos.controller;

import cl.duoc.museo.museos.dto.QrResolveDto;
import cl.duoc.museo.museos.service.QrService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint REST para resolver códigos QR de museos. */
@RestController
@RequestMapping("/api/qr")
public class QrController {

    private final QrService service;

    public QrController(QrService service) { this.service = service; }

    @GetMapping("/{code}")
    public QrResolveDto resolve(@PathVariable String code) { return service.resolve(code); }
}
