package com.calculadora.calculadora2.multa.service;

import com.calculadora.calculadora2.multa.model.LinhaCalculada;
import com.calculadora.calculadora2.multa.model.ResumoOperadora;
import com.calculadora.calculadora2.multa.model.TipoCalculo;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

@Service
public class FileService {

    private final ValidacaoService validacaoService;
    private final ExcelService excelService;
    private final MultaService multaService;
    private final ProRataService proRataService;
    private final BaseContratoService baseContratoService;

    @Autowired
    public FileService(
            ValidacaoService validacaoService,
            ExcelService excelService,
            MultaService multaService,
            ProRataService proRataService,
            BaseContratoService baseContratoService
    ) {
        this.validacaoService = validacaoService;
        this.excelService = excelService;
        this.multaService = multaService;
        this.proRataService = proRataService;
        this.baseContratoService = baseContratoService;
    }

    public File processarArquivo(
            MultipartFile file,
            String nomeCliente,
            String dataSolicitacao,
            String dataCancelamento,
            int duracao,
            TipoCalculo tipoCalculo,
            List<String> operadoras,
            List<Double> tarifas,
            List<Integer> diasProRata
    ) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Nenhum arquivo enviado.");
        }

        if (duracao <= 0) {
            throw new IllegalArgumentException("Informe uma duracao valida.");
        }
        String nomeArquivo = Optional.ofNullable(file.getOriginalFilename())
                .orElse("")
                .toLowerCase();
        if (nomeArquivo.isBlank()) {
            throw new IllegalArgumentException("Nome do arquivo invalido.");
        }

        LocalDate solicitacao = LocalDate.parse(dataSolicitacao);
        LocalDate cancelamento = LocalDate.parse(dataCancelamento);
        if (cancelamento.isBefore(solicitacao)) {
            throw new IllegalArgumentException(
                    "A data de cancelamento nao pode ser anterior a data de solicitacao."
            );
        }

        Map<String, ConfiguracaoOperadora> configuracoes = montarConfiguracoes(
                operadoras,
                tarifas,
                diasProRata,
                tipoCalculo
        );
        Map<String, AcumuladorOperadora> acumuladores = new LinkedHashMap<>();
        configuracoes.forEach((nome, config) -> acumuladores.put(nome, new AcumuladorOperadora(config)));

        List<LinhaCalculada> linhasCalculadas = new ArrayList<>();
        List<String> linhasErro = new ArrayList<>();
        List<String> linhas = lerArquivo(file, nomeArquivo);
        OrdemData ordemData = detectarOrdemData(linhas, configuracoes.keySet());

        int numeroLinha = 0;
        for (String conteudoLinha : linhas) {
            numeroLinha++;
            Map<String, String> dados = LinhaParser.extrairDados(
                    conteudoLinha,
                    configuracoes.keySet()
            );

            // Cabecalhos, totais e numeros soltos nao viram registros.
            if (dados.size() < 2) {
                continue;
            }

            String iccid = dados.getOrDefault("iccid", "");
            String linhaChip = dados.getOrDefault("linha", "");
            String operadoraLinha = dados.getOrDefault("operadora", "");

            if (!validacaoService.validarIccid(iccid)) {
                linhasErro.add("Linha " + numeroLinha + " -> ICCID ausente ou invalido");
                continue;
            }
            if (!validacaoService.validarLinha(linhaChip)) {
                linhasErro.add("Linha " + numeroLinha + " -> Numero da linha ausente ou invalido");
                continue;
            }
            if (operadoraLinha.isEmpty()) {
                linhasErro.add("Linha " + numeroLinha + " -> Operadora nao encontrada");
                continue;
            }

            AcumuladorOperadora acumulador = acumuladores.get(operadoraLinha);
            if (acumulador == null) {
                linhasErro.add("Linha " + numeroLinha + " -> Operadora nao cadastrada: " + operadoraLinha);
                continue;
            }

            LocalDate dataInicio = null;
            LocalDate fimContrato = null;
            long diasRestantes = 0;
            BigDecimal valorMulta = BigDecimal.ZERO;

            if (tipoCalculo.incluiMulta()) {
                try {
                    dataInicio = resolverDataInicio(dados, ordemData, iccid);
                } catch (Exception e) {
                    linhasErro.add(
                            "Linha " + numeroLinha
                                    + " -> Data de inicio nao encontrada para o ICCID "
                                    + iccid
                    );
                }

                if (dataInicio != null) {
                    MultaService.ResultadoMulta calculo = multaService.calcular(
                            dataInicio,
                            cancelamento,
                            duracao,
                            acumulador.configuracao.tarifaMensal
                    );
                    fimContrato = calculo.fimContrato();
                    diasRestantes = calculo.diasRestantes();
                    valorMulta = calculo.valorMulta();

                    acumulador.totalMulta = acumulador.totalMulta.add(
                            calculo.valorMultaExato()
                    );
                    acumulador.diasMulta += diasRestantes;
                    if (diasRestantes > 0) {
                        acumulador.chipsComMulta++;
                    }
                }
            }

            acumulador.chipsACancelar++;
            linhasCalculadas.add(new LinhaCalculada(
                    iccid,
                    linhaChip,
                    operadoraLinha,
                    dataInicio,
                    fimContrato,
                    diasRestantes,
                    valorMulta
            ));
        }

        List<ResumoOperadora> resumos = new ArrayList<>();
        for (AcumuladorOperadora acumulador : acumuladores.values()) {
            BigDecimal valorDia = BigDecimal.ZERO;
            BigDecimal totalProRata = BigDecimal.ZERO;
            int diasPeriodoProRata = 0;

            if (tipoCalculo.incluiProRata()
                    && acumulador.chipsACancelar > 0) {
                ProRataService.ResultadoProRata proRata = proRataService.calcular(
                        acumulador.configuracao.nome,
                        acumulador.chipsACancelar,
                        acumulador.configuracao.diasProRata,
                        cancelamento,
                        acumulador.configuracao.tarifaMensal
                );
                valorDia = proRata.valorDia();
                totalProRata = proRata.valorTotalExato();
                diasPeriodoProRata = proRata.diasPeriodo();
            }

            resumos.add(new ResumoOperadora(
                    acumulador.configuracao.nome,
                    acumulador.chipsACancelar,
                    acumulador.chipsComMulta,
                    acumulador.diasMulta,
                    acumulador.configuracao.tarifaMensal,
                    acumulador.totalMulta,
                    BigDecimal.ZERO,
                    acumulador.configuracao.diasProRata,
                    diasPeriodoProRata,
                    valorDia,
                    totalProRata
            ));
        }

        return excelService.gerarExcel(
                nomeCliente,
                solicitacao,
                cancelamento,
                duracao,
                tipoCalculo,
                linhasCalculadas,
                resumos,
                linhasErro
        );
    }

    private List<String> lerArquivo(MultipartFile file, String nomeArquivo) throws Exception {
        if (nomeArquivo.endsWith(".txt") || nomeArquivo.endsWith(".csv")) {
            return lerTexto(file);
        }
        if (nomeArquivo.endsWith(".xls") || nomeArquivo.endsWith(".xlsx")) {
            return lerExcel(file);
        }
        throw new IllegalArgumentException("Formato nao suportado: " + nomeArquivo);
    }

    private List<String> lerTexto(MultipartFile file) throws Exception {
        List<String> linhas = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)
        )) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                linha = linha.replace("\uFEFF", "").trim();
                if (!linha.isEmpty()) {
                    linhas.add(linha);
                }
            }
        }
        return linhas;
    }

    private List<String> lerExcel(MultipartFile file) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            DataFormatter formatter = new DataFormatter();
            Sheet sheet = workbook.getSheetAt(0);

            ColunasPadronizadas colunas = localizarColunasPadronizadas(sheet, formatter);
            if (colunas != null) {
                return lerLinhasPadronizadas(sheet, formatter, colunas);
            }

            return lerLinhasLivres(sheet, formatter);
        }
    }

    private ColunasPadronizadas localizarColunasPadronizadas(
            Sheet sheet,
            DataFormatter formatter
    ) {
        int ultimaLinhaCabecalho = Math.min(sheet.getLastRowNum(), 19);
        for (int numeroLinha = 0; numeroLinha <= ultimaLinhaCabecalho; numeroLinha++) {
            Row row = sheet.getRow(numeroLinha);
            if (row == null) {
                continue;
            }

            int iccid = -1;
            int telefone = -1;
            int dataInicio = -1;
            int operadora = -1;

            for (int numeroColuna = 0; numeroColuna < row.getLastCellNum(); numeroColuna++) {
                String cabecalho = LinhaParser.normalizarOperadora(
                        formatarCelula(
                                row.getCell(
                                        numeroColuna,
                                        Row.MissingCellPolicy.RETURN_BLANK_AS_NULL
                                ),
                                formatter
                        )
                );

                if (cabecalho.equals("NUMERODESERIE")) {
                    iccid = numeroColuna;
                } else if (cabecalho.equals("TELEFONE")) {
                    telefone = numeroColuna;
                } else if (cabecalho.equals("DATAINICIO")
                        || cabecalho.equals("DATAINICIOCONTRATO")
                        || cabecalho.equals("INICIOCONTRATO")
                        || cabecalho.equals("DATACONTRATACAO")) {
                    dataInicio = numeroColuna;
                } else if (cabecalho.equals("OPERADORA")) {
                    operadora = numeroColuna;
                }
            }

            if (iccid >= 0 && telefone >= 0 && operadora >= 0) {
                return new ColunasPadronizadas(
                        numeroLinha,
                        iccid,
                        telefone,
                        dataInicio,
                        operadora
                );
            }
        }

        return null;
    }

    private List<String> lerLinhasPadronizadas(
            Sheet sheet,
            DataFormatter formatter,
            ColunasPadronizadas colunas
    ) {
        List<String> linhas = new ArrayList<>();
        for (int numeroLinha = colunas.linhaCabecalho + 1;
             numeroLinha <= sheet.getLastRowNum();
             numeroLinha++) {
            Row row = sheet.getRow(numeroLinha);
            if (row == null) {
                continue;
            }

            StringJoiner conteudoLinha = new StringJoiner(" ");
            adicionarSePreenchido(conteudoLinha, valorDaColuna(row, colunas.iccid, formatter));
            adicionarSePreenchido(conteudoLinha, valorDaColuna(row, colunas.telefone, formatter));
            adicionarSePreenchido(conteudoLinha, valorDaColuna(row, colunas.operadora, formatter));
            if (colunas.dataInicio >= 0) {
                adicionarSePreenchido(
                        conteudoLinha,
                        valorDaColuna(row, colunas.dataInicio, formatter)
                );
            }

            String conteudo = conteudoLinha.toString().trim();
            if (!conteudo.isEmpty()) {
                linhas.add(conteudo);
            }
        }
        return linhas;
    }

    private List<String> lerLinhasLivres(Sheet sheet, DataFormatter formatter) {
        List<String> linhas = new ArrayList<>();
        for (Row row : sheet) {
            StringJoiner conteudoLinha = new StringJoiner(" ");
            int ultimaColuna = row.getLastCellNum();
            for (int i = 0; i < ultimaColuna; i++) {
                adicionarSePreenchido(
                        conteudoLinha,
                        formatarCelula(
                                row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL),
                                formatter
                        )
                );
            }

            String conteudo = conteudoLinha.toString().trim();
            if (!conteudo.isEmpty()) {
                linhas.add(conteudo);
            }
        }
        return linhas;
    }

    private String valorDaColuna(Row row, int numeroColuna, DataFormatter formatter) {
        return formatarCelula(
                row.getCell(numeroColuna, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL),
                formatter
        );
    }

    private String formatarCelula(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }

        boolean celulaNumerica = cell.getCellType() == CellType.NUMERIC
                || (cell.getCellType() == CellType.FORMULA
                && cell.getCachedFormulaResultType() == CellType.NUMERIC);

        if (celulaNumerica && DateUtil.isCellDateFormatted(cell)) {
            // A hora e o formato visual (americano ou brasileiro) nao
            // interferem: uma data real do Excel vira sempre yyyy-MM-dd.
            return cell.getLocalDateTimeCellValue().toLocalDate().toString();
        }

        return formatter.formatCellValue(cell).trim();
    }

    private void adicionarSePreenchido(StringJoiner conteudoLinha, String valor) {
        if (!valor.isEmpty()) {
            conteudoLinha.add(valor);
        }
    }

    private Map<String, ConfiguracaoOperadora> montarConfiguracoes(
            List<String> operadoras,
            List<Double> tarifas,
            List<Integer> diasProRata,
            TipoCalculo tipoCalculo
    ) {
        if (operadoras == null || tarifas == null || operadoras.size() != tarifas.size()) {
            throw new IllegalArgumentException("Quantidade de operadoras diferente da quantidade de tarifas.");
        }
        Map<String, ConfiguracaoOperadora> tabela = new LinkedHashMap<>();
        for (int i = 0; i < operadoras.size(); i++) {
            String nome = LinhaParser.normalizarOperadora(operadoras.get(i));
            if (nome.isBlank()) {
                throw new IllegalArgumentException("Informe o nome de todas as operadoras.");
            }
            if (tabela.containsKey(nome)) {
                throw new IllegalArgumentException("Operadora cadastrada mais de uma vez: " + nome);
            }

            double tarifa = tarifas.get(i);
            if (!Double.isFinite(tarifa) || tarifa < 0) {
                throw new IllegalArgumentException("Tarifa invalida para a operadora " + nome + ".");
            }
            Integer diasInformados = tipoCalculo.incluiProRata()
                    && diasProRata != null
                    && i < diasProRata.size()
                    ? diasProRata.get(i)
                    : null;
            int dias = diasInformados == null ? 0 : diasInformados;
            if (dias < 0 || dias > 62) {
                throw new IllegalArgumentException("Dias de pro rata invalidos para a operadora " + nome + ".");
            }

            tabela.put(nome, new ConfiguracaoOperadora(
                    nome,
                    BigDecimal.valueOf(tarifa),
                    dias
            ));
        }
        return tabela;
    }

    private OrdemData detectarOrdemData(List<String> linhas, Set<String> operadoras) {
        int votosDiaMes = 0;
        int votosMesDia = 0;

        for (String linha : linhas) {
            String data = LinhaParser.extrairDados(linha, operadoras).get("dataInicio");
            if (data == null || data.matches("\\d{4}-.*")) {
                continue;
            }

            String[] partes = data.split("[/\\-]");
            if (partes.length != 3) {
                continue;
            }

            try {
                int primeiro = Integer.parseInt(partes[0]);
                int segundo = Integer.parseInt(partes[1]);
                if (primeiro > 12 && segundo <= 12) {
                    votosDiaMes++;
                } else if (segundo > 12 && primeiro <= 12) {
                    votosMesDia++;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return votosMesDia > votosDiaMes ? OrdemData.MES_DIA_ANO : OrdemData.DIA_MES_ANO;
    }

    private LocalDate parseData(String data, OrdemData ordemData) {
        String apenasData = data.trim().split("[ T]", 2)[0];
        List<DateTimeFormatter> formatos = ordemData == OrdemData.MES_DIA_ANO
                ? List.of(
                        DateTimeFormatter.ofPattern("M/d/uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT),
                        DateTimeFormatter.ofPattern("M-d-uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT),
                        DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT),
                        DateTimeFormatter.ofPattern("d-M-uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT),
                        DateTimeFormatter.ofPattern("uuuu-M-d").withResolverStyle(java.time.format.ResolverStyle.STRICT)
                )
                : List.of(
                        DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT),
                        DateTimeFormatter.ofPattern("d-M-uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT),
                        DateTimeFormatter.ofPattern("M/d/uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT),
                        DateTimeFormatter.ofPattern("M-d-uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT),
                        DateTimeFormatter.ofPattern("uuuu-M-d").withResolverStyle(java.time.format.ResolverStyle.STRICT)
                );
        for (DateTimeFormatter formato : formatos) {
            try {
                return LocalDate.parse(apenasData, formato);
            } catch (Exception ignored) {
            }
        }
        throw new IllegalArgumentException("Formato invalido: " + data);
    }

    private LocalDate resolverDataInicio(
            Map<String, String> dados,
            OrdemData ordemData,
            String iccid
    ) {
        String dataInformada = dados.getOrDefault("dataInicio", "").trim();
        if (!dataInformada.isEmpty()) {
            try {
                return parseData(dataInformada, ordemData);
            } catch (Exception ignored) {
                // Se a data do arquivo estiver invalida, tenta a base historica.
            }
        }

        return baseContratoService.buscarDataInicio(iccid)
                .orElseThrow(() -> new IllegalArgumentException("ICCID sem data na base historica."));
    }

    private record ConfiguracaoOperadora(
            String nome,
            BigDecimal tarifaMensal,
            int diasProRata
    ) {
    }

    private record ColunasPadronizadas(
            int linhaCabecalho,
            int iccid,
            int telefone,
            int dataInicio,
            int operadora
    ) {
    }

    private static class AcumuladorOperadora {
        private final ConfiguracaoOperadora configuracao;
        private int chipsACancelar;
        private int chipsComMulta;
        private long diasMulta;
        private BigDecimal totalMulta = BigDecimal.ZERO;

        private AcumuladorOperadora(ConfiguracaoOperadora configuracao) {
            this.configuracao = configuracao;
        }
    }

    private enum OrdemData {
        DIA_MES_ANO,
        MES_DIA_ANO
    }
}
