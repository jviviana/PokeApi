package pokemon.combate;

import pokemon.modelo.Pokemon;

import java.util.Random;

/**
 * "Battle" = "COMBATE" en ingles. (El nombre lo exige el taller.)
 *
 * Esta clase tiene las REGLAS de la pelea: quien ataca primero, cuanto daño
 * hace cada golpe y cuando termina. No dibuja nada; solo avisa lo que pasa
 * a traves de un BattleListener (ver esa interface).
 *
 * Formula de daño elegida (el taller pide documentarla):
 *   base  = ATAQUE * aleatorio(0.5 a 1.0) - DEFENSA * aleatorio(0 a 0.5)
 *   base  = minimo 1 (para que siempre haya daño y el combate termine)
 *   daño  = base * efectividad * (1.5 si es critico)
 *
 * ¿Por que no la del ejemplo (ATK*random - DEF*random)? Porque muchas veces
 * da negativa, y un Pokemon con mucha defensa nunca recibiria daño.
 */
public class Battle
{
    //constantes con las reglas del taller (ver PokeApiClient para que es "private static final")
    private static final double PROBABILIDAD_CRITICO = 0.10;   //10%
    private static final double MULTIPLICADOR_CRITICO = 1.5;
    private static final double SUPER_EFECTIVO = 1.3;
    private static final double POCO_EFECTIVO = 0.7;

    //pausa entre turnos (en milisegundos) para que el usuario alcance a ver la pelea
    private static final long PAUSA_ENTRE_TURNOS_MS = 700;

    private final Pokemon pokemon1;
    private final Pokemon pokemon2;
    private final String nombre1;
    private final String nombre2;
    private final BattleListener escuchador;   //a quien le avisamos lo que pasa
    private final Random aleatorio = new Random();

    public Battle(Pokemon pokemon1, Pokemon pokemon2, BattleListener escuchador)
    {
        this.pokemon1 = pokemon1;
        this.pokemon2 = pokemon2;
        this.escuchador = escuchador;

        //si son el mismo Pokemon (ej. Pikachu vs Pikachu) les agregamos (J1)/(J2)
        //para que la ventana sepa cual barra de vida actualizar
        String n1 = pokemon1.getNombreParaMostrar();
        String n2 = pokemon2.getNombreParaMostrar();
        if (n1.equals(n2))
        {
            n1 = n1 + " (J1)";
            n2 = n2 + " (J2)";
        }
        this.nombre1 = n1;
        this.nombre2 = n2;
    }

    /**
     * Ejecuta TODO el combate hasta que haya un ganador.
     *
     * Como tiene pausas (Thread.sleep), debe llamarse desde un HILO APARTE,
     * nunca desde el hilo de la ventana, o la ventana se congelaria.
     *
     * "throws InterruptedException": Thread.sleep puede lanzar este error si
     * alguien interrumpe el hilo mientras duerme. Java nos obliga a declararlo.
     */
    public void iniciar() throws InterruptedException
    {
        pokemon1.restaurarHp();
        pokemon2.restaurarHp();
        escuchador.onHpChanged(nombre1, pokemon1.getHpActual());
        escuchador.onHpChanged(nombre2, pokemon2.getHpActual());

        Pokemon atacante = elegirPrimerAtacante();
        // operador ternario (ver Pokemon.getTipoPrincipal): si el atacante es el 1, defiende el 2, y al reves
        Pokemon defensor = (atacante == pokemon1) ? pokemon2 : pokemon1;
        escuchador.onCombateIniciado(nombreDe(atacante));

        // while (true) = repetir "para siempre"... hasta que el "return" de adentro lo detenga
        while (true)
        {
            // Thread.sleep(700) = detener ESTE hilo 700 milisegundos (0.7 segundos)
            Thread.sleep(PAUSA_ENTRE_TURNOS_MS);
            ejecutarTurno(atacante, defensor);

            if (defensor.estaDebilitado())
            {
                escuchador.onBattleEnded(nombreDe(atacante));
                return; //termina el metodo, y con eso el combate
            }

            //cambio de turno: intercambiamos atacante y defensor usando una variable temporal
            Pokemon temporal = atacante;
            atacante = defensor;
            defensor = temporal;
        }
    }

    //ataca primero el de mayor velocidad; si empatan, al azar
    private Pokemon elegirPrimerAtacante()
    {
        if (pokemon1.getVelocidad() > pokemon2.getVelocidad()) return pokemon1;
        if (pokemon2.getVelocidad() > pokemon1.getVelocidad()) return pokemon2;

        // nextBoolean() devuelve true o false al azar (50% y 50%), como lanzar una moneda
        return aleatorio.nextBoolean() ? pokemon1 : pokemon2;
    }

    //un ataque: calcula el daño, se lo aplica al defensor y avisa al escuchador
    private void ejecutarTurno(Pokemon atacante, Pokemon defensor)
    {
        // nextDouble() devuelve un decimal al azar entre 0.0 y 1.0
        //   0.5 + nextDouble() * 0.5  -> entre 0.5 y 1.0
        //   nextDouble() * 0.5        -> entre 0.0 y 0.5
        double base = atacante.getAtaque() * (0.5 + aleatorio.nextDouble() * 0.5)
                - defensor.getDefensa() * (aleatorio.nextDouble() * 0.5);
        base = Math.max(1, base); //nunca menos de 1

        //10% de probabilidad: el numero al azar (0 a 1) es menor que 0.10 una de cada 10 veces
        boolean esCritico = aleatorio.nextDouble() < PROBABILIDAD_CRITICO;
        double efectividad = calcularEfectividad(atacante.getTipoPrincipal(), defensor.getTipoPrincipal());

        double total = base * efectividad * (esCritico ? MULTIPLICADOR_CRITICO : 1.0);

        // Math.round redondea (23.6 -> 24) y devuelve un long;
        // "(int)" es un CAST: convierte ese numero a int.
        int danio = Math.max(1, (int) Math.round(total));

        defensor.recibirDanio(danio);

        //avisamos lo que paso; Battle no sabe ni le importa como se va a mostrar
        escuchador.onTurn(nombreDe(atacante), nombreDe(defensor), danio, esCritico, efectividad);
        escuchador.onHpChanged(nombreDe(defensor), defensor.getHpActual());
    }

    //Agua > Fuego, Fuego > Planta, Planta > Agua (x1.3); al reves x0.7; cualquier otro x1.0
    //(la API devuelve los tipos en ingles: water, fire, grass)
    private double calcularEfectividad(String tipoAtaque, String tipoDefensa)
    {
        if (leGanaA(tipoAtaque, tipoDefensa)) return SUPER_EFECTIVO;
        if (leGanaA(tipoDefensa, tipoAtaque)) return POCO_EFECTIVO;
        return 1.0;
    }

    //true si el tipo "a" le gana al tipo "b"
    private boolean leGanaA(String a, String b)
    {
        return (a.equals("water") && b.equals("fire"))
                || (a.equals("fire") && b.equals("grass"))
                || (a.equals("grass") && b.equals("water"));
    }

    //devuelve el nombre que usamos en los avisos para ese Pokemon
    private String nombreDe(Pokemon pokemon)
    {
        return pokemon == pokemon1 ? nombre1 : nombre2;
    }

    public String getNombre1() { return nombre1; }
    public String getNombre2() { return nombre2; }
}
