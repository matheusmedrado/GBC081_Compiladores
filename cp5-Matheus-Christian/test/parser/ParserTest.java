package parser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ast.Assign;
import ast.Bin;
import ast.CallStmt;
import ast.FuncDecl;
import ast.If;
import ast.Node;
import ast.Program;
import ast.Return;
import ast.VarDecl;
import lexer.Scanner;
import lexer.Token;
import lexer.TokenType;
import visitor.SourcePrinter;
import visitor.TreePrinter;

/**
 * Testes do parser sem dependência externa, no mesmo formato do ScannerTest:
 * cada teste é um método, as verificações lançam AssertionError e o main
 * conta os resultados.
 *
 * A suíte do CP2 continua inteira (mesmos veredictos, mesmas mensagens) e
 * ganha os testes da AST: árvore esperada em arquivo, precedência e
 * associatividade, parênteses redundantes e ida e volta.
 */
public class ParserTest {

    private static int passed = 0;
    private static final List<String> failed = new ArrayList<>();

    /** Tempo máximo de uma análise. Passar disso é tratado como loop infinito. */
    private static final long TIMEOUT_MS = 5000;

    public static void main(String[] args) {
        System.out.println("== programas válidos ==");
        run("programa do CP1 (fatorial)", ParserTest::programaDoCp1);
        run("tipos básicos e declarações", ParserTest::tiposEDeclaracoes);
        run("seleção e repetição", ParserTest::selecaoERepeticao);
        run("procedimentos e funções", ParserTest::procedimentosEFuncoes);
        run("precedência e operadores unários", ParserTest::precedencia);
        run("bloco solto e blocos vazios", ParserTest::blocos);
        run("programa vazio ou só com comentários", ParserTest::programaVazio);

        System.out.println();
        System.out.println("== árvore esperada ==");
        run("árvore do fatorial", ParserTest::arvoreFatorial);
        run("árvore de tipos e declarações", ParserTest::arvoreTipos);
        run("árvore de seleção e repetição", ParserTest::arvoreSelecao);
        run("árvore de procedimentos e funções", ParserTest::arvoreProcs);
        run("árvore dos primos", ParserTest::arvorePrimos);
        run("posição guardada em cada nó", ParserTest::posicoes);

        System.out.println();
        System.out.println("== precedência, associatividade e parênteses ==");
        run("precedências diferentes (a + b * c)", ParserTest::precedenciasDiferentes);
        run("mesmo operador repetido (a - b - c)", ParserTest::associatividadeAEsquerda);
        run("parênteses que mudam a árvore (a - (b - c))", ParserTest::parentesesMudamAArvore);
        run("as três árvores são diferentes", ParserTest::tresArvoresDiferentes);
        run("parênteses redundantes não viram nó", ParserTest::parentesesRedundantes);

        System.out.println();
        System.out.println("== ida e volta ==");
        run("ida e volta do fatorial", ParserTest::voltaFatorial);
        run("ida e volta de tipos e declarações", ParserTest::voltaTipos);
        run("ida e volta de seleção e repetição", ParserTest::voltaSelecao);
        run("ida e volta de procedimentos e funções", ParserTest::voltaProcs);
        run("ida e volta dos primos", ParserTest::voltaPrimos);
        run("ida e volta de precedência e operadores unários", ParserTest::voltaOperadores);
        run("ida e volta de bloco solto e blocos vazios", ParserTest::voltaBlocos);
        run("ida e volta do programa vazio ou só com comentários", ParserTest::voltaVazio);
        run("ida e volta dos programas de precedência e parênteses", ParserTest::voltaPrecedenciaEParenteses);

        System.out.println();
        System.out.println("== programas com um erro ==");
        run("falta ';' depois de expressão", ParserTest::faltaPontoEVirgulaDepoisDeExpressao);
        run("falta ';' depois de chamada", ParserTest::faltaPontoEVirgulaDepoisDeChamada);
        run("expressão incompleta", ParserTest::expressaoIncompleta);
        run("if sem '('", ParserTest::ifSemAbreParentese);
        run("')' a mais", ParserTest::fechaParenteseAMais);
        run("corpo de if sem chaves", ParserTest::corpoSemChaves);
        run("comparação encadeada (ε robusto)", ParserTest::comparacaoEncadeada);
        run("atribuição dentro da condição", ParserTest::atribuicaoNaCondicao);
        run("comando fora de função", ParserTest::comandoForaDeFuncao);
        run("parâmetro sem tipo", ParserTest::parametroSemTipo);
        run("else sem if", ParserTest::elseSemIf);
        run("falta '}' no fim do arquivo", ParserTest::faltaFechaChaveNoFim);
        run("erro léxico vira erro sintático", ParserTest::erroLexicoEsintatico);

        System.out.println();
        System.out.println("== vários erros numa rodada só ==");
        run("quatro erros em funções diferentes", ParserTest::quatroErros);
        run("mesmo erro repetido em cada função", ParserTest::mesmoErroEmCadaFuncao);
        run("erros em cascata não são reportados", ParserTest::semCascata);

        System.out.println();
        System.out.println("== árvore inválida e nenhum caminho de erro trava ==");
        run("com erro a árvore vem nula", ParserTest::arvoreNulaComErro);
        run("todos os prefixos de um programa", ParserTest::todosOsPrefixos);
        run("programa sem cada um dos tokens", ParserTest::semCadaToken);
        run("sequências aleatórias de tokens", ParserTest::sequenciasAleatorias);

        System.out.println();
        System.out.printf("%d testes, %d passaram, %d falharam%n",
                passed + failed.size(), passed, failed.size());
        if (!failed.isEmpty()) {
            System.exit(1);
        }
    }

