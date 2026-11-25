package types;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import ast.Assign;
import ast.Bin;
import ast.Block;
import ast.Call;
import ast.CallStmt;
import ast.Expr;
import ast.FuncDecl;
import ast.Id;
import ast.If;
import ast.Literal;
import ast.Param;
import ast.Program;
import ast.Return;
import ast.StmtNode;
import ast.TypeNode;
import ast.Unary;
import ast.Var;
import ast.VarDecl;
import ast.While;
import visitor.Visitor;

/**
 * Cada test/programas/X.txt passa por scanner, parser, identificação e
 * tipos, e o relatório é comparado com test/esperado/X.txt, escrito a partir
 * das regras de tipos-design.md. Nos programas aceitos, confere ainda que
 * toda expressão saiu com tipo gravado.
 */
public class TypeCheckTest {

    private static int passed = 0;
    private static final List<String> failed = new ArrayList<String>();
    private static final long TIMEOUT_MS = 5000;

    public static void main(String[] args) {
        System.out.println("== programas obrigatórios ==");
        program("válido com todos os tipos e comandos", "valido");
        program("erro de tipo em expressão", "expr_tipo");
        program("condição não booleana", "cond_int");
        program("return incompatível", "return_incompativel");
        program("chamada errada (aridade e tipo)", "chamada_errada");
        program("procedimento como expressão", "proc_expressao");
        program("vários erros, em ordem, sem cascata", "varios_erros");
        program("variável sem valor dá aviso", "aviso_sem_valor");

        System.out.println();
        System.out.println("== funções ==");
        program("recursiva", "recursiva");
        program("mutuamente recursiva (não permitida)", "d07_recursao_mutua");
        program("variável do nível acima", "nivel_acima");

        System.out.println();
        System.out.println("== um programa por decisão ==");
        program("1. int → double implícito", "d01_int_double");
        program("2. char com int", "d02_char_int");
        program("3. bool fora da aritmética", "d03_bool_aritmetica");
        program("4. string sem operadores", "d04_string");
        program("5. == entre tipos diferentes", "d05_igualdade");
        program("6. função como comando", "d06_funcao_comando");
        program("7. recursão mútua", "d07_recursao_mutua");
        program("8. argumento int → double", "d08_argumentos");
        program("9. return em todos os caminhos", "d09_return_caminhos");
        program("10. mesmo nome, outra assinatura", "d10_sobrecarga");
        program("11. função sem parênteses", "d11_funcao_sem_parenteses");

        System.out.println();
        System.out.println("== tipos gravados na árvore ==");
        run("valido.txt: toda expressão tem tipo", TypeCheckTest::everyExprTyped);
        run("valido.txt: c + 1 é int, d * -1 é double", TypeCheckTest::someTypes);

        System.out.println();
        System.out.println("== não quebra e não entra em laço ==");
        run("árvore nula", TypeCheckTest::arvoreNula);
        run("programa vazio", () -> expectEquals("programa aceito\n", analyze("").report(), "vazio"));
        run("erro sintático não chega aos tipos", TypeCheckTest::erroSintatico);
        run("fatorial do CP3: só imprime não declarado", () -> onlyImprime("exemplos/fatorial.txt", 2));
        run("primos do CP3: só imprime não declarado", () -> onlyImprime("exemplos/primos.txt", 4));
        run("programas do CP4 passam sem exceção", TypeCheckTest::programasCp4);

        System.out.println();
        System.out.printf("%d testes, %d passaram, %d falharam%n",
                passed + failed.size(), passed, failed.size());
        if (!failed.isEmpty()) {
            System.exit(1);
        }
    }

    static void program(String title, final String name) {
        run(title + " (" + name + ")", () -> {
            Analysis analysis = analyze(read("test/programas/" + name + ".txt"));
            String expected = read("test/esperado/" + name + ".txt");
            if (!expected.endsWith("\n")) {
                expected = expected + "\n";
            }
            if (!analysis.report().equals(expected)) {
                throw new AssertionError(name + "\n--- obtido ---\n" + analysis.report()
                        + "--- esperado ---\n" + expected);
            }
        });
    }

    static void everyExprTyped() {
        Analysis analysis = analyze(read("test/programas/valido.txt"));
        List<Expr> all = new ArrayList<Expr>();
        analysis.tree.accept(new ExprCollector(all));
        if (all.size() < 40) {
            throw new AssertionError("poucas expressões: " + all.size());
        }
        for (Expr expr : all) {
            if (expr.type == null || expr.type == Type.ERROR) {
                throw new AssertionError("expressão sem tipo na linha " + expr.line());
            }
        }
    }

