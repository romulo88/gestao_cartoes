package com.cartoesgestao.categoria;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "categorias")
@Getter
@Setter
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Integer id;

    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    @Column(nullable = false, length = 60)
    private String nome;

    @Column(nullable = false, length = 7)
    private String cor;

    @Column(name = "limite_mensal", precision = 12, scale = 2)
    private BigDecimal limiteMensal;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
