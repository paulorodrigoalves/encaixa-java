package br.com.encaixa.admin;

import br.com.encaixa.admin.AdminDtos.AtualizarRegraPrecoRequest;
import br.com.encaixa.admin.AdminDtos.CatalogoAdminDTO;
import br.com.encaixa.admin.AdminDtos.CompartimentoCorteDTO;
import br.com.encaixa.admin.AdminDtos.CompraAdminDTO;
import br.com.encaixa.admin.AdminDtos.CriarMaterialRequest;
import br.com.encaixa.admin.AdminDtos.CriarTipoMaterialRequest;
import br.com.encaixa.admin.AdminDtos.CriarTipoObjetoRequest;
import br.com.encaixa.admin.AdminDtos.EtapaProducaoDTO;
import br.com.encaixa.admin.AdminDtos.MaterialAdminDTO;
import br.com.encaixa.admin.AdminDtos.PecaCorteDTO;
import br.com.encaixa.admin.AdminDtos.PedidoEtapaHistoricoDTO;
import br.com.encaixa.admin.AdminDtos.PlanoCorteProjetoDTO;
import br.com.encaixa.admin.AdminDtos.RegistrarEtapaRequest;
import br.com.encaixa.admin.AdminDtos.RegraPrecificacaoDTO;
import br.com.encaixa.admin.AdminDtos.TipoMaterialAdminDTO;
import br.com.encaixa.admin.AdminDtos.TipoObjetoAdminDTO;
import br.com.encaixa.catalogo.material.Material;
import br.com.encaixa.catalogo.material.MaterialRepository;
import br.com.encaixa.catalogo.material.TipoMaterial;
import br.com.encaixa.catalogo.material.TipoMaterialRepository;
import br.com.encaixa.catalogo.tipoobjeto.TipoObjeto;
import br.com.encaixa.catalogo.tipoobjeto.TipoObjetoRepository;
import br.com.encaixa.precificacao.RegraPrecificacao;
import br.com.encaixa.precificacao.RegraPrecificacaoRepository;
import br.com.encaixa.producao.etapa.EtapaProducao;
import br.com.encaixa.producao.etapa.EtapaProducaoRepository;
import br.com.encaixa.producao.etapa.PedidoEtapa;
import br.com.encaixa.producao.pedido.Pedido;
import br.com.encaixa.producao.pedido.PedidoRepository;
import br.com.encaixa.venda.compartimento.Compartimento;
import br.com.encaixa.venda.compra.Compra;
import br.com.encaixa.venda.compra.CompraRepository;
import br.com.encaixa.venda.projeto.Projeto;
import br.com.encaixa.venda.projeto.ProjetoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
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
    private final ProjetoRepository projetoRepository;
    private final PedidoRepository pedidoRepository;
    private final EtapaProducaoRepository etapaProducaoRepository;
    private final TipoMaterialRepository tipoMaterialRepository;
    private final MaterialRepository materialRepository;
    private final TipoObjetoRepository tipoObjetoRepository;

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

    @Transactional(readOnly = true)
    public List<PlanoCorteProjetoDTO> gerarPlanoCorteCompra(UUID compraId) {
        List<Projeto> projetos = projetoRepository.findAllByCompraIdWithCompartimentos(compraId);
        return projetos.stream().map(this::montarPlanoCorte).toList();
    }

    private PlanoCorteProjetoDTO montarPlanoCorte(Projeto p) {
        int larguraUtil = p.getLarguraMm() - p.getMargemSegurancaMm();
        int profundidadeUtil = p.getProfundidadeMm() - p.getMargemSegurancaMm();
        int altura = p.getAlturaMm();
        int esp = p.getEspessuraMaterialMm();

        List<Compartimento> ordenados = p.getCompartimentos().stream()
                .sorted(Comparator.comparingInt(Compartimento::getPosXMm)
                        .thenComparingInt(Compartimento::getPosYMm))
                .toList();

        // Agrupa os compartimentos por coluna (mesmo posXMm)
        Map<Integer, List<Compartimento>> colunas = new LinkedHashMap<>();
        for (Compartimento c : ordenados) {
            colunas.computeIfAbsent(c.getPosXMm(), k -> new ArrayList<>()).add(c);
        }

        int nColunas = Math.max(1, colunas.size());
        int divisoriasVerticaisInternas = Math.max(0, nColunas - 1);
        int compLateralInterno = Math.max(1, profundidadeUtil - 2 * esp);

        List<PecaCorteDTO> pecas = new ArrayList<>();
        // 1. Paredes externas frontais/traseiras (2 peças inteiras na largura útil)
        pecas.add(new PecaCorteDTO(
                "Parede Externa Frontal / Traseira",
                2,
                larguraUtil,
                altura,
                esp
        ));
        // 2. Paredes externas laterais esquerda/direita (encaixadas entre frontal e traseira)
        pecas.add(new PecaCorteDTO(
                "Parede Externa Lateral (Esq / Dir)",
                2,
                compLateralInterno,
                altura,
                esp
        ));
        // 3. Divisórias verticais internas (se houver mais de 1 coluna)
        if (divisoriasVerticaisInternas > 0) {
            pecas.add(new PecaCorteDTO(
                    "Divisória Longitudinal Interna (Coluna)",
                    divisoriasVerticaisInternas,
                    compLateralInterno,
                    altura,
                    esp
            ));
        }
        // 4. Divisórias horizontais internas de cada coluna (N subdivisões -> N-1 divisórias internas)
        int idxColuna = 1;
        for (List<Compartimento> itensColuna : colunas.values()) {
            int nHorizontaisInternas = Math.max(0, itensColuna.size() - 1);
            if (nHorizontaisInternas > 0) {
                int larguraVaoColuna = itensColuna.getFirst().getLarguraMm();
                pecas.add(new PecaCorteDTO(
                        "Divisória Transversal Interna (Coluna " + idxColuna + ")",
                        nHorizontaisInternas,
                        larguraVaoColuna,
                        altura,
                        esp
                ));
            }
            idxColuna++;
        }

        List<CompartimentoCorteDTO> listaCompartimentos = new ArrayList<>();
        for (int i = 0; i < ordenados.size(); i++) {
            Compartimento c = ordenados.get(i);
            listaCompartimentos.add(new CompartimentoCorteDTO(
                    i + 1,
                    c.getPosXMm(),
                    c.getPosYMm(),
                    c.getLarguraMm(),
                    c.getProfundidadeMm()
            ));
        }

        return new PlanoCorteProjetoDTO(
                p.getId(),
                p.getMaterial().getNome(),
                p.getMaterial().getTipoMaterial().getNome(),
                p.getQuantidade(),
                p.getLarguraMm(),
                p.getProfundidadeMm(),
                altura,
                p.getMargemSegurancaMm(),
                esp,
                larguraUtil,
                profundidadeUtil,
                pecas,
                listaCompartimentos
        );
    }

    @Transactional(readOnly = true)
    public CatalogoAdminDTO obterCatalogoAdmin() {
        var tiposMat = tipoMaterialRepository.findAllByOrderByNomeAsc().stream()
                .map(tm -> new TipoMaterialAdminDTO(tm.getId(), tm.getNome(), tm.getEspessuraMm(), tm.getMargemMm(), tm.isAtivo()))
                .toList();

        var materiais = materialRepository.findAllWithTipoMaterial().stream()
                .map(m -> new MaterialAdminDTO(
                        m.getId(),
                        m.getNome(),
                        m.getCor(),
                        m.getValorFixoAdicional(),
                        m.getTipoMaterial().getId(),
                        m.getTipoMaterial().getNome(),
                        m.getTipoMaterial().getEspessuraMm(),
                        m.getTipoMaterial().getMargemMm(),
                        m.isAtivo()
                ))
                .toList();

        var tiposObj = tipoObjetoRepository.findAll().stream()
                .sorted(Comparator.comparing(TipoObjeto::getCategoria).thenComparing(TipoObjeto::getNome))
                .map(to -> new TipoObjetoAdminDTO(
                        to.getId(),
                        to.getNome(),
                        to.getCategoria(),
                        to.getLarguraMinMm(),
                        to.getLarguraMaxMm(),
                        to.getProfundidadeMinMm(),
                        to.getProfundidadeMaxMm(),
                        to.getAlturaMinMm(),
                        to.isAtivo()
                ))
                .toList();

        return new CatalogoAdminDTO(tiposMat, materiais, tiposObj);
    }

    @Transactional
    public void criarTipoMaterial(CriarTipoMaterialRequest req) {
        TipoMaterial tm = new TipoMaterial();
        tm.setNome(req.nome().trim());
        tm.setEspessuraMm(req.espessuraMm());
        tm.setMargemMm(req.margemMm());
        tm.setAtivo(true);
        tipoMaterialRepository.save(tm);
    }

    @Transactional
    public void criarMaterial(CriarMaterialRequest req) {
        TipoMaterial tm = tipoMaterialRepository.findById(req.tipoMaterialId())
                .orElseThrow(() -> new IllegalArgumentException("Tipo de material não encontrado."));
        Material m = new Material();
        m.setNome(req.nome().trim());
        m.setCor(req.cor().trim());
        m.setValorFixoAdicional(req.valorFixoAdicional());
        m.setTipoMaterial(tm);
        m.setAtivo(true);
        materialRepository.save(m);
    }

    @Transactional
    public void alternarAtivoMaterial(UUID id) {
        Material m = materialRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Material não encontrado."));
        m.setAtivo(!m.isAtivo());
        materialRepository.save(m);
    }

    @Transactional
    public void criarTipoObjeto(CriarTipoObjetoRequest req) {
        TipoObjeto to = new TipoObjeto();
        to.setNome(req.nome().trim());
        to.setCategoria(req.categoria().trim());
        to.setLarguraMinMm(req.larguraMinMm());
        to.setLarguraMaxMm(req.larguraMaxMm());
        to.setProfundidadeMinMm(req.profundidadeMinMm());
        to.setProfundidadeMaxMm(req.profundidadeMaxMm());
        to.setAlturaMinMm(req.alturaMinMm());
        to.setAtivo(true);
        tipoObjetoRepository.save(to);
    }

    @Transactional
    public void alternarAtivoTipoObjeto(UUID id) {
        TipoObjeto to = tipoObjetoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tipo de objeto não encontrado."));
        to.setAtivo(!to.isAtivo());
        tipoObjetoRepository.save(to);
    }
}
