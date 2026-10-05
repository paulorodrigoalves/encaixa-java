package br.com.encaixa.catalogo.dto;

import java.util.List;

public record CatalogoCompletoDTO(
        List<MaterialDTO> materiais,
        List<TemplateDTO> templates,
        List<TipoObjetoDTO> tiposObjeto
) {}
