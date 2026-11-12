package com.universidad.refactoringu11.inventario;

/**
 * Tipos de producto del inventario. Cada tipo sabe calcular su recargo de envio y armar su
 * etiqueta, asi que agregar un tipo nuevo solo exige declararlo aqui: el compilador obliga a
 * definir su recargo y los datos de su etiqueta.
 */
public enum TipoProducto {

    LIBRO("LIBRO", "Exento de IVA") {
        @Override
        public double calcularRecargoEnvio(double precio) {
            return 0.0;
        }
    },
    ELECTRONICO("ELECTRO", "Garantia 1 ano") {
        @Override
        public double calcularRecargoEnvio(double precio) {
            return precio * 0.08;
        }
    },
    ROPA("ROPA", "Ver talla") {
        @Override
        public double calcularRecargoEnvio(double precio) {
            return precio * 0.03;
        }
    },
    ALIMENTO("ALIMENTO", "Perecedero") {
        @Override
        public double calcularRecargoEnvio(double precio) {
            return 2.50;
        }
    };

    private final String prefijoEtiqueta;
    private final String leyendaEtiqueta;

    TipoProducto(String prefijoEtiqueta, String leyendaEtiqueta) {
        this.prefijoEtiqueta = prefijoEtiqueta;
        this.leyendaEtiqueta = leyendaEtiqueta;
    }

    /** Recargo de envio que este tipo agrega al precio del producto. */
    public abstract double calcularRecargoEnvio(double precio);

    /** Etiqueta con el formato comun a todos los tipos: [PREFIJO] nombre - leyenda. */
    public String generarEtiqueta(String nombreProducto) {
        return "[" + prefijoEtiqueta + "] " + nombreProducto + " - " + leyendaEtiqueta;
    }
}
