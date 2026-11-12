package com.universidad.refactoringu11.reportes;

import com.universidad.refactoringu11.inventario.InventarioService;
import com.universidad.refactoringu11.inventario.Producto;
import com.universidad.refactoringu11.inventario.TipoProducto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReporteBajoStockServiceTest {

    private static final String SALTO = System.lineSeparator();

    private InventarioService inventario;
    private ReporteBajoStockService reporte;

    @BeforeEach
    void setUp() {
        inventario = new InventarioService();
        inventario.agregarProducto(new Producto(1L, "Clean Code", TipoProducto.LIBRO, 45.0, 20, 5));
        inventario.agregarProducto(new Producto(2L, "Cafe", TipoProducto.ALIMENTO, 15.0, 2, 4));
        reporte = new ReporteBajoStockService(inventario);
    }

    @Test
    void generarReporte_conProductosBajoElMinimo_listaSoloEsos() {
        assertEquals("Productos bajo stock minimo:" + SALTO
                + "- Cafe: stock 2 de minimo 4", reporte.generarReporte());
    }

    @Test
    void generarReporte_sinProductosBajoElMinimo_loIndica() {
        ReporteBajoStockService vacio = new ReporteBajoStockService(new InventarioService());
        assertEquals("Sin productos bajo stock minimo", vacio.generarReporte());
    }

    @Test
    void generarReporte_despuesDeUnaSalida_reflejaElStockActual() {
        inventario.actualizarInventario(1L, 17, "SALIDA");
        assertEquals("Productos bajo stock minimo:" + SALTO
                + "- Clean Code: stock 3 de minimo 5" + SALTO
                + "- Cafe: stock 2 de minimo 4", reporte.generarReporte());
    }

    @Test
    void generarReporte_conReordenNotificada_incluyeLaCantidadAReordenar() {
        inventario.setNotificador(reporte);
        inventario.actualizarInventario(1L, 17, "SALIDA");
        assertEquals("Productos bajo stock minimo:" + SALTO
                + "- Clean Code: stock 3 de minimo 5, reordenar 12 unidades" + SALTO
                + "- Cafe: stock 2 de minimo 4", reporte.generarReporte());
    }

    @Test
    void generarReporte_productoRepuestoDespuesDeLaAlerta_dejaDeAparecer() {
        inventario.setNotificador(reporte);
        inventario.actualizarInventario(1L, 17, "SALIDA");
        inventario.actualizarInventario(1L, 10, "ENTRADA");
        assertEquals("Productos bajo stock minimo:" + SALTO
                + "- Cafe: stock 2 de minimo 4", reporte.generarReporte());
    }
}
