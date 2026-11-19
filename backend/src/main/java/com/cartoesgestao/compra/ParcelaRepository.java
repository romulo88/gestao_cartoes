package com.cartoesgestao.compra;

import com.cartoesgestao.dashboard.dto.TotalAgrupado;
import com.cartoesgestao.dashboard.dto.TotalPorChave;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParcelaRepository extends JpaRepository<Parcela, Integer> {

    /** Todas as parcelas de uma fatura (de todos os cartões), já com compra/cartão/categoria. */
    @Query("""
            select p from Parcela p
              join fetch p.compra c
              join fetch c.cartao
              left join fetch c.categoria
             where c.usuarioId = :usuarioId and p.competencia = :competencia
             order by c.dataCompra asc, c.id asc
            """)
    List<Parcela> daFatura(@Param("usuarioId") Integer usuarioId, @Param("competencia") LocalDate competencia);

    /** Total por cartão e mês, no intervalo [inicio, fim]. chave = id do cartão. */
    @Query("""
            select new com.cartoesgestao.dashboard.dto.TotalAgrupado(c.cartao.id, p.competencia, sum(p.valor))
              from Parcela p join p.compra c
             where c.usuarioId = :usuarioId and p.competencia between :inicio and :fim
             group by c.cartao.id, p.competencia
            """)
    List<TotalAgrupado> totaisPorCartaoEMes(
            @Param("usuarioId") Integer usuarioId, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    /** Total por categoria num mês. chave = id da categoria (null = sem categoria). */
    @Query("""
            select new com.cartoesgestao.dashboard.dto.TotalAgrupado(cat.id, p.competencia, sum(p.valor))
              from Parcela p join p.compra c left join c.categoria cat
             where c.usuarioId = :usuarioId and p.competencia = :competencia
             group by cat.id, p.competencia
            """)
    List<TotalAgrupado> totaisPorCategoria(
            @Param("usuarioId") Integer usuarioId, @Param("competencia") LocalDate competencia);

    /** Saldo ainda a pagar por cartão: parcelas da fatura {@code desde} em diante. */
    @Query("""
            select new com.cartoesgestao.dashboard.dto.TotalPorChave(c.cartao.id, sum(p.valor))
              from Parcela p join p.compra c
             where c.usuarioId = :usuarioId and p.competencia >= :desde
             group by c.cartao.id
            """)
    List<TotalPorChave> emAbertoPorCartao(@Param("usuarioId") Integer usuarioId, @Param("desde") LocalDate desde);
}
