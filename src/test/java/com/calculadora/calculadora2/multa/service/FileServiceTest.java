package com.calculadora.calculadora2.multa.service;
import com.calculadora.calculadora2.multa.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class FileServiceTest {
    static class Captura extends ExcelService {
        List<LinhaCalculada> dados; List<ResumoOperadora> resumos; List<String> erros;
        @Override public File gerarExcel(String nome, LocalDate solicitacao,LocalDate cancelamento,int duracao,TipoCalculo tipo,List<LinhaCalculada> dados,List<ResumoOperadora> resumos,List<String> erros) {
            this.dados=dados;this.resumos=resumos;this.erros=erros;return new File("nao-gravado.xlsx");
        }
    }
    private FileService service(ExcelService excel) { return new FileService(new ValidacaoService(),excel,new MultaService(),new ProRataService(),new BaseContratoService()); }
    private MockMultipartFile txt(String value) { return new MockMultipartFile("file","demo.txt","text/plain",value.getBytes(StandardCharsets.UTF_8)); }
    private void processar(ExcelService excel, MockMultipartFile file, TipoCalculo tipo) throws Exception {
        service(excel).processarArquivo(file,"CLIENTE DEMONSTRACAO","2026-10-01","2026-10-15",12,tipo,List.of("ALFA","BETA"),List.of(30.0,45.0),List.of(15,15));
    }
    @Test void calculaTxtComBaseSinteticaESeparaPendencias() throws Exception {
        var c=new Captura();
        processar(c,txt("00000000000000000001 000000000001 ALFA 01/01/2026\n00000000000000000002 000000000002 BETA 01/04/2026\n00000000000000000099 000000000099 ALFA 31/02/2026"),TipoCalculo.MULTA_E_PRO_RATA);
        assertEquals(3,c.dados.size()); assertEquals(1,c.erros.size());
        assertEquals(new BigDecimal("15.60"),c.dados.get(0).multa());
        assertNull(c.dados.get(2).inicioContrato());
        assertEquals(2,c.resumos.get(0).chipsACancelar());
        assertEquals(1,c.resumos.get(0).chipsComMulta());
    }
    @Test void consultaSomenteBaseFicticiaQuandoFaltaData() throws Exception {
        var c=new Captura(); processar(c,txt("00000000000000000001 000000000001 ALFA"),TipoCalculo.MULTA);
        assertEquals(LocalDate.of(2026,1,1),c.dados.get(0).inicioContrato()); assertTrue(c.erros.isEmpty());
    }
    @Test void rejeitaCancelamentoAnteriorASolicitacaoEDuplicidadeDeOperadoras() {
        var s=service(new Captura());
        assertThrows(IllegalArgumentException.class,()->s.processarArquivo(txt("demo"),"DEMO","2026-10-16","2026-10-15",12,TipoCalculo.MULTA,List.of("ALFA"),List.of(30.0),null));
        assertThrows(IllegalArgumentException.class,()->s.processarArquivo(txt("demo"),"DEMO","2026-10-01","2026-10-15",12,TipoCalculo.MULTA,List.of("ALFA","alfa"),List.of(30.0,30.0),null));
    }
    @Test void leXlsxPreservandoZerosEDataRealDoExcel() throws Exception {
        byte[] bytes;
        try(var w=new XSSFWorkbook();var out=new ByteArrayOutputStream()) {
            var sh=w.createSheet("Sinteticos");var h=sh.createRow(0);
            h.createCell(0).setCellValue("Número de série");h.createCell(1).setCellValue("Telefone");h.createCell(2).setCellValue("Operadora");h.createCell(3).setCellValue("Data início");
            var r=sh.createRow(1);r.createCell(0).setCellValue("00000000000000000001");r.createCell(1).setCellValue("000000000001");r.createCell(2).setCellValue("ALFA");r.createCell(3).setCellValue(LocalDate.of(2026,1,1));
            var style=w.createCellStyle();style.setDataFormat(w.createDataFormat().getFormat("mm/dd/yyyy"));r.getCell(3).setCellStyle(style);w.write(out);bytes=out.toByteArray();
        }
        var c=new Captura();processar(c,new MockMultipartFile("file","demo.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",bytes),TipoCalculo.MULTA);
        assertEquals("00000000000000000001",c.dados.get(0).iccid());assertEquals(LocalDate.of(2026,1,1),c.dados.get(0).inicioContrato());
        assertEquals(new BigDecimal("15.60"),c.dados.get(0).multa());
    }
}
