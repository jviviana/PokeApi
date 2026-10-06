package pokemon;

import pokemon.interfaz.VentanaCombate;

import javax.swing.*;

/**
 * PUNTO DE ENTRADA del programa: la primera clase que se ejecuta.

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

        SwingUtilities.invokeLater(() ->
        {
            VentanaCombate ventana = new VentanaCombate(); //se crea la ventana (aun invisible)
            ventana.setVisible(true);                      //se muestra en pantalla
        });

        // OJO: el main termina aqui, pero el programa NO se cierra, porque la
        // ventana sigue abierta. Se cierra cuando el usuario cierra la ventana.
    }
}
