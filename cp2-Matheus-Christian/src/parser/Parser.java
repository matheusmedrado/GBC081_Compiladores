package parser;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import lexer.Scanner;
import lexer.Token;
import lexer.TokenType;

import static lexer.TokenType.*;

/**
 * Parser recursivo descendente (só reconhecedor) da gramática em
 * gramatica-first-follow.md.
 *
 * Há um método parseX() para cada não-terminal X, na mesma ordem da gramática
 * BNF do documento, com a produção copiada no comentário. Terminal vira
 * match(), não-terminal vira chamada, e a alternativa é escolhida pelo
 * lookahead olhando os conjuntos FIRST_X. Alternativa vazia só é aceita com
 * o lookahead em FOLLOW_X (versão robusta do ε); fora de FIRST e FOLLOW é erro.
 *
 * Recuperação de erros (detalhada no README):
 *  - erro de decisão em parseX: reporta e descarta tokens até SYNC(X), que é
 *    FOLLOW(X) mais as âncoras; nas listas (DeclList, StmtList) o SYNC tem
 *    também o FIRST do elemento, e a lista continua dali;
 *  - erro em match(t): se o token seguinte é t, o atual está sobrando e é
 *    descartado; senão t é dado como ausente e o parser segue sem consumir;
 *  - modo pânico: depois de um erro, os seguintes não são reportados até um
 *    match() dar certo, para não listar erros em cascata.
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
     * Analisa o programa inteiro numa rodada só. Devolve true se o programa
     * foi aceito, isto é, se não houve erro sintático nem léxico.
     */
    public boolean parse() {
        current = read();
        parseProgram();
        return errors.isEmpty() && scanner.getErrors().isEmpty();
    }

    public List<SyntaxError> getErrors() {
        return errors;
    }

    // ---------------------------------------------------------------
    // declarações
    // ---------------------------------------------------------------

    // Program → DeclList $
    private void parseProgram() {
        parseDeclList();
        match(EOF);
    }

    // DeclList → Decl DeclList | ε
    private void parseDeclList() {
        if (FIRST_DECL.contains(la())) {
            parseDecl();
            parseDeclList();
        } else if (FOLLOW_DECL_LIST.contains(la())) {
            // ε
        } else {
            listError("DeclList", FIRST_DECL, FOLLOW_DECL_LIST);
            if (FIRST_DECL.contains(la())) {
                parseDeclList();
            }
        }
    }

    // Decl → Type id DeclRest | 'void' id '(' Params ')' Block
    private void parseDecl() {
        if (TYPES.contains(la())) {
            parseType();
            match(IDENTIFIER);
            parseDeclRest();
        } else if (la() == VOID) {
            match(VOID);
            match(IDENTIFIER);
            match(LPAREN);
            parseParams();
            match(RPAREN);
            parseBlock();
        } else {
            syntaxError("Decl", FIRST_DECL, sync(FOLLOW_DECL));
        }
    }

    // DeclRest → '(' Params ')' Block | VarRest
    private void parseDeclRest() {
        if (la() == LPAREN) {
            match(LPAREN);
            parseParams();
            match(RPAREN);
            parseBlock();
        } else if (FIRST_VAR_REST.contains(la())) {
            parseVarRest();
        } else {
            syntaxError("DeclRest", FIRST_DECL_REST, sync(FOLLOW_DECL_REST));
        }
    }

    // Type → 'int' | 'double' | 'bool' | 'char' | 'string'
    private void parseType() {
        if (TYPES.contains(la())) {
            match(la());
        } else {
            syntaxError("Type", TYPES, syncHeader(FOLLOW_TYPE));
        }
    }

    // Params → Param ParamList | ε
    private void parseParams() {
        if (TYPES.contains(la())) {
            parseParam();
            parseParamList();
        } else if (FOLLOW_PARAMS.contains(la())) {
            // ε
        } else {
            syntaxError("Params", union(TYPES, FOLLOW_PARAMS), syncHeader(FOLLOW_PARAMS));
        }
    }

    // ParamList → ',' Param ParamList | ε
    private void parseParamList() {
        if (la() == COMMA) {
            match(COMMA);
            parseParam();
            parseParamList();
        } else if (FOLLOW_PARAM_LIST.contains(la())) {
            // ε
        } else {
            syntaxError("ParamList", union(EnumSet.of(COMMA), FOLLOW_PARAM_LIST), syncHeader(FOLLOW_PARAM_LIST));
        }
    }

    // Param → Type id
    private void parseParam() {
        parseType();
        match(IDENTIFIER);
    }

    // ---------------------------------------------------------------
    // blocos e comandos
    // ---------------------------------------------------------------

    // Block → '{' StmtList '}'
    private void parseBlock() {
        if (la() != LBRACE && FIRST_STMT.contains(la()) && peekNext().type != LBRACE) {
            // Recuperação: corpo sem chaves, como em "if (c) x = 1;". O comando
            // vira o corpo, para que o '}' de fora não feche um '{' que não existe.
            missing(LBRACE, ": o corpo precisa estar entre chaves");
            parseStmt();
            return;
        }
        match(LBRACE);
        parseStmtList();
        match(RBRACE);
    }

    // StmtList → Stmt StmtList | ε
    private void parseStmtList() {
        if (FIRST_STMT.contains(la())) {
            parseStmt();
            parseStmtList();
        } else if (FOLLOW_STMT_LIST.contains(la())) {
            // ε
        } else {
            listError("StmtList", FIRST_STMT, FOLLOW_STMT_LIST);
            if (FIRST_STMT.contains(la())) {
                parseStmtList();
            }
        }
    }

    // Stmt → VarDecl | IfStmt | WhileStmt | ReturnStmt | Block | id AssignOrCall ';'
    private void parseStmt() {
        if (TYPES.contains(la())) {
            parseVarDecl();
        } else if (la() == IF) {
            parseIfStmt();
        } else if (la() == WHILE) {
            parseWhileStmt();
        } else if (la() == RETURN) {
            parseReturnStmt();
        } else if (la() == LBRACE) {
            parseBlock();
        } else if (la() == IDENTIFIER) {
            match(IDENTIFIER);
            parseAssignOrCall();
            match(SEMICOLON);
        } else {
            syntaxError("Stmt", FIRST_STMT, sync(FOLLOW_STMT));
        }
    }

    // VarDecl → Type id VarRest
    private void parseVarDecl() {
        parseType();
        match(IDENTIFIER);
        parseVarRest();
    }

    // VarRest → VarInit VarList ';'
    private void parseVarRest() {
        parseVarInit();
        parseVarList();
        match(SEMICOLON);
    }

    // VarInit → '=' Expr | ε
    private void parseVarInit() {
        if (la() == ASSIGN) {
            match(ASSIGN);
            parseExpr();
        } else if (FOLLOW_VAR_INIT.contains(la())) {
            // ε
        } else {
            syntaxError("VarInit", union(EnumSet.of(ASSIGN), FOLLOW_VAR_INIT), sync(FOLLOW_VAR_INIT));
        }
    }

    // VarList → ',' id VarInit VarList | ε
    private void parseVarList() {
        if (la() == COMMA) {
            match(COMMA);
            match(IDENTIFIER);
            parseVarInit();
            parseVarList();
        } else if (FOLLOW_VAR_LIST.contains(la())) {
            // ε
        } else {
            syntaxError("VarList", union(EnumSet.of(COMMA), FOLLOW_VAR_LIST), sync(FOLLOW_VAR_LIST));
        }
    }

    // IfStmt → 'if' '(' Expr ')' Block ElsePart
    private void parseIfStmt() {
        match(IF);
        match(LPAREN);
        parseExpr();
        match(RPAREN);
        parseBlock();
        parseElsePart();
    }

    // ElsePart → 'else' ElseBody | ε
    private void parseElsePart() {
        if (la() == ELSE) {
            match(ELSE);
            parseElseBody();
        } else if (FOLLOW_ELSE_PART.contains(la())) {
            // ε
        } else {
            syntaxError("ElsePart", union(EnumSet.of(ELSE), FOLLOW_ELSE_PART), sync(FOLLOW_ELSE_PART));
        }
    }

    // ElseBody → Block | IfStmt
    private void parseElseBody() {
        if (la() == LBRACE) {
            parseBlock();
        } else if (la() == IF) {
            parseIfStmt();
        } else {
            syntaxError("ElseBody", FIRST_ELSE_BODY, sync(FOLLOW_ELSE_BODY));
        }
    }

    // WhileStmt → 'while' '(' Expr ')' Block
    private void parseWhileStmt() {
        match(WHILE);
        match(LPAREN);
        parseExpr();
        match(RPAREN);
        parseBlock();
    }

    // ReturnStmt → 'return' ReturnValue ';'
    private void parseReturnStmt() {
        match(RETURN);
        parseReturnValue();
        match(SEMICOLON);
    }

    // ReturnValue → Expr | ε
    private void parseReturnValue() {
        if (FIRST_EXPR.contains(la())) {
            parseExpr();
        } else if (FOLLOW_RETURN_VALUE.contains(la())) {
            // ε
        } else {
            syntaxError("ReturnValue", union(FIRST_EXPR, FOLLOW_RETURN_VALUE), sync(FOLLOW_RETURN_VALUE));
        }
    }

    // AssignOrCall → '=' Expr | '(' Args ')'
    private void parseAssignOrCall() {
        if (la() == ASSIGN) {
            match(ASSIGN);
            parseExpr();
        } else if (la() == LPAREN) {
            match(LPAREN);
            parseArgs();
            match(RPAREN);
        } else {
            syntaxError("AssignOrCall", FIRST_ASSIGN_OR_CALL, sync(FOLLOW_ASSIGN_OR_CALL));
        }
    }

    // Args → Expr ArgList | ε
    private void parseArgs() {
        if (FIRST_EXPR.contains(la())) {
            parseExpr();
            parseArgList();
        } else if (FOLLOW_ARGS.contains(la())) {
            // ε
        } else {
            syntaxError("Args", union(FIRST_EXPR, FOLLOW_ARGS), sync(FOLLOW_ARGS));
        }
    }

    // ArgList → ',' Expr ArgList | ε
    private void parseArgList() {
        if (la() == COMMA) {
            match(COMMA);
            parseExpr();
            parseArgList();
        } else if (FOLLOW_ARG_LIST.contains(la())) {
            // ε
        } else {
            syntaxError("ArgList", union(EnumSet.of(COMMA), FOLLOW_ARG_LIST), sync(FOLLOW_ARG_LIST));
        }
    }

    // ---------------------------------------------------------------
    // expressões, da menor para a maior precedência
    // ---------------------------------------------------------------

    // Expr → OrExpr
    private void parseExpr() {
        parseOrExpr();
    }

    // OrExpr → AndExpr OrRest
    private void parseOrExpr() {
        parseAndExpr();
        parseOrRest();
    }

    // OrRest → '||' AndExpr OrRest | ε
    private void parseOrRest() {
        if (la() == OR) {
            match(OR);
            parseAndExpr();
            parseOrRest();
        } else if (FOLLOW_OR_REST.contains(la())) {
            // ε
        } else {
            syntaxError("OrRest", union(EnumSet.of(OR), FOLLOW_OR_REST), sync(FOLLOW_OR_REST));
        }
    }

    // AndExpr → EqExpr AndRest
    private void parseAndExpr() {
        parseEqExpr();
        parseAndRest();
    }

    // AndRest → '&&' EqExpr AndRest | ε
    private void parseAndRest() {
        if (la() == AND) {
            match(AND);
            parseEqExpr();
            parseAndRest();
        } else if (FOLLOW_AND_REST.contains(la())) {
            // ε
        } else {
            syntaxError("AndRest", union(EnumSet.of(AND), FOLLOW_AND_REST), sync(FOLLOW_AND_REST));
        }
    }

    // EqExpr → RelExpr EqRest
    private void parseEqExpr() {
        parseRelExpr();
        parseEqRest();
    }

    // EqRest → EqOp RelExpr EqRest | ε
    private void parseEqRest() {
        if (FIRST_EQ_OP.contains(la())) {
            parseEqOp();
            parseRelExpr();
            parseEqRest();
        } else if (FOLLOW_EQ_REST.contains(la())) {
            // ε
        } else {
            syntaxError("EqRest", union(FIRST_EQ_OP, FOLLOW_EQ_REST), sync(FOLLOW_EQ_REST));
        }
    }

    // EqOp → '==' | '!='
    private void parseEqOp() {
        if (FIRST_EQ_OP.contains(la())) {
            match(la());
        } else {
            syntaxError("EqOp", FIRST_EQ_OP, sync(FOLLOW_OPERATOR));
        }
    }

    // RelExpr → AddExpr RelRest
    private void parseRelExpr() {
        parseAddExpr();
        parseRelRest();
    }

    // RelRest → RelOp AddExpr | ε
    private void parseRelRest() {
        if (FIRST_REL_OP.contains(la())) {
            parseRelOp();
            parseAddExpr();
        } else if (FOLLOW_REL_REST.contains(la())) {
            // ε
        } else {
            syntaxError("RelRest", union(FIRST_REL_OP, FOLLOW_REL_REST), sync(FOLLOW_REL_REST));
        }
    }

    // RelOp → '<' | '<=' | '>' | '>='
    private void parseRelOp() {
        if (FIRST_REL_OP.contains(la())) {
            match(la());
        } else {
            syntaxError("RelOp", FIRST_REL_OP, sync(FOLLOW_OPERATOR));
        }
    }

    // AddExpr → MulExpr AddRest
    private void parseAddExpr() {
        parseMulExpr();
        parseAddRest();
    }

    // AddRest → AddOp MulExpr AddRest | ε
    private void parseAddRest() {
        if (FIRST_ADD_OP.contains(la())) {
            parseAddOp();
            parseMulExpr();
            parseAddRest();
        } else if (FOLLOW_ADD_REST.contains(la())) {
            // ε
        } else {
            syntaxError("AddRest", union(FIRST_ADD_OP, FOLLOW_ADD_REST), sync(FOLLOW_ADD_REST));
        }
    }

    // AddOp → '+' | '-'
    private void parseAddOp() {
        if (FIRST_ADD_OP.contains(la())) {
            match(la());
        } else {
            syntaxError("AddOp", FIRST_ADD_OP, sync(FOLLOW_OPERATOR));
        }
    }

    // MulExpr → Unary MulRest
    private void parseMulExpr() {
        parseUnary();
        parseMulRest();
    }

    // MulRest → MulOp Unary MulRest | ε
    private void parseMulRest() {
        if (FIRST_MUL_OP.contains(la())) {
            parseMulOp();
            parseUnary();
            parseMulRest();
        } else if (FOLLOW_MUL_REST.contains(la())) {
            // ε
        } else {
            syntaxError("MulRest", union(FIRST_MUL_OP, FOLLOW_MUL_REST), sync(FOLLOW_MUL_REST));
        }
    }

    // MulOp → '*' | '/' | '%'
    private void parseMulOp() {
        if (FIRST_MUL_OP.contains(la())) {
            match(la());
        } else {
            syntaxError("MulOp", FIRST_MUL_OP, sync(FOLLOW_OPERATOR));
        }
    }

    // Unary → UnaryOp Unary | Primary
    private void parseUnary() {
        if (FIRST_UNARY_OP.contains(la())) {
            parseUnaryOp();
            parseUnary();
        } else if (FIRST_PRIMARY.contains(la())) {
            parsePrimary();
        } else {
            syntaxError("Unary", FIRST_EXPR, sync(FOLLOW_UNARY));
        }
    }

    // UnaryOp → '!' | '-'
    private void parseUnaryOp() {
        if (FIRST_UNARY_OP.contains(la())) {
            match(la());
        } else {
            syntaxError("UnaryOp", FIRST_UNARY_OP, sync(FOLLOW_OPERATOR));
        }
    }

    // Primary → id CallSuffix | Literal | '(' Expr ')'
    private void parsePrimary() {
        if (la() == IDENTIFIER) {
            match(IDENTIFIER);
            parseCallSuffix();
        } else if (LITERALS.contains(la())) {
            parseLiteral();
        } else if (la() == LPAREN) {
            match(LPAREN);
            parseExpr();
            match(RPAREN);
        } else {
            syntaxError("Primary", FIRST_PRIMARY, sync(FOLLOW_PRIMARY));
        }
    }

    // CallSuffix → '(' Args ')' | ε
    private void parseCallSuffix() {
        if (la() == LPAREN) {
            match(LPAREN);
            parseArgs();
            match(RPAREN);
        } else if (FOLLOW_CALL_SUFFIX.contains(la())) {
            // ε
        } else {
            syntaxError("CallSuffix", union(EnumSet.of(LPAREN), FOLLOW_CALL_SUFFIX), sync(FOLLOW_CALL_SUFFIX));
        }
    }

    // Literal → int_lit | double_lit | char_lit | string_lit | 'true' | 'false'
    private void parseLiteral() {
        if (LITERALS.contains(la())) {
            match(la());
        } else {
            syntaxError("Literal", LITERALS, sync(FOLLOW_LITERAL));
        }
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
     * Casa o terminal esperado. Se o lookahead não é ele:
     *  - token a mais: o seguinte é o esperado, então o atual é descartado;
     *  - token faltando: reporta e segue sem consumir nada, como se o esperado
     *    estivesse ali (o terminal sai da pilha de análise sem casar).
     * Nos dois casos nada pode entrar em loop: ou consome, ou só retorna.
     */
    private void match(TokenType expected) {
        if (current.type == expected) {
            advance();
            panicMode = false;
            return;
        }
        if (current.type != EOF && peekNext().type == expected) {
            boolean reported = report(current.line, current.column, "encontrado "
                    + TokenNames.describe(current) + " a mais antes de " + TokenNames.describe(expected));
            trace("    [match] " + cascade(reported) + "descartou " + TokenNames.describe(current)
                    + " e casou " + TokenNames.describe(expected));
            advance();
            advance();
            panicMode = false;
            return;
        }
        boolean reported = missing(expected, "");
        trace("    [match] " + cascade(reported) + TokenNames.describe(expected)
                + " ausente, segue sem consumir");
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
