package br.com.encaixa.producao.pedido;

import br.com.encaixa.producao.etapa.PedidoEtapa;
import br.com.encaixa.venda.compra.Compra;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Criado quando o admin confirma o pagamento de uma compra (um por compra).
 * Nao tem coluna de status: a situacao atual e a ultima etapa do historico.
 */
@Entity
@Table(name = "pedidos")
@Getter
@Setter
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compra_id", nullable = false, unique = true, updatable = false)
    private Compra compra;

    @Column(name = "codigo_rastreio")
    private String codigoRastreio;

    @Column(nullable = false)
    private Integer prazoEstimadoDias = 15;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("criadoEm ASC")
    private List<PedidoEtapa> etapas = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime atualizadoEm;

    public void registrarEtapa(PedidoEtapa etapa) {
        etapa.setPedido(this);
        etapas.add(etapa);
    }
}
