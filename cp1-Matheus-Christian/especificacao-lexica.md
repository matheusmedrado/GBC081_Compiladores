# Especificação léxica

Construção de Compiladores (GBC081), Checkpoint 1. Grupo Matheus-Christian.

A linguagem segue o estilo de C, com palavras reservadas em inglês. Este documento define exatamente o que o scanner (`src/lexer/Scanner.java`) aceita, e o código implementa só o que está aqui.

Notação usada nas regras: `{ x }` é zero ou mais repetições, `[ x ]` é opcional, `|` é alternativa e texto entre aspas é literal. Classes auxiliares:

```
letra   = "a".."z" | "A".."Z"
digito  = "0".."9"
escape  = "\" ( "n" | "t" | "\" | '"' | "'" )
```

## 1. Alfabeto de entrada

Fora de strings, literais de caractere e comentários, só são válidos:

- letras ASCII `a`..`z` e `A`..`Z` (sem acento)
- dígitos `0`..`9`
- `_` (só dentro de identificador, nunca no início)
- espaço, tab (`\t`), `\r` e `\n`
- `+ - * / % = ! < > & | ( ) { } ; ,`
- `"` e `'`, que abrem string e literal de caractere
- `.`, só entre dígitos de um literal numérico

Dentro de strings, literais de caractere e comentários, qualquer caractere é aceito, inclusive acentos (o arquivo é lido em UTF-8), com as restrições de cada categoria (seção 2). Qualquer outro caractere fora desses contextos, como `@`, `#`, `$`, `?`, `.` solto ou `ç`, é erro léxico.

## 2. Categorias de token

| Categoria | Notação | Exemplos válidos | Decisões |
|---|---|---|---|
| Identificador | `letra { letra \| digito \| "_" }` | `total`, `x1`, `conta_itens` | case-sensitive; `_` permitido, mas não no primeiro caractere; no máximo 32 caracteres |
| Palavra reservada | mesmo padrão do identificador + consulta na tabela | `int`, `while`, `return` | lista fechada na seção 4 |
| String | `'"' { qualquer exceto '"', "\" e quebra de linha \| escape } '"'` | `"ok"`, `"linha 1"`, `"diz \"oi\"\n"`, `""` | escapes `\n \t \\ \" \'`; não pode quebrar linha; se não fechar até o fim da linha ou do arquivo, é erro |
| Operador | ver lista abaixo | `=`, `==`, `<=`, `+`, `&&` | formas compostas: `== != <= >= && \|\|` |
| Literal numérico | `digito { digito } [ "." digito { digito } ]` | `0`, `10`, `3.14`, `007` | sem sinal (o `-` é operador); sem notação científica nem hexadecimal; `3.` e `.5` não são números |
| Literal de caractere | `"'" ( qualquer exceto "'", "\" e quebra de linha \| escape ) "'"` | `'a'`, `'\n'`, `'\''` | exatamente um caractere; necessário por causa do tipo `char` |
| Delimitador | um caractere | `( ) { } ; ,` | |

Operadores, com o nome do token que o scanner gera:

| Aritméticos | Relacionais | Lógicos | Atribuição |
|---|---|---|---|
| `+` PLUS, `-` MINUS, `*` STAR, `/` SLASH, `%` PERCENT | `==` EQ, `!=` NEQ, `<` LT, `<=` LE, `>` GT, `>=` GE | `&&` AND, `\|\|` OR, `!` NOT | `=` ASSIGN |

`&` e `|` sozinhos não são operadores: aparecer um deles sem o par é erro léxico.

Um literal `INT_LITERAL` é qualquer sequência de dígitos. Se o valor cabe no tipo `int` é uma verificação da análise semântica, não do scanner.

## 3. Maiúsculas e minúsculas

A linguagem é case-sensitive, e a regra vale igual para palavras reservadas e identificadores. `total` e `Total` são identificadores diferentes. As palavras reservadas existem só em minúsculas, então `If`, `WHILE` e `Return` são identificadores comuns.

