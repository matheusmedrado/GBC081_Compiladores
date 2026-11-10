# Ambiente e decisões de escopo

Construção de Compiladores (GBC081), Checkpoint 4. Grupo Matheus-Christian.

Este texto foi fechado antes de conferir a passada. Os arquivos em `test/esperado/id-*.txt` são este desenho, não uma saída copiada da execução. O programa da seção 1 é `programs/sombreamento.txt`. A numeração é a do arquivo: a linha 1 é `int x;`, e a linha 2 está em branco.

A passada não confere tipos. Ela só diz, para cada uso, qual declaração aquele nome enxerga. O tipo que o Checkpoint 5 vai ler em `i.decl.type` fica no símbolo, vazio.

## 1. O ambiente em cada ponto

```
 1  int x;
 2
 3  void f(int n) {
 4      int x;
 5      x = n;
 6      {
 7          int x;
 8          x = n;
 9          {
10              int y;
11              y = x;
12          }
13      }
14      x = n;
15  }
16
17  void main() {
18      x = 1;
19  }
```

Há dois blocos aninhados (linhas 6 e 9) dentro de uma função com parâmetro. O nome `x` é sombreado três vezes. A linha 14 usa `x` depois que os dois blocos fecharam. A linha 18 usa `x` depois que a função fechou.

O ambiente é uma pilha. O nível 0 é o global. Em cada figura, o nível mais alto é o mais interno. `x@4` é o `x` declarado na linha 4.

**Depois da linha 1**, `int x`.

```
0  x@1 variável
```

**Depois de declarar `f`, antes de abrir o escopo da função.** O nome do procedimento entra no global. O parâmetro ainda não existe.

```
0  x@1 variável    f@3 procedimento
```

**Depois do parâmetro `n`.** Abriu o escopo da função (nível 1). O corpo não abre outro: os parâmetros e as declarações soltas do corpo dividem este nível.

```
1  n@3 parâmetro
0  x@1 variável    f@3 procedimento
```

**Depois da linha 4**, `int x`. Este `x` sombreia o global. Os dois continuam na pilha; o de dentro é o que um uso acha.

```
1  n@3 parâmetro    x@4 variável
0  x@1 variável     f@3 procedimento
```

**Uso na linha 5**, `x = n`. O `x` da esquerda é o da linha 4. O `n` é o parâmetro. O literal não é nome.

- `x` → `x@4` variável
- `n` → `n@3` parâmetro

**Depois da linha 7**, `int x`, já dentro do bloco aberto na linha 6.

```
2  x@7 variável
1  n@3 parâmetro    x@4 variável
0  x@1 variável     f@3 procedimento
```

**Uso na linha 8**, `x = n`.

- `x` → `x@7` variável
- `n` → `n@3` parâmetro (não está no bloco; a busca desce até o nível 1)

**Depois da linha 10**, `int y`, já dentro do bloco aberto na linha 9.

```
3  y@10 variável
2  x@7 variável
1  n@3 parâmetro    x@4 variável
0  x@1 variável     f@3 procedimento
```

**Uso na linha 11**, `y = x`.

- `y` → `y@10` variável
- `x` → `x@7` variável

**Depois de fechar o bloco (linha 12).** `y@10` deixa de existir. O resto fica.

```
2  x@7 variável
1  n@3 parâmetro    x@4 variável
0  x@1 variável     f@3 procedimento
```

**Depois de fechar o bloco (linha 13).** `x@7` deixa de existir.

```
1  n@3 parâmetro    x@4 variável
0  x@1 variável     f@3 procedimento
```

**Uso na linha 14**, `x = n`, depois do fechamento dos dois blocos. O `x` voltou a ser o da função, não o da linha 7. `y` aqui seria "não declarado"; o programa `programs/depois-do-bloco.txt` é esse caso, com erro.

- `x` → `x@4` variável
- `n` → `n@3` parâmetro

**Depois de fechar a função (linha 15).**

```
0  x@1 variável    f@3 procedimento
```

**Depois de declarar `main` e abrir o escopo do corpo (linha 17).** `main` não tem parâmetro, então o nível 1 fica vazio até uma declaração local. Neste programa não há.

```
1  (vazio)
0  x@1 variável    f@3 procedimento    main@17 procedimento
```

**Uso na linha 18**, `x = 1`. A função já fechou, então este `x` é o global. O `1` não entra na tabela.

- `x` → `x@1` variável

No fim, os dois escopos fecham e a tabela pode ser descartada. A árvore fica com `decl` em cada uso. A saída esperada, escrita daqui, é `test/esperado/id-sombreamento.txt`:

