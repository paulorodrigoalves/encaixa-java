package br.com.encaixa.catalogo;

import br.com.encaixa.catalogo.dto.CatalogoCompletoDTO;
import br.com.encaixa.catalogo.dto.MaterialDTO;
import br.com.encaixa.catalogo.dto.TemplateDTO;
import br.com.encaixa.catalogo.dto.TipoObjetoDTO;
import br.com.encaixa.catalogo.material.MaterialRepository;
import br.com.encaixa.catalogo.tipoobjeto.TipoObjetoRepository;
import br.com.encaixa.catalogo.template.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CatalogoService {

    private final MaterialRepository materialRepository;
    private final TemplateRepository templateRepository;
    private final TipoObjetoRepository tipoObjetoRepository;

    @Transactional(readOnly = true)
    public CatalogoCompletoDTO obterCatalogoCompleto() {
        var materiais = materialRepository.findAllAtivos().stream().map(MaterialDTO::from).toList();
        var templates = templateRepository.findAllByAtivoTrue().stream().map(TemplateDTO::from).toList();
        var tipos = tipoObjetoRepository.findAllByAtivoTrue().stream().map(TipoObjetoDTO::from).toList();

        return new CatalogoCompletoDTO(materiais, templates, tipos);
    }
}

