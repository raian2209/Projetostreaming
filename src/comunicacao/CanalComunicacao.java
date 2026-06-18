package streaming.comunicacao;

/**
 * Canal de comunicacao entre cliente e servidor.
 *
 * Toda mensagem trocada entre os dois lados passa por aqui e e obrigatoriamente
 * comprimida com Huffman antes de "trafegar". Quem envia chama
 * {@link #comprimir(String)} e quem recebe chama {@link #descomprimir(Pacote)}.
 *
 * O canal acumula estatisticas (numero de mensagens, bytes originais e bytes
 * comprimidos) para permitir mostrar a taxa de compressao alcancada.
 */
public class CanalComunicacao {
    private long mensagensEnviadas;
    private long bytesOriginais;
    private long bytesComprimidos;

    public Pacote comprimir(String texto) {
        Pacote pacote = Huffman.comprimir(texto);
        mensagensEnviadas++;
        bytesOriginais += pacote.getBytesOriginais();
        bytesComprimidos += pacote.getBytesComprimidos();
        return pacote;
    }

    public String descomprimir(Pacote pacote) {
        return Huffman.descomprimir(pacote);
    }

    public long getMensagens() {
        return mensagensEnviadas;
    }

    public long getBytesOriginais() {
        return bytesOriginais;
    }

    public long getBytesComprimidos() {
        return bytesComprimidos;
    }

    public void imprimirEstatisticas() {
        System.out.println("\n--- Estatisticas de compressao (Huffman) ---");
        System.out.println("Mensagens trocadas pelo canal: " + mensagensEnviadas);
        System.out.println("Bytes originais (sem compressao): " + bytesOriginais);
        System.out.println("Bytes trafegados (comprimidos):   " + bytesComprimidos);
        if (bytesOriginais > 0) {
            double taxa = 100.0 * (1.0 - (double) bytesComprimidos / bytesOriginais);
            double razao = (double) bytesOriginais / Math.max(1, bytesComprimidos);
            System.out.printf("Reducao media: %.2f%% | razao de compressao: %.2f:1%n", taxa, razao);
        }
        System.out.println("Observacao: cada requisicao e cada resposta sao comprimidas separadamente.");
    }
}
