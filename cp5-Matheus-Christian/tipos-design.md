# Regras de tipos e decisões

Construção de Compiladores (GBC081), Checkpoint 5. Grupo Matheus-Christian.

Este é o texto passado a limpo da Parte 1. O original, escrito à mão, é `tipos-a-mao.pdf`. As saídas em `test/esperado/` foram escritas a partir destas regras, não copiadas da execução.

## 1. Notação

- `Γ(x) = T`: o símbolo que a identificação gravou em `x.decl` é variável ou parâmetro, de tipo `T`.
- `Γ(f) = (T1, ..., Tn) → R`: o símbolo de `f` é procedimento ou função, com parâmetros `T1..Tn` e retorno `R`. Procedimento tem `R = void`.
- `e : T`: a expressão `e` tem tipo `T`. `S ok`: o comando `S` está bem tipado.
- `num = {char, int, double}`. A ordem numérica é `char < int < double`.
- `sup(T1, T2)` é `double` se algum dos dois é `double`; senão é `int`. `char` nunca é resultado de operação.
- `S ⪯ T` (lê-se "S cabe em T") é a compatibilidade usada em inicializador, atribuição, argumento e `return`:
  - `T ⪯ T`, para todo `T` diferente de `void`;
  - `char ⪯ int`, `char ⪯ double`, `int ⪯ double`;
  - nenhum outro par. Em especial, `double ⋠ int`, `int ⋠ char`, `bool ⋠ int`.
- `erro` é o tipo de uma expressão que já gerou mensagem. Ver a regra [ERRO].

Cada regra tem as premissas acima da linha e a conclusão abaixo. A condição do lado vem depois de `se`.

## 2. Regras

### Literais

```
[LIT-INT]  ──────────── se n é int_lit       [LIT-DOUBLE] ──────────── se r é double_lit
             n : int                                        r : double

[LIT-CHAR] ──────────── se c é char_lit      [LIT-STRING] ──────────── se s é string_lit
             c : char                                       s : string

[LIT-BOOL] ──────────── se b é true ou false
             b : bool
```

### Identificador

```
              Γ(x) = T
[ID]       ────────────── se x é variável ou parâmetro
              x : T
```

Se `x` é procedimento ou função, nenhuma regra se aplica: erro (decisão 11). Se `x` não foi declarado, a identificação já reportou; aqui `x : erro`, sem mensagem.

### Operadores aritméticos

```
            e1 : T1    e2 : T2
[ARIT]   ──────────────────────── se op ∈ {+, -, *, /} e T1, T2 ∈ num
          e1 op e2 : sup(T1, T2)

            e1 : T1    e2 : T2
[MOD]    ──────────────────────── se T1, T2 ∈ {char, int}
             e1 % e2 : int
```

### Relacionais

```
            e1 : T1    e2 : T2
[REL]    ──────────────────────── se op ∈ {<, <=, >, >=} e T1, T2 ∈ num
             e1 op e2 : bool
```

### Igualdade

```
            e1 : T1    e2 : T2
[IGUAL-NUM]──────────────────────── se op ∈ {==, !=} e T1, T2 ∈ num
             e1 op e2 : bool

            e1 : bool  e2 : bool
[IGUAL-BOOL]─────────────────────── se op ∈ {==, !=}
             e1 op e2 : bool
```

### Lógicos

```
            e1 : bool  e2 : bool
[LOG]    ──────────────────────── se op ∈ {&&, ||}
             e1 op e2 : bool
```

### Unários

```
               e : T                            e : bool
[NEG]    ──────────────── se T ∈ num   [NOT] ──────────────
          -e : sup(T, int)                       !e : bool
```

### Declaração com inicializador e atribuição

```
              e : S    S ⪯ T
[DECL]     ──────────────────
             T x = e;  ok

           Γ(x) = T    e : S    S ⪯ T
[ATRIB]  ─────────────────────────────── se x é variável ou parâmetro
                   x = e;  ok
```

Em [DECL], o inicializador é conferido antes de `x` contar como tendo valor (seção 5). O nome já existe no inicializador desde o CP4 (decisão 4.2 de `escopo-design.md`), então `int c = c;` é bem tipado e gera aviso.

### if, while e bloco