    // ---------------------------------------------------------------
    // programas válidos
    // ---------------------------------------------------------------

    static final String[] FATORIAL = {
            "// calcula o fatorial e confere o resultado",
            "int fatorial(int n) {",
            "    int r = 1;",
            "    while (n > 1) {",
            "        r = r * n;",
            "        n = n - 1;",
            "    }",
            "    return r;",
            "}",
            "",
            "void main() {",
            "    /* testa com",
            "       um valor fixo */",
            "    int x = fatorial(5);",
            "    double media = (x + 0.5) / 2.0;",
            "    bool ok = x == 120 && !(media <= 0.0);",
            "    char c = 'x';",
            "    if (x != 120 || c == '\\n') {",
            "        imprime(\"errado\");",
            "    } else {",
            "        imprime(\"ok: 5! = 120\");",
            "    }",
            "    return;",
            "}",
    };

    static final String[] TIPOS = {
            "int contador;",
            "double taxa = 0.25;",
            "bool ativo = true, pronto = false;",
            "char letra = 'a', quebra = '\\n';",
            "string nome = \"Ana\", vazio = \"\";",
            "int a, b = 2, c;",
            "",
            "void main() {",
            "    int x = 10;",
            "    double y = 3.14, z;",
            "    bool b = !ativo;",
            "    char aspas = '\\'';",
            "    string s = \"diz \\\"oi\\\"\";",
            "    int negativo = -1;",
            "    x = 007;",
            "}",
    };

    static final String[] SELECAO = {
            "int classifica(int n) {",
            "    if (n < 0) {",
            "        return -1;",
            "    } else if (n == 0) {",
            "        return 0;",
            "    } else if (n <= 9) {",
            "        return 1;",
            "    } else {",
            "        return 2;",
            "    }",
            "}",
            "",
            "void tabuada() {",
            "    int i = 1;",
            "    while (i <= 10) {",
            "        int j = 1;",
            "        while (j <= 10) {",
            "            if (i * j % 2 == 0 && !(i == j)) {",
            "                imprime(i * j);",
            "            }",
            "            j = j + 1;",
            "        }",
            "        i = i + 1;",
            "    }",
            "    if (i > 10) {",
            "    }",
            "}",
    };

