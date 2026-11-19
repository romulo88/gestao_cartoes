package com.cartoesgestao.usuario;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    @Query("select u from Usuario u where lower(u.email) = lower(:email)")
    Optional<Usuario> findByEmail(String email);
}
