package com.calculadora.calculadora2.multa.service;

import com.calculadora.calculadora2.multa.model.LinhaCalculada;
import com.calculadora.calculadora2.multa.model.ResumoOperadora;
import com.calculadora.calculadora2.multa.model.TipoCalculo;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Service
public class ExcelService {

    private static final String FORMATO_DINHEIRO =
            "\"R$\" #,##0.00;[Red]-\"R$\" #,##0.00;\"R$\" -";

    public File gerarExcel(
            String nomeCliente,
            LocalDate dataSolicitacao,
            LocalDate dataCancelamento,
            int duracao,
            TipoCalculo tipoCalculo,
            List<LinhaCalculada> dados,
            List<ResumoOperadora> resumos,
            List<String> linhasErro
    ) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Estilos estilos = criarEstilos(workbook);
            Sheet resumo = workbook.createSheet("Resumo");
            configurarResumo(resumo);

            preencherInformacoes(
                    resumo,
                    nomeCliente,
                    dataSolicitacao,
                    dataCancelamento,
                    duracao,
                    estilos
            );

            Row aviso = resumo.createRow(6);
            aviso.createCell(0).setCellValue("DEMONSTRAÇÃO DIDÁTICA — usar somente dados fictícios. Não utilizar para cobrança.");
            resumo.addMergedRegion(new CellRangeAddress(6, 6, 0, 7));
            Row pendencias = resumo.createRow(7);
            pendencias.createCell(0).setCellValue("Inconsistências: " + linhasErro.size()
                    + (linhasErro.isEmpty() ? " — processamento concluído." : " — resultado parcial; confira a aba Erros."));
            resumo.addMergedRegion(new CellRangeAddress(7, 7, 0, 7));
            int proximaLinha = 9;
            String subtotalMulta = null;
            String subtotalProRata = null;

            if (tipoCalculo.incluiMulta()) {
                ResultadoSecao secao = criarSecaoMulta(
                        resumo,
                        proximaLinha,
                        resumos,
                        estilos,
                        1
                );
                proximaLinha = secao.proximaLinha;
                subtotalMulta = secao.referenciaSubtotal;
            }

            if (tipoCalculo.incluiProRata()) {
                int numero = tipoCalculo.incluiMulta() ? 2 : 1;
                ResultadoSecao secao = criarSecaoProRata(
                        resumo,
                        proximaLinha + (tipoCalculo.incluiMulta() ? 1 : 0),
                        resumos,
                        estilos,
                        numero
                );
                proximaLinha = secao.proximaLinha;
                subtotalProRata = secao.referenciaSubtotal;
            }

            criarTotalAPagar(
                    resumo,
                    proximaLinha + 1,
                    subtotalMulta,
                    subtotalProRata,
                    estilos,
                    tipoCalculo
            );
            criarDetalhes(workbook, dados, tipoCalculo, estilos);
            criarErros(workbook, linhasErro, estilos);

            workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();

