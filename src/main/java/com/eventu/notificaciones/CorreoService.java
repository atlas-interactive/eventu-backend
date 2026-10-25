package com.eventu.notificaciones;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Envío de correos con la API HTTP de SendGrid
 * Si no hay clave configurada, el correo no se envía y se deja un aviso en el log, para que el backend
 * funcione en local sin credenciales
 */
@Service
public class CorreoService {

    private static final Logger LOG = LoggerFactory.getLogger(CorreoService.class);
    private static final String URL_SENDGRID = "https://api.sendgrid.com";

    private final String apiKey;
    private final String remitente;
    private final RestClient cliente;

    public CorreoService(@Value("${eventu.correo.sendgrid-api-key:}") String apiKey,
                         @Value("${eventu.correo.remitente:}") String remitente) {
        this.apiKey = apiKey;
        this.remitente = remitente;
        this.cliente = RestClient.builder().baseUrl(URL_SENDGRID).build();
    }

    /** Indica si hay clave y remitente configurados */
    public boolean estaConfigurado() {
        return apiKey != null && !apiKey.isBlank() && remitente != null && !remitente.isBlank();
    }

    /**
     * Envía un correo de texto plano a un destinatario
     *
     * @throws org.springframework.web.client.RestClientException si SendGrid rechaza la solicitud o no responde
     */
    public void enviar(String destinatario, String asunto, String texto) {
        if (!estaConfigurado()) {
            LOG.warn("Correo no enviado a {}: falta eventu.correo.sendgrid-api-key o eventu.correo.remitente.", destinatario);
            return;
        }
        Map<String, Object> cuerpo = Map.of(
                "personalizations", List.of(Map.of("to", List.of(Map.of("email", destinatario)))),
                "from", Map.of("email", remitente),
                "subject", asunto,
                "content", List.of(Map.of("type", "text/plain", "value", texto)));

        cliente.post()
                .uri("/v3/mail/send")
                .header("Authorization", "Bearer " + apiKey)
                .body(cuerpo)
                .retrieve()
                .toBodilessEntity();
    }
}
