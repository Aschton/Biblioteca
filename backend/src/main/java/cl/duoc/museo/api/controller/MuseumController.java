package cl.duoc.museo.api.controller;

import cl.duoc.museo.api.model.Museum;
import cl.duoc.museo.api.repository.MuseumRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/museums")
public class MuseumController {

    private final MuseumRepository repo;

    public MuseumController(MuseumRepository repo) { this.repo = repo; }

    /** Lista museos, con filtro opcional por país o proyecto. */
    @GetMapping
    public List<Museum> list(@RequestParam(required = false) String country,
                             @RequestParam(required = false) String project) {
        return repo.findAll().stream()
                .filter(m -> country == null || m.getCountry().equalsIgnoreCase(country))
                .filter(m -> project == null || m.getProject().equalsIgnoreCase(project))
                .toList();
    }

    @GetMapping("/{id}")
    public Museum get(@PathVariable Long id) {
        return repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Museo no encontrado"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Museum create(@Valid @RequestBody Museum museum) {
        museum.setId(null);
        Museum saved = repo.save(museum);
        saved.setQrCode("MUSEO:" + saved.getId());
        return repo.save(saved);
    }

    @PutMapping("/{id}")
    public Museum update(@PathVariable Long id, @Valid @RequestBody Museum body) {
        get(id);
        body.setId(id);
        body.setQrCode("MUSEO:" + id);
        return repo.save(body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { repo.deleteById(id); }
}
