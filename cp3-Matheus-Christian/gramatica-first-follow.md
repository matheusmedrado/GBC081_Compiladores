# Gramática, FIRST/FOLLOW e verificação LL(1)

Construção de Compiladores (GBC081), Checkpoint 2. Grupo Matheus-Christian.

A gramática usa exatamente os tokens da especificação léxica do CP1 (`../cp1-Matheus-Christian/especificacao-lexica.md`), sem nenhum token novo. O parser (`src/parser/Parser.java`) tem um método `parseX()` para cada não-terminal `X` da seção 1.3, na mesma ordem e com a produção copiada no comentário. Os conjuntos da seção 3 aparecem no código como as constantes `FIRST_X` e `FOLLOW_X`.

## 1. Gramática final

### 1.1 Notação

- Terminais são tokens do scanner. Palavras reservadas e símbolos aparecem entre aspas simples, do jeito que estão no fonte (`'int'`, `';'`, `'&&'`). As classes de token têm nome em minúsculas: `id` (IDENTIFIER), `int_lit`, `double_lit`, `char_lit` e `string_lit`.
- `$` é o token `EOF`, que o scanner sempre emite no fim.
- Não-terminais em PascalCase, com o mesmo nome do método (`StmtList` vira `parseStmtList()`).
- `ε` é a cadeia vazia. Nas tabelas, os terminais aparecem sem aspas.

### 1.2 Visão geral em EBNF

Esta versão é só para leitura: `{ x }` é repetição, `[ x ]` é opcional. A gramática que vale para FIRST/FOLLOW e para o parser é a BNF da seção 1.3, que reconhece a mesma linguagem.

```
Program  = { Decl } $
Decl     = Type id ( "(" [ Params ] ")" Block | VarRest )
         | "void" id "(" [ Params ] ")" Block
VarRest  = [ "=" Expr ] { "," id [ "=" Expr ] } ";"
Params   = Type id { "," Type id }
Type     = "int" | "double" | "bool" | "char" | "string"

Block    = "{" { Stmt } "}"
Stmt     = Type id VarRest
         | IfStmt
         | "while" "(" Expr ")" Block
         | "return" [ Expr ] ";"
         | Block
         | id ( "=" Expr | "(" [ Args ] ")" ) ";"
IfStmt   = "if" "(" Expr ")" Block [ "else" ( Block | IfStmt ) ]
Args     = Expr { "," Expr }

Expr     = AndExpr { "||" AndExpr }
AndExpr  = EqExpr { "&&" EqExpr }
EqExpr   = RelExpr { ( "==" | "!=" ) RelExpr }
RelExpr  = AddExpr [ ( "<" | "<=" | ">" | ">=" ) AddExpr ]
AddExpr  = MulExpr { ( "+" | "-" ) MulExpr }
MulExpr  = Unary { ( "*" | "/" | "%" ) Unary }
Unary    = ( "!" | "-" ) Unary | Primary
Primary  = id [ "(" [ Args ] ")" ] | Literal | "(" Expr ")"
Literal  = int_lit | double_lit | char_lit | string_lit | "true" | "false"
```

Precedência e associatividade dos operadores, da menor para a maior:

| Nível | Operadores | Associatividade |
|---|---|---|
| 1 | `\|\|` | esquerda |
| 2 | `&&` | esquerda |
| 3 | `==` `!=` | esquerda |
| 4 | `<` `<=` `>` `>=` | nenhuma (`a < b < c` é erro) |
| 5 | `+` `-` binários | esquerda |
| 6 | `*` `/` `%` | esquerda |
| 7 | `!` `-` unários | prefixo, podem repetir (`!!x`, `- -x`) |

A associatividade à esquerda não aparece na forma da gramática, porque a recursão à esquerda foi tirada (seção 2.2). Para um reconhecedor isso não muda nada, porque as cadeias aceitas são as mesmas. Quando o parser passar a montar a AST (Aula 7), o `AddRest` vai receber o operando da esquerda e montar a árvore da esquerda para a direita.

### 1.3 Gramática BNF (a que o parser implementa)

São 46 não-terminais. O símbolo inicial é `Program`.

