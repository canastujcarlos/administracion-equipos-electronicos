package main.java.com.aliprinsa.administracion.gusto.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import main.java.com.aliprinsa.administracion.gusto.model.Equipo;
import main.java.com.aliprinsa.administracion.gusto.util.EstadosFuncionales;

public class ExcelExportService {

    private static final String[] COLUMNAS = { 
        "Código", "Tipo", "Marca", "Modelo", "Serie", "Ubicación", "Estado", "Responsable", "Garantía hasta" 
    };

    public void exportarEquipos(List<Equipo> equipos, File destino) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet hoja = wb.createSheet("Inventario");

            // ---------- Estilos ----------
            XSSFCellStyle estTitulo = estilo(wb, null, "1E3A5F", true, 16);
            XSSFCellStyle estSub = estilo(wb, null, "5B7A9D", false, 11);
            XSSFCellStyle estHeader = estilo(wb, "F97316", "FFFFFF", true, 11);
            estHeader.setAlignment(HorizontalAlignment.LEFT);

            XSSFCellStyle estPar = estilo(wb, "F2F7FD", "1E3A5F", false, 11);
            XSSFCellStyle estImpar = estilo(wb, "E3EEFA", "1E3A5F", false, 11);
            
            XSSFCellStyle estFecha = estilo(wb, "F2F7FD", "1E3A5F", false, 11);
            XSSFCellStyle estFechaI = estilo(wb, "E3EEFA", "1E3A5F", false, 11);
            short fmt = wb.createDataFormat().getFormat("yyyy-mm-dd");
            estFecha.setDataFormat(fmt);
            estFechaI.setDataFormat(fmt);
            estFecha.setAlignment(HorizontalAlignment.LEFT);
            estFechaI.setAlignment(HorizontalAlignment.LEFT);

            XSSFCellStyle estOk = estilo(wb, "C7F0D8", "14532D", true, 11);     // verde
            XSSFCellStyle estRep = estilo(wb, "FDE9A8", "854D0E", true, 11);    // ámbar
            XSSFCellStyle estBaja = estilo(wb, "FBCFD8", "9F1239", true, 11);   // rosa
            XSSFCellStyle estRev = estilo(wb, "CFE3FA", "1E3A5F", true, 11);    // azul pastel

            int ultimaCol = COLUMNAS.length - 1;

            // ---------- Título (combinado para no ensanchar la columna A) ----------
            Row t = hoja.createRow(0);
            t.setHeightInPoints(26);
            Cell ct = t.createCell(0);
            ct.setCellValue("Inventario de equipos electrónicos");
            ct.setCellStyle(estTitulo);
            hoja.addMergedRegion(new CellRangeAddress(0, 0, 0, ultimaCol));

            Row s = hoja.createRow(1);
            Cell cs = s.createCell(0);
            cs.setCellValue("Generado el " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    + " · " + equipos.size() + " equipo(s)");
            cs.setCellStyle(estSub);
            hoja.addMergedRegion(new CellRangeAddress(1, 1, 0, ultimaCol));

            // ---------- Encabezado ----------
            int filaHeader = 3;
            Row h = hoja.createRow(filaHeader);
            h.setHeightInPoints(24);
            for (int i = 0; i < COLUMNAS.length; i++) {
                Cell c = h.createCell(i);
                c.setCellValue(COLUMNAS[i]);
                c.setCellStyle(estHeader);
            }

            // ---------- Datos ----------
            int fila = filaHeader + 1;
            for (int n = 0; n < equipos.size(); n++) {
                Equipo e = equipos.get(n);
                boolean impar = n % 2 == 1;
                CellStyle base = impar ? estImpar : estPar;

                Row r = hoja.createRow(fila++);
                r.setHeightInPoints(20);

                texto(r, 0, e.codigoInventario(), base);
                texto(r, 1, e.tipo(), base);
                texto(r, 2, e.marca(), base);
                texto(r, 3, e.modelo(), base);
                texto(r, 4, e.numeroSerie(), base);
                texto(r, 5, e.ubicacion() == null ? "—" : e.ubicacion(), base);

                // Switch corregido para que sea compatible con EstadosFuncionales.Categoria
                String estado = e.estadoFuncional();
                CellStyle estEstado = switch (EstadosFuncionales.categoria(estado)) {
                    case OK -> estOk;
                    case FALLA -> estRep;
                    case BAJA -> estBaja;
                    case REVISION -> estRev;
                    default -> base;
                };
                texto(r, 6, estado, estEstado);

                texto(r, 7, e.responsable() == null ? "Sin responsable" : e.responsable(), base);

                Cell cg = r.createCell(8);
                LocalDate g = e.garantiaHasta();
                if (g == null) {
                    cg.setCellValue("—");
                    cg.setCellStyle(base);
                } else {
                    cg.setCellValue(g);
                    cg.setCellStyle(impar ? estFechaI : estFecha);
                }
            }

            // ---------- Detalles finales ----------
            hoja.createFreezePane(0, filaHeader + 1);
            if (!equipos.isEmpty()) {
                hoja.setAutoFilter(new CellRangeAddress(filaHeader, fila - 1, 0, ultimaCol));
            }

            for (int i = 0; i < COLUMNAS.length; i++) {
                hoja.autoSizeColumn(i); // ignora las celdas combinadas del título
                hoja.setColumnWidth(i, Math.min(hoja.getColumnWidth(i) + 1200, 255 * 256));
            }
            hoja.setDisplayGridlines(false);

            try (FileOutputStream out = new FileOutputStream(destino)) {
                wb.write(out);
            }
        }
    }

    // ---------- Auxiliares ----------
    private void texto(Row r, int col, String valor, CellStyle estilo) {
        Cell c = r.createCell(col);
        c.setCellValue(valor == null ? "" : valor);
        c.setCellStyle(estilo);
    }

    /** fondo = hex sin # (o null para sin fondo); texto = hex sin #. */
    private XSSFCellStyle estilo(XSSFWorkbook wb, String fondo, String texto, boolean negrita, int tam) {
        XSSFCellStyle st = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setFontName("Segoe UI");
        f.setFontHeightInPoints((short) tam);
        f.setBold(negrita);
        f.setColor(color(texto));
        st.setFont(f);
        st.setVerticalAlignment(VerticalAlignment.CENTER);

        if (fondo != null) {
            st.setFillForegroundColor(color(fondo));
            st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        return st;
    }

    private XSSFColor color(String hex) {
        int rgb = Integer.parseInt(hex, 16);
        byte[] bytes = { (byte) ((rgb >> 16) & 0xFF), (byte) ((rgb >> 8) & 0xFF), (byte) (rgb & 0xFF) };
        return new XSSFColor(bytes, new DefaultIndexedColorMap());
    }
}
