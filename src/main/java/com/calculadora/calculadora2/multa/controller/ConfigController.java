package com.calculadora.calculadora2.multa.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConfigController {

    @Value("${spring.servlet.multipart.max-file-size}")
    private String maxFileSize;

    @Value("${spring.servlet.multipart.max-request-size}")
    private String maxRequestSize;

    @GetMapping("/config")
    public String config() {

        return "MaxFileSize = "
                + maxFileSize
                + " | MaxRequestSize = "
                + maxRequestSize;
    }
}