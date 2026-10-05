package br.com.encaixa.catalogo.material;

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
@Table(name = "tipos_material")
@Getter
@Setter
public class TipoMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String nome;

    private int espessuraMm;
    private int margemMm;

    private boolean ativo = true;

    @CreationTimestamp
    private OffsetDateTime criadoEm;
}
