# Checkpoint 2: gramática e parser recursivo descendente

```
cp2-Matheus-Christian/
  gramatica-first-follow.md   gramática final, antes/depois, FIRST/FOLLOW e verificação LL(1)
  log-uso-ia.md
  src/lexer/                  scanner do CP1, sem modificação
  src/parser/                 parser (reconhecedor) e programa de linha de comando
  test/lexer/                 testes do scanner, copiados do CP1
  test/parser/                testes do parser
  test/saida-testes.txt       saída da última execução das duas suítes
  exemplos/                   programas de exemplo e a saída do parser para cada um
```

## Compilar e rodar

Precisa só do JDK (testado com o 21; o código compila a partir do Java 8). Na pasta `cp2-Matheus-Christian`:

```
javac -encoding UTF-8 -d out src/lexer/*.java src/parser/*.java test/lexer/*.java test/parser/*.java
java -cp out parser.ParserTest
java -cp out lexer.ScannerTest
java -cp out parser.Main exemplos/primos.txt
java -cp out parser.Main exemplos/varios-erros.txt
java -cp out parser.Main --trace exemplos/varios-erros.txt
```

`ParserTest` e `ScannerTest` saem com código 1 se algum teste falhar. `parser.Main` imprime os erros léxicos e sintáticos em ordem de posição (linha:coluna) e termina com `programa aceito` (código 0) ou `programa rejeitado` (código 1). Com `--trace`, imprime antes o traço da recuperação: cada erro reportado, os tokens descartados, onde a análise sincronizou e o que o `match` fez.

O scanner é o do CP1 sem nenhuma alteração. Para conferir, este comando não imprime nada:

```
diff -r ../cp1-Matheus-Christian/src/lexer src/lexer
```

## Estrutura do código

- `Parser`: um método `parseX()` por não-terminal da gramática, na ordem da seção 1.3 do documento e com a produção no comentário. Os conjuntos da tabela da seção 3 são as constantes `FIRST_X` e `FOLLOW_X` no topo da classe. `parse()` devolve `true` se o programa foi aceito, ou seja, sem erro sintático nem léxico. `getErrors()` devolve os erros sintáticos.
- `SyntaxError`: mensagem, linha e coluna, no mesmo formato do `LexicalError` do CP1.
- `TokenNames`: texto das mensagens. Mostra um token encontrado (`'x'`, `fim do arquivo`) e um conjunto de esperados. Para encurtar a mensagem, um conjunto que contém todo o `FIRST(Stmt)` aparece como "comando", todo o `FIRST(Expr)` como "expressão", e assim por diante. A mensagem lista exatamente o conjunto que o parser testou naquele ponto.
- `Main`: roda scanner e parser sobre um arquivo.

O parser puxa os tokens do `Scanner` um de cada vez com `nextToken()`. Normalmente só olha o lookahead. O token seguinte só é lido na recuperação de erro do `match` (abaixo), nunca para escolher alternativa.

## Como o parser decide

Todo método com mais de uma alternativa segue a tradução da Aula 6:

1. lookahead no FIRST de uma alternativa: segue essa alternativa;
2. o não-terminal é anulável e o lookahead está no FOLLOW: aceita ε;
3. qualquer outro caso: erro de decisão (abaixo).

O passo 2 é a versão robusta do ε: o vazio nunca é aceito "por falta de opção". Os métodos de produção única (`parseBlock`, `parseIfStmt`, `parseAddExpr`...) não decidem nada, só casam e chamam na ordem da produção.

## Recuperação de erros

Um erro nunca para a análise: o parser reporta, se recupera e continua até o EOF numa rodada só.

### Erro de decisão: modo pânico com o conjunto de sincronização

Quando `parseX()` não tem alternativa para o lookahead, o parser reporta o erro na posição do lookahead, com o conjunto que foi testado ("encontrado '{', esperado operador, ')', ';' ou ','"). Depois descarta tokens até achar um de `SYNC(X)` e retorna, como se `X` tivesse sido reconhecido. O conjunto de sincronização depende do tipo de não-terminal:

| Não-terminais | SYNC(X) | Por quê |
|---|---|---|
| a maioria | `FOLLOW(X)` ∪ âncoras ∪ `{ $ }` | |
| `Type`, `Params`, `ParamList` (cabeçalho de função) | `FOLLOW(X)` ∪ `{ { } }` ∪ `{ $ }` | tipo e identificador aparecem dentro da própria lista de parâmetros, então não podem ser âncora ali |
| `DeclList`, `StmtList` (listas) | `FOLLOW(X)` ∪ `FIRST(elemento)` ∪ `{ $ }` | parando no começo de um novo elemento, a lista continua dali |

