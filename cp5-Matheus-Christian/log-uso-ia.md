# Log de uso de ferramentas de IA

- **Disciplina:** GBC081 - Compiladores
- **Trabalho:** Checkpoint 5, verificador de tipos
- **Integrantes:** Matheus Medrado Ferreira e Christian Silva Rodrigues
- **Ferramentas usadas:** ZCode (agente de programação com IA), durante o desenvolvimento

## Como a IA foi usada

O Checkpoint 5 adiciona o verificador de tipos sobre a AST e a tabela de símbolos do CP4. As regras de tipos, a tabela de operadores e as onze decisões foram escritas pela dupla, primeiro à mão (`tipos-a-mao.pdf`) e depois passadas a limpo em `tipos-design.md`. A IA entrou em seis pontos de apoio: a notação das regras, os prós e contras de uma decisão, o esqueleto da classe `Type`, a contenção do efeito cascata, a estratégia do aviso de variável sem valor e o formato dos testes de unidade sem parser.

As saídas esperadas em `test/esperado/` foram escritas a partir das regras de `tipos-design.md`, antes de rodar o verificador. Nenhuma sugestão da IA foi incorporada sem conferência contra o `tipos-design.md` e a suíte de testes.

## O que fizemos, por etapa

### Regras de tipos (`tipos-a-mao.pdf`, `tipos-design.md`)

- **O que fizemos:** a notação (`Γ`, `e : T`, `S ok`, a compatibilidade `⪯`), as regras de cada expressão e comando, a tabela de operadores e o formato das mensagens.
- **Onde a IA entrou — teoria e notação:** pedimos o formato de dedução natural da Aula 12 aplicado ao `if` e às chamadas de função.
- **Como usamos:** aproveitamos a estrutura (premissas acima da linha, conclusão abaixo) para desenhar as regras à mão no `tipos-a-mao.pdf`. As regras em si, como [IF], [IF-SEM], [CHAMADA] e [CHAMADA-CMD], são do grupo.

### Decisão 7, recursão mútua (`tipos-design.md`, seção 4)

- **O que fizemos:** mantivemos a decisão 4.3 do CP4: recursão mútua não é permitida, nem por declaração antecipada nem por duas passadas.
- **Onde a IA entrou — trade-offs:** pedimos os prós e contras das abordagens possíveis (pré-passada coletando as assinaturas globais, declaração antecipada, proibição).
- **Como usamos:** escolhemos a opção mais simples, que não muda a identificação do CP4, e escrevemos a justificativa com nossas próprias palavras. O comportamento está travado em `test/programas/d07_recursao_mutua.txt`.

### Representação dos tipos (`src/types/Type.java`, `src/types/TypeRules.java`)

- **O que fizemos:** `Type` com uma instância por tipo (`INT`, `DOUBLE`, `CHAR`, `BOOL`, `STRING`, `VOID` e `ERROR`), comparada por referência, e `TypeRules` com a compatibilidade e a tabela de operadores.
- **Onde a IA entrou — boilerplate:** pedimos o esqueleto da classe/enum `Type` e de métodos utilitários como `isNumeric`.
- **Como usamos:** aproveitamos os métodos auxiliares (`Type.named` e `TypeRules.isNumeric`). A hierarquia `char < int < double`, a regra `⪯` e cada linha da tabela de operadores foram escritas por nós a partir do `tipos-design.md`.

### Tipo de erro e efeito cascata (`Type.ERROR`, regra [ERRO])

- **O que fizemos:** o tipo `erro` é o tipo de uma expressão que já gerou mensagem, ou cujo nome não foi declarado.
- **Onde a IA entrou — arquitetura:** perguntamos como estruturar o tipo de erro para conter o efeito cascata.
- **Como usamos:** adotamos a regra de que o tipo de erro é compatível com qualquer tipo e não gera mensagem nova. Ela está nas primeiras linhas de `binary`, `unary` e `assignable` e em `isCondition`. `test/programas/varios_erros.txt` confere que oito erros saem em ordem, sem mensagens duplicadas.

### Variáveis sem valor (`src/visitor/TypeChecker.java`, seção 5 de `tipos-design.md`)

- **O que fizemos:** uma análise conservadora, reportada como aviso, que não rejeita o programa.
- **Onde a IA entrou — algoritmo:** pedimos uma estratégia simples para o aviso conservador de variáveis não inicializadas.
- **Como usamos:** implementamos o rastreio da inicialização no escopo com o conjunto `unset` do `TypeChecker`, com as regras de `if`, `while` e `return` que definimos na seção 5. O teste é `test/programas/aviso_sem_valor.txt`.

### Testes de unidade das regras (`test/types/TypeRulesTest.java`)

- **O que fizemos:** testes das regras sem passar pelo parser, além do `TypeCheckTest`, que compara a saída de cada programa de `test/programas/` com `test/esperado/`.
- **Onde a IA entrou — testes:** pedimos a estrutura de um teste de unidade que instancia nós da AST diretamente (`Bin` e `Unary` sobre `Literal`), sem parser.
- **Como usamos:** replicamos o padrão para cobrir todas as linhas da tabela de operadores, aceitas e recusadas, a compatibilidade `⪯` e o tipo de erro sem cascata.

---

## Resumo das interações

| # | Categoria | Prompt / objetivo | Uso real / decisão do grupo |
|---|---|---|---|
| 1 | Teoria / notação | Formato de dedução natural (Aula 12) para `if` e chamadas de função. | Usamos a estrutura para desenhar as regras à mão no `tipos-a-mao.pdf`. |
| 2 | Trade-offs | Prós e contras das abordagens para a decisão 7 (recursão mútua). | O grupo escolheu a opção mais simples e escreveu a justificativa com as próprias palavras. |
| 3 | Boilerplate | Esqueleto da classe/enum `Type` e de seus métodos utilitários (`isNumeric` etc.). | Aproveitamos os métodos auxiliares em `Type.java` e `TypeRules.java`. |
| 4 | Arquitetura | Como estruturar o tipo de erro para conter o efeito cascata. | Adotamos a regra de que `Type.ERROR` é compatível com qualquer tipo, para não duplicar erros. |
| 5 | Algoritmo | Estratégia simples para o aviso conservador de variáveis não inicializadas. | Implementamos o rastreio da inicialização no escopo (o conjunto `unset`). |
| 6 | Testes | Estrutura de teste de unidade instanciando nós da AST diretamente (sem parser). | Replicamos o padrão para cobrir todas as linhas da tabela de operadores. |

---

## O que não foi feito com IA

1. **As regras e a tabela de operadores:** escritas pela dupla, à mão no `tipos-a-mao.pdf` e passadas a limpo no `tipos-design.md`.
2. **As onze decisões:** a escolha, a alternativa descartada e o motivo de cada uma são do grupo.
3. **Os programas de teste e as saídas esperadas:** cada arquivo em `test/programas/` e sua saída em `test/esperado/` foram escritos a partir das regras, antes de rodar o verificador.
4. **Nenhum trecho de código foi integrado sem inspeção:** todo código sugerido foi lido, adaptado ao padrão do projeto e testado contra as seis suítes.

---

## Declaração

Declaramos que todo o conteúdo deste trabalho foi lido, compreendido e é de nossa responsabilidade, e que este log descreve com sinceridade o uso das ferramentas de IA listadas acima.

- Matheus Medrado Ferreira, 25/11/2026
- Christian Silva Rodrigues, 25/11/2026