```
erros:
usos:
linha 5: x -> x@4 variável
linha 5: n -> n@3 parâmetro
linha 8: x -> x@7 variável
linha 8: n -> n@3 parâmetro
linha 11: y -> y@10 variável
linha 11: x -> x@7 variável
linha 14: x -> x@4 variável
linha 14: n -> n@3 parâmetro
linha 18: x -> x@1 variável
```

Os três `x` são símbolos diferentes. Os três `n` são o mesmo objeto, o parâmetro.

## 2. O que o Symbol guarda

| Campo | Papel | Quem preenche |
|---|---|---|
| `name` | o identificador, do jeito que está no fonte | a identificação, na hora do `declare` |
| `category` | variável, parâmetro, procedimento ou função | a identificação, pelo nó da declaração |
| `declaration` | o nó da declaração: `Var`, `Param` ou `FuncDecl` | a identificação |
| `level` | a profundidade de `table.level()` no momento em que o símbolo é criado | a identificação. 0 é o global |
| `type` | o tipo semântico | ninguém neste checkpoint. Fica `null`. O Checkpoint 5 lê `i.decl.type` |
| `displacement` | o deslocamento na geração de código | ninguém neste checkpoint. Fica 0 |

`declare` não substitui um símbolo já existente, então a linha guardada em `declaration` é sempre a da primeira declaração daquele nome naquele escopo. A mensagem de redeclaração cita essa linha.

Uma declaração com vários nomes (`int a, b = 2`) produz um símbolo por `Var`. O `VarDecl` inteiro não é o nó da declaração: ele não tem um nome só.

Não acrescentamos campo de constante nem de parâmetro por referência. A gramática não tem `const`, e todo parâmetro é por valor. As categorias são só as quatro da tabela. `void f` é procedimento (`returnType` null); `int f` é função.

A linha citada como `nome@L` é a linha que o parser do CP3 já gravou no nó: no `Var`, a do nome; no `Param`, a do tipo; no `FuncDecl`, a do tipo de retorno ou do `void`. Nos programas de teste o nome está na mesma linha que esse token.

## 3. A tabela

A interface é a da Aula 11, sem mudança de assinatura: `openScope`, `closeScope`, `declare`, `lookupLocal`, `lookup`, `level`.

Escolhemos a pilha de mapas (`StackedSymbolTable`). Cada escopo é um `HashMap` de nome para `Symbol`; abrir empilha um mapa vazio, fechar desempilha, `lookupLocal` lê só o topo e `lookup` desce a pilha. O nível é o tamanho da pilha menos um.

Descartamos a tabela única com registro: ela precisa de uma lista à parte dos nomes inseridos para apagá-los no `closeScope`, e só funciona se `Symbol.level` for exatamente o nível da tabela na inserção. Descartamos os escopos encadeados pelo mesmo motivo prático ao contrário: a pilha já é esse encadeamento, com o pai na posição de baixo em vez de num ponteiro, e é o desenho da seção 1.

Contrato, além do que a interface já diz:

- `declare` devolve `false` e não troca o símbolo quando o nome já está no escopo corrente. Um nome de um escopo de fora não conta: isso é sombreamento, e `declare` devolve `true`.
- `lookup` e `lookupLocal` devolvem `null` quando não acham. Sem escopo aberto, os dois devolvem `null`.
- `closeScope` sem escopo aberto não faz nada. Não lança `NullPointerException`.
- `declare` sem escopo aberto lança `IllegalStateException`. A passada não chega nesse caso: o programa abre o global antes de qualquer declaração.
- Sem escopo aberto, `level` devolve `-1`. Com o global aberto, devolve `0`.

O visitor só vê a interface. `new StackedSymbolTable()` aparece numa linha, em `IdentificationPass.analyze`. A tabela é descartada no fim; a lista de erros fica no objeto da passada.

## 4. As cinco decisões

### 4.1 Sombreamento

**Escolha.** Um nome interno pode ocultar um externo. Redeclarar no mesmo escopo é sempre erro, seja variável, parâmetro, procedimento ou função. Não há sobrecarga.

**Alternativa descartada.** Proibir sombreamento em qualquer nível, ou aceitar a segunda declaração no mesmo bloco e ficar com ela. A primeira impede o bloco interno de usar um nome óbvio (`x` dentro de `f` quando já existe um `x` global). A segunda faz o mesmo texto ter dois significados no mesmo trecho, e `declare` deixaria de ser "a primeira permanece".

**Motivo.** É o que a pilha já faz: `declare` olha só o topo, `lookup` desce. O desenho da seção 1 depende disso. O `x` da linha 14 volta a ser o `x` da linha 4 justamente porque o da linha 7 morreu no `closeScope`, não porque foi apagado por uma redeclaração.