            File arquivo = Files.createTempFile("calculadora2-resultado-", ".xlsx").toFile();
            arquivo.deleteOnExit();
            try (FileOutputStream fos = new FileOutputStream(arquivo)) {
                workbook.write(fos);
            }
            return arquivo;
        }
    }

    private void preencherInformacoes(
            Sheet sheet,
            String nomeCliente,
            LocalDate solicitacao,
            LocalDate cancelamento,
            int duracao,
            Estilos estilos
    ) {
        criarRotuloValor(sheet, 0, "Cliente :", nomeCliente, estilos.infoLabel, estilos.infoValue);

        Row rowSolicitacao = sheet.createRow(1);
        Cell rotuloSolicitacao = rowSolicitacao.createCell(0);
        rotuloSolicitacao.setCellValue("Data de solicitação do cliente :");
        rotuloSolicitacao.setCellStyle(estilos.infoLabel);
        Cell dataSolicitacao = rowSolicitacao.createCell(1);
        dataSolicitacao.setCellValue(Date.valueOf(solicitacao));
        dataSolicitacao.setCellStyle(estilos.dataInfo);

        Row rowCancelamento = sheet.createRow(3);
        Cell rotuloCancelamento = rowCancelamento.createCell(0);
        rotuloCancelamento.setCellValue("Data fim de Faturamento");
        rotuloCancelamento.setCellStyle(estilos.infoLabel);
        Cell dataCancelamento = rowCancelamento.createCell(1);
        dataCancelamento.setCellValue(Date.valueOf(cancelamento));
        dataCancelamento.setCellStyle(estilos.dataInfo);

        criarRotuloValor(
                sheet,
                4,
                "Duração do contrato :",
                duracao + " meses",
                estilos.infoLabel,
                estilos.infoValue
        );
    }

    private void criarRotuloValor(
            Sheet sheet,
            int linha,
            String rotulo,
            String valor,
            CellStyle estiloRotulo,
            CellStyle estiloValor
    ) {
        Row row = sheet.createRow(linha);
        Cell label = row.createCell(0);
        label.setCellValue(rotulo);
        label.setCellStyle(estiloRotulo);
        Cell value = row.createCell(1);
        value.setCellValue(valor);
        value.setCellStyle(estiloValor);
    }

    private ResultadoSecao criarSecaoMulta(
            Sheet sheet,
            int inicio,
            List<ResumoOperadora> resumos,
            Estilos estilos,
            int numero
    ) {
        Row titulo = sheet.createRow(inicio);
        titulo.createCell(0).setCellValue(numero + ". Multa de Cancelamento");
        titulo.getCell(0).setCellStyle(estilos.tituloSecao);

        int cabecalhoLinha = inicio + 1;
        String[] colunas = {
                "Operadora",
                "Chips a Cancelar",
                "Chips com Multa",
                "Dias de multa",
                "Tarifa Mensal (R$)",
                "Multa Cancelamento",
                "Reposição",
                "Valor Total (R$)"
        };
        criarCabecalho(sheet, cabecalhoLinha, colunas, estilos.cabecalho);

        int linha = cabecalhoLinha + 1;
        int primeiraLinhaDados = linha;
        for (ResumoOperadora resumo : resumos) {
            Row row = sheet.createRow(linha);
            criarCelula(row, 0, resumo.operadora(), estilos.textoTabela);
            criarCelula(row, 1, resumo.chipsACancelar(), estilos.numeroTabela);
            criarCelula(row, 2, resumo.chipsComMulta(), estilos.numeroTabela);
            criarCelula(row, 3, resumo.diasMulta(), estilos.numeroTabela);
            criarCelula(row, 4, resumo.tarifaMensal(), estilos.dinheiroTabela);
            criarCelula(row, 5, resumo.multaCancelamento(), estilos.dinheiroTabela);
            criarCelula(row, 6, resumo.reposicao(), estilos.dinheiroTabela);
            Cell total = row.createCell(7);
            total.setCellFormula("F" + (linha + 1) + "+G" + (linha + 1));
            total.setCellStyle(estilos.dinheiroTabela);
            linha++;
        }

        Row subtotal = sheet.createRow(linha);
        for (int coluna = 0; coluna < 8; coluna++) {
            subtotal.createCell(coluna).setCellStyle(estilos.totalTabela);
        }
        subtotal.getCell(1).setCellFormula(
                "SUM(B" + (primeiraLinhaDados + 1) + ":B" + linha + ")"
        );
        subtotal.getCell(2).setCellFormula(
                "SUM(C" + (primeiraLinhaDados + 1) + ":C" + linha + ")"
        );
        subtotal.getCell(6).setCellValue("Sub total");
        subtotal.getCell(7).setCellFormula(
                "SUM(H" + (primeiraLinhaDados + 1) + ":H" + linha + ")"
        );
        subtotal.getCell(7).setCellStyle(estilos.totalDinheiro);

        return new ResultadoSecao(
                linha + 1,
                new CellReference(linha, 7).formatAsString()
        );
    }

    private ResultadoSecao criarSecaoProRata(
            Sheet sheet,
            int inicio,
            List<ResumoOperadora> resumos,
            Estilos estilos,
            int numero
    ) {
        Row titulo = sheet.createRow(inicio);
        titulo.createCell(0).setCellValue(numero + ". Pro Rata");
        titulo.getCell(0).setCellStyle(estilos.tituloSecao);

        int cabecalhoLinha = inicio + 1;
        String[] colunas = {
                "Operadora",
                "Chips a Cancelar",
                "Tarifa Mensal (R$)",
                "Último mês a faturar",
                "Dias a Faturar Por chip",
                "Valor Dia (R$)",
                "Valor Total (R$)"
        };
        criarCabecalho(sheet, cabecalhoLinha, colunas, estilos.cabecalho);

        int linha = cabecalhoLinha + 1;
        int primeiraLinhaDados = linha;
        for (ResumoOperadora resumo : resumos) {
            Row row = sheet.createRow(linha);
            criarCelula(row, 0, resumo.operadora(), estilos.textoTabela);
            criarCelula(row, 1, resumo.chipsACancelar(), estilos.numeroTabela);
            criarCelula(row, 2, resumo.tarifaMensal(), estilos.dinheiroTabela);

            Cell mes = row.createCell(3);
            Cell dias = row.createCell(4);
            Cell valorDia = row.createCell(5);
            Cell total = row.createCell(6);
            mes.setCellStyle(estilos.mesTabela);
            dias.setCellStyle(estilos.numeroTabela);
            valorDia.setCellStyle(estilos.dinheiroTabela);
            total.setCellStyle(estilos.dinheiroTabela);

            if (resumo.chipsACancelar() > 0
                    && resumo.diasPeriodoProRata() > 0) {
                mes.setCellFormula(
                        "CHOOSE(MONTH($B$4),\"janeiro\",\"fevereiro\",\"março\","
                                + "\"abril\",\"maio\",\"junho\",\"julho\",\"agosto\","
                                + "\"setembro\",\"outubro\",\"novembro\",\"dezembro\")"
                                + "&\"-\"&TEXT($B$4,\"yy\")"
                );
                dias.setCellValue(resumo.diasProRata());
                int excelRow = linha + 1;
                valorDia.setCellFormula(
                        "C" + excelRow
                                + "/" + resumo.diasPeriodoProRata()
                );
                total.setCellFormula(
                        "B" + excelRow + "*E" + excelRow + "*F" + excelRow
                );
            } else {
                valorDia.setCellValue(0);
                total.setCellValue(0);
            }
            linha++;
        }

        Row subtotal = sheet.createRow(linha);
        for (int coluna = 0; coluna < 7; coluna++) {
            subtotal.createCell(coluna).setCellStyle(estilos.totalTabela);
        }
        subtotal.getCell(1).setCellFormula(
                "SUM(B" + (primeiraLinhaDados + 1) + ":B" + linha + ")"
        );
        subtotal.getCell(5).setCellValue("Sub total");
        subtotal.getCell(6).setCellFormula(
                "SUM(G" + (primeiraLinhaDados + 1) + ":G" + linha + ")"
        );
        subtotal.getCell(6).setCellStyle(estilos.totalDinheiro);

        return new ResultadoSecao(
                linha + 1,
                new CellReference(linha, 6).formatAsString()
        );
    }

    private void criarTotalAPagar(
            Sheet sheet,
            int linha,
            String subtotalMulta,
            String subtotalProRata,
            Estilos estilos,
            TipoCalculo tipoCalculo
    ) {
        int colunaRotulo = tipoCalculo == TipoCalculo.MULTA ? 6 : 5;
        int colunaValor = colunaRotulo + 1;
        Row row = sheet.createRow(linha);
        Cell label = row.createCell(colunaRotulo);
        label.setCellValue("Total simulado");
        label.setCellStyle(estilos.totalTabela);
        Cell valor = row.createCell(colunaValor);

        if (subtotalMulta != null && subtotalProRata != null) {
            valor.setCellFormula(subtotalMulta + "+" + subtotalProRata);
        } else if (subtotalMulta != null) {
            valor.setCellFormula(subtotalMulta);
        } else {
            valor.setCellFormula(subtotalProRata);
        }
        valor.setCellStyle(estilos.totalDinheiro);
    }

    private void criarDetalhes(
            Workbook workbook,
            List<LinhaCalculada> dados,
            TipoCalculo tipoCalculo,
            Estilos estilos
    ) {
        Sheet sheet = workbook.createSheet("Resultado");
        sheet.getHeader().setCenter("DEMONSTRAÇÃO — usar somente dados fictícios");
        String[] colunas = tipoCalculo.incluiMulta()
                ? new String[]{"ICCID", "LINHA", "OPERADORA", "INÍCIO", "FIM", "DIAS RESTANTES", "MULTA", "SITUAÇÃO"}
                : new String[]{"ICCID", "LINHA", "OPERADORA"};
        criarCabecalho(sheet, 0, colunas, estilos.cabecalho);

        int linha = 1;
        for (LinhaCalculada item : dados) {
            Row row = sheet.createRow(linha++);
            criarCelula(row, 0, item.iccid(), estilos.identificador);
            criarCelula(row, 1, item.linha(), estilos.identificador);
            criarCelula(row, 2, item.operadora(), estilos.textoTabela);
            if (tipoCalculo.incluiMulta()) {
                criarData(row, 3, item.inicioContrato(), estilos.dataTabela);
                criarData(row, 4, item.fimContrato(), estilos.dataTabela);
                criarCelula(row, 5, item.diasRestantes(), estilos.numeroTabela);
                criarCelula(row, 6, item.multa(), estilos.dinheiroTabela);
                criarCelula(row, 7, item.inicioContrato() == null ? "PENDENTE — consultar Erros" : "CALCULADO", estilos.textoTabela);
            }
        }

        if (linha > 1) {
            sheet.setAutoFilter(new CellRangeAddress(0, linha - 1, 0, colunas.length - 1));
        }
        sheet.createFreezePane(0, 1);
        sheet.setColumnWidth(0, 24 * 256);
        sheet.setColumnWidth(1, 18 * 256);
        sheet.setColumnWidth(2, 16 * 256);
        for (int i = 3; i < colunas.length; i++) {
            sheet.setColumnWidth(i, 16 * 256);
        }
    }

    private void criarErros(Workbook workbook, List<String> linhasErro, Estilos estilos) {
        Sheet erros = workbook.createSheet("Erros");
        criarCabecalho(erros, 0, new String[]{"Descrição"}, estilos.cabecalho);
        int linha = 1;
        for (String erro : linhasErro) {
            Row row = erros.createRow(linha++);
            criarCelula(row, 0, erro, estilos.textoTabela);
        }
        erros.setColumnWidth(0, 70 * 256);
        erros.createFreezePane(0, 1);
    }

    private void configurarResumo(Sheet sheet) {
        sheet.setDisplayGridlines(false);
        sheet.setColumnWidth(0, 26 * 256);
        sheet.setColumnWidth(1, 18 * 256);
        sheet.setColumnWidth(2, 19 * 256);
        sheet.setColumnWidth(3, 18 * 256);
        sheet.setColumnWidth(4, 22 * 256);
        sheet.setColumnWidth(5, 22 * 256);
        sheet.setColumnWidth(6, 20 * 256);
        sheet.setColumnWidth(7, 18 * 256);
        sheet.getPrintSetup().setLandscape(true);
        sheet.setFitToPage(true);
        sheet.getPrintSetup().setFitWidth((short) 1);
        sheet.getPrintSetup().setFitHeight((short) 0);
    }

    private void criarCabecalho(
            Sheet sheet,
            int linha,
            String[] colunas,
            CellStyle estilo
    ) {
        Row row = sheet.createRow(linha);
        row.setHeightInPoints(28);
        for (int i = 0; i < colunas.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(colunas[i]);
            cell.setCellStyle(estilo);
        }
    }

    private void criarCelula(Row row, int coluna, String valor, CellStyle estilo) {
        Cell cell = row.createCell(coluna);
        cell.setCellValue(valor == null ? "" : valor);
        cell.setCellStyle(estilo);
    }

    private void criarCelula(Row row, int coluna, long valor, CellStyle estilo) {
        Cell cell = row.createCell(coluna);
        cell.setCellValue(valor);
        cell.setCellStyle(estilo);
    }

    private void criarCelula(Row row, int coluna, BigDecimal valor, CellStyle estilo) {
        Cell cell = row.createCell(coluna);
        cell.setCellValue(valor.doubleValue());
        cell.setCellStyle(estilo);
    }

    private void criarData(Row row, int coluna, LocalDate valor, CellStyle estilo) {
        Cell cell = row.createCell(coluna);
        if (valor != null) {
            cell.setCellValue(Date.valueOf(valor));
        }
        cell.setCellStyle(estilo);
    }

    private Estilos criarEstilos(Workbook workbook) {
        DataFormat format = workbook.createDataFormat();

        Font fonteNormal = workbook.createFont();
        fonteNormal.setFontName("Arial");
        fonteNormal.setFontHeightInPoints((short) 10);

        Font fonteNegrito = workbook.createFont();
        fonteNegrito.setFontName("Arial");
        fonteNegrito.setFontHeightInPoints((short) 10);
        fonteNegrito.setBold(true);

        CellStyle infoLabel = workbook.createCellStyle();
        infoLabel.setFont(fonteNegrito);

        CellStyle infoValue = workbook.createCellStyle();
        infoValue.setFont(fonteNegrito);

        CellStyle dataInfo = workbook.createCellStyle();
        dataInfo.setFont(fonteNegrito);
        dataInfo.setDataFormat(format.getFormat("dd/mm/yyyy"));

        CellStyle tituloSecao = workbook.createCellStyle();
        tituloSecao.setFont(fonteNegrito);

        CellStyle cabecalho = estiloComBorda(workbook, fonteNegrito);
        cabecalho.setAlignment(HorizontalAlignment.CENTER);
        cabecalho.setVerticalAlignment(VerticalAlignment.CENTER);
        cabecalho.setWrapText(true);

        CellStyle textoTabela = estiloComBorda(workbook, fonteNormal);
        textoTabela.setAlignment(HorizontalAlignment.CENTER);

        CellStyle numeroTabela = estiloComBorda(workbook, fonteNormal);
        numeroTabela.setAlignment(HorizontalAlignment.CENTER);
        numeroTabela.setDataFormat(format.getFormat("#,##0"));

        CellStyle dinheiroTabela = estiloComBorda(workbook, fonteNormal);
        dinheiroTabela.setAlignment(HorizontalAlignment.RIGHT);
        dinheiroTabela.setDataFormat(format.getFormat(FORMATO_DINHEIRO));

        CellStyle mesTabela = estiloComBorda(workbook, fonteNormal);
        mesTabela.setAlignment(HorizontalAlignment.CENTER);
        mesTabela.setDataFormat(format.getFormat("[$-0416]mmmm-yy"));

        CellStyle dataTabela = estiloComBorda(workbook, fonteNormal);
        dataTabela.setAlignment(HorizontalAlignment.CENTER);
        dataTabela.setDataFormat(format.getFormat("dd/mm/yyyy"));

        CellStyle identificador = estiloComBorda(workbook, fonteNormal);
        identificador.setDataFormat(format.getFormat("@"));

        CellStyle totalTabela = estiloComBorda(workbook, fonteNegrito);
        totalTabela.setAlignment(HorizontalAlignment.CENTER);

        CellStyle totalDinheiro = estiloComBorda(workbook, fonteNegrito);
        totalDinheiro.setAlignment(HorizontalAlignment.RIGHT);
        totalDinheiro.setDataFormat(format.getFormat(FORMATO_DINHEIRO));

        return new Estilos(
                infoLabel,
                infoValue,
                dataInfo,
                tituloSecao,
                cabecalho,
                textoTabela,
                numeroTabela,
                dinheiroTabela,
                mesTabela,
                dataTabela,
                identificador,
                totalTabela,
                totalDinheiro
        );
    }

    private CellStyle estiloComBorda(Workbook workbook, Font fonte) {
        CellStyle estilo = workbook.createCellStyle();
        estilo.setFont(fonte);
        estilo.setBorderTop(BorderStyle.THIN);
        estilo.setBorderBottom(BorderStyle.THIN);
        estilo.setBorderLeft(BorderStyle.THIN);
        estilo.setBorderRight(BorderStyle.THIN);
        estilo.setVerticalAlignment(VerticalAlignment.CENTER);
        return estilo;
    }

    private record ResultadoSecao(int proximaLinha, String referenciaSubtotal) {
    }

    private record Estilos(
            CellStyle infoLabel,
            CellStyle infoValue,
            CellStyle dataInfo,
            CellStyle tituloSecao,
            CellStyle cabecalho,
            CellStyle textoTabela,
            CellStyle numeroTabela,
            CellStyle dinheiroTabela,
            CellStyle mesTabela,
            CellStyle dataTabela,
            CellStyle identificador,
            CellStyle totalTabela,
            CellStyle totalDinheiro
    ) {
    }
}
