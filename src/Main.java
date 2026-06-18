package streaming;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import streaming.cliente.ClienteStreaming;
import streaming.comunicacao.CanalComunicacao;
import streaming.modelo.Filme;
import streaming.resultado.ConsultaCategoriaResultado;
import streaming.servidor.ServidorStreaming;

public class Main {
    private static final int TOTAL_FILMES = 1000;
    private static final int CAPACIDADE_CACHE = 50;
    private static final int CAPACIDADE_HASH = 211;
    private static final int QTD_RECOMENDACOES = 5;
    private static final String[] CAMINHOS_ARQUIVO_FILMES = {
            "filmes.csv",
            "Projetostreaming/filmes.csv"
    };

    public static void main(String[] args) {
        CanalComunicacao canal = new CanalComunicacao();
        ServidorStreaming servidor = new ServidorStreaming(CAPACIDADE_HASH, canal);
        servidor.carregarBaseDeArquivo(localizarArquivoFilmes());
        if (servidor.getTotalFilmes() != TOTAL_FILMES) {
            throw new IllegalStateException("A base deve ter exatamente " + TOTAL_FILMES + " filmes.");
        }

        System.out.println("Sistema de Streaming - Simulacao Cliente/Servidor com compressao Huffman");
        System.out.println("Base do servidor: " + servidor.getTotalFilmes() + " filmes reais na tabela hash.");
        System.out.println("Tabela hash do servidor: " + servidor.getCapacidadeHash()
                + " posicoes com encadeamento exterior.");
        System.out.println("Cada usuario tem seu proprio cache AVL e sua propria arvore Splay de recomendacao.");
        System.out.println("O servidor e compartilhado: sua arvore Splay reflete a popularidade global de todos.");
        System.out.println("Comunicacao: toda requisicao e resposta trafega comprimida com Huffman pelo canal.");

        Scanner scanner = new Scanner(System.in);
        executarLogin(servidor, canal, scanner);
        scanner.close();
        System.out.println("Programa encerrado.");
    }

    private static String localizarArquivoFilmes() {
        for (String caminho : CAMINHOS_ARQUIVO_FILMES) {
            File arquivo = new File(caminho);
            if (arquivo.isFile()) {
                return arquivo.getPath();
            }
        }
        throw new IllegalStateException("Arquivo filmes.csv nao encontrado.");
    }

    /**
     * Tela de login: permite entrar com varios usuarios. Cada usuario tem seu
     * proprio ClienteStreaming (cache e recomendacoes pessoais), preservado entre
     * sessoes. Aqui se escolhe o usuario, e ao deslogar volta-se para esta tela.
     */
    private static void executarLogin(ServidorStreaming servidor, CanalComunicacao canal, Scanner scanner) {
        Map<String, ClienteStreaming> usuarios = new LinkedHashMap<>();

        while (true) {
            System.out.println("\n=== Tela de login ===");
            if (usuarios.isEmpty()) {
                System.out.println("Nenhum usuario cadastrado ainda.");
            } else {
                System.out.println("Usuarios ja existentes: " + nomesUsuarios(usuarios));
            }
            System.out.println("Digite o nome do usuario para entrar (um nome novo cria um usuario).");
            System.out.print("Usuario (ou 0 para encerrar o programa): ");

            String entrada = scanner.nextLine().trim();
            if (entrada.equals("0")) {
                return;
            }
            if (entrada.isEmpty()) {
                System.out.println("Nome invalido.");
                continue;
            }

            String chave = entrada.toLowerCase();
            ClienteStreaming cliente = usuarios.get(chave);
            boolean novo = cliente == null;
            if (novo) {
                cliente = new ClienteStreaming(entrada, servidor, canal, CAPACIDADE_CACHE);
                cliente.sincronizarTabelaTraducao();
                usuarios.put(chave, cliente);
            }

            System.out.println("Logado como '" + cliente.getNome() + "'"
                    + (novo ? " (novo usuario)." : " (bem-vindo de volta)."));
            executarSessao(servidor, cliente, canal, scanner);
        }
    }

    private static String nomesUsuarios(Map<String, ClienteStreaming> usuarios) {
        StringBuilder sb = new StringBuilder();
        for (ClienteStreaming cliente : usuarios.values()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(cliente.getNome());
        }
        return sb.toString();
    }

