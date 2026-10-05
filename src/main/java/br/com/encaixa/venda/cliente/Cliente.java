package br.com.encaixa.venda.cliente;

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

import java.time.OffsetDateTime;
import java.util.UUID;

/** Cliente final. Cadastrado pelo vendedor (telefone obrigatorio, email opcional). */
@Entity
@Table(name = "clientes")
@Getter
@Setter
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String telefone;

    private String email;

    @Convert(converter = JsonbConverters.EnderecoConverter.class)
    @ColumnTransformer(write = "?::jsonb")
    @Column(columnDefinition = "jsonb")
    private Endereco endereco;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime criadoEm;
}
