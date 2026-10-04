package pokemon.combate;

/**
 * "BattleListener" = "Escuchador del combate". (El nombre lo exige el taller.)
 *
 * ¿Que es una INTERFACE?
 * Es una lista de metodos SIN codigo adentro, solo los nombres. Es como un
 * "contrato": cualquier clase que diga "implements BattleListener" esta
 * obligada a escribir el codigo de TODOS estos metodos.
 *
 * ¿Para que sirve aqui?
 * Battle (el combate) va a "avisar" lo que pasa llamando a estos metodos,
 * pero Battle NO sabe quien lo esta escuchando ni como lo va a mostrar.
 * En nuestro caso quien escucha es VentanaCombate, que lo dibuja en pantalla.
 * Pero podria ser cualquier otra clase (una que imprima en consola, por ejemplo)
 * sin cambiar ni una linea de Battle. Eso es "desacoplar la UI de la logica".
 *
 * Es la MISMA idea que ActionListener en los botones: el boton no sabe que
 * hace tu codigo, solo llama a actionPerformed() cuando lo presionan.
 *
 * Los 3 primeros metodos (y sus parametros) son exactamente los que pide el taller.
 */
public interface BattleListener
{
    /**
     * Se llama en cada ataque. "Turn" = turno.
     * @param attacker nombre del atacante
     * @param defender nombre del que recibe el golpe
     * @param damage   daño causado
     * @param critical true si fue golpe critico
     * @param modifier multiplicador por tipo: 1.3 super efectivo, 0.7 poco efectivo, 1.0 normal
     */
    void onTurn(String attacker, String defender, int damage, boolean critical, double modifier);

    /**
     * Se llama cada vez que cambia la vida de un Pokemon. "HpChanged" = cambio el HP.
     * @param pokemon  nombre del Pokemon
     * @param hpActual su vida actual
     */
    void onHpChanged(String pokemon, int hpActual);

    /**
     * Se llama cuando termina el combate. "BattleEnded" = combate terminado.
     * @param winner nombre del ganador
     */
    void onBattleEnded(String winner);

    /**
     * EXTRA (el taller dice "al menos" esos 3, asi que podemos agregar mas).
     * Se llama una vez al empezar, para avisar quien ataca primero.
     * @param primerAtacante nombre del que ataca primero
     */
    void onCombateIniciado(String primerAtacante);
}
