package visitor;

import ast.Assign;
import ast.Bin;
import ast.Block;
import ast.Call;
import ast.CallStmt;
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

/**
 * Impressora da árvore: um nó por linha, com indentação de dois espaços por
 * nível e os atributos relevantes ao lado do nome. O formato está documentado
 * em ast-design.md e é determinístico: a mesma árvore sempre produz o mesmo
 * texto. A posição não entra no texto, para que o arquivo esperado dependa só
 * da forma da árvore.
 */
public class TreePrinter implements Visitor<Void> {

    private final StringBuilder out = new StringBuilder();
    private int depth = 0;

    public static String print(Node node) {
        TreePrinter printer = new TreePrinter();
        node.accept(printer);
        return printer.out.toString();
    }

    private void line(String text) {
        for (int i = 0; i < depth; i++) {
            out.append("  ");
        }
        out.append(text).append('\n');
    }

    private void child(Node node) {
        depth++;
        node.accept(this);
        depth--;
    }

    /** Filho que pode ser comando ou declaração local (Block.statements, If.elseBranch). */
    private void childStatement(StmtNode statement) {
        depth++;
        statement.accept(this);
        depth--;
    }

    @Override
    public Void visitProgram(Program node) {
        line("Program");
        for (Node declaration : node.declarations) {
            child(declaration);
        }
        return null;
    }

    @Override
    public Void visitVarDecl(VarDecl node) {
        line("VarDecl");
        child(node.type);
        for (Var variable : node.variables) {
            child(variable);
        }
        return null;
    }

    @Override
    public Void visitVar(Var node) {
        line("Var(" + node.name + ")");
        if (node.initializer != null) {
            child(node.initializer);
        }
        return null;
    }

    @Override
    public Void visitFuncDecl(FuncDecl node) {
        line("FuncDecl(" + node.name + ")");
        if (node.returnType != null) {
            child(node.returnType);
        }
        for (Param parameter : node.parameters) {
            child(parameter);
        }
        child(node.body);
        return null;
    }

    @Override
    public Void visitParam(Param node) {
        line("Param(" + node.name + ")");
        child(node.type);
        return null;
    }

    @Override
    public Void visitTypeNode(TypeNode node) {
        line("Type(" + node.name + ")");
        return null;
    }

    @Override
    public Void visitBlock(Block node) {
        line("Block");
        for (StmtNode statement : node.statements) {
            childStatement(statement);
        }
        return null;
    }

    @Override
    public Void visitAssign(Assign node) {
        line("Assign(" + node.name + ")");
        child(node.value);
        return null;
    }

    @Override
    public Void visitCallStmt(CallStmt node) {
        line("CallStmt");
        child(node.call);
        return null;
    }

    @Override
    public Void visitIf(If node) {
        line("If");
        child(node.condition);
        child(node.thenBranch);
        if (node.elseBranch != null) {
            childStatement(node.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitWhile(While node) {
        line("While");
        child(node.condition);
        child(node.body);
        return null;
    }

    @Override
    public Void visitReturn(Return node) {
        line("Return");
        if (node.value != null) {
            child(node.value);
        }
        return null;
    }

    @Override
    public Void visitBin(Bin node) {
        line("Bin(" + node.operator + ")");
        child(node.left);
        child(node.right);
        return null;
    }

    @Override
    public Void visitUnary(Unary node) {
        line("Unary(" + node.operator + ")");
        child(node.operand);
        return null;
    }

    @Override
    public Void visitId(Id node) {
        line("Id(" + node.name + ")");
        return null;
    }

    @Override
    public Void visitCall(Call node) {
        line("Call(" + node.name + ")");
        for (Expr argument : node.arguments) {
            child(argument);
        }
        return null;
    }

    @Override
    public Void visitLiteral(Literal node) {
        line("Literal(" + node.kind + ", " + node.value + ")");
        return null;
    }
}
