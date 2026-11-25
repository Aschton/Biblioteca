package cl.duoc.museo.museos.controller;

import cl.duoc.museo.museos.model.Notice;
import cl.duoc.museo.museos.service.NoticeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Endpoints REST de avisos. */
@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService service;

    public NoticeController(NoticeService service) { this.service = service; }

    @GetMapping
    public List<Notice> list() { return service.list(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Notice create(@Valid @RequestBody Notice notice) { return service.create(notice); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }
}
