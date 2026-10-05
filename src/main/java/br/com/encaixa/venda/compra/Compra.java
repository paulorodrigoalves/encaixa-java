package br.com.encaixa.venda.compra;

import br.com.encaixa.venda.cliente.Cliente;
import br.com.encaixa.venda.projeto.Projeto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Carrinho fechado: agrupa N projetos (itens). Frete e total ficam aqui.
 * Fluxo: FECHADA -> APROVADA -> (pagamento confirmado gera um {@code Pedido}).
 */
@Entity
@Table(name = "compras")
@Getter
@Setter
public class Compra {

    public enum StatusCompra {
        FECHADA,
        APROVADA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusCompra status = StatusCompra.FECHADA;

    @Column(nullable = false)
    private BigDecimal valorFrete = BigDecimal.ZERO;

    /** soma(itens x quantidade) + frete. */
    @Column(nullable = false)
    private BigDecimal valorTotal = BigDecimal.ZERO;

    private String transportadora;

    private Integer prazoFreteDias;

    @OneToMany(mappedBy = "compra")
    private List<Projeto> projetos = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime atualizadoEm;
}
