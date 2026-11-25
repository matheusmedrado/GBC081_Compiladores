package symbols;

import java.util.List;

import ast.Node;
import types.Type;

/**
 * Uma declaração. A passada de identificação preenche nome, categoria, nó,
 * nível e o tipo declarado: para variável e parâmetro, type é o tipo dela;
 * para procedimento e função, type é o tipo de retorno (VOID no
 * procedimento) e parameters são os tipos dos parâmetros, na ordem. O
 * verificador de tipos só lê esses campos. displacement fica para a geração
 * de código.
 */
public class Symbol {

    public final String name;
    public final Category category;
    public final Node declaration;
    public final int level;

    /** Tipo declarado (ou de retorno). Null até a identificação preencher. */
    public Type type;

    /** Tipos dos parâmetros de procedimento e função. Null nas outras categorias. */
    public List<Type> parameters;

    /** Deslocamento na geração de código. Zero até lá. */
    public int displacement;

    public Symbol(String name, Category category, Node declaration, int level) {
        this.name = name;
        this.category = category;
        this.declaration = declaration;
        this.level = level;
    }

    /** Procedimento ou função: pode ser chamado. */
    public boolean isCallable() {
        return category == Category.PROCEDURE || category == Category.FUNCTION;
    }

    @Override
    public String toString() {
        int line = declaration == null ? 0 : declaration.line();
        return name + "@" + line + " " + category.label() + " nível " + level;
    }
}
