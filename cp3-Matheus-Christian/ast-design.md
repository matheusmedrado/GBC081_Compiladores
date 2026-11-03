# Projeto da AST

Construção de Compiladores (GBC081), Checkpoint 3. Grupo Matheus-Christian.

A hierarquia foi desenhada antes das classes, a partir da gramática do CP2 (`gramatica-first-follow.md`, que não mudou). O critério da Aula 7 é um tipo de nó por categoria sintática que importa para as fases seguintes, não um tipo por não-terminal.

## 1. Hierarquia de nós

`Node` é a classe abstrata raiz: guarda a posição (linha e coluna do primeiro token que originou o nó) e declara o `accept` abstrato. `Decl`, `Stmt` e `Expr` são as classes abstratas que agrupam as categorias. `VarDecl` é ao mesmo tempo uma `Decl` (pode ser global) e um comando (pode ser local), por isso estende `Decl` e implementa `StmtNode`, a interface do que pode aparecer dentro de um bloco (`Stmt` também a implementa).

| Nó | Atributos | Produção (ou produções) que o gera |
|---|---|---|
| `Program` | `declarations: List<Decl>` | `Program → DeclList $` |
| `VarDecl` | `type: TypeNode`, `variables: List<Var>` | `Decl → Type id DeclRest` com `DeclRest → VarRest`; `Stmt → VarDecl` |
| `Var` | `name: String`, `initializer: Expr` (null se não há `=`) | `VarInit → '=' Expr \| ε`, uma vez por nome de `VarRest` e de `VarList` |
| `FuncDecl` | `returnType: TypeNode` (null se `void`), `name: String`, `parameters: List<Param>`, `body: Block` | `Decl → Type id DeclRest` com `DeclRest → '(' Params ')' Block`; `Decl → 'void' id '(' Params ')' Block` |
| `Param` | `type: TypeNode`, `name: String` | `Param → Type id` |
| `TypeNode` | `name: String` | `Type → 'int' \| 'double' \| 'bool' \| 'char' \| 'string'` |
| `Block` | `statements: List<StmtNode>` | `Block → '{' StmtList '}'` |
| `Assign` | `name: String`, `value: Expr` | `Stmt → id AssignOrCall ';'` com `AssignOrCall → '=' Expr` |
| `CallStmt` | `call: Call` | `Stmt → id AssignOrCall ';'` com `AssignOrCall → '(' Args ')'` |
| `If` | `condition: Expr`, `thenBranch: Block`, `elseBranch: StmtNode` (null se não há else) | `IfStmt → 'if' '(' Expr ')' Block ElsePart` |
| `While` | `condition: Expr`, `body: Block` | `WhileStmt → 'while' '(' Expr ')' Block` |
| `Return` | `value: Expr` (null no `return;`) | `ReturnStmt → 'return' ReturnValue ';'` |
| `Bin` | `operator: String`, `left: Expr`, `right: Expr` | os níveis binários: `OrRest`, `AndRest`, `EqRest`, `RelRest`, `AddRest`, `MulRest` |
| `Unary` | `operator: String`, `operand: Expr` | `Unary → UnaryOp Unary` |
| `Id` | `name: String` | `Primary → id CallSuffix` com `CallSuffix → ε` |
| `Call` | `name: String`, `arguments: List<Expr>` | `Primary → id CallSuffix` com `CallSuffix → '(' Args ')'` |
| `Literal` | `kind: String` (`int`, `double`, `char`, `string`, `bool`), `value: String` (o lexema) | `Literal → int_lit \| double_lit \| char_lit \| string_lit \| 'true' \| 'false'` |

### Decisões de representação

