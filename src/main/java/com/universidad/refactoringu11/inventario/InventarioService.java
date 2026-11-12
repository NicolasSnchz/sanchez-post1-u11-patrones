package com.universidad.refactoringu11.inventario;

import com.universidad.refactoringu11.reportes.ReporteBajoStockService; // inventario ahora importa reportes
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class InventarioService {

    private final List<Producto> productos = new ArrayList<>();
    private final List<String> movimientos = new ArrayList<>();

    private ReporteBajoStockService reporteBajoStockService; // colaborador opcional, sin final

    @Autowired(required = false)
    public void setReporteBajoStockService(ReporteBajoStockService reporteBajoStockService) {
        this.reporteBajoStockService = reporteBajoStockService;
    }

    public void agregarProducto(Producto producto) {
        productos.add(producto);
    }

    public List<Producto> productosBajoStockMinimo() {
        return productos.stream().filter(this::necesitaReorden).toList();
    }

    public String actualizarInventario(Long productoId, int cantidad, String tipoMovimiento) {
        if (!esSolicitudValida(productoId, cantidad, tipoMovimiento)) return "ERROR_DATOS_INVALIDOS";
        Producto producto = buscarProducto(productoId);
        if (producto == null) return "ERROR_PRODUCTO_NO_EXISTE";
        int nuevoStock = calcularNuevoStock(producto.getStock(), cantidad, tipoMovimiento);
        if (esSalidaSinStock(tipoMovimiento, nuevoStock)) return "ERROR_STOCK_INSUFICIENTE";
        producto.setStock(nuevoStock);
        registrarMovimiento(tipoMovimiento, cantidad, producto);
        return necesitaReorden(producto) ? solicitarReorden(producto) : "OK_" + nuevoStock;
    }

    private boolean esSolicitudValida(Long productoId, int cantidad, String tipoMovimiento) {
        return productoId != null && cantidad != 0
                && (esEntrada(tipoMovimiento) || esSalida(tipoMovimiento));
    }

    private boolean esEntrada(String tipoMovimiento) {
        return tipoMovimiento.equals("ENTRADA");
    }

    private boolean esSalida(String tipoMovimiento) {
        return tipoMovimiento.equals("SALIDA");
    }

    private Producto buscarProducto(Long productoId) {
        return productos.stream()
                .filter(p -> p.getId().equals(productoId))
                .findFirst()
                .orElse(null);
    }

    private int calcularNuevoStock(int stockActual, int cantidad, String tipoMovimiento) {
        return esEntrada(tipoMovimiento) ? stockActual + cantidad : stockActual - cantidad;
    }

    private boolean esSalidaSinStock(String tipoMovimiento, int nuevoStock) {
        return esSalida(tipoMovimiento) && nuevoStock < 0;
    }

    private void registrarMovimiento(String tipoMovimiento, int cantidad, Producto producto) {
        String registro = tipoMovimiento + " de " + cantidad + " unidades de "
                + producto.getNombre() + " -> stock final: " + producto.getStock();
        movimientos.add(registro);
        System.out.println(registro);
    }

    private boolean necesitaReorden(Producto producto) {
        return producto.getStock() < producto.getStockMinimo();
    }

    private String solicitarReorden(Producto producto) {
        int cantidadReorden = calcularCantidadReorden(producto);
        alertarBajoStock(producto, cantidadReorden);
        return "OK_BAJO_STOCK_REORDEN_" + cantidadReorden;
    }

    private int calcularCantidadReorden(Producto producto) {
        return producto.getStockMinimo() * 3 - producto.getStock();
    }

    private void alertarBajoStock(Producto producto, int cantidadReorden) {
        if (reporteBajoStockService != null) {
            System.out.println(reporteBajoStockService.generarReporte());
        }
        System.out.println("ALERTA: " + producto.getNombre()
                + " bajo stock minimo. Reordenar " + cantidadReorden + " unidades.");
    }
}
