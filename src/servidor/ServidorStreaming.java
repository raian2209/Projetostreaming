package streaming.servidor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import streaming.comunicacao.CanalComunicacao;
import streaming.comunicacao.Pacote;
import streaming.comunicacao.Protocolo;
import streaming.estruturas.ArvoreSplay;
import streaming.estruturas.TabelaHashFilmes;
import streaming.modelo.Filme;
import streaming.resultado.BuscaCategoriaResultado;
import streaming.resultado.BuscaFilmeResultado;

/**
 * Lado servidor do sistema de streaming.
 *
 * Estruturas:
 *  - {@link TabelaHashFilmes}: banco principal (catalogo) com encadeamento exterior.
 *  - {@link ArvoreSplay}: arvore de recomendacoes do servidor. Cada filme servido
 *    a qualquer cliente sobe para perto da raiz; assim os mais procurados no banco
 *    ficam no topo e formam a recomendacao global.
 *
 * O servidor nunca recebe texto puro do cliente: ele recebe {@link Pacote}
 * comprimidos com Huffman pelo {@link CanalComunicacao}, decodifica, processa e
 * devolve a resposta tambem comprimida.
 */
public class ServidorStreaming {
    private static final String[] CATEGORIAS = {
            "Acao", "Comedia", "Drama", "Ficcao", "Documentario",
            "Suspense", "Animacao", "Aventura", "Romance", "Terror"
    };

    private static final int CAPACIDADE_RECOMENDACOES = 50;

    private final TabelaHashFilmes tabelaHash;
    private final ArvoreSplay<Integer, Filme> recomendacoesServidor;
    private final List<Filme> catalogo;
    private final Random random;
    private final CanalComunicacao canal;

    public ServidorStreaming(int capacidadeHash, CanalComunicacao canal) {
        this.tabelaHash = new TabelaHashFilmes(capacidadeHash);
        this.recomendacoesServidor = new ArvoreSplay<>(CAPACIDADE_RECOMENDACOES);
        this.catalogo = new ArrayList<>();
        this.random = new Random();
        this.canal = canal;
    }

    public void carregarBaseDeArquivo(String caminhoArquivo) {
        try (BufferedReader leitor = new BufferedReader(new FileReader(caminhoArquivo))) {
            String linha;
            int numeroLinha = 0;
            while ((linha = leitor.readLine()) != null) {
                numeroLinha++;
                if (linha.trim().isEmpty() || linha.startsWith("#")) {
                    continue;
                }

                String[] campos = linha.split(";", 5);
                if (campos.length != 5) {
                    throw new IllegalArgumentException(
                            "Linha " + numeroLinha + " invalida no arquivo de filmes: " + linha);
                }

                int id = Integer.parseInt(campos[0].trim());
                String nome = campos[1].trim();
                int ano = Integer.parseInt(campos[3].trim());
                String categoria = campos[4].trim();
                Filme filme = new Filme(id, nome, ano, categoria);
                tabelaHash.inserir(filme);
                catalogo.add(filme);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Nao foi possivel carregar o arquivo de filmes: " + caminhoArquivo, e);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Arquivo de filmes possui id ou ano invalido.", e);
        }
    }

    /**
     * Ponto unico de entrada da comunicacao: recebe a requisicao comprimida,
     * decodifica com Huffman, processa e devolve a resposta tambem comprimida.
     */
    public Pacote atender(Pacote requisicao) {
        String textoRequisicao = canal.descomprimir(requisicao);
        String textoResposta = processar(textoRequisicao);
        return canal.comprimir(textoResposta);
    }

    private String processar(String requisicao) {
        String[] partes = requisicao.split("\\" + Protocolo.SEP_CAMPO);
        switch (partes[0]) {
            case Protocolo.REQ_INDICE:
                return processarIndice();
            case Protocolo.REQ_BUSCAR_ID:
                return processarBuscaId(Integer.parseInt(partes[1]));
            case Protocolo.REQ_BUSCAR_CATEGORIA:
                return processarCategoria(partes[1], Integer.parseInt(partes[2]));
            case Protocolo.REQ_RECOMENDACOES:
                return processarRecomendacoes(Integer.parseInt(partes[1]));
            case Protocolo.REQ_ALEATORIOS:
                return processarAleatorios(Integer.parseInt(partes[1]));
            default:
                return Protocolo.RESP_VAZIO + Protocolo.SEP_CAMPO + "0";
        }
    }

    private String processarIndice() {
        return Protocolo.respostaIndice(catalogo);
    }

    private String processarBuscaId(int id) {
        BuscaFilmeResultado resultado = tabelaHash.buscarPorId(id);
        Filme filme = resultado.getFilme();
        if (filme == null) {
            return Protocolo.respostaVazio(resultado.getComparacoes());
        }
        registrarRecomendacao(filme);
        return Protocolo.respostaFilme(filme, resultado.getComparacoes());
    }

    private String processarAleatorios(int quantidade) {
        List<Filme> copia = new ArrayList<>(catalogo);
        Collections.shuffle(copia, random);
        List<Filme> amostra = copia.subList(0, Math.min(quantidade, copia.size()));
        return Protocolo.respostaRecomendacoes(amostra);
    }

    private String processarCategoria(String categoria, int limite) {
        BuscaCategoriaResultado resultado = tabelaHash.buscarPorCategoria(categoria, limite);
        for (Filme filme : resultado.getFilmes()) {
            registrarRecomendacao(filme);
        }
        return Protocolo.respostaCategoria(resultado);
    }

    private String processarRecomendacoes(int quantidade) {
        List<Filme> recomendados = recomendacoesServidor.recomendar(quantidade);
        return Protocolo.respostaRecomendacoes(recomendados);
    }

    /** Cada filme servido sobe na arvore Splay do servidor (popularidade global). */
    private void registrarRecomendacao(Filme filme) {
        recomendacoesServidor.inserir(filme.getId(), filme);
    }

    public void imprimirCategorias() {
        for (int i = 0; i < CATEGORIAS.length; i++) {
            System.out.println((i + 1) + " - " + CATEGORIAS[i]);
        }
    }

    public String categoriaPorOpcao(int opcao) {
        if (opcao < 1 || opcao > CATEGORIAS.length) {
            return null;
        }
        return CATEGORIAS[opcao - 1];
    }

    public int getTotalFilmes() {
        return tabelaHash.getTamanho();
    }

    public int getCapacidadeHash() {
        return tabelaHash.getCapacidade();
    }

    public int getTamanhoRecomendacoes() {
        return recomendacoesServidor.getTamanho();
    }
}
