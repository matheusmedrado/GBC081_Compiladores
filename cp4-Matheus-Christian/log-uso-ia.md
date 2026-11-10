# Log de uso de ferramentas de IA

- **Disciplina:** GBC081 - Compiladores
- **Trabalho:** Checkpoint 4, tabela de símbolos e identificação
- **Integrantes:** Matheus Medrado Ferreira e Christian Silva Rodrigues
- **Ferramentas usadas:** ZCode (agente de programação com IA), durante o desenvolvimento; Claude Code (Anthropic), na revisão final

## Como a IA foi usada

O Checkpoint 4 adiciona o ambiente semântico sobre a AST do CP3: a tabela de símbolos e a passada de identificação. O documento `escopo-design.md`, com o desenho do ambiente, as cinco decisões de escopo e os relatórios esperados de cada programa, foi planejado e fechado pela dupla antes de consolidar o código. A IA entrou em momentos bem específicos de apoio: para discutir alternativas de implementação da tabela de símbolos, revisar o contrato de métodos que poderiam quebrar silenciosamente e apontar casos de teste de borda.

Toda a estrutura de classes, os doze programas de teste e os arquivos esperados `id-*.txt` foram escritos e conferidos por nós. Nenhuma sugestão da IA foi incorporada sem validação contra o `escopo-design.md` e verificação na suíte de testes.

## O que fizemos, por etapa

### Projeto do ambiente e decisões (`escopo-design.md`)

- **O que fizemos:** definimos o formato do ambiente em pilha (nível 0 global, níveis internos para funções e blocos), os campos do `Symbol`, o formato dos relatórios (`erros:` e `usos:`) e as cinco decisões de escopo (sombreamento permitido com primeira declaração fixa no mesmo nível; declaração antes do uso em uma leitura só da esquerda para a direita; proibição de recursão mútua sem protótipo; mesmo escopo para parâmetros e corpo; e espaço de nomes unificado estilo C).
- **Onde a IA entrou — discussão conceitual:** perguntamos como o livro do Aho e compiladores reais costumam tratar o escopo de parâmetros em relação ao bloco do corpo (`void f(int a) { int a; }`). A resposta confirmou que a abordagem de C/Java (mesmo nível, sendo redeclaração imediata) era a mais limpa e consistente com a nossa gramática, descartando a ideia de abrir um escopo extra só pelas chaves da função.
- **Como conferimos:** desenhamos o ambiente linha por linha em `programs/sombreamento.txt` e `programs/param-corpo.txt` à mão antes de rodar os testes.

### Tabela de símbolos (`src/symbols/`)

- **O que fizemos:** a interface `SymbolTable`, a classe `Symbol` (com `category`, `declaration`, `level`, e os campos vazios `type` e `displacement` reservados para o CP5 e CP6), a enumeração `Category` e a implementação `StackedSymbolTable` usando `ArrayDeque<Map<String, Symbol>>`.
- **Onde a IA entrou — revisão de contrato:** pedimos para a IA revisar o comportamento da tabela sob condições de erro. Ela chamou a atenção para dois pontos: `closeScope()` em pilha vazia (que poderia estourar `NoSuchElementException` se chamada indevidamente) e `declare()` sem escopo aberto (que deve lançar `IllegalStateException` explícita, nunca `NullPointerException`).
- **Como conferimos:** escrevemos `test/symbols/SymbolTableTest.java` cobrindo esses casos limite de forma isolada, sem depender da AST ou de arquivos de entrada.

### Ajuste na AST do CP3 (`src/ast/`)

- **O que fizemos:** a AST do CP3 foi preservada quase intacta. O único acréscimo foi o atributo `public Symbol decl;` nos nós onde um identificador é utilizado.
- **Onde a IA entrou — revisão:** quando mostramos a hierarquia da AST, a IA nos alertou que `Assign` e `Call` guardavam seus nomes como `String` direta e não como nós filhos `Id`. Sem esse aviso, poderíamos ter colocado o campo `decl` apenas em `Id`, deixando atribuições (`x = 1`) e chamadas (`f()`) sem a amarração do símbolo resolvido.
- **Como conferimos:** adicionamos `decl` nas três classes (`Id`, `Assign` e `Call`) e garantimos que o `TreePrinter` e o `SourcePrinter` continuassem funcionando e passando nos testes do CP3 sem alteração.

### Passada de identificação (`src/visitor/IdentificationPass.java`)