```
 1  Program      → DeclList $

 2  DeclList     → Decl DeclList | ε
 3  Decl         → Type id DeclRest | 'void' id '(' Params ')' Block
 4  DeclRest     → '(' Params ')' Block | VarRest
 5  Type         → 'int' | 'double' | 'bool' | 'char' | 'string'
 6  Params       → Param ParamList | ε
 7  ParamList    → ',' Param ParamList | ε
 8  Param        → Type id

 9  Block        → '{' StmtList '}'
10  StmtList     → Stmt StmtList | ε
11  Stmt         → VarDecl | IfStmt | WhileStmt | ReturnStmt | Block | id AssignOrCall ';'
12  VarDecl      → Type id VarRest
13  VarRest      → VarInit VarList ';'
14  VarInit      → '=' Expr | ε
15  VarList      → ',' id VarInit VarList | ε
16  IfStmt       → 'if' '(' Expr ')' Block ElsePart
17  ElsePart     → 'else' ElseBody | ε
18  ElseBody     → Block | IfStmt
19  WhileStmt    → 'while' '(' Expr ')' Block
20  ReturnStmt   → 'return' ReturnValue ';'
21  ReturnValue  → Expr | ε
22  AssignOrCall → '=' Expr | '(' Args ')'
23  Args         → Expr ArgList | ε
24  ArgList      → ',' Expr ArgList | ε

25  Expr         → OrExpr
26  OrExpr       → AndExpr OrRest
27  OrRest       → '||' AndExpr OrRest | ε
28  AndExpr      → EqExpr AndRest
29  AndRest      → '&&' EqExpr AndRest | ε
30  EqExpr       → RelExpr EqRest
31  EqRest       → EqOp RelExpr EqRest | ε
32  EqOp         → '==' | '!='
33  RelExpr      → AddExpr RelRest
34  RelRest      → RelOp AddExpr | ε
35  RelOp        → '<' | '<=' | '>' | '>='
36  AddExpr      → MulExpr AddRest
37  AddRest      → AddOp MulExpr AddRest | ε
38  AddOp        → '+' | '-'
39  MulExpr      → Unary MulRest
40  MulRest      → MulOp Unary MulRest | ε
41  MulOp        → '*' | '/' | '%'
42  Unary        → UnaryOp Unary | Primary
43  UnaryOp      → '!' | '-'
44  Primary      → id CallSuffix | Literal | '(' Expr ')'
45  CallSuffix   → '(' Args ')' | ε
46  Literal      → int_lit | double_lit | char_lit | string_lit | 'true' | 'false'
```

### 1.4 Decisões de linguagem que aparecem na gramática

- **O corpo de `if`, `else` e `while` é sempre um bloco com chaves.** `if (x > 0) x = 0;` é erro sintático. `else if` continua existindo, por causa de `ElseBody → IfStmt`. O motivo está na seção 2.4.
- **Atribuição é comando, não expressão.** `x = y = 0;` e `if (x = 1)` são erros sintáticos. Seção 2.4.
- **Só atribuição e chamada podem ser comando começando com `id`.** Uma expressão solta como `x + 1;` não é comando, porque não teria efeito nenhum. Também não existe comando vazio (`;` sozinho).
- **Chamada de função** pode ser comando (`imprime(x);`) ou fazer parte de uma expressão (`fib(n - 1) + fib(n - 2)`).
- **Relacionais não se encadeiam:** `RelRest → RelOp AddExpr | ε` não repete. `a < b < c` compararia um `bool` com um número, então preferimos rejeitar já na sintaxe.
- **Declarações de variável** podem ser globais ou locais, em qualquer ponto do bloco, com vários nomes e inicialização opcional: `int a, b = 2, c;`.
- **`void` só como tipo de retorno.** Não existe variável nem parâmetro `void`. Função sem parâmetros é `()`.
- **Literais numéricos não têm sinal** (decisão do CP1). `-1` é `UnaryOp` seguido de `Literal`. É isso que deixa `FIRST(UnaryOp)` e `FIRST(Primary)` disjuntos em `Unary`.
- **Ficam para a análise semântica:** tipos compatíveis, `return` com ou sem valor conforme o tipo da função, existência de `main` e de funções chamadas, e declaração antes do uso. Por isso o programa vazio é aceito pela gramática.

## 2. Da primeira versão à gramática final

### 2.1 A primeira versão

