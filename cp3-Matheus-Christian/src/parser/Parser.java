package parser;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.EnumSet;
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
import ast.Param;
import ast.Program;
import ast.Return;
import ast.StmtNode;
import ast.TypeNode;
import ast.Unary;
import ast.Var;
import ast.VarDecl;
import ast.While;
import lexer.Scanner;
import lexer.Token;
import lexer.TokenType;

import static lexer.TokenType.*;

/**
 * Parser recursivo descendente da gramática em gramatica-first-follow.md,
 * agora devolvendo a AST (Aula 7).
 *
 * Há um método parseX() para cada não-terminal X, na mesma ordem da gramática
 * BNF do documento, com a produção copiada no comentário. Os métodos que
 * correspondem a um nó devolvem esse nó; os não-terminais auxiliares de
 * recursão à esquerda (OrRest, AndRest, EqRest, AddRest, MulRest e as listas)
 * recebem o que já foi construído e devolvem a árvore maior já montada.
 * Terminal vira match(), não-terminal vira chamada, e a alternativa é
 * escolhida pelo lookahead olhando os conjuntos FIRST_X. Alternativa vazia só
 * é aceita com o lookahead em FOLLOW_X (versão robusta do ε).
 *
 * O reconhecimento é o do CP2, sem mudança: as decisões por FIRST, as
 * alternativas vazias por FOLLOW e a sincronização em modo pânico. Quando há
 * erro, parse() devolve a lista de erros e a árvore como nula.
 */
public class Parser {

    // ---------------------------------------------------------------
    // conjuntos FIRST e FOLLOW (tabela da seção 3 do documento)
    // ---------------------------------------------------------------

    static final Set<TokenType> TYPES = EnumSet.of(INT, DOUBLE, BOOL, CHAR, STRING);
    static final Set<TokenType> LITERALS = EnumSet.of(
            INT_LITERAL, DOUBLE_LITERAL, CHAR_LITERAL, STRING_LITERAL, TRUE, FALSE);

    static final Set<TokenType> FIRST_DECL = union(TYPES, EnumSet.of(VOID));
    static final Set<TokenType> FIRST_VAR_REST = EnumSet.of(ASSIGN, COMMA, SEMICOLON);
    static final Set<TokenType> FIRST_DECL_REST = union(EnumSet.of(LPAREN), FIRST_VAR_REST);
    static final Set<TokenType> FIRST_STMT = union(TYPES, EnumSet.of(IF, WHILE, RETURN, LBRACE, IDENTIFIER));
    static final Set<TokenType> FIRST_ELSE_BODY = EnumSet.of(LBRACE, IF);
    static final Set<TokenType> FIRST_ASSIGN_OR_CALL = EnumSet.of(ASSIGN, LPAREN);
    static final Set<TokenType> FIRST_PRIMARY = union(EnumSet.of(IDENTIFIER, LPAREN), LITERALS);
    static final Set<TokenType> FIRST_UNARY_OP = EnumSet.of(NOT, MINUS);
    static final Set<TokenType> FIRST_EXPR = union(FIRST_UNARY_OP, FIRST_PRIMARY);
    static final Set<TokenType> FIRST_EQ_OP = EnumSet.of(EQ, NEQ);
    static final Set<TokenType> FIRST_REL_OP = EnumSet.of(LT, LE, GT, GE);
    static final Set<TokenType> FIRST_ADD_OP = EnumSet.of(PLUS, MINUS);
    static final Set<TokenType> FIRST_MUL_OP = EnumSet.of(STAR, SLASH, PERCENT);

