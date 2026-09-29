# Checkpoint 1: especificação léxica e scanner

```
cp1-Matheus-Christian/
  especificacao-lexica.md   tokens, alfabeto, palavras reservadas, erros
  log-uso-ia.md
  automatos/                desenhos dos AFDs (png) e fontes em Graphviz
  src/lexer/                scanner
  test/lexer/               suíte de testes
  test/saida-testes.txt     saída da última execução dos testes
  exemplos/                 programas de exemplo e a saída do scanner para cada um
```

## Compilar e rodar

Precisa só do JDK (testado com o 21, o código compila a partir do Java 8). Na pasta `cp1-Matheus-Christian`:

```
javac -encoding UTF-8 -d out src/lexer/*.java test/lexer/*.java
java -cp out lexer.ScannerTest
java -cp out lexer.Main exemplos/fatorial.txt
java -cp out lexer.Main exemplos/com-erros.txt
```

`ScannerTest` sai com código 1 se algum teste falhar. `Main` imprime um token por linha (linha:coluna, tipo, lexema) e os erros léxicos na ordem em que aparecem, e sai com código 1 se houver erro léxico.

## Estrutura do código

- `Token`: tipo, lexema, linha e coluna onde começa.
- `TokenType`: todos os tipos de token.
- `LexicalError`: mensagem, linha e coluna.
- `Scanner`: `peek()`, `advance()` e `hasNext()` sobre o texto; `nextToken()` é o AFD, com um `case` por estado; `getErrors()` devolve os erros encontrados.
- `Main`: roda o scanner sobre um arquivo.

A correspondência entre os estados dos desenhos e as linhas do código está em [automatos/README.md](automatos/README.md).