    static void someTypes() {
        Analysis analysis = analyze(read("test/programas/valido.txt"));
        List<Expr> all = new ArrayList<Expr>();
        analysis.tree.accept(new ExprCollector(all));
        boolean line43 = false, line44 = false;
        for (Expr expr : all) {
            if (expr.line() == 43 && expr instanceof Bin) {
                expectSame(Type.DOUBLE, expr.type, "d * -1");
                line43 = true;
            }
            if (expr.line() == 44 && expr instanceof Bin) {
                expectSame(Type.INT, expr.type, "c + 1");
                line44 = true;
            }
        }
        if (!line43 || !line44) {
            throw new AssertionError("não achei as expressões das linhas 43 e 44");
        }
    }

    static void arvoreNula() {
        Analysis analysis = Analysis.of(null);
        if (analysis.accepted()) {
            throw new AssertionError("árvore nula não é aceita");
        }
    }

    static void erroSintatico() {
        Analysis analysis = analyze(read("exemplos/com-erros.txt"));
        if (analysis.syntaxErrors.isEmpty() || !analysis.diagnostics.isEmpty() || analysis.accepted()) {
            throw new AssertionError("com-erros.txt devia parar no parser");
        }
    }

    static void onlyImprime(String path, int count) {
        Analysis analysis = analyze(read(path));
        expectEquals(count, analysis.diagnostics.size(), "mensagens");
        for (Diagnostic d : analysis.diagnostics) {
            if (!d.text.endsWith("'imprime' não declarado")) {
                throw new AssertionError("mensagem inesperada: " + d.text);
            }
        }
    }

    static void programasCp4() {
        String[] names = {"sombreamento", "redeclaracao", "nao-declarado", "declara-antes",
                "ponto-declaracao", "mutua", "param-corpo", "espaco-nomes", "tres-erros",
                "recursivo", "depois-do-bloco", "funcao"};
        for (String name : names) {
            analyze(read("programs/" + name + ".txt"));
        }
    }

    /** Roda a análise numa thread com prazo: nenhum programa pode travar ou lançar. */
    static Analysis analyze(final String source) {
        final Analysis[] out = new Analysis[1];
        final Throwable[] thrown = new Throwable[1];
        Thread thread = new Thread(() -> {
            try {
                out[0] = Analysis.run(source);
            } catch (Throwable t) {
                thrown[0] = t;
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
            throw new AssertionError("a análise não terminou");
        }
        if (thrown[0] != null) {
            throw new AssertionError("a análise lançou " + thrown[0], thrown[0]);
        }
        return out[0];
    }

    static String read(String path) {
        try {
            return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError("não leu " + path + ": " + e.getMessage());
        }
    }

    static void expectSame(Object expected, Object actual, String what) {
        if (expected != actual) {
            throw new AssertionError(what + ": esperava " + expected + ", veio " + actual);
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

    /** Junta toda expressão da árvore, na ordem do texto. */
    static final class ExprCollector implements Visitor<Void> {
        private final List<Expr> out;

        ExprCollector(List<Expr> out) {
            this.out = out;
        }

        public Void visitProgram(Program n) { n.declarations.forEach(d -> d.accept(this)); return null; }
        public Void visitVarDecl(VarDecl n) { n.variables.forEach(v -> v.accept(this)); return null; }
        public Void visitVar(Var n) { if (n.initializer != null) n.initializer.accept(this); return null; }
        public Void visitFuncDecl(FuncDecl n) { n.body.accept(this); return null; }
        public Void visitParam(Param n) { return null; }
        public Void visitTypeNode(TypeNode n) { return null; }
        public Void visitBlock(Block n) { for (StmtNode s : n.statements) s.accept(this); return null; }
        public Void visitAssign(Assign n) { n.value.accept(this); return null; }
        public Void visitCallStmt(CallStmt n) { n.call.accept(this); return null; }
        public Void visitIf(If n) {
            n.condition.accept(this);
            n.thenBranch.accept(this);
            if (n.elseBranch != null) n.elseBranch.accept(this);
            return null;
        }
        public Void visitWhile(While n) { n.condition.accept(this); n.body.accept(this); return null; }
        public Void visitReturn(Return n) { if (n.value != null) n.value.accept(this); return null; }
        public Void visitBin(Bin n) { out.add(n); n.left.accept(this); n.right.accept(this); return null; }
        public Void visitUnary(Unary n) { out.add(n); n.operand.accept(this); return null; }
        public Void visitId(Id n) { out.add(n); return null; }
        public Void visitCall(Call n) { out.add(n); n.arguments.forEach(a -> a.accept(this)); return null; }
        public Void visitLiteral(Literal n) { out.add(n); return null; }
    }
}
