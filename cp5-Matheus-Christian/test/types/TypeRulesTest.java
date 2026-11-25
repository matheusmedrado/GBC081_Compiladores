package types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ast.Bin;
import ast.Expr;
import ast.Literal;
import ast.Unary;
import visitor.TypeChecker;

/**
 * As regras isoladas, sem parser: cada linha da tabela de operadores de
 * tipos-design.md, aceita e recusada. A primeira metade chama TypeRules
 * direto; a segunda monta a expressão à mão (Bin e Unary sobre Literal) e
 * passa pelo TypeChecker, para conferir o tipo gravado no nó e a mensagem.
 */
public class TypeRulesTest {

    private static int passed = 0;
    private static final List<String> failed = new ArrayList<String>();

    static final Type I = Type.INT, D = Type.DOUBLE, C = Type.CHAR, B = Type.BOOL,
            S = Type.STRING, E = Type.ERROR;

    public static void main(String[] args) {
        System.out.println("== tabela de operadores: aceitos ==");
        for (String op : Arrays.asList("+", "-", "*", "/")) {
            row(op, I, I, I);
            row(op, I, D, D);
            row(op, D, I, D);
            row(op, D, D, D);
            row(op, C, I, I);
            row(op, C, C, I);
            row(op, C, D, D);
        }
        row("%", I, I, I);
        row("%", C, I, I);
        row("%", C, C, I);
        for (String op : Arrays.asList("<", "<=", ">", ">=")) {
            row(op, I, I, B);
            row(op, I, D, B);
            row(op, C, C, B);
            row(op, C, D, B);
        }
        for (String op : Arrays.asList("==", "!=")) {
            row(op, I, I, B);
            row(op, I, D, B);
            row(op, C, I, B);
            row(op, D, D, B);
            row(op, B, B, B);
        }
        for (String op : Arrays.asList("&&", "||")) {
            row(op, B, B, B);
        }
        unaryRow("-", I, I);
        unaryRow("-", D, D);
        unaryRow("-", C, I);
        unaryRow("!", B, B);

        System.out.println();
        System.out.println("== tabela de operadores: recusados ==");
        for (String op : Arrays.asList("+", "-", "*", "/")) {
            row(op, B, I, null);
            row(op, I, B, null);
            row(op, S, S, null);
            row(op, S, I, null);
        }
        row("%", D, I, null);
        row("%", I, D, null);
        row("%", B, B, null);
        for (String op : Arrays.asList("<", "<=", ">", ">=")) {
            row(op, B, B, null);
            row(op, S, S, null);
            row(op, I, B, null);
        }
        for (String op : Arrays.asList("==", "!=")) {
            row(op, S, S, null);
            row(op, I, B, null);
            row(op, B, D, null);
            row(op, C, S, null);
        }
        for (String op : Arrays.asList("&&", "||")) {
            row(op, I, I, null);
            row(op, B, I, null);
            row(op, S, B, null);
        }
        unaryRow("-", B, null);
        unaryRow("-", S, null);
        unaryRow("!", I, null);
        unaryRow("!", S, null);

        System.out.println();
        System.out.println("== erro não gera cascata ==");
        for (String op : Arrays.asList("+", "%", "<", "==", "&&")) {
            row(op, E, I, E);
            row(op, S, E, E);
        }
        unaryRow("-", E, E);
        unaryRow("!", E, E);
        run("ERROR é compatível com tudo", TypeRulesTest::errorCompat);
        run("condição", TypeRulesTest::condition);

        System.out.println();
        System.out.println("== compatibilidade (atribuição, argumento, return) ==");
        compat(I, I, true);
        compat(D, I, true);
        compat(D, C, true);
        compat(I, C, true);
        compat(I, D, false);
        compat(C, I, false);
        compat(C, D, false);
        compat(B, I, false);
        compat(I, B, false);
        compat(S, C, false);
        compat(C, S, false);
        compat(S, S, true);
        compat(B, B, true);

        System.out.println();
        System.out.println("== pelo verificador, sem parser ==");
        run("1 + 2.5 grava double no nó", TypeRulesTest::nodeArith);
        run("'a' + 1 grava int no nó", TypeRulesTest::nodeChar);
        run("true + 1 dá uma mensagem e ERROR", TypeRulesTest::nodeRejected);
        run("(true + 1) * 2 dá uma mensagem só", TypeRulesTest::nodeNoCascade);
        run("!1 dá mensagem", TypeRulesTest::nodeUnary);
        run("nome não declarado é ERROR sem mensagem", TypeRulesTest::nodeUndeclared);

        System.out.println();
        System.out.printf("%d testes, %d passaram, %d falharam%n",
                passed + failed.size(), passed, failed.size());
        if (!failed.isEmpty()) {
            System.exit(1);
        }
    }

