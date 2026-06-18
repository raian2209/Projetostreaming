package streaming.estruturas;

import java.util.Arrays;
import streaming.modelo.Filme;
import streaming.resultado.BuscaCategoriaResultado;
import streaming.resultado.BuscaFilmeResultado;

public class TabelaHashFilmes {

    private static class EntradaHash {
        private final int chave;
        private final Filme filme;
        private EntradaHash proxima;

        private EntradaHash(Filme filme) {
            this.chave = filme.getId();
            this.filme = filme;
        }
    }

    private final EntradaHash[] tabela;
    private int tamanho;

    public TabelaHashFilmes(int capacidade) {
        tabela = new EntradaHash[capacidade];
    }

    public void inserir(Filme filme) {
        int indice = calcularIndice(filme.getId());
        EntradaHash nova = new EntradaHash(filme);
        nova.proxima = tabela[indice];
        tabela[indice] = nova;
        tamanho++;
    }

    public BuscaFilmeResultado buscarPorId(int id) {
        int indice = calcularIndice(id);
        int comparacoes = 0;
        EntradaHash entrada = tabela[indice];

        while (entrada != null) {
            comparacoes++;

            if (entrada.chave == id) {
                return new BuscaFilmeResultado(entrada.filme, comparacoes);
            }

            entrada = entrada.proxima;
        }
        return new BuscaFilmeResultado(null, comparacoes);
    }

    public BuscaCategoriaResultado buscarPorCategoria(String categoria, int limite) {
        Filme[] encontrados = new Filme[limite];
        int[] comparacoesPorFilme = new int[limite];
        int exibidos = 0;
        int comparacoes = 0;
        int comparacoesNoUltimoAchado = 0;

        for (EntradaHash entrada : tabela) {
            while (entrada != null && exibidos < limite) {
                comparacoes++;
                if (entrada.filme.getCategoria().equalsIgnoreCase(categoria)) {
                    encontrados[exibidos] = entrada.filme;
                    comparacoesPorFilme[exibidos] = comparacoes - comparacoesNoUltimoAchado;
                    comparacoesNoUltimoAchado = comparacoes;
                    exibidos++;
                }
                entrada = entrada.proxima;
            }
            if (exibidos == limite) {
                break;
            }
        }

        return new BuscaCategoriaResultado(
                categoria,
                Arrays.copyOf(encontrados, exibidos),
                Arrays.copyOf(comparacoesPorFilme, exibidos),
                comparacoes);
    }

    private int calcularIndice(int id) {
        return Math.floorMod(id, tabela.length);
    }

    public int getCapacidade() {
        return tabela.length;
    }

    public int getTamanho() {
        return tamanho;
    }
}
