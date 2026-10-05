package br.com.encaixa.catalogo.dto;

import br.com.encaixa.catalogo.template.Template;
import br.com.encaixa.domain.engine.model.Modelos.LayoutProporcional;

import java.util.List;
import java.util.UUID;

public record TemplateDTO(
        UUID id,
        String nome,
        String categoria,
        LayoutProporcional layoutProporcional,
        String thumbnailUrl,
        List<String> tagsObjetos
) {
    public static TemplateDTO from(Template t) {
        return new TemplateDTO(
                t.getId(),
                t.getNome(),
                t.getCategoria(),
                t.getLayoutProporcional(),
                t.getThumbnailUrl(),
                t.getTagsObjetos()
        );
    }
}