**Teste.** `programs/sombreamento.txt` é o caso válido. `programs/redeclaracao.txt` é o erro: o segundo `int a` global e o segundo `int b` do `main` são recusados, e o uso que vem depois resolve para a primeira declaração (`b@5`, `a@1`).

### 4.2 Declarar antes de usar

**Escolha.** Uma passada só, da esquerda para a direita. O uso precisa aparecer depois da declaração no texto. Não há passada prévia nem declaração antecipada.

O nome entra no escopo no declarador, antes do inicializador. `int a = 1, b = a` enxerga `a`. `int c = c` resolve para o próprio `c`. `int d = e, e = 1` não enxerga o `e`, que ainda não foi declarado. É a mesma regra nos dois: cada nome é inserido, e só então o inicializador dele é resolvido.

**Alternativa descartada.** Uma passada que primeiro coleta toda declaração do bloco e só depois resolve os usos. Isso faria `x = 1` no começo de `f` enxergar um `int x` escrito depois de `f`, e a ordem dos erros deixaria de ser a ordem do texto.

**Motivo.** A passada é um visitor. Quando ela está num uso, a tabela contém exatamente o que já foi declarado. Dá para desenhar o ambiente em qualquer linha sem lembrar o que vem abaixo.

**Teste.** `programs/declara-antes.txt`: o `x = 1` dentro de `f` é erro, porque o `int x` global está na linha 5, depois de `f`; o `a = x` do `main` resolve para `x@5`. O `a = b` é erro; o `b = a` da linha seguinte resolve, porque `int b` ficou entre os dois. `programs/ponto-declaracao.txt` trava o ponto do declarador: `c = c` resolve para `c@4`, e o `e` do inicializador de `d` não.

### 4.3 Funções mutuamente recursivas

**Escolha.** Não são permitidas. A recursão direta é. O nome do procedimento ou da função é declarado no escopo de fora antes do `openScope`, então o corpo pode chamar a si mesmo. Uma chamada a um nome que só vai ser declarado mais abaixo é "não declarado".

**Alternativa descartada.** Uma coleta de todos os nomes globais de função antes de visitar os corpos. Isso permitiria `par` e `impar` se chamarem, mas a gramática não tem protótipo, e a coleta reportaria a redeclaração de uma função antes dos erros que aparecem acima dela no texto. A passada deixaria de ser uma leitura só.

**Motivo.** O enunciado da passada já descreve o mecanismo da recursão direta (declarar o nome, depois abrir o escopo). Estender isso para a recursão mútua exigiria uma segunda ida, que a decisão 4.2 recusou. Quem precisar chamar uma função declara ela antes.

**Teste.** `programs/mutua.txt`: `impar` dentro de `par` não foi declarado; `par` dentro de `impar` resolve para `par@1`. `programs/recursivo.txt`: `contagem` chama `contagem` e a chamada resolve para o procedimento da linha 1, nível 0.

Função aninhada usaria o mesmo `visitFuncDecl`: o nome seria declarado no escopo corrente, seja ele o global ou um bloco, e o corpo abriria um escopo novo em seguida. A AST do CP3 não tem onde pendurar uma `FuncDecl` dentro de um bloco (`FuncDecl` não é `StmtNode`) e a gramática também não gera isso. O método não testa se o nível é 0.

### 4.4 Parâmetros e corpo

**Escolha.** O mesmo escopo. `void f(int a) { int a; }` é redeclaração. O bloco do corpo não chama `openScope`; a passada abre o escopo da função, declara os parâmetros e percorre os comandos do corpo. Um bloco escrito dentro do corpo, o corpo de um `if` e o corpo de um `while` abrem escopo próprio e podem sombrear o parâmetro.

**Alternativa descartada.** O corpo abrir outro escopo por causa das chaves. Aí `int a` no começo do corpo sombrearia o parâmetro em vez de ser erro, e o parâmetro ficaria invisível no trecho inteiro em que o programador acha que está usando ele. As chaves da função, nesta linguagem, só existem porque a gramática exige bloco; não são um escopo a mais.

**Motivo.** É o que C e Java fazem com parâmetro e variável local do corpo, e é o que o enunciado coloca como primeira opção. O teste do sombreamento continua valendo para um bloco interno: a decisão é sobre o corpo, não sobre todo bloco.

**Teste.** `programs/param-corpo.txt`. O segundo parâmetro de `f(int a, int a)` é redeclaração na linha 1. O `int a` solto no corpo de `g` é redeclaração do parâmetro da linha 5. O `int a` dentro do bloco de `h` é outro símbolo, nível 2, e o `a = 1` resolve para ele (`a@11`), não para o parâmetro.

### 4.5 Espaço de nomes

**Escolha.** Um só, como em C. Variável, parâmetro, procedimento e função disputam o mesmo nome no mesmo escopo. `lookup` não filtra por categoria.

