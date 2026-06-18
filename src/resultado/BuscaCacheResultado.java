package streaming.resultado;

public class BuscaCacheResultado<V> {
    private static final BuscaCacheResultado<Object> VAZIO = new BuscaCacheResultado<>(null, 0);

    private final V valor;
    private final int comparacoes;

    public BuscaCacheResultado(V valor, int comparacoes) {
        this.valor = valor;
        this.comparacoes = comparacoes;
    }

    @SuppressWarnings("unchecked")
    public static <V> BuscaCacheResultado<V> vazio() {
        return (BuscaCacheResultado<V>) VAZIO;
    }

    public V getValor() {
        return valor;
    }

    public int getComparacoes() {
        return comparacoes;
    }

    public boolean hit() {
        return valor != null;
    }
}
