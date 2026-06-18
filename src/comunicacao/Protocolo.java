package streaming.comunicacao;

import java.util.ArrayList;
import java.util.List;
import streaming.modelo.Filme;
import streaming.resultado.BuscaCategoriaResultado;

/**
 * Formato textual das mensagens trocadas pelo canal.
 *
 * As mensagens sao texto simples (otimo para demonstrar a compressao Huffman,
 * que trabalha sobre caracteres). Campos sao separados por '|' e registros de
 * uma lista por quebra de linha.
 *
 * A busca por nome e resolvida no cliente por uma tabela de traducao (nome->id);
 * com o id em maos, o protocolo de rede continua sendo por id, como no base.
 *
 * Requisicoes (cliente -> servidor):
 *   GET_INDICE                   (pede o indice nome->id para a tabela de traducao)
 *   GET_ID|8
 *   GET_CATEGORIA|Acao|20
 *   GET_REC|5
 *   GET_ALEATORIOS|20
 *
 * Respostas (servidor -> cliente):
 *   FILME|8|Matrix|1999|Acao|3   (filme + comparacoes da hash)
 *   VAZIO|3                      (nao encontrado + comparacoes da hash)
 *   INDICE|<n>\n nome|id\n ...    (indice nome->id)
 *   LISTA|<categoria ou REC>|<comparacoes>\n id|nome|ano|categoria\n ...
 */
public final class Protocolo {
    public static final String SEP_CAMPO = "|";
    public static final String SEP_REGISTRO = "\n";

    public static final String REQ_INDICE = "GET_INDICE";
    public static final String REQ_BUSCAR_ID = "GET_ID";
    public static final String REQ_BUSCAR_CATEGORIA = "GET_CATEGORIA";
    public static final String REQ_RECOMENDACOES = "GET_REC";
    public static final String REQ_ALEATORIOS = "GET_ALEATORIOS";

    public static final String RESP_FILME = "FILME";
    public static final String RESP_VAZIO = "VAZIO";
    public static final String RESP_LISTA = "LISTA";
    public static final String RESP_INDICE = "INDICE";

    private Protocolo() {
    }

    public static String requisicaoIndice() {
        return REQ_INDICE;
    }

    public static String requisicaoBuscarId(int id) {
        return REQ_BUSCAR_ID + SEP_CAMPO + id;
    }

    public static String requisicaoCategoria(String categoria, int limite) {
        return REQ_BUSCAR_CATEGORIA + SEP_CAMPO + categoria + SEP_CAMPO + limite;
    }

    public static String requisicaoRecomendacoes(int quantidade) {
        return REQ_RECOMENDACOES + SEP_CAMPO + quantidade;
    }

    public static String requisicaoAleatorios(int quantidade) {
        return REQ_ALEATORIOS + SEP_CAMPO + quantidade;
    }

    /** Resposta de uma busca por ID encontrada, carregando o numero de comparacoes da hash. */
    public static String respostaFilme(Filme filme, int comparacoes) {
        return RESP_FILME + SEP_CAMPO + filme.getId() + SEP_CAMPO + filme.getNome()
                + SEP_CAMPO + filme.getAno() + SEP_CAMPO + filme.getCategoria()
                + SEP_CAMPO + comparacoes;
    }

    /** Resposta de uma busca por nome nao encontrada, carregando o numero de comparacoes da hash. */
    public static String respostaVazio(int comparacoes) {
        return RESP_VAZIO + SEP_CAMPO + comparacoes;
    }

    /** Le um registro "id|nome|ano|categoria" (com ou sem o prefixo FILME). */
    public static Filme filmeDeTexto(String registro) {
        String[] campos = registro.split("\\" + SEP_CAMPO);
        int desloc = campos[0].equals(RESP_FILME) ? 1 : 0;
        int id = Integer.parseInt(campos[desloc]);
        String nome = campos[desloc + 1];
        int ano = Integer.parseInt(campos[desloc + 2]);
        String categoria = campos[desloc + 3];
        return new Filme(id, nome, ano, categoria);
    }

    public static List<Filme> listaDeTexto(String corpo) {
        List<Filme> filmes = new ArrayList<>();
        if (corpo == null || corpo.isEmpty()) {
            return filmes;
        }
        for (String registro : corpo.split(SEP_REGISTRO)) {
            if (!registro.trim().isEmpty()) {
                filmes.add(filmeDeTexto(registro));
            }
        }
        return filmes;
    }

    /** Indice nome->id: cabecalho INDICE|<n> + uma linha "nome|id" por filme. */
    public static String respostaIndice(List<Filme> filmes) {
        StringBuilder sb = new StringBuilder(RESP_INDICE + SEP_CAMPO + filmes.size());
        for (Filme filme : filmes) {
            sb.append(SEP_REGISTRO).append(filme.getNome()).append(SEP_CAMPO).append(filme.getId());
        }
        return sb.toString();
    }

    /** Resposta de recomendacao: cabecalho LISTA|REC|0 + um filme por linha. */
    public static String respostaRecomendacoes(List<Filme> filmes) {
        StringBuilder sb = new StringBuilder(RESP_LISTA + SEP_CAMPO + "REC" + SEP_CAMPO + 0);
        for (Filme filme : filmes) {
            sb.append(SEP_REGISTRO).append(filme.getId()).append(SEP_CAMPO).append(filme.getNome())
                    .append(SEP_CAMPO).append(filme.getAno()).append(SEP_CAMPO).append(filme.getCategoria());
        }
        return sb.toString();
    }

    /** Resposta de categoria: cabecalho LISTA|categoria|comparacoes + um filme por linha (com comparacoes). */
    public static String respostaCategoria(BuscaCategoriaResultado resultado) {
        StringBuilder sb = new StringBuilder(RESP_LISTA + SEP_CAMPO + resultado.getCategoria()
                + SEP_CAMPO + resultado.getComparacoes());
        Filme[] filmes = resultado.getFilmes();
        int[] comparacoes = resultado.getComparacoesPorFilme();
        for (int i = 0; i < filmes.length; i++) {
            sb.append(SEP_REGISTRO).append(filmes[i].getId()).append(SEP_CAMPO).append(filmes[i].getNome())
                    .append(SEP_CAMPO).append(filmes[i].getAno()).append(SEP_CAMPO).append(filmes[i].getCategoria())
                    .append(SEP_CAMPO).append(comparacoes[i]);
        }
        return sb.toString();
    }

    public static BuscaCategoriaResultado categoriaDeTexto(String mensagem) {
        String[] linhas = mensagem.split(SEP_REGISTRO);
        String[] cabecalho = linhas[0].split("\\" + SEP_CAMPO);
        String categoria = cabecalho[1];
        int comparacoes = Integer.parseInt(cabecalho[2]);

        int total = linhas.length - 1;
        Filme[] filmes = new Filme[total];
        int[] comparacoesPorFilme = new int[total];
        for (int i = 1; i < linhas.length; i++) {
            String[] campos = linhas[i].split("\\" + SEP_CAMPO);
            filmes[i - 1] = new Filme(
                    Integer.parseInt(campos[0]), campos[1], Integer.parseInt(campos[2]), campos[3]);
            comparacoesPorFilme[i - 1] = Integer.parseInt(campos[4]);
        }
        return new BuscaCategoriaResultado(categoria, filmes, comparacoesPorFilme, comparacoes);
    }

    /** Corpo de uma mensagem em lista (tudo depois da primeira linha). */
    public static String corpo(String mensagem) {
        int quebra = mensagem.indexOf(SEP_REGISTRO);
        return quebra < 0 ? "" : mensagem.substring(quebra + 1);
    }
}
