package symbols;

import java.util.ArrayList;
import java.util.List;

/**
 * A tabela sozinha, sem parser e sem árvore. Cada teste é um método;
 * as verificações lançam AssertionError e o main conta os resultados.
 */
public class SymbolTableTest {

    private static int passed = 0;
    private static final List<String> failed = new ArrayList<String>();

    public static void main(String[] args) {
        run("declarar e buscar", SymbolTableTest::declararEBuscar);
        run("sombreamento e restauração no closeScope", SymbolTableTest::sombreamentoERestauracao);
        run("redeclaração devolve false e mantém o primeiro", SymbolTableTest::redeclaracaoMantemOPrimeiro);
        run("lookupLocal não vê o escopo de fora", SymbolTableTest::lookupLocalNaoVeODeFora);
        run("nome inexistente devolve null", SymbolTableTest::inexistenteDevolveNull);
        run("level", SymbolTableTest::nivel);
        run("closeScope sem escopo aberto não lança", SymbolTableTest::closeSemEscopo);
        run("declare sem escopo aberto não é NullPointerException", SymbolTableTest::declareSemEscopo);
        run("type e displacement começam vazios e podem ser preenchidos", SymbolTableTest::camposReservados);

        System.out.printf("%d testes, %d passaram, %d falharam%n",
                passed + failed.size(), passed, failed.size());
        if (!failed.isEmpty()) {
            System.exit(1);
        }
    }

    static void declararEBuscar() {
        SymbolTable table = new StackedSymbolTable();
        table.openScope();
        Symbol a = symbol("a", 0);
        Symbol b = symbol("b", 0);
        expect(table.declare(a), "declare a");
        expect(table.declare(b), "declare b");
        expectSame(a, table.lookup("a"), "lookup a");
        expectSame(b, table.lookup("b"), "lookup b");
        expectSame(a, table.lookupLocal("a"), "lookupLocal a");
    }

    static void sombreamentoERestauracao() {
        SymbolTable table = new StackedSymbolTable();
        table.openScope();
        Symbol outer = symbol("a", 0);
        expect(table.declare(outer), "declare a externo");

        table.openScope();
        Symbol inner = symbol("a", 1);
        expect(table.declare(inner), "declare a interno");
        expectSame(inner, table.lookup("a"), "lookup no interno");
        expectSame(inner, table.lookupLocal("a"), "lookupLocal no interno");

        Symbol onlyInner = symbol("z", 1);
        expect(table.declare(onlyInner), "declare z");
        expectSame(onlyInner, table.lookup("z"), "z visível dentro");

        table.closeScope();
        expectSame(outer, table.lookup("a"), "a externo restaurado");
        expectSame(outer, table.lookupLocal("a"), "lookupLocal depois do close");
        expectNull(table.lookup("z"), "z morre com o escopo");
    }

    static void redeclaracaoMantemOPrimeiro() {
        SymbolTable table = new StackedSymbolTable();
        table.openScope();
        Symbol first = symbol("a", 0);
        Symbol second = symbol("a", 0);
        expect(table.declare(first), "primeira declaração");
        expect(!table.declare(second), "segunda devolve false");
        expectSame(first, table.lookup("a"), "o primeiro permanece");
        expectSame(first, table.lookupLocal("a"), "lookupLocal também");

        table.openScope();
        Symbol inner = symbol("a", 1);
        expect(table.declare(inner), "o interno pode sombrear");
        table.closeScope();
        expectSame(first, table.lookup("a"), "o externo segue o primeiro");
    }

    static void lookupLocalNaoVeODeFora() {
        SymbolTable table = new StackedSymbolTable();
        table.openScope();
        Symbol outer = symbol("a", 0);
        table.declare(outer);
        table.openScope();
        expectNull(table.lookupLocal("a"), "lookupLocal no escopo vazio");
        expectSame(outer, table.lookup("a"), "lookup atravessa");

        table.openScope();
        expectNull(table.lookupLocal("a"), "dois níveis acima");
        expectSame(outer, table.lookup("a"), "lookup acha o de baixo");
        Symbol mid = symbol("b", 1);
        table.closeScope();
        table.declare(mid);
        table.openScope();
        expectNull(table.lookupLocal("b"), "b está no meio, não aqui");
        expectSame(mid, table.lookup("b"), "lookup acha o do meio");
    }