A primeira gramática que escrevemos seguia de perto a de C. As partes que tiveram que mudar estavam assim (as outras produções já eram como as da seção 1.3):

```
DeclList  → DeclList Decl | ε
Decl      → FuncDecl | VarDecl
FuncDecl  → Type id '(' Params ')' Block | 'void' id '(' Params ')' Block
VarDecl   → Type id VarInit VarList ';'
Params    → ParamList | ε
ParamList → ParamList ',' Param | Param
VarList   → VarList ',' id VarInit | ε

StmtList  → StmtList Stmt | ε
Stmt      → VarDecl | IfStmt | WhileStmt | ReturnStmt | Block | Expr ';'
IfStmt    → 'if' '(' Expr ')' Stmt | 'if' '(' Expr ')' Stmt 'else' Stmt
WhileStmt → 'while' '(' Expr ')' Stmt

Expr      → id '=' Expr | OrExpr
OrExpr    → OrExpr '||' AndExpr | AndExpr
AndExpr   → AndExpr '&&' EqExpr | EqExpr
EqExpr    → EqExpr EqOp RelExpr | RelExpr
RelExpr   → RelExpr RelOp AddExpr | AddExpr
AddExpr   → AddExpr AddOp MulExpr | MulExpr
MulExpr   → MulExpr MulOp Unary | Unary
Primary   → id | id '(' Args ')' | Literal | '(' Expr ')'
Args      → ArgList | ε
ArgList   → ArgList ',' Expr | Expr
```

Ela é a gramática natural para a linguagem (e já tem a precedência certa), mas não serve para um parser descendente. Os problemas estão abaixo, na ordem em que corrigimos.

### 2.2 Recursão à esquerda

Havia recursão à esquerda **direta** em onze não-terminais: `DeclList`, `StmtList`, `ParamList`, `VarList`, `ArgList`, `OrExpr`, `AndExpr`, `EqExpr`, `RelExpr`, `AddExpr` e `MulExpr`. Com ela, `parseAddExpr()` chamaria `parseAddExpr()` sem consumir nada, ou seja, recursão infinita. A transformação da aula troca

```
A → A α | β          por          A  → β A'
                                  A' → α A' | ε
```

Nas expressões, por exemplo:

```
antes:   AddExpr → AddExpr AddOp MulExpr | MulExpr
depois:  AddExpr → MulExpr AddRest
         AddRest → AddOp MulExpr AddRest | ε
```

`OrExpr`, `AndExpr`, `EqExpr` e `MulExpr` ficaram iguais, com `OrRest`, `AndRest`, `EqRest` e `MulRest`. Em `RelExpr` aproveitamos para tirar a repetição (seção 1.4): `RelRest → RelOp AddExpr | ε`.

Nas listas:

```
antes:   DeclList  → DeclList Decl | ε               (β = ε)
depois:  DeclList  → Decl DeclList | ε               (A' é o próprio DeclList)

antes:   Params    → ParamList | ε
         ParamList → ParamList ',' Param | Param
depois:  Params    → Param ParamList | ε
         ParamList → ',' Param ParamList | ε         (ParamList agora é o "resto" da lista)
```

`StmtList` saiu igual a `DeclList`. `VarList` e `ArgList` saíram igual a `ParamList`, e `Args → ArgList | ε` virou `Args → Expr ArgList | ε`.

**Recursão indireta.** Conferimos que não existe nenhum ciclo `A ⇒+ A γ`. Na gramática final, o primeiro símbolo de cada produção é um terminal ou um não-terminal "mais baixo" numa ordem fixa: `Program` › `DeclList` › `Decl` › `Type`, `Params` › `Param` › `Type`, `StmtList` › `Stmt` › `VarDecl`/`Block`/... e `Expr` › `OrExpr` › `AndExpr` › ... › `Unary` › `Primary` › `Literal`. Os únicos caminhos que voltam para um não-terminal "mais alto" passam antes por um terminal: `Primary → '(' Expr ')'`, `Block → '{' StmtList '}'` e `ElseBody → IfStmt`, que só é alcançado depois de `'else'`.

### 2.3 Fatoração à esquerda

Três lugares tinham alternativas com o mesmo prefixo. Nenhum pode ser decidido olhando um token só.