```
           e : bool    B1 ok    B2 ok                     e : bool    B1 ok
[IF]     ──────────────────────────────       [IF-SEM] ──────────────────────
          if (e) B1 else B2  ok                          if (e) B1  ok

           e : bool    B ok                               S1 ok  ...  Sn ok
[WHILE]  ────────────────────                 [BLOCO]  ───────────────────────
          while (e) B  ok                                { S1 ... Sn }  ok
```

### return

`f` é a função corrente, a da declaração que contém o comando.

```
          Γ(f) = (...) → R    e : S    S ⪯ R
[RETURN] ──────────────────────────────────── se R ≠ void
                  return e;  ok

             Γ(f) = (...) → void
[RETURN-VOID]──────────────────────
               return;  ok
```

`return e;` em procedimento, `return;` em função e `e` com tipo que não cabe em `R` são erro.

### Chamada

```
          Γ(f) = (T1, ..., Tn) → R    e1 : S1 ... en : Sn    Si ⪯ Ti
[CHAMADA]──────────────────────────────────────────────────────────── se R ≠ void
                          f(e1, ..., en) : R

          Γ(f) = (T1, ..., Tn) → R    e1 : S1 ... en : Sn    Si ⪯ Ti
[CHAMADA-CMD]──────────────────────────────────────────────────────── (R qualquer, void inclusive)
                          f(e1, ..., en);  ok
```

O número de argumentos tem de ser `n`. Em expressão, um procedimento (`R = void`) não tem regra: é erro.

### Declaração de função

```
           corpo ok, com f como função corrente    sempreRetorna(corpo)
[FUNC]   ────────────────────────────────────────────────────────────── se R ≠ void
                       R f(T1 x1, ..., Tn xn) corpo  ok

           corpo ok, com f como função corrente
[PROC]   ────────────────────────────────────────
           void f(T1 x1, ..., Tn xn) corpo  ok
```

`sempreRetorna` (decisão 9):

| Comando | sempreRetorna |
|---|---|
| `return ...;` | sim |
| `{ S1 ... Sn }` | algum `Si` sempre retorna |
| `if (e) B1 else B2` | `B1` e `B2` sempre retornam (o `else if` é um `if` no `B2`) |
| `if (e) B1` sem `else` | não |
| `while (e) B` | não (o corpo pode não rodar) |
| declaração, atribuição, chamada | não |

### Erro

```
[ERRO]  Uma premissa e : T, com e : erro, vale para qualquer T. Se uma
        premissa só vale por causa de erro, a conclusão é : erro (ou ok, num
        comando), e nenhuma mensagem nova é gerada.
```

É o que impede a cascata: `(true + 1) * 2 > x` gera uma mensagem só, na soma.

## 3. Tabela de operadores

`T1` e `T2` são os tipos dos operandos, depois de `char` subir para `int` onde a regra deixa. Todo par que não está na tabela é recusado.

| Operador | Operandos aceitos | Resultado | Regra | Recusados (exemplos) |
|---|---|---|---|---|
| `+` `-` `*` `/` | `int, int` · `char, int` · `char, char` | `int` | [ARIT] | `bool, int` · `string, string` · `string, int` |
| `+` `-` `*` `/` | algum `double`, o outro `num` | `double` | [ARIT] | `double, bool` |
| `%` | `int` ou `char` nos dois | `int` | [MOD] | `double, int` · `int, double` · `bool, bool` |
| `<` `<=` `>` `>=` | `num, num` | `bool` | [REL] | `bool, bool` · `string, string` · `int, bool` |
| `==` `!=` | `num, num` | `bool` | [IGUAL-NUM] | `int, bool` · `char, string` · `string, string` |
| `==` `!=` | `bool, bool` | `bool` | [IGUAL-BOOL] | `bool, double` |
| `&&` `\|\|` | `bool, bool` | `bool` | [LOG] | `int, int` · `bool, int` · `string, bool` |
| `-` unário | `int` ou `char` | `int` | [NEG] | `bool` · `string` |
| `-` unário | `double` | `double` | [NEG] | |
| `!` | `bool` | `bool` | [NOT] | `int` · `string` |

`/` entre dois inteiros é a divisão inteira; o resultado é `int`.

## 4. As 11 decisões

### 1. int → double implícito

