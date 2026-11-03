package parser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ast.Program;
import lexer.LexicalError;
import lexer.Scanner;
import visitor.SourcePrinter;
import visitor.TreePrinter;

/**
 * Roda o scanner e o parser sobre um arquivo e imprime a AST.
 *
 * Uso: java parser.Main [--trace] [--source] arquivo
 *   --trace   traço da recuperação de erros
 *   --source  reimprime o programa a partir da árvore, em vez da árvore
 */
public class Main {

    public static void main(String[] args) throws IOException {
        boolean trace = false;
        boolean source = false;
        String file = null;
        for (String arg : args) {
            if ("--trace".equals(arg)) {
                trace = true;
            } else if ("--source".equals(arg)) {
                source = true;
            } else if (file == null) {
                file = arg;
            } else {
                usage();
            }
        }
        if (file == null) {
            usage();
        }

        String text = new String(Files.readAllBytes(Paths.get(file)), StandardCharsets.UTF_8);
        Scanner scanner = new Scanner(text);
        Parser parser = new Parser(scanner);
        if (trace) {
            System.out.println("== traço ==");
            parser.setTrace(System.out);
        }
        ParseResult result = parser.parse();

        if (trace) {
            System.out.println();
            System.out.println("== erros ==");
        }
        List<String> messages = sortedByPosition(scanner.getErrors(), result.errors);
        for (String message : messages) {
            System.out.println(message);
        }
        if (!messages.isEmpty()) {
            System.out.println();
        }

        if (result.accepted()) {
            Program tree = result.tree;
            System.out.print(source ? SourcePrinter.print(tree) : TreePrinter.print(tree));
            System.out.println("programa aceito");
        } else {
            System.out.printf("programa rejeitado: %d erro(s) léxico(s), %d erro(s) sintático(s)%n",
                    scanner.getErrors().size(), result.errors.size());
            System.exit(1);
        }
    }

    /** Junta as duas listas pela posição; no empate, o erro léxico vem antes. */
    private static List<String> sortedByPosition(List<LexicalError> lexical, List<SyntaxError> syntax) {
        List<int[]> keys = new ArrayList<>();      // linha, coluna, origem, índice
        for (int i = 0; i < lexical.size(); i++) {
            keys.add(new int[] {lexical.get(i).line, lexical.get(i).column, 0, i});
        }
        for (int i = 0; i < syntax.size(); i++) {
            keys.add(new int[] {syntax.get(i).line, syntax.get(i).column, 1, i});
        }
        Collections.sort(keys, new Comparator<int[]>() {
            @Override
            public int compare(int[] a, int[] b) {
                for (int k = 0; k < 4; k++) {
                    if (a[k] != b[k]) {
                        return Integer.compare(a[k], b[k]);
                    }
                }
                return 0;
            }
        });
        List<String> lines = new ArrayList<>();
        for (int[] key : keys) {
            lines.add(key[2] == 0 ? lexical.get(key[3]).toString() : syntax.get(key[3]).toString());
        }
        return lines;
    }

    private static void usage() {
        System.err.println("uso: java parser.Main [--trace] [--source] arquivo");
        System.exit(2);
    }
}
