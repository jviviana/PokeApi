package pokemon.api;

import org.json.JSONArray;
import org.json.JSONObject;
import pokemon.modelo.Pokemon;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * CLIENTE DE LA API: se encarga SOLO de hablar con PokeAPI.
 * Hace la peticion HTTP (igual que en clase) y convierte el JSON en un objeto Pokemon.
 *
 * (El nombre "PokeApiClient" lo exige el taller; significa "cliente de PokeAPI".)
 *
 * IMPORTANTE: estos metodos tardan porque van a internet, por eso
 * NUNCA se deben llamar desde el hilo de la ventana (ver PanelPokemon).
 */
public class PokeApiClient
{
    // "private static final" = CONSTANTE:
    //   static -> pertenece a la clase, no a cada objeto (hay una sola copia)
    //   final  -> no se puede cambiar
    // Por convencion las constantes se escriben EN_MAYUSCULAS.
    private static final String URL_BASE = "https://pokeapi.co/api/v2/pokemon/";

    //PokeAPI tiene Pokemon con id del 1 al 1025
    private static final int ID_MAXIMO = 1025;

    // En clase usamos HttpClient.newHttpClient(). Aqui usamos newBuilder() para
    // poder configurarle un tiempo maximo de conexion (connectTimeout):
    // si en 10 segundos no conecta, lanza un error en vez de quedarse esperando para siempre.
    // Duration.ofSeconds(10) = "una duracion de 10 segundos".
    private final HttpClient cliente = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // Random = generador de numeros aleatorios
    private final Random aleatorio = new Random();