**Declarações no nível global.** `FuncDecl → Type id '(' ...` e `VarDecl → Type id ...` começam os dois com `Type id`. `FIRST` das duas alternativas de `Decl` é `TIPO` nos dois casos, violando a cláusula (a). O parser só sabe se é função ou variável no terceiro token.

```
antes:   Decl     → FuncDecl | VarDecl
         FuncDecl → Type id '(' Params ')' Block | 'void' id '(' Params ')' Block
         VarDecl  → Type id VarInit VarList ';'
depois:  Decl     → Type id DeclRest | 'void' id '(' Params ')' Block
         DeclRest → '(' Params ')' Block | VarRest
         VarDecl  → Type id VarRest                  (declaração local, dentro de bloco)
         VarRest  → VarInit VarList ';'
```

`VarRest` é compartilhado entre a declaração global (por `DeclRest`) e a local (por `VarDecl`).

**Chamada em expressão.** `Primary → id | id '(' Args ')'`:

```
antes:   Primary    → id | id '(' Args ')' | Literal | '(' Expr ')'
depois:  Primary    → id CallSuffix | Literal | '(' Expr ')'
         CallSuffix → '(' Args ')' | ε
```

**`if` com e sem `else`.** As duas alternativas de `IfStmt` têm o prefixo `'if' '(' Expr ')' Stmt`:

```
antes:   IfStmt   → 'if' '(' Expr ')' Stmt | 'if' '(' Expr ')' Stmt 'else' Stmt
depois:  IfStmt   → 'if' '(' Expr ')' Stmt ElsePart
         ElsePart → 'else' Stmt | ε
```

Essa fatoração está certa, mas deixou um conflito LL(1), que é o segundo caso da seção 2.4.

### 2.4 Violações LL(1) que sobraram, e a correção de cada uma

Depois de 2.2 e 2.3, calculamos FIRST/FOLLOW e verificamos as três cláusulas. Dois não-terminais ainda falhavam.

**(1) `Expr`: atribuição como expressão, cláusula (a).** Em `Expr → id '=' Expr | OrExpr`, temos `FIRST(id '=' Expr) = { id }`, e `id ∈ FIRST(OrExpr)` porque `OrExpr ⇒ AndExpr ⇒ ... ⇒ Unary ⇒ Primary ⇒ id CallSuffix`. Com lookahead `id`, o parser não sabe se é `x = 1` ou `x + 1`. Fatorar não resolve de forma razoável: o `id` de `OrExpr` está oito níveis abaixo, e trazê-lo para cima desmontaria a hierarquia de precedência.

Correção: atribuição deixou de ser expressão e virou comando, como em Pascal ou Go. Isso criou o mesmo conflito em `Stmt`: com `Stmt → id '=' Expr ';' | Expr ';'`, as duas alternativas têm `id` no FIRST. Restringimos então o comando-expressão ao único caso útil, a chamada, e fatoramos:

```
antes:   Stmt → ... | Expr ';'
         Expr → id '=' Expr | OrExpr
depois:  Stmt         → ... | id AssignOrCall ';'
         AssignOrCall → '=' Expr | '(' Args ')'
         Expr         → OrExpr
```

Agora `FIRST(AssignOrCall)` tem `{ = }` e `{ ( }` nas alternativas. Em `Stmt`, `id` só aparece numa alternativa (os tipos são palavras reservadas, não identificadores, então `VarDecl` começa por `TIPO`).

**(2) `ElsePart`: o `else` pendurado, cláusula (c).** Com `IfStmt → 'if' '(' Expr ')' Stmt ElsePart` e `ElsePart → 'else' Stmt | ε`:

- `FIRST('else' Stmt) = { else }`;
- como `Stmt` aparece antes de `ElsePart`, `FOLLOW(Stmt) ⊇ FIRST(ElsePart) − {ε} = { else }`;
- como `ElsePart` é o fim de `IfStmt` e `IfStmt` é um `Stmt`, `FOLLOW(ElsePart) ⊇ FOLLOW(IfStmt) ⊇ FOLLOW(Stmt)`, que contém `else`.

Então `else ∈ FIRST('else' Stmt) ∩ FOLLOW(ElsePart)`, e a célula `M[ElsePart, else]` da tabela teria duas produções. É a ambiguidade de `if (a) if (b) x = 1; else x = 2;`: o `else` pode ser do primeiro `if` ou do segundo.

