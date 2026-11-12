#!/usr/bin/env bash
# Mide la evidencia de las dos partes del laboratorio y la deja en $SALIDA.
# No se detiene cuando una prueba falla: en este laboratorio el fallo tambien es evidencia.
set -u

SALIDA=${SALIDA:-$PWD/evid}
REPO=$PWD
TRABAJO=$(mktemp -d)
HERRAMIENTAS=$TRABAJO/herramientas
cp -r "$REPO/.github/evidencias" "$HERRAMIENTAS"
mkdir -p "$SALIDA/logs" "$SALIDA/jacoco" "$SALIDA/surefire" "$SALIDA/cpd"

J17=$JAVA_HOME_17_X64
J23=$JAVA_HOME_23_X64
PMD=org.apache.maven.plugins:maven-pmd-plugin:3.26.0

commit_con() { git -C "$REPO" log --format=%H -F --grep="$1" | tail -1; }

# correr <nombre> <directorio> <jdk> <argumentos de mvn...>
correr() {
    local nombre=$1 dir=$2 jdk=$3
    shift 3
    echo "### $nombre: mvn $*"
    (cd "$dir" && JAVA_HOME=$jdk PATH="$jdk/bin:$PATH" mvn -B -ntp "$@") > "$SALIDA/logs/$nombre.log" 2>&1
    echo $? > "$SALIDA/logs/$nombre.exit"
    if [ -d "$dir/target/surefire-reports" ]; then
        mkdir -p "$SALIDA/surefire/$nombre"
        cp "$dir"/target/surefire-reports/*.xml "$SALIDA/surefire/$nombre/" 2>/dev/null
    fi
    if [ -f "$dir/target/site/jacoco/jacoco.xml" ]; then
        rm -rf "$SALIDA/jacoco/$nombre"
        cp -r "$dir/target/site/jacoco" "$SALIDA/jacoco/$nombre"
    fi
}

# arrancar <nombre> <directorio>: mvn spring-boot:run (la app no expone web, arranca y termina)
arrancar() {
    correr "$1" "$2" "$J17" spring-boot:run
}

cpd() {
    local nombre=$1 dir=$2
    correr "cpd-$nombre" "$dir" "$J17" "$PMD:cpd" -DminimumTokens=20 -Dcpd.ignoreLiterals=true \
        -Dcpd.ignoreIdentifiers=true -Dformat=xml
    cp "$dir/target/cpd.xml" "$SALIDA/cpd/$nombre.xml" 2>/dev/null
}

sin_lazy() {
    local dir=$1
    sed -i -e '/^import org.springframework.context.annotation.Lazy;$/d' -e 's/(@Lazy InventarioService/(InventarioService/' \
        -e '/@Lazy: InventarioService/d' "$dir/src/main/java/com/universidad/refactoringu11/reportes/ReporteBajoStockService.java"
}

{
    echo "commit analizado: $(git -C "$REPO" rev-parse HEAD)"
    JAVA_HOME=$J17 mvn -v
    echo
    "$J23/bin/java" -version 2>&1
} > "$SALIDA/entorno.txt" 2>&1

# ---------- Parte 1: los commits reales del historial ----------
C1=$(commit_con "feat: crear inventario con Long Method")
C2=$(commit_con "test: agregar pruebas de caracterizacion")
C3=$(commit_con "refactor: eliminar Long Method")
C4=$(commit_con "refactor: eliminar switch duplicado")
printf 'C1 %s\nC2 %s\nC3 %s\nC4 %s\n' "$C1" "$C2" "$C3" "$C4" > "$SALIDA/commits.txt"

for c in C1 C2 C3 C4; do
    git -C "$REPO" worktree add -q --detach "$TRABAJO/$c" "${!c}"
done

correr p1-c1-compile "$TRABAJO/C1" "$J17" compile
correr p1-c2-test "$TRABAJO/C2" "$J17" test
correr p1-c3-test "$TRABAJO/C3" "$J17" test
correr p1-c4-test "$TRABAJO/C4" "$J17" test
cpd original "$TRABAJO/C2"
cpd refactorizado "$TRABAJO/C4"

for c in C2 C4; do
    (cd "$TRABAJO/$c" && grep -rn "switch\|else if" src/main/java || echo "(sin coincidencias)") > "$SALIDA/logs/grep-switch-$c.txt"
done

# ---------- Parte 2: los mismos cambios que tendran sus commits, aplicados en orden ----------
P=$TRABAJO/parte2
git -C "$REPO" worktree add -q --detach "$P" HEAD
aplicar() {
    (cd "$P" && git apply "$HERRAMIENTAS/parte2/$1" && git add -A \
        && git -c user.name=evidencias -c user.email=evidencias@local commit -qm "$1" \
        && echo "$1 src=$(git rev-parse HEAD:src) pom=$(git rev-parse HEAD:pom.xml)" >> "$SALIDA/arboles.txt")
}
copia_sin_lazy() {
    local destino=$1
    rm -rf "$destino" && mkdir -p "$destino"
    (cd "$P" && git ls-files -z | xargs -0 cp --parents -t "$destino")
    sin_lazy "$destino"
}

aplicar 1-modulo-reportes.diff
correr p2-1-test "$P" "$J17" test
arrancar p2-1-arranque "$P"
copia_sin_lazy "$TRABAJO/p2-1-guia"
correr p2-1-guia-test "$TRABAJO/p2-1-guia" "$J17" test
arrancar p2-1-guia-arranque "$TRABAJO/p2-1-guia"

aplicar 2-prueba-arquitectura.diff
correr p2-2-arquitectura "$P" "$J17" test -Dtest=ArquitecturaPaquetesTest
correr p2-2-test "$P" "$J17" test

aplicar 3-inversion-dependencia.diff
correr p2-3-arquitectura "$P" "$J17" test -Dtest=ArquitecturaPaquetesTest
correr p2-3-verify "$P" "$J17" verify
arrancar p2-3-arranque "$P"
correr p2-3-verify-jdk23 "$P" "$J23" verify
correr p2-3-dependencias "$P" "$J17" dependency:tree -Dscope=test
copia_sin_lazy "$TRABAJO/p2-3-guia"
correr p2-3-guia-test "$TRABAJO/p2-3-guia" "$J17" test
arrancar p2-3-guia-arranque "$TRABAJO/p2-3-guia"
(cd "$P" && grep -rn "import" src/main/java/com/universidad/refactoringu11/inventario) > "$SALIDA/logs/imports-inventario-final.txt"

# Compatibilidad de ArchUnit: la version de la guia frente a la ultima, en los dos JDK
for v in 1.3.0 1.4.2; do
    D=$TRABAJO/archunit-$v
    rm -rf "$D" && mkdir -p "$D" && (cd "$P" && git ls-files -z | xargs -0 cp --parents -t "$D")
    sed -i "s#<version>1.3.0</version>#<version>$v</version>#" "$D/pom.xml"
    correr "archunit-$v-jdk17" "$D" "$J17" test -Dtest=ArquitecturaPaquetesTest
    correr "archunit-$v-jdk23" "$D" "$J23" test -Dtest=ArquitecturaPaquetesTest
done

for f in "$SALIDA"/logs/*.exit; do echo "$(basename "$f" .exit) -> $(cat "$f")"; done > "$SALIDA/resumen.txt"
cat "$SALIDA/resumen.txt"
