package symbols;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import ast.Assign;
import ast.Call;
import ast.Id;
import ast.Node;
import ast.Program;
import lexer.Scanner;
import parser.ParseResult;
import parser.Parser;
import visitor.IdentificationPass;
import visitor.IdentificationPass.Use;

/**
 * Passada de identificação. A saída esperada está em test/esperado/id-*.txt,
 * escrita a partir do desenho em escopo-design.md, e o teste confere o
 * relatório contra esse arquivo.
 */
public class IdentificationTest {

    private static int passed = 0;
    private static final List<String> failed = new ArrayList<String>();
    private static final long TIMEOUT_MS = 5000;

    public static void main(String[] args) {
        System.out.println("== programas obrigatórios ==");
        run("válido com sombreamento", IdentificationTest::sombreamento);
        run("redeclaração", IdentificationTest::redeclaracao);
        run("nome não declarado", IdentificationTest::naoDeclarado);

        System.out.println();
        System.out.println("== um programa por decisão ==");
        run("declarar antes de usar", IdentificationTest::declaraAntes);
        run("ponto da declaração", IdentificationTest::pontoDeclaracao);
        run("recursão mútua não é permitida", IdentificationTest::mutua);
        run("parâmetros e corpo no mesmo escopo", IdentificationTest::paramCorpo);
        run("um só espaço de nomes", IdentificationTest::espacoNomes);

        System.out.println();
        System.out.println("== casos pedidos à parte ==");
        run("três erros na ordem do texto", IdentificationTest::tresErros);
        run("procedimento recursivo", IdentificationTest::recursivo);
        run("uso depois do fechamento do bloco", IdentificationTest::depoisDoBloco);
        run("função devolve valor", IdentificationTest::funcao);

        System.out.println();
        System.out.println("== não quebra e não entra em laço ==");
        run("árvore nula", IdentificationTest::arvoreNula);
        run("programa vazio", IdentificationTest::programaVazio);
        run("fatorial do CP3", IdentificationTest::fatorial);
        run("primos do CP3", IdentificationTest::primos);
        run("entrada com erro sintático não é analisada", IdentificationTest::erroSintatico);

        System.out.println();
        System.out.printf("%d testes, %d passaram, %d falharam%n",
                passed + failed.size(), passed, failed.size());
        if (!failed.isEmpty()) {
            System.exit(1);
        }
    }

    static void sombreamento() {
        IdentificationPass pass = check("sombreamento");
        List<Use> uses = pass.uses();
        Use xFunc = uses.get(0);
        Use n = uses.get(1);
        Use xBloco = uses.get(2);
        Use y = uses.get(4);
        Use xDepois = uses.get(6);
        Use xGlobal = uses.get(8);

        expectEquals(1, xFunc.symbol.level, "x da função é nível 1");
        expectEquals(4, xFunc.symbol.declaration.line(), "x da função é o da linha 4");
        expectEquals(Category.VARIABLE, xFunc.symbol.category, "x da função é variável");

        expectEquals(1, n.symbol.level, "n é nível 1");
        expectEquals(Category.PARAMETER, n.symbol.category, "n é parâmetro");
        expectSame(n.symbol, uses.get(3).symbol, "o n da linha 8 é o parâmetro");
        expectSame(n.symbol, uses.get(7).symbol, "o n da linha 14 é o parâmetro");

        expectEquals(2, xBloco.symbol.level, "x do bloco é nível 2");
        expectEquals(7, xBloco.symbol.declaration.line(), "x do bloco é o da linha 7");
        expectSame(xBloco.symbol, uses.get(5).symbol, "o x da linha 11 é o do bloco");

        expectEquals(3, y.symbol.level, "y é nível 3");
        expectSame(xFunc.symbol, xDepois.symbol, "depois do bloco, x volta ao da função");
        if (xDepois.symbol == xBloco.symbol || xGlobal.symbol == xFunc.symbol) {
            throw new AssertionError("os três x precisam ser símbolos diferentes");
        }
        expectEquals(0, xGlobal.symbol.level, "x do main é o global");
        expectEquals(1, xGlobal.symbol.declaration.line(), "x do main é o da linha 1");
        expectNull(xGlobal.symbol.type, "type ainda é null");
        expectEquals(0, xGlobal.symbol.displacement, "displacement ainda é 0");
    }

    static void redeclaracao() {
        IdentificationPass pass = check("redeclaracao");
        expectEquals(5, pass.uses().get(0).symbol.declaration.line(), "b fica na primeira, a da linha 5");
        expectEquals(1, pass.uses().get(1).symbol.declaration.line(), "a fica na primeira, a da linha 1");
    }

    static void naoDeclarado() {
        IdentificationPass pass = check("nao-declarado");
        expectNull(pass.uses().get(1).symbol, "b não tem símbolo");
        if (pass.uses().get(0).symbol == null) {
            throw new AssertionError("a foi declarado e precisa resolver");
        }
    }

    static void declaraAntes() {
        check("declara-antes");
    }

