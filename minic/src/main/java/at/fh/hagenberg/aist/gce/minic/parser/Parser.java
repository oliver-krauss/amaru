
/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.parser;

import com.oracle.truffle.api.source.Source;
import at.fh.hagenberg.aist.gce.minic.language.MinicContext;
import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicNode;
import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicExpressionNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicBlockNode;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.PrimitiveIterator;

public class Parser {
	public static final int _EOF = 0;
	public static final int _ident = 1;
	public static final int _intCon = 2;
	public static final int _floatCon = 3;
	public static final int _charCon = 4;
	public static final int _stringCon = 5;
	public static final int _Addop = 6;
	public static final int _Mulop = 7;
	public static final int _Relop = 8;
	public static final int maxT = 30;

	static final boolean _T = true;
	static final boolean _x = false;
	static final int minErrDist = 2;

	public Token t;    // last recognized token
	public Token la;   // lookahead token
	int errDist = minErrDist;
	
	public Scanner scanner;
	public Errors errors;
	private final MinicNodeFactory factory;

	
    public Parser(MinicContext context, Source source) {
        this.scanner = new Scanner(new InputStream() {
                    PrimitiveIterator.OfInt iterator = source.getCharacters().chars().iterator();
                    @Override
                    public int read() throws IOException {
                        if (!iterator.hasNext()) {
                            return -1;
                        }
                        return iterator.next();
                    }
                });
        this.factory = new MinicNodeFactory(context, source);
        errors = new Errors();
    }

	void SynErr (int n) {
		if (errDist >= minErrDist) errors.SynErr(la.line, la.col, n);
		errDist = 0;
	}

	public void SemErr (String msg) {
		if (errDist >= minErrDist) errors.SemErr(t.line, t.col, msg);
		errDist = 0;
	}
	
	void Get () {
		for (;;) {
			t = la;
			la = scanner.Scan();
			if (la.kind <= maxT) {
				++errDist;
				break;
			}

			la = t;
		}
	}
	
	void Expect (int n) {
		if (la.kind==n) Get(); else { SynErr(n); }
	}
	
	boolean StartOf (int s) {
		return set[s][la.kind];
	}
	
	void ExpectWeak (int n, int follow) {
		if (la.kind == n) Get();
		else {
			SynErr(n);
			while (!StartOf(follow)) Get();
		}
	}
	
	boolean WeakSeparator (int n, int syFol, int repFol) {
		int kind = la.kind;
		if (kind == n) { Get(); return true; }
		else if (StartOf(repFol)) return false;
		else {
			SynErr(n);
			while (!(set[syFol][kind] || set[repFol][kind] || set[0][kind])) {
				Get();
				kind = la.kind;
			}
			return StartOf(syFol);
		}
	}
	
	void Minic() {
		Program();
	}

	void Program() {
		while (StartOf(1)) {
			if (la.kind == _ident && scanner.Peek().kind == _ident && !"(".equals(scanner.Peek().val)) {
				scanner.ResetPeek(); 
				MinicNode variable = VarDecl();
			} else if (la.kind == 1 || la.kind == 18) {
				ProcDecl();
			} else if (la.kind == 9) {
				MinicNode constant = ConstDecl();
			} else {
				MinicNode struct = StructDecl();
			}
		}
	}

	MinicNode  VarDecl() {
		MinicNode  variable;
		List<MinicNode> variables = new ArrayList<>(); 
		MinicBaseType type = Type();
		variable = VarOrArrayDecl(type);
		if (variable != null) { variables.add(variable); } 
		while (la.kind == 15) {
			Get();
			variable = VarOrArrayDecl(type);
			if (variable != null) { variables.add(variable); } 
		}
		Expect(11);
		if (variables.size() == 1) {
		     return variables.get(0);
		  } else if (variables.size() > 1) {
		     return new MinicBlockNode(variables.toArray(new MinicNode[variables.size()]));
		  }
		
		return variable;
	}

	void ProcDecl() {
		MinicBaseType type = null; 
		if (la.kind == 1) {
			type = Type();
		} else if (la.kind == 18) {
			Get();
			type = MinicBaseType.VOID; 
		} else SynErr(31);
		Expect(1);
		Token identifierToken = t; 
		factory.startFunction(identifierToken, type); 
		Expect(19);
		if (la.kind == 1) {
			FormPars();
		}
		Expect(20);
		if (la.kind == 13) {
			MinicNode body = BlockStatement(false);
			factory.finishFunction(body, type); 
		} else if (la.kind == 11) {
			Get();
			factory.finishFunctionDeclaration(type); 
		} else SynErr(32);
	}

