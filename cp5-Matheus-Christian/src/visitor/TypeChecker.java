package visitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import symbols.Symbol;
import types.Diagnostic;
import types.Type;
import types.TypeRules;

/**
 * Verificador de tipos: roda depois da identificação, sobre a mesma árvore.
 * Cada visit de expressão calcula o tipo, grava em node.type e devolve; os
 * visits de comando devolvem null. Não usa instanceof nem switch sobre o
 * tipo do nó, e não mexe na tabela: as assinaturas vêm do Symbol que a
 * identificação gravou em decl.
 *
 * As regras estão em types.TypeRules; aqui fica a parte que depende da
 * árvore e do contexto (função corrente, caminhos que retornam, variáveis
 * que podem estar sem valor). Os nomes entre colchetes são os de
 * tipos-design.md.
 *
 * Sem cascata: uma expressão com erro tem tipo ERROR, e ERROR é aceito por
 * toda regra sem mensagem nova.
 */
public class TypeChecker implements Visitor<Type> {

    private final List<Diagnostic> diagnostics = new ArrayList<Diagnostic>();

    // Contexto da função corrente. null fora de função (declarações globais).
    private FuncDecl currentFunction;
    private Type currentReturn = Type.VOID;

    /** Tipo do VarDecl corrente, para os seus Var. */
    private Type declaredType = Type.ERROR;

    /**
     * Decisão 9: o último comando visitado termina, em todo caminho, num
     * return. Bloco: algum comando retorna. if: os dois ramos retornam.
     * while: nunca (o corpo pode não rodar).
     */
    private boolean returns;

    /**
     * Variáveis locais (o nó Var da declaração) que podem estar sem valor
     * neste ponto. Análise conservadora: no if, fica sem valor o que ficou
     * em algum ramo; no while, o que estava antes do laço. Depois de um
     * return o conjunto esvazia (o resto do caminho não roda).
     */
    private Set<Node> unset = new HashSet<Node>();

    /** Uma variável gera no máximo um aviso. */
    private final Set<Node> warned = new HashSet<Node>();

    public static TypeChecker check(Program program) {
        TypeChecker checker = new TypeChecker();
        if (program != null) {
            program.accept(checker);
        }
        return checker;
    }

    /** Erros e avisos na ordem em que foram achados. */
    public List<Diagnostic> diagnostics() {
        return Collections.unmodifiableList(diagnostics);
    }

    public boolean hasErrors() {
        for (Diagnostic d : diagnostics) {
            if (!d.warning) {
                return true;
            }
        }
        return false;
    }

    private void error(Node node, String message) {
        diagnostics.add(Diagnostic.error(node.line(), message));
    }

    private void warning(Node node, String message) {
        diagnostics.add(Diagnostic.warning(node.line(), message));
    }

    /** Tipo guardado no símbolo; ERROR se não há símbolo ou tipo. */
    private static Type typeOf(Symbol symbol) {
        return symbol == null || symbol.type == null ? Type.ERROR : symbol.type;
    }

    private static String kindOf(Symbol callable) {
        return callable.type == Type.VOID ? "procedimento" : "função";
    }

    // ---------------------------------------------------------------
    // declarações

    @Override
    public Type visitProgram(Program node) {
        for (Decl declaration : node.declarations) {
            declaration.accept(this);
        }
        return null;
    }

    @Override
    public Type visitVarDecl(VarDecl node) {
        Type saved = declaredType;
        declaredType = Type.named(node.type.name);
        for (Var variable : node.variables) {
            variable.accept(this);
        }
        declaredType = saved;
        returns = false;
        return null;
    }

    @Override
    public Type visitVar(Var node) {
        // Só variável local entra na análise de valor (tipos-design.md,
        // seção 5): global e parâmetro sempre têm valor.
        boolean local = currentFunction != null;
        if (local) {
            unset.add(node);
        }
        // [DECL] O inicializador é conferido antes de a variável contar como
        // tendo valor: em int c = c, o c da direita dá aviso.
        if (node.initializer != null) {
            Type value = node.initializer.accept(this);
            if (!TypeRules.assignable(declaredType, value)) {
                error(node, "inicialização de '" + node.name + "': esperado "
                        + declaredType + ", veio " + value);
            }
            unset.remove(node);
        }
        return null;
    }

