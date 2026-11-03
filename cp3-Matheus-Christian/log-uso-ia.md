# Log de uso de ferramentas de IA

- **Disciplina:** GBC081 - Compiladores
- **Trabalho:** Checkpoint 3, AST e visitors
- **Integrantes:** Matheus Medrado Ferreira e Christian Silva Rodrigues
- **Ferramentas usadas:** ZCode (agente de programação com IA), durante o desenvolvimento;
  Claude Code (Anthropic), na revisão final

## Como a IA foi usada

Durante o desenvolvimento, o ZCode foi pouco usado, e do mesmo jeito que no CP2: dúvidas
conceituais pontuais sobre AST e visitors e pedidos de revisão de trechos que já tínhamos
escrito. Não guardamos o histórico dessas conversas (a ferramenta ficou no computador do
Christian), então não temos como listar pergunta por pergunta. A hierarquia de nós, o
mapeamento da gramática, o parser devolvendo a árvore, os dois visitors, as árvores
esperadas e os testes são do grupo.

Com o CP3 pronto e a suíte passando, pedimos ao Claude Code uma revisão para procurar
bugs e inconsistências entre o código e a documentação. Cada mudança que veio dela está
listada abaixo, com o motivo e com o modo como conferimos.

## O que fizemos, por etapa

### Projeto da AST (`ast-design.md`, `src/ast/`)

- **O que fizemos:** desenhamos a hierarquia antes das classes, a partir da gramática do
  CP2: um tipo de nó por categoria sintática (Aula 7), o destino de cada um dos 46
  não-terminais, a posição em todo nó e o formato do impressor.
- **Como conferimos:** a árvore do programa curto da seção 5 foi escrita à mão e depois
  comparada com a saída do `TreePrinter`.

### Parser devolvendo a árvore (`src/parser/Parser.java`)

- **O que fizemos:** cada `parseX()` que corresponde a um nó passou a devolvê-lo. Os
  `Rest` recebem o operando da esquerda e montam o `Bin` da esquerda para a direita. A
  recuperação de erros do CP2 não mudou.
- **Como conferimos:** a suíte do CP2 continuou passando com os mesmos veredictos e as
  mesmas mensagens, e as árvores saíram iguais aos arquivos esperados.

### Visitors (`src/visitor/`)

- **O que fizemos:** a interface `Visitor<T>` com um método por tipo de nó, o
  `TreePrinter` e o `SourcePrinter`, que reimprime o programa com toda expressão entre
  parênteses.
- **Como conferimos:** pela ida e volta: o texto reimpresso, lido de novo, reimprime igual
  e dá a mesma árvore.

### Testes (`test/parser/`, `test/esperado/`)

- **O que fizemos:** as árvores esperadas escritas à mão, os casos de precedência,
  associatividade e parênteses, a ida e volta e a verificação de que, com erro, a árvore
  vem nula.
- **Como conferimos:** rodando a suíte inteira.

## Revisão final (Claude Code)

O pedido foi procurar bugs no CP3 pronto. A ferramenta leu o código e a documentação,
compilou, rodou as duas suítes, comparou o parser com o do CP2 e testou casos fora da
suíte (programas grandes, ida e volta com expressões difíceis, posição de cada nó). O que
ela apontou e o que fizemos:

- **Listas montadas com cópia.** Os métodos das listas (`parseDeclList`,
  `parseStmtList`, `parseParamList`, `parseVarList`, `parseArgList`) criavam uma lista
  nova a cada chamada e copiavam o resto com `addAll`. Além de copiar a lista de novo em
  cada nível, isso contrariava o comentário da própria classe, que diz que as listas
  recebem o que já foi construído. Também gastava mais pilha: um bloco com 10 mil
  comandos estourava a pilha, e o CP2 aguentava uns 20 mil. Agora cada lista recebe a
  lista já lida e acrescenta um elemento por chamada, como os `Rest` das expressões, e o
  limite subiu para uns 17 mil comandos. A recursão continua, como no CP2, porque cada
  método segue a sua produção.
- **Posição do bloco na recuperação.** No corpo sem chaves (`if (c) x = 1;`), o `Block`
  pegava a posição do token seguinte ao comando. Isso não aparecia em lugar nenhum, porque
  com erro a árvore é descartada, mas passou a usar a posição do comando.
- **Posição sem teste.** A seção 3 do `ast-design.md` diz que todo nó guarda a posição,
  mas nenhum teste conferia isso, já que a impressão da árvore não mostra a posição.
  Entrou o teste "posição guardada em cada nó".
- **Documentação.** No `ast-design.md`: a árvore da seção 5 tem 35 nós, não 24. A conta
  dos 46 não-terminais não fechava: dizia que 17 viram nó, mas 17 é o número de tipos de
  nó, e `VarRest` e `VarList` não apareciam em lugar nenhum. Agora a conta é 12 + 15 + 19,
  e cada um dos 12 diz qual nó vira. O item 1 da seção 5 dizia que a condição `n < 0`
  vira um `Id(n)`, quando ela vira um `Bin(<)` com o `Id(n)` embaixo.
- **Conferência com o enunciado.** Pedimos para comparar o trabalho com o enunciado item
  por item. Três pontos mudaram. O enunciado pede uma classe abstrata `Node` com a
  posição e o `accept`; tínhamos uma interface `Node` e a posição numa classe `Pos`.
  Agora `Node` é a classe abstrata e `StmtNode` continua como interface, só para o que
  pode aparecer num bloco. O método do `TypeNode` no visitor passou de `visitType` para
  `visitTypeNode`, nomeado pelo tipo como os outros. E a ida e volta, que cobria só os
  cinco programas maiores, passou a cobrir todos os programas válidos da suíte. No
  `ast-design.md`, as decisões de representação, que estavam espalhadas, ganharam uma
  seção própria.
- **Limpeza.** Dois `import` sem uso e o comentário de `Stmt`, que citava a declaração
  local (`VarDecl` entra no bloco como `StmtNode`, mas não estende `Stmt`).
- **Como conferimos:** lemos o diff de cada mudança. A suíte passou inteira (47 testes do
  parser, 27 do scanner), as saídas dos quatro exemplos ficaram iguais às de antes e o
  `test/saida-testes.txt` foi gerado de novo. Para ver se o teste novo de posição pega
  erro de verdade, trocamos a posição do `Bin` pela do operando da esquerda numa cópia do
  código: o teste falhou. Fora da suíte, 60 mil versões alteradas dos exemplos (tokens
  apagados, trocados ou inseridos) passaram pelo parser sem exceção, sem travar e sem
  árvore com erro.

## O que não foi feito com IA

O projeto da árvore (que não-terminal vira nó, `VarDecl` como `Decl` e `StmtNode`, o
`else if` sem nó próprio, a árvore nula quando há erro), o formato do impressor, a
escolha do segundo visitor e as árvores esperadas foram da dupla. Nenhuma sugestão da IA
entrou sem ser conferida e coberta por teste.

## Declaração

Declaramos que todo o conteúdo deste trabalho foi lido, compreendido e é de nossa
responsabilidade, e que este log descreve com sinceridade o uso das ferramentas de IA
listadas acima.

- Matheus Medrado Ferreira, 03/11/2026
- Christian Silva Rodrigues, 03/11/2026
