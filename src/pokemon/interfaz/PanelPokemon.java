package pokemon.interfaz;

import pokemon.api.PokeApiClient;
import pokemon.api.PokemonNoEncontradoException;
import pokemon.modelo.Pokemon;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Panel de UN jugador: campo de texto, botones Cargar/Aleatorio, imagen,
 * datos y barra de vida. La ventana crea DOS de estos (uno por jugador).
 *
 * "extends JPanel" = HERENCIA: esta clase ES un JPanel (un panel de Swing),
 * con todo lo que tiene un JPanel, mas lo que nosotros le agregamos.
 */
public class PanelPokemon extends JPanel
{
    private static final int TAMANIO_IMAGEN = 140;

    // Ancho de la etiqueta de la imagen. Es ancha a proposito: la imagen del Pokemon queda
    // en el centro y le sobra espacio a los lados para poder "lanzarse" hacia el rival.
    private static final int ANCHO_ZONA_IMAGEN = 340;
    private static final int ALTO_ZONA_IMAGEN = 150;

    // maximo de nombres que aparecen en la lista de sugerencias del buscador
    private static final int MAX_SUGERENCIAS = 6;

    // Cuantos pixeles se mueve la imagen al atacar (tiene que caber en el espacio que sobra)
    private static final int DISTANCIA_ATAQUE = 70;

    // texto que se muestra en las estadisticas cuando todavia no hay Pokemon
    private static final String TEXTO_STATS_VACIO = "VIDA - | ATAQUE - | DEFENSA - | VELOCIDAD -";

    private final PokeApiClient clienteApi;

    // Runnable = un "pedacito de codigo" guardado en una variable para ejecutarlo despues
    // con .run(). La ventana nos lo pasa para enterarse cada vez que cambia el Pokemon
    // (asi sabe si debe habilitar el boton ¡PELEAR!).
    private final Runnable alCambiarPokemon;

    private final JTextField campoNombre = new JTextField(12);
    private final JButton botonCargar = new JButton("Cargar");
    private final JButton botonAleatorio = new JButton("Aleatorio");

    // ---- SUGERENCIAS DEL BUSCADOR ----
    // Todos los nombres de Pokemon que existen (la ventana nos los da con setNombresDisponibles).
    private List<String> nombresDisponibles = new ArrayList<>();
    // JPopupMenu = una lista flotante que aparece debajo del campo de texto.
    private final JPopupMenu menuSugerencias = new JPopupMenu();
    // Cuando NOSOTROS escribimos en el campo (no el usuario), esto vale true para que
    // no se abra la lista de sugerencias sin que nadie la pida.
    private boolean escribiendoPorCodigo = false;

    // desplazamiento = cuantos pixeles esta movida la imagen hacia la derecha (negativo = izquierda).
    // Normalmente es 0 (quieta). Solo cambia durante la animacion de ataque.
    private int desplazamiento = 0;

    // "new JLabel(...) { ... }" es otra CLASE ANONIMA (como el SwingWorker): un JLabel normal
    // al que le reescribimos paintComponent, el metodo que DIBUJA la etiqueta.
    // g.translate(x, 0) corre el "lapiz" x pixeles a la derecha antes de dibujar,
    // y asi la imagen se ve movida sin tocar el acomodo (layout) del panel.
    private final JLabel etiquetaImagen = new JLabel("Sin Pokémon", SwingConstants.CENTER)
    {
        @Override
        protected void paintComponent(Graphics g)
        {
            // g.create() hace una COPIA del lapiz. Movemos solo la copia y la desechamos al final
            // (dispose); si moviéramos el lapiz original, los demas componentes
            // se dibujarian tambien corridos y la pantalla se veria rota.
            Graphics copia = g.create();
            copia.translate(desplazamiento, 0);
            super.paintComponent(copia);
            copia.dispose();
        }
    };
    private final JLabel etiquetaNombre = new JLabel("-", SwingConstants.CENTER);
    private final JLabel etiquetaTipos = new JLabel("Tipos: -", SwingConstants.CENTER);
    private final JLabel etiquetaStats = new JLabel(TEXTO_STATS_VACIO, SwingConstants.CENTER);
    // JProgressBar = barra de progreso; la usamos como barra de vida
    private final JProgressBar barraVida = new JProgressBar();
    private final JLabel etiquetaEstado = new JLabel(" ", SwingConstants.CENTER); //para mensajes de error

