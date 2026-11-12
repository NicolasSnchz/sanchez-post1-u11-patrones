package com.universidad.refactoringu11.inventario;

/**
 * Abstraccion con la que el inventario avisa que un producto quedo por debajo de su stock
 * minimo. Vive en el paquete inventario para que el dominio no dependa de quien la implementa.
 */
public interface NotificadorBajoStock {

    /** Se invoca una vez por cada movimiento que deja al producto bajo su stock minimo. */
    void notificar(Producto producto, int cantidadReorden);
}
