# Projetostreaming - Streaming com Recomendacoes (Splay) e Compressao (Huffman)

Projeto em Java puro, sem framework, derivado do projeto base `projetoBanco`. Simula um sistema de streaming cliente-servidor com estruturas de dados implementadas manualmente.

Em relacao ao projeto base, este projeto acrescenta:

- **Pesquisa por NOME do filme**, resolvida por uma **tabela de traducao** (nome -> ID). Com o ID em maos, o sistema executa o fluxo normal de busca por ID.
- **Catalogo de 1000 filmes reais** (dataset IMDB Top 1000) no lugar dos dados gerados automaticamente.
- **Duas arvores Splay de recomendacao**: uma no cliente (gosto pessoal) e outra no servidor (popularidade global).
- **Compressao de dados com Huffman em toda a comunicacao** entre cliente e servidor: cada requisicao e cada resposta trafega comprimida.

O que foi mantido do projeto base:

- Cache do cliente em **arvore AVL** por ID (eviction FIFO).
- Banco do servidor em **tabela hash** por ID com encadeamento exterior.

## Visao Geral

A pesquisa e feita pelo nome do filme, mas internamente tudo continua por ID:

```text
Cliente recebe o NOME do filme
-> traduz o nome em ID pela tabela de traducao (nome -> ID)
   -> se o nome nao existe no indice: avisa e encerra a consulta
-> com o ID, segue o fluxo normal:
   -> procura primeiro no cache AVL (por ID)
   -> se encontrar: HIT cache, nao acessa o servidor
   -> se nao encontrar: MISS cache, envia requisicao COMPRIMIDA por ID ao servidor
      -> o servidor busca na tabela hash (por ID)
      -> se encontrar: devolve o filme COMPRIMIDO, o cliente insere no cache AVL
-> em qualquer acerto, o filme tambem sobe na arvore Splay de recomendacoes
```

A tabela de traducao e montada no inicio: o cliente pede ao servidor o indice
nome->id, que chega **comprimido com Huffman**. Nenhuma ida ao servidor acontece
sem passar pelo canal comprimido.

## Como Executar

Entre na pasta do projeto:

```bash
cd /home/raimundo/Projetos/projetoED2/Projetostreaming
```

Forma mais simples (script que compila e executa):

```bash
./executar.sh
```

Manualmente: o codigo esta organizado em pacotes (veja "Organizacao do Codigo"), entao a compilacao percorre as subpastas de `src`:

```bash
javac -d out $(find src -name "*.java")
```

Execute pela classe principal (no pacote `streaming`):

```bash
java -cp out streaming.Main
```

Tambem funciona a partir da pasta raiz `projetoED2`:

```bash
javac -d out $(find Projetostreaming/src -name "*.java")
java -cp out streaming.Main
```

## Arquivo de Dados

O catalogo fica em `filmes.csv`, com `1000` filmes **reais** extraidos do dataset
IMDB Top 1000 (titulos como The Shawshank Redemption, The Godfather, Pulp Fiction,
Inception, Interstellar). Os titulos estao no idioma original. Formato de cada linha:

```text
id;nome;sinopse;ano;categoria
```

Os nomes sao unicos e usados como chave de busca. A pesquisa por nome e tolerante a maiusculas/minusculas (a chave de traducao e normalizada). Os generos do IMDB foram mapeados para as 10 categorias do sistema; como o IMDB Top 1000 praticamente nao tem documentarios, a categoria `Documentario` pode aparecer vazia.

## Configuracoes Principais

As constantes ficam em `Main.java`:

```java
private static final int TOTAL_FILMES = 1000;
private static final int CAPACIDADE_CACHE = 50;
private static final int CAPACIDADE_HASH = 211;
private static final int QTD_RECOMENDACOES = 10;
```

A capacidade da hash (`211`) e bem menor que os `1000` filmes, entao varios filmes
caem no mesmo balde e sao ligados por encadeamento exterior (cerca de 5 por balde).
O cache AVL (`50`) e menor que o catalogo para demonstrar a eviction FIFO. Cada
arvore Splay de recomendacao tem capacidade `50` com eviction LRU.