Pensamos em três saídas:

1. Manter a gramática e decidir no código que o `else` fica com o `if` mais próximo (preferir a alternativa não vazia). É o que muitos compiladores fazem e funcionaria, mas aí a gramática entregue não seria LL(1). O parser estaria tomando uma decisão que a tabela não justifica.
2. Usar a gramática sem ambiguidade com `MatchedStmt`/`UnmatchedStmt`. Ela tira a ambiguidade mas continua sem ser LL(1): as duas formas começam com `'if' '(' Expr ')'`, e fatorar isso traz de volta o mesmo conflito.
3. **Exigir chaves no corpo** de `if`, `else` e `while`, como em Go, Rust e Swift. Foi a escolhida.

```
antes:   IfStmt    → 'if' '(' Expr ')' Stmt ElsePart
         ElsePart  → 'else' Stmt | ε
         WhileStmt → 'while' '(' Expr ')' Stmt
depois:  IfStmt    → 'if' '(' Expr ')' Block ElsePart
         ElsePart  → 'else' ElseBody | ε
         ElseBody  → Block | IfStmt
         WhileStmt → 'while' '(' Expr ')' Block
```

Agora o que vem antes de `ElsePart` é um `Block`, então o `else` entra em `FOLLOW(Block)`, e isso não causa conflito porque `Block` tem uma produção só. Já `FOLLOW(ElsePart) = FOLLOW(IfStmt) = FOLLOW(Stmt) = CMD ∪ { } }`, sem `else`. Com a chave obrigatória, o `}` diz de qual `if` é cada `else`. `ElseBody → IfStmt` mantém o `else if` sem chave extra. `FIRST(Block) = { { }` e `FIRST(IfStmt) = { if }` são disjuntos. O `while` passou a exigir bloco também, para a regra ser uma só.

## 3. FIRST e FOLLOW

Abreviações usadas na tabela:

| Nome | Conjunto |
|---|---|
| TIPO | `{ int double bool char string }` |
| LIT | `{ int_lit double_lit char_lit string_lit true false }` |
| EXPR | `FIRST(Expr)` = `{ ! - id ( }` ∪ LIT |
| CMD | `FIRST(Stmt)` = TIPO ∪ `{ if while return { id }` |
| FIM | `{ ) ; , }`, o que pode vir depois de uma expressão completa |

