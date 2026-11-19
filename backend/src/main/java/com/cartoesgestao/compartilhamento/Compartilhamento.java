package com.cartoesgestao.compartilhamento;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "compartilhamentos")
@Getter
@Setter
public class Compartilhamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_compartilhamento")
    private Integer id;

    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    @Column(nullable = false, length = 60)
    private String nome;

    /** SHA-256 (hex) do token - o token em si nunca é gravado. */
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "ultimo_acesso")
    private LocalDateTime ultimoAcesso;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