- **O que fizemos:** implementamos a passada como `Visitor<Void>`, com percurso em uma ida da esquerda para a direita. O visitor abre e fecha escopos com blocos `try/finally`, registra declarações com `define()` e resolve usos com `bind()`, acumulando erros em ordem de ocorrência sem abortar a análise.
- **Onde a IA entrou — depuração pontual:** no método `visitVar()`, tínhamos colocado a visita ao inicializador antes de chamar `define()`. Ao rodar `programs/ponto-declaracao.txt`, percebemos que `int c = c;` reportava `c não declarado`. A IA explicou a diferença clássica do ponto de declaração: se o nome entra no declarador, o inicializador já enxerga o próprio símbolo. Invertemos a ordem para refletir a decisão 4.2 do nosso design.
- **Como conferimos:** o teste `ponto da declaração` passou a validar que `c = c` resolve para `c@4` e que `int d = e, e = 1` falha no `e`.

### Testes e exemplos (`test/symbols/`, `programs/`)

- **O que fizemos:** os doze programas de teste cobrindo todas as decisões e casos obrigatórios, os doze relatórios esperados escritos à mão em `test/esperado/id-*.txt`, e os executores `SymbolTableTest` e `IdentificationTest`.
- **Onde a IA entrou — sugestão de casos de teste:** ela sugeriu testar explicitamente se o visitor tolerava árvore nula sem estourar `NullPointerException` (caso o parser falhe) e se programas com recursão direta resolviam o identificador da própria função no nível 0. Nós criamos os testes `arvoreNula`, `programaVazio` e `recursivo`.
- **Como conferimos:** rodamos a suíte completa de 17 testes de identificação e 9 da tabela de símbolos, todos passando com sucesso determinístico.

---

## Interações representativas

### 1. Sugestão que rejeitamos: Tabela única com lista de undo
Na discussão inicial da estrutura de dados para a tabela de símbolos, a IA sugeriu utilizar uma tabela hash única com registro de nível e lista encadeada de redefinições, alegando ser a estrutura com menor custo de alocação de memória.
- **Por que rejeitamos:** a tabela única exige percorrer listas ou manter pilhas auxiliares para desempilhar e restaurar símbolos antigos a cada `closeScope()`, além de tornar a inspeção do estado da tabela confusa durante a depuração. A pilha de mapas (`StackedSymbolTable`) com um `HashMap` por escopo é direta, fecha escopos em $O(1)$ simplesmente removendo o topo da pilha sem deixar resíduos, e espelha perfeitamente a semântica de pilha de blocos que modelamos no `escopo-design.md`.

### 2. Sugestão que rejeitamos: Duas passadas para permitir recursão mútua
A IA propôs fazer uma pré-passada no programa coletando todas as assinaturas de funções globais antes de visitar os corpos, para permitir chamadas mútuas como `par()` chamando `impar()` e vice-versa.
- **Por que rejeitamos:** a nossa linguagem segue o estilo de C estrito sem protótipos e o enunciado do checkpoint estabelece uma única passada da esquerda para a direita. Permitir recursão mútua sem protótipo violaria a regra de que todo uso deve ter declaração prévia textual e alteraria a ordem determinística de apresentação dos erros. Mantivemos a decisão 4.3 firme: funções mutuamente recursivas são recusadas como "não declaradas", e adicionamos o teste `programs/mutua.txt` travando esse comportamento.

### 3. Ajuste de implementação: Parâmetros e corpo da função
A IA inicialmente sugeriu que `visitFuncDecl` apenas abrisse o escopo dos parâmetros e que o `visitBlock` do corpo abrisse outro escopo interno automaticamente.
- **O que fizemos:** percebemos que isso faria com que variáveis declaradas no corpo da função sombreadas fossem aceitas em vez de acusar erro de colisão com o parâmetro (`void f(int a) { int a; }`). Recusamos essa abordagem e implementamos o bloco do corpo como caso especial: `visitFuncDecl` abre o escopo nível 1, declara os parâmetros e itera diretamente sobre os `statements` do corpo, garantindo que o corpo e os parâmetros disputem o mesmo escopo (decisão 4.4).

---

## O que não foi feito com IA

1. **As cinco decisões de escopo:** a definição de como sombreamento, ordem de declaração, recursão, parâmetros e espaço de nomes operam nesta linguagem foi decidida integralmente pela dupla e formalizada no `escopo-design.md`.
2. **A arquitetura do `Symbol` e da AST:** os nós e atributos continuam sendo os desenhados no CP3, com o acréscimo pontual de `decl`.
3. **Os programas de teste e relatórios esperados:** cada um dos doze arquivos em `programs/` e seus relatórios em `test/esperado/` foram elaborados e calculados manualmente por nós antes da execução dos testes automatizados.
4. **Nenhum trecho de código foi integrado sem inspeção:** todo código gerado ou sugerido como consulta foi lido, refatorado para o padrão do projeto e testado contra a suíte.

---

## Declaração

Declaramos que todo o conteúdo deste trabalho foi lido, compreendido e é de nossa responsabilidade, e que este log descreve com sinceridade o uso das ferramentas de IA listadas acima.

- Matheus Medrado Ferreira, 10/11/2026
- Christian Silva Rodrigues, 10/11/2026
