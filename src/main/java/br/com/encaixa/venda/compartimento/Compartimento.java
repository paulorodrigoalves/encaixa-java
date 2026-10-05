package br.com.encaixa.venda.compartimento;

import br.com.encaixa.domain.engine.model.Modelos.ObjetoUsuario;
import br.com.encaixa.shared.jpa.JsonbConverters;
import br.com.encaixa.venda.projeto.Projeto;
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

import java.util.UUID;

@Entity
@Table(name = "compartimentos")
@Getter
@Setter
public class Compartimento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    private int xMm;
    private int yMm;
    private int larguraMm;
    private int profundidadeMm;

    @Convert(converter = JsonbConverters.ObjetoUsuarioConverter.class)
    @ColumnTransformer(write = "?::jsonb")
    @Column(columnDefinition = "jsonb")
    private ObjetoUsuario objetoAlocado;
}
