package streaming.modelo;

public class Filme {
    private final int id;
    private final String nome;
    private final int ano;
    private final String categoria;

    public Filme(int id, String nome, int ano, String categoria) {
        this.id = id;
        this.nome = nome;
        this.ano = ano;
        this.categoria = categoria;
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public int getAno() {
        return ano;
    }

    public String getCategoria() {
        return categoria;
    }

    /** Chave de busca normalizada (sem espacos nas pontas, minuscula) usada nas estruturas. */
    public String getChave() {
        return chave(nome);
    }

    /** Normaliza um nome para servir de chave: a busca por nome e tolerante a caixa. */
    public static String chave(String nome) {
        return nome.trim().toLowerCase();
    }

    public String resumo() {
        return "[" + id + "] " + nome + " (" + ano + ") - " + categoria;
    }
}
