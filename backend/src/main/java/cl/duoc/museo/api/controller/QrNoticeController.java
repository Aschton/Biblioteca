package cl.duoc.museo.api.controller;

import cl.duoc.museo.api.dto.Dtos.QrResolve;
import cl.duoc.museo.api.model.Notice;
import cl.duoc.museo.api.repository.MuseumRepository;
import cl.duoc.museo.api.repository.NoticeRepository;
import cl.duoc.museo.api.repository.ProductRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class QrNoticeController {

    private final MuseumRepository museums;
    private final ProductRepository products;
    private final NoticeRepository notices;

    public QrNoticeController(MuseumRepository museums, ProductRepository products, NoticeRepository notices) {
        this.museums = museums;
        this.products = products;
        this.notices = notices;
    }

    /** Resuelve un código QR ("MUSEO:<id>" o "PROD:<id>") a su ficha. */
    @GetMapping("/qr/{code}")
    public QrResolve resolve(@PathVariable String code) {
        if (code.startsWith("MUSEO:")) {
            return museums.findByQrCode(code)
                    .map(m -> new QrResolve(code, "MUSEUM", m.getId(), m.getName(), m.getDescription()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Código desconocido"));
        }
        if (code.startsWith("PROD:")) {
            return products.findByQrCode(code)
                    .map(p -> new QrResolve(code, "PRODUCT", p.getId(), p.getName(), p.getDescription()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Código desconocido"));
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de código inválido");
    }

    @GetMapping("/notices")
    public List<Notice> notices() { return notices.findAllByOrderByCreatedAtDesc(); }

    /** El Administrador publica avisos informativos de prueba. */
    @PostMapping("/notices")
    @ResponseStatus(HttpStatus.CREATED)
    public Notice create(@Valid @RequestBody Notice notice) {
        if (notice.getCreatedAt() == null) notice.setCreatedAt(System.currentTimeMillis());
        return notices.save(notice);
    }
}
