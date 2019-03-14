/*
This class provides a recursive descent parser
for Corgi (the new version),
creating a parse tree which can be interpreted
to simulate execution of a Corgi program
*/

import java.util.*;
import java.io.*;

public class Parser {

  private Lexer lex;

  public Parser( Lexer lexer ) {
    lex = lexer;
  }

  public Node parseClasses() {
    System.out.println("-----> parsing <classes>:");
    Node first = parseClass();
    Token token = lex.getNextToken();
    if ( token.isKind("eof") ) {
      return new Node( "classes", first, null, null, null)
    }
    else {
      lex.putBackToken();
      Node second = parseClasses();
      return new Node( "classes", first, second, null, null);
    }
  }

  public Node parseClass() {
    System.out.println("-----> parsing <class>:");

    Token token = lex.getNextToken();
    errorCheck(token, "class");

    token = lex.getNextToken();
    errorCheck(token, "classname");
    classname = token.details;

//We left off here

    token = lex.getNextToken();
    errorCheck(token, "{");

    token = lex.getNextToken();
    errorCheck(token, "}");
    Node members = parseMembers();
    return new Node ( "class", classname, members, null, null);
  }

  public Node parseMembers() {
    System.out.println("------> parsing <members>");
  }

  public Node parseProgram() {
    System.out.println("-----> parsing <program>:");
    Node first = parseFuncCall();
    Token token = lex.getNextToken();
    if ( token.isKind("eof") ) {
      return new Node( "program", first, null, null );
    }
    else {// have a funcDef
      lex.putBackToken( token );
      Node second = parseFuncDefs();
      return new Node("program", first, second, null );
    }
  }

  public Node parseFuncDefs() {
    System.out.println("-----> parsing <funcDefs>:");

    Node first = parseFuncDef();

    // look ahead to see if there are more funcDef's
    Token token = lex.getNextToken();

    if ( token.isKind("eof") ) {
      return new Node( "funcDefs", first, null, null );
    }
    else {
      lex.putBackToken( token );
      Node second = parseFuncDefs();
      return new Node( "funcDefs", first, second, null );
    }
  }

  public Node parseFuncDef() {
    System.out.println("-----> parsing <funcDef>:");

    Token token = lex.getNextToken();
    errorCheck( token, "def" );

    Token name = lex.getNextToken();  // the function name
    errorCheck( name, "var" );

    token = lex.getNextToken();
    errorCheck( token, "single", "(" );

    token = lex.getNextToken();

    if ( token.matches("single", ")" )) {// no params

      token = lex.getNextToken();
      if ( token.isKind("end") ) {// no statements
        return new Node("funcDef", name.getDetails(), null, null, null );
      }
      else {// have a statement
        lex.putBackToken( token );
        Node second = parseStatements();
        token = lex.getNextToken();
        errorCheck( token, "end" );
        return new Node("funcDef", name.getDetails(), null, second, null );
      }
    }// no params
    else {// have params
      lex.putBackToken( token );
      Node first = parseParams();
      token = lex.getNextToken();
      errorCheck( token, "single", ")" );

      token = lex.getNextToken();

      if ( token.isKind( "end" ) ) {// no statements
        return new Node( "funcDef", name.getDetails(), first, null, null );
      }
      else {// have statements
        lex.putBackToken( token );
        Node second = parseStatements();
        token = lex.getNextToken();
        errorCheck( token, "end" );
        return new Node("funcDef", name.getDetails(), first, second, null );
      }

    }// have params

  }// parseFuncDef


  private Node parseParams() {
    System.out.println("-----> parsing <params>:");

    Token token = lex.getNextToken();
    errorCheck( token, "var" );

    Node first = new Node( "var", token.getDetails(), null, null, null );

    token = lex.getNextToken();

    if ( token.matches( "single", ")" ) ) {// no more params
      lex.putBackToken( token );  // funcCall handles the )
      return new Node( "params", first, null, null );
    }
    else if ( token.matches( "single", "," ) ) {// have more params
      Node second = parseParams();
      return new Node( "params", first, second, null );
    }
    else {// error
      System.out.println("expected , or ) and saw " + token );
      System.exit(1);
      return null;
    }

  }// <params>

  private Node parseStatements() {
    System.out.println("-----> parsing <statements>:");

    Node first = parseStatement();

    // look ahead to see if there are more statement's
    Token token = lex.getNextToken();

    if ( token.isKind("eof") ) {
      return new Node( "stmts", first, null, null );
    }
    else if ( token.isKind("end") ||
    token.isKind("else")
    ) {
      lex.putBackToken( token );
      return new Node( "stmts", first, null, null );
    }
    else {
      lex.putBackToken( token );
      Node second = parseStatements();
      return new Node( "stmts", first, second, null );
    }
  }// <statements>

  private Node parseFuncCall() {
    System.out.println("-----> parsing <funcCall>:");

    Token name = lex.getNextToken(); // function name
    errorCheck( name, "var" );

    Token token = lex.getNextToken();
    errorCheck( token, "single", "(" );

    token = lex.getNextToken();

    if ( token.matches( "single", ")" ) ) {// no args
      return new Node( "funcCall", name.getDetails(), null, null, null );
    }
    else {// have args
      lex.putBackToken( token );
      Node first = parseArgs();
      return new Node( "funcCall", name.getDetails(), first, null, null );
    }

  }// <funcCall>

