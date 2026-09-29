# Log de uso de ferramentas de IA

- **Disciplina:** GBC081 - Compiladores
- **Trabalho:** Checkpoint 1, especificação léxica e scanner
- **Integrantes:** Matheus Medrado e Christian [sobrenome]
- **Ferramentas usadas:** ZCode (agente de programação com IA)

## Como a IA foi usada

O trabalho foi feito por nós dois. A especificação da linguagem, os desenhos dos autômatos, a estrutura do scanner, os testes e os exemplos saíram da gente. A IA entrou como apoio em alguns momentos: para tirar dúvida de conceito, conferir uma decisão que já tínhamos tomado e ajudar a achar problema no código quando a gente travava. Nada foi entregue sem lermos, entendermos e conferirmos o resultado.

## Uso por etapa

### Especificação léxica (`especificacao-lexica.md`)

- **O que fizemos:** definimos o alfabeto, os tokens, as palavras reservadas e as regras de erro da linguagem.
- **Onde a IA entrou:** só para tirar dúvida de consulta, tipo como outras linguagens organizam as categorias de token. Quem escreveu a especificação fomos nós.
- **Como conferimos:** leitura em dupla, comparando com os exemplos de aula.

### Autômatos (`automatos/`)

- **O que fizemos:** desenhamos os AFDs de cada tipo de token e a visão geral, com as fontes em Graphviz.
- **Onde a IA entrou:** quase nada, só para conferir se algum caso da especificação tinha ficado de fora dos desenhos.
- **Como conferimos:** simulamos os AFDs na mão com os programas de exemplo.

### Scanner (`src/lexer/`)

- **O que fizemos:** implementamos o scanner a partir dos AFDs (um `case` por estado) e as classes de apoio (`Token`, `TokenType`, `LexicalError`, `Main`).
- **Onde a IA entrou:** para ajudar a entender problema pontual durante o desenvolvimento, como comportamento estranho quando o arquivo acaba no meio de um token. O código que ficou foi revisado por nós contra os desenhos dos autômatos.
- **Como conferimos:** rodando a suíte de testes e olhando as saídas.

### Testes e exemplos (`test/`, `exemplos/`)

- **O que fizemos:** escrevemos os testes e os programas de exemplo, com a saída correspondente de cada um.
- **Onde a IA entrou:** ela sugeriu alguns casos extras que valia a pena testar; a escolha do que entrava e a conferência das saídas foram nossas.
- **Como conferimos:** rodando `ScannerTest` e comparando com o comportamento esperado dos AFDs.

## Interações representativas

- **Dúvida de conceito:** na época da especificação, perguntamos para a IA como linguagens parecidas organizam os tokens. A resposta serviu para conferir se a nossa lista estava razoável; o que ficou e o que ficou de fora foi decisão nossa.
- **Depuração:** em um momento do scanner, um caso estava dando resultado errado e a gente não achava o motivo. Descrevemos a situação para a IA, ela apontou onde olhar, a gente entendeu o erro, corrigiu e o caso virou teste novo.

## O que não foi feito com IA

As decisões sobre a linguagem, os desenhos dos autômatos, o formato do scanner e os testes escolhidos foram definidos pela dupla. A IA não escreveu nenhuma parte do trabalho sozinha; em todo momento foi ferramenta de consulta e revisão, não de execução.

## Declaração

Declaramos que todo o conteúdo deste trabalho foi lido, compreendido e é de nossa responsabilidade, e que este log descreve com sinceridade o uso das ferramentas de IA listadas acima.

- Matheus Medrado, 29/09/2026
- Christian [sobrenome], 29/09/2026
