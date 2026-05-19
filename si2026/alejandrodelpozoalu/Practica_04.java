package si2026.alejandrodelpozoalu.p05;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Practica_04 {

    static int n;
    static int[][] tablero;
    static int[] sumaFilasObjetivo;
    static int[] sumaColumnasObjetivo;

    static boolean[][] seleccionados;

    public static void main(String[] args) {
        String ficheroEntrada = "./src/tableros.txt";
        String ficheroSalida = "./src/soluciones.txt";

        try (BufferedReader br = new BufferedReader(new FileReader(ficheroEntrada));
             BufferedWriter bw = new BufferedWriter(new FileWriter(ficheroSalida))) {

            String linea;

            while ((linea = br.readLine()) != null) {
                cargarTablero(linea);

                seleccionados = new boolean[n][n];

                int[] sumaFilasActual = new int[n];
                int[] sumaColumnasActual = new int[n];

                boolean solucion = resolver(0, 0, sumaFilasActual, sumaColumnasActual);

                if (solucion) {
                    String solucionTexto = obtenerSolucionFormatoTexto();
                    bw.write(solucionTexto);
                    bw.newLine();
                } else {
                    bw.write("Sin solucion");
                    bw.newLine();
                }
            }

            System.out.println("Soluciones guardadas en soluciones.txt");

        } catch (IOException e) {
            System.out.println("Error con los ficheros");
            e.printStackTrace();
        }
    }

    static void cargarTablero(String entrada) {

        String[] partes = entrada.split(";");

        n = partes.length - 1;

        tablero = new int[n][n];

        sumaFilasObjetivo = new int[n];
        sumaColumnasObjetivo = new int[n];

        // Leer filas
        for (int fila = 0; fila < n; fila++) {

            String[] numeros = partes[fila].trim().split("\\s+");

            for (int columna = 0; columna < n; columna++) {
                tablero[fila][columna] = Integer.parseInt(numeros[columna]);
            }

            sumaFilasObjetivo[fila] = Integer.parseInt(numeros[n]);
        }

        // Leer sumas columnas
        String[] columnas = partes[n].trim().split("\\s+");

        for (int columna = 0; columna < n; columna++) {
            sumaColumnasObjetivo[columna] = Integer.parseInt(columnas[columna]);
        }
    }

    static boolean resolver(int fila, int columna, int[] sumaFilasActual, int[] sumaColumnasActual) {
        // Caso final
        if (fila == n) {
            for (int i = 0; i < n; i++) {
                if (sumaFilasActual[i] != sumaFilasObjetivo[i]) return false;
            }

            for (int j = 0; j < n; j++) {
                if (sumaColumnasActual[j] != sumaColumnasObjetivo[j]) return false;
            }

            return true;
        }

        // Siguiente posición
        int siguienteFila = fila;
        int siguienteColumna = columna + 1;

        if (siguienteColumna == n) {
            siguienteColumna = 0;
            siguienteFila++;
        }

        int valor = tablero[fila][columna];

        // OPCIÓN 1 -> seleccionar el valor
        if (sumaFilasActual[fila] + valor <= sumaFilasObjetivo[fila] && sumaColumnasActual[columna] + valor <= sumaColumnasObjetivo[columna]) {
            seleccionados[fila][columna] = true;

            sumaFilasActual[fila] += valor;
            sumaColumnasActual[columna] += valor;

            if (resolver(siguienteFila, siguienteColumna, sumaFilasActual, sumaColumnasActual)) return true;

            // Backtracking
            sumaFilasActual[fila] -= valor;
            sumaColumnasActual[columna] -= valor;
        }

        // OPCIÓN 2 -> no seleccionar el valor
        seleccionados[fila][columna] = false;

        if (resolver(siguienteFila, siguienteColumna, sumaFilasActual, sumaColumnasActual)) return true;

        return false;
    }

    static String obtenerSolucionFormatoTexto() {
        StringBuilder sb = new StringBuilder();

        // Filas
        for (int fila = 0; fila < n; fila++) {
            for (int columna = 0; columna < n; columna++) {
                if (seleccionados[fila][columna]) {
                    sb.append(tablero[fila][columna]);
                } else {
                    sb.append(".");
                }

                sb.append(" ");
            }

            sb.append(sumaFilasObjetivo[fila]);

            if (fila < n - 1) {
                sb.append(" ; ");
            }
        }

        // Última fila
        sb.append(" ; ");

        for (int columna = 0; columna < n; columna++) {
            sb.append(sumaColumnasObjetivo[columna]);
            if (columna < n - 1) {
                sb.append(" ");
            }
        }
        
        return sb.toString();
    }
}