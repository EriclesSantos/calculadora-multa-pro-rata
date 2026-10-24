package com.calculadora.calculadora2.multa.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import java.time.format.DateTimeParseException;
import java.util.Map;

@RestControllerAdvice
public class ErroController {
    @ExceptionHandler({IllegalArgumentException.class, DateTimeParseException.class})
    public ResponseEntity<Map<String, String>> entradaInvalida(RuntimeException erro) {
        String mensagem = erro instanceof DateTimeParseException
                ? "Informe datas válidas no formato solicitado." : erro.getMessage();
        return ResponseEntity.badRequest().body(Map.of("erro", mensagem));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> arquivoGrande() {
        return ResponseEntity.status(413).body(Map.of("erro", "O arquivo excede o limite de 200 MB."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> falhaDeProcessamento(Exception erro) {
        // Do not expose a filename, record, local path or internal exception in the response.
        return ResponseEntity.internalServerError().body(Map.of("erro",
                "Não foi possível processar o arquivo. Confira o formato e os dados enviados."));
    }
}
