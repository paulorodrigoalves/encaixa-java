package br.com.encaixa.precificacao;

import br.com.encaixa.domain.engine.model.Modelos.ObjetoUsuario;
import br.com.encaixa.shared.jpa.JsonbConverters;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orcamentos")
@Getter
@Setter
public class Orcamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private BigDecimal precoCalculado;
    private OffsetDateTime dataValidade;

    @Convert(converter = JsonbConverters.ObjetosUsuarioListConverter.class)
    @ColumnTransformer(write = "?::jsonb")
    @Column(columnDefinition = "jsonb")
    private List<ObjetoUsuario> cacheObjetos;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "regra_precificacao_id")
    private RegraPrecificacao regraPrecificacao;

    @CreationTimestamp
    private OffsetDateTime criadoEm;
}
