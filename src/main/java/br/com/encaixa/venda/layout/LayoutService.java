package br.com.encaixa.venda.layout;

import br.com.encaixa.catalogo.material.MaterialRepository;
import br.com.encaixa.catalogo.template.TemplateRepository;
import br.com.encaixa.domain.engine.EscalaTemplateEngine;
import br.com.encaixa.domain.engine.model.Modelos.ConfigEscala;
import br.com.encaixa.domain.engine.model.Modelos.MedidasGaveta;
import br.com.encaixa.venda.layout.dto.PreviewLayoutRequest;
import br.com.encaixa.venda.layout.dto.PreviewLayoutResponse;
import br.com.encaixa.venda.layout.dto.PreviewLayoutResponse.DivisoriaRenderDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LayoutService {

    private final MaterialRepository materialRepository;
    private final TemplateRepository templateRepository;

    @Transactional(readOnly = true)
    public PreviewLayoutResponse gerarPreview(PreviewLayoutRequest req) {
        var material = materialRepository.findById(req.materialId())
                .orElseThrow(() -> new IllegalArgumentException("Material invalido"));

        var config = new ConfigEscala(
                material.getTipoMaterial().getEspessuraMm(),
                material.getTipoMaterial().getMargemMm()
        );

        var medidas = new MedidasGaveta(req.larguraMm(), req.profundidadeMm(), req.alturaMm());

        // Por enquanto, o preview vai focar so nos Templates para te provar o front.
        // Se templateId for null, estouramos erro (temporario ate plugar o motor de objetos).
        if (req.templateId() == null) {
            throw new IllegalArgumentException("Motor por objetos ainda nao exposto. Use um templateId.");
        }

        var template = templateRepository.findById(req.templateId())
                .orElseThrow(() -> new IllegalArgumentException("Template invalido"));

        var resultado = new EscalaTemplateEngine().escalar(template.getLayoutProporcional(), medidas, config);

        // Agora transformamos a lista de compartimentos (espacos vazios) em divisorias (paredes solidadas).
        // AUI simplificada: o Angular renderiza os "furos" se usarmos SVG masks, ou
        // podemos mandar as paredes reais. Como a UI atual pede DivisoriaRenderDTO:
        // Vamos enviar uma lista provisória baseada nos compartimentos (espessuras entre eles).
        
        List<DivisoriaRenderDTO> divisorias = new ArrayList<>();
        int espessura = material.getTipoMaterial().getEspessuraMm();
        
        // Simples gerador de divisorias visuais:
        resultado.compartimentos().forEach(c -> {
            // Desenha a "parede direita" de cada compartimento
            if (c.posXMm() + c.larguraMm() + espessura < resultado.larguraUtilMm()) {
                divisorias.add(new DivisoriaRenderDTO(
                        c.posXMm() + c.larguraMm(), c.posYMm(),
                        espessura, c.profundidadeMm(), "VERTICAL"
                ));
            }
            // Desenha a "parede de baixo" de cada compartimento
            if (c.posYMm() + c.profundidadeMm() + espessura < resultado.profundidadeUtilMm()) {
                divisorias.add(new DivisoriaRenderDTO(
                        c.posXMm(), c.posYMm() + c.profundidadeMm(),
                        c.larguraMm(), espessura, "HORIZONTAL"
                ));
            }
        });

        return new PreviewLayoutResponse(
                (int) resultado.larguraUtilMm(),
                (int) resultado.profundidadeUtilMm(),
                req.alturaMm(),
                divisorias,
                List.of(), // Objetos virão depois se for gerador por objetos
                espessura
        );
    }
}

