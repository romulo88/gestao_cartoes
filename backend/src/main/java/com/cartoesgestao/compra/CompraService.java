package com.cartoesgestao.compra;

import com.cartoesgestao.cartao.Cartao;
import com.cartoesgestao.cartao.CartaoService;
import com.cartoesgestao.categoria.CategoriaService;
import com.cartoesgestao.common.InvalidRequestException;
import com.cartoesgestao.common.PaginaResponse;
import com.cartoesgestao.common.ResourceNotFoundException;
import com.cartoesgestao.compra.dto.CompraRequest;
import com.cartoesgestao.compra.dto.CompraResponse;
import com.cartoesgestao.compra.dto.PrevisaoFaturaResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompraService {

    static final int TAMANHO_PAGINA = 30;

    private final CompraRepository repository;
    private final CartaoService cartaoService;
    private final CategoriaService categoriaService;

    public CompraService(CompraRepository repository, CartaoService cartaoService, CategoriaService categoriaService) {
        this.repository = repository;
        this.cartaoService = cartaoService;
        this.categoriaService = categoriaService;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<CompraResponse> listar(
            Integer usuarioId, Integer cartaoId, Integer categoriaId, String busca, boolean somenteEmAberto, int pagina) {
        Specification<Compra> spec = CompraSpecs.doUsuario(usuarioId);
        if (cartaoId != null) spec = spec.and(CompraSpecs.doCartao(cartaoId));
        if (categoriaId != null) spec = spec.and(CompraSpecs.daCategoria(categoriaId));
        if (busca != null && !busca.isBlank()) spec = spec.and(CompraSpecs.descricaoContem(busca));
        LocalDate hoje = LocalDate.now();
        if (somenteEmAberto) {
            // Cada cartão tem o seu "daqui em diante" (fecham em dias diferentes).
            Map<Integer, LocalDate> desdePorCartao = new HashMap<>();
            for (Cartao cartao : cartaoService.todos(usuarioId)) {
                desdePorCartao.put(cartao.getId(), RegraFatura
                        .primeiraNaoVencida(hoje, cartao.getDiaFechamento(), cartao.getDiaVencimento()).atDay(1));
            }
            spec = spec.and(CompraSpecs.comParcelaAVencer(desdePorCartao));
        }

        Page<Compra> page = repository.findAll(spec, PageRequest.of(Math.max(pagina, 0), TAMANHO_PAGINA,
                Sort.by(Sort.Order.desc("dataCompra"), Sort.Order.desc("id"))));
        List<CompraResponse> itens = page.getContent().stream().map(c -> CompraResponse.de(c, hoje)).toList();
        return new PaginaResponse<>(itens, page.getNumber(), page.getTotalPages(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public CompraResponse buscar(Integer usuarioId, Integer id) {
        return CompraResponse.de(carregar(usuarioId, id), LocalDate.now());
    }

    @Transactional(readOnly = true)
    public PrevisaoFaturaResponse prever(Integer usuarioId, Integer cartaoId, LocalDate dataCompra) {
        Cartao cartao = cartaoService.buscar(usuarioId, cartaoId);
        YearMonth comp = RegraFatura.competenciaDaCompra(dataCompra, cartao.getDiaFechamento());
        return new PrevisaoFaturaResponse(
                comp,
                RegraFatura.dataFechamento(comp, cartao.getDiaFechamento()),
                RegraFatura.dataVencimento(comp, cartao.getDiaFechamento(), cartao.getDiaVencimento()));
    }

    @Transactional
    public CompraResponse criar(Integer usuarioId, CompraRequest req) {
        Compra c = new Compra();
        c.setUsuarioId(usuarioId);
        aplicar(usuarioId, c, req);
        return CompraResponse.de(repository.save(c), LocalDate.now());
    }

    /** Compra gerada por uma recorrência (chamado dentro da transação da geração). */
    public Compra gerarDeRecorrencia(Integer usuarioId, Integer recorrenciaId, CompraRequest req) {
        Compra c = new Compra();
        c.setUsuarioId(usuarioId);
        c.setRecorrenciaId(recorrenciaId);
        aplicar(usuarioId, c, req);
        return repository.save(c);
    }

    /** Editar regera todas as parcelas a partir dos dados novos. */
    @Transactional
    public CompraResponse atualizar(Integer usuarioId, Integer id, CompraRequest req) {
        Compra c = carregar(usuarioId, id);
        aplicar(usuarioId, c, req);
        repository.flush();
        return CompraResponse.de(c, LocalDate.now());
    }

    @Transactional
    public void excluir(Integer usuarioId, Integer id) {
        repository.delete(carregar(usuarioId, id));
    }

    private Compra carregar(Integer usuarioId, Integer id) {
        return repository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Compra não encontrada"));
    }

    private void aplicar(Integer usuarioId, Compra c, CompraRequest req) {
        // Cada parcela precisa de pelo menos 1 centavo (CHECK valor > 0 no banco).
        if (req.valorTotal().movePointRight(2).compareTo(BigDecimal.valueOf(req.qtdParcelas())) < 0) {
            throw new InvalidRequestException("Valor baixo demais para essa quantidade de parcelas");
        }
        Cartao cartao = cartaoService.buscar(usuarioId, req.cartaoId());
        c.setCartao(cartao);
        c.setCategoria(req.categoriaId() == null ? null : categoriaService.buscar(usuarioId, req.categoriaId()));
        c.setDescricao(req.descricao().trim());
        c.setValorTotal(req.valorTotal());
        c.setQtdParcelas(req.qtdParcelas());
        c.setDataCompra(req.dataCompra());
        c.setObservacao(req.observacao() == null || req.observacao().isBlank() ? null : req.observacao().trim());

        YearMonth primeira = req.primeiraFatura() != null
                ? req.primeiraFatura()
                : RegraFatura.competenciaDaCompra(req.dataCompra(), cartao.getDiaFechamento());
        c.setPrimeiraFatura(primeira.atDay(1));

        // Orphan removal apaga as antigas; o flush no atualizar garante que o DELETE sai
        // antes do INSERT (senão a UNIQUE (compra_id, numero) reclamaria).
        c.getParcelas().clear();
        if (c.getId() != null) repository.flush();
        List<BigDecimal> valores = RegraFatura.dividir(req.valorTotal(), req.qtdParcelas());
        for (int i = 0; i < valores.size(); i++) {
            Parcela p = new Parcela();
            p.setCompra(c);
            p.setNumero((short) (i + 1));
            p.setValor(valores.get(i));
            p.setCompetencia(primeira.plusMonths(i).atDay(1));
            c.getParcelas().add(p);
        }
    }
}
