package streaming.resultado;

import streaming.modelo.Filme;

public class BuscaFilmeResultado {
    private static final BuscaFilmeResultado VAZIO = new BuscaFilmeResultado(null, 0);

    private final Filme filme;
    private final int comparacoes;

    public BuscaFilmeResultado(Filme filme, int comparacoes) {
        this.filme = filme;
        this.comparacoes = comparacoes;
    }

    public static BuscaFilmeResultado vazio() {
        return VAZIO;
    }

    public Filme getFilme() {
        return filme;
    }

    public int getComparacoes() {
        return comparacoes;
    }

    public boolean encontrou() {
        return filme != null;
    }
}
