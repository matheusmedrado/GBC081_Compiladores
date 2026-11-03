package visitor;

import ast.Assign;
import ast.Bin;
import ast.Block;
import ast.Call;
import ast.CallStmt;
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

/**
 * Reimpressora de código-fonte: produz, a partir da AST, um programa na
 * sintaxe concreta da linguagem, em forma canônica. Toda expressão binária
 * sai entre parênteses, então a precedência e a associatividade ficam
 * explícitas no texto. O texto produzido pode ser lido de novo pelo próprio
 * parser (teste de ida e volta).
 */
public class SourcePrinter implements Visitor<String> {

    public static String print(Node node) {
        return node.accept(new SourcePrinter());
    }

    private static String indent(String text) {
        StringBuilder out = new StringBuilder();
        for (String line : text.split("\n", -1)) {
            if (!line.isEmpty()) {
                out.append("    ");
            }
            out.append(line).append('\n');
        }
        if (out.length() > 0) {
            out.setLength(out.length() - 1);
        }
        return out.toString();
    }

    @Override
    public String visitProgram(Program node) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < node.declarations.size(); i++) {
            if (i > 0) {
                out.append('\n');
            }
            out.append(node.declarations.get(i).accept(this)).append('\n');
        }
        return out.toString();
    }

    @Override
    public String visitVarDecl(VarDecl node) {
        StringBuilder out = new StringBuilder();
        out.append(node.type.accept(this));
        for (int i = 0; i < node.variables.size(); i++) {
            out.append(i == 0 ? " " : ", ");
            out.append(node.variables.get(i).accept(this));
        }
        out.append(';');
        return out.toString();
    }

    @Override
    public String visitVar(Var node) {
        if (node.initializer == null) {
            return node.name;
        }
        return node.name + " = " + node.initializer.accept(this);
    }

    @Override
    public String visitFuncDecl(FuncDecl node) {
        StringBuilder out = new StringBuilder();
        out.append(node.returnType == null ? "void" : node.returnType.accept(this));
        out.append(' ').append(node.name).append('(');
        for (int i = 0; i < node.parameters.size(); i++) {
            if (i > 0) {
                out.append(", ");
            }
            out.append(node.parameters.get(i).accept(this));
        }
        out.append(") ").append(node.body.accept(this));
        return out.toString();
    }

    @Override
    public String visitParam(Param node) {
        return node.type.accept(this) + " " + node.name;
    }

    @Override
    public String visitTypeNode(TypeNode node) {
        return node.name;
    }

    @Override
    public String visitBlock(Block node) {
        if (node.statements.isEmpty()) {
            return "{ }";
        }
        StringBuilder out = new StringBuilder();
        out.append("{\n");
        for (StmtNode statement : node.statements) {
            out.append(indent(statement.accept(this))).append('\n');
        }
        out.append('}');
        return out.toString();
    }

    @Override
    public String visitAssign(Assign node) {
        return node.name + " = " + node.value.accept(this) + ";";
    }

    @Override
    public String visitCallStmt(CallStmt node) {
        return node.call.accept(this) + ";";
    }

    @Override
    public String visitIf(If node) {
        StringBuilder out = new StringBuilder();
        out.append("if (").append(node.condition.accept(this)).append(") ");
        out.append(node.thenBranch.accept(this));
        if (node.elseBranch != null) {
            out.append(" else ").append(node.elseBranch.accept(this));
        }
        return out.toString();
    }

    @Override
    public String visitWhile(While node) {
        return "while (" + node.condition.accept(this) + ") " + node.body.accept(this);
    }

    @Override
    public String visitReturn(Return node) {
        if (node.value == null) {
            return "return;";
        }
        return "return " + node.value.accept(this) + ";";
    }

    @Override
    public String visitBin(Bin node) {
        return "(" + node.left.accept(this) + " " + node.operator + " " + node.right.accept(this) + ")";
    }

    @Override
    public String visitUnary(Unary node) {
        return "(" + node.operator + node.operand.accept(this) + ")";
    }

    @Override
    public String visitId(Id node) {
        return node.name;
    }

    @Override
    public String visitCall(Call node) {
        StringBuilder out = new StringBuilder();
        out.append(node.name).append('(');
        for (int i = 0; i < node.arguments.size(); i++) {
            if (i > 0) {
                out.append(", ");
            }
            out.append(node.arguments.get(i).accept(this));
        }
        out.append(')');
        return out.toString();
    }

    @Override
    public String visitLiteral(Literal node) {
        return node.value;
    }
}
