package br.com.encaixa.pagamento;

import br.com.encaixa.admin.AdminService;
import br.com.encaixa.pagamento.MercadoPagoClient.PixCriado;
import br.com.encaixa.pagamento.MercadoPagoClient.StatusPagamentoMp;
import br.com.encaixa.pagamento.PagamentoDtos.PagamentoResponse;
import br.com.encaixa.pagamento.Pagamento.StatusPagamento;
import br.com.encaixa.producao.pedido.PedidoRepository;
import br.com.encaixa.venda.compra.Compra;
import br.com.encaixa.venda.compra.CompraRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Compra compra = carregarCompraDoUsuario(compraId, jwt);

        if (compra.getStatus() == Compra.StatusCompra.APROVADA) {
            throw new IllegalArgumentException("Esta compra ja foi paga.");
        }
        if (compra.getTransportadora() == null) {
            throw new IllegalArgumentException("Finalize o endereco e o frete antes de pagar.");
        }

        // Reaproveita PIX pendente de mesmo valor (evita gerar varias cobrancas para o mesmo carrinho)
        Optional<Pagamento> pendente = pagamentoRepository.findAllByCompraIdOrderByCriadoEmDesc(compraId).stream()
                .filter(p -> p.getStatus() == StatusPagamento.PENDENTE
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

    private PagamentoResponse sincronizar(Pagamento pg) {
        if (pg.getStatus() == StatusPagamento.PENDENTE && pg.getProviderPaymentId() != null && mercadoPago.configurado()) {
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
        log.info("[Pagamento] PIX aprovado para compra {}. Gerando pedido.", compra.getId());
        adminService.aprovarCompraGerarPedido(compra.getId());
    }

    private Compra carregarCompraDoUsuario(UUID compraId, Jwt jwt) {
        Compra compra = compraRepository.findById(compraId)
                .orElseThrow(() -> new IllegalArgumentException("Compra nao encontrada."));
        String emailJwt = extrairEmail(jwt);
        String emailCompra = compra.getCliente().getEmail();
        if (emailCompra == null || !emailCompra.equalsIgnoreCase(emailJwt)) {
            // Mesma resposta de "nao encontrada" para nao vazar a existencia de compras alheias
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
        return new PagamentoResponse(pg.getId(), pg.getCompraId(), pg.getStatus().name(), pg.getValor(),
                pg.getPixCopiaCola(), pg.getPixQrBase64(), pg.getTicketUrl());
    }
}
