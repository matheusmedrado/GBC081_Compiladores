package parser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import lexer.LexicalError;
import lexer.Scanner;

/**
 * Roda scanner e parser sobre um arquivo. Imprime os erros léxicos e
 * sintáticos em ordem de posição e diz se o programa foi aceito.
 * Com --trace, imprime antes o traço da recuperação de erros.
 */
public class Main {

    public static void main(String[] args) throws IOException {
        String path = null;
        boolean trace = false;
        for (String arg : args) {
            if (arg.equals("--trace")) {
                trace = true;
            } else if (path == null) {
                path = arg;
            } else {
                path = null;
                break;
            }
        }
        if (path == null) {
            System.err.println("uso: java -cp out parser.Main [--trace] <arquivo-fonte>");
            System.exit(2);
        }

        String source = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        if (source.startsWith("\uFEFF")) {
            source = source.substring(1);   // BOM de editores no Windows
        }

        Scanner scanner = new Scanner(source);
        Parser parser = new Parser(scanner);
        if (trace) {
            System.out.println("== traço ==");
            parser.setTrace(System.out);
        }
        boolean accepted = parser.parse();

        List<LexicalError> lexical = scanner.getErrors();
        List<SyntaxError> syntax = parser.getErrors();
        if (trace) {
            System.out.println();
            System.out.println("== erros ==");
        }
        for (String line : sortedByPosition(lexical, syntax)) {
            System.out.println(line);
        }

        if (!lexical.isEmpty() || !syntax.isEmpty()) {
            System.out.println();
        }
        if (accepted) {
            System.out.println("programa aceito");
        } else {
            System.out.println("programa rejeitado: " + lexical.size() + " erro(s) léxico(s), "
                    + syntax.size() + " erro(s) sintático(s)");
        }
        System.exit(accepted ? 0 : 1);
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
}
