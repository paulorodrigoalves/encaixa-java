package br.com.encaixa.catalogo.material;

import jakarta.persistence.Entity;
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
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "materiais")
@Getter
@Setter
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String nome;
    private String cor;

    private BigDecimal valorFixoAdicional = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_material_id", nullable = false)
    private TipoMaterial tipoMaterial;

    @OneToMany(mappedBy = "material")
    private List<MaterialFoto> fotos = new ArrayList<>();

    private boolean ativo = true;

    @CreationTimestamp
    private OffsetDateTime criadoEm;
}
