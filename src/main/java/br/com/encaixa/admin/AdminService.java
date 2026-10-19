package br.com.encaixa.admin;

import br.com.encaixa.admin.AdminDtos.AtualizarRegraPrecoRequest;
import br.com.encaixa.admin.AdminDtos.CompraAdminDTO;
import br.com.encaixa.admin.AdminDtos.EtapaProducaoDTO;
import br.com.encaixa.admin.AdminDtos.PedidoEtapaHistoricoDTO;
import br.com.encaixa.admin.AdminDtos.RegistrarEtapaRequest;
import br.com.encaixa.admin.AdminDtos.RegraPrecificacaoDTO;
import br.com.encaixa.precificacao.RegraPrecificacao;
import br.com.encaixa.precificacao.RegraPrecificacaoRepository;
import br.com.encaixa.producao.etapa.EtapaProducao;
import br.com.encaixa.producao.etapa.EtapaProducaoRepository;
import br.com.encaixa.producao.etapa.PedidoEtapa;
import br.com.encaixa.producao.pedido.Pedido;
import br.com.encaixa.producao.pedido.PedidoRepository;
import br.com.encaixa.venda.compra.Compra;
import br.com.encaixa.venda.compra.CompraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final RegraPrecificacaoRepository regraPrecificacaoRepository;
    private final CompraRepository compraRepository;
    private final PedidoRepository pedidoRepository;
    private final EtapaProducaoRepository etapaProducaoRepository;

    @Transactional(readOnly = true)
    public RegraPrecificacaoDTO obterRegraVigente() {
        RegraPrecificacao r = regraPrecificacaoRepository.findVigente()
                .orElseThrow(() -> new IllegalStateException("Nenhuma regra vigente encontrada."));
        return new RegraPrecificacaoDTO(
                r.getId(),
                r.getPrecoPorLitro(),
                r.getPrecoPorCm2Divisoria(),
                r.getVigenteDesde(),
                r.getCriadoPor()
        );
    }

    @Transactional
    public RegraPrecificacaoDTO atualizarRegraPreco(AtualizarRegraPrecoRequest req, Jwt jwt) {
        regraPrecificacaoRepository.findVigente().ifPresent(atual -> {
            atual.setVigenteAte(OffsetDateTime.now());
            regraPrecificacaoRepository.save(atual);
        });

        String autor = jwt != null ? jwt.getClaimAsString("preferred_username") : "admin";
        RegraPrecificacao nova = new RegraPrecificacao();
        nova.setPrecoPorLitro(req.precoPorLitro());
        nova.setPrecoPorCm2Divisoria(req.precoPorCm2Divisoria());
        nova.setCriadoPor(autor != null ? autor : "admin");
        nova = regraPrecificacaoRepository.save(nova);

        return new RegraPrecificacaoDTO(
                nova.getId(),
                nova.getPrecoPorLitro(),
                nova.getPrecoPorCm2Divisoria(),
                nova.getVigenteDesde(),
                nova.getCriadoPor()
        );
    }

    @Transactional(readOnly = true)
    public List<EtapaProducaoDTO> listarEtapasCatalogo() {
        return etapaProducaoRepository.findAllByAtivoTrueOrderByOrdemAsc().stream()
                .map(e -> new EtapaProducaoDTO(e.getId(), e.getNome(), e.getOrdem(), e.isUsaRastreio()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CompraAdminDTO> listarComprasEPedidos() {
        List<Compra> compras = compraRepository.findAllWithClienteAndProjetos();
        Map<UUID, Pedido> pedidosPorCompra = pedidoRepository.findAllWithDetails().stream()
                .collect(Collectors.toMap(p -> p.getCompra().getId(), Function.identity(), (a, b) -> a));

        return compras.stream().map(c -> {
            Pedido p = pedidosPorCompra.get(c.getId());
            List<PedidoEtapaHistoricoDTO> historico = p == null ? List.of() : p.getEtapas().stream()
                    .map(pe -> new PedidoEtapaHistoricoDTO(pe.getId(), pe.getNomeEtapa(), pe.getObservacao(), pe.getCriadoEm()))
                    .toList();

            String etapaAtual = historico.isEmpty() ? null : historico.getLast().nomeEtapa();

            return new CompraAdminDTO(
                    c.getId(),
                    c.getCliente().getNome(),
                    c.getCliente().getEmail(),
                    c.getStatus().name(),
                    c.getValorTotal(),
                    c.getProjetos().size(),
                    c.getCriadoEm(),
                    p != null ? p.getId() : null,
                    etapaAtual,
                    p != null ? p.getCodigoRastreio() : null,
                    historico
            );
        }).toList();
    }

    @Transactional
    public void aprovarCompraGerarPedido(UUID compraId) {
        Compra compra = compraRepository.findById(compraId)
                .orElseThrow(() -> new IllegalArgumentException("Compra não encontrada."));

        if (pedidoRepository.findByCompraId(compraId).isPresent()) {
            throw new IllegalArgumentException("Esta compra já possui um pedido de produção.");
        }

        compra.setStatus(Compra.StatusCompra.APROVADA);
        compraRepository.save(compra);

        Pedido pedido = new Pedido();
        pedido.setCompra(compra);
        pedido.setPrazoEstimadoDias(15);

        List<EtapaProducao> etapas = etapaProducaoRepository.findAllByAtivoTrueOrderByOrdemAsc();
        if (!etapas.isEmpty()) {
            EtapaProducao primeira = etapas.getFirst();
            PedidoEtapa pe = new PedidoEtapa();
            pe.setEtapa(primeira);
            pe.setNomeEtapa(primeira.getNome());
            pe.setObservacao("Pagamento confirmado. Pedido enviado para produção.");
            pedido.registrarEtapa(pe);
        }

        pedidoRepository.save(pedido);
    }

    @Transactional
    public void registrarEtapaPedido(UUID pedidoId, RegistrarEtapaRequest req) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado."));

        EtapaProducao etapa = etapaProducaoRepository.findById(req.etapaId())
                .orElseThrow(() -> new IllegalArgumentException("Etapa de produção inválida."));

        if (etapa.isUsaRastreio() && (req.codigoRastreio() == null || req.codigoRastreio().isBlank())) {
            throw new IllegalArgumentException("A etapa \"" + etapa.getNome() + "\" exige informar o código de rastreio.");
        }

        if (req.codigoRastreio() != null && !req.codigoRastreio().isBlank()) {
            pedido.setCodigoRastreio(req.codigoRastreio().trim());
        }

        PedidoEtapa novaEtapa = new PedidoEtapa();
        novaEtapa.setEtapa(etapa);
        novaEtapa.setNomeEtapa(etapa.getNome());
        novaEtapa.setObservacao(req.observacao());
        pedido.registrarEtapa(novaEtapa);

        pedidoRepository.save(pedido);
    }
}