As âncoras são `int double bool char string if while return void { }`: palavras que só aparecem no começo de um comando ou de uma declaração, e as chaves. Sem elas, um erro dentro de uma expressão descartaria tudo até o próximo `)` ou `;` do FOLLOW, engolindo comandos inteiros e até o fim de um bloco. Exemplo: em `while (x > 0 {`, o `MulRest` vê `{`, que não está no FOLLOW dele. Como `{` é âncora, a sincronização para ali mesmo e o corpo do `while` é analisado normalmente. O identificador ficou fora das âncoras de propósito: ele aparece no meio das expressões, e parar nele faria o resto de uma expressão quebrada virar um comando novo (era o que acontecia com `if (total > 0 & nome != "")`, em que o `&` sozinho é descartado pelo scanner).

### Erro no `match(t)`: token a mais ou token faltando

Quando o lookahead não é o terminal esperado `t`:

- **token a mais:** se o token *seguinte* é `t`, o atual está sobrando. Ele é descartado e `t` é casado (`while (x > 0)) {` dá "encontrado ')' a mais antes de '{'");
- **token faltando:** senão, `t` é dado como ausente. O erro aponta logo depois do último token consumido ("esperado ';' depois de ')'") e o parser segue sem consumir nada. Para um terminal, todo outro token é de sincronização (Aho et al., seção 4.4.5).

### Corpo sem chaves

Como a gramática exige bloco no corpo de `if`/`else`/`while` (seção 2.4 do documento), `if (x < 0) x = -x;` é erro. Se `parseBlock()` encontra um comando onde esperava `{`, ele reporta "esperado '{' ... o corpo precisa estar entre chaves" e analisa aquele comando como se fosse o corpo. Sem isso, o `{` dado como ausente nunca seria fechado, e o `}` da função fecharia o corpo do `if`, gerando erros falsos até o fim do arquivo.

### Modo pânico: sem erros em cascata

Depois de um erro reportado, o parser entra em modo pânico. Os erros seguintes não são reportados até um `match` dar certo. Eles são consequência do primeiro: por exemplo, cada nível da expressão (`MulRest`, `AddRest`, `EqRest`...) vê o mesmo `{` em que o de dentro já sincronizou, e o `match(')')` logo depois o dá como ausente. Com `--trace` esses aparecem marcados como "em cascata". Um erro novo, depois de pelo menos um token casado, volta a ser reportado. É isso que faz o programa de `exemplos/varios-erros.txt` ter seus quatro erros reportados e nenhum a mais.

### Por que nenhum caminho de erro entra em loop

- `synchronize()` só para num token do conjunto ou no EOF, e o EOF nunca é consumido. O token que causou o erro de decisão não está em FIRST nem em FOLLOW, então ou ele é descartado ou já é o EOF.
- Toda chamada recursiva que pode se repetir (as listas, os `Rest` das expressões, `Unary`, `ElseBody → IfStmt`) só acontece com o lookahead no FIRST do que vem, e esse primeiro token é consumido. Depois de sincronizar, uma lista só é retomada se o lookahead estiver no FIRST do elemento.
- O `match` ou consome token, ou só retorna.

Os testes da última seção da suíte verificam isso na prática (abaixo). Conferimos também que eles pegam o problema de verdade: trocando a retomada de `StmtList` por uma chamada sem a guarda do FIRST, quatro testes falham com estouro de pilha.

## Testes

`test/parser/ParserTest.java`, sem dependência externa, no mesmo formato do `ScannerTest`:

- **programas válidos:** o programa do CP1, todos os tipos básicos e declarações (globais, locais, várias por linha), seleção com `else if` e repetição aninhada, procedimentos e funções (sem parâmetros, com vários, `void` com `return;`, recursão, chamada como comando e dentro de expressão), precedência, blocos soltos e vazios, programa vazio;
- **um erro por programa** (13 casos): cada um confere que sai exatamente um erro, na linha e coluna certas, com a mensagem esperada, e que o resto do programa é analisado sem erro falso;
- **vários erros numa rodada só:** o programa de `exemplos/varios-erros.txt` com quatro erros, o mesmo erro em três funções e um caso de cascata que deve virar um erro só;
- **nenhum caminho de erro trava:** todos os prefixos de um programa, o programa sem cada um dos tokens, e 2000 sequências aleatórias de tokens. Cada análise roda com tempo limite e confere o limite do modo pânico (no máximo um erro a mais que o número de tokens).

## Exemplos

| Arquivo | O que mostra | Saída |
|---|---|---|
| `exemplos/fatorial.txt` | programa do CP1 | `saida-fatorial.txt` |
| `exemplos/primos.txt` | todos os tipos, globais, `else if`, funções com e sem retorno | `saida-primos.txt` |
| `exemplos/varios-erros.txt` | quatro erros sintáticos recuperados numa rodada | `saida-varios-erros.txt` (com `--trace`) |
| `exemplos/com-erros.txt` | o exemplo de erros léxicos do CP1: os tokens descartados pelo scanner viram erros sintáticos | `saida-com-erros.txt` |