| # | Não-terminal | FIRST | FOLLOW |
|---|---|---|---|
| 1 | Program | TIPO ∪ `{ void $ }` | (símbolo inicial; o `$` já está na produção) |
| 2 | DeclList | TIPO ∪ `{ void ε }` | `{ $ }` |
| 3 | Decl | TIPO ∪ `{ void }` | TIPO ∪ `{ void $ }` |
| 4 | DeclRest | `{ ( = , ; }` | TIPO ∪ `{ void $ }` |
| 5 | Type | TIPO | `{ id }` |
| 6 | Params | TIPO ∪ `{ ε }` | `{ ) }` |
| 7 | ParamList | `{ , ε }` | `{ ) }` |
| 8 | Param | TIPO | `{ , ) }` |
| 9 | Block | `{ { }` | CMD ∪ `{ } else void $ }` |
| 10 | StmtList | CMD ∪ `{ ε }` | `{ } }` |
| 11 | Stmt | CMD | CMD ∪ `{ } }` |
| 12 | VarDecl | TIPO | CMD ∪ `{ } }` |
| 13 | VarRest | `{ = , ; }` | CMD ∪ `{ } void $ }` |
| 14 | VarInit | `{ = ε }` | `{ , ; }` |
| 15 | VarList | `{ , ε }` | `{ ; }` |
| 16 | IfStmt | `{ if }` | CMD ∪ `{ } }` |
| 17 | ElsePart | `{ else ε }` | CMD ∪ `{ } }` |
| 18 | ElseBody | `{ { if }` | CMD ∪ `{ } }` |
| 19 | WhileStmt | `{ while }` | CMD ∪ `{ } }` |
| 20 | ReturnStmt | `{ return }` | CMD ∪ `{ } }` |
| 21 | ReturnValue | EXPR ∪ `{ ε }` | `{ ; }` |
| 22 | AssignOrCall | `{ = ( }` | `{ ; }` |
| 23 | Args | EXPR ∪ `{ ε }` | `{ ) }` |
| 24 | ArgList | `{ , ε }` | `{ ) }` |
| 25 | Expr | EXPR | FIM |
| 26 | OrExpr | EXPR | FIM |
| 27 | OrRest | `{ \|\| ε }` | FIM |
| 28 | AndExpr | EXPR | `{ \|\| }` ∪ FIM |
| 29 | AndRest | `{ && ε }` | `{ \|\| }` ∪ FIM |
| 30 | EqExpr | EXPR | `{ && \|\| }` ∪ FIM |
| 31 | EqRest | `{ == != ε }` | `{ && \|\| }` ∪ FIM |
| 32 | EqOp | `{ == != }` | EXPR |
| 33 | RelExpr | EXPR | `{ == != && \|\| }` ∪ FIM |
| 34 | RelRest | `{ < <= > >= ε }` | `{ == != && \|\| }` ∪ FIM |
| 35 | RelOp | `{ < <= > >= }` | EXPR |
| 36 | AddExpr | EXPR | `{ < <= > >= == != && \|\| }` ∪ FIM |
| 37 | AddRest | `{ + - ε }` | `{ < <= > >= == != && \|\| }` ∪ FIM |
| 38 | AddOp | `{ + - }` | EXPR |
| 39 | MulExpr | EXPR | `{ + - < <= > >= == != && \|\| }` ∪ FIM |
| 40 | MulRest | `{ * / % ε }` | `{ + - < <= > >= == != && \|\| }` ∪ FIM |
| 41 | MulOp | `{ * / % }` | EXPR |
| 42 | Unary | EXPR | `{ * / % + - < <= > >= == != && \|\| }` ∪ FIM |
| 43 | UnaryOp | `{ ! - }` | EXPR |
| 44 | Primary | `{ id ( }` ∪ LIT | `{ * / % + - < <= > >= == != && \|\| }` ∪ FIM |
| 45 | CallSuffix | `{ ( ε }` | `{ * / % + - < <= > >= == != && \|\| }` ∪ FIM |
| 46 | Literal | LIT | `{ * / % + - < <= > >= == != && \|\| }` ∪ FIM |

### Como alguns FOLLOW saíram

Os que mais aparecem nas perguntas, passo a passo:

- **FOLLOW(Expr) = FIM.** `Expr` aparece em `VarInit → '=' Expr` (recebe `FOLLOW(VarInit) = { , ; }`), em `'(' Expr ')'` de `IfStmt`, `WhileStmt` e `Primary` (recebe `)`), em `ReturnValue → Expr` e `AssignOrCall → '=' Expr` (recebe `;`), e em `Args → Expr ArgList` e `ArgList → ',' Expr ArgList` (recebe `FIRST(ArgList) − ε = { , }` e, como `ArgList` é anulável, `FOLLOW(Args) = { ) }`).
- **A cadeia das expressões.** Em `AndExpr → EqExpr AndRest`, o `FOLLOW(EqExpr)` recebe `FIRST(AndRest) − ε = { && }` e, como `AndRest` é anulável, também `FOLLOW(AndExpr)`. Cada nível herda o FOLLOW do nível de cima e soma os operadores do próprio nível. Por isso a tabela cresce de cima para baixo até `Unary`. O `Rest` de cada nível tem o mesmo FOLLOW do seu `Expr` (`AndRest` está no fim de `AndExpr → EqExpr AndRest` e no fim da própria produção recursiva).
- **FOLLOW(Block).** `Block` aparece no fim de `Decl` e de `DeclRest` (recebe `TIPO ∪ { void $ }`), no fim de `WhileStmt`, `Stmt` e `ElseBody` (recebe `CMD ∪ { } }`), e antes de `ElsePart` em `IfStmt` (recebe `FIRST(ElsePart) − ε = { else }` e, como `ElsePart` é anulável, `FOLLOW(IfStmt)`).
- **FOLLOW(Stmt) = CMD ∪ { } }.** De `StmtList → Stmt StmtList` vêm `FIRST(StmtList) − ε = CMD` e, como `StmtList` é anulável, `FOLLOW(StmtList) = { } }`. `VarDecl`, `IfStmt`, `WhileStmt` e `ReturnStmt` estão no fim de `Stmt` e herdam esse conjunto. `IfStmt` também está no fim de `ElseBody`, mas `FOLLOW(ElseBody) = FOLLOW(ElsePart) = FOLLOW(IfStmt)`, então não acrescenta nada.