**Escolha.** Sim. `int` (e `char`) sobe para `double` sem conversão escrita, em todo contexto: operando de aritmética, relacional e igualdade misturado com `double`, inicializador, atribuição, argumento e `return`. O inverso, `double → int`, nunca é implícito: é erro em todos esses contextos.

**Alternativa descartada.** Exigir o mesmo tipo e pedir uma conversão explícita. A gramática não tem cast, então `double d = 3;` e `media(1, 2)` ficariam impossíveis de escrever sem um literal `3.0`.

**Motivo.** Alargar não perde informação; estreitar perde a parte fracionária em silêncio. É a regra de C para o sentido seguro e a de Java para o outro. O gerador vai precisar inserir a conversão onde `int ⪯ double` foi usado; o tipo gravado no nó diz onde.

**Teste.** `d01_int_double.txt`: `double d = 3`, `1 + 2.5` e `media(1, 2)` passam; `return x` em função `int`, `int i = 2.0` e `i = d` são recusados.

### 2. char com int

**Escolha.** `char` é numérico e sobe para `int` (e para `double`). `c + 1`, `c < 'z'` e `c % 2` são `int` ou `bool`. Toda aritmética com `char` dá `int`, inclusive `char + char`. `int → char` não é implícito: `char e = c + 1;` e `char f = 65;` são erro.

**E se não couber em char?** A pergunta não aparece: como o resultado é sempre `int` e `int` não cabe em `char`, nenhum valor é truncado para `char` sem o programador ver o erro.

**Alternativa descartada.** `char` só com `char` (só `==` e `<`), sem aritmética. Isso proibiria o `c - '0'` e o `c + 1` que todo programa com caractere usa.

**Motivo.** Na TAM `char` já é uma palavra com o código do caractere; subir para `int` não custa instrução nenhuma.

**Teste.** `d02_char_int.txt`.

### 3. bool em aritmética

**Escolha.** Não. `bool` só entra em `&&`, `||`, `!`, `==` e `!=` com outro `bool`. E a recíproca: `int` não é condição. `if`, `while` e os operandos de `&&` exigem `bool`.

**Alternativa descartada.** O modelo de C, em que `bool` é `int` e qualquer `int` é condição. Isso aceitaria `if (n)` escrito no lugar de `if (n > 0)`, e `true + 1`.

**Motivo.** A linguagem tem `bool` e literais `true`/`false` próprios; se tudo vira `int`, o tipo não serve para nada. `n != 0` é curto o bastante.

**Teste.** `d03_bool_aritmetica.txt` e `cond_int.txt`.

### 4. string

**Escolha.** `string` só pode ser declarada, inicializada, atribuída, passada como argumento e devolvida. Nenhum operador aceita `string`: nem `==`, nem `<`, nem `+`.

**Alternativa descartada.** `+` como concatenação e `==` comparando conteúdo. As duas precisam de rotina em tempo de execução (alocar, copiar, percorrer caractere por caractere), e a TAM só opera sobre palavras. Aceitar `==` comparando o endereço seria pior: o programa compila e responde `false` para dois textos iguais.

**Motivo.** Representação na TAM (seção 6): uma `string` é o endereço de uma área com o comprimento e os caracteres. Sem rotina de biblioteca, a única operação honesta é mover o endereço.

**Teste.** `d04_string.txt`.

### 5. == entre tipos diferentes

**Escolha.** Aceitos: qualquer par de numéricos (`int`, `double`, `char`, em qualquer combinação, com a mesma subida da aritmética) e `bool` com `bool`. Recusado todo o resto: `int == bool`, `char == string`, `string == string` (decisão 4).

**Alternativa descartada.** Aceitar qualquer par e devolver `false` quando os tipos diferem. Esconde erro: `i == b` com `i : int` e `b : bool` nunca é o que se quis escrever.

**Motivo.** `==` segue as mesmas classes de [REL] e [LOG]: compara números com números e booleanos com booleanos.

**Teste.** `d05_igualdade.txt`.

### 6. Função usada como comando

**Escolha.** Permitido; o valor é descartado. `conta(1);` vale mesmo com `conta` devolvendo `int` ([CHAMADA-CMD]).

**Alternativa descartada.** Exigir que o valor de função seja usado. A gramática não tem como descartar de propósito (não há `(void)`), então uma função chamada só pelo efeito exigiria uma variável inútil.

