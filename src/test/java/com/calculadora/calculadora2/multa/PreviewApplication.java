package com.calculadora.calculadora2.multa;

import org.springframework.boot.builder.SpringApplicationBuilder;

public final class PreviewApplication {

    private PreviewApplication() {
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(Calculadora2Application.class)
                .run(
                        "--server.address=127.0.0.1",
                        "--server.port=48272",
                        "--calculadora.browser.auto-open=false"
                );
    }
}
