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
import br.com.encaixa.venda.cliente.Cliente;
import br.com.encaixa.venda.cliente.ClienteRepository;
import br.com.encaixa.venda.compartimento.Compartimento;
import br.com.encaixa.venda.compra.Compra;
import br.com.encaixa.venda.compra.CompraRepository;
import br.com.encaixa.venda.projeto.dto.CriarProjetoRequest;
import br.com.encaixa.venda.projeto.dto.ProjetoSalvoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjetoService {

    private final ClienteRepository clienteRepository;
    private final CompraRepository compraRepository;
    private final ProjetoRepository projetoRepository;
    private final OrcamentoRepository orcamentoRepository;
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
            resultadoEscala = new GeradorLayoutPorObjetos().gerar(medidas, req.itensObjeto(), constraints, config);
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
        BigDecimal totalCompra = calculoPreco.valorTotal().multiply(BigDecimal.valueOf(qtd));

        // Cria a Compra associada ao Cliente
        Compra compra = new Compra();
        compra.setCliente(cliente);
        compra.setStatus(Compra.StatusCompra.FECHADA);
        compra.setValorFrete(BigDecimal.ZERO);
        compra.setValorTotal(totalCompra);
        compra = compraRepository.save(compra);

        // Cria o Projeto e seus Compartimentos (respeitando constraint projetos_origem_layout)
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
        projeto.setStatus(Projeto.StatusProjeto.FECHADO);

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

        // Salva o snapshot do Orcamento
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

        return toResponse(projeto, orcamento.getValorTotal(), compartimentoEntities.size());
    }

    @Transactional(readOnly = true)
    public List<ProjetoSalvoResponse> listarMeusProjetos(Jwt jwt) {
        String email = extrairEmail(jwt);
        return clienteRepository.findFirstByEmailIgnoreCase(email)
                .map(cliente -> projetoRepository.findAllByClienteIdWithDetails(cliente.getId()).stream()
                        .map(p -> {
                            BigDecimal valor = orcamentoRepository.findFirstByProjetoIdOrderByCriadoEmDesc(p.getId())
                                    .map(Orcamento::getValorTotal)
                                    .orElse(BigDecimal.ZERO);
                            return toResponse(p, valor, p.getCompartimentos().size());
                        })
                        .toList())
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

    private ProjetoSalvoResponse toResponse(Projeto p, BigDecimal valorUnitario, int totalCompartimentos) {
        return new ProjetoSalvoResponse(
                p.getId(),
                p.getCompra() != null ? p.getCompra().getId() : null,
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
                p.getCriadoEm()
        );
    }
}
