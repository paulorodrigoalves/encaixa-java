package br.com.encaixa.precificacao;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "regras_precificacao")
@Getter
@Setter
public class RegraPrecificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private BigDecimal precoPorLitro;
    private BigDecimal precoPorCm2Divisoria;

    @CreationTimestamp
    private OffsetDateTime vigenteDesde;
    
    private OffsetDateTime vigenteAte;

    private String criadoPor;
}
