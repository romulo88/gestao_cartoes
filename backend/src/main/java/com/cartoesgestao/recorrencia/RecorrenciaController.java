package com.cartoesgestao.recorrencia;

import com.cartoesgestao.recorrencia.dto.RecorrenciaRequest;
import com.cartoesgestao.recorrencia.dto.RecorrenciaResponse;
import com.cartoesgestao.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recorrencias")
public class RecorrenciaController {

    private final RecorrenciaService service;

    public RecorrenciaController(RecorrenciaService service) {
        this.service = service;
    }

    @GetMapping
    public List<RecorrenciaResponse> listar(@AuthenticationPrincipal UsuarioAutenticado u) {
        return service.listar(u.id());
    }

    @GetMapping("/{id}")
    public RecorrenciaResponse buscar(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Integer id) {
        return service.buscar(u.id(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecorrenciaResponse criar(@AuthenticationPrincipal UsuarioAutenticado u, @Valid @RequestBody RecorrenciaRequest req) {
        return service.criar(u.id(), req);
    }

    @PutMapping("/{id}")
    public RecorrenciaResponse atualizar(
            @AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Integer id, @Valid @RequestBody RecorrenciaRequest req) {
        return service.atualizar(u.id(), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Integer id) {
        service.excluir(u.id(), id);
    }
}