	MinicNode  ConstDecl() {
		MinicNode  constant;
		Expect(9);
		MinicExpressionNode value = null; 
		MinicBaseType type = Type();
		Expect(1);
		Token identifierToken = t; 
		Expect(10);
		if (la.kind == 2) {
			Get();
			value = factory.createIntLiteral(t); 
		} else if (la.kind == 3) {
			Get();
			value = factory.createFloatLiteral(t); 
		} else if (la.kind == 4) {
			Get();
			value = factory.createCharLiteral(t); 
		} else if (la.kind == 5) {
			Get();
			value = factory.createStringLiteral(t); 
		} else SynErr(33);
		Expect(11);
		constant = factory.defineConstant(identifierToken, type, value); 
		return constant;
	}

	MinicNode  StructDecl() {
		MinicNode  struct;
		Expect(12);
		Token structName = null;
		List<MinicNode> structFields = null;
		List<MinicNode> variables = new ArrayList<>();
		MinicNode variable;
		
		if (true) {
			Expect(1);
			structName = t; 
			struct = factory.startStruct(structName); 
		}
		if (la.kind == 13) {
			Get();
			structFields = new ArrayList<>();
			if (struct == null) struct = factory.startStruct(null); 
			while (la.kind == 1 || la.kind == 12) {
				if (la.kind == 12) {
					variable = StructDecl();
					if (variable != null) factory.addStructField(struct, variable); 
				} else {
					variable = VarDecl();
					if (variable != null) factory.addStructField(struct, variable); 
				}
			}
			Expect(14);
		}
		factory.finishStruct(); 
		if (la.kind == 1) {
			variable = VarOrArrayDecl(struct);
			if (variable != null) { variables.add(variable); } 
			while (la.kind == 15) {
				Get();
				variable = VarOrArrayDecl(struct);
				if (variable != null) { variables.add(variable); } 
			}
		}
		Expect(11);
		if (struct == null) SemErr("struct without content");
		
		if (variables.size() == 1) {
		        return variables.get(0);
		     } else if (variables.size() > 1) {
		        return new MinicBlockNode(variables.toArray(new MinicNode[variables.size()]));
		     }
		   
		return struct;
	}

	MinicBaseType  Type() {
		MinicBaseType  type;
		Expect(1);
		Token typeToken = t; 
		type = factory.selectType(typeToken); 
		return type;
	}

	MinicNode  VarOrArrayDecl(Object type) {
		MinicNode  variable;
		Expect(1);
		Token identifierToken = t; List<MinicExpressionNode> arraySize = new ArrayList<>(); 
		while (la.kind == 16) {
			Get();
			MinicExpressionNode size = Condition();
			arraySize.add(size); 
			Expect(17);
		}
		variable = factory.defineVariable(identifierToken, type, arraySize); 
		return variable;
	}

	MinicExpressionNode  Condition() {
		MinicExpressionNode  condition;
		condition = null; 
		if (la.kind == 26) {
			Get();
			Token op = t; 
			condition = Condition();
			condition = factory.createUnary(op, condition); 
		} else if (StartOf(2)) {
			condition = CondTerm();
			while (la.kind == 27) {
				Get();
				Token op = t; 
				MinicExpressionNode right = CondTerm();
				condition = factory.createBinary(op, condition, right); 
			}
		} else SynErr(34);
		return condition;
	}

	void FormPars() {
		MinicBaseType type = Type();
		MinicNode variableCreation = VarOrArrayDecl(type);
		factory.addFormalParameter(t, type, variableCreation); 
		while (la.kind == 15) {
			Get();
			MinicBaseType nextType = Type();
			MinicNode variableCreation2 = VarOrArrayDecl(nextType);
			factory.addFormalParameter(t, nextType, variableCreation2); 
		}
	}

	MinicNode  BlockStatement(boolean inLoop) {
		MinicNode  result;
		Expect(13);
		factory.startBlock();
		List<MinicNode> body = new ArrayList<>(); 
		while (StartOf(3)) {
			MinicNode s = Statement(inLoop);
			if (s != null) { body.add(s); } 
		}
		Expect(14);
		result = factory.finishBlock(body); 
		return result;
	}

