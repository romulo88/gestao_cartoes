package com.cartoesgestao.categoria;

import com.cartoesgestao.categoria.dto.CategoriaRequest;
import com.cartoesgestao.categoria.dto.CategoriaResponse;
import com.cartoesgestao.categoria.dto.SugestaoCategoriaResponse;
import com.cartoesgestao.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService service;

    public CategoriaController(CategoriaService service) {
        this.service = service;
    }

    @GetMapping
    public List<CategoriaResponse> listar(@AuthenticationPrincipal UsuarioAutenticado u) {
        return service.listar(u.id());
    }

    /** Sugestão para a tela de lançamento (o frontend chama enquanto a descrição é digitada). */
    @GetMapping("/sugestao")
    public SugestaoCategoriaResponse sugerir(@AuthenticationPrincipal UsuarioAutenticado u, @RequestParam String descricao) {
        return service.sugerir(u.id(), descricao);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaResponse criar(@AuthenticationPrincipal UsuarioAutenticado u, @Valid @RequestBody CategoriaRequest req) {
        return service.criar(u.id(), req);
    }

    @PutMapping("/{id}")
    public CategoriaResponse atualizar(
            @AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Integer id, @Valid @RequestBody CategoriaRequest req) {
        return service.atualizar(u.id(), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Integer id) {
        service.excluir(u.id(), id);
    }
}
