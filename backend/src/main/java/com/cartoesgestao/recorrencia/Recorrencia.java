package com.cartoesgestao.recorrencia;

import com.cartoesgestao.cartao.Cartao;
import com.cartoesgestao.categoria.Categoria;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "recorrencias")
@Getter
@Setter
public class Recorrencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_recorrencia")
    private Integer id;

    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cartao_id", nullable = false)
    private Cartao cartao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Column(nullable = false, length = 160)
    private String descricao;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "qtd_parcelas", nullable = false)
    private Short qtdParcelas;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Periodicidade periodicidade;

    @Column(name = "dia_cobranca", nullable = false)
    private Short diaCobranca;

    @Column(name = "proxima_cobranca", nullable = false)
    private LocalDate proximaCobranca;

    @Column(name = "data_fim")
    private LocalDate dataFim;

    @Column(nullable = false)
    private Boolean ativa = true;

    @Column(length = 500)
    private String observacao;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
