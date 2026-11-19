package com.cartoesgestao.compra;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "parcelas")
@Getter
@Setter
public class Parcela {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_parcela")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compra_id", nullable = false)
    private Compra compra;

    @Column(nullable = false)
    private Short numero;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    /** Mês da fatura, sempre no dia 1. */
    @Column(nullable = false)
    private LocalDate competencia;
}