    static final String[] PROCS = {
            "void nada() {",
            "}",
            "",
            "void avisa(string msg) {",
            "    imprime(msg);",
            "    return;",
            "}",
            "",
            "double media3(double a, double b, double c) {",
            "    return (a + b + c) / 3.0;",
            "}",
            "",
            "int fib(int n) {",
            "    if (n < 2) {",
            "        return n;",
            "    }",
            "    return fib(n - 1) + fib(n - 2);",
            "}",
            "",
            "bool par(int n) {",
            "    return n % 2 == 0;",
            "}",
            "",
            "int main() {",
            "    nada();",
            "    avisa(\"oi\");",
            "    double m = media3(1.0, 2.0, fib(3));",
            "    bool p = par(fib(media3(1, 2, 3)));",
            "    imprime(m, p, 'x', \"fim\", true);",
            "    return 0;",
            "}",
    };

    static void programaDoCp1() {
        expectAccepted(FATORIAL);
    }

    static void tiposEDeclaracoes() {
        expectAccepted(TIPOS);
    }

    static void selecaoERepeticao() {
        expectAccepted(SELECAO);
    }

    static void procedimentosEFuncoes() {
        expectAccepted(PROCS);
    }

    static final String[] OPERADORES = {
            "void main() {",
            "    bool r = a || b && c == d < e + f * g;",
            "    r = !(a || b) && -x * -(y - z) % 3 >= 0 || !!pronto;",
            "    r = - -x;",
            "    r = ((((1))));",
            "    r = a != b == (c <= d);",
            "}",
    };

    static final String[] BLOCOS = {
            "void main() {",
            "    {",
            "        int escondido = 1;",
            "        { }",
            "    }",
            "    while (false) { }",
            "    if (true) { } else { }",
            "}",
    };

    static final String[] SO_COMENTARIOS = {"// nada aqui", "/* nem aqui */"};

    // os programas dos testes de precedência e de parênteses
    static final String PREC_DIFERENTES = "void main() { bool r = a + b * c; }";
    static final String PREC_ESQUERDA = "void main() { bool r = a - b - c; }";
    static final String PREC_DIREITA = "void main() { bool r = a - (b - c); }";
    static final String PAREN_REDUNDANTES = "void main() { bool r = (((a))); }";
    static final String SEM_PARENTESES = "void main() { bool r = a; }";

    static void precedencia() {
        expectAccepted(OPERADORES);
    }

    static void blocos() {
        expectAccepted(BLOCOS);
    }

    static void programaVazio() {
        expectAccepted("");
        expectAccepted(SO_COMENTARIOS);
    }

    // ---------------------------------------------------------------
    // árvore esperada (arquivo escrito à mão, no formato do impressor)
    // ---------------------------------------------------------------

    static void arvoreFatorial() {
        expectTree("fatorial", FATORIAL);
    }

    static void arvoreTipos() {
        expectTree("tipos", TIPOS);
    }

    static void arvoreSelecao() {
        expectTree("selecao", SELECAO);
    }

    static void arvoreProcs() {
        expectTree("procs", PROCS);
    }

    static void arvorePrimos() {
        expectTreeFile("primos", "exemplos/primos.txt");
    }

    /**
     * A posição não entra na impressão da árvore, então é conferida aqui, nó a
     * nó: a do primeiro token da construção, e no Bin a do operador
     * (ast-design.md, seção 3).
     */
    static void posicoes() {
        Program tree = parseResult(
                "// comentário antes do primeiro token",
                "int f(int n) {",
                "    int a, b = n * 2;",
                "    if (n < 0) {",
                "        a = -n;",
                "    }",
                "    g(a, 1);",
                "    return a + b;",
                "}").tree;
        expectPos(tree, 2, 1, "Program");
        FuncDecl f = (FuncDecl) tree.declarations.get(0);
        expectPos(f, 2, 1, "FuncDecl");
        expectPos(f.returnType, 2, 1, "tipo de retorno");
        expectPos(f.parameters.get(0), 2, 7, "Param");
        expectPos(f.body, 2, 14, "Block");

        VarDecl decl = (VarDecl) f.body.statements.get(0);
        expectPos(decl, 3, 5, "VarDecl");
        expectPos(decl.variables.get(0), 3, 9, "Var(a)");
        expectPos(decl.variables.get(1), 3, 12, "Var(b)");
        Bin times = (Bin) decl.variables.get(1).initializer;
        expectPos(times, 3, 18, "Bin(*)");
        expectPos(times.left, 3, 16, "Id(n)");
        expectPos(times.right, 3, 20, "Literal(2)");

        If test = (If) f.body.statements.get(1);
        expectPos(test, 4, 5, "If");
        expectPos(test.condition, 4, 11, "Bin(<)");
        Assign assign = (Assign) test.thenBranch.statements.get(0);
        expectPos(assign, 5, 9, "Assign");
        expectPos(assign.value, 5, 13, "Unary(-)");

        CallStmt call = (CallStmt) f.body.statements.get(2);
        expectPos(call, 7, 5, "CallStmt");
        expectPos(call.call, 7, 5, "Call");
        expectPos(call.call.arguments.get(1), 7, 10, "segundo argumento");

        Return ret = (Return) f.body.statements.get(3);
        expectPos(ret, 8, 5, "Return");
        expectPos(ret.value, 8, 14, "Bin(+)");
    }

