package com.cartoesgestao.cartao;

import com.cartoesgestao.cartao.dto.CartaoRequest;
import com.cartoesgestao.cartao.dto.CartaoResponse;
import com.cartoesgestao.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cartoes")
public class CartaoController {

    private final CartaoService service;

    public CartaoController(CartaoService service) {
        this.service = service;
    }

    @GetMapping
    public List<CartaoResponse> listar(@AuthenticationPrincipal UsuarioAutenticado u) {
        return service.listar(u.id());
    }

    @GetMapping("/{id}")
    public CartaoResponse buscar(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Integer id) {
        return CartaoResponse.de(service.buscar(u.id(), id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CartaoResponse criar(@AuthenticationPrincipal UsuarioAutenticado u, @Valid @RequestBody CartaoRequest req) {
        return service.criar(u.id(), req);
    }

    @PutMapping("/{id}")
    public CartaoResponse atualizar(
            @AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Integer id, @Valid @RequestBody CartaoRequest req) {
        return service.atualizar(u.id(), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Integer id) {
        service.excluir(u.id(), id);
    }
}
