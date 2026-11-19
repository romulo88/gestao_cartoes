package com.cartoesgestao.fatura;

import com.cartoesgestao.fatura.dto.FaturasDoMesResponse;
import com.cartoesgestao.security.UsuarioAutenticado;
import java.time.YearMonth;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/faturas")
public class FaturaController {

    private final FaturaService service;

    public FaturaController(FaturaService service) {
        this.service = service;
    }

    /** Mês da fatura em aberto hoje ({"mes": "2026-12"}) - onde Painel e Faturas abrem. */
    @GetMapping("/atual")
    public Map<String, YearMonth> atual(@AuthenticationPrincipal UsuarioAutenticado u) {
        return Map.of("mes", service.atual(u.id()));
    }

    /** {@code mes} no formato 2026-12; sem ele, a fatura em aberto hoje. */
    @GetMapping
    public FaturasDoMesResponse doMes(
            @AuthenticationPrincipal UsuarioAutenticado u, @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        return service.doMes(u.id(), mes != null ? mes : service.atual(u.id()));
    }
}