    /** Menu da sessao de um usuario logado. Sair (0) faz logout e volta ao login. */
    private static void executarSessao(ServidorStreaming servidor, ClienteStreaming cliente, CanalComunicacao canal,
            Scanner scanner) {
        int opcao;

        do {
            System.out.println("\n--- Menu (usuario: " + cliente.getNome() + ") ---");
            System.out.println("1  - Listar categorias");
            System.out.println("2  - Listar 20 filmes de uma categoria");
            System.out.println("3  - Buscar filme por nome");
            System.out.println("4  - Buscar filme por nome (teste manual/invalido)");
            System.out.println("5  - Executar 20 consultas aleatorias (popula recomendacoes)");
            System.out.println("6  - Mostrar cache AVL");
            System.out.println("7  - Testar somente cache AVL por nome");
            System.out.println("8  - Testar somente banco hash por nome");
            System.out.println("9  - Minhas recomendacoes + recomendacoes do servidor");
            System.out.println("10 - Estatisticas de compressao (Huffman)");
            System.out.println("0  - Deslogar (trocar de usuario)");
            System.out.print("Opcao: ");

            opcao = lerInteiro(scanner);
            switch (opcao) {
                case 1:
                    servidor.imprimirCategorias();
                    break;
                case 2:
                    listarPorCategoria(scanner, servidor, cliente);
                    break;
                case 3:
                case 4:
                    buscar(scanner, cliente);
                    break;
                case 5:
                    cliente.executarVinteConsultasAleatorias();
                    break;
                case 6:
                    cliente.imprimirCache();
                    break;
                case 7:
                    testarSomenteCache(scanner, cliente);
                    break;
                case 8:
                    testarSomenteBanco(scanner, cliente);
                    break;
                case 9:
                    mostrarRecomendacoes(cliente);
                    break;
                case 10:
                    canal.imprimirEstatisticas();
                    break;
                case 0:
                    break;
                default:
                    System.out.println("Opcao invalida.");
            }
        } while (opcao != 0);

        System.out.println("Usuario '" + cliente.getNome() + "' deslogado.");
    }

    private static void listarPorCategoria(Scanner scanner, ServidorStreaming servidor, ClienteStreaming cliente) {
        servidor.imprimirCategorias();
        System.out.print("Escolha a categoria: ");
        int opcao = lerInteiro(scanner);
        String categoria = servidor.categoriaPorOpcao(opcao);
        if (categoria == null) {
            System.out.println("Categoria invalida.");
            return;
        }
        ConsultaCategoriaResultado resultado = cliente.consultarCategoria(categoria, 20);
        resultado.imprimir();
    }

    private static void buscar(Scanner scanner, ClienteStreaming cliente) {
        System.out.print("Nome do filme: ");
        String nome = lerTexto(scanner);
        cliente.consultarPorNome(nome);
    }

    private static void testarSomenteCache(Scanner scanner, ClienteStreaming cliente) {
        System.out.print("Nome do filme: ");
        String nome = lerTexto(scanner);
        cliente.testarSomenteCachePorNome(nome);
    }

    private static void testarSomenteBanco(Scanner scanner, ClienteStreaming cliente) {
        System.out.print("Nome do filme: ");
        String nome = lerTexto(scanner);
        cliente.testarSomenteBancoPorNome(nome);
    }

    /** Mostra pelo menos 5 recomendacoes pessoais do usuario e 5 do servidor. */
    private static void mostrarRecomendacoes(ClienteStreaming cliente) {
        System.out.println("\nRecomendacoes de '" + cliente.getNome() + "' (arvore Splay local):");
        imprimirRecomendacoes(cliente.recomendacoesDoCliente(QTD_RECOMENDACOES));

        System.out.println("\nRecomendacoes do servidor (Splay global, recebidas comprimidas):");
        imprimirRecomendacoes(cliente.buscarRecomendacoesServidor(QTD_RECOMENDACOES));
    }

    private static void imprimirRecomendacoes(List<Filme> filmes) {
        if (filmes.isEmpty()) {
            System.out.println("Nenhuma recomendacao disponivel ainda. Faca algumas consultas primeiro.");
            return;
        }
        int posicao = 1;
        for (Filme filme : filmes) {
            System.out.println(posicao + ". " + filme.resumo());
            posicao++;
        }
    }

    private static int lerInteiro(Scanner scanner) {
        while (true) {
            String linha = scanner.nextLine().trim();
            try {
                return Integer.parseInt(linha);
            } catch (NumberFormatException e) {
                System.out.print("Entrada invalida. Digite um numero inteiro: ");
            }
        }
    }

    private static String lerTexto(Scanner scanner) {
        return scanner.nextLine().trim();
    }
}