    @Override
    public Type visitFuncDecl(FuncDecl node) {
        FuncDecl savedFunction = currentFunction;
        Type savedReturn = currentReturn;
        Set<Node> savedUnset = unset;
        currentFunction = node;
        currentReturn = node.returnType == null ? Type.VOID : Type.named(node.returnType.name);
        unset = new HashSet<Node>();
        try {
            // O corpo é o mesmo escopo dos parâmetros (CP4, decisão 4.4);
            // aqui só importa a sequência de comandos.
            statements(node.body.statements);
            if (currentReturn != Type.VOID && !returns) {
                error(node, "função '" + node.name + "' pode terminar sem 'return'");
            }
        } finally {
            currentFunction = savedFunction;
            currentReturn = savedReturn;
            unset = savedUnset;
            returns = false;
        }
        return null;
    }

    @Override
    public Type visitParam(Param node) {
        return null;
    }

    @Override
    public Type visitTypeNode(TypeNode node) {
        return null;
    }

    // ---------------------------------------------------------------
    // comandos

    /** Visita em ordem; a sequência retorna se algum comando retorna. */
    private void statements(List<StmtNode> list) {
        boolean any = false;
        for (StmtNode statement : list) {
            statement.accept(this);
            any = any || returns;
        }
        returns = any;
    }

    @Override
    public Type visitBlock(Block node) {
        statements(node.statements);
        return null;
    }

    @Override
    public Type visitAssign(Assign node) {
        // [ATRIB] x : T, e : S, S compatível com T ⊢ x = e ok
        // A esquerda é conferida primeiro, para as mensagens saírem na ordem
        // do texto; o valor é visitado antes de x contar como tendo valor.
        Symbol target = node.decl;
        boolean variable = target != null && !target.isCallable();
        if (target != null && !variable) {
            // Decisão 11: nome de função não é variável.
            error(node, "'" + node.name + "' é " + kindOf(target) + ", não variável");
        }
        Type value = node.value.accept(this);
        if (variable) {
            if (!TypeRules.assignable(typeOf(target), value)) {
                error(node, "atribuição a '" + node.name + "': esperado "
                        + typeOf(target) + ", veio " + value);
            }
            unset.remove(target.declaration);
        }
        returns = false;
        return null;
    }

    @Override
    public Type visitCallStmt(CallStmt node) {
        // Decisão 6: função usada como comando tem o valor descartado.
        checkCall(node.call);
        returns = false;
        return null;
    }

    @Override
    public Type visitIf(If node) {
        // [IF] e : bool, S1 ok, S2 ok ⊢ if (e) S1 else S2 ok
        Type condition = node.condition.accept(this);
        if (!TypeRules.isCondition(condition)) {
            error(node.condition, "condição de 'if' deve ser bool, mas é " + condition);
        }
        Set<Node> before = new HashSet<Node>(unset);
        node.thenBranch.accept(this);
        boolean thenReturns = returns;
        Set<Node> afterThen = unset;
        unset = before;
        boolean elseReturns = false;
        if (node.elseBranch != null) {
            node.elseBranch.accept(this);
            elseReturns = returns;
        }
        unset.addAll(afterThen);
        returns = thenReturns && elseReturns;
        return null;
    }

    @Override
    public Type visitWhile(While node) {
        // [WHILE] e : bool, S ok ⊢ while (e) S ok
        Type condition = node.condition.accept(this);
        if (!TypeRules.isCondition(condition)) {
            error(node.condition, "condição de 'while' deve ser bool, mas é " + condition);
        }
        Set<Node> before = new HashSet<Node>(unset);
        node.body.accept(this);
        unset.addAll(before);
        returns = false;
        return null;
    }

    @Override
    public Type visitReturn(Return node) {
        String name = currentFunction == null ? "?" : currentFunction.name;
        if (node.value != null) {
            Type value = node.value.accept(this);
            if (currentReturn == Type.VOID) {
                // [RETURN-VOID] em procedimento só "return;"
                error(node, "'return' com valor no procedimento '" + name + "'");
            } else if (!TypeRules.assignable(currentReturn, value)) {
                // [RETURN] e : S, S compatível com o retorno T de f ⊢ return e ok
                error(node, "'return' de '" + name + "': esperado " + currentReturn + ", veio " + value);
            }
        } else if (currentReturn != Type.VOID) {
            error(node, "'return' sem valor na função '" + name + "', que devolve " + currentReturn);
        }
        returns = true;
        unset = new HashSet<Node>();
        return null;
    }

