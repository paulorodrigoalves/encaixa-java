package br.com.encaixa.producao.etapa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Catalogo editavel de etapas de fabricacao. */
@Entity
@Table(name = "etapas_producao")
@Getter
@Setter
public class EtapaProducao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String nome;

    @Column(nullable = false)
    private Integer ordem;

    /** Etapas de envio/entrega exigem codigo de rastreio. */
    @Column(nullable = false)
    private boolean usaRastreio = false;

    @Column(nullable = false)
    private boolean ativo = true;
}