## 4. Verificação da condição LL(1)

Para cada não-terminal, com as alternativas `α1 | α2 | ...`:

- **(a)** os `FIRST(αi)` são disjuntos dois a dois;
- **(b)** no máximo uma alternativa é anulável;
- **(c)** se uma alternativa é anulável, o FIRST das outras é disjunto de `FOLLOW(A)`.

### 4.1 Não-terminais com mais de uma alternativa

| Não-terminal | Alternativas e FIRST de cada uma | (a) | (b) | (c) |
|---|---|---|---|---|
| DeclList | `Decl DeclList`: TIPO ∪ `{void}` · `ε` | uma só não vazia | só `ε` | TIPO ∪ `{void}` ∩ `{$}` = ∅ |
| Decl | `Type id DeclRest`: TIPO · `'void' ...`: `{void}` | TIPO ∩ `{void}` = ∅ | nenhuma | não se aplica |
| DeclRest | `'(' ...`: `{(}` · `VarRest`: `{= , ;}` | disjuntos | nenhuma (`VarRest` termina em `;`) | não se aplica |
| Type | cinco terminais diferentes | disjuntos | nenhuma | não se aplica |
| Params | `Param ParamList`: TIPO · `ε` | uma só não vazia | só `ε` | TIPO ∩ `{)}` = ∅ |
| ParamList | `',' ...`: `{,}` · `ε` | uma só não vazia | só `ε` | `{,}` ∩ `{)}` = ∅ |
| StmtList | `Stmt StmtList`: CMD · `ε` | uma só não vazia | só `ε` | CMD ∩ `{ } }` = ∅ |
| Stmt | `VarDecl`: TIPO · `IfStmt`: `{if}` · `WhileStmt`: `{while}` · `ReturnStmt`: `{return}` · `Block`: `{ { }` · `id ...`: `{id}` | disjuntos (tipos são palavras reservadas, não `id`) | nenhuma | não se aplica |
| VarInit | `'=' Expr`: `{=}` · `ε` | uma só não vazia | só `ε` | `{=}` ∩ `{, ;}` = ∅ |
| VarList | `',' ...`: `{,}` · `ε` | uma só não vazia | só `ε` | `{,}` ∩ `{;}` = ∅ |
| ElsePart | `'else' ElseBody`: `{else}` · `ε` | uma só não vazia | só `ε` | `{else}` ∩ (CMD ∪ `{ } }`) = ∅ (era o conflito da seção 2.4) |
| ElseBody | `Block`: `{ { }` · `IfStmt`: `{if}` | disjuntos | nenhuma | não se aplica |
| ReturnValue | `Expr`: EXPR · `ε` | uma só não vazia | só `ε` | EXPR ∩ `{;}` = ∅ |
| AssignOrCall | `'=' Expr`: `{=}` · `'(' Args ')'`: `{(}` | disjuntos | nenhuma | não se aplica |
| Args | `Expr ArgList`: EXPR · `ε` | uma só não vazia | só `ε` | EXPR ∩ `{)}` = ∅ |
| ArgList | `',' ...`: `{,}` · `ε` | uma só não vazia | só `ε` | `{,}` ∩ `{)}` = ∅ |
| OrRest | `'\|\|' ...`: `{\|\|}` · `ε` | uma só não vazia | só `ε` | `{\|\|}` ∩ FIM = ∅ |
| AndRest | `'&&' ...`: `{&&}` · `ε` | uma só não vazia | só `ε` | `{&&}` ∩ (`{\|\|}` ∪ FIM) = ∅ |
| EqRest | `EqOp ...`: `{== !=}` · `ε` | uma só não vazia | só `ε` | `{== !=}` ∩ (`{&& \|\|}` ∪ FIM) = ∅ |
| EqOp | `'=='` · `'!='` | disjuntos | nenhuma | não se aplica |
| RelRest | `RelOp AddExpr`: `{< <= > >=}` · `ε` | uma só não vazia | só `ε` | `{< <= > >=}` ∩ (`{== != && \|\|}` ∪ FIM) = ∅ |
| RelOp | quatro terminais diferentes | disjuntos | nenhuma | não se aplica |
| AddRest | `AddOp ...`: `{+ -}` · `ε` | uma só não vazia | só `ε` | `{+ -}` ∩ (`{< <= > >= == != && \|\|}` ∪ FIM) = ∅ |
| AddOp | `'+'` · `'-'` | disjuntos | nenhuma | não se aplica |
| MulRest | `MulOp ...`: `{* / %}` · `ε` | uma só não vazia | só `ε` | `{* / %}` ∩ (`{+ - < <= > >= == != && \|\|}` ∪ FIM) = ∅ |
| MulOp | três terminais diferentes | disjuntos | nenhuma | não se aplica |
| Unary | `UnaryOp Unary`: `{! -}` · `Primary`: `{id (}` ∪ LIT | disjuntos (literal não tem sinal) | nenhuma | não se aplica |
| UnaryOp | `'!'` · `'-'` | disjuntos | nenhuma | não se aplica |
| Primary | `id CallSuffix`: `{id}` · `Literal`: LIT · `'(' Expr ')'`: `{(}` | disjuntos | nenhuma | não se aplica |
| CallSuffix | `'(' Args ')'`: `{(}` · `ε` | uma só não vazia | só `ε` | `{(}` ∩ FOLLOW(CallSuffix) = ∅ (o FOLLOW só tem operadores binários e FIM) |
| Literal | seis terminais diferentes | disjuntos | nenhuma | não se aplica |