  private Node parseArgs() {
    System.out.println("-----> parsing <args>:");

    Node first = parseExpr();

    Token token = lex.getNextToken();

    if ( token.matches( "single", ")" ) ) {// no more args
      return new Node( "args", first, null, null );
    }
    else if ( token.matches( "single", "," ) ) {// have more args
      Node second = parseArgs();
      return new Node( "args", first, second, null );
    }
    else {// error
      System.out.println("expected , or ) and saw " + token );
      System.exit(1);
      return null;
    }

  }// <args>

  private Node parseStatement() {
    System.out.println("-----> parsing <statement>:");

    Token token = lex.getNextToken();

    // --------------->>>  <str>
    if ( token.isKind("string") ) {
      return new Node( "str", token.getDetails(),
      null, null, null );
    }
    // --------------->>>   <var> = <expr> or funcCall
    else if ( token.isKind("var") ) {
      String varName = token.getDetails();
      token = lex.getNextToken();

      if ( token.matches("single","=") ) {// assignment
        Node first = parseExpr();
        return new Node( "sto", varName, first, null, null );
      }
      else if ( token.matches("single","(")) {// funcCall
        lex.putBackToken( token );
        lex.putBackToken( new Token("var",varName) );
        Node first = parseFuncCall();
        return first;
      }
      else {
        System.out.println("<var> must be followed by = or (, "
        + " not " + token );
        System.exit(1);
        return null;
      }
    }
    // --------------->>>   if ...
    else if ( token.isKind("if") ) {
      Node first = parseExpr();

      token = lex.getNextToken();

      if ( token.isKind( "else" ) ) {// no statements for true case
        token = lex.getNextToken();
        if ( token.isKind( "end" ) ) {// no statements for false case
          return new Node( "if", first, null, null );
        }
        else {// have statements for false case
          lex.putBackToken( token );
          Node third = parseStatements();
          token = lex.getNextToken();
          errorCheck( token, "end" );
          return new Node( "if", first, null, third );
        }
      }
      else {// have statements for true case
        lex.putBackToken( token );
        Node second = parseStatements();

        token = lex.getNextToken();
        errorCheck( token, "else" );

        token = lex.getNextToken();

        if ( token.isKind( "end" ) ) {// no statements for false case
          return new Node( "if", first, second, null );
        }
        else {// have statements for false case
          lex.putBackToken( token );
          Node third = parseStatements();
          token = lex.getNextToken();
          errorCheck( token, "end" );
          return new Node( "if", first, second, third );
        }
      }

    }// if ...

    else if ( token.isKind( "return" ) ) {
      Node first = parseExpr();
      return new Node( "return", first, null, null );
    }// return

    else {
      System.out.println("Token " + token +
      " can't begin a statement");
      System.exit(1);
      return null;
    }

  }// <statement>

  private Node parseExpr() {
    System.out.println("-----> parsing <expr>");

    Node first = parseTerm();

    // look ahead to see if there's an addop
    Token token = lex.getNextToken();

    if ( token.matches("single", "+") ||
    token.matches("single", "-")
    ) {
      Node second = parseExpr();
      return new Node( token.getDetails(), first, second, null );
    }
    else {// is just one term
      lex.putBackToken( token );
      return first;
    }

  }// <expr>

  private Node parseTerm() {
    System.out.println("-----> parsing <term>");

    Node first = parseFactor();

    // look ahead to see if there's a multop
    Token token = lex.getNextToken();

    if ( token.matches("single", "*") ||
    token.matches("single", "/")
    ) {
      Node second = parseTerm();
      return new Node( token.getDetails(), first, second, null );
    }
    else {// is just one factor
      lex.putBackToken( token );
      return first;
    }

  }// <term>

  private Node parseFactor() {
    System.out.println("-----> parsing <factor>");

    Token token = lex.getNextToken();

    if ( token.isKind("num") ) {
      return new Node("num", token.getDetails(), null, null, null );
    }
    else if ( token.isKind("var") ) {
      // could be simply a variable or could be a function call
      String name = token.getDetails();

      token = lex.getNextToken();

      if ( token.matches( "single", "(" ) ) {// is a funcCall
        lex.putBackToken( new Token( "single", "(") );  // put back the (
        lex.putBackToken( new Token( "var", name ) );  // put back name
        Node first = parseFuncCall();
        return first;
      }
      else {// is just a <var>
        lex.putBackToken( token );  // put back the non-( token
        return new Node("var", name, null, null, null );
      }
    }
    else if ( token.matches("single","(") ) {
      Node first = parseExpr();
      token = lex.getNextToken();
      errorCheck( token, "single", ")" );
      return first;
    }
    else if ( token.matches("single","-") ) {
      Node first = parseFactor();
      return new Node("opp", first, null, null );
    }
    else {
      System.out.println("Can't have a factor starting with " + token );
      System.exit(1);
      return null;
    }

  }// <factor>

  // check whether token is correct kind
  private void errorCheck( Token token, String kind ) {
    if( ! token.isKind( kind ) ) {
      System.out.println("Error:  expected " + token +
      " to be of kind " + kind );
      System.exit(1);
    }
  }

  // check whether token is correct kind and details
  private void errorCheck( Token token, String kind, String details ) {
    if( ! token.isKind( kind ) ||
    ! token.getDetails().equals( details ) ) {
      System.out.println("Error:  expected " + token +
      " to be kind= " + kind +
      " and details= " + details );
      System.exit(1);
    }
  }

}
