package com.cartoesgestao.compra;

import com.cartoesgestao.cartao.Cartao;
import com.cartoesgestao.categoria.Categoria;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "compras")
@Getter
@Setter
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_compra")
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

    @Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "qtd_parcelas", nullable = false)
    private Short qtdParcelas;

    @Column(name = "data_compra", nullable = false)
    private LocalDate dataCompra;

    @Column(name = "primeira_fatura", nullable = false)
    private LocalDate primeiraFatura;

    @Column(length = 500)
    private String observacao;

    /** Recorrência que gerou a compra (nulo = lançada à mão). Só o id: a compra não
     * depende da recorrência depois de gerada. */
    @Column(name = "recorrencia_id")
    private Integer recorrenciaId;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numero ASC")
    private List<Parcela> parcelas = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