**Motivo.** É o comportamento de C e Java. Na geração de código basta um `POP` depois da chamada.

**Teste.** `d06_funcao_comando.txt`.

### 7. Recursão mútua

**Escolha.** Não é permitida, nem por declaração antecipada nem por duas passadas. É a decisão 4.3 do CP4: a identificação é uma passada só, e a gramática não tem protótipo. O verificador de tipos não muda isso: ele só lê o `decl` que a identificação gravou. A chamada a um nome não declarado tem tipo `erro` e não gera mensagem de tipo.

**Alternativa descartada.** Coletar as assinaturas globais antes dos corpos. Ver `escopo-design.md`, 4.3.

**Motivo.** Manter uma ida só, da esquerda para a direita, e as mensagens na ordem do texto. A recursão direta funciona, porque o nome e a assinatura entram no escopo antes do corpo.

**Teste.** `d07_recursao_mutua.txt` (só o erro de identificação, sem cascata no `return`) e `recursiva.txt`.

### 8. Argumentos

**Escolha.** O argumento segue a mesma compatibilidade da atribuição: `S ⪯ T`. Aceita `int → double` e `char → int`; recusa `double → int`.

**Alternativa descartada.** Exigir tipo igual. `area(3, 4)` exigiria `area(3.0, 4.0)`, e passar um argumento ficaria mais restrito que atribuir o mesmo valor a uma variável do tipo do parâmetro, que é o que a passagem por valor faz.

**Motivo.** Uma regra só (`⪯`) para os quatro lugares em que um valor vai para um lugar com tipo declarado.

**Teste.** `d08_argumentos.txt`.

### 9. return em todos os caminhos

**Escolha.** Análise de fluxo simples, pela tabela `sempreRetorna` da seção 2. Uma função (`R ≠ void`) cujo corpo não sempre retorna é erro, reportado na linha da declaração. Procedimento não precisa de `return`. `while` nunca conta como retorno, mesmo com `return` no corpo, porque a análise não avalia a condição.

**Alternativa descartada.** Basta algum `return` no corpo. Aceitaria `semElse`, que no caminho `x <= 0` cai no fim da função sem valor; na TAM isso devolve lixo da pilha.

**Motivo.** A tabela é uma regra por comando, cabe no visitor (o campo `returns` do `TypeChecker`) e é conservadora: pode recusar uma função que na prática sempre retorna (o `noLaco` com condição sempre verdadeira), mas não aceita uma que não retorna.

**Teste.** `d09_return_caminhos.txt`.

### 10. Mesmo nome, assinaturas diferentes

**Escolha.** Proibido: não há sobrecarga. É a decisão 4.1/4.5 do CP4: um nome por escopo, qualquer categoria. A segunda `soma` é redeclaração; fica a primeira, com a assinatura `(int, int) → int`, e as chamadas são conferidas contra ela.

**Alternativa descartada.** Sobrecarga por tipo dos parâmetros. A tabela teria de guardar uma lista de símbolos por nome, e a chamada teria de escolher entre eles, o que junto com a decisão 8 (`int ⪯ double`) cria ambiguidade (`soma(1, 2)` serve para as duas).

**Motivo.** A tabela da Aula 11 tem uma chave por nome, e a subida implícita de tipos torna a escolha da sobrecarga uma regra a mais para documentar e testar.

**Teste.** `d10_sobrecarga.txt`: a redeclaração na linha 5 e, na linha 10, os dois argumentos `double` recusados contra `soma(int, int)`.

### 11. Nome de função sem parênteses

**Escolha.** Erro. Não há tipo função entre os valores da linguagem. `int x = cinco;` e `bool b = nada == true;` são erro; a expressão fica `erro`. Atribuir a uma função (`cinco = 3;`) também é erro, e chamar uma variável (`x(3)`) também.

**Alternativa descartada.** Tratar o nome como valor (ponteiro de função). Não há tipo para escrever a variável que o receberia, nem instrução para chamá-lo depois.

**Motivo.** Sem tipo função, o nome só tem sentido seguido de `(`. Em [ID], a premissa exige variável ou parâmetro.

**Teste.** `d11_funcao_sem_parenteses.txt` e a linha 16 de `chamada_errada.txt`.

## 5. Variáveis sem valor

Análise conservadora, reportada como **aviso**: o programa continua aceito.

