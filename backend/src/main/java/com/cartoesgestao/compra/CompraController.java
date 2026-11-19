package com.cartoesgestao.compra;

import com.cartoesgestao.common.PaginaResponse;
import com.cartoesgestao.compra.dto.CompraRequest;
import com.cartoesgestao.compra.dto.CompraResponse;
import com.cartoesgestao.compra.dto.PrevisaoFaturaResponse;
import com.cartoesgestao.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/compras")
public class CompraController {

    private final CompraService service;

    public CompraController(CompraService service) {
        this.service = service;
    }

    @GetMapping
    public PaginaResponse<CompraResponse> listar(
            @AuthenticationPrincipal UsuarioAutenticado u,
            @RequestParam(required = false) Integer cartaoId,
            @RequestParam(required = false) Integer categoriaId,
            @RequestParam(required = false) String busca,
            @RequestParam(defaultValue = "false") boolean emAberto,
            @RequestParam(defaultValue = "0") int pagina) {
        return service.listar(u.id(), cartaoId, categoriaId, busca, emAberto, pagina);
    }

    /** Em qual fatura cai uma compra nesse cartão nessa data. */
    @GetMapping("/previsao-fatura")
    public PrevisaoFaturaResponse prever(
            @AuthenticationPrincipal UsuarioAutenticado u,
            @RequestParam Integer cartaoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataCompra) {
        return service.prever(u.id(), cartaoId, dataCompra);
    }

    @GetMapping("/{id}")
    public CompraResponse buscar(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Integer id) {
        return service.buscar(u.id(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompraResponse criar(@AuthenticationPrincipal UsuarioAutenticado u, @Valid @RequestBody CompraRequest req) {
        return service.criar(u.id(), req);
    }

    @PutMapping("/{id}")
    public CompraResponse atualizar(
            @AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Integer id, @Valid @RequestBody CompraRequest req) {
        return service.atualizar(u.id(), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Integer id) {
        service.excluir(u.id(), id);
    }
}
