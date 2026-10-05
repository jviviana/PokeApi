package pokemon.modelo;

import java.util.ArrayList;
import java.util.List;

/**
 * MODELO de un Pokemon: una clase que solo guarda datos.
 * No sabe nada de internet ni de ventanas, solo representa "un Pokemon".
 *
 * (El nombre "Pokemon" lo exige el taller.)
 */
public class Pokemon
{
    // "final" significa que el valor se asigna UNA vez (en el constructor)
    // y despues ya no se puede cambiar. Los datos de un Pokemon no cambian
    // durante el combate, por eso son final.
    private final int id;
    private final String nombre;
    private final List<String> tipos;   //List = lista de tamaño variable (como un arreglo que crece)
    private final int hpMaximo;
    private final int ataque;
    private final int defensa;
    private final int velocidad;
    private final String urlImagen;

    // la vida actual (hpActual) es lo UNICO que cambia durante el combate, por eso NO es final
    private int hpActual;

    //constructor: se ejecuta al hacer "new Pokemon(...)"
    public Pokemon(int id, String nombre, List<String> tipos, int hpMaximo, int ataque, int defensa, int velocidad, String urlImagen)
    {
        this.id = id;
        this.nombre = nombre;
        //hacemos una copia de la lista para que nadie de afuera pueda modificar la nuestra
        this.tipos = new ArrayList<>(tipos);
        this.hpMaximo = hpMaximo;
        this.ataque = ataque;
        this.defensa = defensa;
        this.velocidad = velocidad;
        this.urlImagen = urlImagen;
        this.hpActual = hpMaximo; //empieza con la vida llena
    }

    //le resta el daño a la vida
    public void recibirDanio(int danio)
    {
        // Math.max(a, b) devuelve el mayor de los dos numeros.
        // Si hpActual - danio da negativo (ej: -5), Math.max(0, -5) devuelve 0.
        // Asi cumplimos la regla del taller: "el HP no puede ser negativo".
        hpActual = Math.max(0, hpActual - danio);
    }

    //true si el Pokemon ya no tiene vida
    public boolean estaDebilitado()
    {
        return hpActual == 0;
    }

    //llena la vida otra vez (para poder pelear de nuevo)
    public void restaurarHp()
    {
        hpActual = hpMaximo;
    }

    //nombre con la primera letra en mayuscula: "pikachu" -> "Pikachu"
    public String getNombreParaMostrar()
    {
        // substring(0, 1) = la primera letra; substring(1) = desde la segunda letra hasta el final
        return nombre.substring(0, 1).toUpperCase() + nombre.substring(1);
    }

    //el taller dice que la efectividad usa solo el PRIMER tipo
    public String getTipoPrincipal()
    {
        // Esto es el OPERADOR TERNARIO:   condicion ? valorSiEsVerdad : valorSiEsFalso
        // Es un if/else corto. Equivale a:
        //   if (tipos.isEmpty()) return ""; else return tipos.get(0);
        return tipos.isEmpty() ? "" : tipos.get(0);
    }

    // ---- getters: metodos para LEER los atributos privados desde otras clases ----
    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public List<String> getTipos() { return new ArrayList<>(tipos); }
    public int getHpMaximo() { return hpMaximo; }
    public int getAtaque() { return ataque; }
    public int getDefensa() { return defensa; }
    public int getVelocidad() { return velocidad; }
    public String getUrlImagen() { return urlImagen; }
    public int getHpActual() { return hpActual; }
}
