package br.com.encaixa.venda.cliente;

import jakarta.persistence.Convert;
import jakarta.persistence.Column;
import org.hibernate.annotations.ColumnTransformer;
import br.com.encaixa.shared.jpa.JsonbConverters;
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
@Table(name = "clientes")
@Getter
@Setter
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String keycloakUserId;

    private String nome;
    private String email;
    private String telefone;

    @Convert(converter = JsonbConverters.EnderecoConverter.class)
    @ColumnTransformer(write = "?::jsonb")
    @Column(columnDefinition = "jsonb")
    private Endereco endereco;

    @CreationTimestamp
    private OffsetDateTime criadoEm;
}

