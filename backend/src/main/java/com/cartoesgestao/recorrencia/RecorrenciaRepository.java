package com.cartoesgestao.recorrencia;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecorrenciaRepository extends JpaRepository<Recorrencia, Integer> {

    @EntityGraph(attributePaths = {"cartao", "categoria"})
    List<Recorrencia> findByUsuarioIdOrderByAtivaDescDescricaoAsc(Integer usuarioId);

    @EntityGraph(attributePaths = {"cartao", "categoria"})
    Optional<Recorrencia> findByIdAndUsuarioId(Integer id, Integer usuarioId);

    @EntityGraph(attributePaths = {"cartao", "categoria"})
    List<Recorrencia> findByUsuarioIdAndAtivaTrue(Integer usuarioId);

    boolean existsByCartaoId(Integer cartaoId);

    /** Recorrências com cobrança vencida até {@code hoje} ainda não gerada (de todos os usuários). */
    @Query("select r.id from Recorrencia r where r.ativa = true and r.proximaCobranca <= :hoje")
    List<Integer> idsPendentes(@Param("hoje") LocalDate hoje);

    /** Trava a linha durante a geração: agendador e requisição não geram a mesma cobrança. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Recorrencia r where r.id = :id")
    Optional<Recorrencia> travar(@Param("id") Integer id);
}
