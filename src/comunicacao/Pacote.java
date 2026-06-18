package streaming.comunicacao;

/**
 * Mensagem ja comprimida que trafega pelo canal entre cliente e servidor.
 *
 * Guarda os bytes compactados, a quantidade exata de bits validos e a raiz da
 * arvore de Huffman usada na codificacao (o "dicionario" necessario para
 * decodificar do outro lado). Tambem guarda o tamanho original em bytes para
 * permitir calcular a taxa de compressao.
 */
public class Pacote {
    private final byte[] dados;
    private final int totalBits;
    private final Huffman.NoHuffman arvore;
    private final int bytesOriginais;

    public Pacote(byte[] dados, int totalBits, Huffman.NoHuffman arvore, int bytesOriginais) {
        this.dados = dados;
        this.totalBits = totalBits;
        this.arvore = arvore;
        this.bytesOriginais = bytesOriginais;
    }

    public byte[] getDados() {
        return dados;
    }

    public int getTotalBits() {
        return totalBits;
    }

    public Huffman.NoHuffman getArvore() {
        return arvore;
    }

    public int getBytesOriginais() {
        return bytesOriginais;
    }

    public int getBytesComprimidos() {
        return dados.length;
    }
}
