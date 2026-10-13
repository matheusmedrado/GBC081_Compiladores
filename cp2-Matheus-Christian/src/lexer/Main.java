package lexer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/** Roda o scanner sobre um arquivo e imprime os tokens e erros na ordem em que aparecem. */
public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("uso: java -cp out lexer.Main <arquivo-fonte>");
            System.exit(2);
        }

        String source = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);
        if (source.startsWith("﻿")) {
            source = source.substring(1);   // BOM de editores no Windows
        }

        Scanner scanner = new Scanner(source);
        List<LexicalError> errors = scanner.getErrors();
        int printedErrors = 0;
        int tokenCount = 0;

        Token token;
        do {
            token = scanner.nextToken();
            // erros encontrados enquanto o scanner procurava este token
            while (printedErrors < errors.size()) {
                System.out.println(errors.get(printedErrors++));
            }
            System.out.printf("%4d:%-4d %-15s %s%n", token.line, token.column, token.type, token.lexeme);
            tokenCount++;
        } while (token.type != TokenType.EOF);

        System.out.println();
        System.out.println(tokenCount + " tokens, " + errors.size() + " erro(s) léxico(s)");
        System.exit(errors.isEmpty() ? 0 : 1);
    }
}
