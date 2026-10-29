package br.com.encaixa.pagamento;

import br.com.encaixa.pagamento.PagamentoDtos.PagamentoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PagamentoController {

    private final PagamentoService pagamentoService;

    @Value("${encaixa.mercadopago.webhook-secret:}")
    private String webhookSecret;

    @PostMapping("/api/compras/{compraId}/pagamento/pix")
    public ResponseEntity<PagamentoResponse> iniciarPix(@PathVariable UUID compraId, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(pagamentoService.iniciarPix(compraId, jwt));
    }

    @GetMapping("/api/compras/{compraId}/pagamento")
    public ResponseEntity<PagamentoResponse> status(@PathVariable UUID compraId, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(pagamentoService.obterStatus(compraId, jwt));
    }

    /** Webhook do Mercado Pago (publico). Sempre responde 200 para evitar retentativas infinitas. */
    @PostMapping("/api/public/pagamentos/webhook")
    public ResponseEntity<Void> webhook(
            @RequestParam(name = "data.id", required = false) String dataId,
            @RequestParam(name = "id", required = false) String legacyId,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "topic", required = false) String topic,
            @RequestHeader(name = "x-signature", required = false) String signature,
            @RequestHeader(name = "x-request-id", required = false) String requestId,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        String tipo = type != null ? type : topic;
        String paymentId = dataId != null ? dataId : legacyId;
        if (paymentId == null && body != null && body.get("data") instanceof Map<?, ?> data && data.get("id") != null) {
            paymentId = String.valueOf(data.get("id"));
            if (tipo == null && body.get("type") != null) {
                tipo = String.valueOf(body.get("type"));
            }
        }
        if (!"payment".equals(tipo) || paymentId == null) {
            return ResponseEntity.ok().build();
        }
        if (!assinaturaValida(signature, requestId, dataId != null ? dataId : paymentId)) {
            log.warn("[Pagamento] Webhook com assinatura invalida ignorado.");
            return ResponseEntity.status(401).build();
        }
        try {
            pagamentoService.processarWebhook(paymentId);
        } catch (RuntimeException ex) {
            log.error("[Pagamento] Falha ao processar webhook {}: {}", paymentId, ex.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    /** Valida x-signature (HMAC-SHA256) quando MERCADOPAGO_WEBHOOK_SECRET esta configurado. */
    private boolean assinaturaValida(String signature, String requestId, String dataId) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            return true;
        }
        if (signature == null) {
            return false;
        }
        String ts = null;
        String v1 = null;
        for (String part : signature.split(",")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length == 2) {
                if (kv[0].equals("ts")) ts = kv[1];
                if (kv[0].equals("v1")) v1 = kv[1];
            }
        }
        if (ts == null || v1 == null) {
            return false;
        }
        String manifest = "id:" + dataId.toLowerCase() + ";request-id:" + (requestId == null ? "" : requestId) + ";ts:" + ts + ";";
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String esperado = HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));
            return MessageDigest.isEqual(esperado.getBytes(StandardCharsets.UTF_8), v1.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }
}