    static void pontoDeclaracao() {
        IdentificationPass pass = check("ponto-declaracao");
        Use c = pass.uses().get(1);
        expectEquals("c", c.name, "o segundo uso é c");
        expectEquals(4, c.symbol.declaration.line(), "c = c resolve para o próprio c");
        if (c.symbol.declaration == c.site) {
            throw new AssertionError("a declaração e o uso são nós diferentes");
        }
    }

    static void mutua() {
        IdentificationPass pass = check("mutua");
        expectNull(pass.uses().get(1).symbol, "impar ainda não existe");
        expectEquals(Category.PROCEDURE, pass.uses().get(4).symbol.category, "par é procedimento");
        expectEquals(1, pass.uses().get(4).symbol.declaration.line(), "par é o da linha 1");
    }

    static void paramCorpo() {
        IdentificationPass pass = check("param-corpo");
        Use inner = pass.uses().get(0);
        expectEquals(11, inner.symbol.declaration.line(), "o a de dentro é o do bloco");
        expectEquals(2, inner.symbol.level, "o bloco interno é nível 2");
        expectEquals(Category.VARIABLE, inner.symbol.category, "o a de dentro é variável");
    }

    static void espacoNomes() {
        IdentificationPass pass = check("espaco-nomes");
        Use assign = pass.uses().get(0);
        Use call = pass.uses().get(1);
        expectSame(assign.symbol, call.symbol, "g() e g = 1 são o mesmo símbolo");
        expectEquals(Category.VARIABLE, call.symbol.category, "a chamada achou a variável");
        if (!(call.site instanceof Call)) {
            throw new AssertionError("o segundo uso de g devia ser a chamada");
        }
        Use left = pass.uses().get(2);
        Use right = pass.uses().get(3);
        expectSame(left.symbol, right.symbol, "os dois h são o parâmetro");
        expectEquals(Category.PARAMETER, left.symbol.category, "h sombreia o procedimento");
        if (!(left.site instanceof Assign) || !(right.site instanceof Id)) {
            throw new AssertionError("h = h é uma atribuição e um Id");
        }
    }

    static void tresErros() {
        IdentificationPass pass = check("tres-erros");
        if (pass.errors().size() < 3) {
            throw new AssertionError("esperava pelo menos 3 erros, veio " + pass.errors().size());
        }
        int previous = 0;
        for (String error : pass.errors()) {
            int line = lineOf(error);
            if (line < previous) {
                throw new AssertionError("erro fora da ordem do texto: " + error);
            }
            previous = line;
        }
        Use b = pass.uses().get(1);
        Use bDepois = pass.uses().get(4);
        expectSame(b.symbol, bDepois.symbol, "o segundo b não substituiu o primeiro");
        expectEquals(3, b.symbol.declaration.line(), "b continua o da linha 3");
    }

    static void recursivo() {
        IdentificationPass pass = check("recursivo");
        Use call = pass.uses().get(1);
        expectEquals(Category.PROCEDURE, call.symbol.category, "contagem é procedimento");
        expectEquals(1, call.symbol.declaration.line(), "a chamada resolve para a própria declaração");
        expectEquals(0, call.symbol.level, "o nome está no escopo de fora");
        if (!(call.site instanceof Call)) {
            throw new AssertionError("a recursão é uma chamada");
        }
    }

    static void depoisDoBloco() {
        IdentificationPass pass = check("depois-do-bloco");
        expectEquals(3, pass.uses().get(0).symbol.declaration.line(), "a de dentro resolve");
        expectNull(pass.uses().get(2).symbol, "a de fora do bloco não resolve");
    }

    static void funcao() {
        IdentificationPass pass = check("funcao");
        expectEquals(Category.PARAMETER, pass.uses().get(0).symbol.category, "n é parâmetro");
        expectEquals(Category.FUNCTION, pass.uses().get(1).symbol.category, "id devolve valor, é função");
        expectEquals(0, pass.uses().get(1).symbol.level, "id está no global");
    }

    static void arvoreNula() {
        IdentificationPass pass = analyze(null);
        if (!pass.errors().isEmpty() || !pass.uses().isEmpty()) {
            throw new AssertionError("árvore nula não tem o que analisar: " + pass.report());
        }
        expectEquals("erros:\nusos:\n", pass.report(), "relatório vazio");
    }

    static void programaVazio() {
        IdentificationPass pass = analyze(parse("// só comentário\n"));
        if (!pass.errors().isEmpty() || !pass.uses().isEmpty()) {
            throw new AssertionError("programa vazio: " + pass.report());
        }
    }

    static void fatorial() {
        IdentificationPass pass = analyze(parse(read("exemplos/fatorial.txt")));
        expectEquals(2, pass.errors().size(), "só as duas chamadas de imprime");
        expectEquals("linha 19: 'imprime' não declarado", pass.errors().get(0), "primeiro imprime");
        expectEquals("linha 21: 'imprime' não declarado", pass.errors().get(1), "segundo imprime");
        boolean achou = false;
        for (Use use : pass.uses()) {
            if ("fatorial".equals(use.name)) {
                achou = true;
                expectEquals(Category.FUNCTION, use.symbol.category, "fatorial é função");
                expectEquals(2, use.symbol.declaration.line(), "fatorial declarado na linha 2");
            }
        }
        if (!achou) {
            throw new AssertionError("não achei o uso de fatorial");
        }
    }

