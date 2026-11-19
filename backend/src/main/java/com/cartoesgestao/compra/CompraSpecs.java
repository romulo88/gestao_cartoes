package com.cartoesgestao.compra;

import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.jpa.domain.Specification;

/** Filtros da listagem de compras - cada um só entra se o parâmetro vier preenchido. */
final class CompraSpecs {

    private CompraSpecs() {
    }

    static Specification<Compra> doUsuario(Integer usuarioId) {
        return (root, q, cb) -> cb.equal(root.get("usuarioId"), usuarioId);
    }

    static Specification<Compra> doCartao(Integer cartaoId) {
        return (root, q, cb) -> cb.equal(root.get("cartao").get("id"), cartaoId);
    }

    static Specification<Compra> daCategoria(Integer categoriaId) {
        return (root, q, cb) -> cb.equal(root.get("categoria").get("id"), categoriaId);
    }

    static Specification<Compra> descricaoContem(String busca) {
        String padrao = "%" + busca.trim().toLowerCase() + "%";
        return (root, q, cb) -> cb.like(cb.lower(root.<String>get("descricao")), padrao);
    }

    /** Compras com alguma parcela numa fatura que ainda não venceu. {@code desdePorCartao}:
     * id do cartão -> primeira fatura não vencida dele (ver RegraFatura.primeiraNaoVencida). */
    static Specification<Compra> comParcelaAVencer(Map<Integer, LocalDate> desdePorCartao) {
        return (root, q, cb) -> {
            if (desdePorCartao.isEmpty()) return cb.disjunction();
            List<Predicate> porCartao = new ArrayList<>();
            for (Map.Entry<Integer, LocalDate> e : desdePorCartao.entrySet()) {
                Subquery<Integer> sub = q.subquery(Integer.class);
                var p = sub.from(Parcela.class);
                sub.select(p.<Integer>get("id")).where(
                        cb.equal(p.get("compra"), root),
                        cb.greaterThanOrEqualTo(p.<LocalDate>get("competencia"), e.getValue()));
                porCartao.add(cb.and(cb.equal(root.get("cartao").get("id"), e.getKey()), cb.exists(sub)));
            }
            return cb.or(porCartao.toArray(new Predicate[0]));
        };
    }
}
