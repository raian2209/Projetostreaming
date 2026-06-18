package streaming.resultado;

import streaming.modelo.Filme;

public class BuscaCategoriaResultado {
    private final String categoria;
    private final Filme[] filmes;
    private final int[] comparacoesPorFilme;
    private final int comparacoes;

    public BuscaCategoriaResultado(
            String categoria,
            Filme[] filmes,
            int[] comparacoesPorFilme,
            int comparacoes) {
        this.categoria = categoria;
        this.filmes = filmes;
        this.comparacoesPorFilme = comparacoesPorFilme;
        this.comparacoes = comparacoes;
    }

    public String getCategoria() {
        return categoria;
    }

    public Filme[] getFilmes() {
        return filmes;
    }

    public int[] getComparacoesPorFilme() {
        return comparacoesPorFilme;
    }

    public int getComparacoes() {
        return comparacoes;
    }

    public int getExibidos() {
        return filmes.length;
    }

    public void imprimirFilmes() {
        if (filmes.length == 0) {
            System.out.println("Nenhum filme encontrado nessa categoria.");
            return;
        }
        for (int i = 0; i < filmes.length; i++) {
            System.out.printf(
                    "%-42s | comp tabela hash=%4d%n",
                    filmes[i].resumo(),
                    comparacoesPorFilme[i]);
        }
    }
}