    // ---------------------------------------------------------------
    // precedência, associatividade e parênteses
    // ---------------------------------------------------------------

    static void precedenciasDiferentes() {
        expectTree("prec1", PREC_DIFERENTES);
    }

    static void associatividadeAEsquerda() {
        expectTree("prec2", PREC_ESQUERDA);
    }

    static void parentesesMudamAArvore() {
        expectTree("prec3", PREC_DIREITA);
    }

    static void tresArvoresDiferentes() {
        String soma = treeOf(PREC_DIFERENTES);
        String esquerda = treeOf(PREC_ESQUERDA);
        String direita = treeOf(PREC_DIREITA);
        if (soma.equals(esquerda) || soma.equals(direita) || esquerda.equals(direita)) {
            throw new AssertionError("as três árvores precisavam ser diferentes");
        }
    }

    static void parentesesRedundantes() {
        String comParenteses = treeOf(PAREN_REDUNDANTES);
        String semParenteses = treeOf(SEM_PARENTESES);
        expectEquals(semParenteses, comParenteses, "(((a))) e a");
        expectEquals(readExpected("paren"), comParenteses, "arquivo esperado");
        // o nome do nó não pode carregar parênteses: (((a))) virou um Id só
        if (!comParenteses.contains("Id(a)") || comParenteses.split("\n", -1).length != 8) {
            throw new AssertionError("(((a))) não virou um único Id: " + comParenteses);
        }
    }

    // ---------------------------------------------------------------
    // ida e volta: reimprimir, ler de novo e reimprimir outra vez
    // ---------------------------------------------------------------

    static void voltaFatorial() {
        expectRoundTrip(FATORIAL);
    }

    static void voltaOperadores() {
        expectRoundTrip(OPERADORES);
    }

    static void voltaBlocos() {
        expectRoundTrip(BLOCOS);
    }

    static void voltaVazio() {
        expectRoundTrip("");
        expectRoundTrip(SO_COMENTARIOS);
    }

    static void voltaPrecedenciaEParenteses() {
        expectRoundTrip(PREC_DIFERENTES);
        expectRoundTrip(PREC_ESQUERDA);
        expectRoundTrip(PREC_DIREITA);
        expectRoundTrip(PAREN_REDUNDANTES);
        expectRoundTrip(SEM_PARENTESES);
    }

    static void voltaTipos() {
        expectRoundTrip(TIPOS);
    }

    static void voltaSelecao() {
        expectRoundTrip(SELECAO);
    }

    static void voltaProcs() {
        expectRoundTrip(PROCS);
    }

    static void voltaPrimos() {
        try {
            String source = new String(Files.readAllBytes(Paths.get("exemplos/primos.txt")),
                    StandardCharsets.UTF_8);
            expectRoundTrip(source.split("\n", -1));
        } catch (IOException e) {
            throw new AssertionError("não leu exemplos/primos.txt: " + e.getMessage());
        }
    }

