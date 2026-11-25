package symbols;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import lexer.Scanner;
import parser.ParseResult;
import parser.Parser;
import parser.SyntaxError;
import visitor.IdentificationPass;

/**
 * Roda o scanner, o parser e a passada de identificação.
 *
 * Uso: java symbols.Main arquivo
 * Imprime o relatório (erros e usos). Sai com 0 se não houve erro de
 * identificação, 1 se houve erro sintático ou de identificação.
 */
public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("uso: java symbols.Main arquivo");
            System.exit(2);
        }
        String text = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);
        ParseResult result = new Parser(new Scanner(text)).parse();
        if (!result.accepted()) {
            for (SyntaxError error : result.errors) {
                System.out.println(error);
            }
            System.out.println("programa rejeitado");
            System.exit(1);
        }
        IdentificationPass pass = IdentificationPass.analyze(result.tree);
        System.out.print(pass.report());
        if (!pass.errors().isEmpty()) {
            System.exit(1);
        }
    }
}
