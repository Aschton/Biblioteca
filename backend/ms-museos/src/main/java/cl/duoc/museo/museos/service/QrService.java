package cl.duoc.museo.museos.service;

import cl.duoc.museo.museos.dto.QrResolveDto;
import cl.duoc.museo.museos.repository.MuseumRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Resuelve códigos QR de museos ("MUSEO:<id>"). Los "PROD:<id>" los resuelve ms-catalogo. */
@Service
public class QrService {

    private final MuseumRepository museums;

    public QrService(MuseumRepository museums) { this.museums = museums; }

    public QrResolveDto resolve(String code) {
        if (!code.startsWith("MUSEO:"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este servicio solo resuelve códigos MUSEO:<id>");
        return museums.findByQrCode(code)
                .map(m -> new QrResolveDto(code, "MUSEUM", m.getId(), m.getName(), m.getDescription()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Código desconocido"));
    }
}
