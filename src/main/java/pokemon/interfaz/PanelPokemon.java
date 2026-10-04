package pokemon.interfaz;

import pokemon.api.PokeApiClient;
import pokemon.api.PokemonNoEncontradoException;
import pokemon.modelo.Pokemon;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Panel de UN jugador: campo de texto, botones Load/Random, imagen,
 * datos y barra de vida. La ventana crea DOS de estos (uno por jugador).
 *
 * "extends JPanel" = HERENCIA: esta clase ES un JPanel (un panel de Swing),
 * con todo lo que tiene un JPanel, mas lo que nosotros le agregamos.
 */
public class PanelPokemon extends JPanel
{
    private static final int TAMANIO_IMAGEN = 160;

    private final PokeApiClient clienteApi;

    // Runnable = un "pedacito de codigo" guardado en una variable para ejecutarlo despues
    // con .run(). La ventana nos lo pasa para enterarse cada vez que cambia el Pokemon
    // (asi sabe si debe habilitar el boton Fight!).
    private final Runnable alCambiarPokemon;

    private final JTextField campoNombre = new JTextField(12);
    private final JButton botonCargar = new JButton("Load");
    private final JButton botonAleatorio = new JButton("Random");
    private final JLabel etiquetaImagen = new JLabel("Sin Pokémon", SwingConstants.CENTER);
    private final JLabel etiquetaNombre = new JLabel("-", SwingConstants.CENTER);
    private final JLabel etiquetaTipos = new JLabel("Tipos: -", SwingConstants.CENTER);
    private final JLabel etiquetaStats = new JLabel("HP - | ATK - | DEF - | SPD -", SwingConstants.CENTER);
    // JProgressBar = barra de progreso; la usamos como barra de vida
    private final JProgressBar barraVida = new JProgressBar();
    private final JLabel etiquetaEstado = new JLabel(" ", SwingConstants.CENTER); //para mensajes de error

    private Pokemon pokemon; //el Pokemon cargado (null si todavia no hay ninguno)

    public PanelPokemon(String titulo, PokeApiClient clienteApi, Runnable alCambiarPokemon)
    {
        this.clienteApi = clienteApi;
        this.alCambiarPokemon = alCambiarPokemon;
        armarDisenio(titulo);

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
    }

    //acomoda los componentes en el panel
    private void armarDisenio(String titulo)
    {
        // LAYOUTS = la forma en que Swing acomoda los componentes:
        //   BoxLayout(Y_AXIS) -> uno debajo del otro, en columna
        //   FlowLayout        -> uno al lado del otro, en fila
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        //borde con titulo ("Jugador 1" / "Jugador 2")
        setBorder(BorderFactory.createTitledBorder(titulo));

        JPanel filaBusqueda = new JPanel(new FlowLayout());
        filaBusqueda.add(campoNombre);
        filaBusqueda.add(botonCargar);
        filaBusqueda.add(botonAleatorio);

        etiquetaImagen.setPreferredSize(new Dimension(TAMANIO_IMAGEN, TAMANIO_IMAGEN));
        // deriveFont = copia la fuente actual pero cambiandole estilo y tamaño
        etiquetaNombre.setFont(etiquetaNombre.getFont().deriveFont(Font.BOLD, 20f));
        etiquetaEstado.setForeground(Color.RED);

        barraVida.setStringPainted(true); //mostrar texto encima de la barra
        barraVida.setString("HP");
        barraVida.setForeground(new Color(46, 160, 67)); //verde (Rojo, Verde, Azul de 0 a 255)

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
        mostrarCargando(true);
        pokemon = null;
        alCambiarPokemon.run(); //avisamos a la ventana: mientras carga, Fight! se deshabilita

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
        campoNombre.setText(cargado.getNombre());

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
        // String.join(", ", lista) une los elementos con comas: ["fire","flying"] -> "fire, flying"
        etiquetaTipos.setText("Tipos: " + String.join(", ", cargado.getTipos()));
        etiquetaStats.setText("HP " + cargado.getHpMaximo()
                + " | ATK " + cargado.getAtaque()
                + " | DEF " + cargado.getDefensa()
                + " | SPD " + cargado.getVelocidad());

        barraVida.setMaximum(cargado.getHpMaximo()); //la barra llena = HP maximo
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
        etiquetaStats.setText("HP - | ATK - | DEF - | SPD -");
        barraVida.setValue(0);
        barraVida.setString("HP");
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    //actualiza la barra de vida y le cambia el color segun cuanta vida queda
    public void actualizarVida(int hp)
    {
        if (pokemon == null) return;

        barraVida.setValue(hp);
        barraVida.setString(hp + " / " + pokemon.getHpMaximo());

        // "(double)" convierte a decimal; si no, 30 / 60 en enteros daria 0 en vez de 0.5
        double porcentaje = (double) hp / pokemon.getHpMaximo();
        if (porcentaje > 0.5) barraVida.setForeground(new Color(46, 160, 67));       //verde
        else if (porcentaje > 0.2) barraVida.setForeground(new Color(230, 160, 0));  //naranja
        else barraVida.setForeground(new Color(210, 50, 50));                        //rojo
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

    //la ventana lo usa para bloquear Load/Random mientras hay un combate
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
