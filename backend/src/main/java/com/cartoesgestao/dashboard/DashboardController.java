package com.cartoesgestao.dashboard;

import com.cartoesgestao.dashboard.dto.DashboardResponse;
import com.cartoesgestao.fatura.FaturaService;
import com.cartoesgestao.security.UsuarioAutenticado;
import java.time.YearMonth;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService service;
    private final FaturaService faturaService;

    public DashboardController(DashboardService service, FaturaService faturaService) {
        this.service = service;
        this.faturaService = faturaService;
    }

    @GetMapping
    public DashboardResponse doMes(
            @AuthenticationPrincipal UsuarioAutenticado u, @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        return service.doMes(u.id(), mes != null ? mes : faturaService.atual(u.id()));
    }
}
