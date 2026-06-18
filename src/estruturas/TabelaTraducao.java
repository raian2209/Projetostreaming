package streaming.estruturas;

import streaming.modelo.Filme;
import streaming.resultado.TraducaoResultado;

/**
 * Tabela de traducao do nome do filme para o seu ID.
 *
 * E uma tabela hash propria (encadeamento exterior), com chave no nome
 * normalizado do filme (ver {@link Filme#chave(String)}) e valor no ID.
 *
 * O cliente recebe o indice nome->id do servidor (comprimido com Huffman) e
 * monta esta tabela. Quando o usuario pesquisa por nome, a tabela devolve o ID
 * e o sistema segue o fluxo normal de busca por ID (cache AVL -> servidor hash).
 *
 * A busca por nome fica tolerante a maiusculas/minusculas porque a chave e
 * sempre normalizada.
 */
public class TabelaTraducao {

    private static class Entrada {
        private final String chave;
        private final int id;
        private Entrada proxima;

        private Entrada(String chave, int id) {
            this.chave = chave;
            this.id = id;
        }
    }

    private final Entrada[] tabela;
    private int tamanho;

    public TabelaTraducao(int capacidade) {
        tabela = new Entrada[capacidade];
    }

    public void inserir(String nome, int id) {
        String chave = Filme.chave(nome);
        int indice = calcularIndice(chave);
        Entrada nova = new Entrada(chave, id);
        nova.proxima = tabela[indice];
        tabela[indice] = nova;
        tamanho++;
    }

    /** Traduz um nome para o ID, contando as comparacoes feitas na cadeia. */
    public TraducaoResultado traduzir(String nome) {
        String chave = Filme.chave(nome);
        int indice = calcularIndice(chave);
        int comparacoes = 0;
        Entrada entrada = tabela[indice];

        while (entrada != null) {
            comparacoes++;
            if (entrada.chave.equals(chave)) {
                return new TraducaoResultado(true, entrada.id, comparacoes);
            }
            entrada = entrada.proxima;
        }
        return new TraducaoResultado(false, -1, comparacoes);
    }

    private int calcularIndice(String chave) {
        return Math.floorMod(chave.hashCode(), tabela.length);
    }

    public int getTamanho() {
        return tamanho;
    }

    public int getCapacidade() {
        return tabela.length;
    }
}
