package pokemon.api;

import java.io.IOException;

/**
 * EXCEPCION PROPIA: un tipo de error creado por nosotros.
 *
 * ¿Para que? Para poder distinguir dos errores diferentes:
 *   - PokemonNoEncontradoException -> el Pokemon no existe (la API respondio 404)
 *   - IOException normal           -> fallo el internet
 * Asi la ventana puede mostrar un mensaje distinto para cada caso.
 *
 * "extends IOException" = HERENCIA: esta clase ES UN tipo de IOException,
 * asi que hereda todo lo que tiene IOException (como getMessage()).
 */
public class PokemonNoEncontradoException extends IOException
{
    public PokemonNoEncontradoException(String nombre)
    {
        // super(...) llama al constructor de la clase padre (IOException)
        // y le pasa el mensaje. Despues se puede leer con getMessage().
        super("Pokémon no encontrado: " + nombre);
    }
}
