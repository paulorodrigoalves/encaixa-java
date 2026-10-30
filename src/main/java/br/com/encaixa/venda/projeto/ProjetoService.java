package br.com.encaixa.venda.projeto;

import br.com.encaixa.catalogo.material.MaterialRepository;
import br.com.encaixa.catalogo.template.Template;
import br.com.encaixa.catalogo.template.TemplateRepository;
import br.com.encaixa.catalogo.tipoobjeto.TipoObjeto;
import br.com.encaixa.catalogo.tipoobjeto.TipoObjetoRepository;
import br.com.encaixa.domain.engine.EscalaTemplateEngine;
import br.com.encaixa.domain.engine.GeradorLayoutPorObjetos;
import br.com.encaixa.domain.engine.PricingEngine;
import br.com.encaixa.domain.engine.model.Modelos.ConfigEscala;
import br.com.encaixa.domain.engine.model.Modelos.MedidasGaveta;
import br.com.encaixa.domain.engine.model.Modelos.ResultadoEscala;
import br.com.encaixa.domain.engine.model.Modelos.TipoObjetoConstraint;
import br.com.encaixa.precificacao.Orcamento;
import br.com.encaixa.precificacao.OrcamentoRepository;
import br.com.encaixa.precificacao.RegraPrecificacaoRepository;
import br.com.encaixa.producao.pedido.Pedido;
import br.com.encaixa.producao.pedido.PedidoRepository;
import br.com.encaixa.venda.cliente.Cliente;
import br.com.encaixa.venda.cliente.ClienteRepository;
import br.com.encaixa.venda.compartimento.Compartimento;
import br.com.encaixa.venda.compra.Compra;
import br.com.encaixa.venda.compra.CompraRepository;
import br.com.encaixa.venda.projeto.dto.CriarProjetoRequest;
import br.com.encaixa.venda.projeto.dto.ProjetoSalvoResponse;
import br.com.encaixa.venda.projeto.dto.ProjetoSalvoResponse.EtapaClienteDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjetoService {

    private final ClienteRepository clienteRepository;
    private final CompraRepository compraRepository;
    private final ProjetoRepository projetoRepository;
    private final OrcamentoRepository orcamentoRepository;
    private final PedidoRepository pedidoRepository;
    private final MaterialRepository materialRepository;
    private final TemplateRepository templateRepository;
    private final TipoObjetoRepository tipoObjetoRepository;
    private final RegraPrecificacaoRepository regraPrecificacaoRepository;

    @Transactional
    public ProjetoSalvoResponse salvarProjeto(CriarProjetoRequest req, Jwt jwt) {
        Cliente cliente = resolverOuCriarCliente(jwt);

        var material = materialRepository.findById(req.materialId())
                .orElseThrow(() -> new IllegalArgumentException("Material inválido"));
        var regraEntity = regraPrecificacaoRepository.findVigente()
                .orElseThrow(() -> new IllegalStateException("Nenhuma regra de precificação vigente."));

        var config = new ConfigEscala(
                material.getTipoMaterial().getEspessuraMm(),
                material.getTipoMaterial().getMargemMm()
        );
        var medidas = new MedidasGaveta(req.larguraMm(), req.profundidadeMm(), req.alturaMm());

        Template template = null;
        ResultadoEscala resultadoEscala;

        if (req.templateId() != null) {
            template = templateRepository.findById(req.templateId())
                    .orElseThrow(() -> new IllegalArgumentException("Template inválido"));
            resultadoEscala = new EscalaTemplateEngine().escalar(template.getLayoutProporcional(), medidas, config);
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
            resultadoEscala = new GeradorLayoutPorObjetos().gerarDetalhado(medidas, req.itensObjeto(), constraints, config,
                    br.com.encaixa.domain.engine.EstrategiaLayout.de(req.estrategiaLayout())).escala();
        } else {
            throw new IllegalArgumentException("Informe um templateId ou itensObjeto para salvar o projeto.");
        }

        var regraVigente = new PricingEngine.RegraVigente(
                regraEntity.getId(),
                regraEntity.getPrecoPorLitro(),
                regraEntity.getPrecoPorCm2Divisoria()
        );
        var entradaOrcamento = new PricingEngine.EntradaOrcamento(
                medidas,
                resultadoEscala.compartimentos(),
                material.getValorFixoAdicional(),
                BigDecimal.ZERO
        );
        var calculoPreco = new PricingEngine().calcular(entradaOrcamento, regraVigente);

        int qtd = req.quantidade() != null && req.quantidade() > 0 ? req.quantidade() : 1;
        BigDecimal subtotalProjeto = calculoPreco.valorTotal().multiply(BigDecimal.valueOf(qtd));

        // Reutiliza a compra em andamento do cliente (status FECHADA e ainda sem transportadora fechada)
        Compra compra = compraRepository.findAllByClienteIdOrderByCriadoEmDesc(cliente.getId()).stream()
                .filter(c -> c.getStatus() == Compra.StatusCompra.FECHADA && c.getTransportadora() == null)
                .findFirst()
                .orElseGet(() -> {
                    Compra nova = new Compra();
                    nova.setCliente(cliente);
                    nova.setStatus(Compra.StatusCompra.FECHADA);
                    nova.setValorFrete(BigDecimal.ZERO);
                    nova.setValorTotal(BigDecimal.ZERO);
                    return nova;
                });

        compra.setValorTotal(compra.getValorTotal().add(subtotalProjeto));
        compra = compraRepository.save(compra);

        Projeto projeto = new Projeto();
        projeto.setCompra(compra);
        if (template != null) {
            projeto.setTemplate(template);
            projeto.setItensObjeto(null);
        } else {
            projeto.setTemplate(null);
            projeto.setItensObjeto(req.itensObjeto());
        }
        projeto.setMaterial(material);
        projeto.setLarguraMm(req.larguraMm());
        projeto.setProfundidadeMm(req.profundidadeMm());
        projeto.setAlturaMm(req.alturaMm());
        projeto.setEspessuraMaterialMm(material.getTipoMaterial().getEspessuraMm());
        projeto.setMargemSegurancaMm(material.getTipoMaterial().getMargemMm());
        projeto.setQuantidade(qtd);
        projeto.setStatus(Projeto.StatusProjeto.RASCUNHO);

        List<Compartimento> compartimentoEntities = resultadoEscala.compartimentos().stream().map(c -> {
            Compartimento comp = new Compartimento();
            comp.setPosXMm(c.posXMm());
            comp.setPosYMm(c.posYMm());
            comp.setLarguraMm(c.larguraMm());
            comp.setProfundidadeMm(c.profundidadeMm());
            comp.setAreaPaineisCm2(BigDecimal.valueOf(c.areaPaineisCm2()));
            return comp;
        }).toList();

        projeto.substituirCompartimentos(compartimentoEntities);
        projeto = projetoRepository.save(projeto);

        Orcamento orcamento = new Orcamento();
        orcamento.setProjeto(projeto);
        orcamento.setRegraPrecificacao(regraEntity);
        orcamento.setVolumeTotalLitros(calculoPreco.volumeTotalLitros());
        orcamento.setAreaDivisoriasCm2(calculoPreco.areaDivisoriasCm2());
        orcamento.setValorVolume(calculoPreco.valorVolume());
        orcamento.setValorDivisorias(calculoPreco.valorDivisorias());
        orcamento.setValorMaterial(calculoPreco.valorMaterial());
        orcamento.setValorFrete(calculoPreco.valorFrete());
        orcamento.setValorTotal(calculoPreco.valorTotal());
        orcamentoRepository.save(orcamento);

        return toResponse(projeto, orcamento.getValorTotal(), compartimentoEntities.size(), null);
    }

    @Transactional(readOnly = true)
    public List<ProjetoSalvoResponse> listarMeusProjetos(Jwt jwt) {
        String email = extrairEmail(jwt);
        return clienteRepository.findFirstByEmailIgnoreCase(email)
                .map(cliente -> {
                    Map<UUID, Pedido> pedidosPorCompra = pedidoRepository.findAllByClienteIdWithEtapas(cliente.getId()).stream()
                            .collect(Collectors.toMap(p -> p.getCompra().getId(), Function.identity(), (a, b) -> a));

                    return projetoRepository.findAllByClienteIdWithDetails(cliente.getId()).stream()
                            .map(p -> {
                                BigDecimal valor = orcamentoRepository.findFirstByProjetoIdOrderByCriadoEmDesc(p.getId())
                                        .map(Orcamento::getValorTotal)
                                        .orElse(BigDecimal.ZERO);
                                Pedido pedido = p.getCompra() != null ? pedidosPorCompra.get(p.getCompra().getId()) : null;
                                return toResponse(p, valor, p.getCompartimentos().size(), pedido);
                            })
                            .toList();
                })
                .orElse(List.of());
    }

    private Cliente resolverOuCriarCliente(Jwt jwt) {
        String email = extrairEmail(jwt);
        return clienteRepository.findFirstByEmailIgnoreCase(email)
                .orElseGet(() -> {
                    Cliente novo = new Cliente();
                    String nome = jwt.getClaimAsString("name");
                    if (nome == null || nome.isBlank()) {
                        nome = jwt.getClaimAsString("preferred_username");
                    }
                    novo.setNome(nome != null ? nome : email);
                    novo.setEmail(email);
                    novo.setTelefone("00000000000");
                    return clienteRepository.save(novo);
                });
    }

    private String extrairEmail(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            String username = jwt.getClaimAsString("preferred_username");
            email = (username != null ? username : jwt.getSubject()) + "@encaixa.local";
        }
        return email;
    }

    private ProjetoSalvoResponse toResponse(Projeto p, BigDecimal valorUnitario, int totalCompartimentos, Pedido pedido) {
        List<EtapaClienteDTO> historico = pedido == null ? List.of() : pedido.getEtapas().stream()
                .map(pe -> new EtapaClienteDTO(pe.getNomeEtapa(), pe.getObservacao(), pe.getCriadoEm()))
                .toList();

        String etapaAtual = historico.isEmpty() ? null : historico.getLast().nomeEtapa();
        Compra c = p.getCompra();

        int espessura = p.getEspessuraMaterialMm();
        int larguraUtil = p.getLarguraMm() - p.getMargemSegurancaMm();
        int profundidadeUtil = p.getProfundidadeMm() - p.getMargemSegurancaMm();

        List< br.com.encaixa.venda.layout.dto.PreviewLayoutResponse.DivisoriaRenderDTO > divisorias = new java.util.ArrayList<>();
        for (Compartimento comp : p.getCompartimentos()) {
            if (comp.getPosXMm() + comp.getLarguraMm() + espessura < larguraUtil) {
                divisorias.add(new br.com.encaixa.venda.layout.dto.PreviewLayoutResponse.DivisoriaRenderDTO(
                        comp.getPosXMm() + comp.getLarguraMm(),
                        comp.getPosYMm(),
                        espessura,
                        comp.getProfundidadeMm(),
                        "VERTICAL"
                ));
            }
            if (comp.getPosYMm() + comp.getProfundidadeMm() + espessura < profundidadeUtil) {
                divisorias.add(new br.com.encaixa.venda.layout.dto.PreviewLayoutResponse.DivisoriaRenderDTO(
                        comp.getPosXMm(),
                        comp.getPosYMm() + comp.getProfundidadeMm(),
                        comp.getLarguraMm(),
                        espessura,
                        "HORIZONTAL"
                ));
            }
        }

        return new ProjetoSalvoResponse(
                p.getId(),
                c != null ? c.getId() : null,
                c != null ? c.getStatus().name() : "FECHADA",
                p.getLarguraMm(),
                p.getProfundidadeMm(),
                p.getAlturaMm(),
                p.getEspessuraMaterialMm(),
                p.getMargemSegurancaMm(),
                p.getQuantidade(),
                p.getStatus().name(),
                p.getMaterial().getNome(),
                p.getTemplate() != null ? p.getTemplate().getNome() : "Personalizado por Objetos",
                totalCompartimentos,
                valorUnitario,
                c != null ? c.getTransportadora() : null,
                c != null ? c.getValorFrete() : BigDecimal.ZERO,
                c != null ? c.getPrazoFreteDias() : null,
                p.getCriadoEm(),
                etapaAtual,
                pedido != null ? pedido.getCodigoRastreio() : null,
                pedido != null ? pedido.getPrazoEstimadoDias() : null,
                historico,
                divisorias
        );
    }
}
