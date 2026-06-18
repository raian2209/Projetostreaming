package streaming.cliente;

import java.util.List;
import streaming.comunicacao.CanalComunicacao;
import streaming.comunicacao.Pacote;
import streaming.comunicacao.Protocolo;
import streaming.estruturas.ArvoreAVLCache;
import streaming.estruturas.ArvoreSplay;
import streaming.estruturas.TabelaTraducao;
import streaming.modelo.Filme;
import streaming.resultado.BuscaCacheResultado;
import streaming.resultado.BuscaCategoriaResultado;
import streaming.resultado.BuscaFilmeResultado;
import streaming.resultado.ConsultaCategoriaResultado;
import streaming.resultado.ConsultaResultado;
import streaming.resultado.TraducaoResultado;
import streaming.servidor.ServidorStreaming;

/**
 * Lado cliente do sistema de streaming.
 *
 * Estruturas:
 *  - {@link TabelaTraducao}: traduz o nome do filme para o ID. E montada a
 *    partir do indice nome->id recebido do servidor (comprimido com Huffman).
 *  - {@link ArvoreAVLCache}: cache local de filmes por ID (eviction FIFO).
 *  - {@link ArvoreSplay}: arvore de recomendacoes do cliente (gosto pessoal).
 *
 * A pesquisa e feita pelo NOME: o cliente traduz o nome em ID pela tabela de
 * traducao e, com o ID, segue o fluxo normal de busca (cache AVL -> servidor
 * hash). Toda ida ao servidor trafega comprimida pelo {@link CanalComunicacao}.
 */
public class ClienteStreaming {
    private static final int TOTAL_CONSULTAS_AUTOMATICAS = 20;
    private static final int CAPACIDADE_TRADUCAO = 211;
    private static final int CAPACIDADE_RECOMENDACOES = 50;
    private static final String FLUXO_PADRAO = "AVL -> hash";

    private static final String[] CONSULTAS_INICIAIS = {
            "The Shawshank Redemption", "The Godfather", "The Dark Knight", "Pulp Fiction", "Forrest Gump",
            "Inception", "Fight Club", "The Matrix", "Goodfellas", "Interstellar",
            "Se7en", "Gladiator", "Joker", "The Departed", "The Lion King",
            "Psycho", "Whiplash", "Saving Private Ryan", "Star Wars", "The Usual Suspects"
    };

    private final String nome;
    private final ServidorStreaming servidor;
    private final CanalComunicacao canal;
    private final int capacidadeCache;
    private ArvoreAVLCache<Integer, Filme> cache;
    private final ArvoreSplay<Integer, Filme> recomendacoesCliente;
    private final TabelaTraducao traducao;

    public ClienteStreaming(String nome, ServidorStreaming servidor, CanalComunicacao canal, int capacidadeCache) {
        this.nome = nome;
        this.servidor = servidor;
        this.canal = canal;
        this.capacidadeCache = capacidadeCache;
        this.cache = new ArvoreAVLCache<>(capacidadeCache);
        this.recomendacoesCliente = new ArvoreSplay<>(CAPACIDADE_RECOMENDACOES);
        this.traducao = new TabelaTraducao(CAPACIDADE_TRADUCAO);
    }

    public String getNome() {
        return nome;
    }

    /** Pede ao servidor (pelo canal comprimido) o indice nome->id e monta a tabela de traducao. */
    public void sincronizarTabelaTraducao() {
        String resposta = enviar(Protocolo.requisicaoIndice());
        String corpo = Protocolo.corpo(resposta);
        if (!corpo.isEmpty()) {
            for (String linha : corpo.split(Protocolo.SEP_REGISTRO)) {
                if (linha.trim().isEmpty()) {
                    continue;
                }
                String[] campos = linha.split("\\" + Protocolo.SEP_CAMPO);
                traducao.inserir(campos[0], Integer.parseInt(campos[1]));
            }
        }
        System.out.println("Tabela de traducao recebida do servidor (comprimida): "
                + traducao.getTamanho() + " nomes mapeados para IDs.");
    }

