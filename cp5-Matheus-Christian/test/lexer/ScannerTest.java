package lexer;

import java.util.ArrayList;
import java.util.List;

import static lexer.TokenType.*;

/**
 * Testes do scanner sem dependência externa: cada teste é um método,
 * as verificações lançam AssertionError e o main conta os resultados.
 */
public class ScannerTest {

    private static int passed = 0;
    private static final List<String> failed = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("== casos válidos ==");
        run("identificadores", ScannerTest::identificadores);
        run("identificador com o tamanho máximo", ScannerTest::identificadorTamanhoMaximo);
        run("palavras reservadas", ScannerTest::palavrasReservadas);
        run("case-sensitive", ScannerTest::caseSensitive);
        run("strings", ScannerTest::strings);
        run("literais de caractere", ScannerTest::literaisDeCaractere);
        run("literais numéricos", ScannerTest::literaisNumericos);
        run("operadores", ScannerTest::operadores);
        run("maximal munch", ScannerTest::maximalMunch);
        run("delimitadores", ScannerTest::delimitadores);
        run("comentários", ScannerTest::comentarios);
        run("comentário de bloco não aninha", ScannerTest::comentarioNaoAninha);
        run("linha e coluna", ScannerTest::linhaEColuna);
        run("EOF repetido", ScannerTest::eofRepetido);

        System.out.println();
        System.out.println("== casos de erro ==");
        run("string não fechada até o EOF", ScannerTest::stringNaoFechadaEof);
        run("string não fechada até o fim da linha", ScannerTest::stringNaoFechadaFimDeLinha);
        run("caractere fora do alfabeto", ScannerTest::caractereInvalido);
        run("vários caracteres inválidos seguidos", ScannerTest::variosInvalidos);
        run("comentário de bloco não fechado", ScannerTest::comentarioNaoFechado);
        run("número malformado", ScannerTest::numeroMalformado);
        run("& e | sozinhos", ScannerTest::ampEPipeSozinhos);
        run("escape inválido em string", ScannerTest::escapeInvalido);
        run("literais de caractere inválidos", ScannerTest::caractereLiteralInvalido);
        run("identificador começando com _", ScannerTest::identificadorComUnderline);
        run("identificador longo demais", ScannerTest::identificadorLongo);

        System.out.println();
        System.out.println("== código realista ==");
        run("linha com declarações, expressões e comentários", ScannerTest::linhaRealista);
        run("programa completo", ScannerTest::programaCompleto);

