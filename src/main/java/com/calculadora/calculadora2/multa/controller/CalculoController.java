package com.calculadora.calculadora2.multa.controller;

import com.calculadora.calculadora2.multa.service.MultaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
public class CalculoController {

    private final MultaService multaService;

    public CalculoController(MultaService multaService) {
        this.multaService = multaService;
    }

    @GetMapping("/")
    public String formulario(Model model){
        model.addAttribute("percentualDemonstracao", multaService.percentual().movePointRight(2).stripTrailingZeros().toPlainString());
        return "formulario";
    }

    @PostMapping("/calcular")
    public String calcular(

            @RequestParam String cliente,
            @RequestParam String dataSolicitacao,
            @RequestParam(required = false) String dataCancelamento,
            @RequestParam String iccid,
            @RequestParam String linha,
            @RequestParam String operadora,
            @RequestParam String dataInicio,
            @RequestParam int duracao,
            @RequestParam double tarifa,
            @RequestParam(required = false) Double reposicao,

            Model model
    ){

        LocalDate inicio = LocalDate.parse(dataInicio);
        LocalDate solicitacao = LocalDate.parse(dataSolicitacao);
        LocalDate cancelamento = dataCancelamento == null || dataCancelamento.isBlank()
                ? solicitacao
                : LocalDate.parse(dataCancelamento);

        if (cancelamento.isBefore(solicitacao)) {
            throw new IllegalArgumentException(
                    "A data de cancelamento nao pode ser anterior a data de solicitacao."
            );
        }

        BigDecimal multa = multaService.calcular(
                inicio,
                cancelamento,
                duracao,
                BigDecimal.valueOf(tarifa)
        ).valorMulta();

        if(reposicao != null){
            multa = multa.add(BigDecimal.valueOf(reposicao));
        }

        model.addAttribute("multa", multa.toPlainString());

        return "resultado";
    }
}
