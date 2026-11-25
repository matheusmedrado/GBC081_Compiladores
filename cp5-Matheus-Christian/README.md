# Checkpoint 5: verificador de tipos

```
cp5-Matheus-Christian/
  tipos-a-mao.pdf        Parte 1, escrita à mão (cópia de tipos-design.md): regras, tabela e decisões
  tipos-design.md        o mesmo conteúdo passado a limpo, com o formato das mensagens
  log-uso-ia.md          o log de uso de ferramentas de IA
  escopo-design.md       o ambiente e as decisões de escopo do CP4
  ast-design.md          a hierarquia do CP3
  gramatica-first-follow.md
  src/lexer/ src/parser/ scanner e parser do CP3, sem modificação
  src/ast/               a AST; Expr ganhou o campo type
  src/symbols/           a tabela do CP4; Symbol.type virou Type e ganhou parameters
  src/visitor/           os visitors do CP3/CP4 e o TypeChecker
  src/types/             Type, TypeRules, Diagnostic, Analysis e o main
  test/programas/        os programas do CP5
  test/esperado/         a saída esperada de cada um (e as do CP3/CP4)
  test/types/            TypeRulesTest (regras sem parser) e TypeCheckTest (programas)
  test/lexer/ test/parser/ test/symbols/   as suítes do CP3 e do CP4
  test/saida-testes.txt  a saída da última execução das seis suítes
  programs/ exemplos/    os programas do CP4 e os exemplos do CP3
```

## Compilar e rodar

Precisa só do JDK (testado com o 21; o código-fonte compila a partir do Java 8, os testes usam lambda). Na pasta `cp5-Matheus-Christian`:

```
javac -encoding UTF-8 -d out src/*/*.java test/*/*.java
java -cp out types.TypeRulesTest
java -cp out types.TypeCheckTest
java -cp out symbols.SymbolTableTest
java -cp out symbols.IdentificationTest
java -cp out parser.ParserTest
java -cp out lexer.ScannerTest
java -cp out types.Main test/programas/varios_erros.txt
```

Cada suíte sai com código 1 se algum teste falhar. `types.Main` imprime as mensagens e `programa aceito` ou `programa rejeitado`, e sai com 0 só quando o programa é aceito. Avisos não rejeitam. Com erro sintático, a análise para no parser. Com erro de identificação ou de tipo, o programa é rejeitado: `Analysis.accepted()` é a porta da geração de código.

## Como os tipos estão representados

`types.Type` tem uma instância por tipo: `INT`, `DOUBLE`, `CHAR`, `BOOL`, `STRING`, `VOID` e `ERROR`. Comparar tipos é comparar referência. `ERROR` é o tipo de uma expressão que já gerou mensagem; toda regra aceita `ERROR` sem gerar mensagem nova.

O tipo de uma expressão fica em `Expr.type`, gravado pelo verificador em todo nó de expressão. É a única mudança na hierarquia da AST.

A assinatura fica no `Symbol`: `type` é o tipo da variável ou do parâmetro, ou o retorno da função (`VOID` no procedimento), e `parameters` é a lista dos tipos dos parâmetros. Quem preenche é a passada de identificação, no `define`, porque é lá que o `TypeNode` está à mão; o nome e a assinatura entram antes do corpo, então a chamada recursiva já enxerga os dois. O verificador não chama `declare`, `openScope` nem `closeScope`: só lê o `decl`.

## Onde fica cada regra

