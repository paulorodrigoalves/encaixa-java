package br.com.encaixa.catalogo.template;

import br.com.encaixa.domain.engine.model.Modelos.LayoutProporcional;
import br.com.encaixa.shared.jpa.JsonbConverters;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "templates")
@Getter
@Setter
public class Template {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String nome;
    private String categoria;

    @Convert(converter = JsonbConverters.LayoutProporcionalConverter.class)
    @ColumnTransformer(write = "?::jsonb")
    @Column(columnDefinition = "jsonb")
    private LayoutProporcional layoutProporcional;

    private String thumbnailUrl;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags_objetos", columnDefinition = "text[]")
    private List<String> tagsObjetos;

    private boolean ativo = true;

    @CreationTimestamp
    private OffsetDateTime criadoEm;
}
