package streaming.resultado;

public class ConsultaCategoriaResultado {
    private final BuscaCategoriaResultado dados;
    private final int filmesInseridosNoCache;

    public ConsultaCategoriaResultado(BuscaCategoriaResultado dados, int filmesInseridosNoCache) {
        this.dados = dados;
        this.filmesInseridosNoCache = filmesInseridosNoCache;
    }

    public void imprimir() {
        dados.imprimirFilmes();
        System.out.printf(
                "hash -> AVL categoria='%s' | comp tabela hash=%4d | exibidos=%d | filmes inseridos na AVL=%d%n",
                dados.getCategoria(),
                dados.getComparacoes(),
                dados.getExibidos(),
                filmesInseridosNoCache);
    }
}
