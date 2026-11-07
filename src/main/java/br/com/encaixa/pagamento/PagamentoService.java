package br.com.encaixa.pagamento;

import br.com.encaixa.admin.AdminService;
import br.com.encaixa.pagamento.MercadoPagoClient.PixCriado;
import br.com.encaixa.pagamento.MercadoPagoClient.StatusPagamentoMp;
import br.com.encaixa.pagamento.Pagamento.StatusPagamento;
import br.com.encaixa.pagamento.PagamentoDtos.PagamentoCartaoRequest;
import br.com.encaixa.pagamento.PagamentoDtos.PagamentoResponse;
import br.com.encaixa.producao.pedido.PedidoRepository;
import br.com.encaixa.venda.compra.Compra;
import br.com.encaixa.venda.compra.CompraRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final CompraRepository compraRepository;
    private final PedidoRepository pedidoRepository;
    private final MercadoPagoClient mercadoPago;
    private final AdminService adminService;

    @Transactional
    public PagamentoResponse iniciarPix(UUID compraId, Jwt jwt) {
        Compra compra = validarCompraParaPagamento(compraId, jwt);

        // Reaproveita PIX pendente de mesmo valor (evita gerar varias cobrancas para o mesmo carrinho)
        Optional<Pagamento> pendente = pagamentoRepository.findAllByCompraIdOrderByCriadoEmDesc(compraId).stream()
                .filter(p -> p.getStatus() == StatusPagamento.PENDENTE
                        && p.getProvider() != null
                        && p.getProvider().contains("PIX")
                        && p.getProviderPaymentId() != null
                        && p.getValor().compareTo(compra.getValorTotal()) == 0)
                .findFirst();
        if (pendente.isPresent()) {
            return sincronizar(pendente.get());
        }

        Pagamento pg = new Pagamento();
        pg.setCompraId(compraId);
        pg.setValor(compra.getValorTotal());
        pg = pagamentoRepository.save(pg);

        if (mercadoPago.configurado()) {
            pg.setProvider("MERCADO_PAGO_PIX");
            PixCriado pix = mercadoPago.criarPix(
                    pg.getId(),
                    pg.getValor(),
                    "Encaixa - Pedido " + compraId.toString().substring(0, 8),
                    compra.getCliente().getEmail(),
                    compraId.toString()
            );
            pg.setProviderPaymentId(pix.paymentId());
            pg.setPixCopiaCola(pix.copiaECola());
            pg.setPixQrBase64(pix.qrBase64());
            pg.setTicketUrl(pix.ticketUrl());
            pg.setStatus(mapear(pix.status()));
        } else {
            // Modo Sandbox / Demonstração local quando MERCADOPAGO_ACCESS_TOKEN não está definido
            String refCurta = compraId.toString().replace("-", "").substring(0, 12).toUpperCase();
            String copiaCola = "00020126580014BR.GOV.BCB.PIX0136encaixa-pagamentos@encaixa.com.br5204000053039865406"
                    + pg.getValor().toPlainString() + "5802BR5915ENCAIXA MARCEN6009SAO PAULO62160512" + refCurta + "6304ABCD";
            pg.setProvider("SIMULADO_PIX");
            pg.setProviderPaymentId("PIX-SIM-" + pg.getId());
            pg.setPixCopiaCola(copiaCola);
            pg.setPixQrBase64(gerarQrCodeSimuladoBase64(copiaCola));
            pg.setStatus(StatusPagamento.PENDENTE);
        }

        pg = pagamentoRepository.save(pg);
        aplicarEfeitos(pg);
        return toResponse(pg);
    }

    @Transactional
    public PagamentoResponse pagarComCartao(UUID compraId, PagamentoCartaoRequest req, Jwt jwt) {
        Compra compra = validarCompraParaPagamento(compraId, jwt);

        String digitosCartao = req.numeroCartao() == null ? "" : req.numeroCartao().replaceAll("\\D", "");
        if (digitosCartao.length() < 13 || digitosCartao.length() > 19) {
            throw new IllegalArgumentException("Número do cartão inválido (informe de 13 a 16 dígitos).");
        }
        if (req.nomeTitular() == null || req.nomeTitular().trim().length() < 3) {
            throw new IllegalArgumentException("Informe o nome impresso no cartão.");
        }
        if (req.validade() == null || !req.validade().trim().matches("^(0[1-9]|1[0-2])/\\d{2,4}$")) {
            throw new IllegalArgumentException("Validade inválida. Use o formato MM/AA.");
        }
        if (req.cvv() == null || !req.cvv().trim().matches("^\\d{3,4}$")) {
            throw new IllegalArgumentException("CVV inválido (3 ou 4 dígitos).");
        }
        int parcelas = (req.parcelas() == null || req.parcelas() < 1) ? 1 : Math.min(req.parcelas(), 12);

        Pagamento pg = new Pagamento();
        pg.setCompraId(compraId);
        pg.setProvider("CARTAO_CREDITO_" + parcelas + "X");
        pg.setProviderPaymentId("CC-" + UUID.randomUUID());
        pg.setValor(compra.getValorTotal());
        pg.setStatus(StatusPagamento.APROVADO);
        pg = pagamentoRepository.save(pg);

        aplicarEfeitos(pg);
        return toResponse(pg);
    }

    @Transactional
    public PagamentoResponse iniciarBoleto(UUID compraId, Jwt jwt) {
        Compra compra = validarCompraParaPagamento(compraId, jwt);

        Optional<Pagamento> pendente = pagamentoRepository.findAllByCompraIdOrderByCriadoEmDesc(compraId).stream()
                .filter(p -> p.getStatus() == StatusPagamento.PENDENTE
                        && "BOLETO_BANCARIO".equals(p.getProvider())
                        && p.getValor().compareTo(compra.getValorTotal()) == 0)
                .findFirst();
        if (pendente.isPresent()) {
            return toResponse(pendente.get());
        }

        String valorCentavos = String.format("%010d", compra.getValorTotal().movePointRight(2).longValue());
        String linhaDigitavel = "23793.38128 60082.000126 34000.063305 8 9895" + valorCentavos;

        Pagamento pg = new Pagamento();
        pg.setCompraId(compraId);
        pg.setProvider("BOLETO_BANCARIO");
        pg.setProviderPaymentId("BOL-" + UUID.randomUUID());
        pg.setValor(compra.getValorTotal());
        pg.setPixCopiaCola(linhaDigitavel);
        pg.setStatus(StatusPagamento.PENDENTE);
        pg = pagamentoRepository.save(pg);

        return toResponse(pg);
    }

    @Transactional
    public PagamentoResponse simularAprovacao(UUID compraId, Jwt jwt) {
        carregarCompraDoUsuario(compraId, jwt);
        Pagamento pg = pagamentoRepository.findAllByCompraIdOrderByCriadoEmDesc(compraId).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Nenhum pagamento iniciado para esta compra."));

        pg.setStatus(StatusPagamento.APROVADO);
        pg = pagamentoRepository.save(pg);
        aplicarEfeitos(pg);
        return toResponse(pg);
    }

    @Transactional
    public PagamentoResponse obterStatus(UUID compraId, Jwt jwt) {
        carregarCompraDoUsuario(compraId, jwt);
        Pagamento pg = pagamentoRepository.findAllByCompraIdOrderByCriadoEmDesc(compraId).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Nenhum pagamento iniciado para esta compra."));
        return sincronizar(pg);
    }

    /** Chamado pelo webhook: so processa pagamentos que conhecemos e SEMPRE re-consulta o status na API do MP. */
    @Transactional
    public void processarWebhook(String providerPaymentId) {
        pagamentoRepository.findByProviderPaymentId(providerPaymentId).ifPresentOrElse(
                this::sincronizar,
                () -> log.warn("[Pagamento] Webhook para pagamento desconhecido: {}", providerPaymentId));
    }

    private Compra validarCompraParaPagamento(UUID compraId, Jwt jwt) {
        Compra compra = carregarCompraDoUsuario(compraId, jwt);
        if (compra.getStatus() == Compra.StatusCompra.APROVADA) {
            throw new IllegalArgumentException("Esta compra já foi paga.");
        }
        if (compra.getTransportadora() == null) {
            throw new IllegalArgumentException("Finalize o endereço e o frete antes de pagar.");
        }
        return compra;
    }

    private PagamentoResponse sincronizar(Pagamento pg) {
        if (pg.getStatus() == StatusPagamento.PENDENTE
                && pg.getProviderPaymentId() != null
                && pg.getProvider() != null
                && pg.getProvider().startsWith("MERCADO_PAGO")
                && mercadoPago.configurado()) {
            StatusPagamentoMp atual = mercadoPago.consultar(pg.getProviderPaymentId());
            StatusPagamento novo = mapear(atual.status());
            if (novo != pg.getStatus()) {
                pg.setStatus(novo);
                pg = pagamentoRepository.save(pg);
            }
        }
        aplicarEfeitos(pg);
        return toResponse(pg);
    }

    /** Idempotente: pagamento aprovado aprova a compra e gera o Pedido uma unica vez. */
    private void aplicarEfeitos(Pagamento pg) {
        if (pg.getStatus() != StatusPagamento.APROVADO) {
            return;
        }
        Compra compra = compraRepository.findById(pg.getCompraId()).orElse(null);
        if (compra == null || compra.getStatus() == Compra.StatusCompra.APROVADA
                || pedidoRepository.findByCompraId(compra.getId()).isPresent()) {
            return;
        }
        log.info("[Pagamento] Pagamento aprovado ({}) para compra {}. Gerando pedido.", pg.getProvider(), compra.getId());
        adminService.aprovarCompraGerarPedido(compra.getId());
    }

    private Compra carregarCompraDoUsuario(UUID compraId, Jwt jwt) {
        Compra compra = compraRepository.findById(compraId)
                .orElseThrow(() -> new IllegalArgumentException("Compra nao encontrada."));
        String emailJwt = extrairEmail(jwt);
        String emailCompra = compra.getCliente().getEmail();
        if (emailCompra == null || !emailCompra.equalsIgnoreCase(emailJwt)) {
            throw new IllegalArgumentException("Compra nao encontrada.");
        }
        return compra;
    }

    private String extrairEmail(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            String username = jwt.getClaimAsString("preferred_username");
            email = (username != null ? username : jwt.getSubject()) + "@encaixa.local";
        }
        return email;
    }

    private StatusPagamento mapear(String statusMp) {
        if (statusMp == null) {
            return StatusPagamento.PENDENTE;
        }
        return switch (statusMp) {
            case "approved", "authorized" -> StatusPagamento.APROVADO;
            case "rejected" -> StatusPagamento.REJEITADO;
            case "cancelled", "expired" -> StatusPagamento.CANCELADO;
            case "refunded", "charged_back" -> StatusPagamento.ESTORNADO;
            default -> StatusPagamento.PENDENTE;
        };
    }

    private PagamentoResponse toResponse(Pagamento pg) {
        String provider = pg.getProvider() != null ? pg.getProvider() : "PIX";
        String metodo = "PIX";
        int parcelas = 1;

        if (provider.startsWith("CARTAO_CREDITO")) {
            metodo = "CARTAO_CREDITO";
            String num = provider.replaceAll("\\D", "");
            if (!num.isEmpty()) {
                parcelas = Integer.parseInt(num);
            }
        } else if (provider.contains("BOLETO")) {
            metodo = "BOLETO";
        }

        boolean simulado = !provider.startsWith("MERCADO_PAGO") || !mercadoPago.configurado();

        return new PagamentoResponse(
                pg.getId(),
                pg.getCompraId(),
                metodo,
                parcelas,
                pg.getStatus().name(),
                pg.getValor(),
                pg.getPixCopiaCola(),
                pg.getPixQrBase64(),
                pg.getTicketUrl(),
                simulado
        );
    }

    private String gerarQrCodeSimuladoBase64(String semente) {
        try {
            int grade = 25;
            int escala = 8;
            int margem = 2;
            int tamanho = (grade + margem * 2) * escala;

            BufferedImage img = new BufferedImage(tamanho, tamanho, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, tamanho, tamanho);
            g.setColor(Color.BLACK);

            int hash = semente.hashCode();
            for (int y = 0; y < grade; y++) {
                for (int x = 0; x < grade; x++) {
                    boolean cantoSuperiorEsq = x < 7 && y < 7;
                    boolean cantoSuperiorDir = x >= grade - 7 && y < 7;
                    boolean cantoInferiorEsq = x < 7 && y >= grade - 7;

                    boolean pintar;
                    if (cantoSuperiorEsq || cantoSuperiorDir || cantoInferiorEsq) {
                        int lx = x >= grade - 7 ? x - (grade - 7) : x;
                        int ly = y >= grade - 7 ? y - (grade - 7) : y;
                        pintar = lx == 0 || lx == 6 || ly == 0 || ly == 6 || (lx >= 2 && lx <= 4 && ly >= 2 && ly <= 4);
                    } else {
                        int v = (x * 31 + y * 17 + hash) ^ (x * y);
                        pintar = (v & 1) == 0 && ((x + y + hash) % 3 != 0);
                    }

                    if (pintar) {
                        g.fillRect((x + margem) * escala, (y + margem) * escala, escala, escala);
                    }
                }
            }
            g.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "png", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }
}