        System.out.println();
        System.out.printf("%d testes, %d passaram, %d falharam%n",
                passed + failed.size(), passed, failed.size());
        if (!failed.isEmpty()) {
            System.exit(1);
        }
    }

    // ---------------------------------------------------------------
    // casos válidos
    // ---------------------------------------------------------------

    static void identificadores() {
        Scanner s = new Scanner("total x1 contaItens conta_itens");
        List<Token> t = s.scanAll();
        expectTypes(t, IDENTIFIER, IDENTIFIER, IDENTIFIER, IDENTIFIER, EOF);
        expectToken(t.get(0), IDENTIFIER, "total", 1, 1);
        expectToken(t.get(1), IDENTIFIER, "x1", 1, 7);
        expectToken(t.get(2), IDENTIFIER, "contaItens", 1, 10);
        expectToken(t.get(3), IDENTIFIER, "conta_itens", 1, 21);
        expectNoErrors(s);
    }

    static void identificadorTamanhoMaximo() {
        String nome = repeat('a', Scanner.MAX_IDENT_LENGTH);
        Scanner s = new Scanner(nome);
        List<Token> t = s.scanAll();
        expectTypes(t, IDENTIFIER, EOF);
        expectToken(t.get(0), IDENTIFIER, nome, 1, 1);
        expectNoErrors(s);
    }

    static void palavrasReservadas() {
        Scanner s = new Scanner("int double bool char string void if else while return true false");
        expectTypes(s.scanAll(),
                INT, DOUBLE, BOOL, CHAR, STRING, VOID, IF, ELSE, WHILE, RETURN, TRUE, FALSE, EOF);
        expectNoErrors(s);
    }

    static void caseSensitive() {
        // só a forma minúscula é palavra reservada
        Scanner s = new Scanner("If WHILE While while");
        List<Token> t = s.scanAll();
        expectTypes(t, IDENTIFIER, IDENTIFIER, IDENTIFIER, WHILE, EOF);
        expectToken(t.get(0), IDENTIFIER, "If", 1, 1);
        expectNoErrors(s);
    }

    static void strings() {
        Scanner s = new Scanner("\"ok\" \"linha 1\" \"\" \"diz \\\"oi\\\"\\n\" \"ação\"");
        List<Token> t = s.scanAll();
        expectTypes(t, STRING_LITERAL, STRING_LITERAL, STRING_LITERAL, STRING_LITERAL, STRING_LITERAL, EOF);
        expectToken(t.get(0), STRING_LITERAL, "\"ok\"", 1, 1);
        expectToken(t.get(1), STRING_LITERAL, "\"linha 1\"", 1, 6);
        expectToken(t.get(2), STRING_LITERAL, "\"\"", 1, 16);
        expectToken(t.get(3), STRING_LITERAL, "\"diz \\\"oi\\\"\\n\"", 1, 19);
        expectToken(t.get(4), STRING_LITERAL, "\"ação\"", 1, 34);
        expectNoErrors(s);
    }

    static void literaisDeCaractere() {
        Scanner s = new Scanner("'a' '\\n' '\\'' ' '");
        List<Token> t = s.scanAll();
        expectTypes(t, CHAR_LITERAL, CHAR_LITERAL, CHAR_LITERAL, CHAR_LITERAL, EOF);
        expectToken(t.get(0), CHAR_LITERAL, "'a'", 1, 1);
        expectToken(t.get(1), CHAR_LITERAL, "'\\n'", 1, 5);
        expectToken(t.get(2), CHAR_LITERAL, "'\\''", 1, 10);
        expectToken(t.get(3), CHAR_LITERAL, "' '", 1, 15);
        expectNoErrors(s);
    }

    static void literaisNumericos() {
        Scanner s = new Scanner("0 10 3.14 007 2.50");
        List<Token> t = s.scanAll();
        expectTypes(t, INT_LITERAL, INT_LITERAL, DOUBLE_LITERAL, INT_LITERAL, DOUBLE_LITERAL, EOF);
        expectToken(t.get(0), INT_LITERAL, "0", 1, 1);
        expectToken(t.get(1), INT_LITERAL, "10", 1, 3);
        expectToken(t.get(2), DOUBLE_LITERAL, "3.14", 1, 6);
        expectToken(t.get(3), INT_LITERAL, "007", 1, 11);
        expectToken(t.get(4), DOUBLE_LITERAL, "2.50", 1, 15);
        expectNoErrors(s);
    }

    static void operadores() {
        Scanner s = new Scanner("+ - * / % = == != < <= > >= && || !");
        expectTypes(s.scanAll(),
                PLUS, MINUS, STAR, SLASH, PERCENT, ASSIGN, EQ, NEQ, LT, LE, GT, GE, AND, OR, NOT, EOF);
        expectNoErrors(s);
    }

    static void maximalMunch() {
        expectTypes(scanOk("a<=b"), IDENTIFIER, LE, IDENTIFIER, EOF);
        expectTypes(scanOk("x===y"), IDENTIFIER, EQ, ASSIGN, IDENTIFIER, EOF);
        expectTypes(scanOk("!=="), NEQ, ASSIGN, EOF);
        expectTypes(scanOk("a=-1"), IDENTIFIER, ASSIGN, MINUS, INT_LITERAL, EOF);
        expectTypes(scanOk("x>=y&&!z"), IDENTIFIER, GE, IDENTIFIER, AND, NOT, IDENTIFIER, EOF);
        expectTypes(scanOk("a/b"), IDENTIFIER, SLASH, IDENTIFIER, EOF);
        // palavra reservada só vale se for o lexema inteiro
        expectTypes(scanOk("ifx intx while1"), IDENTIFIER, IDENTIFIER, IDENTIFIER, EOF);
        // o número termina no primeiro caractere que não é dígito
        List<Token> t = scanOk("12abc");
        expectTypes(t, INT_LITERAL, IDENTIFIER, EOF);
        expectToken(t.get(1), IDENTIFIER, "abc", 1, 3);
    }

    static void delimitadores() {
        expectTypes(scanOk("( ) { } ; ,"), LPAREN, RPAREN, LBRACE, RBRACE, SEMICOLON, COMMA, EOF);
    }

    static void comentarios() {
        Scanner s = new Scanner("a // comentário\nb /* bloco\ncom * e / */ c");
        List<Token> t = s.scanAll();
        expectTypes(t, IDENTIFIER, IDENTIFIER, IDENTIFIER, EOF);
        expectToken(t.get(0), IDENTIFIER, "a", 1, 1);
        expectToken(t.get(1), IDENTIFIER, "b", 2, 1);
        expectToken(t.get(2), IDENTIFIER, "c", 3, 14);
        expectNoErrors(s);
    }

    static void comentarioNaoAninha() {
        // o primeiro */ fecha o comentário; o resto vira tokens comuns
        List<Token> t = scanOk("/* a /* b */ c */");
        expectTypes(t, IDENTIFIER, STAR, SLASH, EOF);
        expectToken(t.get(0), IDENTIFIER, "c", 1, 14);
    }

    static void linhaEColuna() {
        // tab conta como uma coluna
        List<Token> t = scanOk("int x;\n\tx = 10;\n\n  return x;");
        expectTypes(t, INT, IDENTIFIER, SEMICOLON,
                IDENTIFIER, ASSIGN, INT_LITERAL, SEMICOLON,
                RETURN, IDENTIFIER, SEMICOLON, EOF);
        expectToken(t.get(1), IDENTIFIER, "x", 1, 5);
        expectToken(t.get(3), IDENTIFIER, "x", 2, 2);
        expectToken(t.get(5), INT_LITERAL, "10", 2, 6);
        expectToken(t.get(7), RETURN, "return", 4, 3);
        expectToken(t.get(10), EOF, "", 4, 12);
    }

    static void eofRepetido() {
        Scanner s = new Scanner("x");
        expectEquals(IDENTIFIER, s.nextToken().type, "primeiro token");
        expectEquals(EOF, s.nextToken().type, "segundo token");
        expectEquals(EOF, s.nextToken().type, "depois do EOF");
    }

    // ---------------------------------------------------------------
    // casos de erro
    // ---------------------------------------------------------------

    static void stringNaoFechadaEof() {
        Scanner s = new Scanner("x = \"abc");
        List<Token> t = s.scanAll();
        expectTypes(t, IDENTIFIER, ASSIGN, EOF);
        expectErrorCount(s, 1);
        expectError(s, 0, 1, 5, "fim do arquivo");
    }

    static void stringNaoFechadaFimDeLinha() {
        // strings não podem ter quebra de linha: o erro fica na linha 1
        // e a linha 2 é lida normalmente
        Scanner s = new Scanner("s = \"abc\ny = 2;");
        List<Token> t = s.scanAll();
        expectTypes(t, IDENTIFIER, ASSIGN, IDENTIFIER, ASSIGN, INT_LITERAL, SEMICOLON, EOF);
        expectToken(t.get(2), IDENTIFIER, "y", 2, 1);
        expectToken(t.get(4), INT_LITERAL, "2", 2, 5);
        expectErrorCount(s, 1);
        expectError(s, 0, 1, 5, "fim da linha");
    }

    static void caractereInvalido() {
        Scanner s = new Scanner("a @ b # c");
        List<Token> t = s.scanAll();
        expectTypes(t, IDENTIFIER, IDENTIFIER, IDENTIFIER, EOF);
        expectToken(t.get(1), IDENTIFIER, "b", 1, 5);
        expectToken(t.get(2), IDENTIFIER, "c", 1, 9);
        expectErrorCount(s, 2);
        expectError(s, 0, 1, 3, "'@'");
        expectError(s, 1, 1, 7, "'#'");
    }

    static void variosInvalidos() {
        Scanner s = new Scanner("@@$ int x;");
        List<Token> t = s.scanAll();
        expectTypes(t, INT, IDENTIFIER, SEMICOLON, EOF);
        expectToken(t.get(0), INT, "int", 1, 5);
        expectErrorCount(s, 3);
        expectError(s, 0, 1, 1, "'@'");
        expectError(s, 1, 1, 2, "'@'");
        expectError(s, 2, 1, 3, "'$'");
    }

    static void comentarioNaoFechado() {
        Scanner s = new Scanner("x /* sem fim\nint y;");
        expectTypes(s.scanAll(), IDENTIFIER, EOF);
        expectErrorCount(s, 1);
        expectError(s, 0, 1, 3, "comentário de bloco não fechado");

        // terminando logo depois de um '*' (estado BLOCK_COMMENT_STAR)
        s = new Scanner("/* quase *");
        expectTypes(s.scanAll(), EOF);
        expectErrorCount(s, 1);
        expectError(s, 0, 1, 1, "comentário de bloco não fechado");
    }

    static void numeroMalformado() {
        Scanner s = new Scanner("3. + 1");
        List<Token> t = s.scanAll();
        expectTypes(t, PLUS, INT_LITERAL, EOF);
        expectToken(t.get(1), INT_LITERAL, "1", 1, 6);
        expectErrorCount(s, 1);
        expectError(s, 0, 1, 1, "'3.'");
    }

    static void ampEPipeSozinhos() {
        Scanner s = new Scanner("a & b | c");
        expectTypes(s.scanAll(), IDENTIFIER, IDENTIFIER, IDENTIFIER, EOF);
        expectErrorCount(s, 2);
        expectError(s, 0, 1, 3, "'&&'");
        expectError(s, 1, 1, 7, "'||'");
    }

    static void escapeInvalido() {
        // o erro é reportado na barra, mas a string continua sendo um token
        Scanner s = new Scanner("\"a\\qb\" x");
        List<Token> t = s.scanAll();
        expectTypes(t, STRING_LITERAL, IDENTIFIER, EOF);
        expectToken(t.get(0), STRING_LITERAL, "\"a\\qb\"", 1, 1);
        expectErrorCount(s, 1);
        expectError(s, 0, 1, 3, "'\\q'");
    }

    static void caractereLiteralInvalido() {
        Scanner s = new Scanner("'' 'ab' 'c");
        expectTypes(s.scanAll(), EOF);
        expectErrorCount(s, 3);
        expectError(s, 0, 1, 1, "vazio");
        expectError(s, 1, 1, 4, "mais de um caractere");
        expectError(s, 2, 1, 9, "não fechado");
    }

    static void identificadorComUnderline() {
        Scanner s = new Scanner("_x");
        List<Token> t = s.scanAll();
        expectTypes(t, IDENTIFIER, EOF);
        expectToken(t.get(0), IDENTIFIER, "x", 1, 2);
        expectErrorCount(s, 1);
        expectError(s, 0, 1, 1, "'_'");
    }

    static void identificadorLongo() {
        String nome = repeat('a', Scanner.MAX_IDENT_LENGTH + 1);
        Scanner s = new Scanner(nome + " = 1;");
        expectTypes(s.scanAll(), ASSIGN, INT_LITERAL, SEMICOLON, EOF);
        expectErrorCount(s, 1);
        expectError(s, 0, 1, 1, "mais de " + Scanner.MAX_IDENT_LENGTH);
    }

    // ---------------------------------------------------------------
    // código realista
    // ---------------------------------------------------------------

    static void linhaRealista() {
        String linha = "double media = (nota1 + nota2) / 2.0; /* média simples */"
                + " bool ok = media >= 6.0 && !reprovado; // fim";
        List<Token> t = scanOk(linha);
        expectTypes(t,
                DOUBLE, IDENTIFIER, ASSIGN, LPAREN, IDENTIFIER, PLUS, IDENTIFIER, RPAREN,
                SLASH, DOUBLE_LITERAL, SEMICOLON,
                BOOL, IDENTIFIER, ASSIGN, IDENTIFIER, GE, DOUBLE_LITERAL, AND, NOT, IDENTIFIER,
                SEMICOLON, EOF);
        expectToken(t.get(1), IDENTIFIER, "media", 1, 8);
        expectToken(t.get(9), DOUBLE_LITERAL, "2.0", 1, 34);
        expectToken(t.get(11), BOOL, "bool", 1, 59);
        expectToken(t.get(15), GE, ">=", 1, 75);
        expectToken(t.get(18), NOT, "!", 1, 85);
        expectToken(t.get(19), IDENTIFIER, "reprovado", 1, 86);
    }

    static void programaCompleto() {
        String programa =
                "// calcula o fatorial e confere o resultado\n"
              + "int fatorial(int n) {\n"
              + "    int r = 1;\n"
              + "    while (n > 1) {\n"
              + "        r = r * n;\n"
              + "        n = n - 1;\n"
              + "    }\n"
              + "    return r;\n"
              + "}\n"
              + "\n"
              + "void main() {\n"
              + "    /* testa com\n"
              + "       um valor fixo */\n"
              + "    int x = fatorial(5);\n"
              + "    char c = 'x';\n"
              + "    if (x != 120 || c == '\\n') {\n"
              + "        imprime(\"errado\");\n"
              + "    } else {\n"
              + "        imprime(\"ok: 5! = 120\");\n"
              + "    }\n"
              + "    return;\n"
              + "}\n";
        List<Token> t = scanOk(programa);

        expectEquals(18, count(t, IDENTIFIER), "quantidade de identificadores");
        expectEquals(4, count(t, INT), "quantidade de 'int'");
        expectEquals(2, count(t, RETURN), "quantidade de 'return'");
        expectEquals(1, count(t, WHILE), "quantidade de 'while'");

        expectToken(find(t, "fatorial"), IDENTIFIER, "fatorial", 2, 5);
        expectToken(find(t, "while"), WHILE, "while", 4, 5);
        expectToken(find(t, "'x'"), CHAR_LITERAL, "'x'", 15, 14);
        expectToken(find(t, "'\\n'"), CHAR_LITERAL, "'\\n'", 16, 26);
        expectToken(find(t, "\"ok: 5! = 120\""), STRING_LITERAL, "\"ok: 5! = 120\"", 19, 17);
        expectToken(t.get(t.size() - 2), RBRACE, "}", 22, 1);
        expectToken(t.get(t.size() - 1), EOF, "", 23, 1);
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

    /** Tokeniza e exige que não tenha nenhum erro léxico. */
    static List<Token> scanOk(String source) {
        Scanner s = new Scanner(source);
        List<Token> tokens = s.scanAll();
        expectNoErrors(s);
        return tokens;
    }

    static void expectTypes(List<Token> tokens, TokenType... expected) {
        List<TokenType> actual = new ArrayList<>();
        for (Token t : tokens) {
            actual.add(t.type);
        }
        List<TokenType> wanted = new ArrayList<>();
        for (TokenType type : expected) {
            wanted.add(type);
        }
        if (!actual.equals(wanted)) {
            throw new AssertionError("esperava " + wanted + ", veio " + actual);
        }
    }

    static void expectToken(Token t, TokenType type, String lexeme, int line, int column) {
        if (t.type != type || !t.lexeme.equals(lexeme) || t.line != line || t.column != column) {
            throw new AssertionError(String.format("esperava %d:%d %s '%s', veio %s",
                    line, column, type, lexeme, t));
        }
    }

    static void expectNoErrors(Scanner s) {
        if (!s.getErrors().isEmpty()) {
            throw new AssertionError("não esperava erros, veio " + s.getErrors());
        }
    }

    static void expectErrorCount(Scanner s, int n) {
        if (s.getErrors().size() != n) {
            throw new AssertionError("esperava " + n + " erro(s), veio " + s.getErrors());
        }
    }

    static void expectError(Scanner s, int index, int line, int column, String messagePart) {
        LexicalError e = s.getErrors().get(index);
        if (e.line != line || e.column != column || !e.message.contains(messagePart)) {
            throw new AssertionError(String.format("esperava erro em %d:%d contendo \"%s\", veio %s",
                    line, column, messagePart, e));
        }
    }

    static void expectEquals(Object expected, Object actual, String what) {
        if (!expected.equals(actual)) {
            throw new AssertionError(what + ": esperava " + expected + ", veio " + actual);
        }
    }

    static int count(List<Token> tokens, TokenType type) {
        int n = 0;
        for (Token t : tokens) {
            if (t.type == type) {
                n++;
            }
        }
        return n;
    }

    static Token find(List<Token> tokens, String lexeme) {
        for (Token t : tokens) {
            if (t.lexeme.equals(lexeme)) {
                return t;
            }
        }
        throw new AssertionError("nenhum token com lexema " + lexeme);
    }

    static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