**Alternativa descartada.** Dois espaços, um de valores e um de funções, como em Pascal. A interface da aula recebe só o nome. Dois mapas obrigariam `lookup` a saber se o uso é chamada ou variável, e `int f; void f()` deixaria de ser erro.

**Motivo.** A linguagem é no estilo de C, e a tabela da seção 3 tem uma chave por nome. A categoria fica no símbolo para as fases seguintes; ela não particiona a busca.

**Teste.** `programs/espaco-nomes.txt`. `int f` e `void f` no global são redeclaração: o procedimento é recusado e o símbolo que fica é a variável da linha 1. Dentro de `g`, `int g` sombreia o procedimento (escopo diferente, decisão 4.1). Tanto `g = 1` quanto `g()` resolvem para essa variável, não para o procedimento. Dentro de `h`, o parâmetro `h` sombreia o procedimento, e os dois lados de `h = h` resolvem para o parâmetro.

`programs/funcao.txt` só separa a categoria: `int id` é função, `void` é procedimento. A busca é a mesma.

## 5. A passada

`IdentificationPass` é um `Visitor<Void>`. Não usa `instanceof` nem `switch` sobre o tipo do nó.

- **Programa.** Abre o global, visita cada declaração na ordem, fecha.
- **Bloco** (`Block` de verdade: bloco solto, `if`, `while`, `else`). `openScope`, os comandos na ordem, `closeScope`. O bloco do corpo da função é a exceção da decisão 4.4: os comandos são visitados, o bloco em si não abre escopo.
- **Declaração.** Monta o `Symbol` com `table.level()` e chama `declare`. Se voltar `false`, registra a redeclaração, fica com o primeiro símbolo e continua. O inicializador é visitado depois do `declare`.
- **Uso.** `lookup`. Se achou, grava o símbolo em `decl`. Se não achou, registra "não declarado" e deixa `decl` null.
- **Onde está o uso.** Na AST do CP3 o nome de uso nem sempre é um filho `Id`. `Id` é o uso dentro de expressão. `Assign` guarda o nome da esquerda como texto, e `Call` guarda o nome chamado como texto. Os três recebem `decl`. Nenhum filho mudou, então a impressão da árvore do CP3 continua igual. Atribuição resolve o nome e depois desce na expressão. Chamada resolve o nome e depois desce nos argumentos. `If`, `While`, `Return`, `Bin`, `Unary`, `Literal` e `CallStmt` só descem.
- **Tipo.** `int`, `double`, `bool`, `char`, `string` e `void` são palavra reservada. A passada não os declara e não os busca.

Erros se acumulam e a passada continua. Cada mensagem tem a linha e o nome:

```
linha 2: 'a' já declarado neste escopo (linha 1)
linha 4: 'b' não declarado
```

A primeira forma cita a linha da declaração que ficou. A segunda é o nome que `lookup` não achou. Não há coluna: a linha basta para o desenho, e dois erros na mesma linha saem na ordem em que o texto é lido (esquerda, depois direita).

Nenhuma entrada válida lança `NullPointerException`: filho opcional (`inicializador`, `else`, valor de `return`) é testado antes do `accept`, e árvore null faz a passada devolver a lista vazia. Programa com erro sintático não chega aqui, porque o parser do CP3 devolve árvore null. Não há ciclo na árvore, então a passada não entra em laço.

O relatório que os testes comparam tem duas seções, `erros:` e `usos:`, nessa ordem. Um uso resolvido sai como `linha L: nome -> nome@D categoria`. Um uso sem declaração sai como `linha L: nome -> ?`.

## 6. Programas de teste

| Arquivo | O que trava |
|---|---|
| `programs/sombreamento.txt` | seção 1, sombreamento válido |
| `programs/redeclaracao.txt` | redeclaração no mesmo escopo, a primeira permanece |
| `programs/nao-declarado.txt` | nome usado e nunca declarado |
| `programs/declara-antes.txt` | uso antes da declaração é erro; uso depois resolve |
| `programs/ponto-declaracao.txt` | o nome entra antes do inicializador |
| `programs/mutua.txt` | recursão mútua não resolve |
| `programs/param-corpo.txt` | parâmetro repetido, corpo no mesmo escopo, bloco interno sombreia |
| `programs/espaco-nomes.txt` | variável e função no mesmo nome; a chamada não tem mapa próprio |
| `programs/tres-erros.txt` | quatro erros, na ordem das linhas 2, 4, 5 e 6 |
| `programs/recursivo.txt` | procedimento que chama a si mesmo |
| `programs/depois-do-bloco.txt` | o nome de dentro não existe depois do `}` |
| `programs/funcao.txt` | `int` no retorno é função, não procedimento |