	MinicNode  Statement(boolean inLoop) {
		MinicNode  result;
		result = null; 
		if (la.kind == 9) {
			result = ConstDecl();
		} else if (la.kind == 12) {
			result = StructDecl();
		} else if (la.kind == 23) {
			MinicNode returnSt = ReturnStatement();
			result = returnSt; 
		} else if (la.kind == 13) {
			MinicNode body = BlockStatement(inLoop);
			result = body; 
		} else if (la.kind == 24) {
			MinicNode ifNode = IfStatement(inLoop);
			result = ifNode; 
		} else if (la.kind == 21) {
			MinicNode whileNode = WhileStatement();
			result = whileNode; 
		} else if (la.kind == 22) {
			MinicNode forNode = ForStatement();
			result = forNode; 
		} else if (la.kind == 11) {
			EmptyStatement();
		} else if (isAssignmentOrMethodCall()) {
			DesignatorHelper designatorHelper; 
			designatorHelper = Designator();
			if (la.kind == 10) {
				Get();
				MinicExpressionNode assignment = Condition();
				if (designatorHelper.name == null) {
				SemErr("invalid assignment target");
				} else {
				if (designatorHelper.arrayPositions == null || designatorHelper.arrayPositions.size() == 0) {
				result = factory.createAssignment(designatorHelper.name, assignment);
				} else {
				result = factory.createArrayAssignment(designatorHelper.name, assignment, designatorHelper.arrayPositions);
				}
				}
				factory.leaveStruct();
				
			} else if (la.kind == 19) {
				result = ActPars(null, t);
			} else SynErr(35);
			Expect(11);
		} else if (la.kind == 1) {
			result = VarDecl();
		} else SynErr(36);
		return result;
	}

	MinicNode  ReturnStatement() {
		MinicNode  returnNode;
		Expect(23);
		MinicExpressionNode returnStatement = Condition();
		returnNode = factory.createReturn(returnStatement); 
		Expect(11);
		return returnNode;
	}

	MinicNode  IfStatement(boolean inLoop) {
		MinicNode  ifStatement;
		Expect(24);
		Expect(19);
		MinicExpressionNode condition = Condition();
		Expect(20);
		MinicNode thenStatement = Statement(inLoop);
		MinicNode elseStatement = null; 
		if (la.kind == 25) {
			Get();
			elseStatement = Statement(inLoop);
		}
		ifStatement = factory.createIf(condition, thenStatement, elseStatement); 
		return ifStatement;
	}

	MinicNode  WhileStatement() {
		MinicNode  whileNode;
		Expect(21);
		Expect(19);
		MinicExpressionNode conditionNode = Condition();
		Expect(20);
		MinicNode bodyNode = Statement(true);
		whileNode = factory.createWhileLoop(conditionNode, bodyNode); 
		return whileNode;
	}

	MinicNode  ForStatement() {
		MinicNode  forNode;
		Expect(22);
		Expect(19);
		MinicNode initNode = Assignment();
		Expect(11);
		MinicExpressionNode conditionNode = Condition();
		Expect(11);
		MinicNode stepNode = Assignment();
		Expect(20);
		MinicNode bodyNode = Statement(true);
		forNode = factory.createForLoop(initNode, conditionNode, stepNode, bodyNode); 
		return forNode;
	}

	void EmptyStatement() {
		Expect(11);
	}

	DesignatorHelper  Designator() {
		DesignatorHelper  helper;
		helper = new DesignatorHelper(); 
		Expect(1);
		helper.name = t; 
		while (la.kind == 16 || la.kind == 29) {
			if (la.kind == 29) {
				Get();
				Expect(1);
				factory.enterStruct(helper.name);
				helper.name = t;
				helper.arrayPositions = new ArrayList<>();
				
			} else {
				Get();
				MinicExpressionNode result = Condition();
				helper.arrayPositions.add(result); 
				Expect(17);
			}
		}
		return helper;
	}

	MinicExpressionNode  ActPars(MinicExpressionNode receiver, Token assignmentName) {
		MinicExpressionNode  result;
		Expect(19);
		List<MinicExpressionNode> parameters = new ArrayList<>();
		MinicExpressionNode parameter;
		if (receiver == null) {
		   receiver = factory.findFunction(assignmentName);
		}
		
		if (StartOf(4)) {
			parameter = Condition();
			parameters.add(parameter); 
			while (la.kind == 15) {
				Get();
				parameter = Condition();
				parameters.add(parameter); 
			}
		}
		Expect(20);
		result = factory.createCall(receiver, parameters); 
		return result;
	}