    static void primos() {
        IdentificationPass pass = analyze(parse(read("exemplos/primos.txt")));
        expectEquals(4, pass.errors().size(), "quatro chamadas de imprime");
        expectEquals("linha 27: 'imprime' não declarado", pass.errors().get(0), "imprime 27");
        expectEquals("linha 28: 'imprime' não declarado", pass.errors().get(1), "imprime 28");
        expectEquals("linha 46: 'imprime' não declarado", pass.errors().get(2), "imprime 46");
        expectEquals("linha 51: 'imprime' não declarado", pass.errors().get(3), "imprime 51");
    }

    static void erroSintatico() {
        ParseResult result = new Parser(new Scanner(read("exemplos/com-erros.txt"))).parse();
        if (result.accepted()) {
            throw new AssertionError("com-erros.txt devia ser rejeitado pelo parser");
        }
        IdentificationPass pass = analyze(result.tree);
        if (!pass.errors().isEmpty()) {
            throw new AssertionError("a passada não roda sobre árvore rejeitada");
        }
    }

    /** Confere o relatório contra test/esperado/id-nome.txt e os invariantes de cada uso. */
    static IdentificationPass check(String name) {
        IdentificationPass pass = analyze(parse(read("programs/" + name + ".txt")));
        String expected = read("test/esperado/id-" + name + ".txt");
        if (!expected.endsWith("\n")) {
            expected = expected + "\n";
        }
        if (!pass.report().equals(expected)) {
            throw new AssertionError(name + "\n--- obtido ---\n" + pass.report()
                    + "--- esperado ---\n" + expected);
        }
        for (Use use : pass.uses()) {
            expectSame(use.symbol, declOf(use.site), "decl do nó " + use.name + " na linha " + use.line);
            if (use.symbol == null) {
                continue;
            }
            if (use.symbol.declaration == null) {
                throw new AssertionError("símbolo sem nó de declaração: " + use.name);
            }
            if (use.symbol.declaration == use.site) {
                throw new AssertionError("declaração e uso são o mesmo nó: " + use.name);
            }
            if (use.symbol.level < 0) {
                throw new AssertionError("nível negativo: " + use.symbol);
            }
            expectEquals(use.name, use.symbol.name, "nome do símbolo");
            expectNull(use.symbol.type, "type não é preenchido aqui");
            expectEquals(0, use.symbol.displacement, "displacement não é preenchido aqui");
        }
        return pass;
    }

    static Symbol declOf(Node site) {
        if (site instanceof Id) {
            return ((Id) site).decl;
        }
        if (site instanceof Assign) {
            return ((Assign) site).decl;
        }
        if (site instanceof Call) {
            return ((Call) site).decl;
        }
        throw new AssertionError("uso fora de Id, Assign e Call");
    }

    static IdentificationPass analyze(final Program program) {
        final IdentificationPass[] out = new IdentificationPass[1];
        final Throwable[] thrown = new Throwable[1];
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    out[0] = IdentificationPass.analyze(program);
                } catch (Throwable t) {
                    thrown[0] = t;
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
        try {
            thread.join(TIMEOUT_MS);
        } catch (InterruptedException e) {
            throw new AssertionError("interrompido");
        }
        if (thread.isAlive()) {
            throw new AssertionError("a passada não terminou");
        }
        if (thrown[0] != null) {
            throw new AssertionError("a passada lançou " + thrown[0], thrown[0]);
        }
        return out[0];
    }

    static Program parse(String source) {
        ParseResult result = new Parser(new Scanner(source)).parse();
        if (!result.accepted()) {
            throw new AssertionError("o parser rejeitou o programa: " + result.errors);
        }
        return result.tree;
    }

    static String read(String path) {
        try {
            return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError("não leu " + path + ": " + e.getMessage());
        }
    }

    static int lineOf(String error) {
        int colon = error.indexOf(':');
        return Integer.parseInt(error.substring("linha ".length(), colon));
    }

    static void expectSame(Object expected, Object actual, String what) {
        if (expected != actual) {
            throw new AssertionError(what + ": esperava " + expected + ", veio " + actual);
        }
    }

    static void expectNull(Object actual, String what) {
        if (actual != null) {
            throw new AssertionError(what + ": esperava null, veio " + actual);
        }
    }

    static void expectEquals(Object expected, Object actual, String what) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(what + ": esperava " + expected + ", veio " + actual);
        }
    }

    static void run(String name, Runnable test) {
        try {
            test.run();
            passed++;
            System.out.println("ok  " + name);
        } catch (Throwable e) {
            failed.add(name);
            System.out.println("FALHOU  " + name);
            System.out.println("    " + e.getMessage());
        }
    }
}