    static final Set<TokenType> FOLLOW_DECL_LIST = EnumSet.of(EOF);
    static final Set<TokenType> FOLLOW_DECL = union(FIRST_DECL, EnumSet.of(EOF));
    static final Set<TokenType> FOLLOW_DECL_REST = FOLLOW_DECL;
    static final Set<TokenType> FOLLOW_TYPE = EnumSet.of(IDENTIFIER);
    static final Set<TokenType> FOLLOW_PARAMS = EnumSet.of(RPAREN);
    static final Set<TokenType> FOLLOW_PARAM_LIST = FOLLOW_PARAMS;
    static final Set<TokenType> FOLLOW_STMT_LIST = EnumSet.of(RBRACE);
    static final Set<TokenType> FOLLOW_STMT = union(FIRST_STMT, EnumSet.of(RBRACE));
    static final Set<TokenType> FOLLOW_ELSE_PART = FOLLOW_STMT;
    static final Set<TokenType> FOLLOW_ELSE_BODY = FOLLOW_STMT;
    static final Set<TokenType> FOLLOW_VAR_INIT = EnumSet.of(COMMA, SEMICOLON);
    static final Set<TokenType> FOLLOW_VAR_LIST = EnumSet.of(SEMICOLON);
    static final Set<TokenType> FOLLOW_RETURN_VALUE = EnumSet.of(SEMICOLON);
    static final Set<TokenType> FOLLOW_ASSIGN_OR_CALL = EnumSet.of(SEMICOLON);
    static final Set<TokenType> FOLLOW_ARGS = EnumSet.of(RPAREN);
    static final Set<TokenType> FOLLOW_ARG_LIST = FOLLOW_ARGS;
    static final Set<TokenType> FOLLOW_EXPR = EnumSet.of(RPAREN, SEMICOLON, COMMA);
    static final Set<TokenType> FOLLOW_OR_REST = FOLLOW_EXPR;
    static final Set<TokenType> FOLLOW_AND_REST = union(FOLLOW_OR_REST, EnumSet.of(OR));
    static final Set<TokenType> FOLLOW_EQ_REST = union(FOLLOW_AND_REST, EnumSet.of(AND));
    static final Set<TokenType> FOLLOW_REL_REST = union(FOLLOW_EQ_REST, FIRST_EQ_OP);
    static final Set<TokenType> FOLLOW_ADD_REST = union(FOLLOW_REL_REST, FIRST_REL_OP);
    static final Set<TokenType> FOLLOW_MUL_REST = union(FOLLOW_ADD_REST, FIRST_ADD_OP);
    static final Set<TokenType> FOLLOW_UNARY = union(FOLLOW_MUL_REST, FIRST_MUL_OP);
    static final Set<TokenType> FOLLOW_PRIMARY = FOLLOW_UNARY;
    static final Set<TokenType> FOLLOW_CALL_SUFFIX = FOLLOW_UNARY;
    static final Set<TokenType> FOLLOW_LITERAL = FOLLOW_UNARY;
    static final Set<TokenType> FOLLOW_OPERATOR = FIRST_EXPR;   // EqOp, RelOp, AddOp, MulOp e UnaryOp

    // ---------------------------------------------------------------
    // âncoras da sincronização (README, "Recuperação de erros")
    // ---------------------------------------------------------------

    /**
     * Somadas ao FOLLOW na sincronização: palavras reservadas que só aparecem
     * no começo de um comando ou de uma declaração, e as chaves. Parar nelas
     * evita que um erro dentro de uma expressão engula os comandos seguintes.
     * O identificador fica de fora: ele aparece no meio das expressões, e parar
     * nele faria o resto de uma expressão quebrada virar um comando novo.
     */
    static final Set<TokenType> ANCHORS = union(TYPES, EnumSet.of(IF, WHILE, RETURN, VOID, LBRACE, RBRACE));

    /**
     * Âncoras do cabeçalho de função (Params, ParamList e Type): só as chaves,
     * porque tipo e identificador aparecem dentro da própria lista de parâmetros.
     */
    static final Set<TokenType> HEADER_ANCHORS = EnumSet.of(LBRACE, RBRACE);

    // ---------------------------------------------------------------
    // estado
    // ---------------------------------------------------------------

    private final Scanner scanner;
    private Token current;    // lookahead
    private Token next;       // segundo token, lido só quando match() precisa olhar adiante
    private Token previous;   // último token consumido
    private boolean panicMode = false;
    private final List<SyntaxError> errors = new ArrayList<>();
    private PrintStream trace;

    public Parser(Scanner scanner) {
        this.scanner = scanner;
    }

    /** Liga o traço da recuperação de erros (erros, tokens descartados, sincronização). */
    public void setTrace(PrintStream out) {
        this.trace = out;
    }

    /**
     * Analisa o programa inteiro numa rodada só. A árvore só vem preenchida
     * quando não houve erro sintático nem léxico; com erro, ela vem nula.
     */
    public ParseResult parse() {
        current = read();
        Program tree = parseProgram();
        boolean accepted = errors.isEmpty() && scanner.getErrors().isEmpty();
        return new ParseResult(accepted ? tree : null, errors);
    }

    public List<SyntaxError> getErrors() {
        return errors;
    }

    // ---------------------------------------------------------------
    // declarações
    // ---------------------------------------------------------------

    // Program → DeclList $
    private Program parseProgram() {
        int line = current.line;
        int column = current.column;
        List<Decl> declarations = parseDeclList(new ArrayList<Decl>());
        match(EOF);
        return new Program(line, column, declarations);
    }

