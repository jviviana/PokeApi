package pokemon.interfaz;

import pokemon.api.PokeApiClient;
import pokemon.combate.Battle;
import pokemon.combate.BattleListener;
import pokemon.modelo.Pokemon;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * VENTANA PRINCIPAL: une los dos paneles de jugador, el boton ¡PELEAR! y el log.

 */
public class VentanaCombate extends JFrame implements BattleListener
{
    private final PanelPokemon panel1;
    private final PanelPokemon panel2;
    private final JButton botonPelear = new JButton("¡PELEAR!");
    private final JLabel etiquetaResultado = new JLabel(" ", SwingConstants.CENTER);
    private final JTextArea areaLog = new JTextArea(7, 60); //7 filas, 60 columnas

    private Battle combateActual;
    private boolean hayCombate = false;

    public VentanaCombate()
    {
        super("Pokémon Stadium Lite"); //llama al constructor de JFrame con el titulo de la ventana

        //un solo cliente de la API, compartido por los dos paneles
        PokeApiClient clienteApi = new PokeApiClient();

        // "this::actualizarBotonPelear" es una REFERENCIA A METODO: otra forma corta de lambda.

        panel1 = new PanelPokemon("Jugador 1", new Color(219, 234, 254), clienteApi, this::actualizarBotonPelear);
        panel2 = new PanelPokemon("Jugador 2", new Color(254, 226, 226), clienteApi, this::actualizarBotonPelear);

        armarDisenio();
        descargarNombres(clienteApi);

        botonPelear.addActionListener(e -> iniciarCombate());
        actualizarBotonPelear(); //empieza deshabilitado porque no hay Pokemon cargados

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); //al cerrar la ventana, se cierra el programa
        pack();                       //ajusta el tamaño de la ventana a lo que tiene adentro
        setLocationRelativeTo(null);  //null = centrar la ventana en la pantalla
    }

    /**
     * Descarga la lista de nombres de todos los Pokemon para las sugerencias del buscador.
     * Se hace con un SwingWorker (igual que al cargar un Pokemon) para no congelar la ventana.
     * Si falla (por ejemplo, sin internet) simplemente no habra sugerencias; el resto sigue igual.
     */
    private void descargarNombres(PokeApiClient clienteApi)
    {
        new SwingWorker<List<String>, Void>()
        {
            @Override
            protected List<String> doInBackground() throws Exception
            {
                return clienteApi.obtenerNombres(); //hilo en segundo plano: aqui va lo lento
            }

            @Override
            protected void done()
            {
                try
                {
                    // hilo de Swing: le damos la misma lista a los dos paneles
                    List<String> nombres = get();
                    panel1.setNombresDisponibles(nombres);
                    panel2.setNombresDisponibles(nombres);
                }
                catch (Exception e)
                {
                    //sin sugerencias, pero la app funciona igual
                }
            }
        }.execute();
    }

    private void armarDisenio()
    {
        // boton azul oscuro con letras blancas para que resalte
        botonPelear.setFont(botonPelear.getFont().deriveFont(Font.BOLD, 22f));
        botonPelear.setBackground(new Color(30, 58, 138));
        botonPelear.setForeground(Color.WHITE);
        botonPelear.setPreferredSize(new Dimension(180, 60));
        etiquetaResultado.setFont(etiquetaResultado.getFont().deriveFont(Font.BOLD, 17f));
        etiquetaResultado.setForeground(new Color(30, 58, 138)); //mismo azul oscuro del boton

        // BorderLayout divide el espacio en 5 zonas: NORTH (arriba), SOUTH (abajo),
        // WEST (izquierda), EAST (derecha) y CENTER (centro).
        // GridBagLayout con un solo componente lo deja centrado vertical y horizontalmente.
        JPanel centro = new JPanel(new GridBagLayout());
        JPanel contenidoCentro = new JPanel(new BorderLayout(0, 10)); //10 px de separacion vertical
        contenidoCentro.add(botonPelear, BorderLayout.CENTER);
        contenidoCentro.add(etiquetaResultado, BorderLayout.SOUTH);
        centro.add(contenidoCentro);
        // Paneles transparentes: asi se ve el color de fondo de la ventana
        centro.setOpaque(false);
        contenidoCentro.setOpaque(false);
        // Le damos un ancho fijo de 260 px a la columna del centro. Sin esto, el ancho se calcula
        // cuando todavia no hay texto, y despues "Ganador: Squirtle" no cabe y se ve cortado.
        // El alto en 0 significa "no importa": la altura la manda lo mas alto (los paneles de jugador).
        centro.setPreferredSize(new Dimension(260, 0));

        //jugador 1 a la izquierda, boton al centro, jugador 2 a la derecha
        JPanel arriba = new JPanel(new BorderLayout(10, 0));
        arriba.setOpaque(false); //transparente para que se vea el fondo crema
        arriba.add(panel1, BorderLayout.WEST);
        arriba.add(centro, BorderLayout.CENTER);
        arriba.add(panel2, BorderLayout.EAST);

        // LOG DE BATALLA (lo pide el taller): JTextArea dentro de un JScrollPane.
        // JScrollPane le agrega barras de desplazamiento cuando el texto no cabe.
        areaLog.setEditable(false); //el usuario no puede escribir en el log
        areaLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        areaLog.setBackground(Color.WHITE);              //fondo blanco
        areaLog.setForeground(new Color(30, 41, 59));    //letras gris azulado oscuro
        JScrollPane scrollLog = new JScrollPane(areaLog);
        scrollLog.setBorder(BorderFactory.createTitledBorder("Log de batalla"));

        JPanel raiz = new JPanel(new BorderLayout(10, 10));
        raiz.setBackground(new Color(241, 245, 249)); //gris azulado muy claro, fondo de la ventana
        raiz.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); //margen de 10 px
        raiz.add(arriba, BorderLayout.CENTER);
        raiz.add(scrollLog, BorderLayout.SOUTH);
        setContentPane(raiz); //este panel es el contenido de la ventana
    }

    //¡PELEAR! solo se habilita si los dos Pokemon estan cargados y no hay un combate en curso
    private void actualizarBotonPelear()
    {
        boolean ambosCargados = panel1.getPokemon() != null && panel2.getPokemon() != null;
        botonPelear.setEnabled(ambosCargados && !hayCombate);
    }

    private void iniciarCombate()
    {
        Pokemon p1 = panel1.getPokemon();
        Pokemon p2 = panel2.getPokemon();
        if (p1 == null || p2 == null) return;

        marcarCombateEnCurso(true);
        areaLog.setText("");
        etiquetaResultado.setText(" ");

        // le pasamos "this" (esta ventana) como escuchador: Battle nos avisara a nosotros
        combateActual = new Battle(p1, p2, this);
        Battle combate = combateActual;

        // ¿Que es un Thread (hilo)?
        // Un hilo es una "linea de ejecucion": el programa puede hacer varias cosas
        // a la vez si las pone en hilos distintos.
        //
        // El combate tiene pausas de 0.7 s entre turnos. Si lo corrieramos en el hilo
        // de Swing, la ventana se congelaria durante TODO el combate. Por eso creamos
        // un hilo nuevo solo para el combate. La lambda "() -> {...}" es el codigo
        // que ese hilo va a ejecutar, y "hilo-combate" es solo un nombre para identificarlo.
        Thread hiloCombate = new Thread(() ->
        {
            try
            {
                combate.iniciar();
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }, "hilo-combate");

        // daemon = hilo "secundario": si cierras la ventana a mitad del combate,
        // el programa se cierra sin esperar a que este hilo termine.
        hiloCombate.setDaemon(true);
        hiloCombate.start(); //start() arranca el hilo (sin esto no pasa nada)
    }

    private void marcarCombateEnCurso(boolean enCurso)
    {
        hayCombate = enCurso;
        //durante el combate no se puede cambiar de Pokemon
        panel1.habilitarControles(!enCurso);
        panel2.habilitarControles(!enCurso);
        actualizarBotonPelear();
    }

    //agrega una linea al log y baja el scroll hasta el final
    private void escribirEnLog(String linea)
    {
        areaLog.append(linea + "\n");
        //mover el cursor al final hace que el scroll baje solo a la ultima linea
        areaLog.setCaretPosition(areaLog.getDocument().getLength());
    }

    // ===================== METODOS DE BattleListener =====================
    // Estos metodos los llama Battle DESDE EL HILO DEL COMBATE (no desde el de Swing).
    // Pero Swing solo se puede modificar desde SU hilo (el EDT).
    // Por eso cada uno envuelve su codigo en SwingUtilities.invokeLater(...),
    // que significa: "hilo de Swing, ejecuta esto tu" (ver explicacion en Main).

    @Override
    public void onCombateIniciado(String primerAtacante)
    {
        SwingUtilities.invokeLater(() ->
        {
            escribirEnLog("¡Comienza el combate! " + combateActual.getNombre1() + " vs " + combateActual.getNombre2());
            escribirEnLog(primerAtacante + " es más rápido y ataca primero.");
            escribirEnLog("");
        });
    }

    @Override
    public void onTurn(String attacker, String defender, int damage, boolean critical, double modifier)
    {
        SwingUtilities.invokeLater(() ->
        {
            // ANIMACION: el que ataca se lanza hacia el rival.
            // Si el atacante es el Jugador 1 (izquierda) se mueve a la derecha; si es el 2, a la izquierda.
            boolean ataca1 = attacker.equals(combateActual.getNombre1());
            PanelPokemon panelAtacante = ataca1 ? panel1 : panel2;
            panelAtacante.animarAtaque(ataca1);

            // StringBuilder sirve para armar un texto por partes con append(...);
            // al final toString() lo convierte en un String normal.
            StringBuilder linea = new StringBuilder();
            linea.append(attacker).append(" ataca a ").append(defender)
                    .append(" y causa ").append(damage).append(" de daño");
            if (critical) linea.append(" ¡GOLPE CRÍTICO!");
            if (modifier > 1.0) linea.append(" ¡Es súper efectivo!");
            if (modifier < 1.0) linea.append(" No es muy efectivo...");
            escribirEnLog(linea.toString());
        });
    }

    @Override
    public void onHpChanged(String pokemon, int hpActual)
    {
        SwingUtilities.invokeLater(() ->
        {
            //por el nombre sabemos cual de los dos paneles hay que actualizar
            PanelPokemon panel = pokemon.equals(combateActual.getNombre1()) ? panel1 : panel2;
            panel.actualizarVida(hpActual);

            Pokemon p = panel.getPokemon();
            //no escribimos en el log la vida llena del inicio, solo cuando baja
            if (p != null && hpActual < p.getHpMaximo())
            {
                escribirEnLog("   Vida de " + pokemon + ": " + hpActual + " / " + p.getHpMaximo());
            }
        });
    }

    @Override
    public void onBattleEnded(String winner)
    {
        SwingUtilities.invokeLater(() ->
        {
            escribirEnLog("");
            escribirEnLog("*** ¡" + winner + " gana el combate! ***");
            etiquetaResultado.setText("Ganador: " + winner);
            marcarCombateEnCurso(false); //se pueden volver a cargar Pokemon y pelear otra vez
        });
    }
}
