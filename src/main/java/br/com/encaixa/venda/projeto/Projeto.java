package br.com.encaixa.venda.projeto;

import br.com.encaixa.catalogo.material.Material;
import br.com.encaixa.catalogo.template.Template;
import br.com.encaixa.domain.engine.model.Modelos.ItemObjetoSelecionado;
import br.com.encaixa.shared.jpa.JsonbConverters;
import br.com.encaixa.venda.compartimento.Compartimento;
import br.com.encaixa.venda.compra.Compra;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Um organizador configurado. O layout vem de um template OU de uma lista de objetos,
 * nunca dos dois (constraint {@code projetos_origem_layout} no banco).
 * Espessura/margem sao snapshot do tipo de material no momento da criacao.
 */
@Entity
@Table(name = "projetos")
@Getter
@Setter
public class Projeto {

    public enum StatusProjeto {
        RASCUNHO,
        FECHADO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Null enquanto o projeto ainda nao entrou em uma compra. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_id")
    private Compra compra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private Template template;

    @Convert(converter = JsonbConverters.ItensObjetoConverter.class)
    @ColumnTransformer(write = "?::jsonb")
    @Column(columnDefinition = "jsonb")
    private List<ItemObjetoSelecionado> itensObjeto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(nullable = false)
    private Integer larguraMm;

    @Column(nullable = false)
    private Integer profundidadeMm;

    @Column(nullable = false)
    private Integer alturaMm;

    @Column(nullable = false)
    private Integer margemSegurancaMm;

    @Column(nullable = false)
    private Integer espessuraMaterialMm;

    @Column(nullable = false)
    private Integer quantidade = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusProjeto status = StatusProjeto.RASCUNHO;

    @OneToMany(mappedBy = "projeto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Compartimento> compartimentos = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime atualizadoEm;

    /** Substitui os compartimentos mantendo o vinculo bidirecional consistente. */
    public void substituirCompartimentos(List<Compartimento> novos) {
        compartimentos.clear();
        novos.forEach(c -> {
            c.setProjeto(this);
            compartimentos.add(c);
        });
    }
}
