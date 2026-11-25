package cl.duoc.museo.museos.service;

import cl.duoc.museo.museos.model.Notice;
import cl.duoc.museo.museos.repository.NoticeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Avisos informativos publicados por el Administrador. */
@Service
public class NoticeService {

    private final NoticeRepository repo;

    public NoticeService(NoticeRepository repo) { this.repo = repo; }

    public List<Notice> list() { return repo.findAllByOrderByCreatedAtDesc(); }

    public Notice create(Notice notice) {
        if (notice.getCreatedAt() == null) notice.setCreatedAt(System.currentTimeMillis());
        return repo.save(notice);
    }

    public void delete(Long id) {
        if (!repo.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aviso no encontrado");
        repo.deleteById(id);
    }
}
