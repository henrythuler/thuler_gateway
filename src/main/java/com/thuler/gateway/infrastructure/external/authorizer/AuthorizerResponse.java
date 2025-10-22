package com.thuler.gateway.infrastructure.external.authorizer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizerResponse {

    @JsonProperty("status")
    private String status;

    @JsonProperty("data")
    private AuthorizerData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorizerData {
        @JsonProperty("authorized")
        private Boolean authorized;
    }

    public boolean isAutorizado() {
        return data != null && Boolean.TRUE.equals(data.getAuthorized());
    }
}