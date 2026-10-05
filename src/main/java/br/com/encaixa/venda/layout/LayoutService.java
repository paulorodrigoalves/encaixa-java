package br.com.encaixa.venda.layout;

import br.com.encaixa.catalogo.material.MaterialRepository;
import br.com.encaixa.catalogo.template.TemplateRepository;
import br.com.encaixa.domain.engine.EscalaTemplateEngine;
import br.com.encaixa.domain.engine.PricingEngine;
import br.com.encaixa.domain.engine.model.Modelos.ConfigEscala;
import br.com.encaixa.domain.engine.model.Modelos.MedidasGaveta;
import br.com.encaixa.precificacao.RegraPrecificacaoRepository;
import br.com.encaixa.venda.layout.dto.PreviewLayoutRequest;
import br.com.encaixa.venda.layout.dto.PreviewLayoutResponse;
import br.com.encaixa.venda.layout.dto.PreviewLayoutResponse.DivisoriaRenderDTO;
import br.com.encaixa.venda.layout.dto.PreviewLayoutResponse.OrcamentoPreviewDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LayoutService {

    private final MaterialRepository materialRepository;
    private final TemplateRepository templateRepository;
    private final RegraPrecificacaoRepository regraPrecificacaoRepository;

    @Transactional(readOnly = true)
    public PreviewLayoutResponse gerarPreview(PreviewLayoutRequest req) {
        var material = materialRepository.findById(req.materialId())
                .orElseThrow(() -> new IllegalArgumentException("Material invalido"));

        var config = new ConfigEscala(
                material.getTipoMaterial().getEspessuraMm(),
                material.getTipoMaterial().getMargemMm()
        );

        var medidas = new MedidasGaveta(req.larguraMm(), req.profundidadeMm(), req.alturaMm());

        if (req.templateId() == null) {
            throw new IllegalArgumentException("Motor por objetos ainda nao exposto. Use um templateId.");
        }

        var template = templateRepository.findById(req.templateId())
                .orElseThrow(() -> new IllegalArgumentException("Template invalido"));

        var resultado = new EscalaTemplateEngine().escalar(template.getLayoutProporcional(), medidas, config);

        List<DivisoriaRenderDTO> divisorias = new ArrayList<>();
        int espessura = material.getTipoMaterial().getEspessuraMm();
        
        resultado.compartimentos().forEach(c -> {
            if (c.posXMm() + c.larguraMm() + espessura < resultado.larguraUtilMm()) {
                divisorias.add(new DivisoriaRenderDTO(
                        c.posXMm() + c.larguraMm(), c.posYMm(),
                        espessura, c.profundidadeMm(), "VERTICAL"
                ));
            }
            if (c.posYMm() + c.profundidadeMm() + espessura < resultado.profundidadeUtilMm()) {
                divisorias.add(new DivisoriaRenderDTO(
                        c.posXMm(), c.posYMm() + c.profundidadeMm(),
                        c.larguraMm(), espessura, "HORIZONTAL"
                ));
            }
        });

        // ---------------------------------------------------------
        // INTEGRAÇÃO COM PRICING ENGINE
        // ---------------------------------------------------------
        var regraEntity = regraPrecificacaoRepository.findVigente()
                .orElseThrow(() -> new IllegalStateException("Nenhuma regra de precificação vigente encontrada no banco."));

        var regraVigente = new PricingEngine.RegraVigente(
                regraEntity.getId(),
                regraEntity.getPrecoPorLitro(),
                regraEntity.getPrecoPorCm2Divisoria()
        );

        var entradaOrcamento = new PricingEngine.EntradaOrcamento(
                medidas,
                resultado.compartimentos(),
                material.getValorFixoAdicional(),
                BigDecimal.ZERO // Sem frete no preview
        );

        var calculoPreco = new PricingEngine().calcular(entradaOrcamento, regraVigente);

        var orcamentoDto = new OrcamentoPreviewDTO(
                calculoPreco.valorMaterial(),
                calculoPreco.valorDivisorias(),
                calculoPreco.valorVolume(),
                calculoPreco.valorTotal()
        );

        return new PreviewLayoutResponse(
                (int) resultado.larguraUtilMm(),
                (int) resultado.profundidadeUtilMm(),
                req.alturaMm(),
                divisorias,
                List.of(),
                espessura,
                orcamentoDto
        );
    }
}