    /**
     * Lê o programa, reimprime, lê o texto reimpresso e reimprime de novo.
     * O segundo texto precisa ser igual ao primeiro, e as duas árvores
     * precisam ter a mesma impressão.
     */
    static void expectRoundTrip(String... lines) {
        ParseResult first = parseResult(lines);
        if (!first.accepted()) {
            throw new AssertionError("esperava aceito, veio " + first.errors);
        }
        String reprinted = SourcePrinter.print(first.tree);
        String firstTree = TreePrinter.print(first.tree);

        ParseResult second = parseResult(reprinted.split("\n", -1));
        if (!second.accepted()) {
            throw new AssertionError("o texto reimpresso foi rejeitado: " + second.errors
                    + "\n" + reprinted);
        }
        String reprintedAgain = SourcePrinter.print(second.tree);
        expectEquals(reprinted, reprintedAgain, "segunda reimpressão");
        expectEquals(firstTree, TreePrinter.print(second.tree), "árvores da ida e volta");
    }

    // ---------------------------------------------------------------
    // programas com um erro
    // ---------------------------------------------------------------

    static void faltaPontoEVirgulaDepoisDeExpressao() {
        // depois de "a + b", o lookahead 'return' não está em FIRST nem em
        // FOLLOW de MulRest: o erro sai ali, no começo da linha seguinte
        List<SyntaxError> e = parse(
                "int soma(int a, int b) {",
                "    int r = a + b",
                "    return r;",
                "}");
        expectErrors(e, 1);
        expectError(e.get(0), 3, 5, "encontrado 'return', esperado operador, '(', ')', ';' ou ','");
    }

    static void faltaPontoEVirgulaDepoisDeChamada() {
        // aqui quem percebe é o match(';') de Stmt, e o erro aponta logo depois do ')'
        List<SyntaxError> e = parse(
                "void main() {",
                "    imprime(x)",
                "    x = 1;",
                "}");
        expectErrors(e, 1);
        expectError(e.get(0), 2, 15, "esperado ';' depois de ')'");
    }

    static void expressaoIncompleta() {
        List<SyntaxError> e = parse(
                "void main() {",
                "    x = 3 + ;",
                "    y = 2;",
                "}");
        expectErrors(e, 1);
        expectError(e.get(0), 2, 13, "encontrado ';', esperado expressão");
    }

    static void ifSemAbreParentese() {
        List<SyntaxError> e = parse(
                "void main() {",
                "    if x > 0) {",
                "        x = 0;",
                "    }",
                "}");
        expectErrors(e, 1);
        expectError(e.get(0), 2, 7, "esperado '(' depois de 'if'");
    }

    static void fechaParenteseAMais() {
        List<SyntaxError> e = parse(
                "void main() {",
                "    while (x > 0)) {",
                "        x = x - 1;",
                "    }",
                "}",
                "int depois() {",
                "    return 1;",
                "}");
        expectErrors(e, 1);
        expectError(e.get(0), 2, 18, "encontrado ')' a mais antes de '{'");
    }

    static void corpoSemChaves() {
        // na nossa gramática o corpo de if/while é sempre um Block (seção 2.4 do documento)
        List<SyntaxError> e = parse(
                "int abs(int x) {",
                "    if (x < 0) x = -x;",
                "    return x;",
                "}",
                "int main() {",
                "    return abs(-3);",
                "}");
        expectErrors(e, 1);
        expectError(e.get(0), 2, 15, "esperado '{' depois de ')': o corpo precisa estar entre chaves");
    }

    static void comparacaoEncadeada() {
        // RelRest → RelOp AddExpr | ε não repete. Depois de "a < b", EqRest vê
        // '<', que não está em FIRST(EqRest) nem em FOLLOW(EqRest). Na versão
        // ingênua do ε o erro só apareceria mais tarde, fora da expressão.
        List<SyntaxError> e = parse(
                "void main() {",
                "    bool ok = a < b < c;",
                "}");
        expectErrors(e, 1);
        expectError(e.get(0), 2, 21, "encontrado '<', esperado '==', '!=', '&&', '||', ')', ';' ou ','");
    }