    // DeclList → Decl DeclList | ε
    // as listas recebem o que já foi lido e acrescentam o resto, como os Rest
    // das expressões: cada chamada põe um elemento, sem copiar a lista
    private List<Decl> parseDeclList(List<Decl> declarations) {
        if (FIRST_DECL.contains(la())) {
            declarations.add(parseDecl());
            return parseDeclList(declarations);
        } else if (FOLLOW_DECL_LIST.contains(la())) {
            // ε
        } else {
            listError("DeclList", FIRST_DECL, FOLLOW_DECL_LIST);
            if (FIRST_DECL.contains(la())) {
                return parseDeclList(declarations);
            }
        }
        return declarations;
    }

    // Decl → Type id DeclRest | 'void' id '(' Params ')' Block
    private Decl parseDecl() {
        if (TYPES.contains(la())) {
            TypeNode type = parseType();
            Token name = match(IDENTIFIER);
            return parseDeclRest(type, name);
        } else if (la() == VOID) {
            Token keyword = match(VOID);
            Token name = match(IDENTIFIER);
            match(LPAREN);
            List<Param> parameters = parseParams();
            match(RPAREN);
            Block body = parseBlock();
            return new FuncDecl(keyword.line, keyword.column, null, text(name), parameters, body);
        } else {
            syntaxError("Decl", FIRST_DECL, sync(FOLLOW_DECL));
            return null;
        }
    }

    // DeclRest → '(' Params ')' Block | VarRest
    private Decl parseDeclRest(TypeNode type, Token name) {
        if (la() == LPAREN) {
            match(LPAREN);
            List<Param> parameters = parseParams();
            match(RPAREN);
            Block body = parseBlock();
            return new FuncDecl(type.line, type.column, type, text(name), parameters, body);
        } else if (FIRST_VAR_REST.contains(la())) {
            return parseVarRest(type, name);
        } else {
            syntaxError("DeclRest", FIRST_DECL_REST, sync(FOLLOW_DECL_REST));
            return null;
        }
    }

    // Type → 'int' | 'double' | 'bool' | 'char' | 'string'
    private TypeNode parseType() {
        if (TYPES.contains(la())) {
            Token token = match(la());
            return new TypeNode(token.line, token.column, token.lexeme);
        } else {
            syntaxError("Type", TYPES, syncHeader(FOLLOW_TYPE));
            return null;
        }
    }

    // Params → Param ParamList | ε
    private List<Param> parseParams() {
        List<Param> parameters = new ArrayList<>();
        if (TYPES.contains(la())) {
            parameters.add(parseParam());
            parseParamList(parameters);
        } else if (FOLLOW_PARAMS.contains(la())) {
            // ε
        } else {
            syntaxError("Params", union(TYPES, FOLLOW_PARAMS), syncHeader(FOLLOW_PARAMS));
        }
        return parameters;
    }

    // ParamList → ',' Param ParamList | ε
    private List<Param> parseParamList(List<Param> parameters) {
        if (la() == COMMA) {
            match(COMMA);
            parameters.add(parseParam());
            return parseParamList(parameters);
        } else if (FOLLOW_PARAM_LIST.contains(la())) {
            // ε
        } else {
            syntaxError("ParamList", union(EnumSet.of(COMMA), FOLLOW_PARAM_LIST), syncHeader(FOLLOW_PARAM_LIST));
        }
        return parameters;
    }

    // Param → Type id
    private Param parseParam() {
        TypeNode type = parseType();
        Token name = match(IDENTIFIER);
        if (type == null) {
            return null;
        }
        return new Param(type.line, type.column, type, text(name));
    }

    // ---------------------------------------------------------------
    // blocos e comandos
    // ---------------------------------------------------------------

    // Block → '{' StmtList '}'
    private Block parseBlock() {
        if (la() != LBRACE && FIRST_STMT.contains(la()) && peekNext().type != LBRACE) {
            // Recuperação: corpo sem chaves, como em "if (c) x = 1;". O comando
            // vira o corpo, para que o '}' de fora não feche um '{' que não existe.
            missing(LBRACE, ": o corpo precisa estar entre chaves");
            int line = current.line;
            int column = current.column;
            List<StmtNode> statements = new ArrayList<>();
            statements.add(parseStmt());
            return new Block(line, column, statements);
        }
        Token brace = match(LBRACE);
        List<StmtNode> statements = parseStmtList(new ArrayList<StmtNode>());
        match(RBRACE);
        return new Block(brace.line, brace.column, statements);
    }

