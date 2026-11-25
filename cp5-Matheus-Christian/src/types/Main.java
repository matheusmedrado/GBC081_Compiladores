package types;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Roda scanner, parser, identificação e verificação de tipos.
 *
 * Uso: java types.Main arquivo
 * Imprime as mensagens e "programa aceito" ou "programa rejeitado". Sai com
 * 0 quando o programa é aceito (avisos não contam) e 1 quando não é: é
 * esse código que impede a geração de código de rodar.
 */
public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("uso: java types.Main arquivo");
            System.exit(2);
        }
        String text = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);
        Analysis analysis = Analysis.run(text);
        System.out.print(analysis.report());
        if (!analysis.accepted()) {
            System.exit(1);
        }
    }
}
