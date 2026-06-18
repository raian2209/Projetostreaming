package streaming.estruturas;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import streaming.resultado.BuscaCacheResultado;

/**
 * Arvore Splay generica ordenada por chave, com capacidade maxima.
 *
 * Caracteristica principal: a cada acesso (busca ou insercao) o no acessado e
 * levado ate a raiz por rotacoes (operacao de "splay"). Cada no tambem guarda
 * o instante do seu ultimo acesso, o que define a recomendacao e a eviction.
 *
 * Recomendacao: os filmes com acesso mais recente vem primeiro.
 * Capacidade: a arvore guarda no maximo {@code capacidade} filmes; ao passar do
 * limite, remove o filme acessado ha mais tempo (eviction LRU).
 *
 * O cliente mantem uma arvore Splay com os filmes que ele mesmo consultou e o
 * servidor mantem outra com os filmes mais procurados no banco.
 */
public class ArvoreSplay<K extends Comparable<K>, V> {

    private class NoSplay {
        private K chave;
        private V valor;
        private long ultimoAcesso;
        private NoSplay esquerda;
        private NoSplay direita;

        private NoSplay(K chave, V valor, long ultimoAcesso) {
            this.chave = chave;
            this.valor = valor;
            this.ultimoAcesso = ultimoAcesso;
        }
    }

    private class MenosRecente {
        private NoSplay no;
    }

    private NoSplay raiz;
    private final int capacidade;
    private int tamanho;
    private long relogio;

    public ArvoreSplay(int capacidade) {
        this.capacidade = capacidade;
    }

    /**
     * Busca a chave e, se existir, faz o splay dela para a raiz e marca o acesso.
     * Conta as comparacoes feitas para localizar a chave.
     */
    public BuscaCacheResultado<V> buscar(K chave) {
        if (raiz == null) {
            return new BuscaCacheResultado<>(null, 0);
        }

        int comparacoes = contarComparacoes(chave);
        raiz = splay(raiz, chave);
        if (raiz.chave.compareTo(chave) == 0) {
            raiz.ultimoAcesso = ++relogio;
            return new BuscaCacheResultado<>(raiz.valor, comparacoes);
        }
        return new BuscaCacheResultado<>(null, comparacoes);
    }

    /**
     * Insere ou atualiza a chave, marca o acesso e, se passar da capacidade,
     * remove o filme acessado ha mais tempo (LRU).
     */
    public void inserir(K chave, V valor) {
        if (raiz == null) {
            raiz = new NoSplay(chave, valor, ++relogio);
            tamanho++;
            return;
        }

        raiz = splay(raiz, chave);
        int cmp = chave.compareTo(raiz.chave);
        if (cmp == 0) {
            raiz.valor = valor;
            raiz.ultimoAcesso = ++relogio;
            return;
        }

        NoSplay novo = new NoSplay(chave, valor, ++relogio);
        if (cmp < 0) {
            novo.direita = raiz;
            novo.esquerda = raiz.esquerda;
            raiz.esquerda = null;
        } else {
            novo.esquerda = raiz;
            novo.direita = raiz.direita;
            raiz.direita = null;
        }
        raiz = novo;
        tamanho++;

        if (capacidade > 0 && tamanho > capacidade) {
            removerMenosRecente();
        }
    }

    /**
     * Retorna ate {@code quantidade} filmes, do acesso mais recente para o mais
     * antigo. E a lista de recomendacao.
     */
    public List<V> recomendar(int quantidade) {
        List<NoSplay> nos = new ArrayList<>();
        coletar(raiz, nos);
        nos.sort(Comparator.comparingLong((NoSplay no) -> no.ultimoAcesso).reversed());

        List<V> recomendados = new ArrayList<>();
        for (int i = 0; i < nos.size() && i < quantidade; i++) {
            recomendados.add(nos.get(i).valor);
        }
        return recomendados;
    }

    private void coletar(NoSplay no, List<NoSplay> nos) {
        if (no == null) {
            return;
        }
        coletar(no.esquerda, nos);
        nos.add(no);
        coletar(no.direita, nos);
    }

    private void removerMenosRecente() {
        MenosRecente alvo = new MenosRecente();
        encontrarMenosRecente(raiz, alvo);
        if (alvo.no != null) {
            raiz = remover(raiz, alvo.no.chave);
            tamanho--;
        }
    }

    private void encontrarMenosRecente(NoSplay no, MenosRecente alvo) {
        if (no == null) {
            return;
        }
        if (alvo.no == null || no.ultimoAcesso < alvo.no.ultimoAcesso) {
            alvo.no = no;
        }
        encontrarMenosRecente(no.esquerda, alvo);
        encontrarMenosRecente(no.direita, alvo);
    }

    private int contarComparacoes(K chave) {
        int comparacoes = 0;
        NoSplay atual = raiz;
        while (atual != null) {
            comparacoes++;
            int cmp = chave.compareTo(atual.chave);
            if (cmp == 0) {
                break;
            }
            atual = cmp < 0 ? atual.esquerda : atual.direita;
        }
        return comparacoes;
    }

    /**
     * Splay top-down classico (Sleator e Tarjan): reorganiza a arvore trazendo
     * a chave (ou o ultimo no visitado no caminho dela) para a raiz.
     */
    private NoSplay splay(NoSplay no, K chave) {
        if (no == null) {
            return null;
        }

        NoSplay cabecalho = new NoSplay(null, null, 0);
        NoSplay maiorDaEsquerda = cabecalho;
        NoSplay menorDaDireita = cabecalho;

        while (true) {
            int cmp = chave.compareTo(no.chave);
            if (cmp < 0) {
                if (no.esquerda == null) {
                    break;
                }
                if (chave.compareTo(no.esquerda.chave) < 0) {
                    NoSplay filho = no.esquerda;
                    no.esquerda = filho.direita;
                    filho.direita = no;
                    no = filho;
                    if (no.esquerda == null) {
                        break;
                    }
                }
                menorDaDireita.esquerda = no;
                menorDaDireita = no;
                no = no.esquerda;
            } else if (cmp > 0) {
                if (no.direita == null) {
                    break;
                }
                if (chave.compareTo(no.direita.chave) > 0) {
                    NoSplay filho = no.direita;
                    no.direita = filho.esquerda;
                    filho.esquerda = no;
                    no = filho;
                    if (no.direita == null) {
                        break;
                    }
                }
                maiorDaEsquerda.direita = no;
                maiorDaEsquerda = no;
                no = no.direita;
            } else {
                break;
            }
        }

        maiorDaEsquerda.direita = no.esquerda;
        menorDaDireita.esquerda = no.direita;
        no.esquerda = cabecalho.direita;
        no.direita = cabecalho.esquerda;
        return no;
    }

    /** Remove uma chave: faz o splay dela para a raiz e junta as subarvores. */
    private NoSplay remover(NoSplay arvore, K chave) {
        if (arvore == null) {
            return null;
        }
        arvore = splay(arvore, chave);
        if (chave.compareTo(arvore.chave) != 0) {
            return arvore;
        }

        NoSplay esquerda = arvore.esquerda;
        NoSplay direita = arvore.direita;
        if (esquerda == null) {
            return direita;
        }
        NoSplay novaRaiz = splay(esquerda, chave);
        novaRaiz.direita = direita;
        return novaRaiz;
    }

    public int getTamanho() {
        return tamanho;
    }

    public int getCapacidade() {
        return capacidade;
    }
}
