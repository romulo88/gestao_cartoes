package com.cartoesgestao.recorrencia;

import com.cartoesgestao.cartao.CartaoService;
import com.cartoesgestao.categoria.CategoriaService;
import com.cartoesgestao.common.InvalidRequestException;
import com.cartoesgestao.common.ResourceNotFoundException;
import com.cartoesgestao.compra.CompraService;
import com.cartoesgestao.compra.dto.CompraRequest;
import com.cartoesgestao.recorrencia.dto.RecorrenciaRequest;
import com.cartoesgestao.recorrencia.dto.RecorrenciaResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecorrenciaService {

    /** Máximo de cobranças geradas de uma vez para uma recorrência (data inicial muito antiga). */
    static final int MAX_POR_GERACAO = 120;

    private static final Logger log = LoggerFactory.getLogger(RecorrenciaService.class);

    private final RecorrenciaRepository repository;
    private final CartaoService cartaoService;
    private final CategoriaService categoriaService;
    private final CompraService compraService;

    public RecorrenciaService(
            RecorrenciaRepository repository,
            CartaoService cartaoService,
            CategoriaService categoriaService,
            CompraService compraService) {
        this.repository = repository;
        this.cartaoService = cartaoService;
        this.categoriaService = categoriaService;
        this.compraService = compraService;
    }

    @Transactional(readOnly = true)
    public List<RecorrenciaResponse> listar(Integer usuarioId) {
        return repository.findByUsuarioIdOrderByAtivaDescDescricaoAsc(usuarioId).stream()
                .map(RecorrenciaResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public RecorrenciaResponse buscar(Integer usuarioId, Integer id) {
        return RecorrenciaResponse.de(carregar(usuarioId, id));
    }

    /** Cria e já gera as cobranças com data até hoje (se a próxima cobrança foi informada no passado). */
    @Transactional
    public RecorrenciaResponse criar(Integer usuarioId, RecorrenciaRequest req) {
        Recorrencia r = new Recorrencia();
        r.setUsuarioId(usuarioId);
        aplicar(usuarioId, r, req);
        repository.saveAndFlush(r);
        gerar(r, LocalDate.now());
        return RecorrenciaResponse.de(r);
    }

    /** Muda só o que vem daqui para frente - compras já geradas ficam como estão. */
    @Transactional
    public RecorrenciaResponse atualizar(Integer usuarioId, Integer id, RecorrenciaRequest req) {
        carregar(usuarioId, id);
        Recorrencia r = repository.travar(id).orElseThrow();
        aplicar(usuarioId, r, req);
        gerar(r, LocalDate.now());
        return RecorrenciaResponse.de(r);
    }

    /** As compras já geradas ficam (recorrencia_id vira nulo). */
    @Transactional
    public void excluir(Integer usuarioId, Integer id) {
        repository.delete(carregar(usuarioId, id));
    }

    /** Gera as cobranças vencidas de uma recorrência (agendador). Trava a linha, então
     * duas gerações simultâneas não duplicam. */
    @Transactional
    public int gerarPendentes(Integer id, LocalDate hoje) {
        return repository.travar(id).map(r -> gerar(r, hoje)).orElse(0);
    }

    private int gerar(Recorrencia r, LocalDate hoje) {
        if (!r.getAtiva()) return 0;
        List<LocalDate> datas = RegraRecorrencia.cobrancas(
                r.getProximaCobranca(), r.getPeriodicidade(), r.getDiaCobranca(), r.getDataFim(), hoje, MAX_POR_GERACAO);
        for (LocalDate data : datas) {
            compraService.gerarDeRecorrencia(r.getUsuarioId(), r.getId(), new CompraRequest(
                    r.getCartao().getId(),
                    r.getCategoria() == null ? null : r.getCategoria().getId(),
                    r.getDescricao(),
                    r.getValor(),
                    r.getQtdParcelas(),
                    data,
                    null,
                    r.getObservacao()));
            r.setProximaCobranca(RegraRecorrencia.proxima(data, r.getPeriodicidade(), r.getDiaCobranca()));
        }
        if (!datas.isEmpty()) {
            log.info("Recorrência {} gerou {} compra(s); próxima cobrança {}", r.getId(), datas.size(), r.getProximaCobranca());
        }
        return datas.size();
    }

    private Recorrencia carregar(Integer usuarioId, Integer id) {
        return repository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Recorrência não encontrada"));
    }

    private void aplicar(Integer usuarioId, Recorrencia r, RecorrenciaRequest req) {
        short qtd = req.qtdParcelas() == null ? 1 : req.qtdParcelas();
        // Cada parcela precisa de pelo menos 1 centavo (mesma regra da compra).
        if (req.valor().movePointRight(2).compareTo(BigDecimal.valueOf(qtd)) < 0) {
            throw new InvalidRequestException("Valor baixo demais para essa quantidade de parcelas");
        }
        if (req.dataFim() != null && req.dataFim().isBefore(req.proximaCobranca())) {
            throw new InvalidRequestException("A data de fim não pode ser antes da próxima cobrança");
        }
        r.setCartao(cartaoService.buscar(usuarioId, req.cartaoId()));
        r.setCategoria(req.categoriaId() == null ? null : categoriaService.buscar(usuarioId, req.categoriaId()));
        r.setDescricao(req.descricao().trim());
        r.setValor(req.valor());
        r.setQtdParcelas(qtd);
        r.setPeriodicidade(req.periodicidade());
        // O dia nominal só muda quando a data muda: se a próxima é 28/02 porque o dia é 31,
        // salvar sem mexer na data não pode transformar o dia em 28.
        if (r.getProximaCobranca() == null || !r.getProximaCobranca().equals(req.proximaCobranca())) {
            r.setDiaCobranca((short) req.proximaCobranca().getDayOfMonth());
        }
        r.setProximaCobranca(req.proximaCobranca());
        r.setDataFim(req.dataFim());
        r.setAtiva(req.ativa() == null || req.ativa());
        r.setObservacao(req.observacao() == null || req.observacao().isBlank() ? null : req.observacao().trim());
    }
}
