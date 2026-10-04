package pokemon;

import pokemon.interfaz.VentanaCombate;

import javax.swing.*;

/**
 * PUNTO DE ENTRADA del programa: la primera clase que se ejecuta.
 *
 * ¿Como funciona el main?
 * Cuando le das "Run" a esta clase, Java busca un metodo que se llame
 * exactamente asi:   public static void main(String[] args)
 * y empieza a ejecutar desde ahi. Cada palabra tiene su razon:
 *   - public  -> Java (que esta "afuera" de la clase) tiene que poder llamarlo.
 *   - static  -> se puede ejecutar SIN crear un objeto de la clase Main primero
 *                (al arrancar todavia no existe ningun objeto).
 *   - void    -> no devuelve nada.
 *   - String[] args -> arreglo con textos que se pueden pasar al ejecutar
 *                desde la terminal. Aqui no lo usamos, pero Java lo exige.
 *
 * En tu PokeApi.java el main hacia: crear el objeto y llamar un metodo.
 * Aqui es igual, solo que el "objeto" es la ventana del juego.
 */
public class Main
{
    public static void main(String[] args)
    {
        // ¿Que es SwingUtilities.invokeLater?
        // Swing (la libreria de ventanas) tiene UN hilo especial que se encarga de
        // dibujar la ventana y atender los clics. Se llama EDT (Event Dispatch Thread).
        // Regla de Swing: las ventanas y botones SOLO se deben crear y modificar
        // desde ese hilo. invokeLater(...) significa:
        //   "EDT, cuando puedas, ejecuta este codigo".
        //
        // ¿Que es "() -> ..." ?
        // Es una LAMBDA: una forma corta de escribir "un pedacito de codigo" que se
        // le pasa a otro metodo para que lo ejecute despues. Es lo mismo que escribir:
        //
        //   SwingUtilities.invokeLater(new Runnable() {
        //       @Override
        //       public void run() {
        //           VentanaCombate ventana = new VentanaCombate();
        //           ventana.setVisible(true);
        //       }
        //   });
        //
        // pero en una sola linea. Los "()" significan que no recibe parametros.
        SwingUtilities.invokeLater(() ->
        {
            VentanaCombate ventana = new VentanaCombate(); //se crea la ventana (aun invisible)
            ventana.setVisible(true);                      //se muestra en pantalla
        });

        // OJO: el main termina aqui, pero el programa NO se cierra, porque la
        // ventana sigue abierta. Se cierra cuando el usuario cierra la ventana.
    }
}
