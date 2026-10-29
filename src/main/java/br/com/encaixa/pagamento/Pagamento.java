package br.com.encaixa.pagamento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "pagamentos")
@Getter
@Setter
public class Pagamento {

    public enum StatusPagamento {
        PENDENTE, APROVADO, REJEITADO, CANCELADO, ESTORNADO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "compra_id", nullable = false)
    private UUID compraId;

    @Column(nullable = false)
    private String provider = "MERCADO_PAGO";

    @Column(name = "provider_payment_id")
    private String providerPaymentId;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(nullable = false)
    private StatusPagamento status = StatusPagamento.PENDENTE;

    @Column(nullable = false)
    private BigDecimal valor;

    @Column(name = "pix_copia_cola")
    private String pixCopiaCola;

    @Column(name = "pix_qr_base64")
    private String pixQrBase64;

    @Column(name = "ticket_url")
    private String ticketUrl;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime atualizadoEm;
}
