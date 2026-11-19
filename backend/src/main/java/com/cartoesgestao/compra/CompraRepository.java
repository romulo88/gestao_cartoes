package com.cartoesgestao.compra;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompraRepository extends JpaRepository<Compra, Integer>, JpaSpecificationExecutor<Compra> {

    @EntityGraph(attributePaths = {"cartao", "categoria", "parcelas"})
    Optional<Compra> findByIdAndUsuarioId(Integer id, Integer usuarioId);

    boolean existsByCartaoId(Integer cartaoId);

    /** [descricao, categoriaId] das compras categorizadas, mais recentes primeiro - base da
     * sugestão de categoria (ver RegraSugestao). O Pageable limita quantas olhar. */
    @Query("""
            select c.descricao, c.categoria.id from Compra c
             where c.usuarioId = :usuarioId and c.categoria is not null
             order by c.dataCompra desc, c.id desc
            """)
    List<Object[]> historicoDeCategorias(@Param("usuarioId") Integer usuarioId, Pageable pageable);
}
