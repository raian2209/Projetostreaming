package streaming.resultado;

/**
 * Resultado da traducao de um nome de filme para o seu ID.
 *
 * Guarda se o nome existe no indice, o ID correspondente e quantas comparacoes
 * foram feitas na tabela de traducao ate decidir.
 */
public class TraducaoResultado {
    private final boolean encontrado;
    private final int id;
    private final int comparacoes;

    public TraducaoResultado(boolean encontrado, int id, int comparacoes) {
        this.encontrado = encontrado;
        this.id = id;
        this.comparacoes = comparacoes;
    }

    public boolean encontrado() {
        return encontrado;
    }

    public int getId() {
        return id;
    }

    public int getComparacoes() {
        return comparacoes;
    }
}
