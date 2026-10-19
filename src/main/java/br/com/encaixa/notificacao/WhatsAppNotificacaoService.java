package br.com.encaixa.notificacao;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Cliente de integração com gateway WhatsApp gratuito (ex: WA-AKG / Evolution / WAHA).
 * Quando o webhook não está configurado ou offline, registra o disparo estruturado no log
 * sem interromper a transação da marcenaria.
 */
@Service
@Slf4j
public class WhatsAppNotificacaoService {

    @Value("${encaixa.whatsapp.webhook-url:}")
    private String webhookUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();

    public void notificarAtualizacaoPedido(String nomeCliente, String telefone, String nomeEtapa, String observacao, String codigoRastreio) {
        if (telefone == null || telefone.isBlank() || "00000000000".equals(telefone)) {
            log.info("[WhatsApp] Cliente {} sem telefone válido cadastrado. Notificação ignorada.", nomeCliente);
            return;
        }

        StringBuilder msg = new StringBuilder();
        msg.append("Olá, *").append(nomeCliente).append("*! 🛠️\n");
        msg.append("Seu organizador sob medida *Encaixa* avançou para a etapa: *").append(nomeEtapa).append("*");
        if (observacao != null && !observacao.isBlank()) {
            msg.append("\n📝 Obs: ").append(observacao);
        }
        if (codigoRastreio != null && !codigoRastreio.isBlank()) {
            msg.append("\n📦 Código de Rastreio: *").append(codigoRastreio).append("*");
        }

        String textoFinal = msg.toString();
        log.info("[WhatsApp -> {}] {}", telefone, textoFinal.replace("\n", " | "));

        if (webhookUrl != null && !webhookUrl.isBlank()) {
            try {
                String foneLimpo = telefone.replaceAll("\\D", "");
                String payload = """
                        {"phone":"%s","message":"%s"}
                        """.formatted(foneLimpo, textoFinal.replace("\"", "\\\"").replace("\n", "\\n"));

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(webhookUrl))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(5))
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();

                httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(res -> log.info("[WhatsApp Webhook] Status HTTP: {}", res.statusCode()))
                        .exceptionally(ex -> {
                            log.warn("[WhatsApp Webhook] Falha ao enviar para {}: {}", webhookUrl, ex.getMessage());
                            return null;
                        });
            } catch (Exception e) {
                log.warn("[WhatsApp Webhook] Erro ao montar requisição: {}", e.getMessage());
            }
        }
    }
}
