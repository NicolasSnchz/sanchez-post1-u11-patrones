package com.universidad.refactoringu11.inventario;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class InventarioServiceNotificacionTest {

    // Spy escrito a mano: NotificadorBajoStock tiene un solo metodo, asi que basta una lambda que anote cada llamada
    private final List<Producto> productosNotificados = new ArrayList<>();
    private final List<Integer> cantidadesNotificadas = new ArrayList<>();

    private InventarioService service;
    private Producto libro;

    @BeforeEach
    void setUp() {
        service = new InventarioService();
        libro = new Producto(1L, "Clean Code", TipoProducto.LIBRO, 45.0, 20, 5);
        service.agregarProducto(libro);
        service.setNotificador((producto, cantidadReorden) -> {
            productosNotificados.add(producto);
            cantidadesNotificadas.add(cantidadReorden);
        });
    }

    @Test
    void actualizarInventario_salidaBajoElMinimo_notificaUnaVezConLaCantidadDeReorden() {
        assertEquals("OK_BAJO_STOCK_REORDEN_12", service.actualizarInventario(1L, 17, "SALIDA"));
        assertEquals(List.of(libro), productosNotificados);
        assertEquals(List.of(12), cantidadesNotificadas);
    }

    @Test
    void actualizarInventario_movimientoSinReorden_noNotifica() {
        assertEquals("OK_30", service.actualizarInventario(1L, 10, "ENTRADA"));
        assertTrue(productosNotificados.isEmpty());
    }

    @Test
    void actualizarInventario_movimientoRechazado_noNotifica() {
        assertEquals("ERROR_STOCK_INSUFICIENTE", service.actualizarInventario(1L, 50, "SALIDA"));
        assertTrue(productosNotificados.isEmpty());
    }
}