- **Uma declaração com vários nomes** (`int a, b = 2, c;`) é um `VarDecl` só, com um `Var` por nome.
- **A declaração de variável global e a local são o mesmo nó**, `VarDecl`, pelo motivo acima.
- **Procedimento e função são o mesmo nó**, `FuncDecl`. No procedimento, `returnType` é null.
- **O `else if` não tem nó próprio**: o `elseBranch` de um `If` é outro `If`.
- **Atribuição e chamada como comando são nós diferentes** (`Assign` e `CallStmt`), porque atribuição não é expressão nesta linguagem. `CallStmt` guarda um `Call`, para a chamada ser o mesmo nó como comando e dentro de uma expressão.
- **Um `Bin` só para todos os operadores binários**, com o operador como texto. A precedência e a associatividade ficam na forma da árvore, não no tipo do nó. O mesmo vale para `Unary`.
- **`Literal` guarda o lexema** do jeito que está no fonte e o tipo básico em `kind`. Converter o valor (por exemplo, `'\n'` para o caractere) fica para a fase que precisar dele.
- **Com erro, não há árvore**: `parse()` devolve um `ParseResult` com a árvore nula e a lista de erros, porque uma árvore montada no meio da recuperação não pode seguir para as fases seguintes.

### Campos que entram depois, sem mudar a forma da árvore

- Na aula da tabela de símbolos, `Id`, `Var`, `Param` e `FuncDecl` ganham um ponteiro para o símbolo. São atributos novos; nenhum filho muda.
- Na análise semântica, `Expr` ganha o tipo calculado. Como `Bin`, `Unary`, `Id`, `Call` e `Literal` estendem `Expr`, o campo aparece em todos sem tocar em cada classe.

## 2. Mapeamento da gramática para a árvore

**Vira nó (12).** `Program`, `Param`, `Block`, `VarDecl` e `Literal` viram o nó de mesmo nome. `Type` vira `TypeNode`. `IfStmt`, `WhileStmt` e `ReturnStmt` viram `If`, `While` e `Return`. `Unary` vira `Unary` na alternativa `UnaryOp Unary` (na outra, é o nó do `Primary`). `Decl` vira um `FuncDecl` ou um `VarDecl`, e `Stmt` vira um dos comandos; os dois também dão nome às classes abstratas que agrupam esses nós.

Os outros 34 dos 46 não-terminais:

**Vira atributo de outro nó (15).** `Params` e `ParamList` viram a lista `parameters` de `FuncDecl`. `Args` e `ArgList` viram a lista `arguments` de `Call`. `VarList` vira a lista `variables` de `VarDecl`, um `Var` por nome. `VarInit` vira o `initializer` de `Var`. `ReturnValue` vira o `value` de `Return`. `ElsePart` vira o `elseBranch` de `If`. `EqOp`, `RelOp`, `AddOp`, `MulOp` e `UnaryOp` viram o `operator` do `Bin` ou do `Unary`. `DeclList` e `StmtList` viram as listas `declarations` e `statements`.

**Não vira nada (19).** Três casos, os mesmos da Aula 7:

- **(a) Não-terminais criados para a precedência.** `Expr`, `OrExpr`, `AndExpr`, `EqExpr`, `RelExpr`, `AddExpr`, `MulExpr` e `Primary` só empurram a análise um nível para baixo. A precedência fica na forma da árvore (o `*` fica mais fundo que o `+`), então o nível em si não precisa de nó.
- **(b) Não-terminais criados para eliminar a recursão à esquerda.** `OrRest`, `AndRest`, `EqRest`, `AddRest` e `MulRest` recebem o operando da esquerda já construído e devolvem o `Bin` montado, ou o próprio operando quando a alternativa é vazia. `RelRest` faz o mesmo, mas sem repetir, porque os relacionais não se encadeiam.
- **(c) Não-terminais criados pela fatoração.** `DeclRest` só escolhe entre `FuncDecl` e `VarDecl`. `VarRest` é o trecho depois do nome, comum a `DeclRest` e `VarDecl`: o `VarDecl` é montado nele, com o tipo e o nome que ele recebe. `AssignOrCall` escolhe entre `Assign` e `CallStmt`. `CallSuffix` escolhe entre `Call` e `Id`. `ElseBody` escolhe entre um `Block` e outro `If`. Nenhum deles carrega informação que o nó montado já não tenha.