## 4. Palavras reservadas

A lista é fechada, são exatamente estas 12:

| Grupo | Palavras |
|---|---|
| Tipos | `int` `double` `bool` `char` `string` `void` |
| Controle | `if` `else` `while` `return` |
| Literais booleanos | `true` `false` |

- A repetição é só com `while`, não existe `for`.
- Funções não têm palavra reservada própria. A declaração segue o estilo de C: `int soma(int a, int b) { return a + b; }`. Função sem valor de retorno usa `void` e `return;`.
- Funções de entrada e saída, quando existirem, vão ser funções pré-definidas com nome de identificador comum (por exemplo `imprime(x)`), não palavras reservadas.

## 5. Espaços em branco e comentários

- Espaço, tab, `\r` e `\n` separam tokens e são descartados. Cada `\n` avança uma linha; linha e coluna começam em 1 e o tab conta como uma coluna.
- Comentário de linha: começa em `//` e vai até o fim da linha.
- Comentário de bloco: começa em `/*` e termina no primeiro `*/`, podendo ocupar várias linhas.
- Comentários de bloco **não** podem ser aninhados. Em `/* a /* b */ c */`, o comentário termina no primeiro `*/` e ` c */` vira os tokens `c`, `*` e `/`.
- Comentário de bloco sem `*/` até o fim do arquivo é erro léxico.
- `//` e `/*` dentro de uma string fazem parte do texto dela, não iniciam comentário.

## 6. Desambiguação (maximal munch)

O scanner sempre consome o maior prefixo da entrada que ainda forma um token válido, olhando um caractere à frente. Ele só para quando o próximo caractere não tem transição a partir do estado atual. Exemplos:

| Entrada | Tokens | Por quê |
|---|---|---|
| `a<=b` | `a` `<=` `b` | depois de `<`, o `=` ainda continua um token válido |
| `x===y` | `x` `==` `=` `y` | o primeiro token é o maior possível (`==`), depois recomeça |
| `a=-1` | `a` `=` `-` `1` | `=-` não é operador, então `=` termina ali |
| `a/b` | `a` `/` `b` | `/` só vira comentário se o próximo for `/` ou `*` |
| `ifx` | `ifx` | o identificador é lido inteiro antes de consultar a tabela; `ifx` não está nela |
| `12abc` | `12` `abc` | o número termina no primeiro caractere que não é dígito nem `.` (quem reclama disso é o parser) |

Em nenhum caso o scanner precisa voltar atrás na entrada. Quando o autômato para num estado que não é de aceitação (por exemplo, `3.` seguido de espaço), isso é reportado como erro.

## 7. Erros léxicos

Todo erro é reportado com linha e coluna. Depois disso o scanner continua a partir do próximo caractere, sem parar a análise. Tokens com erro não são passados para frente.

| Situação | Posição reportada | Recuperação |
|---|---|---|
| Caractere fora do alfabeto (`@`, `#`, `.` solto...) | o caractere | descarta o caractere |
| `_` no início de um nome | o `_` | descarta o `_`; o resto do nome vira um identificador |
| `&` ou `\|` sozinho | o caractere | descarta o caractere |
| Número com ponto e sem dígito depois (`3.`) | início do número | descarta `3.` |
| Identificador com mais de 32 caracteres | início do identificador | descarta o identificador |
| String não fechada até o fim da linha | aspa de abertura | descarta a string e segue na linha seguinte |
| String não fechada até o fim do arquivo | aspa de abertura | descarta a string e emite EOF |
| Escape inválido (`"a\qb"`) | a barra | reporta, mas a string continua sendo um token |
| Literal de caractere vazio (`''`), com mais de um caractere (`'ab'`) ou não fechado | aspa de abertura | descarta até a aspa que fecha ou até o fim da linha |
| Comentário de bloco não fechado | o `/*` | descarta até o fim do arquivo e emite EOF |
