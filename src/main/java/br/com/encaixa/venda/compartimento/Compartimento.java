package br.com.encaixa.venda.compartimento;

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

import java.math.BigDecimal;
import java.util.UUID;

/** Vao interno ja posicionado (saida do motor de layout), persistido para producao. */
@Entity
@Table(name = "compartimentos")
@Getter
@Setter
public class Compartimento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    // Nomes explicitos: a naming strategy nao separa "posXMm" em pos_x_mm.
    @Column(name = "pos_x_mm", nullable = false)
    private Integer posXMm;

    @Column(name = "pos_y_mm", nullable = false)
    private Integer posYMm;

    @Column(nullable = false)
    private Integer larguraMm;

    @Column(nullable = false)
    private Integer profundidadeMm;

    @Column(nullable = false)
    private BigDecimal areaPaineisCm2;
}