    static void inexistenteDevolveNull() {
        SymbolTable table = new StackedSymbolTable();
        expectNull(table.lookup("a"), "lookup sem escopo");
        expectNull(table.lookupLocal("a"), "lookupLocal sem escopo");
        table.openScope();
        expectNull(table.lookup("ausente"), "lookup ausente");
        expectNull(table.lookupLocal("ausente"), "lookupLocal ausente");
        table.declare(symbol("a", 0));
        expectNull(table.lookup("b"), "outro nome");
    }

    static void nivel() {
        SymbolTable table = new StackedSymbolTable();
        expectEquals(-1, table.level(), "sem escopo");
        table.openScope();
        expectEquals(0, table.level(), "global");
        table.openScope();
        expectEquals(1, table.level(), "um bloco");
        table.openScope();
        expectEquals(2, table.level(), "dois blocos");
        table.closeScope();
        expectEquals(1, table.level(), "fechou o de dentro");
        table.closeScope();
        expectEquals(0, table.level(), "voltou ao global");
        table.closeScope();
        expectEquals(-1, table.level(), "fechou o global");
    }

    static void closeSemEscopo() {
        SymbolTable table = new StackedSymbolTable();
        table.closeScope();
        table.closeScope();
        expectEquals(-1, table.level(), "continua sem escopo");
        table.openScope();
        table.closeScope();
        table.closeScope();
        expectEquals(-1, table.level(), "close a mais não fura");
        expectNull(table.lookup("a"), "lookup depois do close a mais");
    }

    static void declareSemEscopo() {
        SymbolTable table = new StackedSymbolTable();
        try {
            table.declare(symbol("a", -1));
            throw new AssertionError("declare sem escopo deveria falhar");
        } catch (IllegalStateException expected) {
            if (expected.getMessage() == null || expected.getMessage().isEmpty()) {
                throw new AssertionError("a falha precisa explicar o que houve");
            }
        } catch (NullPointerException e) {
            throw new AssertionError("declare sem escopo lançou NullPointerException");
        }
        expectNull(table.lookup("a"), "nada foi inserido");
    }

    static void camposReservados() {
        Symbol symbol = symbol("a", 0);
        expectNull(symbol.type, "type começa null");
        expectEquals(0, symbol.displacement, "displacement começa 0");
        Object type = new Object();
        symbol.type = type;
        symbol.displacement = 8;
        expectSame(type, symbol.type, "type preenchido depois");
        expectEquals(8, symbol.displacement, "displacement preenchido depois");
        expectEquals("a", symbol.name, "o resto do símbolo não muda");
        expectEquals(Category.VARIABLE, symbol.category, "categoria");
    }

    private static Symbol symbol(String name, int level) {
        return new Symbol(name, Category.VARIABLE, null, level);
    }

    private static void expect(boolean condition, String what) {
        if (!condition) {
            throw new AssertionError(what);
        }
    }

    private static void expectSame(Object expected, Object actual, String what) {
        if (expected != actual) {
            throw new AssertionError(what + ": esperava o mesmo objeto, veio " + actual);
        }
    }

    private static void expectNull(Object actual, String what) {
        if (actual != null) {
            throw new AssertionError(what + ": esperava null, veio " + actual);
        }
    }

    private static void expectEquals(int expected, int actual, String what) {
        if (expected != actual) {
            throw new AssertionError(what + ": esperava " + expected + ", veio " + actual);
        }
    }

    private static void expectEquals(Object expected, Object actual, String what) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(what + ": esperava " + expected + ", veio " + actual);
        }
    }

    private static void run(String name, Runnable test) {
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
