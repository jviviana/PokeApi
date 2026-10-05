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
 *   daño = 10 * (ATAQUE del atacante / DEFENSA del defensor)
 *             * aleatorio(0.85 a 1.0) * efectividad * (1.5 si es critico)
 *   daño = minimo 1 (para que siempre haya daño y el combate termine)
 *
 * ¿Por que esta formula?
 *   - ATAQUE / DEFENSA: si el ataque es el doble de la defensa, el golpe hace el doble;
 *     si la defensa es el doble del ataque, hace la mitad. Asi la defensa SI protege.
 *   - El 10 (POTENCIA) hace que un golpe normal quite mas o menos 1/4 o 1/5 de la vida,
 *     y la pelea dure varios turnos (como la vida de los Pokemon esta entre 40 y 100 casi siempre).
 *   - El azar es pequeño (85% a 100%, igual que en los juegos de Pokemon): un golpe puede
 *     variar un poco, pero las estadisticas deciden quien gana, no la suerte.
 *
 * ¿Por que NO la del ejemplo (ATK*random - DEF*random)? La probamos: con Charmander vs Squirtle
 * un golpe de Squirtle podia quitar de 3 a 62 de vida (Charmander tiene 39), asi que a veces
 * mataba de un golpe y a veces necesitaba 3, y Charmander ganaba 1 de cada 6 peleas aunque
 * todas sus estadisticas y su tipo estan en desventaja. Era casi pura suerte.
 */
public class Battle
{
    //constantes con las reglas del taller (ver PokeApiClient para que es "private static final")
    private static final double PROBABILIDAD_CRITICO = 0.10;   //10%
    private static final double MULTIPLICADOR_CRITICO = 1.5;
    private static final double SUPER_EFECTIVO = 1.3;
    private static final double POCO_EFECTIVO = 0.7;

    //numero que multiplica la formula para que cada golpe quite una parte razonable de la vida
    private static final double POTENCIA = 10;
    //el azar del daño va del 85% al 100% del golpe
    private static final double AZAR_MINIMO = 0.85;

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
        // 1) Proporcion ataque / defensa.
        //    "(double)" convierte a decimal: si no, 48 / 65 en enteros daria 0 en vez de 0.738
        double proporcion = (double) atacante.getAtaque() / defensor.getDefensa();

        // 2) Azar pequeño. nextDouble() devuelve un decimal al azar entre 0.0 y 1.0, entonces:
        //    0.85 + nextDouble() * 0.15  -> un numero entre 0.85 y 1.0
        double azar = AZAR_MINIMO + aleatorio.nextDouble() * (1 - AZAR_MINIMO);

        // 3) Golpe base. Ejemplo: Squirtle (ATK 48) contra Charmander (DEF 43):
        //    10 * (48 / 43) = 11.2  ->  con el azar queda entre 9.5 y 11.2
        double base = POTENCIA * proporcion * azar;

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
