package parser;

import java.util.List;

import ast.Program;

/**
 * Resultado de uma análise. Quando há erro sintático ou léxico, tree é null:
 * uma árvore montada no meio de uma recuperação não pode ser apresentada como
 * válida às fases seguintes.
 */
public class ParseResult {

    public final Program tree;
    public final List<SyntaxError> errors;

    public ParseResult(Program tree, List<SyntaxError> errors) {
        this.tree = tree;
        this.errors = errors;
    }

    public boolean accepted() {
        return tree != null && errors.isEmpty();
    }
}
