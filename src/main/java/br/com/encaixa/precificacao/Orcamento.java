package br.com.encaixa.precificacao;

import br.com.encaixa.venda.projeto.Projeto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Snapshot imutavel do calculo de preco de um projeto, amarrado a regra vigente no momento.
 * Nunca e atualizado: um novo calculo gera um novo orcamento.
 */
@Entity
@Table(name = "orcamentos")
@Getter
@Setter
public class Orcamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projeto_id", nullable = false, updatable = false)
    private Projeto projeto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "regra_precificacao_id", nullable = false, updatable = false)
    private RegraPrecificacao regraPrecificacao;

    @Column(nullable = false, updatable = false)
    private BigDecimal volumeTotalLitros;

    @Column(nullable = false, updatable = false)
    private BigDecimal areaDivisoriasCm2;

    @Column(nullable = false, updatable = false)
    private BigDecimal valorVolume;

    @Column(nullable = false, updatable = false)
    private BigDecimal valorDivisorias;

    @Column(nullable = false, updatable = false)
    private BigDecimal valorMaterial;

    @Column(nullable = false, updatable = false)
    private BigDecimal valorFrete = BigDecimal.ZERO;

    @Column(nullable = false, updatable = false)
    private BigDecimal valorTotal;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime criadoEm;
}
