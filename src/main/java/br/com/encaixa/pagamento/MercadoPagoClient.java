package br.com.encaixa.pagamento;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Cliente HTTP minimo da API do Mercado Pago (somente PIX). Docs: https://www.mercadopago.com.br/developers */
@Component
public class MercadoPagoClient {

    public record PixCriado(String paymentId, String status, String copiaECola, String qrBase64, String ticketUrl) {}

    public record StatusPagamentoMp(String paymentId, String status) {}

    private final RestClient http;
    private final String accessToken;
    private final String notificationUrl;
    private final String emailFallback;

    public MercadoPagoClient(
            @Value("${encaixa.mercadopago.access-token:}") String accessToken,
            @Value("${encaixa.mercadopago.notification-url:}") String notificationUrl,
            @Value("${encaixa.mercadopago.payer-email-fallback:comprador@example.com}") String emailFallback,
            @Value("${encaixa.mercadopago.base-url:https://api.mercadopago.com}") String baseUrl
    ) {
        this.accessToken = accessToken;
        this.notificationUrl = notificationUrl;
        this.emailFallback = emailFallback;
        this.http = RestClient.builder().baseUrl(baseUrl).build();
    }

    public boolean configurado() {
        return accessToken != null && !accessToken.isBlank();
    }

    @SuppressWarnings("unchecked")
    public PixCriado criarPix(UUID idempotencyKey, BigDecimal valor, String descricao, String emailPagador, String referencia) {
        exigirConfiguracao();
        String email = (emailPagador == null || emailPagador.isBlank() || emailPagador.endsWith("@encaixa.local"))
                ? emailFallback : emailPagador;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("transaction_amount", valor);
        body.put("description", descricao);
        body.put("payment_method_id", "pix");
        body.put("external_reference", referencia);
        body.put("payer", Map.of("email", email));
        if (notificationUrl != null && !notificationUrl.isBlank()) {
            body.put("notification_url", notificationUrl);
        }

        Map<String, Object> resp = http.post()
                .uri("/v1/payments")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Idempotency-Key", idempotencyKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        if (resp == null || resp.get("id") == null) {
            throw new IllegalStateException("Resposta inesperada do Mercado Pago ao criar o PIX.");
        }
        Map<String, Object> poi = (Map<String, Object>) resp.getOrDefault("point_of_interaction", Map.of());
        Map<String, Object> td = (Map<String, Object>) poi.getOrDefault("transaction_data", Map.of());
        return new PixCriado(
                String.valueOf(resp.get("id")),
                String.valueOf(resp.get("status")),
                (String) td.get("qr_code"),
                (String) td.get("qr_code_base64"),
                (String) td.get("ticket_url")
        );
    }

    @SuppressWarnings("unchecked")
    public StatusPagamentoMp consultar(String paymentId) {
        exigirConfiguracao();
        Map<String, Object> resp = http.get()
                .uri("/v1/payments/{id}", paymentId)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(Map.class);
        if (resp == null) {
            throw new IllegalStateException("Resposta vazia do Mercado Pago.");
        }
        return new StatusPagamentoMp(paymentId, String.valueOf(resp.get("status")));
    }

    private void exigirConfiguracao() {
        if (!configurado()) {
            throw new IllegalArgumentException(
                    "Pagamento PIX indisponivel: configure MERCADOPAGO_ACCESS_TOKEN no backend.");
        }
    }
}