## Estruturas de Dados

### Tabela de Traducao nome -> ID (cliente) - NOVO

Implementada em `TabelaTraducao.java`. E uma tabela hash propria (encadeamento exterior) com chave no nome normalizado do filme e valor no ID.

O cliente recebe o indice nome->id do servidor (comprimido) e monta esta tabela. Quando o usuario pesquisa por nome, a tabela devolve o ID e o sistema segue o fluxo normal de busca por ID. Assim, a pesquisa muda para nome sem alterar a hash do servidor nem o cache AVL, que continuam por ID. O retorno (`traduzir`) tambem conta as comparacoes feitas na cadeia.

### Cache AVL (cliente)

Implementado em `ArvoreAVLCache.java`. Arvore AVL generica ordenada pela chave (ID), busca `O(log n)`, capacidade `50` e eviction FIFO (o primeiro filme que entrou e o primeiro removido quando o cache lota).

### Tabela Hash (servidor)

Implementada em `TabelaHashFilmes.java`. Banco principal do servidor. Indice por `Math.floorMod(id, capacidade)` e colisoes tratadas por encadeamento exterior.

### Arvore Splay (recomendacoes) - NOVO

Implementada em `ArvoreSplay.java`, generica e usada nos dois lados.

Caracteristica central: a cada acesso (busca ou insercao) o no acessado e levado ate a raiz por rotacoes (operacao de *splay*). Assim, os filmes acessados mais recentemente ficam proximos da raiz.

Isso e usado como recomendacao:

- **Splay do cliente**: cada filme que o cliente consulta sobe na sua arvore. Os filmes do topo formam a recomendacao **pessoal** daquele cliente.
- **Splay do servidor**: cada filme servido a qualquer cliente sobe na arvore do servidor. Os filmes do topo formam a recomendacao **global** (mais procurados no banco).

Cada arvore Splay tem **capacidade 50**. Todo no guarda o instante do seu ultimo acesso; ao passar de 50 filmes, a arvore remove o filme acessado ha mais tempo (eviction LRU). A recomendacao (`recomendar(n)`) devolve ate `n` filmes do acesso mais recente para o mais antigo.

O algoritmo de splay usado e o *top-down* classico de Sleator e Tarjan; a remocao faz o splay da chave ate a raiz e junta as subarvores.

### Compressao Huffman (comunicacao) - NOVO

Implementada em `Huffman.java`.

1. conta a frequencia de cada caractere da mensagem;
2. combina sempre os dois nos de menor frequencia ate sobrar uma unica arvore;
3. gera um codigo de bits por caractere (caminho da raiz ate a folha; `0` esquerda, `1` direita) - caracteres frequentes ganham codigos curtos;
4. empacota os bits em bytes.

A decodificacao percorre a arvore bit a bit ate cair em uma folha.

Tudo sem bibliotecas externas: apenas estruturas da linguagem (`HashMap`, `ArrayList`).

## Comunicacao Comprimida

Toda troca entre cliente e servidor passa pelo `CanalComunicacao` e e obrigatoriamente comprimida:

```text
Cliente monta texto da requisicao
-> canal.comprimir(texto)  => Pacote (bytes + arvore de Huffman + total de bits)
-> servidor.atender(Pacote)
     -> canal.descomprimir(Pacote)  => texto
     -> processa (hash / splay)
     -> canal.comprimir(resposta)   => Pacote
-> canal.descomprimir(Pacote)  => texto da resposta
```

Cada requisicao e cada resposta sao comprimidas separadamente. O canal acumula estatisticas (mensagens, bytes originais, bytes comprimidos, taxa de reducao), visiveis na opcao `11` do menu.

O formato textual das mensagens esta em `Protocolo.java`. Exemplos:

```text
GET_INDICE                      (requisicao do indice nome->id no inicio)
INDICE|100\nMatrix|8\n...        (resposta: indice nome->id)
GET_ID|8                        (requisicao por id, depois da traducao)
FILME|8|Matrix|1999|Acao|2      (resposta: filme + comparacoes da hash)
VAZIO|2                         (resposta: id inexistente + comparacoes)
GET_CATEGORIA|Acao|20           (requisicao)
GET_REC|10                      (requisicao de recomendacoes do servidor)
GET_ALEATORIOS|20               (requisicao de amostra aleatoria para o teste)
LISTA|...                       (resposta em lista)
```

## Multiplos Usuarios (login)

O programa comeca numa **tela de login**. Voce digita um nome de usuario:

- nome novo: cria um usuario com cache AVL e arvore Splay de recomendacao proprios;
- nome ja usado: volta ao mesmo usuario, com seu cache e recomendacoes preservados.

Cada usuario tem recomendacoes **pessoais** (a sua arvore Splay local, alimentada pelas suas proprias consultas). O **servidor e compartilhado**: a arvore Splay do servidor acumula a popularidade global de todos os usuarios. Por isso, dois usuarios diferentes veem recomendacoes pessoais diferentes, mas a recomendacao do servidor reflete a atividade de todos.

Dentro da sessao, a opcao `0` faz **logout** e volta para a tela de login (da para entrar com outro usuario). Na tela de login, `0` **encerra o programa**.

## Menu do Programa

Tela de login:

```text
Usuario (ou 0 para encerrar o programa): <nome>
```

Menu da sessao (usuario logado):

```text
1  - Listar categorias
2  - Listar 20 filmes de uma categoria
3  - Buscar filme por nome
4  - Buscar filme por nome (teste manual/invalido)
5  - Executar 20 consultas aleatorias (popula recomendacoes)
6  - Mostrar cache AVL
7  - Testar somente cache AVL por nome
8  - Testar somente banco hash por nome
9  - Minhas recomendacoes + recomendacoes do servidor
10 - Estatisticas de compressao (Huffman)
0  - Deslogar (trocar de usuario)
```

Nas opcoes `3`, `4`, `7` e `8` voce digita o **nome** do filme (ex.: `Inception`). O cliente traduz o nome em ID pela tabela de traducao e mostra essa etapa antes da consulta.

Toda pesquisa imprime tambem a **taxa de compressao daquela pesquisa**: as mensagens trocadas com o servidor (requisicao + resposta), os bytes originais, os bytes comprimidos e a reducao percentual. Exemplo:

```text
compressao Huffman | mensagens=2 | original=39 bytes -> comprimido=19 bytes | reducao=51.28%
```

Se a pesquisa der HIT no cache (ou se o nome nao existir), nao ha comunicacao com o servidor e a linha avisa isso. A opcao `5` mostra a compressao agregada do lote, e a opcao `10` mostra o acumulado de toda a sessao.

A opcao `9` mostra de uma vez pelo menos 5 recomendacoes pessoais do usuario (Splay local) e pelo menos 5 do servidor (Splay global, recebidas comprimidas). Para ter recomendacoes, faca antes algumas consultas (opcoes `3` ou `5`).

- **Opcao 9**: mostra os filmes do topo da arvore Splay do cliente (recomendacao pessoal). Reflete o que este cliente consultou.
- **Opcao 10**: envia uma requisicao comprimida `GET_REC` ao servidor e exibe os filmes do topo da arvore Splay global (mais procurados no banco).
- **Opcao 11**: imprime as estatisticas de compressao acumuladas pelo canal.

## Organizacao do Codigo

O codigo esta separado por responsabilidade, em pacotes sob a base `streaming`:

```text
src/
├── Main.java                         streaming
├── modelo/
│   └── Filme.java                    streaming.modelo
├── estruturas/                       streaming.estruturas
│   ├── ArvoreAVLCache.java           (cache do cliente, AVL)
│   ├── ArvoreSplay.java              (recomendacoes, Splay)
│   ├── TabelaHashFilmes.java         (banco do servidor)
│   └── TabelaTraducao.java           (nome -> ID)
├── comunicacao/                      streaming.comunicacao
│   ├── Huffman.java
│   ├── Pacote.java
│   ├── CanalComunicacao.java
│   └── Protocolo.java
├── servidor/
│   └── ServidorStreaming.java        streaming.servidor
├── cliente/
│   └── ClienteStreaming.java         streaming.cliente
└── resultado/                        streaming.resultado
    ├── ConsultaResultado.java
    ├── ConsultaCategoriaResultado.java
    ├── BuscaCacheResultado.java
    ├── BuscaFilmeResultado.java
    ├── BuscaCategoriaResultado.java
    └── TraducaoResultado.java
```

- `modelo`: a entidade `Filme`.
- `estruturas`: as estruturas de dados implementadas manualmente (o nucleo do trabalho).
- `comunicacao`: compressao Huffman, canal e formato das mensagens.
- `servidor` / `cliente`: os dois lados da simulacao.
- `resultado`: objetos de retorno das operacoes (DTOs).

## Classes do Projeto

| Classe | Papel |
| --- | --- |
| `Main` | Ponto de entrada. Monta canal/servidor, controla a tela de login multiusuario (um `ClienteStreaming` por usuario) e o menu da sessao. |
| `Filme` | Modelo de dados (id, nome, ano, categoria). |
| `ServidorStreaming` | Servidor: tabela hash + Splay de recomendacao global. Recebe e responde pacotes comprimidos. |
| `ClienteStreaming` | Cliente: tabela de traducao + cache AVL + Splay de recomendacao pessoal. Fala com o servidor so por pacotes comprimidos. |
| `TabelaTraducao` | Tabela hash nome -> ID usada para a pesquisa por nome. |
| `TraducaoResultado` | Retorno da traducao (encontrado, id, comparacoes). |
| `TabelaHashFilmes` | Banco do servidor (hash por ID com encadeamento exterior). |
| `ArvoreAVLCache` | Cache do cliente (AVL generica, FIFO). |
| `ArvoreSplay` | Arvore Splay generica usada nas recomendacoes dos dois lados. |
| `Huffman` | Compressao e descompressao por codificacao de Huffman. |
| `Pacote` | Mensagem ja comprimida que trafega no canal (bytes + arvore + bits). |
| `CanalComunicacao` | Comprime/descomprime toda mensagem e acumula estatisticas. |
| `Protocolo` | Formato textual das requisicoes e respostas. |
| `ConsultaResultado` | Resultado de uma consulta por ID (status, comparacoes, filme). |
| `ConsultaCategoriaResultado` | Resultado de uma consulta por categoria. |
| `BuscaCacheResultado` | Retorno generico de busca em arvore (valor + comparacoes). |
| `BuscaFilmeResultado` | Retorno de busca no servidor (filme + comparacoes). |
| `BuscaCategoriaResultado` | Filmes de uma categoria + comparacoes. |

## Pontos Importantes Para Apresentacao

- Estruturas implementadas manualmente, em Java puro, sem framework.
- Varios usuarios: cada um com cache e recomendacoes proprios; o servidor (e sua recomendacao global) e compartilhado. Da para deslogar e trocar de usuario.
- A pesquisa e por nome, mas resolvida por uma tabela de traducao nome->id; o fluxo interno (cache e banco) continua por ID, como no projeto base.
- O catalogo usa 1000 filmes reais do dataset IMDB Top 1000 no lugar dos dados gerados automaticamente.
- Cada arvore Splay de recomendacao tem capacidade 50 com eviction LRU.
- O cache do cliente continua em AVL; o banco do servidor continua em tabela hash.
- Foram acrescentadas duas arvores Splay de recomendacao: uma no cliente e outra no servidor.
- A arvore Splay aproxima da raiz os filmes acessados mais recentemente, o que vira recomendacao natural.
- Toda comunicacao entre cliente e servidor e comprimida com Huffman; nenhuma ida ao servidor escapa do canal comprimido.
- A integridade e garantida: as comparacoes da hash, por exemplo, atravessam o canal comprimido e voltam intactas.
