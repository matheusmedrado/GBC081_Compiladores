# Autômatos do scanner

Os desenhos foram feitos em Graphviz. Os arquivos `.dot` estão em `fontes/` e as imagens são geradas com:

```
dot -Tpng -Gdpi=130 fontes/afd-string.dot -o afd-string.png
```

Como ler os desenhos:

- círculo duplo: estado de aceitação (o que foi lido até ali já é um token válido)
- seta sólida: transição que consome o caractere
- seta tracejada: transição que só olha o caractere (lookahead) e não consome; é assim que o maximal munch termina um token
- caixa arredondada: ação de emitir um token
- caixa tracejada: erro léxico; depois dele o scanner volta para `START`

O scanner é um autômato só. `START` escolhe o sub-autômato pelo primeiro caractere e cada categoria tem um desenho próprio. Os nomes dos estados são os mesmos do `enum State` em `src/lexer/Scanner.java`, e cada estado é um `case` do switch em `nextToken()`.

| Desenho | Estados |
|---|---|
| [afd-visao-geral.png](afd-visao-geral.png) | `START` |
| [afd-identificador.png](afd-identificador.png) | `IDENT` (identificador e palavra reservada) |
| [afd-string.png](afd-string.png) | `STR`, `STR_ESC` |
| [afd-numero.png](afd-numero.png) | `NUM_INT`, `NUM_DOT`, `NUM_FRAC` |
| [afd-caractere.png](afd-caractere.png) | `CHR`, `CHR_ESC`, `CHR_END`, `CHR_LONG` |
| [afd-operadores.png](afd-operadores.png) | `ASSIGN`, `BANG`, `LESS`, `GREATER`, `AMP`, `PIPE` |
| [afd-comentarios.png](afd-comentarios.png) | `SLASH`, `LINE_COMMENT`, `BLOCK_COMMENT`, `BLOCK_COMMENT_STAR` |

## Estado no desenho e linha no código

| Estado | Linha em `Scanner.java` | O que faz |
|---|---|---|
| `START` | 123 | descarta brancos, emite EOF e tokens de um caractere, ou escolhe o próximo estado pelo primeiro caractere |
| `IDENT` | 171 | continua com letra, dígito ou `_`; no fim consulta a tabela `KEYWORDS` (linha 182) |
| `NUM_INT` | 186 | parte inteira; `.` vai para `NUM_DOT` |
| `NUM_DOT` | 198 | exige um dígito depois do ponto, senão é erro |
| `NUM_FRAC` | 208 | parte fracionária |
| `STR` | 217 | corpo da string; `"` fecha, `\` vai para `STR_ESC`, `\n` ou EOF é erro |
| `STR_ESC` | 236 | caractere depois da barra |
| `CHR` | 255 | primeiro caractere depois de `'` |
| `CHR_ESC` | 276 | caractere depois da barra |
| `CHR_END` | 292 | espera a `'` que fecha |
| `CHR_LONG` | 306 | recuperação de `'ab'`: descarta até fechar ou até o fim da linha |
| `SLASH` | 324 | divisão, comentário de linha ou comentário de bloco |
| `LINE_COMMENT` | 337 | até o fim da linha |
| `BLOCK_COMMENT` | 345 | dentro do comentário de bloco |
| `BLOCK_COMMENT_STAR` | 359 | acabou de ler `*`; se vier `/`, o comentário fecha |
| `ASSIGN` | 380 | `=` ou `==` |
| `BANG` | 387 | `!` ou `!=` |
| `LESS` | 394 | `<` ou `<=` |
| `GREATER` | 401 | `>` ou `>=` |
| `AMP` | 408 | só aceita `&&` |
| `PIPE` | 417 | só aceita `\|\|` |