    public void limparCache() {
        cache = new ArvoreAVLCache<>(capacidadeCache);
    }

    /** Envia um texto comprimido ao servidor e devolve a resposta ja decodificada. */
    private String enviar(String requisicao) {
        Pacote pacoteRequisicao = canal.comprimir(requisicao);
        Pacote pacoteResposta = servidor.atender(pacoteRequisicao);
        return canal.descomprimir(pacoteResposta);
    }

    /**
     * Pesquisa principal POR NOME: traduz o nome em ID e segue o fluxo normal.
     * Imprime primeiro a etapa de traducao e depois a linha da consulta por ID.
     */
    public void consultarPorNome(String nome) {
        TraducaoResultado traduzido = traducao.traduzir(nome);
        if (!traduzido.encontrado()) {
            System.out.printf("traducao nome->id  | nome='%s' | comp traducao=%2d | nome inexistente no indice%n",
                    nome, traduzido.getComparacoes());
            return;
        }
        System.out.printf("traducao nome->id  | nome='%s' -> id=%d | comp traducao=%2d%n",
                nome, traduzido.getId(), traduzido.getComparacoes());
        long[] antes = snapshotCanal();
        consultarFilme(traduzido.getId()).imprimirLinha();
        imprimirCompressaoDaPesquisa(antes);
    }

    public void testarSomenteCachePorNome(String nome) {
        TraducaoResultado traduzido = traducao.traduzir(nome);
        if (!traduzido.encontrado()) {
            System.out.printf("traducao nome->id  | nome='%s' | nome inexistente no indice%n", nome);
            return;
        }
        long[] antes = snapshotCanal();
        testarSomenteCache(traduzido.getId()).imprimirLinha();
        imprimirCompressaoDaPesquisa(antes);
    }

    public void testarSomenteBancoPorNome(String nome) {
        TraducaoResultado traduzido = traducao.traduzir(nome);
        if (!traduzido.encontrado()) {
            System.out.printf("traducao nome->id  | nome='%s' | nome inexistente no indice%n", nome);
            return;
        }
        long[] antes = snapshotCanal();
        testarSomenteBanco(traduzido.getId()).imprimirLinha();
        imprimirCompressaoDaPesquisa(antes);
    }

    /** Tira uma foto dos contadores do canal: [mensagens, bytesOriginais, bytesComprimidos]. */
    private long[] snapshotCanal() {
        return new long[] {canal.getMensagens(), canal.getBytesOriginais(), canal.getBytesComprimidos()};
    }

    /** Mostra a taxa de compressao das mensagens trocadas nesta pesquisa (desde o snapshot). */
    private void imprimirCompressaoDaPesquisa(long[] antes) {
        long mensagens = canal.getMensagens() - antes[0];
        long original = canal.getBytesOriginais() - antes[1];
        long comprimido = canal.getBytesComprimidos() - antes[2];
        if (mensagens == 0) {
            System.out.println("compressao Huffman | sem comunicacao com o servidor nesta pesquisa (HIT no cache).");
            return;
        }
        double taxa = original > 0 ? 100.0 * (1.0 - (double) comprimido / original) : 0.0;
        System.out.printf(
                "compressao Huffman | mensagens=%d | original=%d bytes -> comprimido=%d bytes | reducao=%.2f%%%n",
                mensagens, original, comprimido, taxa);
    }

    public ConsultaResultado consultarFilme(int id) {
        BuscaCacheResultado<Filme> cacheResultado = cache.buscar(id);
        if (cacheResultado.hit()) {
            registrarRecomendacao(cacheResultado.getValor());
            return criarResultado(
                    id, FLUXO_PADRAO, true, true,
                    cacheResultado, BuscaFilmeResultado.vazio(), cacheResultado.getValor());
        }

        String resposta = enviar(Protocolo.requisicaoBuscarId(id));
        BuscaFilmeResultado bancoResultado = interpretarRespostaFilme(resposta);
        Filme filme = bancoResultado.getFilme();
        if (filme != null) {
            cache.inserir(filme.getId(), filme);
            registrarRecomendacao(filme);
        }

        return criarResultado(
                id, FLUXO_PADRAO, false, bancoResultado.encontrou(),
                cacheResultado, bancoResultado, filme);
    }

