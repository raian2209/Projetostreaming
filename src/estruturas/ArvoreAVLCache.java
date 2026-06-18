package streaming.estruturas;

import java.util.function.Function;
import streaming.resultado.BuscaCacheResultado;

public class ArvoreAVLCache<K extends Comparable<K>, V> {

    private class NoAVL {
        private K chave;
        private V valor;
        private long ordemEntrada;
        private int altura;
        private NoAVL esquerda;
        private NoAVL direita;

        private NoAVL(K chave, V valor, long ordemEntrada) {
            this.chave = chave;
            this.valor = valor;
            this.ordemEntrada = ordemEntrada;
            this.altura = 1;
        }
    }

    private class MaisAntigo {
        private NoAVL no;
    }

    private NoAVL raiz;
    private final int capacidade;
    private int tamanho;
    private long proximaOrdemEntrada;

    public ArvoreAVLCache(int capacidade) {
        this.capacidade = capacidade;
    }

    public BuscaCacheResultado<V> buscar(K chave) {
        int comparacoes = 0;
        NoAVL atual = raiz;
        while (atual != null) {
            comparacoes++;
            int cmp = chave.compareTo(atual.chave);
            if (cmp == 0) {
                return new BuscaCacheResultado<>(atual.valor, comparacoes);
            }
            if (cmp < 0) {
                atual = atual.esquerda;
            } else {
                atual = atual.direita;
            }
        }
        return new BuscaCacheResultado<>(null, comparacoes);
    }

    public void inserir(K chave, V valor) {
        NoAVL existente = localizar(chave);
        if (existente != null) {
            existente.valor = valor;
            return;
        }

        if (tamanho == capacidade) {
            NoAVL removido = encontrarMaisAntigo();
            if (removido != null) {
                raiz = remover(raiz, removido.chave);
                tamanho--;
            }
        }

        raiz = inserir(raiz, chave, valor, ++proximaOrdemEntrada);
        tamanho++;
    }

    public boolean contem(K chave) {
        return localizar(chave) != null;
    }

    public void imprimirEmOrdem(Function<V, String> formatador) {
        imprimirEmOrdem(raiz, formatador);
    }

    private void imprimirEmOrdem(NoAVL no, Function<V, String> formatador) {
        if (no == null) {
            return;
        }
        imprimirEmOrdem(no.esquerda, formatador);
        System.out.println(formatador.apply(no.valor) + " | ordem entrada: " + no.ordemEntrada);
        imprimirEmOrdem(no.direita, formatador);
    }

    private NoAVL localizar(K chave) {
        NoAVL atual = raiz;
        while (atual != null) {
            int cmp = chave.compareTo(atual.chave);
            if (cmp == 0) {
                return atual;
            }
            if (cmp < 0) {
                atual = atual.esquerda;
            } else {
                atual = atual.direita;
            }
        }
        return null;
    }

    private NoAVL inserir(NoAVL no, K chave, V valor, long ordemEntrada) {
        if (no == null) {
            return new NoAVL(chave, valor, ordemEntrada);
        }

        int cmp = chave.compareTo(no.chave);
        if (cmp < 0) {
            no.esquerda = inserir(no.esquerda, chave, valor, ordemEntrada);
        } else if (cmp > 0) {
            no.direita = inserir(no.direita, chave, valor, ordemEntrada);
        } else {
            no.valor = valor;
            return no;
        }

        atualizarAltura(no);
        return balancear(no);
    }

    private NoAVL remover(NoAVL no, K chave) {
        if (no == null) {
            return null;
        }

        int cmp = chave.compareTo(no.chave);
        if (cmp < 0) {
            no.esquerda = remover(no.esquerda, chave);
        } else if (cmp > 0) {
            no.direita = remover(no.direita, chave);
        } else {
            if (no.esquerda == null || no.direita == null) {
                NoAVL filho = no.esquerda != null ? no.esquerda : no.direita;
                return filho;
            }

            NoAVL sucessor = menorNo(no.direita);
            no.chave = sucessor.chave;
            no.valor = sucessor.valor;
            no.ordemEntrada = sucessor.ordemEntrada;
            no.direita = remover(no.direita, sucessor.chave);
        }

        atualizarAltura(no);
        return balancear(no);
    }

    private NoAVL encontrarMaisAntigo() {
        MaisAntigo resultado = new MaisAntigo();
        encontrarMaisAntigo(raiz, resultado);
        return resultado.no;
    }

    private void encontrarMaisAntigo(NoAVL no, MaisAntigo resultado) {
        if (no == null) {
            return;
        }

        if (resultado.no == null
                || no.ordemEntrada < resultado.no.ordemEntrada
                || (no.ordemEntrada == resultado.no.ordemEntrada
                        && no.chave.compareTo(resultado.no.chave) < 0)) {
            resultado.no = no;
        }
        encontrarMaisAntigo(no.esquerda, resultado);
        encontrarMaisAntigo(no.direita, resultado);
    }

    private NoAVL menorNo(NoAVL no) {
        NoAVL atual = no;
        while (atual.esquerda != null) {
            atual = atual.esquerda;
        }
        return atual;
    }

    private NoAVL balancear(NoAVL no) {
        int fator = fatorBalanceamento(no);

        if (fator > 1) {
            if (fatorBalanceamento(no.esquerda) < 0) {
                no.esquerda = rotacaoEsquerda(no.esquerda);
            }
            return rotacaoDireita(no);
        }

        if (fator < -1) {
            if (fatorBalanceamento(no.direita) > 0) {
                no.direita = rotacaoDireita(no.direita);
            }
            return rotacaoEsquerda(no);
        }

        return no;
    }

    private NoAVL rotacaoDireita(NoAVL noDesbalanceado) {
        NoAVL novaRaiz = noDesbalanceado.esquerda;
        NoAVL subArvoreMovida = novaRaiz.direita;

        novaRaiz.direita = noDesbalanceado;
        noDesbalanceado.esquerda = subArvoreMovida;

        atualizarAltura(noDesbalanceado);
        atualizarAltura(novaRaiz);

        return novaRaiz;
    }

    private NoAVL rotacaoEsquerda(NoAVL noDesbalanceado) {
        NoAVL novaRaiz = noDesbalanceado.direita;
        NoAVL subArvoreMovida = novaRaiz.esquerda;

        novaRaiz.esquerda = noDesbalanceado;
        noDesbalanceado.direita = subArvoreMovida;

        atualizarAltura(noDesbalanceado);
        atualizarAltura(novaRaiz);

        return novaRaiz;
    }

    private void atualizarAltura(NoAVL no) {
        no.altura = 1 + Math.max(altura(no.esquerda), altura(no.direita));
    }

    private int fatorBalanceamento(NoAVL no) {
        if (no == null) {
            return 0;
        }
        return altura(no.esquerda) - altura(no.direita);
    }

    private int altura(NoAVL no) {
        return no == null ? 0 : no.altura;
    }

    public int getTamanho() {
        return tamanho;
    }

    public int getCapacidade() {
        return capacidade;
    }
}
