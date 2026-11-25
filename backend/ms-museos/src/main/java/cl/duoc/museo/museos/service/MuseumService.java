package cl.duoc.museo.museos.service;

import cl.duoc.museo.museos.model.Museum;
import cl.duoc.museo.museos.repository.MuseumRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Lógica de negocio de las fichas de museos. */
@Service
public class MuseumService {

    private final MuseumRepository repo;

    public MuseumService(MuseumRepository repo) { this.repo = repo; }

    /** Lista museos, con filtro opcional por país o proyecto. */
    public List<Museum> list(String country, String project) {
        return repo.findAll().stream()
                .filter(m -> country == null || m.getCountry().equalsIgnoreCase(country))
                .filter(m -> project == null || m.getProject().equalsIgnoreCase(project))
                .toList();
    }

    public Museum get(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Museo no encontrado"));
    }

    /** Crea la ficha y le asigna su código QR "MUSEO:<id>". */
    public Museum create(Museum museum) {
        museum.setId(null);
        Museum saved = repo.save(museum);
        saved.setQrCode("MUSEO:" + saved.getId());
        return repo.save(saved);
    }

    public Museum update(Long id, Museum body) {
        get(id);
        body.setId(id);
        body.setQrCode("MUSEO:" + id);
        return repo.save(body);
    }

    public void delete(Long id) {
        if (!repo.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Museo no encontrado");
        repo.deleteById(id);
    }
}
