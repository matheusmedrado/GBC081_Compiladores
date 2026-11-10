package symbols;

import ast.Node;

/**
 * Uma declaração. A passada de identificação preenche nome, categoria, nó e
 * nível. type e displacement ficam reservados: o Checkpoint 5 lê i.decl.type,
 * e o deslocamento entra na geração de código. Os dois podem ser preenchidos
 * sem mudar os campos desta classe.
 */
public class Symbol {

    public final String name;
    public final Category category;
    public final Node declaration;
    public final int level;

    /** Análise de tipos. Null até lá. */
    public Object type;

    /** Deslocamento na geração de código. Zero até lá. */
    public int displacement;

    public Symbol(String name, Category category, Node declaration, int level) {
        this.name = name;
        this.category = category;
        this.declaration = declaration;
        this.level = level;
    }

    @Override
    public String toString() {
        int line = declaration == null ? 0 : declaration.line();
        return name + "@" + line + " " + category.label() + " nível " + level;
    }
}