    // StmtList → Stmt StmtList | ε
    private List<StmtNode> parseStmtList(List<StmtNode> statements) {
        if (FIRST_STMT.contains(la())) {
            statements.add(parseStmt());
            return parseStmtList(statements);
        } else if (FOLLOW_STMT_LIST.contains(la())) {
            // ε
        } else {
            listError("StmtList", FIRST_STMT, FOLLOW_STMT_LIST);
            if (FIRST_STMT.contains(la())) {
                return parseStmtList(statements);
            }
        }
        return statements;
    }

    // Stmt → VarDecl | IfStmt | WhileStmt | ReturnStmt | Block | id AssignOrCall ';'
    private StmtNode parseStmt() {
        if (TYPES.contains(la())) {
            return parseVarDecl();
        } else if (la() == IF) {
            return parseIfStmt();
        } else if (la() == WHILE) {
            return parseWhileStmt();
        } else if (la() == RETURN) {
            return parseReturnStmt();
        } else if (la() == LBRACE) {
            return parseBlock();
        } else if (la() == IDENTIFIER) {
            Token name = match(IDENTIFIER);
            StmtNode statement = parseAssignOrCall(name);
            match(SEMICOLON);
            return statement;
        } else {
            syntaxError("Stmt", FIRST_STMT, sync(FOLLOW_STMT));
            return null;
        }
    }

    // VarDecl → Type id VarRest
    private VarDecl parseVarDecl() {
        TypeNode type = parseType();
        Token name = match(IDENTIFIER);
        return parseVarRest(type, name);
    }

    // VarRest → VarInit VarList ';'
    private VarDecl parseVarRest(TypeNode type, Token name) {
        List<Var> variables = new ArrayList<>();
        variables.add(parseVarInit(name));
        parseVarList(variables);
        match(SEMICOLON);
        int line = type != null ? type.line : name.line;
        int column = type != null ? type.column : name.column;
        return new VarDecl(line, column, type, variables);
    }

    // VarInit → '=' Expr | ε
    private Var parseVarInit(Token name) {
        Expr initializer = null;
        if (la() == ASSIGN) {
            match(ASSIGN);
            initializer = parseExpr();
        } else if (FOLLOW_VAR_INIT.contains(la())) {
            // ε
        } else {
            syntaxError("VarInit", union(EnumSet.of(ASSIGN), FOLLOW_VAR_INIT), sync(FOLLOW_VAR_INIT));
        }
        return new Var(name.line, name.column, text(name), initializer);
    }

    // VarList → ',' id VarInit VarList | ε
    private List<Var> parseVarList(List<Var> variables) {
        if (la() == COMMA) {
            match(COMMA);
            Token name = match(IDENTIFIER);
            variables.add(parseVarInit(name));
            return parseVarList(variables);
        } else if (FOLLOW_VAR_LIST.contains(la())) {
            // ε
        } else {
            syntaxError("VarList", union(EnumSet.of(COMMA), FOLLOW_VAR_LIST), sync(FOLLOW_VAR_LIST));
        }
        return variables;
    }

    // IfStmt → 'if' '(' Expr ')' Block ElsePart
    private If parseIfStmt() {
        Token keyword = match(IF);
        match(LPAREN);
        Expr condition = parseExpr();
        match(RPAREN);
        Block thenBranch = parseBlock();
        StmtNode elseBranch = parseElsePart();
        return new If(keyword.line, keyword.column, condition, thenBranch, elseBranch);
    }

    // ElsePart → 'else' ElseBody | ε
    private StmtNode parseElsePart() {
        if (la() == ELSE) {
            match(ELSE);
            return parseElseBody();
        } else if (FOLLOW_ELSE_PART.contains(la())) {
            return null;   // ε
        } else {
            syntaxError("ElsePart", union(EnumSet.of(ELSE), FOLLOW_ELSE_PART), sync(FOLLOW_ELSE_PART));
            return null;
        }
    }

    // ElseBody → Block | IfStmt
    private StmtNode parseElseBody() {
        if (la() == LBRACE) {
            return parseBlock();
        } else if (la() == IF) {
            return parseIfStmt();
        } else {
            syntaxError("ElseBody", FIRST_ELSE_BODY, sync(FOLLOW_ELSE_BODY));
            return null;
        }
    }

    // WhileStmt → 'while' '(' Expr ')' Block
    private While parseWhileStmt() {
        Token keyword = match(WHILE);
        match(LPAREN);
        Expr condition = parseExpr();
        match(RPAREN);
        Block body = parseBlock();
        return new While(keyword.line, keyword.column, condition, body);
    }

