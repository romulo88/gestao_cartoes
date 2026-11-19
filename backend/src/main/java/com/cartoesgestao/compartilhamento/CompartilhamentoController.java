package com.cartoesgestao.compartilhamento;

import com.cartoesgestao.compartilhamento.dto.CompartilhamentoDtos.CompartilhamentoResponse;
import com.cartoesgestao.compartilhamento.dto.CompartilhamentoDtos.CriadoResponse;
import com.cartoesgestao.compartilhamento.dto.CompartilhamentoDtos.CriarRequest;
import com.cartoesgestao.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Gerência dos links do usuário logado. A visualização em si fica em {@link PainelPublicoController}. */
@RestController
@RequestMapping("/api/compartilhamentos")
public class CompartilhamentoController {

    private final CompartilhamentoService service;

    public CompartilhamentoController(CompartilhamentoService service) {
        this.service = service;
    }

    @GetMapping
    public List<CompartilhamentoResponse> listar(@AuthenticationPrincipal UsuarioAutenticado u) {
        return service.listar(u.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CriadoResponse criar(@AuthenticationPrincipal UsuarioAutenticado u, @Valid @RequestBody CriarRequest req) {
        return service.criar(u.id(), req.nome());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revogar(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Integer id) {
        service.revogar(u.id(), id);
    }
}