O `-` aparece em `FIRST(UnaryOp)` e em `FIRST(AddOp)`, mas em não-terminais diferentes. Quem olha o `-` como possível início de `AddRest` é o parser depois de um operando completo. Quem olha como `UnaryOp` é o parser no começo de um operando. Nunca existe um ponto em que as duas leituras estejam em disputa.

### 4.2 Não-terminais com uma produção só

`Program`, `Param`, `Block`, `VarDecl`, `VarRest`, `IfStmt`, `WhileStmt`, `ReturnStmt`, `Expr`, `OrExpr`, `AndExpr`, `EqExpr`, `RelExpr`, `AddExpr` e `MulExpr` têm uma única produção, e nenhuma delas deriva `ε`. As cláusulas (a) e (b) valem trivialmente e (c) não se aplica. O parser não toma decisão nesses métodos, só casa e chama na ordem da produção.

### 4.3 Conclusão

Os 46 não-terminais passam nas três cláusulas: cada célula `M[A, a]` da tabela LL(1) tem no máximo uma produção. Os conflitos que existiam na primeira versão (recursão à esquerda, prefixos comuns em `Decl`, `Primary` e `IfStmt`, atribuição como expressão e o `else` pendurado) estão documentados na seção 2 com a correção aplicada.

## 5. Da tabela para o código

Cada decisão do parser é uma consulta a esta tabela, sempre no mesmo formato. Usando `ElsePart` como exemplo:

```java
// ElsePart → 'else' ElseBody | ε
private void parseElsePart() {
    if (la() == ELSE) {                              // lookahead ∈ FIRST('else' ElseBody)
        match(ELSE);
        parseElseBody();
    } else if (FOLLOW_ELSE_PART.contains(la())) {    // ε só com lookahead ∈ FOLLOW(ElsePart)
        // ε
    } else {                                         // fora de FIRST e de FOLLOW: erro
        syntaxError("ElsePart", union(EnumSet.of(ELSE), FOLLOW_ELSE_PART), sync(FOLLOW_ELSE_PART));
    }
}
```

A versão ingênua do ε seria só o primeiro `if` com um `else` vazio. Ela aceitaria qualquer token depois do bloco e deixaria o erro para alguém mais acima, mais longe de onde ele está. O teste "comparação encadeada" da suíte mostra a diferença: em `a < b < c`, o segundo `<` não está em `FIRST(EqRest)` nem em `FOLLOW(EqRest)`, e o erro sai exatamente nele. O que acontece depois do erro (sincronização, âncoras, modo pânico) está no `README.md`.