	MinicNode  Assignment() {
		MinicNode  assignmentNode;
		DesignatorHelper designatorHelper;
		assignmentNode = null; 
		designatorHelper = Designator();
		Expect(10);
		MinicExpressionNode assignment = Condition();
		if (designatorHelper.name == null) {
		SemErr("invalid assignment target");
		} else {
		if (designatorHelper.arrayPositions == null || designatorHelper.arrayPositions.size() == 0) {
		assignmentNode = factory.createAssignment(designatorHelper.name, assignment);
		} else {
		assignmentNode = factory.createArrayAssignment(designatorHelper.name, assignment, designatorHelper.arrayPositions);
		}
		}
		
		return assignmentNode;
	}

	MinicExpressionNode  CondTerm() {
		MinicExpressionNode  condition;
		condition = CondFact();
		while (la.kind == 28) {
			Get();
			Token op = t; 
			MinicExpressionNode right = CondFact();
			condition = factory.createBinary(op, condition, right); 
		}
		return condition;
	}

	MinicExpressionNode  CondFact() {
		MinicExpressionNode  condition;
		condition = Expr();
		if (la.kind == 8) {
			Get();
			Token op = t; 
			MinicExpressionNode right = Expr();
			condition = factory.createBinary(op, condition, right); 
		}
		return condition;
	}

	MinicExpressionNode  Expr() {
		MinicExpressionNode  result;
		result = Term();
		while (la.kind == 6) {
			Get();
			Token op = t; 
			MinicExpressionNode right = Term();
			result = factory.createBinary(op, result, right); 
		}
		return result;
	}

	MinicExpressionNode  Term() {
		MinicExpressionNode  result;
		result = Factor();
		while (la.kind == 7) {
			Get();
			Token op = t; 
			MinicExpressionNode right = Factor();
			result = factory.createBinary(op, result, right); 
		}
		return result;
	}

	MinicExpressionNode  Factor() {
		MinicExpressionNode  result;
		result = null; DesignatorHelper designatorHelper; 
		switch (la.kind) {
		case 1: {
			Token variableName = la; 
			designatorHelper = Designator();
			if (la.kind == 19) {
				result = ActPars(null, variableName);
			}
			if (result == null) {
			    if (designatorHelper.arrayPositions == null || designatorHelper.arrayPositions.size() == 0) {
			        result = factory.createRead(designatorHelper.name);
			    } else {
			        result = factory.createArrayRead(designatorHelper.name, designatorHelper.arrayPositions);
			    }
			}
			factory.leaveStruct();
			
			break;
		}
		case 2: {
			Get();
			result = factory.createIntLiteral(t); 
			break;
		}
		case 3: {
			Get();
			result = factory.createFloatLiteral(t); 
			break;
		}
		case 4: {
			Get();
			result = factory.createCharLiteral(t); 
			break;
		}
		case 5: {
			Get();
			result = factory.createStringLiteral(t); 
			break;
		}
		case 6: {
			Get();
			Token op = t; 
			MinicExpressionNode inversion = Factor();
			result = factory.createUnary(op, inversion); 
			break;
		}
		case 19: {
			Get();
			if (isCast()) {
				MinicBaseType type = Type();
				Expect(20);
				MinicExpressionNode value = Factor();
				result = factory.cast(value, type); 
			} else if (StartOf(4)) {
				result = Condition();
				Expect(20);
			} else SynErr(37);
			break;
		}
		default: SynErr(38); break;
		}
		return result;
	}



	public void Parse() {
		la = new Token();
		la.val = "";		
		Get();
		Minic();
		Expect(0);

	}

	private static final boolean[][] set = {
		{_T,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x},
		{_x,_T,_x,_x, _x,_x,_x,_x, _x,_T,_x,_x, _T,_x,_x,_x, _x,_x,_T,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x},
		{_x,_T,_T,_T, _T,_T,_T,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_T, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x},
		{_x,_T,_x,_x, _x,_x,_x,_x, _x,_T,_x,_T, _T,_T,_x,_x, _x,_x,_x,_x, _x,_T,_T,_T, _T,_x,_x,_x, _x,_x,_x,_x},
		{_x,_T,_T,_T, _T,_T,_T,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_T, _x,_x,_x,_x, _x,_x,_T,_x, _x,_x,_x,_x}

	};

    public static void parseMinic(MinicContext context, Source source) {
        Parser parser = new Parser(context, source);
        parser.Parse();
        if (parser.errors.errors.size() > 0) {
            StringBuilder msg = new StringBuilder("Error(s) parsing script:\n");
            for (String error : parser.errors.errors) {
                msg.append(error).append("\n");
            }
            throw new RuntimeException(msg.toString());
        }
    }