    public ConsultaCategoriaResultado consultarCategoria(String categoria, int limite) {
        String resposta = enviar(Protocolo.requisicaoCategoria(categoria, limite));
        BuscaCategoriaResultado bancoResultado = Protocolo.categoriaDeTexto(resposta);

        int inseridosNoCache = 0;
        for (Filme filme : bancoResultado.getFilmes()) {
            if (!cache.contem(filme.getId())) {
                inseridosNoCache++;
            }
            cache.inserir(filme.getId(), filme);
            registrarRecomendacao(filme);
        }
        return new ConsultaCategoriaResultado(bancoResultado, inseridosNoCache);
    }

    /** Pede ao servidor (pelo canal comprimido) as recomendacoes globais dele. */
    public List<Filme> buscarRecomendacoesServidor(int quantidade) {
        String resposta = enviar(Protocolo.requisicaoRecomendacoes(quantidade));
        return Protocolo.listaDeTexto(Protocolo.corpo(resposta));
    }

    /** Recomendacoes locais deste cliente, lidas da sua arvore Splay. */
    public List<Filme> recomendacoesDoCliente(int quantidade) {
        return recomendacoesCliente.recomendar(quantidade);
    }

    public ConsultaResultado testarSomenteCache(int id) {
        BuscaCacheResultado<Filme> cacheResultado = cache.buscar(id);
        return criarResultado(
                id, ConsultaResultado.TIPO_SOMENTE_CACHE, cacheResultado.hit(), false,
                cacheResultado, BuscaFilmeResultado.vazio(), cacheResultado.getValor());
    }

    public ConsultaResultado testarSomenteBanco(int id) {
        String resposta = enviar(Protocolo.requisicaoBuscarId(id));
        BuscaFilmeResultado bancoResultado = interpretarRespostaFilme(resposta);
        return criarResultado(
                id, ConsultaResultado.TIPO_SOMENTE_HASH, false, bancoResultado.encontrou(),
                BuscaCacheResultado.vazio(), bancoResultado, bancoResultado.getFilme());
    }

    private BuscaFilmeResultado interpretarRespostaFilme(String resposta) {
        String[] campos = resposta.split("\\" + Protocolo.SEP_CAMPO);
        if (Protocolo.RESP_FILME.equals(campos[0])) {
            Filme filme = new Filme(
                    Integer.parseInt(campos[1]), campos[2], Integer.parseInt(campos[3]), campos[4]);
            int comparacoes = Integer.parseInt(campos[5]);
            return new BuscaFilmeResultado(filme, comparacoes);
        }
        int comparacoes = Integer.parseInt(campos[1]);
        return new BuscaFilmeResultado(null, comparacoes);
    }

    private void registrarRecomendacao(Filme filme) {
        if (filme != null) {
            recomendacoesCliente.inserir(filme.getId(), filme);
        }
    }

    private ConsultaResultado criarResultado(
            int id,
            String tipo,
            boolean hitCache,
            boolean encontrouServidor,
            BuscaCacheResultado<Filme> cacheResultado,
            BuscaFilmeResultado bancoResultado,
            Filme filme) {
        return new ConsultaResultado(
                id, tipo, hitCache, encontrouServidor,
                cacheResultado.getComparacoes(), bancoResultado.getComparacoes(), filme);
    }

