package com.universidad.refactoringu11.reportes;

import com.universidad.refactoringu11.inventario.InventarioService;
import com.universidad.refactoringu11.inventario.NotificadorBajoStock;
import com.universidad.refactoringu11.inventario.Producto;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReporteBajoStockService implements NotificadorBajoStock {

    private final InventarioService inventarioService;
    private final Map<Long, Integer> reordenesNotificadas = new HashMap<>();

    // @Lazy: InventarioService tambien depende de este servicio, y sin el proxy Spring no puede crear ninguno de los dos
    public ReporteBajoStockService(@Lazy InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @Override
    public void notificar(Producto producto, int cantidadReorden) {
        reordenesNotificadas.put(producto.getId(), cantidadReorden);
    }

    public String generarReporte() {
        List<Producto> bajoStock = inventarioService.productosBajoStockMinimo();
        if (bajoStock.isEmpty()) {
            return "Sin productos bajo stock minimo";
        }
        return bajoStock.stream()
                .map(this::describir)
                .collect(Collectors.joining(System.lineSeparator(),
                        "Productos bajo stock minimo:" + System.lineSeparator(), ""));
    }

    private String describir(Producto producto) {
        String linea = "- " + producto.getNombre() + ": stock " + producto.getStock()
                + " de minimo " + producto.getStockMinimo();
        Integer cantidadReorden = reordenesNotificadas.get(producto.getId());
        return cantidadReorden == null ? linea : linea + ", reordenar " + cantidadReorden + " unidades";
    }
}