    // ReturnStmt → 'return' ReturnValue ';'
    private Return parseReturnStmt() {
        Token keyword = match(RETURN);
        Expr value = parseReturnValue();
        match(SEMICOLON);
        return new Return(keyword.line, keyword.column, value);
    }

    // ReturnValue → Expr | ε
    private Expr parseReturnValue() {
        if (FIRST_EXPR.contains(la())) {
            return parseExpr();
        } else if (FOLLOW_RETURN_VALUE.contains(la())) {
            return null;   // ε
        } else {
            syntaxError("ReturnValue", union(FIRST_EXPR, FOLLOW_RETURN_VALUE), sync(FOLLOW_RETURN_VALUE));
            return null;
        }
    }

    // AssignOrCall → '=' Expr | '(' Args ')'
    private StmtNode parseAssignOrCall(Token name) {
        if (la() == ASSIGN) {
            match(ASSIGN);
            Expr value = parseExpr();
            return new Assign(name.line, name.column, text(name), value);
        } else if (la() == LPAREN) {
            match(LPAREN);
            List<Expr> arguments = parseArgs();
            match(RPAREN);
            Call call = new Call(name.line, name.column, text(name), arguments);
            return new CallStmt(name.line, name.column, call);
        } else {
            syntaxError("AssignOrCall", FIRST_ASSIGN_OR_CALL, sync(FOLLOW_ASSIGN_OR_CALL));
            return null;
        }
    }

    // Args → Expr ArgList | ε
    private List<Expr> parseArgs() {
        List<Expr> arguments = new ArrayList<>();
        if (FIRST_EXPR.contains(la())) {
            arguments.add(parseExpr());
            parseArgList(arguments);
        } else if (FOLLOW_ARGS.contains(la())) {
            // ε
        } else {
            syntaxError("Args", union(FIRST_EXPR, FOLLOW_ARGS), sync(FOLLOW_ARGS));
        }
        return arguments;
    }

    // ArgList → ',' Expr ArgList | ε
    private List<Expr> parseArgList(List<Expr> arguments) {
        if (la() == COMMA) {
            match(COMMA);
            arguments.add(parseExpr());
            return parseArgList(arguments);
        } else if (FOLLOW_ARG_LIST.contains(la())) {
            // ε
        } else {
            syntaxError("ArgList", union(EnumSet.of(COMMA), FOLLOW_ARG_LIST), sync(FOLLOW_ARG_LIST));
        }
        return arguments;
    }

    // ---------------------------------------------------------------
    // expressões, da menor para a maior precedência
    // ---------------------------------------------------------------

    // Expr → OrExpr
    private Expr parseExpr() {
        return parseOrExpr();
    }

    // OrExpr → AndExpr OrRest
    private Expr parseOrExpr() {
        return parseOrRest(parseAndExpr());
    }

    // OrRest → '||' AndExpr OrRest | ε
    // recebe o operando da esquerda e devolve a árvore já montada, da esquerda
    // para a direita: a || b || c vira (a || b) || c
    private Expr parseOrRest(Expr left) {
        if (la() == OR) {
            Token operator = match(OR);
            Expr right = parseAndExpr();
            return parseOrRest(binary(operator, left, right));
        } else if (FOLLOW_OR_REST.contains(la())) {
            return left;   // ε
        } else {
            syntaxError("OrRest", union(EnumSet.of(OR), FOLLOW_OR_REST), sync(FOLLOW_OR_REST));
            return left;
        }
    }

    // AndExpr → EqExpr AndRest
    private Expr parseAndExpr() {
        return parseAndRest(parseEqExpr());
    }

    // AndRest → '&&' EqExpr AndRest | ε
    private Expr parseAndRest(Expr left) {
        if (la() == AND) {
            Token operator = match(AND);
            Expr right = parseEqExpr();
            return parseAndRest(binary(operator, left, right));
        } else if (FOLLOW_AND_REST.contains(la())) {
            return left;   // ε
        } else {
            syntaxError("AndRest", union(EnumSet.of(AND), FOLLOW_AND_REST), sync(FOLLOW_AND_REST));
            return left;
        }
    }

    // EqExpr → RelExpr EqRest
    private Expr parseEqExpr() {
        return parseEqRest(parseRelExpr());
    }

    // EqRest → EqOp RelExpr EqRest | ε
    private Expr parseEqRest(Expr left) {
        if (FIRST_EQ_OP.contains(la())) {
            Token operator = parseEqOp();
            Expr right = parseRelExpr();
            return parseEqRest(binary(operator, left, right));
        } else if (FOLLOW_EQ_REST.contains(la())) {
            return left;   // ε
        } else {
            syntaxError("EqRest", union(FIRST_EQ_OP, FOLLOW_EQ_REST), sync(FOLLOW_EQ_REST));
            return left;
        }
    }

