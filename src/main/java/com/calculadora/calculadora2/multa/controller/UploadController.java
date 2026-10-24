package com.calculadora.calculadora2.multa.controller;

import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.calculadora.calculadora2.multa.service.FileService;
import com.calculadora.calculadora2.multa.model.TipoCalculo;

@RestController
public class UploadController {

    private final FileService fileService;

    public UploadController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public ResponseEntity<InputStreamResource> upload(

            @RequestParam("file") MultipartFile file,

            @RequestParam(required = false)
            String nomeCliente,

            @RequestParam
            String dataSolicitacao,

            @RequestParam
            String dataCancelamento,

            @RequestParam
            int duracao,

            @RequestParam(defaultValue = "MULTA")
            String tipoCalculo,

            @RequestParam
            List<String> operadora,

            @RequestParam
            List<Double> tarifa,

            @RequestParam(required = false)
            List<Integer> diasProRata

    ) throws Exception {

        /* ==========================
           VALIDAÇÕES
           ========================== */

        if (nomeCliente == null || nomeCliente.isBlank()) {
            nomeCliente = "CLIENTE NÃO INFORMADO";
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Selecione um arquivo."
            );
        }

        if (dataSolicitacao == null ||
                dataSolicitacao.isBlank()) {

            throw new IllegalArgumentException(
                    "Informe a data de solicitacao."
            );
        }

        if (dataCancelamento == null ||
                dataCancelamento.isBlank()) {

            throw new IllegalArgumentException(
                    "Informe a data de cancelamento."
            );
        }

        try {
            LocalDate solicitacao = LocalDate.parse(dataSolicitacao);
            LocalDate cancelamento = LocalDate.parse(dataCancelamento);

            if (cancelamento.isBefore(solicitacao)) {
                throw new IllegalArgumentException(
                        "A data de cancelamento nao pode ser anterior a data de solicitacao."
                );
            }
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Informe datas validas.");
        }

        TipoCalculo tipo = TipoCalculo.from(tipoCalculo);

        if (duracao <= 0) {
            throw new IllegalArgumentException(
                    "Informe uma duração válida."
            );
        }

        if (operadora == null ||
                operadora.isEmpty()) {

            throw new IllegalArgumentException(
                    "Informe pelo menos uma operadora."
            );
        }

        if (tarifa == null ||
                tarifa.isEmpty()) {

            throw new IllegalArgumentException(
                    "Informe pelo menos uma tarifa."
            );
        }

        if (operadora.size() != tarifa.size()) {

            throw new IllegalArgumentException(
                    "Quantidade de operadoras diferente da quantidade de tarifas."
            );
        }

        File resultado = fileService.processarArquivo(
                file,
                nomeCliente,
                dataSolicitacao,
                dataCancelamento,
                duracao,
                tipo,
                operadora,
                tarifa,
                diasProRata
        );

        InputStreamResource resource =
                new InputStreamResource(
                        new java.io.FilterInputStream(new FileInputStream(resultado)) {
                            @Override
                            public void close() throws java.io.IOException {
                                try { super.close(); }
                                finally { java.nio.file.Files.deleteIfExists(resultado.toPath()); }
                            }
                        }
                );

        return ResponseEntity.ok()
                .header(
 HttpHeaders.CONTENT_DISPOSITION,
 "attachment; filename=resultado.xlsx"
)
                .contentType(
 MediaType.parseMediaType(
 "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
 )
)
                .contentLength(resultado.length())
                .body(resource);
    }
}
