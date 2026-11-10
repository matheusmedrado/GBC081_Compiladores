package visitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ast.Assign;
import ast.Bin;
import ast.Block;
import ast.Call;
import ast.CallStmt;
import ast.Decl;
import ast.Expr;
import ast.FuncDecl;
import ast.Id;
import ast.If;
import ast.Literal;
import ast.Node;
import ast.Param;
import ast.Program;
import ast.Return;
import ast.StmtNode;
import ast.TypeNode;
import ast.Unary;
import ast.Var;
import ast.VarDecl;
import ast.While;
import symbols.Category;
import symbols.StackedSymbolTable;
import symbols.Symbol;
import symbols.SymbolTable;

/**
 * Passada de identificação: abre e fecha escopos, declara os nomes e grava
 * em cada uso o símbolo da declaração. Não confere tipos. Não usa instanceof
 * nem switch sobre o tipo do nó.
 *
 * A única construção de tabela concreta é analyze. O resto desta classe só
 * enxerga SymbolTable.
 */
public class IdentificationPass implements Visitor<Void> {

    /** Um uso, na ordem do texto. symbol é null quando o nome não foi declarado. */
    public static final class Use {
        public final Node site;
        public final int line;
        public final String name;
        public final Symbol symbol;

        Use(Node site, int line, String name, Symbol symbol) {
            this.site = site;
            this.line = line;
            this.name = name;
            this.symbol = symbol;
        }
    }

    private final SymbolTable table;
    private final List<String> errors = new ArrayList<String>();
    private final List<Use> uses = new ArrayList<Use>();

    public IdentificationPass(SymbolTable table) {
        this.table = table;
    }

    /** Cria a pilha de mapas, roda a passada e descarta a tabela. */
    public static IdentificationPass analyze(Program program) {
        SymbolTable table = new StackedSymbolTable();
        IdentificationPass pass = new IdentificationPass(table);
        if (program != null) {
            program.accept(pass);
        }
        return pass;
    }

    public List<String> errors() {
        return Collections.unmodifiableList(errors);
    }

    public List<Use> uses() {
        return Collections.unmodifiableList(uses);
    }

    /**
     * Relatório determinístico. erros na ordem em que foram achados (a ordem
     * do texto, porque a passada é uma só, da esquerda para a direita) e
     * usos na mesma ordem. Nome não declarado termina em "?".
     */
    public String report() {
        StringBuilder out = new StringBuilder();
        out.append("erros:\n");
        for (String error : errors) {
            out.append(error).append('\n');
        }
        out.append("usos:\n");
        for (Use use : uses) {
            out.append("linha ").append(use.line).append(": ");
            out.append(use.name).append(" -> ");
            if (use.symbol == null) {
                out.append('?');
            } else {
                out.append(use.symbol.name).append('@').append(use.symbol.declaration.line());
                out.append(' ').append(use.symbol.category.label());
            }
            out.append('\n');
        }
        return out.toString();
    }

    private void define(String name, Category category, Node declaration) {
        Symbol symbol = new Symbol(name, category, declaration, table.level());
        if (table.declare(symbol)) {
            return;
        }
        Symbol first = table.lookupLocal(name);
        int firstLine = first != null && first.declaration != null ? first.declaration.line() : 0;
        errors.add("linha " + declaration.line() + ": '" + name
                + "' já declarado neste escopo (linha " + firstLine + ")");
    }

    /** lookup do nome de uso. Grava decl no nó chamador e acumula o erro. */
    private Symbol bind(Node site, String name) {
        Symbol symbol = table.lookup(name);
        uses.add(new Use(site, site.line(), name, symbol));
        if (symbol == null) {
            errors.add("linha " + site.line() + ": '" + name + "' não declarado");
        }
        return symbol;
    }

    @Override
    public Void visitProgram(Program node) {
        table.openScope();
        try {
            for (Decl declaration : node.declarations) {
                declaration.accept(this);
            }
        } finally {
            table.closeScope();
        }
        return null;
    }

    @Override
    public Void visitVarDecl(VarDecl node) {
        for (Var variable : node.variables) {
            variable.accept(this);
        }
        return null;
    }

    @Override
    public Void visitVar(Var node) {
        // O nome entra no escopo no declarador, antes do inicializador.
        // int c = c resolve para este c; int a = 1, b = a enxerga a.
        define(node.name, Category.VARIABLE, node);
        if (node.initializer != null) {
            node.initializer.accept(this);
        }
        return null;
    }

    @Override
    public Void visitFuncDecl(FuncDecl node) {
        Category category = node.returnType == null ? Category.PROCEDURE : Category.FUNCTION;
        // O nome fica no escopo de fora antes de abrir o da função,
        // então o corpo pode chamar a si mesmo.
        define(node.name, category, node);
        table.openScope();
        try {
            for (Param parameter : node.parameters) {
                parameter.accept(this);
            }
            // O bloco do corpo não abre outro escopo: é o mesmo dos parâmetros.
            // void f(int a) { int a; } é redeclaração. Um bloco interno abre.
            for (StmtNode statement : node.body.statements) {
                statement.accept(this);
            }
        } finally {
            table.closeScope();
        }
        return null;
    }

    @Override
    public Void visitParam(Param node) {
        define(node.name, Category.PARAMETER, node);
        return null;
    }

    @Override
    public Void visitTypeNode(TypeNode node) {
        return null;
    }

    @Override
    public Void visitBlock(Block node) {
        table.openScope();
        try {
            for (StmtNode statement : node.statements) {
                statement.accept(this);
            }
        } finally {
            table.closeScope();
        }
        return null;
    }

    @Override
    public Void visitAssign(Assign node) {
        node.decl = bind(node, node.name);
        node.value.accept(this);
        return null;
    }

    @Override
    public Void visitCallStmt(CallStmt node) {
        node.call.accept(this);
        return null;
    }

    @Override
    public Void visitIf(If node) {
        node.condition.accept(this);
        node.thenBranch.accept(this);
        if (node.elseBranch != null) {
            node.elseBranch.accept(this);
        }
        return null;
    }

    @Override
    public Void visitWhile(While node) {
        node.condition.accept(this);
        node.body.accept(this);
        return null;
    }

    @Override
    public Void visitReturn(Return node) {
        if (node.value != null) {
            node.value.accept(this);
        }
        return null;
    }

    @Override
    public Void visitBin(Bin node) {
        node.left.accept(this);
        node.right.accept(this);
        return null;
    }

    @Override
    public Void visitUnary(Unary node) {
        node.operand.accept(this);
        return null;
    }

    @Override
    public Void visitId(Id node) {
        node.decl = bind(node, node.name);
        return null;
    }

    @Override
    public Void visitCall(Call node) {
        node.decl = bind(node, node.name);
        for (Expr argument : node.arguments) {
            argument.accept(this);
        }
        return null;
    }

    @Override
    public Void visitLiteral(Literal node) {
        return null;
    }
}
