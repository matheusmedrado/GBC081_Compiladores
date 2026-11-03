# Checkpoint 3: AST e visitors

```
cp3-Matheus-Christian/
  ast-design.md          hierarquia de nós, mapeamento da gramática e formato do impressor
  gramatica-first-follow.md   a gramática do CP2, sem modificação
  log-uso-ia.md
  src/lexer/             scanner do CP1, sem modificação
  src/parser/            parser do CP2, agora devolvendo a AST
  src/ast/               hierarquia de nós
  src/visitor/           Visitor<T>, impressor da árvore e reimpressor de código
  test/lexer/            testes do scanner, copiados do CP1
  test/parser/           testes do CP2 mais os testes da AST
  test/esperado/         árvores esperadas, no formato do impressor
  test/saida-testes.txt  saída da última execução das duas suítes
  exemplos/              programas de exemplo e a saída do parser para cada um
```

## Compilar e rodar

Precisa só do JDK (testado com o 21; o código compila a partir do Java 8). Na pasta `cp3-Matheus-Christian`:

```
javac -encoding UTF-8 -d out src/lexer/*.java src/parser/*.java src/ast/*.java src/visitor/*.java test/lexer/*.java test/parser/*.java
java -cp out parser.ParserTest
java -cp out lexer.ScannerTest
java -cp out parser.Main exemplos/primos.txt
java -cp out parser.Main --source exemplos/primos.txt
java -cp out parser.Main --trace exemplos/varios-erros.txt
```

`ParserTest` e `ScannerTest` saem com código 1 se algum teste falhar. `parser.Main` imprime a árvore (ou o código reimpresso, com `--source`) e termina com `programa aceito` (código 0). Com erro, a árvore não é impressa: saem os erros léxicos e sintáticos em ordem de posição e `programa rejeitado` (código 1).

O scanner é o do CP1 sem nenhuma alteração. Para conferir, este comando não imprime nada:

```
diff -r ../cp1-Matheus-Christian/src/lexer src/lexer
```

## O que mudou em relação ao CP2

A gramática, os conjuntos FIRST/FOLLOW, as decisões e a recuperação de erros são os mesmos. O que mudou é o valor devolvido: cada `parseX()` que corresponde a um nó devolve esse nó, e `parse()` devolve um `ParseResult` com a raiz e a lista de erros. Quando há erro sintático ou léxico, a árvore vem nula.

Os não-terminais de recursão à esquerda (`OrRest`, `AndRest`, `EqRest`, `AddRest`, `MulRest`) recebem o operando da esquerda e devolvem a árvore montada da esquerda para a direita, então `a - b - c` vira `(a - b) - c`. `RelRest` continua sem repetir. As listas (`DeclList`, `StmtList`, `ParamList`, `VarList`, `ArgList`) seguem a mesma ideia: recebem a lista com o que já foi lido e acrescentam um elemento por chamada.

## Testes

`test/parser/ParserTest.java`, sem dependência externa, no formato do CP2:

- a suíte do CP2 inteira, com os mesmos veredictos e as mesmas mensagens;
- cinco programas válidos (fatorial, tipos e declarações, seleção e repetição, procedimentos e funções, primos) comparados com a árvore em `test/esperado/`, no formato do impressor;
- posição: como a impressão não mostra linha e coluna, um programa curto confere a posição nó a nó (a do primeiro token da construção; no `Bin`, a do operador);
- precedência (`a + b * c`), associatividade à esquerda (`a - b - c`) e parênteses que mudam a árvore (`a - (b - c)`), cada um contra o seu arquivo esperado, mais a verificação de que as três árvores são diferentes;
- parênteses redundantes: `(((a)))` produz a mesma árvore que `a`, sem nenhum nó de parênteses;
- ida e volta de todos os programas válidos da suíte (os cinco acima, os de operadores e de blocos, o vazio e os de precedência e parênteses): o texto reimpresso, lido de novo, reimprime igual, e as duas árvores têm a mesma impressão;
- com erro, a árvore vem nula, e nenhum programa (válido ou inválido, inclusive os prefixos, as remoções de token e as 2000 sequências aleatórias) deixa escapar exceção.

## Exemplos

| Arquivo | O que mostra | Saída |
|---|---|---|
| `exemplos/fatorial.txt` | programa do CP1, agora com a árvore | `saida-fatorial.txt` |
| `exemplos/primos.txt` | todos os tipos, globais, `else if`, funções | `saida-primos.txt` |
| `exemplos/varios-erros.txt` | quatro erros sintáticos, árvore não impressa | `saida-varios-erros.txt` (com `--trace`) |
| `exemplos/com-erros.txt` | erros léxicos do CP1 virando erros sintáticos | `saida-com-erros.txt` |