    // EqOp → '==' | '!='
    private Token parseEqOp() {
        if (FIRST_EQ_OP.contains(la())) {
            return match(la());
        } else {
            syntaxError("EqOp", FIRST_EQ_OP, sync(FOLLOW_OPERATOR));
            return current;
        }
    }

    // RelExpr → AddExpr RelRest
    private Expr parseRelExpr() {
        return parseRelRest(parseAddExpr());
    }

    // RelRest → RelOp AddExpr | ε   (não repete: a < b < c é erro)
    private Expr parseRelRest(Expr left) {
        if (FIRST_REL_OP.contains(la())) {
            Token operator = parseRelOp();
            Expr right = parseAddExpr();
            return binary(operator, left, right);
        } else if (FOLLOW_REL_REST.contains(la())) {
            return left;   // ε
        } else {
            syntaxError("RelRest", union(FIRST_REL_OP, FOLLOW_REL_REST), sync(FOLLOW_REL_REST));
            return left;
        }
    }

    // RelOp → '<' | '<=' | '>' | '>='
    private Token parseRelOp() {
        if (FIRST_REL_OP.contains(la())) {
            return match(la());
        } else {
            syntaxError("RelOp", FIRST_REL_OP, sync(FOLLOW_OPERATOR));
            return current;
        }
    }

    // AddExpr → MulExpr AddRest
    private Expr parseAddExpr() {
        return parseAddRest(parseMulExpr());
    }

    // AddRest → AddOp MulExpr AddRest | ε
    private Expr parseAddRest(Expr left) {
        if (FIRST_ADD_OP.contains(la())) {
            Token operator = parseAddOp();
            Expr right = parseMulExpr();
            return parseAddRest(binary(operator, left, right));
        } else if (FOLLOW_ADD_REST.contains(la())) {
            return left;   // ε
        } else {
            syntaxError("AddRest", union(FIRST_ADD_OP, FOLLOW_ADD_REST), sync(FOLLOW_ADD_REST));
            return left;
        }
    }

    // AddOp → '+' | '-'
    private Token parseAddOp() {
        if (FIRST_ADD_OP.contains(la())) {
            return match(la());
        } else {
            syntaxError("AddOp", FIRST_ADD_OP, sync(FOLLOW_OPERATOR));
            return current;
        }
    }

    // MulExpr → Unary MulRest
    private Expr parseMulExpr() {
        return parseMulRest(parseUnary());
    }

    // MulRest → MulOp Unary MulRest | ε
    private Expr parseMulRest(Expr left) {
        if (FIRST_MUL_OP.contains(la())) {
            Token operator = parseMulOp();
            Expr right = parseUnary();
            return parseMulRest(binary(operator, left, right));
        } else if (FOLLOW_MUL_REST.contains(la())) {
            return left;   // ε
        } else {
            syntaxError("MulRest", union(FIRST_MUL_OP, FOLLOW_MUL_REST), sync(FOLLOW_MUL_REST));
            return left;
        }
    }

    // MulOp → '*' | '/' | '%'
    private Token parseMulOp() {
        if (FIRST_MUL_OP.contains(la())) {
            return match(la());
        } else {
            syntaxError("MulOp", FIRST_MUL_OP, sync(FOLLOW_OPERATOR));
            return current;
        }
    }

    // Unary → UnaryOp Unary | Primary
    private Expr parseUnary() {
        if (FIRST_UNARY_OP.contains(la())) {
            Token operator = parseUnaryOp();
            Expr operand = parseUnary();
            return new Unary(operator.line, operator.column, operator.lexeme, operand);
        } else if (FIRST_PRIMARY.contains(la())) {
            return parsePrimary();
        } else {
            syntaxError("Unary", FIRST_EXPR, sync(FOLLOW_UNARY));
            return null;
        }
    }

    // UnaryOp → '!' | '-'
    private Token parseUnaryOp() {
        if (FIRST_UNARY_OP.contains(la())) {
            return match(la());
        } else {
            syntaxError("UnaryOp", FIRST_UNARY_OP, sync(FOLLOW_OPERATOR));
            return current;
        }
    }

