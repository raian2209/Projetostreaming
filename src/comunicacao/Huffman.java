package streaming.comunicacao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Compressao de dados por codificacao de Huffman.
 *
 * O algoritmo:
 *  1. conta a frequencia de cada caractere do texto;
 *  2. monta uma floresta de nos folha e combina sempre os dois de menor
 *     frequencia ate sobrar uma unica arvore (a arvore de Huffman);
 *  3. gera um codigo de bits para cada caractere: caminho da raiz ate a folha
 *     (0 = esquerda, 1 = direita). Caracteres frequentes ganham codigos curtos;
 *  4. concatena os codigos e empacota os bits em bytes.
 *
 * A decodificacao percorre a arvore bit a bit ate cair numa folha.
 *
 * Implementado sem bibliotecas externas; usa apenas estruturas da linguagem.
 */
public class Huffman {

    /** No da arvore de Huffman. Folhas guardam um caractere. */
    public static class NoHuffman {
        char caractere;
        int frequencia;
        NoHuffman esquerda;
        NoHuffman direita;

        NoHuffman(char caractere, int frequencia) {
            this.caractere = caractere;
            this.frequencia = frequencia;
        }

        NoHuffman(NoHuffman esquerda, NoHuffman direita) {
            this.frequencia = esquerda.frequencia + direita.frequencia;
            this.esquerda = esquerda;
            this.direita = direita;
        }

        boolean folha() {
            return esquerda == null && direita == null;
        }
    }

    private Huffman() {
    }

    public static Pacote comprimir(String texto) {
        if (texto == null) {
            texto = "";
        }
        int bytesOriginais = texto.getBytes().length;

        Map<Character, Integer> frequencias = contarFrequencias(texto);
        NoHuffman raiz = construirArvore(frequencias);

        Map<Character, String> codigos = new HashMap<>();
        gerarCodigos(raiz, "", codigos);

        StringBuilder bits = new StringBuilder();
        for (int i = 0; i < texto.length(); i++) {
            bits.append(codigos.get(texto.charAt(i)));
        }

        int totalBits = bits.length();
        byte[] dados = empacotarBits(bits);
        return new Pacote(dados, totalBits, raiz, bytesOriginais);
    }

    public static String descomprimir(Pacote pacote) {
        NoHuffman raiz = pacote.getArvore();
        if (raiz == null) {
            return "";
        }

        // Texto de um unico caractere distinto: a arvore e so a folha.
        if (raiz.folha()) {
            StringBuilder unico = new StringBuilder();
            for (int i = 0; i < pacote.getTotalBits(); i++) {
                unico.append(raiz.caractere);
            }
            return unico.toString();
        }

        StringBuilder texto = new StringBuilder();
        byte[] dados = pacote.getDados();
        NoHuffman atual = raiz;
        for (int i = 0; i < pacote.getTotalBits(); i++) {
            int bit = (dados[i / 8] >> (7 - (i % 8))) & 1;
            atual = bit == 0 ? atual.esquerda : atual.direita;
            if (atual.folha()) {
                texto.append(atual.caractere);
                atual = raiz;
            }
        }
        return texto.toString();
    }

    private static Map<Character, Integer> contarFrequencias(String texto) {
        Map<Character, Integer> frequencias = new HashMap<>();
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            frequencias.merge(c, 1, Integer::sum);
        }
        return frequencias;
    }

    private static NoHuffman construirArvore(Map<Character, Integer> frequencias) {
        List<NoHuffman> floresta = new ArrayList<>();
        for (Map.Entry<Character, Integer> entrada : frequencias.entrySet()) {
            floresta.add(new NoHuffman(entrada.getKey(), entrada.getValue()));
        }

        if (floresta.isEmpty()) {
            return null;
        }

        // Combina sempre os dois nos de menor frequencia ate sobrar um.
        while (floresta.size() > 1) {
            NoHuffman menor = removerMenor(floresta);
            NoHuffman segundoMenor = removerMenor(floresta);
            floresta.add(new NoHuffman(menor, segundoMenor));
        }
        return floresta.get(0);
    }

    private static NoHuffman removerMenor(List<NoHuffman> floresta) {
        int indiceMenor = 0;
        for (int i = 1; i < floresta.size(); i++) {
            if (floresta.get(i).frequencia < floresta.get(indiceMenor).frequencia) {
                indiceMenor = i;
            }
        }
        return floresta.remove(indiceMenor);
    }

    private static void gerarCodigos(NoHuffman no, String prefixo, Map<Character, String> codigos) {
        if (no == null) {
            return;
        }
        if (no.folha()) {
            // Texto com um unico simbolo distinto recebe o codigo "0".
            codigos.put(no.caractere, prefixo.isEmpty() ? "0" : prefixo);
            return;
        }
        gerarCodigos(no.esquerda, prefixo + "0", codigos);
        gerarCodigos(no.direita, prefixo + "1", codigos);
    }

    private static byte[] empacotarBits(StringBuilder bits) {
        int totalBytes = (bits.length() + 7) / 8;
        byte[] dados = new byte[totalBytes];
        for (int i = 0; i < bits.length(); i++) {
            if (bits.charAt(i) == '1') {
                dados[i / 8] |= (byte) (1 << (7 - (i % 8)));
            }
        }
        return dados;
    }
}