| No papel | No código |
|---|---|
| `⪯` (compatibilidade) | `TypeRules.assignable` |
| [ARIT], [MOD], [REL], [IGUAL-NUM], [IGUAL-BOOL], [LOG] | `TypeRules.binary`, um `if` por linha da tabela |
| [NEG], [NOT] | `TypeRules.unary` |
| [ERRO] | as primeiras linhas de `binary`, `unary` e `assignable`, e `TypeRules.isCondition` |
| [LIT-*] | `TypeChecker.visitLiteral` |
| [ID], decisão 11 | `TypeChecker.visitId` |
| [DECL] | `TypeChecker.visitVar` |
| [ATRIB], decisão 11 (atribuir a função) | `TypeChecker.visitAssign` |
| [IF], [IF-SEM], [WHILE], [BLOCO] | `visitIf`, `visitWhile`, `visitBlock` |
| [RETURN], [RETURN-VOID] | `TypeChecker.visitReturn` |
| [CHAMADA], decisões 8 e 11 (chamar variável) | `TypeChecker.checkCall` e `visitCall` |
| [CHAMADA-CMD], decisão 6 | `TypeChecker.visitCallStmt` |
| [FUNC], [PROC], decisão 9 | `TypeChecker.visitFuncDecl` e o campo `returns` |
| decisões 1, 2 e 5 | a hierarquia `char < int < double` em `TypeRules` |
| decisões 7 e 10 | na identificação do CP4 (`IdentificationPass`), sem mudança |
| variáveis sem valor (seção 5) | o conjunto `unset` do `TypeChecker` |
| ordem das mensagens | `Analysis.of` |

O `TypeChecker` é um `Visitor<Type>`: cada visit de expressão grava e devolve o tipo, cada visit de comando devolve `null`. Não usa `instanceof` nem `switch` sobre o tipo do nó. A chamada como comando passa por `checkCall` direto, sem `accept`, e por isso não cai na regra de procedimento em expressão.

O contexto guarda a função corrente (`currentFunction` e `currentReturn`), salva e restaurada em `visitFuncDecl`. A linguagem não tem `break`, `print` nem `read` (seção 8 de `tipos-design.md`), então não há pilha de laços abertos nem regra para eles.

Nenhum filho opcional é visitado sem teste (`inicializador`, `else`, valor de `return`), símbolo e tipo ausentes viram `ERROR`, e árvore null não é percorrida. A árvore não tem ciclo e cada nó é visitado uma vez, então não há laço.

## Testes

`test/types/TypeRulesTest.java` testa as regras sem parser: cada linha da tabela de operadores, aceita e recusada, chamando `TypeRules`; a compatibilidade `⪯`; `ERROR` sem cascata; e algumas expressões montadas à mão (`Bin` e `Unary` sobre `Literal`) passadas pelo `TypeChecker`, conferindo o tipo gravado no nó e a mensagem.

`test/types/TypeCheckTest.java` roda cada `test/programas/X.txt` e compara o relatório com `test/esperado/X.txt`. Também confere que toda expressão de `valido.txt` saiu com tipo, que nenhuma entrada lança exceção ou passa de 5 segundos, que o erro sintático para no parser e que os exemplos do CP3 só dão o `imprime` não declarado.

| Programa | O que trava |
|---|---|
| `valido.txt` | os cinco tipos, todos os operadores e comandos, subidas implícitas |
| `expr_tipo.txt` | erro de tipo em operador binário e unário |
| `cond_int.txt` | condição `int` no `if` e no `while` |
| `return_incompativel.txt` | `return` com tipo errado, com valor em procedimento, sem valor em função |
| `chamada_errada.txt` | aridade, tipo de argumento, chamar variável |
| `proc_expressao.txt` | procedimento em expressão; como comando passa |
| `varios_erros.txt` | oito mensagens de identificação e de tipo, em ordem, sem cascata |
| `aviso_sem_valor.txt` | avisos de variável sem valor, com `if`, `while` e `int d = d + 1` |
| `recursiva.txt` | função e procedimento recursivos |
| `nivel_acima.txt` | global e local de fora usadas dentro de blocos |
| `d01_*.txt` a `d11_*.txt` | um programa por decisão, na numeração de `tipos-design.md` |

As saídas esperadas foram escritas a partir das regras de `tipos-design.md`, antes de rodar o verificador.

### Regressão

As suítes do CP3 (`ScannerTest`, `ParserTest`) e do CP4 (`SymbolTableTest`, `IdentificationTest`) continuam passando. Duas asserções do CP4 mudaram, porque conferiam que `Symbol.type` ficava vazio, e o CP5 é justamente quem o preenche: `IdentificationTest` agora espera o tipo declarado no símbolo, e `SymbolTableTest` grava um `Type` no campo, que deixou de ser `Object`. Os relatórios `id-*.txt` são os mesmos do CP4.
