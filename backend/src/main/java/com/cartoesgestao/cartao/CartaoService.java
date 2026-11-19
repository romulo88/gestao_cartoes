package com.cartoesgestao.cartao;

import com.cartoesgestao.cartao.dto.CartaoRequest;
import com.cartoesgestao.cartao.dto.CartaoResponse;
import com.cartoesgestao.common.ConflictException;
import com.cartoesgestao.common.ResourceNotFoundException;
import com.cartoesgestao.compra.CompraRepository;
import com.cartoesgestao.recorrencia.RecorrenciaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartaoService {

    private final CartaoRepository cartaoRepository;
    private final CompraRepository compraRepository;
    private final RecorrenciaRepository recorrenciaRepository;

    public CartaoService(
            CartaoRepository cartaoRepository, CompraRepository compraRepository, RecorrenciaRepository recorrenciaRepository) {
        this.cartaoRepository = cartaoRepository;
        this.compraRepository = compraRepository;
        this.recorrenciaRepository = recorrenciaRepository;
    }

    @Transactional(readOnly = true)
    public List<CartaoResponse> listar(Integer usuarioId) {
        return cartaoRepository.findByUsuarioIdOrderByNomeAsc(usuarioId).stream().map(CartaoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public List<Cartao> todos(Integer usuarioId) {
        return cartaoRepository.findByUsuarioIdOrderByNomeAsc(usuarioId);
    }

    @Transactional(readOnly = true)
    public Cartao buscar(Integer usuarioId, Integer id) {
        return cartaoRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Cartão não encontrado"));
    }

    @Transactional
    public CartaoResponse criar(Integer usuarioId, CartaoRequest req) {
        if (cartaoRepository.existsByUsuarioIdAndNomeIgnoreCase(usuarioId, req.nome().trim())) {
            throw new ConflictException("Você já tem um cartão com esse nome");
        }
        Cartao c = new Cartao();
        c.setUsuarioId(usuarioId);
        aplicar(c, req);
        return CartaoResponse.de(cartaoRepository.save(c));
    }

    /** Mudar o dia de fechamento NÃO recalcula as compras já lançadas: cada uma guarda a
     * fatura em que caiu (é o que o extrato do banco mostra). Vale para as próximas. */
    @Transactional
    public CartaoResponse atualizar(Integer usuarioId, Integer id, CartaoRequest req) {
        Cartao c = buscar(usuarioId, id);
        if (cartaoRepository.existsByUsuarioIdAndNomeIgnoreCaseAndIdNot(usuarioId, req.nome().trim(), id)) {
            throw new ConflictException("Você já tem um cartão com esse nome");
        }
        aplicar(c, req);
        return CartaoResponse.de(c);
    }

    @Transactional
    public void excluir(Integer usuarioId, Integer id) {
        Cartao c = buscar(usuarioId, id);
        if (compraRepository.existsByCartaoId(id)) {
            throw new ConflictException(
                    "Esse cartão tem compras lançadas. Desative-o em vez de excluir, para manter o histórico.");
        }
        if (recorrenciaRepository.existsByCartaoId(id)) {
            throw new ConflictException("Esse cartão tem lançamentos recorrentes. Mude-os de cartão ou exclua-os antes.");
        }
        cartaoRepository.delete(c);
    }

    private void aplicar(Cartao c, CartaoRequest req) {
        c.setNome(req.nome().trim());
        c.setBandeira(vazioParaNulo(req.bandeira()));
        c.setFinalCartao(vazioParaNulo(req.finalCartao()));
        c.setDiaFechamento(req.diaFechamento());
        c.setDiaVencimento(req.diaVencimento());
        c.setLimite(req.limite());
        c.setCor(req.cor() != null ? req.cor() : "#2a78d6");
        c.setAtivo(req.ativo() == null || req.ativo());
    }

    private static String vazioParaNulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