    // ---------------------------------------------------------------
    // expressões

    @Override
    public Type visitBin(Bin node) {
        Type left = node.left.accept(this);
        Type right = node.right.accept(this);
        Type result = TypeRules.binary(node.operator, left, right);
        if (result == null) {
            error(node, "operador '" + node.operator + "' não se aplica a " + left + " e " + right);
            result = Type.ERROR;
        }
        node.type = result;
        return result;
    }

    @Override
    public Type visitUnary(Unary node) {
        Type operand = node.operand.accept(this);
        Type result = TypeRules.unary(node.operator, operand);
        if (result == null) {
            error(node, "operador '" + node.operator + "' não se aplica a " + operand);
            result = Type.ERROR;
        }
        node.type = result;
        return result;
    }

    @Override
    public Type visitId(Id node) {
        // [ID] x declarado com tipo T ⊢ x : T. Sem declaração: ERROR, sem
        // mensagem (a identificação já reportou).
        Symbol symbol = node.decl;
        Type result;
        if (symbol == null) {
            result = Type.ERROR;
        } else if (symbol.isCallable()) {
            // Decisão 11: nome de função sem parênteses não é valor.
            error(node, "'" + node.name + "' é " + kindOf(symbol) + "; sem parênteses não é valor");
            result = Type.ERROR;
        } else {
            if (unset.contains(symbol.declaration) && warned.add(symbol.declaration)) {
                warning(node, "'" + node.name + "' pode ser usada sem valor");
            }
            result = typeOf(symbol);
        }
        node.type = result;
        return result;
    }

    @Override
    public Type visitCall(Call node) {
        Type result = checkCall(node);
        if (result == Type.VOID) {
            // [CHAMADA] só vale em expressão se o retorno não é void.
            error(node, "procedimento '" + node.name + "' não devolve valor e não pode ser usado em expressão");
            result = Type.ERROR;
        }
        node.type = result;
        return result;
    }

    /**
     * [CHAMADA] f : (T1..Tn) → R, ei : Si, Si compatível com Ti ⊢ f(e1..en) : R.
     * Confere que é função, a aridade e cada argumento. Devolve R (VOID no
     * procedimento; quem decide se VOID é erro é o chamador) ou ERROR.
     * Com aridade errada, os argumentos são visitados mas não comparados,
     * e o tipo continua R: a chamada errada não gera erro em quem a usa.
     */
    private Type checkCall(Call node) {
        Symbol callee = node.decl;
        Type result;
        if (callee == null) {
            visitArguments(node);
            result = Type.ERROR;
        } else if (!callee.isCallable() || callee.parameters == null) {
            error(node, "'" + node.name + "' não é função nem procedimento");
            visitArguments(node);
            result = Type.ERROR;
        } else if (callee.parameters.size() != node.arguments.size()) {
            error(node, "'" + node.name + "' espera " + callee.parameters.size()
                    + " argumento(s), recebeu " + node.arguments.size());
            visitArguments(node);
            result = typeOf(callee);
        } else {
            for (int i = 0; i < node.arguments.size(); i++) {
                Expr argument = node.arguments.get(i);
                Type actual = argument.accept(this);
                Type expected = callee.parameters.get(i);
                // Decisão 8: argumento segue a mesma compatibilidade da atribuição.
                if (!TypeRules.assignable(expected, actual)) {
                    error(argument, "argumento " + (i + 1) + " de '" + node.name
                            + "': esperado " + expected + ", veio " + actual);
                }
            }
            result = typeOf(callee);
        }
        node.type = result;
        return result;
    }

    private void visitArguments(Call node) {
        for (Expr argument : node.arguments) {
            argument.accept(this);
        }
    }

    @Override
    public Type visitLiteral(Literal node) {
        // [LIT-INT], [LIT-DOUBLE], [LIT-CHAR], [LIT-STRING], [LIT-BOOL]
        Type result = Type.named(node.kind);
        node.type = result;
        return result;
    }
}