    public void executarVinteConsultas() {
        limparCache();
        System.out.println("\n--- Metodo automatico: 20 consultas por nome ---");
        System.out.println("Cada nome e traduzido para ID na tabela de traducao e segue o fluxo AVL -> hash.");
        System.out.println("Criterio de eviction do cache: FIFO. O cache inicia vazio nesta rodada.");
        System.out.println("Toda ida ao servidor passa pelo canal comprimido com Huffman.\n");

        executarConsultas(CONSULTAS_INICIAIS);
    }

    public void executarVinteConsultasAleatorias() {
        System.out.println("\n--- Metodo automatico: 20 consultas aleatorias por nome ---");
        System.out.println("O servidor envia (comprimida) uma amostra aleatoria de filmes;");
        System.out.println("o cliente pesquisa cada um pelo nome, traduzindo para ID e seguindo o fluxo.\n");

        List<Filme> amostra = buscarFilmesAleatorios(TOTAL_CONSULTAS_AUTOMATICAS);
        String[] nomes = new String[amostra.size()];
        for (int i = 0; i < amostra.size(); i++) {
            nomes[i] = amostra.get(i).getNome();
        }
        executarConsultas(nomes);
    }

    private List<Filme> buscarFilmesAleatorios(int quantidade) {
        String resposta = enviar(Protocolo.requisicaoAleatorios(quantidade));
        return Protocolo.listaDeTexto(Protocolo.corpo(resposta));
    }

    private void executarConsultas(String[] nomes) {
        ResumoConsultas resumo = new ResumoConsultas();
        long[] antes = snapshotCanal();

        for (String nome : nomes) {
            TraducaoResultado traduzido = traducao.traduzir(nome);
            if (!traduzido.encontrado()) {
                System.out.printf("traducao nome->id  | nome='%s' | nome inexistente no indice%n", nome);
                continue;
            }
            ConsultaResultado resultado = consultarFilme(traduzido.getId());
            System.out.printf("nome='%-44s -> id=%3d | ", nome + "'", traduzido.getId());
            resultado.imprimirLinha();
            resumo.adicionar(resultado);
        }

        imprimirResumo(resumo, nomes.length);
        long original = canal.getBytesOriginais() - antes[1];
        long comprimido = canal.getBytesComprimidos() - antes[2];
        double taxa = original > 0 ? 100.0 * (1.0 - (double) comprimido / original) : 0.0;
        System.out.printf("Compressao Huffman do lote: %d mensagens | %d -> %d bytes | reducao=%.2f%%%n",
                canal.getMensagens() - antes[0], original, comprimido, taxa);
    }

    private void imprimirResumo(ResumoConsultas resumo, int totalConsultas) {
        System.out.println("\nResumo:");
        System.out.println("Consultas executadas: " + totalConsultas);
        System.out.println("Hits de cache: " + resumo.hitsCache + " de " + totalConsultas);
        System.out.println("Misses de cache: " + (totalConsultas - resumo.hitsCache));
        System.out.println("Comparacoes totais na AVL do cliente: " + resumo.comparacoesCache);
        System.out.println("Comparacoes no servidor apos miss do cache: " + resumo.comparacoesServidor);
        System.out.println("A pesquisa e por nome: o ID vem da tabela de traducao e o resto e o fluxo AVL -> hash.");
        System.out.println("O acesso ao servidor trafega comprimido com Huffman pelo canal.");
        System.out.println("Cada filme consultado tambem sobe na arvore Splay de recomendacoes.");
    }

    public void imprimirCache() {
        System.out.println("\nCache de filmes por ID: " + cache.getTamanho() + "/" + cache.getCapacidade());
        cache.imprimirEmOrdem(Filme::resumo);
    }

    private static class ResumoConsultas {
        private int hitsCache;
        private int comparacoesCache;
        private int comparacoesServidor;

        private void adicionar(ConsultaResultado resultado) {
            if (resultado.isHitCache()) {
                hitsCache++;
            }
            comparacoesCache += resultado.getComparacoesCache();
            comparacoesServidor += resultado.getComparacoesServidor();
        }
    }
}
