package com.thuler.gateway.infrastructure.external.authorizer;

import com.thuler.gateway.infrastructure.exception.AuthorizerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthorizerClient {

    private final RestTemplate restTemplate;

    @Value("${authorizer.url}")
    private String authorizerUrl;

    public AuthorizerResponse authorize() {
        try {
            log.info("Consultando autorizador externo: {}", authorizerUrl);

            ResponseEntity<AuthorizerResponse> response = restTemplate.getForEntity(
                    authorizerUrl,
                    AuthorizerResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("Resposta do autorizador: {}", response.getBody());
                return response.getBody();
            }

            log.warn("Autorizador retornou status não esperado: {}", response.getStatusCode());
            throw new AuthorizerException("Autorizador não disponível");

        } catch (RestClientException e) {
            log.error("Erro ao consultar autorizador externo", e);
            throw new AuthorizerException("Falha na comunicação com o autorizador", e);
        }
    }
}