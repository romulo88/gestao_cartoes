package com.cartoesgestao.fatura;

import com.cartoesgestao.cartao.Cartao;
import com.cartoesgestao.cartao.CartaoRepository;
import com.cartoesgestao.compra.Compra;
import com.cartoesgestao.compra.Parcela;
import com.cartoesgestao.compra.ParcelaRepository;
import com.cartoesgestao.compra.RegraFatura;
import com.cartoesgestao.compra.dto.Ref;
import com.cartoesgestao.fatura.dto.FaturaResponse;
import com.cartoesgestao.fatura.dto.FaturasDoMesResponse;
import com.cartoesgestao.fatura.dto.ItemFaturaResponse;
import com.cartoesgestao.fatura.dto.ItemPrevistoResponse;
import com.cartoesgestao.recorrencia.Recorrencia;
import com.cartoesgestao.recorrencia.RecorrenciaRepository;
import com.cartoesgestao.recorrencia.RegraRecorrencia;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FaturaService {

    private final CartaoRepository cartaoRepository;
    private final ParcelaRepository parcelaRepository;
    private final RecorrenciaRepository recorrenciaRepository;

    public FaturaService(
            CartaoRepository cartaoRepository, ParcelaRepository parcelaRepository, RecorrenciaRepository recorrenciaRepository) {
        this.cartaoRepository = cartaoRepository;
        this.parcelaRepository = parcelaRepository;
        this.recorrenciaRepository = recorrenciaRepository;
    }

    /** Recorrentes previstos do mês, por cartão (cobranças que ainda não viraram compra). */
    private Map<Integer, List<ItemPrevistoResponse>> previstosPorCartao(Integer usuarioId, YearMonth competencia) {
        Map<Integer, List<ItemPrevistoResponse>> porCartao = new HashMap<>();
        for (Recorrencia r : recorrenciaRepository.findByUsuarioIdAndAtivaTrue(usuarioId)) {
            var cat = r.getCategoria();
            for (var p : RegraRecorrencia.previstas(
                    r.getProximaCobranca(), r.getPeriodicidade(), r.getDiaCobranca(), r.getDataFim(),
                    r.getCartao().getDiaFechamento(), r.getValor(), r.getQtdParcelas(), competencia, competencia)) {
                porCartao.computeIfAbsent(r.getCartao().getId(), k -> new ArrayList<>()).add(new ItemPrevistoResponse(
                        r.getId(), r.getDescricao(), p.dataCobranca(), p.numero(), p.qtdParcelas(), p.valor(),
                        cat == null ? null : new Ref(cat.getId(), cat.getNome(), cat.getCor())));
            }
        }
        porCartao.values().forEach(l -> l.sort(Comparator.comparing(ItemPrevistoResponse::dataCobranca)));
        return porCartao;
    }

    /** Mês que as telas abrem por padrão: a fatura ABERTA mais antiga entre os cartões
     * ativos (a que está recebendo compras hoje). Sem cartão, o mês do calendário. */
    @Transactional(readOnly = true)
    public YearMonth atual(Integer usuarioId) {
        LocalDate hoje = LocalDate.now();
        return cartaoRepository.findByUsuarioIdOrderByNomeAsc(usuarioId).stream()
                .filter(Cartao::getAtivo)
                .map(c -> RegraFatura.faturaAberta(hoje, c.getDiaFechamento()))
                .min(YearMonth::compareTo)
                .orElse(YearMonth.from(hoje));
    }

    /** Uma fatura por cartão no mês: todos os cartões ativos (mesmo zerados) e os
     * inativos que ainda tenham parcela nesse mês. */
    @Transactional(readOnly = true)
    public FaturasDoMesResponse doMes(Integer usuarioId, YearMonth competencia) {
        Map<Integer, List<Parcela>> porCartao = new LinkedHashMap<>();
        for (Parcela p : parcelaRepository.daFatura(usuarioId, competencia.atDay(1))) {
            porCartao.computeIfAbsent(p.getCompra().getCartao().getId(), k -> new ArrayList<>()).add(p);
        }
        Map<Integer, List<ItemPrevistoResponse>> previstos = previstosPorCartao(usuarioId, competencia);
        LocalDate hoje = LocalDate.now();
        List<FaturaResponse> faturas = new ArrayList<>();
        BigDecimal totalGeral = BigDecimal.ZERO;
        BigDecimal previstoGeral = BigDecimal.ZERO;
        for (Cartao cartao : cartaoRepository.findByUsuarioIdOrderByNomeAsc(usuarioId)) {
            List<Parcela> parcelas = porCartao.getOrDefault(cartao.getId(), List.of());
            List<ItemPrevistoResponse> previstosDoCartao = previstos.getOrDefault(cartao.getId(), List.of());
            if (!cartao.getAtivo() && parcelas.isEmpty() && previstosDoCartao.isEmpty()) continue;
            BigDecimal totalPrevisto = previstosDoCartao.stream().map(ItemPrevistoResponse::valor).reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal total = BigDecimal.ZERO;
            List<ItemFaturaResponse> itens = new ArrayList<>();
            for (Parcela p : parcelas) {
                Compra c = p.getCompra();
                var cat = c.getCategoria();
                itens.add(new ItemFaturaResponse(
                        c.getId(), c.getDescricao(), c.getDataCompra(), p.getNumero(), c.getQtdParcelas(), p.getValor(),
                        cat == null ? null : new Ref(cat.getId(), cat.getNome(), cat.getCor())));
                total = total.add(p.getValor());
            }
            LocalDate fechamento = RegraFatura.dataFechamento(competencia, cartao.getDiaFechamento());
            LocalDate vencimento = RegraFatura.dataVencimento(competencia, cartao.getDiaFechamento(), cartao.getDiaVencimento());
            String situacao = hoje.isBefore(fechamento) ? "aberta" : hoje.isAfter(vencimento) ? "vencida" : "fechada";
            faturas.add(new FaturaResponse(
                    new Ref(cartao.getId(), cartao.getNome(), cartao.getCor()),
                    competencia, fechamento, vencimento, situacao, total, itens, totalPrevisto, previstosDoCartao));
            totalGeral = totalGeral.add(total);
            previstoGeral = previstoGeral.add(totalPrevisto);
        }
        return new FaturasDoMesResponse(competencia, totalGeral, previstoGeral, faturas);
    }
}
