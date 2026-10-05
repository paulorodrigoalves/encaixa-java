package br.com.encaixa.catalogo.tipoobjeto;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tipos_objeto")
@Getter
@Setter
public class TipoObjeto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String nome;
    
    private String categoria;

    private int larguraMinMm;
    private int larguraMaxMm;
    private int profundidadeMinMm;
    private int profundidadeMaxMm;
    private Integer alturaMinMm;

    private boolean ativo = true;

    @CreationTimestamp
    private OffsetDateTime criadoEm;
}
