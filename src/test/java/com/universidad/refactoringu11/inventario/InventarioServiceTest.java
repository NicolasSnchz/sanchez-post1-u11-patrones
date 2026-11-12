package com.universidad.refactoringu11.inventario;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class InventarioServiceTest {

    private InventarioService service;
    private Producto libro;

    @BeforeEach
    void setUp() {
        service = new InventarioService();
        libro = new Producto(1L, "Clean Code", TipoProducto.LIBRO, 45.0, 20, 5);
        service.agregarProducto(libro);
    }

    @Test
    void entrada_incrementaStockYRetornaOk() {
        assertEquals("OK_30", service.actualizarInventario(1L, 10, "ENTRADA"));
        assertEquals(30, libro.getStock());
    }

    @Test
    void salida_conStockSuficiente_descuentaYRetornaOk() {
        assertEquals("OK_12", service.actualizarInventario(1L, 8, "SALIDA"));
    }

    @Test
    void salida_conStockInsuficiente_retornaErrorYNoModificaStock() {
        assertEquals("ERROR_STOCK_INSUFICIENTE", service.actualizarInventario(1L, 50, "SALIDA"));
        assertEquals(20, libro.getStock());
    }

    @Test
    void salida_dejaStockBajoElMinimo_activaReordenConCantidadCalculada() {
        // stockMinimo=5, stock inicial=20, salida de 17 -> nuevoStock=3 (<5)
        // cantidadReorden = stockMinimo*3 - nuevoStock = 15 - 3 = 12
        assertEquals("OK_BAJO_STOCK_REORDEN_12", service.actualizarInventario(1L, 17, "SALIDA"));
        assertEquals(3, libro.getStock());
    }

    @Test
    void tipoMovimientoInvalido_retornaError() {
        assertEquals("ERROR_DATOS_INVALIDOS", service.actualizarInventario(1L, 5, "TRANSFERENCIA"));
    }

    @Test
    void productoInexistente_retornaError() {
        assertEquals("ERROR_PRODUCTO_NO_EXISTE", service.actualizarInventario(99L, 5, "ENTRADA"));
    }

    // Casos limite agregados a la red de seguridad: cada uno fija una rama o un borde
    // del codigo original que las seis pruebas anteriores no ejercitan.

    @Test
    void cantidadCero_retornaErrorYNoModificaStock() {
        assertEquals("ERROR_DATOS_INVALIDOS", service.actualizarInventario(1L, 0, "ENTRADA"));
        assertEquals(20, libro.getStock());
    }

    @Test
    void productoIdNulo_retornaError() {
        assertEquals("ERROR_DATOS_INVALIDOS", service.actualizarInventario(null, 5, "ENTRADA"));
    }

    @Test
    void tipoMovimientoNulo_lanzaNullPointerException() {
        // el codigo original llama tipoMovimiento.equals(...) sin validar null
        assertThrows(NullPointerException.class, () -> service.actualizarInventario(1L, 5, null));
    }

    @Test
    void salida_queDejaElStockIgualAlMinimo_noActivaReorden() {
        assertEquals("OK_5", service.actualizarInventario(1L, 15, "SALIDA"));
    }

    @Test
    void salida_deTodoElStock_dejaCeroYActivaReorden() {
        assertEquals("OK_BAJO_STOCK_REORDEN_15", service.actualizarInventario(1L, 20, "SALIDA"));
        assertEquals(0, libro.getStock());
    }

    @Test
    void salida_deUnaUnidadMasQueElStock_retornaError() {
        assertEquals("ERROR_STOCK_INSUFICIENTE", service.actualizarInventario(1L, 21, "SALIDA"));
        assertEquals(20, libro.getStock());
    }

    @Test
    void entrada_queNoAlcanzaElMinimo_activaReorden() {
        Producto teclado = new Producto(2L, "Teclado", TipoProducto.ELECTRONICO, 100.0, 1, 5);
        service.agregarProducto(teclado);
        assertEquals("OK_BAJO_STOCK_REORDEN_12", service.actualizarInventario(2L, 2, "ENTRADA"));
        assertEquals(3, teclado.getStock());
    }

    @Test
    void entrada_conCantidadNegativa_restaSinValidarElStock() {
        // comportamiento actual: ENTRADA no revisa que el stock quede negativo
        assertEquals("OK_BAJO_STOCK_REORDEN_20", service.actualizarInventario(1L, -25, "ENTRADA"));
        assertEquals(-5, libro.getStock());
    }

    @Test
    void salida_conCantidadNegativa_sumaAlStock() {
        assertEquals("OK_25", service.actualizarInventario(1L, -5, "SALIDA"));
    }

    @Test
    void productosConIdRepetido_actualizaElPrimeroAgregado() {
        Producto copia = new Producto(1L, "Clean Code (copia)", TipoProducto.LIBRO, 45.0, 50, 5);
        service.agregarProducto(copia);
        assertEquals("OK_30", service.actualizarInventario(1L, 10, "ENTRADA"));
        assertEquals(50, copia.getStock());
    }

    @Test
    void movimientoConReorden_imprimeElRegistroYLuegoLaAlerta() {
        String salida = consolaDurante(() -> service.actualizarInventario(1L, 17, "SALIDA"));
        assertEquals("SALIDA de 17 unidades de Clean Code -> stock final: 3" + System.lineSeparator()
                + "ALERTA: Clean Code bajo stock minimo. Reordenar 12 unidades." + System.lineSeparator(), salida);
    }

    @Test
    void movimientoSinReorden_imprimeSoloElRegistro() {
        String salida = consolaDurante(() -> service.actualizarInventario(1L, 10, "ENTRADA"));
        assertEquals("ENTRADA de 10 unidades de Clean Code -> stock final: 30" + System.lineSeparator(), salida);
    }

    @Test
    void movimientoRechazado_noImprimeNada() {
        assertEquals("", consolaDurante(() -> service.actualizarInventario(1L, 50, "SALIDA")));
    }

    private String consolaDurante(Runnable accion) {
        PrintStream original = System.out;
        ByteArrayOutputStream capturada = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturada, true, StandardCharsets.UTF_8));
        try {
            accion.run();
        } finally {
            System.setOut(original);
        }
        return capturada.toString(StandardCharsets.UTF_8);
    }
}