    static void atribuicaoNaCondicao() {
        // atribuição é comando, não expressão: '=' não pode vir depois de um operando
        List<SyntaxError> e = parse(
                "void main() {",
                "    if (x = 1) {",
                "        y = 2;",
                "    }",
                "}");
        expectErrors(e, 1);
        expectError(e.get(0), 2, 11, "encontrado '=', esperado operador, '(', ')', ';' ou ','");
    }

    static void comandoForaDeFuncao() {
        List<SyntaxError> e = parse(
                "int x;",
                "x = 1;",
                "void main() {",
                "    x = 2;",
                "}");
        expectErrors(e, 1);
        expectError(e.get(0), 2, 1, "encontrado 'x', esperado declaração ou fim do arquivo");
    }

    static void parametroSemTipo() {
        List<SyntaxError> e = parse(
                "int soma(a, b) {",
                "    return a + b;",
                "}");
        expectErrors(e, 1);
        expectError(e.get(0), 1, 10, "encontrado 'a', esperado tipo ou ')'");
    }

    static void elseSemIf() {
        List<SyntaxError> e = parse(
                "void main() {",
                "    x = 1;",
                "    else {",
                "        x = 2;",
                "    }",
                "}");
        expectErrors(e, 1);
        expectError(e.get(0), 3, 5, "encontrado 'else', esperado comando ou '}'");
    }

    static void faltaFechaChaveNoFim() {
        List<SyntaxError> e = parse(
                "void main() {",
                "    while (true) {",
                "        x = 1;",
                "    }");
        expectErrors(e, 1);
        expectError(e.get(0), 5, 1, "encontrado fim do arquivo, esperado comando ou '}'");
    }

    static void erroLexicoEsintatico() {
        // o scanner descarta o '@' e o parser vê "10 2"
        Scanner s = new Scanner("void main() {\n    int total = 10 @ 2;\n}\n");
        Parser p = new Parser(s);
        ParseResult result = parseWithTimeout(p);
        expectEquals(false, result.accepted(), "parse()");
        expectEquals(1, s.getErrors().size(), "erros léxicos");
        expectErrors(p.getErrors(), 1);
        expectError(p.getErrors().get(0), 2, 22, "encontrado '2', esperado operador, ')', ';' ou ','");
    }

    // ---------------------------------------------------------------
    // vários erros numa rodada só
    // ---------------------------------------------------------------

    static void quatroErros() {
        // mesmo programa de exemplos/varios-erros.txt
        List<SyntaxError> e = parse(
                "// quatro erros sintáticos, um em cada lugar; o resto do programa está certo",
                "int soma(int a, int b) {",
                "    int r = a + b",
                "    return r;",
                "}",
                "",
                "void mostra(string msg) {",
                "    if (msg == \"\") {",
                "        imprime(\"vazio\";",
                "    } else {",
                "        imprime(msg);",
                "    }",
                "}",
                "",
                "int main() {",
                "    int x = soma(1, 2);",
                "    while (x > 0 {",
                "        x = x - * 1;",
                "    }",
                "    mostra(\"fim\");",
                "    return 0;",
                "}");
        expectErrors(e, 4);
        expectError(e.get(0), 4, 5, "encontrado 'return'");
        expectError(e.get(1), 9, 24, "encontrado ';', esperado ')' ou ','");
        expectError(e.get(2), 17, 18, "encontrado '{', esperado operador, ')', ';' ou ','");
        expectError(e.get(3), 18, 17, "encontrado '*', esperado expressão");
    }

    static void mesmoErroEmCadaFuncao() {
        // cada match(';') que falha reporta de novo, porque entre um e outro
        // houve match com sucesso e o modo pânico já tinha acabado
        List<SyntaxError> e = parse(
                "void a() {",
                "    f(1)",
                "}",
                "void b() {",
                "    g(2)",
                "}",
                "void c() {",
                "    h(3)",
                "}");
        expectErrors(e, 3);
        expectError(e.get(0), 2, 9, "esperado ';' depois de ')'");
        expectError(e.get(1), 5, 9, "esperado ';' depois de ')'");
        expectError(e.get(2), 8, 9, "esperado ';' depois de ')'");
    }

