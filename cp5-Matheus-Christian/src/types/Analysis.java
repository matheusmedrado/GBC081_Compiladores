package types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ast.Program;
import lexer.Scanner;
import parser.ParseResult;
import parser.Parser;
import parser.SyntaxError;
import visitor.IdentificationPass;
import visitor.TypeChecker;

/**
 * As fases até a análise contextual: scanner, parser, identificação e
 * verificação de tipos. accepted() é a porta da geração de código: com erro
 * de qualquer fase, o programa não segue. Aviso não bloqueia.
 */
public final class Analysis {

    public final List<SyntaxError> syntaxErrors;
    public final Program tree;
    /** Erros de identificação e de tipo e avisos, ordenados pela linha. */
    public final List<Diagnostic> diagnostics;

    private Analysis(List<SyntaxError> syntaxErrors, Program tree, List<Diagnostic> diagnostics) {
        this.syntaxErrors = syntaxErrors;
        this.tree = tree;
        this.diagnostics = Collections.unmodifiableList(diagnostics);
    }

    public static Analysis run(String source) {
        ParseResult parsed = new Parser(new Scanner(source)).parse();
        if (!parsed.accepted()) {
            return new Analysis(parsed.errors, null, new ArrayList<Diagnostic>());
        }
        return of(parsed.tree);
    }

    /** Identificação e tipos sobre uma árvore já montada. */
    public static Analysis of(Program tree) {
        List<Diagnostic> all = new ArrayList<Diagnostic>();
        IdentificationPass identification = IdentificationPass.analyze(tree);
        for (String error : identification.errors()) {
            all.add(new Diagnostic(lineOf(error), error, false));
        }
        all.addAll(TypeChecker.check(tree).diagnostics());
        // Ordenação estável: na mesma linha, identificação antes de tipos, e
        // as mensagens de tipo na ordem do percurso (esquerda para a direita).
        Collections.sort(all, new Comparator<Diagnostic>() {
            @Override
            public int compare(Diagnostic a, Diagnostic b) {
                return Integer.compare(a.line, b.line);
            }
        });
        return new Analysis(new ArrayList<SyntaxError>(), tree, all);
    }

    public boolean accepted() {
        if (tree == null || !syntaxErrors.isEmpty()) {
            return false;
        }
        for (Diagnostic d : diagnostics) {
            if (!d.warning) {
                return false;
            }
        }
        return true;
    }

    /** Uma mensagem por linha e, no fim, "programa aceito" ou "programa rejeitado". */
    public String report() {
        StringBuilder out = new StringBuilder();
        for (SyntaxError error : syntaxErrors) {
            out.append(error).append('\n');
        }
        for (Diagnostic d : diagnostics) {
            out.append(d.text).append('\n');
        }
        out.append(accepted() ? "programa aceito" : "programa rejeitado").append('\n');
        return out.toString();
    }

    /** "linha 12: ..." → 12. As mensagens da identificação sempre começam assim. */
    private static int lineOf(String message) {
        try {
            int colon = message.indexOf(':');
            return Integer.parseInt(message.substring("linha ".length(), colon));
        } catch (RuntimeException e) {
            return 0;
        }
    }
}