    private Pokemon pokemon; //el Pokemon cargado (null si todavia no hay ninguno)

    public PanelPokemon(String titulo, Color colorFondo, PokeApiClient clienteApi, Runnable alCambiarPokemon)
    {
        this.clienteApi = clienteApi;
        this.alCambiarPokemon = alCambiarPokemon;
        armarDisenio(titulo, colorFondo);

        // ActionListener de los botones (lo pide el taller).
        // "e -> cargarPorNombre()" es una LAMBDA (ver Main): es exactamente lo mismo que
        //
        //   botonCargar.addActionListener(new ActionListener() {
        //       @Override
        //       public void actionPerformed(ActionEvent e) {
        //           cargarPorNombre();
        //       }
        //   });
        //
        // que es lo que genera el diseñador de IntelliJ, pero en una linea.
        // La "e" es el ActionEvent (informacion del clic); aqui no la usamos.
        botonCargar.addActionListener(e -> cargarPorNombre());
        botonAleatorio.addActionListener(e -> cargarPokemon(null)); //null = aleatorio
        //en un JTextField, el ActionListener se dispara al presionar Enter
        campoNombre.addActionListener(e -> cargarPorNombre());

        // La lista flotante NO debe quitarle el foco (el cursor) al campo de texto,
        // si no el usuario no podria seguir escribiendo mientras ve las sugerencias.
        menuSugerencias.setFocusable(false);

        // DocumentListener = "escuchador" que avisa cada vez que cambia el TEXTO del campo.
        // Tiene 3 metodos (se inserto texto, se borro texto, cambio otra cosa) y Java nos obliga
        // a escribir los 3; en los tres hacemos lo mismo: actualizar las sugerencias.
        campoNombre.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                actualizarSugerencias();
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                actualizarSugerencias();
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
                actualizarSugerencias();
            }
        });
    }

    //la ventana nos entrega la lista de nombres cuando termina de descargarla
    public void setNombresDisponibles(List<String> nombres)
    {
        nombresDisponibles = nombres;
    }

    /**
     * Muestra una lista corta con los Pokemon cuyo nombre EMPIEZA con lo que el usuario
     * lleva escrito. Ejemplo: escribe "pik" -> aparece "pikachu", "pikipek".
     * Si no hay ninguno (o el campo esta vacio) la lista se esconde.
     */
    private void actualizarSugerencias()
    {
        if (escribiendoPorCodigo) return;

        menuSugerencias.setVisible(false);
        menuSugerencias.removeAll(); //vaciamos las sugerencias de la vez anterior

        String texto = campoNombre.getText().trim().toLowerCase();
        if (texto.isEmpty() || !campoNombre.isEnabled()) return;

        //primero juntamos TODOS los nombres que empiezan con el texto
        List<String> coincidencias = new ArrayList<>();
        for (String nombre : nombresDisponibles)
        {
            if (nombre.startsWith(texto))
            {
                coincidencias.add(nombre);
            }
        }

        // Los ordenamos del mas corto al mas largo: asi "pikachu" sale antes que
        // variantes largas como "pikachu-rock-star". (a, b) -> ... compara dos nombres por su largo.
        coincidencias.sort((a, b) -> a.length() - b.length());

        // y mostramos solo los primeros (maximo MAX_SUGERENCIAS)
        for (int i = 0; i < coincidencias.size() && i < MAX_SUGERENCIAS; i++)
        {
            agregarSugerencia(coincidencias.get(i));
        }

        // show(componente, x, y) abre la lista; con y = alto del campo queda justo debajo de el
        if (!coincidencias.isEmpty())
        {
            menuSugerencias.show(campoNombre, 0, campoNombre.getHeight());
        }
    }

    //crea una opcion de la lista; al hacer clic en ella se escribe el nombre y se carga el Pokemon
    private void agregarSugerencia(String nombre)
    {
        JMenuItem opcion = new JMenuItem(nombre);
        opcion.addActionListener(e ->
        {
            escribirEnCampo(nombre);
            cargarPorNombre();
        });
        menuSugerencias.add(opcion);
    }

    //escribe un texto en el campo SIN que se abra la lista de sugerencias
    private void escribirEnCampo(String texto)
    {
        escribiendoPorCodigo = true;
        campoNombre.setText(texto);
        escribiendoPorCodigo = false;
        menuSugerencias.setVisible(false);
    }

    //acomoda los componentes en el panel
    private void armarDisenio(String titulo, Color colorFondo)
    {
        // LAYOUTS = la forma en que Swing acomoda los componentes:
        //   BoxLayout(Y_AXIS) -> uno debajo del otro, en columna
        //   FlowLayout        -> uno al lado del otro, en fila
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        //color de fondo propio de cada jugador (azul claro / rojo claro)
        setBackground(colorFondo);

        //borde con titulo ("Jugador 1" / "Jugador 2"), con letra mas grande y oscura
        TitledBorder borde = BorderFactory.createTitledBorder(titulo);
        borde.setTitleFont(borde.getTitleFont().deriveFont(Font.BOLD, 15f));
        borde.setTitleColor(new Color(40, 40, 70));
        setBorder(borde);

        JPanel filaBusqueda = new JPanel(new FlowLayout());
        filaBusqueda.setOpaque(false); //transparente, igual que las demas filas
        filaBusqueda.add(campoNombre);
        filaBusqueda.add(botonCargar);
        filaBusqueda.add(botonAleatorio);

        etiquetaImagen.setPreferredSize(new Dimension(ANCHO_ZONA_IMAGEN, ALTO_ZONA_IMAGEN));
        // deriveFont = copia la fuente actual pero cambiandole estilo y tamaño
        etiquetaNombre.setFont(etiquetaNombre.getFont().deriveFont(Font.BOLD, 20f));
        etiquetaEstado.setForeground(Color.RED);

        barraVida.setStringPainted(true); //mostrar texto encima de la barra
        barraVida.setString("VIDA");
        barraVida.setForeground(new Color(46, 160, 67)); //verde (Rojo, Verde, Azul de 0 a 255)
        barraVida.setPreferredSize(new Dimension(280, 26)); //barra mas grande

        add(filaBusqueda);
        add(centrado(etiquetaImagen));
        add(centrado(etiquetaNombre));
        add(centrado(etiquetaTipos));
        add(centrado(etiquetaStats));
        add(centrado(barraVida));
        add(centrado(etiquetaEstado));
    }

    //mete un componente en un panelito con FlowLayout centrado, para que no quede pegado a la izquierda
    private JPanel centrado(JComponent componente)
    {
        JPanel fila = new JPanel(new FlowLayout(FlowLayout.CENTER));
        fila.setOpaque(false); //transparente: deja ver el color de fondo del panel de atras
        fila.add(componente);
        return fila;
    }

    private void cargarPorNombre()
    {
        String nombre = campoNombre.getText().trim();
        if (nombre.isEmpty())
        {
            etiquetaEstado.setText("Escribe un nombre primero");
            return;
        }
        cargarPokemon(nombre);
    }

    /**
     * Carga un Pokemon desde la API SIN CONGELAR LA VENTANA.
     *
     * EL PROBLEMA: Swing tiene un solo hilo (el EDT) que dibuja la ventana y atiende
     * los clics. Si hacemos la peticion a internet en ese hilo (como en clase), la
     * ventana se queda "pegada" hasta que llegue la respuesta: no se puede mover,
     * no responde a clics, no se redibuja. El taller lo prohibe.
     *
     * LA SOLUCION: SwingWorker. Es una clase de Swing que divide el trabajo en dos partes:
     *   1. doInBackground() -> se ejecuta en OTRO hilo (en segundo plano).
     *                          Aqui va lo que tarda: la peticion a internet.
     *                          Aqui NO se debe tocar ningun boton ni etiqueta.
     *   2. done()           -> se ejecuta en el hilo de Swing cuando el paso 1 termina.
     *                          Aqui SI actualizamos la pantalla con el resultado.
     *
     * @param nombre nombre a buscar, o null para uno aleatorio
     */
    private void cargarPokemon(String nombre)
    {
        menuSugerencias.setVisible(false); //al empezar a cargar, escondemos las sugerencias
        mostrarCargando(true);
        pokemon = null;
        alCambiarPokemon.run(); //avisamos a la ventana: mientras carga, ¡PELEAR! se deshabilita

        // SwingWorker<Pokemon, Void>:
        //   Pokemon -> el tipo de dato que devuelve doInBackground() (el resultado)
        //   Void    -> no usamos resultados parciales, por eso "Void" (nada)
        //
        // "new SwingWorker<...>() { ... }" es una CLASE ANONIMA: creamos un objeto
        // de una clase hija de SwingWorker sin darle nombre, escribiendo sus metodos
        // ahi mismo. (Es lo que hace IntelliJ con "new ActionListener() { ... }".)
        new SwingWorker<Pokemon, Void>()
        {
            private BufferedImage imagen;

            // @Override = "estoy reescribiendo un metodo que ya existe en la clase padre".
            // Si escribimos mal el nombre, Java nos avisa con un error.
            @Override
            protected Pokemon doInBackground() throws Exception
            {
                // >>> HILO EN SEGUNDO PLANO: aqui NO se toca nada de la ventana <<<
                Pokemon cargado = (nombre == null)
                        ? clienteApi.buscarPokemonAleatorio()
                        : clienteApi.buscarPokemon(nombre);
                try
                {
                    imagen = clienteApi.descargarImagen(cargado.getUrlImagen());
                }
                catch (IOException e)
                {
                    imagen = null; //si solo falla la imagen, igual mostramos el Pokemon
                }
                return cargado;
            }

            @Override
            protected void done()
            {
                // >>> HILO DE SWING: aqui SI actualizamos la pantalla <<<
                mostrarCargando(false);
                try
                {
                    // get() devuelve lo que retorno doInBackground().
                    // Si alla hubo un error, get() lanza ExecutionException
                    // con el error original guardado adentro.
                    mostrarPokemon(get(), imagen);
                }
                catch (ExecutionException e)
                {
                    // getCause() saca el error original (ej. PokemonNoEncontradoException)
                    mostrarError(e.getCause());
                }
                catch (InterruptedException e)
                {
                    //si interrumpen el hilo, lo dejamos marcado como interrumpido (buena practica)
                    Thread.currentThread().interrupt();
                }
                alCambiarPokemon.run(); //avisamos a la ventana que ya termino de cargar
            }
        }.execute(); // execute() ARRANCA el SwingWorker (sin esto no pasa nada)
    }

    //pone los datos del Pokemon en las etiquetas
    private void mostrarPokemon(Pokemon cargado, BufferedImage imagen)
    {
        pokemon = cargado;
        etiquetaEstado.setText(" ");
        escribirEnCampo(cargado.getNombre());

        if (imagen != null)
        {
            // la imagen original mide 96x96; la agrandamos a 160x160.
            // SCALE_REPLICATE agranda los pixeles sin dejarlos borrosos (ideal para pixel art).
            Image agrandada = imagen.getScaledInstance(TAMANIO_IMAGEN, TAMANIO_IMAGEN, Image.SCALE_REPLICATE);
            // ImageIcon envuelve la imagen para poder ponerla en un JLabel
            etiquetaImagen.setIcon(new ImageIcon(agrandada));
            etiquetaImagen.setText(null);
        }
        else
        {
            etiquetaImagen.setIcon(null);
            etiquetaImagen.setText("Sin imagen");
        }

        etiquetaNombre.setText("#" + cargado.getId() + " " + cargado.getNombreParaMostrar());
        // La API manda los tipos en ingles; los traducimos solo para MOSTRARLOS
        // (el combate sigue usando los nombres en ingles por dentro).
        List<String> tiposEnEspanol = new ArrayList<>();
        for (String tipo : cargado.getTipos())
        {
            tiposEnEspanol.add(traducirTipo(tipo));
        }
        // String.join(", ", lista) une los elementos con comas: ["fuego","volador"] -> "fuego, volador"
        etiquetaTipos.setText("Tipos: " + String.join(", ", tiposEnEspanol));
        etiquetaStats.setText("VIDA " + cargado.getHpMaximo()
                + " | ATAQUE " + cargado.getAtaque()
                + " | DEFENSA " + cargado.getDefensa()
                + " | VELOCIDAD " + cargado.getVelocidad());

        barraVida.setMaximum(cargado.getHpMaximo()); //la barra llena = vida maxima
        actualizarVida(cargado.getHpMaximo());
    }

    //muestra el error en rojo y en una ventanita emergente
    private void mostrarError(Throwable error)
    {
        String mensaje;
        // "instanceof" pregunta "¿este objeto es de este tipo?"
        // Preguntamos primero por PokemonNoEncontradoException porque ella TAMBIEN
        // es una IOException (por la herencia), y si no, entraria al segundo if.
        if (error instanceof PokemonNoEncontradoException)
        {
            mensaje = error.getMessage();
        }
        else if (error instanceof IOException)
        {
            mensaje = "Error de red: revisa tu conexión a internet";
        }
        else
        {
            mensaje = "Error inesperado: " + error.getMessage();
        }

        etiquetaEstado.setText(mensaje);
        etiquetaImagen.setIcon(null);
        etiquetaImagen.setText("Sin Pokémon");
        etiquetaNombre.setText("-");
        etiquetaTipos.setText("Tipos: -");
        etiquetaStats.setText(TEXTO_STATS_VACIO);
        barraVida.setValue(0);
        barraVida.setString("VIDA");
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    //actualiza la barra de vida y le cambia el color segun cuanta vida queda
    public void actualizarVida(int hp)
    {
        if (pokemon == null) return;

        barraVida.setValue(hp);
        barraVida.setString("VIDA " + hp + " / " + pokemon.getHpMaximo());

        // "(double)" convierte a decimal; si no, 30 / 60 en enteros daria 0 en vez de 0.5
        double porcentaje = (double) hp / pokemon.getHpMaximo();
        if (porcentaje > 0.5) barraVida.setForeground(new Color(46, 160, 67));       //verde
        else if (porcentaje > 0.2) barraVida.setForeground(new Color(230, 160, 0));  //naranja
        else barraVida.setForeground(new Color(210, 50, 50));                        //rojo
    }

    //pasa un tipo de la API (ingles) a espanol; si no lo conocemos, lo deja igual
    private String traducirTipo(String tipo)
    {
        switch (tipo)
        {
            case "normal": return "normal";
            case "fire": return "fuego";
            case "water": return "agua";
            case "grass": return "planta";
            case "electric": return "eléctrico";
            case "ice": return "hielo";
            case "fighting": return "lucha";
            case "poison": return "veneno";
            case "ground": return "tierra";
            case "flying": return "volador";
            case "psychic": return "psíquico";
            case "bug": return "bicho";
            case "rock": return "roca";
            case "ghost": return "fantasma";
            case "dragon": return "dragón";
            case "dark": return "siniestro";
            case "steel": return "acero";
            case "fairy": return "hada";
            default: return tipo;
        }
    }

    /**
     * ANIMACION DE ATAQUE: la imagen se lanza hacia el rival y vuelve a su lugar.
     *
     * @param haciaLaDerecha true para el Jugador 1 (el rival esta a su derecha),
     *                       false para el Jugador 2 (el rival esta a su izquierda)
     *
     * Usamos javax.swing.Timer: es un reloj que ejecuta un pedacito de codigo cada
     * cierto tiempo, y lo hace en el hilo de Swing (por eso es seguro tocar la pantalla).
     * Aqui se ejecuta cada 12 milisegundos y mueve la imagen 7 pixeles.
     */
    public void animarAtaque(boolean haciaLaDerecha)
    {
        int signo = haciaLaDerecha ? 1 : -1; //1 = derecha, -1 = izquierda

        // Los arreglos de 1 elemento son un truco: una lambda NO puede cambiar una variable
        // local normal, pero SI puede cambiar el contenido de un arreglo.
        int[] avance = {0};          //cuantos pixeles lleva avanzados
        boolean[] yendo = {true};    //true = yendo hacia el rival, false = regresando

        Timer reloj = new Timer(12, null);
        reloj.addActionListener(e ->
        {
            avance[0] += yendo[0] ? 7 : -7;

            if (yendo[0] && avance[0] >= DISTANCIA_ATAQUE)
            {
                yendo[0] = false; //llego al maximo: ahora regresa
            }
            else if (!yendo[0] && avance[0] <= 0)
            {
                avance[0] = 0;
                reloj.stop(); //volvio a su lugar: termina la animacion
            }

            desplazamiento = avance[0] * signo;
            etiquetaImagen.repaint(); //volver a dibujar la imagen en su nueva posicion
        });
        reloj.start();
    }

    private void mostrarCargando(boolean cargando)
    {
        habilitarControles(!cargando);
        if (cargando)
        {
            etiquetaEstado.setText(" ");
            etiquetaImagen.setIcon(null);
            etiquetaImagen.setText("Cargando...");
        }
    }

    //la ventana lo usa para bloquear Cargar/Aleatorio mientras hay un combate
    public void habilitarControles(boolean habilitar)
    {
        campoNombre.setEnabled(habilitar);
        botonCargar.setEnabled(habilitar);
        botonAleatorio.setEnabled(habilitar);
    }

    public Pokemon getPokemon()
    {
        return pokemon;
    }
}
