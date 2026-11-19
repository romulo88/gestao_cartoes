package com.cartoesgestao.dashboard;

import com.cartoesgestao.cartao.Cartao;
import com.cartoesgestao.cartao.CartaoRepository;
import com.cartoesgestao.categoria.Categoria;
import com.cartoesgestao.categoria.CategoriaRepository;
import com.cartoesgestao.compra.ParcelaRepository;
import com.cartoesgestao.compra.RegraFatura;
import com.cartoesgestao.compra.dto.Ref;
import com.cartoesgestao.dashboard.dto.DashboardResponse;
import com.cartoesgestao.dashboard.dto.DashboardResponse.CartaoNoMes;
import com.cartoesgestao.dashboard.dto.DashboardResponse.CategoriaNoMes;
import com.cartoesgestao.dashboard.dto.DashboardResponse.PontoSerie;
import com.cartoesgestao.dashboard.dto.TotalAgrupado;
import com.cartoesgestao.dashboard.dto.TotalPorChave;
import com.cartoesgestao.recorrencia.Recorrencia;
import com.cartoesgestao.recorrencia.RecorrenciaRepository;
import com.cartoesgestao.recorrencia.RegraRecorrencia;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private static final int MESES_ANTES = 6;
    private static final int MESES_DEPOIS = 6;

    private final CartaoRepository cartaoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ParcelaRepository parcelaRepository;
    private final RecorrenciaRepository recorrenciaRepository;

    public DashboardService(
            CartaoRepository cartaoRepository,
            CategoriaRepository categoriaRepository,
            ParcelaRepository parcelaRepository,
            RecorrenciaRepository recorrenciaRepository) {
        this.cartaoRepository = cartaoRepository;
        this.categoriaRepository = categoriaRepository;
        this.parcelaRepository = parcelaRepository;
        this.recorrenciaRepository = recorrenciaRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse doMes(Integer usuarioId, YearMonth mes) {
        List<Cartao> cartoes = cartaoRepository.findByUsuarioIdOrderByNomeAsc(usuarioId);

        // Série: uma consulta só para os 13 meses, agrupada por cartão e mês.
        YearMonth inicio = mes.minusMonths(MESES_ANTES);
        YearMonth fim = mes.plusMonths(MESES_DEPOIS);
        Map<YearMonth, Map<Integer, BigDecimal>> porMes = new HashMap<>();
        for (TotalAgrupado t : parcelaRepository.totaisPorCartaoEMes(usuarioId, inicio.atDay(1), fim.atDay(1))) {
            porMes.computeIfAbsent(YearMonth.from(t.competencia()), k -> new HashMap<>()).put(t.chave(), t.total());
        }
        Map<YearMonth, BigDecimal> previsto = previstoRecorrente(usuarioId, inicio, fim);
        List<PontoSerie> serie = new ArrayList<>();
        for (YearMonth m = inicio; !m.isAfter(fim); m = m.plusMonths(1)) {
            Map<Integer, BigDecimal> doMes = porMes.getOrDefault(m, Map.of());
            List<TotalPorChave> porCartao = doMes.entrySet().stream()
                    .map(e -> new TotalPorChave(e.getKey(), e.getValue()))
                    .sorted(Comparator.comparing(TotalPorChave::chave))
                    .toList();
            serie.add(new PontoSerie(m, soma(doMes.values()), porCartao, previsto.getOrDefault(m, BigDecimal.ZERO)));
        }

        Map<Integer, BigDecimal> totaisDoMes = porMes.getOrDefault(mes, Map.of());
        BigDecimal totalMes = soma(totaisDoMes.values());
        BigDecimal totalAnterior = soma(porMes.getOrDefault(mes.minusMonths(1), Map.of()).values());
        // Comprometido futuro: só a parte da série depois do mês (a série cobre 6 meses à
        // frente; parcelas além disso também contam, por isso consulta própria).
        BigDecimal comprometidoFuturo = soma(parcelaRepository.emAbertoPorCartao(usuarioId, mes.plusMonths(1).atDay(1))
                .stream().map(TotalPorChave::total).toList());

        // Limite: o que ainda vai sair de cada cartão - parcelas das faturas que não venceram
        // (cada cartão tem a sua, fecham em dias diferentes). Independe do mês escolhido.
        LocalDate hoje = LocalDate.now();
        Map<Integer, YearMonth> desde = new HashMap<>();
        for (Cartao c : cartoes) {
            desde.put(c.getId(), RegraFatura.primeiraNaoVencida(hoje, c.getDiaFechamento(), c.getDiaVencimento()));
        }
        Map<Integer, BigDecimal> emAberto = new HashMap<>();
        YearMonth menor = desde.values().stream().min(Comparator.naturalOrder()).orElse(YearMonth.from(hoje));
        for (TotalAgrupado t : parcelaRepository.totaisPorCartaoEMes(usuarioId, menor.atDay(1), LocalDate.of(2200, 1, 1))) {
            YearMonth d = desde.get(t.chave());
            if (d != null && !YearMonth.from(t.competencia()).isBefore(d)) emAberto.merge(t.chave(), t.total(), BigDecimal::add);
        }

        List<CartaoNoMes> porCartao = new ArrayList<>();
        for (Cartao c : cartoes) {
            BigDecimal noMes = totaisDoMes.getOrDefault(c.getId(), BigDecimal.ZERO);
            if (!c.getAtivo() && noMes.signum() == 0) continue;
            BigDecimal aberto = emAberto.getOrDefault(c.getId(), BigDecimal.ZERO);
            porCartao.add(new CartaoNoMes(
                    new Ref(c.getId(), c.getNome(), c.getCor()),
                    noMes,
                    c.getLimite(),
                    aberto,
                    c.getLimite() == null ? null : c.getLimite().subtract(aberto),
                    RegraFatura.dataFechamento(mes, c.getDiaFechamento()),
                    RegraFatura.dataVencimento(mes, c.getDiaFechamento(), c.getDiaVencimento())));
        }

        Map<Integer, Categoria> categorias = categoriaRepository.findByUsuarioIdOrderByNomeAsc(usuarioId).stream()
                .collect(Collectors.toMap(Categoria::getId, Function.identity()));
        Map<Integer, BigDecimal> porCategoriaTotal = new HashMap<>();
        BigDecimal semCategoria = BigDecimal.ZERO;
        for (TotalAgrupado t : parcelaRepository.totaisPorCategoria(usuarioId, mes.atDay(1))) {
            if (t.chave() == null) semCategoria = t.total();
            else porCategoriaTotal.put(t.chave(), t.total());
        }
        List<CategoriaNoMes> porCategoria = new ArrayList<>();
        for (Categoria cat : categorias.values()) {
            BigDecimal total = porCategoriaTotal.getOrDefault(cat.getId(), BigDecimal.ZERO);
            // Categoria sem gasto só aparece se tiver limite (para mostrar a barra vazia).
            if (total.signum() == 0 && cat.getLimiteMensal() == null) continue;
            porCategoria.add(new CategoriaNoMes(new Ref(cat.getId(), cat.getNome(), cat.getCor()), total, cat.getLimiteMensal()));
        }
        if (semCategoria.signum() > 0) {
            porCategoria.add(new CategoriaNoMes(new Ref(null, "Sem categoria", "#94a3b8"), semCategoria, null));
        }
        porCategoria.sort(Comparator.comparing(CategoriaNoMes::total).reversed());

        return new DashboardResponse(mes, totalMes, totalAnterior, comprometidoFuturo, porCartao, porCategoria, serie);
    }

    /** Parcelas das cobranças recorrentes futuras (ainda não geradas) por fatura, de
     * {@code inicio} a {@code fim}. As já geradas viraram compra e estão no total normal. */
    private Map<YearMonth, BigDecimal> previstoRecorrente(Integer usuarioId, YearMonth inicio, YearMonth fim) {
        Map<YearMonth, BigDecimal> previsto = new HashMap<>();
        for (Recorrencia r : recorrenciaRepository.findByUsuarioIdAndAtivaTrue(usuarioId)) {
            for (var p : RegraRecorrencia.previstas(
                    r.getProximaCobranca(), r.getPeriodicidade(), r.getDiaCobranca(), r.getDataFim(),
                    r.getCartao().getDiaFechamento(), r.getValor(), r.getQtdParcelas(), inicio, fim)) {
                previsto.merge(p.competencia(), p.valor(), BigDecimal::add);
            }
        }
        return previsto;
    }

    private static BigDecimal soma(java.util.Collection<BigDecimal> valores) {
        return valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