    static void semCascata() {
        // Params para no '{' (âncora); o match(')') que falha logo depois é
        // cascata do mesmo erro e não é reportado
        List<SyntaxError> e = parse(
                "void f( {",
                "    x = 1;",
                "}",
                "int g() {",
                "    return 2;",
                "}");
        expectErrors(e, 1);
        expectError(e.get(0), 1, 9, "encontrado '{', esperado tipo ou ')'");
    }

    // ---------------------------------------------------------------
    // árvore inválida e nenhum caminho de erro trava
    // ---------------------------------------------------------------

    static void arvoreNulaComErro() {
        ParseResult result = parseResult(
                "void main() {",
                "    x = 3 + ;",
                "}");
        if (result.accepted() || result.tree != null) {
            throw new AssertionError("com erro a árvore precisava vir nula, veio " + result.tree);
        }
        expectErrors(result.errors, 1);
    }

    static void todosOsPrefixos() {
        List<Token> tokens = tokensOf(FATORIAL);
        for (int n = 0; n < tokens.size(); n++) {
            checkTerminates(tokens.subList(0, n));
        }
    }

    static void semCadaToken() {
        List<Token> tokens = tokensOf(FATORIAL);
        for (int i = 0; i < tokens.size() - 1; i++) {
            List<Token> without = new ArrayList<>(tokens.subList(0, tokens.size() - 1));
            without.remove(i);
            checkTerminates(without);
        }
    }

    static void sequenciasAleatorias() {
        Random random = new Random(2026);
        TokenType[] types = TokenType.values();
        for (int round = 0; round < 2000; round++) {
            List<Token> tokens = new ArrayList<>();
            int length = random.nextInt(80);
            for (int i = 0; i < length; i++) {
                TokenType type = types[random.nextInt(types.length - 1)];   // tudo menos EOF
                tokens.add(new Token(type, sample(type), 1, 1));
            }
            checkTerminates(tokens);
        }
    }

    /**
     * Monta o fonte a partir dos tokens e exige que a análise termine sem
     * lançar nada. Também confere o limite do modo pânico: cada erro reportado
     * depois do primeiro precisa de um match com sucesso antes dele, então há
     * no máximo um erro a mais que o número de tokens.
     */
    static void checkTerminates(List<Token> tokens) {
        StringBuilder source = new StringBuilder();
        for (Token t : tokens) {
            source.append(t.lexeme).append(' ');
        }
        Parser p = new Parser(new Scanner(source.toString()));
        ParseResult result = parseWithTimeout(p);
        if (result.errors.size() > tokens.size() + 1) {
            throw new AssertionError(result.errors.size() + " erros para " + tokens.size()
                    + " tokens em: " + source);
        }
        if (!result.errors.isEmpty() && result.tree != null) {
            throw new AssertionError("árvore apresentada como válida com erros em: " + source);
        }
    }

    static List<Token> tokensOf(String... lines) {
        Scanner s = new Scanner(String.join("\n", lines));
        List<Token> tokens = s.scanAll();
        expectEquals(0, s.getErrors().size(), "erros léxicos no programa de base");
        return tokens;
    }

    static String sample(TokenType type) {
        switch (type) {
            case IDENTIFIER:     return "x";
            case INT_LITERAL:    return "1";
            case DOUBLE_LITERAL: return "1.5";
            case STRING_LITERAL: return "\"s\"";
            case CHAR_LITERAL:   return "'c'";
            default:             return TokenNames.lexeme(type);
        }
    }

    // ---------------------------------------------------------------
    // infraestrutura dos testes
    // ---------------------------------------------------------------

    static void run(String name, Runnable test) {
        try {
            test.run();
            passed++;
            System.out.println("  [ok]    " + name);
        } catch (AssertionError | RuntimeException e) {
            failed.add(name);
            System.out.println("  [FALHA] " + name + ": " + e.getMessage());
        }
    }

