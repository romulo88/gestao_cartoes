package com.cartoesgestao.cartao;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "cartoes")
@Getter
@Setter
public class Cartao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cartao")
    private Integer id;

    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    @Column(nullable = false, length = 60)
    private String nome;

    @Column(length = 30)
    private String bandeira;

    @Column(name = "final_cartao", length = 4)
    private String finalCartao;

    @Column(name = "dia_fechamento", nullable = false)
    private Short diaFechamento;

    @Column(name = "dia_vencimento", nullable = false)
    private Short diaVencimento;

    @Column(precision = 12, scale = 2)
    private BigDecimal limite;

    @Column(nullable = false, length = 7)
    private String cor;

    @Column(nullable = false)
    private Boolean ativo = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
