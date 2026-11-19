package com.cartoesgestao.compartilhamento;

import com.cartoesgestao.common.ResourceNotFoundException;
import com.cartoesgestao.compartilhamento.dto.CompartilhamentoDtos.InfoPublicaResponse;
import com.cartoesgestao.dashboard.DashboardService;
import com.cartoesgestao.dashboard.dto.DashboardResponse;
import com.cartoesgestao.fatura.FaturaService;
import com.cartoesgestao.fatura.dto.FaturasDoMesResponse;
import com.cartoesgestao.usuario.UsuarioRepository;
import java.time.YearMonth;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

/**
 * Painel somente leitura para quem tem um link de compartilhamento (sem login). Só GET, só
 * o que o painel mostra. O token vem no header {@value #HEADER} (o frontend lê do {@code #}
 * da URL), para não aparecer em log de acesso de proxy/servidor.
 */
@RestController
@RequestMapping("/api/publico/painel")
public class PainelPublicoController {

    static final String HEADER = "X-Painel-Token";

    private final CompartilhamentoService compartilhamentoService;
    private final DashboardService dashboardService;
    private final FaturaService faturaService;
    private final UsuarioRepository usuarioRepository;

    public PainelPublicoController(
            CompartilhamentoService compartilhamentoService,
            DashboardService dashboardService,
            FaturaService faturaService,
            UsuarioRepository usuarioRepository) {
        this.compartilhamentoService = compartilhamentoService;
        this.dashboardService = dashboardService;
        this.faturaService = faturaService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/info")
    public InfoPublicaResponse info(@RequestHeader(value = HEADER, required = false) String token) {
        Integer usuarioId = compartilhamentoService.resolver(token);
        String nome = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Link inválido ou revogado."))
                .getNome().trim();
        String primeiroNome = nome.contains(" ") ? nome.substring(0, nome.indexOf(' ')) : nome;
        return new InfoPublicaResponse(primeiroNome, faturaService.atual(usuarioId));
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(
            @RequestHeader(value = HEADER, required = false) String token,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        Integer usuarioId = compartilhamentoService.resolver(token);
        return dashboardService.doMes(usuarioId, mes != null ? mes : faturaService.atual(usuarioId));
    }

    /** Itens da fatura do mês - o painel usa para o hint de cada categoria. */
    @GetMapping("/faturas")
    public FaturasDoMesResponse faturas(
            @RequestHeader(value = HEADER, required = false) String token,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        Integer usuarioId = compartilhamentoService.resolver(token);
        return faturaService.doMes(usuarioId, mes != null ? mes : faturaService.atual(usuarioId));
    }
}
