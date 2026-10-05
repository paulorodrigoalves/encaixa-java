package br.com.encaixa.producao.etapa;

import br.com.encaixa.producao.pedido.Pedido;
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

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Linha do tempo do pedido. {@code nomeEtapa} e snapshot: renomear a etapa no catalogo
 * nao reescreve o historico (e a etapa pode ate ser removida — etapa_id vira null).
 */
@Entity
@Table(name = "pedido_etapas")
@Getter
@Setter
public class PedidoEtapa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etapa_id")
    private EtapaProducao etapa;

    @Column(nullable = false)
    private String nomeEtapa;

    private String observacao;

    private String fotoUrl;

    private String storageKey;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime criadoEm;
}
