# Log de uso de ferramentas de IA

- **Disciplina:** GBC081 - Compiladores
- **Trabalho:** Checkpoint 2, gramática e parser recursivo descendente
- **Integrantes:** Matheus Medrado e Christian [sobrenome]
- **Ferramentas usadas:** ZCode (agente de programação com IA)

## Como a IA foi usada

A gramática tem muita conta repetitiva (FIRST/FOLLOW de 46 não-terminais) e o parser tem
trechos que seguem o mesmo molde (sete níveis de expressão quase iguais), então a IA
entrou mais do que no CP1, em duas frentes: dúvidas conceituais (os conflitos LL(1), a
regra do FOLLOW com não-terminal anulável e a recuperação de erros) e revisão de código
que já tínhamos escrito. A gramática, a tabela FIRST/FOLLOW, o parser e as decisões de
linguagem são do grupo; toda sugestão da IA foi conferida contra a gramática e a tabela
e coberta por teste.

## Uso por etapa

### Gramática (`gramatica-first-follow.md`, seções 1 e 2)

- **O que fizemos:** escrevemos a primeira versão no estilo de C, eliminamos a recursão
  à esquerda, fatoramos e calculamos FIRST/FOLLOW à mão.
- **Onde a IA entrou — dúvida conceitual:** na discussão do `else` pendurado e da
  atribuição como expressão, os dois conflitos que sobraram depois das transformações.
  Pedimos para a IA explicar por que cada um violava LL(1) e quais eram as saídas
  possíveis.
- **Como conferimos:** refizemos a derivação de cada conflito no papel (é o que está na
  seção 2.4) antes de mudar a gramática.

### FIRST, FOLLOW e verificação LL(1) (seções 3 e 4)

- **O que fizemos:** a tabela inteira e a verificação das três cláusulas, não-terminal
  por não-terminal.
- **Onde a IA entrou — dúvida conceitual:** calculando o FOLLOW das expressões, ficamos
  em dúvida sobre o caso em que o não-terminal à direita é anulável: em
  `AndExpr → EqExpr AndRest`, o `FOLLOW(EqExpr)` também recebe o `FOLLOW(AndExpr)`?
  Perguntamos a regra em geral, com um exemplo pequeno de aula, sem passar a nossa
  gramática. Com a regra entendida, vimos que tínhamos esquecido isso na cadeia das
  expressões e refizemos a tabela de `EqExpr` para baixo.
- **Como conferimos:** refizemos à mão a cadeia de expressões inteira com a regra certa
  (a explicação está em "Como alguns FOLLOW saíram"). Só então corrigimos a tabela.

### Parser (`src/parser/`)

- **O que fizemos:** a estrutura do parser, a tradução de todas as produções, inclusive
  os sete níveis de expressão, e as constantes `FIRST_X`/`FOLLOW_X`.
- **Onde a IA entrou — revisão de trecho já escrito:** com os métodos das expressões
  prontos, pedimos para a IA revisar se o ε de cada `Rest` testava o FOLLOW do próprio
  nível e não o do nível de cima, que é o erro mais fácil de cometer copiando um nível
  para o outro.
- **Onde a IA entrou — dúvida conceitual:** na recuperação de erro, a IA explicou a
  abordagem do livro do Aho (seção 4.4.5) para o `match` e para as âncoras. A escolha
  das âncoras foi ajustada por nós rodando os exemplos.
- **Como conferimos:** rodando a suíte e lendo o traço (`--trace`) de cada exemplo com
  erro.

### Testes e exemplos (`test/`, `exemplos/`)

- **O que fizemos:** os programas válidos, os casos de um erro com linha, coluna e
  mensagem esperadas, e o programa com quatro erros.
- **Onde a IA entrou — sugestão de estratégia, que avaliamos e estendemos:** ela
  sugeriu testar a terminação de forma sistemática, com todos os prefixos de um programa
  e o programa sem cada token. Nós acrescentamos as sequências aleatórias e a verificação
  do limite de erros do modo pânico.
- **Como conferimos:** quebramos de propósito a guarda da retomada de `StmtList` e o ε
  robusto, para ver se os testes falhavam. Falharam.

## Interações representativas

- **Sugestão que não aceitamos (`else` pendurado):** a primeira resposta da IA foi
  manter `ElsePart → 'else' Stmt | ε` e resolver no código, preferindo sempre o `else`
  (o `else` fica com o `if` mais próximo). Funciona, e é o que muitos compiladores
  fazem. Mas a gramática entregue precisa ser LL(1) de ponta a ponta, e com isso a
  célula `M[ElsePart, else]` continuaria com duas produções. Ficamos com as chaves
  obrigatórias, que deixam a gramática LL(1) e a regra fácil de explicar.
- **Depuração (loop na recuperação):** um teste com o arquivo terminando dentro de um
  bloco não terminava. Mostramos o método `parseStmtList` para a IA, e ela apontou que
  o EOF não está em `FIRST(Stmt)` nem em `FOLLOW(StmtList)`. A lista sincronizava,
  parava no EOF e chamava a si mesma de novo, para sempre. A correção foi só retomar a
  lista se o lookahead estiver em `FIRST(Stmt)`. O caso virou o teste "falta '}' no fim
  do arquivo".
- **Âncoras:** a primeira versão das âncoras seguia a sugestão do livro, que a IA tinha
  trazido, de usar tudo que começa comando, inclusive o identificador. Rodando o exemplo
  de erros do CP1, vimos que `if (total > 0 & nome != "")` gerava dois erros sintáticos
  para um erro léxico, porque a sincronização parava em `nome`. Tiramos o identificador
  das âncoras. A justificativa está no README.

## O que não foi feito com IA

As decisões de linguagem (bloco obrigatório, atribuição como comando, relacionais sem
encadear, `void` só como retorno) e a escolha de qual saída usar para cada conflito LL(1)
foram da dupla. Também foram nossos a tabela FIRST/FOLLOW, a
verificação LL(1) escrita, a escolha dos conjuntos de sincronização e dos casos de teste.
Nenhuma sugestão da IA entrou sem ser conferida contra a gramática e coberta por
teste.

## Declaração

Declaramos que todo o conteúdo deste trabalho foi lido, compreendido e é de nossa
responsabilidade, e que este log descreve com sinceridade o uso das ferramentas de IA
listadas acima.

- Matheus Medrado, 13/10/2026
- Christian [sobrenome], 13/10/2026