Parênteses, vírgulas e ponto e vírgula também não viram nó: os parênteses só mudam o agrupamento, que a forma da árvore já registra, e os separadores só existem para o parser saber onde uma construção termina.

## 3. Posição

Todo nó guarda `line` e `column` do primeiro token que o originou. O `Token` do CP1 já tem os dois campos, então nada mudou no scanner. No `Bin` a posição é a do operador; nos outros nós, a da palavra reservada ou do primeiro token da construção (`if`, `while`, `return`, o tipo, o nome). A análise semântica vai reportar erros com essa posição, e guardá-la agora evita ter que refazer a árvore depois.

## 4. Formato do impressor

`TreePrinter` escreve um nó por linha. A indentação é de dois espaços por nível, e os atributos que não são filhos aparecem entre parênteses, ao lado do nome. Filhos opcionais que estão ausentes (inicialização, else, valor de retorno, tipo de retorno `void`) simplesmente não aparecem, então duas árvores diferentes nunca produzem o mesmo texto. A posição não entra no texto: o arquivo esperado compara a forma da árvore, não a coluna.

```
Bin(-)
  Bin(-)
    Id(a)
    Id(b)
  Bin(*)
    Id(c)
    Id(d)
```

`Literal` mostra o tipo e o lexema: `Literal(char, '\n')`, `Literal(string, "ok: 5! = 120")`, `Literal(bool, true)`.

## 5. A árvore de um programa curto

O programa (uma função, uma seleção e uma repetição, 35 nós):

```
int abs(int n) {
    int r = n;
    if (n < 0) {
        r = -n;
    }
    return r;
}

void main() {
    int i = 3;
    while (i > 0) {
        i = i - 1;
    }
}
```

```
Program
  FuncDecl(abs)
    Type(int)
    Param(n)
      Type(int)
    Block
      VarDecl
        Type(int)
        Var(r)
          Id(n)
      If
        Bin(<)
          Id(n)
          Literal(int, 0)
        Block
          Assign(r)
            Unary(-)
              Id(n)
      Return
        Id(r)
  FuncDecl(main)
    Block
      VarDecl
        Type(int)
        Var(i)
          Literal(int, 3)
      While
        Bin(>)
          Id(i)
          Literal(int, 0)
        Block
          Assign(i)
            Bin(-)
              Id(i)
              Literal(int, 1)
```

Três pedaços da árvore de derivação que a AST descartou:

1. Do `Expr` da condição `n < 0` até o `n`, a árvore de derivação desce nove produções (`Expr → OrExpr → AndExpr → EqExpr → RelExpr → AddExpr → MulExpr → Unary → Primary → id`), fora os `Rest` vazios de cada nível. Na AST a condição é um `Bin(<)` direto, com o `Id(n)` como filho da esquerda.
2. O `-n` deriva por `Unary → UnaryOp → '-'` e `Unary → Primary → id`. O `UnaryOp`, que existe para fatorar os dois operadores unários, não vira nó: vira o atributo `operator` do `Unary`.
3. O `while (i > 0)` tem os parênteses da produção `WhileStmt → 'while' '(' Expr ')' Block`. Eles não viram nó, como nenhuma vírgula nem ponto e vírgula do programa.

## 6. O segundo visitor

`SourcePrinter` reimprime o programa na sintaxe concreta, em forma canônica: uma declaração por linha, indentação de quatro espaços, bloco vazio como `{ }` e toda expressão binária e unária entre parênteses, para a precedência ficar explícita no texto. `a - b - c` sai como `((a - b) - c)`.

Acrescentar esse visitor não exigiu mudar nenhuma classe de nó. O `accept` já despachava pelo tipo do nó, então a operação nova é só uma classe nova implementando `Visitor<String>`.