    private boolean isAssignmentOrMethodCall() {
    	scanner.ResetPeek();
        Token token = la;
        boolean assignmentOrCall = false;
		boolean inArray = false;
        while (!token.val.equals(";")) {
			if (token.val.equals("[")) {
				inArray = true;
			} else if (token.val.equals("]")) {
				inArray = false;
			} else if (!inArray && (token.val.equals("(") || token.val.equals("="))) {
                return true;
            }
            token = scanner.Peek();
        }
        return assignmentOrCall;
    }

    private boolean isCast() {
        scanner.ResetPeek();
        Token castEnd = scanner.Peek();
        Token fact = scanner.Peek();
        if (la.kind == _ident && castEnd.val.equals(")") && fact.kind != _Mulop) {
            return true;
        }
        return false;
    }

    // Helper Class for Designator part of ATG
    private class DesignatorHelper {
        public Token name;
        public List<MinicExpressionNode> arrayPositions;

        public DesignatorHelper() {
            arrayPositions = new ArrayList<>();
        }
    }
} // end Parser


class Errors {

    protected final List<String> errors = new ArrayList<>();
	public int count = 0;                                    // number of errors detected
	public java.io.PrintStream errorStream = System.out;     // error messages go to this stream
	public String errMsgFormat = "-- line {0} col {1}: {2}"; // 0=line, 1=column, 2=text
	
	protected void printMsg(int line, int column, String msg) {
		StringBuffer b = new StringBuffer(errMsgFormat);
		int pos = b.indexOf("{0}");
		if (pos >= 0) { b.delete(pos, pos+3); b.insert(pos, line); }
		pos = b.indexOf("{1}");
		if (pos >= 0) { b.delete(pos, pos+3); b.insert(pos, column); }
		pos = b.indexOf("{2}");
		if (pos >= 0) b.replace(pos, pos+3, msg);
		errorStream.println(b.toString());
	}
	
	public void SynErr (int line, int col, int n) {
		String s;
		switch (n) {
			case 0: s = "EOF expected"; break;
			case 1: s = "ident expected"; break;
			case 2: s = "intCon expected"; break;
			case 3: s = "floatCon expected"; break;
			case 4: s = "charCon expected"; break;
			case 5: s = "stringCon expected"; break;
			case 6: s = "Addop expected"; break;
			case 7: s = "Mulop expected"; break;
			case 8: s = "Relop expected"; break;
			case 9: s = "\"const\" expected"; break;
			case 10: s = "\"=\" expected"; break;
			case 11: s = "\";\" expected"; break;
			case 12: s = "\"struct\" expected"; break;
			case 13: s = "\"{\" expected"; break;
			case 14: s = "\"}\" expected"; break;
			case 15: s = "\",\" expected"; break;
			case 16: s = "\"[\" expected"; break;
			case 17: s = "\"]\" expected"; break;
			case 18: s = "\"void\" expected"; break;
			case 19: s = "\"(\" expected"; break;
			case 20: s = "\")\" expected"; break;
			case 21: s = "\"while\" expected"; break;
			case 22: s = "\"for\" expected"; break;
			case 23: s = "\"return\" expected"; break;
			case 24: s = "\"if\" expected"; break;
			case 25: s = "\"else\" expected"; break;
			case 26: s = "\"!\" expected"; break;
			case 27: s = "\"||\" expected"; break;
			case 28: s = "\"&&\" expected"; break;
			case 29: s = "\".\" expected"; break;
			case 30: s = "??? expected"; break;
			case 31: s = "invalid ProcDecl"; break;
			case 32: s = "invalid ProcDecl"; break;
			case 33: s = "invalid ConstDecl"; break;
			case 34: s = "invalid Condition"; break;
			case 35: s = "invalid Statement"; break;
			case 36: s = "invalid Statement"; break;
			case 37: s = "invalid Factor"; break;
			case 38: s = "invalid Factor"; break;
			default: s = "error " + n; break;
		}
		printMsg(line, col, s);
		count++;
	}

	public void SemErr (int line, int col, String s) {	
		printMsg(line, col, s);
		count++;
	}
	
	public void SemErr (String s) {
		errorStream.println(s);
		count++;
	}
	
	public void Warning (int line, int col, String s) {	
		printMsg(line, col, s);
	}
	
	public void Warning (String s) {
		errorStream.println(s);
	}
} // Errors


class FatalError extends RuntimeException {
	public static final long serialVersionUID = 1L;
	public FatalError(String s) { super(s); }
}