    // Primary → id CallSuffix | Literal | '(' Expr ')'
    private Expr parsePrimary() {
        if (la() == IDENTIFIER) {
            Token name = match(IDENTIFIER);
            return parseCallSuffix(name);
        } else if (LITERALS.contains(la())) {
            return parseLiteral();
        } else if (la() == LPAREN) {
            match(LPAREN);
            Expr expr = parseExpr();
            match(RPAREN);
            return expr;   // os parênteses não viram nó
        } else {
            syntaxError("Primary", FIRST_PRIMARY, sync(FOLLOW_PRIMARY));
            return null;
        }
    }

    // CallSuffix → '(' Args ')' | ε
    private Expr parseCallSuffix(Token name) {
        if (la() == LPAREN) {
            match(LPAREN);
            List<Expr> arguments = parseArgs();
            match(RPAREN);
            return new Call(name.line, name.column, text(name), arguments);
        } else if (FOLLOW_CALL_SUFFIX.contains(la())) {
            return new Id(name.line, name.column, text(name));   // ε
        } else {
            syntaxError("CallSuffix", union(EnumSet.of(LPAREN), FOLLOW_CALL_SUFFIX), sync(FOLLOW_CALL_SUFFIX));
            return new Id(name.line, name.column, text(name));
        }
    }

    // Literal → int_lit | double_lit | char_lit | string_lit | 'true' | 'false'
    private Literal parseLiteral() {
        if (LITERALS.contains(la())) {
            Token token = match(la());
            return new Literal(token.line, token.column, literalKind(token.type), token.lexeme);
        } else {
            syntaxError("Literal", LITERALS, sync(FOLLOW_LITERAL));
            return null;
        }
    }

    /** Tipo básico de um literal, para o atributo kind do nó. */
    private static String literalKind(TokenType type) {
        switch (type) {
            case INT_LITERAL:    return "int";
            case DOUBLE_LITERAL: return "double";
            case CHAR_LITERAL:   return "char";
            case STRING_LITERAL: return "string";
            case TRUE:
            case FALSE:          return "bool";
            default:             return type.name().toLowerCase();
        }
    }

    /**
     * Nó de operação binária. A posição é a do operador. Se um dos operandos
     * não pôde ser montado (erro no meio da expressão), o nó não é criado: a
     * árvore inteira será descartada de qualquer forma, porque houve erro.
     */
    private static Expr binary(Token operator, Expr left, Expr right) {
        if (left == null || right == null) {
            return null;
        }
        return new Bin(operator.line, operator.column, operator.lexeme, left, right);
    }

    /** Lexema de um token que pode ter sido dado como ausente pelo match. */
    private static String text(Token token) {
        return token.lexeme == null ? "" : token.lexeme;
    }

    // ---------------------------------------------------------------
    // tokens e match
    // ---------------------------------------------------------------

    /** Tipo do lookahead. */
    private TokenType la() {
        return current.type;
    }

    private Token read() {
        int before = scanner.getErrors().size();
        Token token = scanner.nextToken();
        // erros léxicos aparecem no traço no ponto em que o scanner os encontrou
        for (int i = before; i < scanner.getErrors().size(); i++) {
            trace(scanner.getErrors().get(i).toString());
        }
        return token;
    }

    /** Consome o lookahead. O EOF nunca é consumido: o parser fica parado nele. */
    private void advance() {
        if (current.type == EOF) {
            return;
        }
        previous = current;
        current = next != null ? next : read();
        next = null;
    }

    /** Token depois do lookahead. Só a recuperação de erro do match() usa. */
    private Token peekNext() {
        if (next == null) {
            next = current.type == EOF ? current : read();
        }
        return next;
    }

    /**
     * Casa o terminal esperado e devolve o token casado. Se o lookahead não é ele:
     *  - token a mais: o seguinte é o esperado, então o atual é descartado;
     *  - token faltando: reporta e segue sem consumir nada, como se o esperado
     *    estivesse ali (o terminal sai da pilha de análise sem casar). Nesse caso
     *    devolve um token sintético, para quem chama nunca receber null.
     * Nos dois casos nada pode entrar em loop: ou consome, ou só retorna.
     */
    private Token match(TokenType expected) {
        if (current.type == expected) {
            Token matched = current;
            advance();
            panicMode = false;
            return matched;
        }
        if (current.type != EOF && peekNext().type == expected) {
            boolean reported = report(current.line, current.column, "encontrado "
                    + TokenNames.describe(current) + " a mais antes de " + TokenNames.describe(expected));
            trace("    [match] " + cascade(reported) + "descartou " + TokenNames.describe(current)
                    + " e casou " + TokenNames.describe(expected));
            advance();
            Token matched = current;
            advance();
            panicMode = false;
            return matched;
        }
        boolean reported = missing(expected, "");
        trace("    [match] " + cascade(reported) + TokenNames.describe(expected)
                + " ausente, segue sem consumir");
        int line = previous == null ? current.line : previous.line;
        int column = previous == null ? current.column : previous.column + previous.lexeme.length();
        return new Token(expected, "", line, column);
    }