    /**
     * Busca un Pokemon por nombre o por numero.
     *
     * "throws IOException, InterruptedException" avisa que este metodo PUEDE
     * lanzar esos errores, y que quien lo llame debe manejarlos (con try/catch).
     * En clase los atrapabamos aqui mismo con try/catch; ahora los dejamos
     * "subir" para que la ventana decida que mensaje mostrar.
     */
    public Pokemon buscarPokemon(String nombreOId) throws IOException, InterruptedException
    {
        // trim() quita espacios al inicio y al final; toLowerCase() pasa a minusculas.
        // La API solo acepta nombres en minuscula ("Pikachu" fallaria, "pikachu" no).
        String busqueda = nombreOId.trim().toLowerCase();

        // matches(...) revisa si el texto cumple un patron (expresion regular).
        // "[a-z0-9-]+" = solo letras minusculas, numeros o guiones, al menos uno.
        // Si escriben "mr mime" (con espacio), URI.create fallaria con un error raro,
        // asi que mejor avisamos de una vez que no existe.
        if (!busqueda.matches("[a-z0-9-]+"))
        {
            // "throw" LANZA un error: el metodo se detiene aqui y el error
            // viaja hacia quien llamo a este metodo.
            throw new PokemonNoEncontradoException(nombreOId);
        }

        //igual que en clase, pero con .timeout: si la respuesta tarda mas de 15 s, falla
        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(URL_BASE + busqueda))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofString());

        // 200 = todo bien, 404 = no existe, otro = error del servidor
        if (respuesta.statusCode() == 404)
        {
            throw new PokemonNoEncontradoException(nombreOId);
        }
        if (respuesta.statusCode() != 200)
        {
            throw new IOException("PokeAPI respondió con código " + respuesta.statusCode());
        }

        return convertirJsonAPokemon(respuesta.body());
    }

    //busca un Pokemon con un numero aleatorio (para el boton Aleatorio)
    public Pokemon buscarPokemonAleatorio() throws IOException, InterruptedException
    {
        // nextInt(1025) da un numero entre 0 y 1024; le sumamos 1 -> entre 1 y 1025
        int idAleatorio = aleatorio.nextInt(ID_MAXIMO) + 1;

        // la API acepta el numero como texto: .../pokemon/25 es Pikachu
        return buscarPokemon(String.valueOf(idAleatorio));
    }

    /**
     * Trae la lista con los NOMBRES de todos los Pokemon (para las sugerencias del buscador).
     *
     * Es el mismo endpoint de siempre pero sin nombre al final y con ?limit=2000
     * ("dame hasta 2000"). El JSON viene asi:
     * { "count": 1350, "results": [ { "name": "bulbasaur", "url": "..." }, ... ] }
     * Solo nos interesa el "name" de cada elemento de "results".
     */
    public List<String> obtenerNombres() throws IOException, InterruptedException
    {
        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(URL_BASE + "?limit=2000"))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofString());
        if (respuesta.statusCode() != 200)
        {
            throw new IOException("PokeAPI respondió con código " + respuesta.statusCode());
        }

        List<String> nombres = new ArrayList<>();
        JSONArray resultados = new JSONObject(respuesta.body()).getJSONArray("results");
        for (int i = 0; i < resultados.length(); i++)
        {
            nombres.add(resultados.getJSONObject(i).getString("name"));
        }
        return nombres;
    }

    /**
     * Descarga la imagen del Pokemon desde su URL.
     * Devuelve null si no tiene imagen.
     *
     * BufferedImage = una imagen guardada en memoria, lista para mostrarse.
     */
    public BufferedImage descargarImagen(String urlImagen) throws IOException, InterruptedException
    {
        if (urlImagen == null)
        {
            return null;
        }

        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(urlImagen))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        // Diferencia con clase: antes pediamos la respuesta como texto (ofString).
        // Una imagen NO es texto, son bytes (numeros), por eso usamos ofByteArray().
        HttpResponse<byte[]> respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofByteArray());
        if (respuesta.statusCode() != 200)
        {
            return null;
        }

        // ByteArrayInputStream convierte los bytes en un "flujo" que se puede leer,
        // e ImageIO.read(...) lee ese flujo y lo convierte en una imagen (PNG, JPG...).
        return ImageIO.read(new ByteArrayInputStream(respuesta.body()));
    }

    /**
     * Convierte el texto JSON que responde la API en un objeto Pokemon.
     * Es lo mismo que haciamos en clase, pero en vez de imprimir con
     * System.out.println, guardamos los valores en variables.
     */
    private Pokemon convertirJsonAPokemon(String textoJson)
    {
        JSONObject json = new JSONObject(textoJson);

        int id = json.getInt("id");
        String nombre = json.getString("name");

        // En el JSON los tipos vienen asi:
        // "types": [ { "slot": 1, "type": { "name": "fire" } }, ... ]
        // getJSONArray -> el arreglo; getJSONObject(i) -> el elemento i (como en clase con el cast)
        List<String> tipos = new ArrayList<>();
        JSONArray arregloTipos = json.getJSONArray("types");
        for (int i = 0; i < arregloTipos.length(); i++)
        {
            JSONObject elemento = arregloTipos.getJSONObject(i);
            tipos.add(elemento.getJSONObject("type").getString("name"));
        }

        // Las estadisticas vienen asi:
        // "stats": [ { "base_stat": 45, "stat": { "name": "hp" } }, ... ]
        // Recorremos todas y guardamos solo las 4 que necesitamos.
        int hp = 0, ataque = 0, defensa = 0, velocidad = 0;
        JSONArray arregloStats = json.getJSONArray("stats");
        for (int i = 0; i < arregloStats.length(); i++)
        {
            JSONObject stat = arregloStats.getJSONObject(i);
            String nombreStat = stat.getJSONObject("stat").getString("name");
            int valor = stat.getInt("base_stat");

            // switch = un if/else if/else mas ordenado cuando se compara
            // una misma variable con varios valores.
            // "break" sale del switch (si no se pone, sigue al siguiente case).
            switch (nombreStat)
            {
                case "hp": hp = valor; break;
                case "attack": ataque = valor; break;
                case "defense": defensa = valor; break;
                case "speed": velocidad = valor; break;
                default: break; //special-attack y special-defense no se usan
            }
        }

        // En clase usamos imageJson.getString("front_default").
        // Problema: algunos Pokemon tienen la imagen en null y getString lanza error.
        // optString("front_default", null) significa: "dame el texto, y si no hay, dame null"
        // (opt = opcional). Asi no se rompe el programa.
        String urlImagen = json.getJSONObject("sprites").optString("front_default", null);

        return new Pokemon(id, nombre, tipos, hp, ataque, defensa, velocidad, urlImagen);
    }
}
