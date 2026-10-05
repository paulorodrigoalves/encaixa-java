package br.com.encaixa.shared.jpa;

import br.com.encaixa.domain.engine.model.Modelos.ItemObjetoSelecionado;
import br.com.encaixa.domain.engine.model.Modelos.LayoutProporcional;
import br.com.encaixa.domain.engine.model.Modelos.LayoutGerado;
import br.com.encaixa.domain.engine.model.Modelos.ObjetoUsuario;
import br.com.encaixa.venda.cliente.Endereco;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * Conversores JPA para colunas JSONB.
 *
 * <p>Abordagem explicita (AttributeConverter + {@code @ColumnTransformer(write = "?::jsonb")}
 * na entidade) em vez do FormatMapper do Hibernate: funciona igual em qualquer versao do
 * Jackson/Hibernate e deixa o formato do JSON sob controle do dominio.
 */
public final class JsonbConverters {

    private static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    private JsonbConverters() {
    }

    abstract static class Base<T> implements AttributeConverter<T, String> {
        private final TypeReference<T> tipo;

        Base(TypeReference<T> tipo) {
            this.tipo = tipo;
        }

        @Override
        public String convertToDatabaseColumn(T valor) {
            return valor == null ? null : MAPPER.writeValueAsString(valor);
        }

        @Override
        public T convertToEntityAttribute(String json) {
            return json == null || json.isBlank() ? null : MAPPER.readValue(json, tipo);
        }
    }

    @Converter
    public static class LayoutProporcionalConverter extends Base<LayoutProporcional> {
        public LayoutProporcionalConverter() {
            super(new TypeReference<>() {
            });
        }
    }

    @Converter
    public static class ItensObjetoConverter extends Base<List<ItemObjetoSelecionado>> {
        public ItensObjetoConverter() {
            super(new TypeReference<>() {
            });
        }
    }

    @Converter
    public static class LayoutGeradoConverter extends Base<LayoutGerado> {
        public LayoutGeradoConverter() { super(new TypeReference<>() {}); }
    }

    @Converter
    public static class ObjetoUsuarioConverter extends Base<ObjetoUsuario> {
        public ObjetoUsuarioConverter() { super(new TypeReference<>() {}); }
    }

    @Converter
    public static class ObjetosUsuarioListConverter extends Base<List<ObjetoUsuario>> {
        public ObjetosUsuarioListConverter() { super(new TypeReference<>() {}); }
    }

    @Converter
    public static class EnderecoConverter extends Base<Endereco> {
        public EnderecoConverter() {
            super(new TypeReference<>() {
            });
        }
    }
}