- Só variáveis locais entram. Uma global não é conferida, porque qualquer função pode dar valor a ela antes da outra rodar, e a análise é por função. Parâmetro sempre tem valor.
- Uma local declarada sem inicializador começa "talvez sem valor". Atribuição tira. Um uso de uma variável "talvez sem valor" gera o aviso, uma vez por variável.
- `if`: depois dele, está "talvez sem valor" o que ficou assim em algum dos ramos. Sem `else`, o ramo vazio conta.
- `while`: depois dele, vale o estado de antes do laço (o corpo pode não rodar). O corpo é visitado com o estado de antes, que é o mais pessimista.
- Depois de `return`, nada está "talvez sem valor": o resto do caminho não roda. Por isso `if (c) { return 1; } else { x = 1; }` deixa `x` com valor.
- `int c = c;`: o inicializador é visto antes de `c` ganhar valor, então o `c` da direita gera aviso.

Teste: `aviso_sem_valor.txt`.

## 6. Representação na TAM (para a geração de código; não implementado)

| Tipo | Palavras | Representação |
|---|---|---|
| `int` | 1 | o inteiro da palavra |
| `bool` | 1 | 0 é `false`, 1 é `true` |
| `char` | 1 | o código do caractere; subir para `int` não gera instrução |
| `double` | 2 | ponto fixo: a primeira palavra é a parte inteira (com sinal) e a segunda a parte fracionária em décimos de milésimo. As operações são rotinas de biblioteca do compilador. `int → double` empilha a palavra 0 depois do inteiro |
| `string` | 1 | o endereço de uma área no heap: uma palavra com o comprimento e uma palavra por caractere. Atribuir, passar e devolver copiam só o endereço |

A representação de `double` ainda pode mudar no CP6; o verificador não depende dela. O que o gerador precisa saber já está na árvore: o tipo de cada expressão, para escolher a instrução e inserir a conversão onde `int ⪯ double` ou `char ⪯ double` foi usado.

## 7. Mensagens

Cada mensagem é uma linha, `linha L: ...`, com o nome ou o operador entre aspas simples e os tipos pelo nome. O aviso tem `aviso:` depois da linha. As mensagens saem ordenadas pela linha; na mesma linha, as da identificação vêm antes das de tipo, e as de tipo na ordem em que o texto é lido. O relatório termina com `programa aceito` (nenhum erro, avisos permitidos) ou `programa rejeitado`.

| Situação | Mensagem |
|---|---|
| operador binário | `linha L: operador '+' não se aplica a int e bool` |
| operador unário | `linha L: operador '!' não se aplica a int` |
| condição | `linha L: condição de 'if' deve ser bool, mas é int` (ou `'while'`) |
| inicializador | `linha L: inicialização de 'x': esperado int, veio double` |
| atribuição | `linha L: atribuição a 'x': esperado int, veio double` |
| atribuição a função | `linha L: 'f' é função, não variável` |
| return com tipo errado | `linha L: 'return' de 'f': esperado int, veio double` |
| return com valor em procedimento | `linha L: 'return' com valor no procedimento 'p'` |
| return sem valor em função | `linha L: 'return' sem valor na função 'f', que devolve bool` |
| caminho sem return | `linha L: função 'f' pode terminar sem 'return'` (linha da declaração) |
| aridade | `linha L: 'fat' espera 1 argumento(s), recebeu 2` |
| argumento | `linha L: argumento 1 de 'mostra': esperado string, veio int` |
| chamada de não-função | `linha L: 'x' não é função nem procedimento` |
| procedimento em expressão | `linha L: procedimento 'p' não devolve valor e não pode ser usado em expressão` |
| função sem parênteses | `linha L: 'f' é função; sem parênteses não é valor` |
| variável sem valor | `linha L: aviso: 'x' pode ser usada sem valor` |

## 8. O que a linguagem não tem

A gramática do CP2 não tem `print`, `read`, `break` nem função aninhada. O verificador não tem regra para eles e o contexto não guarda laços abertos. As chamadas a `imprime` dos exemplos do CP3 continuam sendo "não declarado". "Função aninhada usando variável do nível acima" vira, nesta linguagem, uma função que usa a global e um bloco interno que usa o parâmetro e a local de fora (`nivel_acima.txt`).
