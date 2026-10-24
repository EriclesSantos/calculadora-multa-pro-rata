package com.calculadora.calculadora2.multa.model;

public class ErroProcessamento {

    private int linha;

    private String motivo;

    public ErroProcessamento(
            int linha,
            String motivo) {

        this.linha = linha;
        this.motivo = motivo;
    }

    public int getLinha() {
        return linha;
    }

    public String getMotivo() {
        return motivo;
    }
}