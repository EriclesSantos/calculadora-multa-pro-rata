package com.calculadora.calculadora2.multa.model;

public enum TipoCalculo {
    MULTA,
    PRO_RATA,
    MULTA_E_PRO_RATA;

    public static TipoCalculo from(String valor) {
        if (valor == null || valor.isBlank()) {
            return MULTA;
        }

        try {
            return valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Tipo de calculo invalido.");
        }
    }

    public boolean incluiMulta() {
        return this == MULTA || this == MULTA_E_PRO_RATA;
    }

    public boolean incluiProRata() {
        return this == PRO_RATA || this == MULTA_E_PRO_RATA;
    }
}