    /** Reporta um terminal que falta logo depois do último token consumido. */
    private boolean missing(TokenType expected, String hint) {
        String message = "esperado " + TokenNames.describe(expected);
        if (previous == null) {
            return report(current.line, current.column, message + hint);
        }
        int column = previous.column + previous.lexeme.length();
        return report(previous.line, column, message + " depois de " + TokenNames.describe(previous) + hint);
    }

    // ---------------------------------------------------------------
    // erros e sincronização (modo pânico)
    // ---------------------------------------------------------------

    /**
     * Erro de decisão em parseX: o lookahead não está em FIRST(X) nem, quando
     * X é anulável, em FOLLOW(X). Reporta com o conjunto que foi testado e
     * descarta tokens até um do conjunto de sincronização. Depois disso parseX
     * retorna, como se X tivesse sido reconhecido.
     */
    private void syntaxError(String nonterminal, Set<TokenType> expected, Set<TokenType> sync) {
        boolean reported = report(current.line, current.column, "encontrado "
                + TokenNames.describe(current) + ", esperado " + TokenNames.describe(expected));
        synchronize(nonterminal, sync, reported);
    }

    /**
     * Erro de decisão numa lista (DeclList, StmtList). O SYNC é o FOLLOW da
     * lista mais o FIRST do elemento: parando num FIRST, a lista é retomada.
     */
    private void listError(String nonterminal, Set<TokenType> firstElement, Set<TokenType> follow) {
        Set<TokenType> expected = union(firstElement, follow);
        boolean reported = report(current.line, current.column, "encontrado "
                + TokenNames.describe(current) + ", esperado " + TokenNames.describe(expected));
        synchronize(nonterminal, expected, reported);
        if (firstElement.contains(la())) {
            trace("    [" + nonterminal + "] retoma a lista em " + TokenNames.describe(current));
        }
    }

    /**
     * Descarta tokens até achar um do conjunto (ou o EOF, que sempre para).
     * No traço, um erro em cascata que não descartou nada não aparece: é só um
     * não-terminal de fora vendo o mesmo token em que o de dentro já parou.
     */
    private void synchronize(String nonterminal, Set<TokenType> sync, boolean reported) {
        List<String> skipped = new ArrayList<>();
        while (current.type != EOF && !sync.contains(current.type)) {
            skipped.add(TokenNames.describe(current));
            advance();
        }
        if (reported || !skipped.isEmpty()) {
            String what = skipped.isEmpty() ? "nada descartado" : "descartou " + abbreviate(skipped);
            trace("    [" + nonterminal + "] " + cascade(reported) + what + "; sincronizou em "
                    + TokenNames.describe(current) + " (" + current.line + ":" + current.column + ")");
        }
    }

    /** SYNC(X) = FOLLOW(X) ∪ âncoras. */
    private static Set<TokenType> sync(Set<TokenType> follow) {
        return union(follow, ANCHORS);
    }

    /** SYNC(X) para os não-terminais do cabeçalho de função. */
    private static Set<TokenType> syncHeader(Set<TokenType> follow) {
        return union(follow, HEADER_ANCHORS);
    }

    /**
     * Guarda o erro, a não ser que o parser já esteja em modo pânico: aí é
     * erro em cascata do anterior e não é reportado. Devolve se reportou.
     */
    private boolean report(int line, int column, String message) {
        if (panicMode) {
            return false;
        }
        SyntaxError error = new SyntaxError(message, line, column);
        errors.add(error);
        panicMode = true;
        trace(error.toString());
        return true;
    }

    private static String cascade(boolean reported) {
        return reported ? "" : "em cascata: ";
    }

    private void trace(String line) {
        if (trace != null) {
            trace.println(line);
        }
    }

    private static String abbreviate(List<String> tokens) {
        int max = 8;
        if (tokens.size() <= max) {
            return String.join(" ", tokens);
        }
        return String.join(" ", tokens.subList(0, max)) + " ... (" + tokens.size() + " tokens)";
    }

    @SafeVarargs
    private static Set<TokenType> union(Set<TokenType>... sets) {
        Set<TokenType> result = EnumSet.noneOf(TokenType.class);
        for (Set<TokenType> set : sets) {
            result.addAll(set);
        }
        return result;
    }
}