    /** Analisa o programa (uma linha por argumento) e devolve os erros sintáticos. */
    static List<SyntaxError> parse(String... lines) {
        Scanner s = new Scanner(String.join("\n", lines) + "\n");
        Parser p = new Parser(s);
        parseWithTimeout(p);
        expectEquals(0, s.getErrors().size(), "erros léxicos");
        return p.getErrors();
    }

    static ParseResult parseResult(String... lines) {
        Scanner s = new Scanner(String.join("\n", lines) + "\n");
        Parser p = new Parser(s);
        ParseResult result = parseWithTimeout(p);
        expectEquals(0, s.getErrors().size(), "erros léxicos");
        return result;
    }

    static void expectAccepted(String... lines) {
        ParseResult result = parseResult(lines);
        if (!result.accepted()) {
            throw new AssertionError("esperava aceito, veio " + result.errors);
        }
    }

    /** Compara a impressão da árvore com o arquivo test/esperado/nome.txt. */
    static void expectTree(String name, String... lines) {
        expectEquals(readExpected(name), treeOf(lines), "árvore de " + name);
    }

    static void expectTreeFile(String name, String path) {
        try {
            String source = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
            expectTree(name, source.split("\n", -1));
        } catch (IOException e) {
            throw new AssertionError("não leu " + path + ": " + e.getMessage());
        }
    }

    static String treeOf(String... lines) {
        ParseResult result = parseResult(lines);
        if (!result.accepted()) {
            throw new AssertionError("esperava aceito, veio " + result.errors);
        }
        Program tree = result.tree;
        if (tree.line < 1 || tree.column < 1) {
            throw new AssertionError("posição inválida na raiz: " + tree.line + ":" + tree.column);
        }
        return TreePrinter.print(tree);
    }

    static String readExpected(String name) {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get("test/esperado/" + name + ".txt"));
            String text = new String(bytes, StandardCharsets.UTF_8);
            if (!text.endsWith("\n")) {
                text = text + "\n";
            }
            return text;
        } catch (IOException e) {
            throw new AssertionError("não leu test/esperado/" + name + ".txt: " + e.getMessage());
        }
    }

    /**
     * Roda parse() numa thread separada. Se não terminar no prazo, é loop
     * infinito; se estourar a pilha, é recursão infinita; se lançar qualquer
     * outra exceção, o teste falha.
     */
    static ParseResult parseWithTimeout(final Parser p) {
        final ParseResult[] result = new ParseResult[1];
        final Throwable[] thrown = new Throwable[1];
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    result[0] = p.parse();
                } catch (Throwable e) {
                    thrown[0] = e;
                }
            }
        });
        t.setDaemon(true);
        t.start();
        try {
            t.join(TIMEOUT_MS);
        } catch (InterruptedException e) {
            throw new AssertionError("interrompido");
        }
        if (t.isAlive()) {
            throw new AssertionError("parse() não terminou em " + TIMEOUT_MS + " ms (loop infinito?)");
        }
        if (thrown[0] instanceof StackOverflowError) {
            throw new AssertionError("estouro de pilha (recursão sem consumir token?)");
        }
        if (thrown[0] != null) {
            throw new AssertionError("parse() lançou " + thrown[0]);
        }
        return result[0];
    }

    static void expectErrors(List<SyntaxError> errors, int n) {
        if (errors.size() != n) {
            throw new AssertionError("esperava " + n + " erro(s), veio " + errors);
        }
    }

    static void expectError(SyntaxError e, int line, int column, String messagePart) {
        if (e.line != line || e.column != column || !e.message.contains(messagePart)) {
            throw new AssertionError(String.format("esperava erro em %d:%d contendo \"%s\", veio %s",
                    line, column, messagePart, e));
        }
    }

    static void expectPos(Node node, int line, int column, String what) {
        if (node.line() != line || node.column() != column) {
            throw new AssertionError(String.format("posição de %s: esperava %d:%d, veio %d:%d",
                    what, line, column, node.line(), node.column()));
        }
    }

    static void expectEquals(Object expected, Object actual, String what) {
        if (!expected.equals(actual)) {
            throw new AssertionError(what + ": esperava " + expected + ", veio " + actual);
        }
    }
}