    static void row(final String op, final Type left, final Type right, final Type expected) {
        String name = left + " " + op + " " + right + " : " + (expected == null ? "recusado" : expected.toString());
        run(name, () -> expectSame(expected, TypeRules.binary(op, left, right), name));
    }

    static void unaryRow(final String op, final Type operand, final Type expected) {
        String name = op + operand + " : " + (expected == null ? "recusado" : expected.toString());
        run(name, () -> expectSame(expected, TypeRules.unary(op, operand), name));
    }

    static void compat(final Type target, final Type source, final boolean expected) {
        String name = source + " → " + target + (expected ? " aceito" : " recusado");
        run(name, () -> {
            if (TypeRules.assignable(target, source) != expected) {
                throw new AssertionError(name);
            }
        });
    }

    static void errorCompat() {
        for (Type t : Arrays.asList(I, D, C, B, S)) {
            if (!TypeRules.assignable(t, E) || !TypeRules.assignable(E, t)) {
                throw new AssertionError("ERROR e " + t);
            }
        }
    }

    static void condition() {
        if (!TypeRules.isCondition(B) || !TypeRules.isCondition(E)) {
            throw new AssertionError("bool e ERROR são condição");
        }
        for (Type t : Arrays.asList(I, D, C, S)) {
            if (TypeRules.isCondition(t)) {
                throw new AssertionError(t + " não é condição");
            }
        }
    }

    static Literal lit(String kind, String value) {
        return new Literal(1, 1, kind, value);
    }

    static TypeChecker typeOf(Expr expr) {
        TypeChecker checker = new TypeChecker();
        expr.accept(checker);
        return checker;
    }

    static void nodeArith() {
        Bin bin = new Bin(1, 3, "+", lit("int", "1"), lit("double", "2.5"));
        TypeChecker checker = typeOf(bin);
        expectSame(D, bin.type, "tipo do Bin");
        expectSame(I, bin.left.type, "tipo do literal 1");
        expectSame(D, bin.right.type, "tipo do literal 2.5");
        expectEquals(0, checker.diagnostics().size(), "nenhuma mensagem");
    }

    static void nodeChar() {
        Bin bin = new Bin(1, 5, "+", lit("char", "'a'"), lit("int", "1"));
        typeOf(bin);
        expectSame(I, bin.type, "char + int");
    }

    static void nodeRejected() {
        Bin bin = new Bin(4, 3, "+", lit("bool", "true"), lit("int", "1"));
        TypeChecker checker = typeOf(bin);
        expectSame(E, bin.type, "tipo de erro");
        expectEquals(1, checker.diagnostics().size(), "uma mensagem");
        expectEquals("linha 4: operador '+' não se aplica a bool e int",
                checker.diagnostics().get(0).text, "mensagem");
    }

    static void nodeNoCascade() {
        Bin inner = new Bin(2, 3, "+", lit("bool", "true"), lit("int", "1"));
        Bin outer = new Bin(2, 8, "*", inner, lit("int", "2"));
        TypeChecker checker = typeOf(outer);
        expectSame(E, outer.type, "ERROR sobe");
        expectEquals(1, checker.diagnostics().size(), "só a mensagem de dentro");
    }

    static void nodeUnary() {
        Unary unary = new Unary(3, 1, "!", lit("int", "1"));
        TypeChecker checker = typeOf(unary);
        expectSame(E, unary.type, "tipo de erro");
        expectEquals("linha 3: operador '!' não se aplica a int",
                checker.diagnostics().get(0).text, "mensagem");
    }

    static void nodeUndeclared() {
        ast.Id id = new ast.Id(1, 1, "x");   // decl null: a identificação não achou
        Bin bin = new Bin(1, 3, "+", id, lit("int", "1"));
        TypeChecker checker = typeOf(bin);
        expectSame(E, id.type, "Id sem declaração");
        expectSame(E, bin.type, "ERROR sobe");
        expectEquals(0, checker.diagnostics().size(), "sem mensagem nova");
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
}
