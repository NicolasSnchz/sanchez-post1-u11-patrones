package com.universidad.refactoringu11;

import com.universidad.refactoringu11.inventario.InventarioService;
import com.universidad.refactoringu11.inventario.Producto;
import com.universidad.refactoringu11.inventario.TipoProducto;
import com.universidad.refactoringu11.reportes.ReporteBajoStockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

// Con el contexto real de Spring: comprueba que el setter conecta ReporteBajoStockService como notificador
@SpringBootTest
class NotificacionBajoStockIntegracionTest {

    @Autowired
    private InventarioService inventarioService;

    @Autowired
    private ReporteBajoStockService reporteBajoStockService;

    @Test
    void salidaBajoElMinimo_quedaEnElReporteConSuCantidadDeReorden() {
        inventarioService.agregarProducto(new Producto(10L, "Monitor", TipoProducto.ELECTRONICO, 300.0, 8, 4));

        assertEquals("OK_BAJO_STOCK_REORDEN_10", inventarioService.actualizarInventario(10L, 6, "SALIDA"));
        assertEquals("Productos bajo stock minimo:" + System.lineSeparator()
                + "- Monitor: stock 2 de minimo 4, reordenar 10 unidades", reporteBajoStockService.generarReporte());
    }
}
