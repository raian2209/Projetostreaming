package streaming.resultado;

import streaming.modelo.Filme;

public class ConsultaResultado {
    public static final String TIPO_SOMENTE_CACHE = "somente cache";
    public static final String TIPO_SOMENTE_HASH = "somente hash";

    private final int id;
    private final String tipo;
    private final boolean hitCache;
    private final boolean encontrouServidor;
    private final int comparacoesCache;
    private final int comparacoesServidor;
    private final Filme filme;

    public ConsultaResultado(
            int id,
            String tipo,
            boolean hitCache,
            boolean encontrouServidor,
            int comparacoesCache,
            int comparacoesServidor,
            Filme filme) {
        this.id = id;
        this.tipo = tipo;
        this.hitCache = hitCache;
        this.encontrouServidor = encontrouServidor;
        this.comparacoesCache = comparacoesCache;
        this.comparacoesServidor = comparacoesServidor;
        this.filme = filme;
    }

    public void imprimirLinha() {
        String status;
        if (hitCache) {
            status = "HIT cache";
        } else if (TIPO_SOMENTE_CACHE.equals(tipo)) {
            status = "MISS cache / nao buscou no servidor";
        } else if (TIPO_SOMENTE_HASH.equals(tipo) && encontrouServidor) {
            status = "Encontrado no banco hash";
        } else if (TIPO_SOMENTE_HASH.equals(tipo)) {
            status = "Invalido no banco hash";
        } else if (encontrouServidor) {
            status = "MISS cache / encontrado no servidor";
        } else {
            status = "MISS cache / invalido no servidor";
        }

        String nome = filme == null ? "-" : filme.getNome();
        int total = comparacoesCache + comparacoesServidor;
        System.out.printf(
                "%-18s id=%4d | %-36s | comp cache=%3d | comp servidor=%4d | total=%4d | %s%n",
                tipo,
                id,
                status,
                comparacoesCache,
                comparacoesServidor,
                total,
                nome);
    }

    public boolean isHitCache() {
        return hitCache;
    }

    public int getComparacoesCache() {
        return comparacoesCache;
    }

    public int getComparacoesServidor() {
        return comparacoesServidor;
    }
}
