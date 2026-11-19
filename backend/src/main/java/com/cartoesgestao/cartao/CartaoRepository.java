package com.cartoesgestao.cartao;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartaoRepository extends JpaRepository<Cartao, Integer> {

    List<Cartao> findByUsuarioIdOrderByNomeAsc(Integer usuarioId);

    Optional<Cartao> findByIdAndUsuarioId(Integer id, Integer usuarioId);

    boolean existsByUsuarioIdAndNomeIgnoreCase(Integer usuarioId, String nome);

    boolean existsByUsuarioIdAndNomeIgnoreCaseAndIdNot(Integer usuarioId, String nome, Integer id);
}
