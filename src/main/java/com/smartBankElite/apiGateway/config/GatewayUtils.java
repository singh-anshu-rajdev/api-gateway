package com.smartBankElite.apiGateway.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class GatewayUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // Problem Details POJO
    public static class ProblemDetail {
        public String type;
        public String title;
        public int status;
        public String detail;
        public String instance;
        public String description;

        public ProblemDetail(String type, String title, int status, String detail, String instance, String description) {
            this.type = type;
            this.title = title;
            this.status = status;
            this.detail = detail;
            this.instance = instance;
            this.description = description;
        }
    }

    public static Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

        ProblemDetail problem = new ProblemDetail(
                "about:blank",
                "Unauthorized",
                401,
                message,
                exchange.getRequest().getURI().getPath().substring(exchange.getRequest().getURI().getPath().indexOf("/", 1)),
                "Missing or Invalid Token"
        );

        byte[] bytes;
        try {
            bytes = OBJECT_MAPPER.writeValueAsBytes(problem);
        } catch (JsonProcessingException e) {
            // fallback plain message
            bytes = ("{\"detail\":\"" + message + "\"}").getBytes();
        }

        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}