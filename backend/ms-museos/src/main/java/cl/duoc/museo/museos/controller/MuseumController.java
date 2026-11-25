package cl.duoc.museo.museos.controller;

import cl.duoc.museo.museos.model.Museum;
import cl.duoc.museo.museos.service.MuseumService;
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

import java.util.List;

/** Endpoints REST del CRUD de museos. La lógica está en MuseumService. */
@RestController
@RequestMapping("/api/museums")
public class MuseumController {

    private final MuseumService service;

    public MuseumController(MuseumService service) { this.service = service; }

    @GetMapping
    public List<Museum> list(@RequestParam(required = false) String country,
                             @RequestParam(required = false) String project) {
        return service.list(country, project);
    }

    @GetMapping("/{id}")
    public Museum get(@PathVariable Long id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Museum create(@Valid @RequestBody Museum museum) { return service.create(museum); }

    @PutMapping("/{id}")
    public Museum update(@PathVariable Long id, @Valid @RequestBody Museum body) { return service.update(id, body); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }
}
