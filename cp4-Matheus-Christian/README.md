# Checkpoint 4: tabela de símbolos e identificação

```
cp4-Matheus-Christian/
  escopo-design.md       ambiente, Symbol, pilha de mapas e as cinco decisões
  ast-design.md          a hierarquia do CP3; decl é o único acréscimo
  gramatica-first-follow.md
  src/lexer/             scanner do CP1, sem modificação
  src/parser/            parser do CP2/CP3, sem modificação
  src/ast/               a AST do CP3; Id, Assign e Call ganharam decl
  src/visitor/           Visitor<T>, os dois impressores e a passada de identificação
  src/symbols/           Symbol, SymbolTable, a pilha de mapas e o main
  test/lexer/            testes do scanner
  test/parser/           testes do CP3
  test/symbols/          a tabela sozinha e a passada
  test/esperado/         árvores do CP3 e os relatórios id-*.txt, escritos à mão
  test/saida-testes.txt  saída da última execução das quatro suítes
  programs/              os programas da passada
  exemplos/              os exemplos do CP3
```

O desenho do ambiente e o motivo de cada decisão estão em `escopo-design.md`. Este arquivo só diz como compilar e o que cada teste cobre.

## Compilar e rodar

Precisa só do JDK (testado com o 21; o código compila a partir do Java 8). Na pasta `cp4-Matheus-Christian`:

```
javac -encoding UTF-8 -d out \
  src/lexer/*.java src/parser/*.java src/ast/*.java src/visitor/*.java src/symbols/*.java \
  test/lexer/*.java test/parser/*.java test/symbols/*.java
java -cp out symbols.SymbolTableTest
java -cp out symbols.IdentificationTest
java -cp out parser.ParserTest
java -cp out lexer.ScannerTest
java -cp out symbols.Main programs/sombreamento.txt
```

`SymbolTableTest`, `IdentificationTest`, `ParserTest` e `ScannerTest` saem com código 1 se algum teste falhar. `symbols.Main` imprime o relatório (`erros:` e `usos:`) e termina com código 0 quando não há erro de identificação. Com erro sintático, a passada não roda: saem os erros do parser e `programa rejeitado`.

O scanner e o parser são os do CP3. Para conferir:

```
diff -r ../cp3-Matheus-Christian/src/lexer src/lexer
diff -r ../cp3-Matheus-Christian/src/parser src/parser
```

## O que a passada faz

Uma ida da esquerda para a direita. Abre o global, declara cada nome no escopo corrente e grava em `decl` o símbolo do uso. Bloco abre e fecha escopo. O nome de procedimento e de função entra no escopo de fora antes do corpo, então a recursão direta resolve; a recursão mútua não. O corpo da função não abre um segundo escopo: parâmetro e variável solta do corpo disputam o mesmo, e `void f(int a) { int a; }` é redeclaração. Variável e função compartilham um espaço de nomes.

O nome entra no escopo antes do inicializador. Uso antes da declaração é erro. Redeclarar no mesmo escopo é erro, a primeira declaração fica, e a passada continua. Cada mensagem traz a linha e o nome:

```
linha 2: 'a' já declarado neste escopo (linha 1)
linha 4: 'b' não declarado
```

Não há checagem de tipos. `Symbol.type` fica null e `Symbol.displacement` fica 0.

## Testes

`test/symbols/SymbolTableTest.java` exercita a pilha sem árvore: declarar e buscar, sombreamento com restauração no `closeScope`, redeclaração que devolve false, `lookupLocal` cego ao escopo de fora, nome ausente, `level`, `closeScope` a mais e os dois campos reservados.

`test/symbols/IdentificationTest.java` compara cada programa de `programs/` com o relatório em `test/esperado/id-*.txt` e ainda confere o objeto: o `decl` do nó é o símbolo do relatório, o nível e a categoria batem com o desenho, e `type` continua null. Os doze programas cobrem o válido com sombreamento, a redeclaração, o nome não declarado, as cinco decisões, quatro erros na ordem do texto, o procedimento recursivo e o uso depois do `}`.

A suíte do CP3 continua em `ParserTest` e `ScannerTest`. `exemplos/fatorial.txt` e `exemplos/primos.txt` passam pela identificação sem exceção; as chamadas de `imprime`, que o exemplo não declara, são os únicos erros.
