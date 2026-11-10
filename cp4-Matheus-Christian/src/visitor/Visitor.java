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
import ast.Param;
import ast.Program;
import ast.Return;
import ast.TypeNode;
import ast.Unary;
import ast.Var;
import ast.VarDecl;
import ast.While;

/**
 * Um método por tipo concreto de nó, nomeado pelo tipo (Aula 7).
 * Acrescentar uma operação nova é escrever uma classe que implementa esta
 * interface; nenhuma classe de nó muda.
 */
public interface Visitor<T> {

    T visitProgram(Program node);

    T visitVarDecl(VarDecl node);

    T visitVar(Var node);

    T visitFuncDecl(FuncDecl node);

    T visitParam(Param node);

    T visitTypeNode(TypeNode node);

    T visitBlock(Block node);

    T visitAssign(Assign node);

    T visitCallStmt(CallStmt node);

    T visitIf(If node);

    T visitWhile(While node);

    T visitReturn(Return node);

    T visitBin(Bin node);

    T visitUnary(Unary node);

    T visitId(Id node);

    T visitCall(Call node);

    T visitLiteral(Literal node);
}
