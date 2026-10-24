package com.calculadora.calculadora2;

import com.calculadora.calculadora2.multa.Calculadora2Application;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.WebApplicationType;
import com.calculadora.calculadora2.multa.service.BaseContratoService;
import static org.junit.jupiter.api.Assertions.*;

class Calculadora2ApplicationTests {

	@Test
	void contextLoads() {
		try (var context = new SpringApplicationBuilder(Calculadora2Application.class)
				.web(WebApplicationType.NONE)
				.run("--calculadora.browser.auto-open=false")) {
			assertTrue(context.isActive());
			assertEquals(3, context.getBean(BaseContratoService.class).quantidadeRegistros());
		}
	}

}
