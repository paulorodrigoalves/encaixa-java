package br.com.encaixa.venda.layout;

import br.com.encaixa.catalogo.material.MaterialRepository;
import br.com.encaixa.catalogo.template.TemplateRepository;
import br.com.encaixa.catalogo.tipoobjeto.TipoObjeto;
import br.com.encaixa.catalogo.tipoobjeto.TipoObjetoRepository;
import br.com.encaixa.domain.engine.EscalaTemplateEngine;
import br.com.encaixa.domain.engine.GeradorLayoutPorObjetos;
import br.com.encaixa.domain.engine.PricingEngine;
import br.com.encaixa.domain.engine.model.Modelos.Compartimento;
import br.com.encaixa.domain.engine.model.Modelos.ConfigEscala;
import br.com.encaixa.domain.engine.model.Modelos.ItemObjetoSelecionado;
import br.com.encaixa.domain.engine.model.Modelos.MedidasGaveta;
import br.com.encaixa.domain.engine.model.Modelos.ResultadoEscala;
import br.com.encaixa.domain.engine.model.Modelos.TipoObjetoConstraint;
import br.com.encaixa.precificacao.RegraPrecificacaoRepository;
import br.com.encaixa.venda.layout.dto.PreviewLayoutRequest;
import br.com.encaixa.venda.layout.dto.PreviewLayoutResponse;
import br.com.encaixa.venda.layout.dto.PreviewLayoutResponse.DivisoriaRenderDTO;
import br.com.encaixa.venda.layout.dto.PreviewLayoutResponse.ObjetoPosicionadoDTO;
import br.com.encaixa.venda.layout.dto.PreviewLayoutResponse.ObjetoRenderDTO;
import br.com.encaixa.venda.layout.dto.PreviewLayoutResponse.OrcamentoPreviewDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LayoutService {

    private static final String[] CORES_PALETA = {
            "#e0f2fe", "#dcfce7", "#fef3c7", "#fce7f3", "#ede9fe", "#ffedd5", "#ccfbf1"
    };

    private final MaterialRepository materialRepository;
    private final TemplateRepository templateRepository;
    private final TipoObjetoRepository tipoObjetoRepository;
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

        ResultadoEscala resultado;
        List<ObjetoPosicionadoDTO> objetosPosicionados = new ArrayList<>();

        if (req.templateId() != null) {
            var template = templateRepository.findById(req.templateId())
                    .orElseThrow(() -> new IllegalArgumentException("Template invalido"));
            resultado = new EscalaTemplateEngine().escalar(template.getLayoutProporcional(), medidas, config);
        } else if (req.itensObjeto() != null && !req.itensObjeto().isEmpty()) {
            List<TipoObjeto> catalogoTipos = tipoObjetoRepository.findAllByAtivoTrue();
            List<TipoObjetoConstraint> constraints = catalogoTipos.stream()
                    .map(t -> new TipoObjetoConstraint(
                            t.getId(),
                            t.getNome(),
                            t.getLarguraMinMm(),
                            t.getLarguraMaxMm(),
                            t.getProfundidadeMinMm(),
                            t.getProfundidadeMaxMm(),
                            t.getAlturaMinMm()
                    ))
                    .toList();

            resultado = new GeradorLayoutPorObjetos().gerar(medidas, req.itensObjeto(), constraints, config);
            objetosPosicionados = montarObjetosPosicionados(req.itensObjeto(), constraints, resultado.compartimentos());
        } else {
            throw new IllegalArgumentException("Informe um templateId ou uma lista de itensObjeto.");
        }

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
                BigDecimal.ZERO
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
                objetosPosicionados,
                espessura,
                orcamentoDto
        );
    }

    private List<ObjetoPosicionadoDTO> montarObjetosPosicionados(
            List<ItemObjetoSelecionado> itensSelecionados,
            List<TipoObjetoConstraint> catalogo,
            List<Compartimento> compartimentos
    ) {
        Map<UUID, TipoObjetoConstraint> mapa = catalogo.stream()
                .collect(Collectors.toMap(TipoObjetoConstraint::id, Function.identity(), (a, b) -> a));

        List<TipoObjetoConstraint> unidades = new ArrayList<>();
        for (ItemObjetoSelecionado item : itensSelecionados) {
            if (item.quantidade() <= 0) continue;
            TipoObjetoConstraint tipo = mapa.get(item.tipoObjetoId());
            if (tipo != null) {
                for (int i = 0; i < item.quantidade(); i++) {
                    unidades.add(tipo);
                }
            }
        }
        unidades.sort(Comparator.comparingInt(TipoObjetoConstraint::larguraMinMm).reversed());

        List<ObjetoPosicionadoDTO> resultado = new ArrayList<>();
        int limite = Math.min(unidades.size(), compartimentos.size());
        for (int i = 0; i < limite; i++) {
            TipoObjetoConstraint u = unidades.get(i);
            Compartimento c = compartimentos.get(i);
            String cor = CORES_PALETA[Math.abs(u.id().hashCode()) % CORES_PALETA.length];
            resultado.add(new ObjetoPosicionadoDTO(
                    new ObjetoRenderDTO(u.id() + "-" + i, u.nome(), cor),
                    c.posXMm(),
                    c.posYMm(),
                    c.larguraMm(),
                    c.profundidadeMm()
            ));
        }
        return resultado;
    }
}
