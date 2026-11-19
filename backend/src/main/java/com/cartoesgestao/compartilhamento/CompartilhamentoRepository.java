package com.cartoesgestao.compartilhamento;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompartilhamentoRepository extends JpaRepository<Compartilhamento, Integer> {

    List<Compartilhamento> findByUsuarioIdOrderByCreatedAtDesc(Integer usuarioId);

    Optional<Compartilhamento> findByIdAndUsuarioId(Integer id, Integer usuarioId);

    Optional<Compartilhamento> findByTokenHash(String tokenHash);

    long countByUsuarioId(Integer usuarioId);
}
