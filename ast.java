import java.io.*;
import java.util.*;

// **********************************************************************
// The ASTnode class defines the nodes of the abstract-syntax tree that
// represents a Wumbo program.
//
// Internal nodes of the tree contain pointers to children, organized
// either in a list (for nodes that may have a variable number of
// children) or as a fixed set of fields.
//
// The nodes for literals and ids contain line and character number
// information; for string literals and identifiers, they also contain a
// string; for integer literals, they also contain an integer value.
//
// Here are all the different kinds of AST nodes and what kinds of children
// they have.  All of these kinds of AST nodes are subclasses of "ASTnode".
// Indentation indicates further subclassing:
//
//     Subclass            Children
//     --------            ----
//     ProgramNode         DeclListNode
//     DeclListNode        linked list of DeclNode
//     DeclNode:
//       VarDeclNode       TypeNode, IdNode, int
//       FnDeclNode        TypeNode, IdNode, FormalsListNode, FnBodyNode
//       FormalDeclNode    TypeNode, IdNode
//       StructDeclNode    IdNode, DeclListNode
//
//     FormalsListNode     linked list of FormalDeclNode
//     FnBodyNode          DeclListNode, StmtListNode
//     StmtListNode        linked list of StmtNode
//     ExpListNode         linked list of ExpNode
//
//     TypeNode:
//       IntNode           -- none --
//       BoolNode          -- none --
//       VoidNode          -- none --
//       StructNode        IdNode
//
//     StmtNode:
//       AssignStmtNode      AssignNode
//       PostIncStmtNode     ExpNode
//       PostDecStmtNode     ExpNode
//       ReadStmtNode        ExpNode
//       WriteStmtNode       ExpNode
//       IfStmtNode          ExpNode, DeclListNode, StmtListNode
//       IfElseStmtNode      ExpNode, DeclListNode, StmtListNode,
//                                    DeclListNode, StmtListNode
//       WhileStmtNode       ExpNode, DeclListNode, StmtListNode
//       RepeatStmtNode      ExpNode, DeclListNode, StmtListNode
//       CallStmtNode        CallExpNode
//       ReturnStmtNode      ExpNode
//
//     ExpNode:
//       IntLitNode          -- none --
//       StrLitNode          -- none --
//       TrueNode            -- none --
//       FalseNode           -- none --
//       IdNode              -- none --
//       DotAccessNode       ExpNode, IdNode
//       AssignNode          ExpNode, ExpNode
//       CallExpNode         IdNode, ExpListNode
//       UnaryExpNode        ExpNode
//         UnaryMinusNode
//         NotNode
//       BinaryExpNode       ExpNode ExpNode
//         PlusNode
//         MinusNode
//         TimesNode
//         DivideNode
//         AndNode
//         OrNode
//         EqualsNode
//         NotEqualsNode
//         LessNode
//         GreaterNode
//         LessEqNode
//         GreaterEqNode
//
// Here are the different kinds of AST nodes again, organized according to
// whether they are leaves, internal nodes with linked lists of children, or
// internal nodes with a fixed number of children:
//
// (1) Leaf nodes:
//        IntNode,   BoolNode,  VoidNode,  IntLitNode,  StrLitNode,
//        TrueNode,  FalseNode, IdNode
//
// (2) Internal nodes with (possibly empty) linked lists of children:
//        DeclListNode, FormalsListNode, StmtListNode, ExpListNode
//
// (3) Internal nodes with fixed numbers of children:
//        ProgramNode,     VarDeclNode,     FnDeclNode,     FormalDeclNode,
//        StructDeclNode,  FnBodyNode,      StructNode,     AssignStmtNode,
//        PostIncStmtNode, PostDecStmtNode, ReadStmtNode,   WriteStmtNode
//        IfStmtNode,      IfElseStmtNode,  WhileStmtNode,  RepeatStmtNode,
//        CallStmtNode
//        ReturnStmtNode,  DotAccessNode,   AssignExpNode,  CallExpNode,
//        UnaryExpNode,    BinaryExpNode,   UnaryMinusNode, NotNode,
//        PlusNode,        MinusNode,       TimesNode,      DivideNode,
//        AndNode,         OrNode,          EqualsNode,     NotEqualsNode,
//        LessNode,        GreaterNode,     LessEqNode,     GreaterEqNode
//
// **********************************************************************

// **********************************************************************
// ASTnode class (base class for all other kinds of nodes)
// **********************************************************************

abstract class ASTnode {
    // every subclass must provide an unparse operation
    abstract public void unparse(PrintWriter p, int indent);

    // this method can be used by the unparse methods to do indenting
    protected void addIndentation(PrintWriter p, int indent) {
        for (int k = 0; k < indent; k++)
            p.print(" ");
    }
}

// **********************************************************************
// ProgramNode, DeclListNode, FormalsListNode, FnBodyNode,
// StmtListNode, ExpListNode
// **********************************************************************

class ProgramNode extends ASTnode {
    public ProgramNode(DeclListNode L) {
        myDeclList = L;
    }

    public void unparse(PrintWriter p, int indent) {
        myDeclList.unparse(p, indent);
    }

    public void nameAnalyze() {
        s = new SymTable();
        myDeclList.nameAnalyze(s);
    }

    private DeclListNode myDeclList;
    private SymTable s;
}

class DeclListNode extends ASTnode {
    public DeclListNode(List<DeclNode> S) {
        myDecls = S;
    }

    public void unparse(PrintWriter p, int indent) {
        Iterator it = myDecls.iterator();
        try {
            while (it.hasNext()) {
                ((DeclNode) it.next()).unparse(p, indent);
            }
        } catch (NoSuchElementException ex) {
            System.err.println("unexpected NoSuchElementException in DeclListNode.print");
            System.exit(-1);
        }
    }

    public void nameAnalyze(SymTable st, SymTable s) {
        try {
            for (int i = 0; i < myDecls.size(); i++)
                ((VarDeclNode) myDecls.get(i)).nameAnalyze(st, s);
        } catch (NoSuchElementException e) {
            e.printStackTrace();
        }

    }

    public void nameAnalyze(SymTable st) {
        try {
            for (int i = 0; i < myDecls.size(); i++)
                ((DeclNode) myDecls.get(i)).nameAnalyze(st);
        } catch (NoSuchElementException e) {
            e.printStackTrace();
        }

    }

    private List<DeclNode> myDecls;
}

class FormalsListNode extends ASTnode {
    public FormalsListNode(List<FormalDeclNode> S) {
        myFormals = S;
    }

    public void unparse(PrintWriter p, int indent) {
        Iterator<FormalDeclNode> it = myFormals.iterator();
        if (it.hasNext()) { // if there is at least one element
            it.next().unparse(p, indent);
            while (it.hasNext()) { // print the rest of the list
                p.print(", ");
                it.next().unparse(p, indent);
            }
        }
    }

    public void nameAnalyze(FunctionSym fs, SymTable st) {
        try {
            for (int i = 0; i < myFormals.size(); i++)
                ((FormalDeclNode) myFormals.get(i)).nameAnalyze(fs, st);
        } catch (NoSuchElementException e) {
            e.printStackTrace();
        }
    }

    private List<FormalDeclNode> myFormals;
}

class FnBodyNode extends ASTnode {
    public FnBodyNode(DeclListNode declList, StmtListNode stmtList) {
        myDeclList = declList;
        myStmtList = stmtList;
    }

    public void unparse(PrintWriter p, int indent) {
        myDeclList.unparse(p, indent);
        myStmtList.unparse(p, indent);
    }

    public void nameAnalyze(SymTable st) {
        myDeclList.nameAnalyze(st);
        myStmtList.nameAnalyze(st);
    }

    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}

class StmtListNode extends ASTnode {
    public StmtListNode(List<StmtNode> S) {
        myStmts = S;
    }

    public void unparse(PrintWriter p, int indent) {
        Iterator<StmtNode> it = myStmts.iterator();
        while (it.hasNext()) {
            it.next().unparse(p, indent);
        }
    }

    public void nameAnalyze(SymTable s) {
        try {
            for (int i = 0; i < myStmts.size(); i++)
                ((StmtNode) myStmts.get(i)).nameAnalyze(s);
        } catch (NoSuchElementException e) {
            e.printStackTrace();
        }
    }

    private List<StmtNode> myStmts;
}

class ExpListNode extends ASTnode {
    public ExpListNode(List<ExpNode> S) {
        myExps = S;
    }

    public void unparse(PrintWriter p, int indent) {
        Iterator<ExpNode> it = myExps.iterator();
        if (it.hasNext()) { // if there is at least one element
            it.next().unparse(p, indent);
            while (it.hasNext()) { // print the rest of the list
                p.print(", ");
                it.next().unparse(p, indent);
            }
        }
    }

    public void nameAnalyze(SymTable st) {
        try {
            for (int i = 0; i < myExps.size(); i++)
                ((ExpNode) myExps.get(i)).nameAnalyze(st);
        } catch (NoSuchElementException e) {
            e.printStackTrace();
        }
    }

    private List<ExpNode> myExps;
}

// **********************************************************************
// DeclNode and its subclasses
// **********************************************************************

abstract class DeclNode extends ASTnode {
    abstract public void nameAnalyze(SymTable st);
}

class VarDeclNode extends DeclNode {
    public VarDeclNode(TypeNode type, IdNode id, int size) {
        myType = type;
        myId = id;
        mySize = size;
    }

    public void unparse(PrintWriter p, int indent) {
        addIndentation(p, indent);
        myType.unparse(p, 0);
        p.print(" ");
        myId.unparse(p, 0);
        p.println(";");
    }

    public void nameAnalyze(SymTable st) {
        this.nameAnalyze(st, st);
    }

    public void nameAnalyze(SymTable st, SymTable s) {
        if (myType.getSym().getType().equals("void")) {
            ErrMsg.fatal(myId.getLineNum(), myId.getCharNum(), "Non-function declared void");
            return;
        }

        if (myType.getSym().getType().equals("struct")) {
            try {
                if (s.lookupGlobal(((StructNode) myType).getId()) == null) {
                    ErrMsg.fatal(myId.getLineNum(), myId.getCharNum(), "Undeclared identifier");
                    return;
                }
                if (s.lookupGlobal(((StructNode) myType).getId()) != null && !(s.lookupGlobal(((StructNode) myType).getId()).getType().equals("struct"))) {
                    ErrMsg.fatal(myId.getLineNum(), myId.getCharNum(), "Invalid name of struct type");
                    return;
                }
            } catch (EmptySymTableException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            StructDefSym sdefs;
            try {
                sdefs = (StructDefSym) (s.lookupGlobal(((StructNode) myType).getId()));
            } catch (EmptySymTableException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            try {
                sdefs = (StructDefSym) (s.lookupGlobal(((StructNode) myType).getId()));
                StructDeclSym sdecls = new StructDeclSym(sdefs, ((StructNode) myType).getId());
                st.addDecl(myId.getStrVal(), sdecls);
            } catch (DuplicateSymException e) {
                ErrMsg.fatal(myId.getLineNum(), myId.getCharNum(), "Multiply declared identifier");
            } catch (EmptySymTableException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            return;
        }

        try {
            st.addDecl(myId.getStrVal(), myType.getSym());
        } catch (DuplicateSymException e) {
            ErrMsg.fatal(myId.getLineNum(), myId.getCharNum(), "Multiply declared identifier");
        } catch (EmptySymTableException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    private TypeNode myType;
    private IdNode myId;
    private int mySize; // use value NOT_STRUCT if this is not a struct type

    public static int NOT_STRUCT = -1;
}

class FnDeclNode extends DeclNode {
    public FnDeclNode(TypeNode type, IdNode id, FormalsListNode formalList, FnBodyNode body) {
        myType = type;
        myId = id;
        myFormalsList = formalList;
        myBody = body;
    }

    public void unparse(PrintWriter p, int indent) {
        addIndentation(p, indent);
        myType.unparse(p, 0);
        p.print(" ");
        myId.unparse(p, 0);
        p.print("(");
        myFormalsList.unparse(p, 0);
        p.println(") {");
        myBody.unparse(p, indent + 4);
        p.println("}\n");
    }

    public void nameAnalyze(SymTable s) {
        FunctionSym curr = new FunctionSym("fn", myType.getSym());
        try {
            s.addDecl(myId.getStrVal(), curr);
        } catch (DuplicateSymException e) {
            ErrMsg.fatal(myId.getLineNum(), myId.getCharNum(), "Multiply declared identifier");
        } catch (EmptySymTableException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        s.addScope();
        myFormalsList.nameAnalyze(curr, s);
        myBody.nameAnalyze(s);

        try {
            s.removeScope();
        } catch (EmptySymTableException e) {
            e.printStackTrace();
        }
    }

    private TypeNode myType;
    private IdNode myId;
    private FormalsListNode myFormalsList;
    private FnBodyNode myBody;
}

class FormalDeclNode extends DeclNode {
    public FormalDeclNode(TypeNode type, IdNode id) {
        myType = type;
        myId = id;
    }

    public void unparse(PrintWriter p, int indent) {
        myType.unparse(p, 0);
        p.print(" ");
        myId.unparse(p, 0);
    }

    public void nameAnalyze(SymTable s) {
    }

    public void nameAnalyze(FunctionSym fn, SymTable s) {
        if (myType.getSym().getType().equals("void")) {
            ErrMsg.fatal(myId.getLineNum(), myId.getCharNum(), "Non-function declared void");
            return;
        }

        try {
            s.addDecl(myId.getStrVal(), myType.getSym());
            fn.addFormal(myType.getSym().toString());
        } catch (DuplicateSymException e) {
            ErrMsg.fatal(myId.getLineNum(), myId.getCharNum(), "Multiply declared identifier");
        } catch (EmptySymTableException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

    }

    private TypeNode myType;
    private IdNode myId;
}

class StructDeclNode extends DeclNode {
    public StructDeclNode(IdNode id, DeclListNode declList) {
        myId = id;
        myDeclList = declList;
    }

    public void unparse(PrintWriter p, int indent) {
        addIndentation(p, indent);
        p.print("struct ");
        myId.unparse(p, 0);
        p.println("{");
        myDeclList.unparse(p, indent + 4);
        addIndentation(p, indent);
        p.println("};\n");

    }

    public void nameAnalyze(SymTable st) {
        SymTable structst = new SymTable();
        myDeclList.nameAnalyze(structst, st);
        try {
            st.addDecl(myId.getStrVal(), new StructDefSym(structst, "struct"));
        } catch (DuplicateSymException e) {
            ErrMsg.fatal(myId.getLineNum(), myId.getCharNum(), "Multiply declared identifier");
        } catch (EmptySymTableException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

    }

    private IdNode myId;
    private DeclListNode myDeclList;
}

// **********************************************************************
// TypeNode and its Subclasses
// **********************************************************************

abstract class TypeNode extends ASTnode {
    abstract public Sym getSym();
}

class IntNode extends TypeNode {
    public IntNode() {
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("int");
    }

    public Sym getSym() {
        return new Sym("int");
    }

}

class BoolNode extends TypeNode {
    public BoolNode() {
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("bool");
    }

    public Sym getSym() {
        return new Sym("bool");
    }
}

class VoidNode extends TypeNode {
    public VoidNode() {
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("void");
    }

    public Sym getSym() {
        return new Sym("void");
    }
}

class StructNode extends TypeNode {
    public StructNode(IdNode id) {
        myId = id;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("struct ");
        myId.unparse(p, 0);
    }

    public Sym getSym() {
        return new Sym("struct");
    }

    public String getId() {
        return myId.getStrVal();
    }

    private IdNode myId;
}

// **********************************************************************
// StmtNode and its subclasses
// **********************************************************************

abstract class StmtNode extends ASTnode {
    abstract public void nameAnalyze(SymTable st);
}

class AssignStmtNode extends StmtNode {
    public AssignStmtNode(AssignNode assign) {
        myAssign = assign;
    }

    public void unparse(PrintWriter p, int indent) {
        addIndentation(p, indent);
        myAssign.unparse(p, -1); // no parentheses
        p.println(";");
    }

    public void nameAnalyze(SymTable st) {
        myAssign.nameAnalyze(st);
    }

    private AssignNode myAssign;
}

class PostIncStmtNode extends StmtNode {
    public PostIncStmtNode(ExpNode exp) {
        myExp = exp;
    }

    public void unparse(PrintWriter p, int indent) {
        addIndentation(p, indent);
        myExp.unparse(p, 0);
        p.println("++;");
    }

    public void nameAnalyze(SymTable st) {
        myExp.nameAnalyze(st);
    }

    private ExpNode myExp;
}

class PostDecStmtNode extends StmtNode {
    public PostDecStmtNode(ExpNode exp) {
        myExp = exp;
    }

    public void unparse(PrintWriter p, int indent) {
        addIndentation(p, indent);
        myExp.unparse(p, 0);
        p.println("--;");
    }

    public void nameAnalyze(SymTable st) {
        myExp.nameAnalyze(st);
    }

    private ExpNode myExp;
}

class ReadStmtNode extends StmtNode {
    public ReadStmtNode(ExpNode exp) {
        myExp = exp;
    }

    public void unparse(PrintWriter p, int indent) {
        addIndentation(p, indent);
        p.print("cin >> ");
        myExp.unparse(p, 0);
        p.println(";");
    }

    public void nameAnalyze(SymTable st) {
        myExp.nameAnalyze(st);
    }

    // 1 kid (actually can only be an IdNode or an ArrayExpNode)
    private ExpNode myExp;
}

class WriteStmtNode extends StmtNode {
    public WriteStmtNode(ExpNode exp) {
        myExp = exp;
    }

    public void unparse(PrintWriter p, int indent) {
        addIndentation(p, indent);
        p.print("cout << ");
        myExp.unparse(p, 0);
        p.println(";");
    }

    public void nameAnalyze(SymTable st) {
        myExp.nameAnalyze(st);
    }

    private ExpNode myExp;
}

class IfStmtNode extends StmtNode {
    public IfStmtNode(ExpNode exp, DeclListNode dlist, StmtListNode slist) {
        myDeclList = dlist;
        myExp = exp;
        myStmtList = slist;
    }

    public void unparse(PrintWriter p, int indent) {
        addIndentation(p, indent);
        p.print("if (");
        myExp.unparse(p, 0);
        p.println(") {");
        myDeclList.unparse(p, indent + 4);
        myStmtList.unparse(p, indent + 4);
        addIndentation(p, indent);
        p.println("}");
    }

    public void nameAnalyze(SymTable st) {
        myExp.nameAnalyze(st);
        st.addScope();
        myDeclList.nameAnalyze(st);
        myStmtList.nameAnalyze(st);

        try {
            st.removeScope();
        } catch (EmptySymTableException e) {
            e.printStackTrace();
        }
    }

    private ExpNode myExp;
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}

class IfElseStmtNode extends StmtNode {
    public IfElseStmtNode(ExpNode exp, DeclListNode dlist1, StmtListNode slist1, DeclListNode dlist2,
            StmtListNode slist2) {
        myExp = exp;
        myThenDeclList = dlist1;
        myThenStmtList = slist1;
        myElseDeclList = dlist2;
        myElseStmtList = slist2;
    }

    public void unparse(PrintWriter p, int indent) {
        addIndentation(p, indent);
        p.print("if (");
        myExp.unparse(p, 0);
        p.println(") {");
        myThenDeclList.unparse(p, indent + 4);
        myThenStmtList.unparse(p, indent + 4);
        addIndentation(p, indent);
        p.println("}");
        addIndentation(p, indent);
        p.println("else {");
        myElseDeclList.unparse(p, indent + 4);
        myElseStmtList.unparse(p, indent + 4);
        addIndentation(p, indent);
        p.println("}");
    }

    public void nameAnalyze(SymTable st) {
        myExp.nameAnalyze(st);
        st.addScope();
        myThenDeclList.nameAnalyze(st);
        myThenStmtList.nameAnalyze(st);

        try {
            st.removeScope();
        } catch (EmptySymTableException e) {
            e.printStackTrace();
        }

        st.addScope();
        myElseDeclList.nameAnalyze(st);
        myElseStmtList.nameAnalyze(st);

        try {
            st.removeScope();
        } catch (EmptySymTableException e) {
            e.printStackTrace();
        }
    }

    private ExpNode myExp;
    private DeclListNode myThenDeclList;
    private StmtListNode myThenStmtList;
    private StmtListNode myElseStmtList;
    private DeclListNode myElseDeclList;
}

class WhileStmtNode extends StmtNode {
    public WhileStmtNode(ExpNode exp, DeclListNode dlist, StmtListNode slist) {
        myExp = exp;
        myDeclList = dlist;
        myStmtList = slist;
    }

    public void unparse(PrintWriter p, int indent) {
        addIndentation(p, indent);
        p.print("while (");
        myExp.unparse(p, 0);
        p.println(") {");
        myDeclList.unparse(p, indent + 4);
        myStmtList.unparse(p, indent + 4);
        addIndentation(p, indent);
        p.println("}");
    }

    public void nameAnalyze(SymTable st) {
        myExp.nameAnalyze(st);
        st.addScope();
        myDeclList.nameAnalyze(st);
        myStmtList.nameAnalyze(st);

        try {
            st.removeScope();
        } catch (EmptySymTableException e) {
            e.printStackTrace();
        }
    }

    private ExpNode myExp;
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}

class RepeatStmtNode extends StmtNode {
    public RepeatStmtNode(ExpNode exp, DeclListNode dlist, StmtListNode slist) {
        myExp = exp;
        myDeclList = dlist;
        myStmtList = slist;
    }

    public void unparse(PrintWriter p, int indent) {
        addIndentation(p, indent);
        p.print("repeat (");
        myExp.unparse(p, 0);
        p.println(") {");
        myDeclList.unparse(p, indent + 4);
        myStmtList.unparse(p, indent + 4);
        addIndentation(p, indent);
        p.println("}");
    }

    public void nameAnalyze(SymTable st) {
        myExp.nameAnalyze(st);
        st.addScope();
        myDeclList.nameAnalyze(st);
        myStmtList.nameAnalyze(st);

        try {
            st.removeScope();
        } catch (EmptySymTableException e) {
            e.printStackTrace();
        }
    }

    private ExpNode myExp;
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}

class CallStmtNode extends StmtNode {
    public CallStmtNode(CallExpNode call) {
        myCall = call;
    }

    public void unparse(PrintWriter p, int indent) {
        addIndentation(p, indent);
        myCall.unparse(p, indent);
        p.println(";");
    }

    public void nameAnalyze(SymTable st) {
        myCall.nameAnalyze(st);
    }

    private CallExpNode myCall;
}

class ReturnStmtNode extends StmtNode {
    public ReturnStmtNode(ExpNode exp) {
        myExp = exp;
    }

    public void unparse(PrintWriter p, int indent) {
        addIndentation(p, indent);
        p.print("return");
        if (myExp != null) {
            p.print(" ");
            myExp.unparse(p, 0);
        }
        p.println(";");
    }

    public void nameAnalyze(SymTable st) {
        if (myExp != null)
            myExp.nameAnalyze(st);
    }

    private ExpNode myExp; // possibly null
}

// **********************************************************************
// ExpNode and its subclasses
// **********************************************************************

abstract class ExpNode extends ASTnode {
    abstract public void nameAnalyze(SymTable st);
}

class IntLitNode extends ExpNode {
    public IntLitNode(int lineNum, int charNum, int intVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myIntVal = intVal;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print(myIntVal);
    }

    public void nameAnalyze(SymTable st) {
    }

    private int myLineNum;
    private int myCharNum;
    private int myIntVal;
}

class StringLitNode extends ExpNode {
    public StringLitNode(int lineNum, int charNum, String strVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myStrVal = strVal;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print(myStrVal);
    }

    public void nameAnalyze(SymTable st) {
    }

    private int myLineNum;
    private int myCharNum;
    private String myStrVal;
}

class TrueNode extends ExpNode {
    public TrueNode(int lineNum, int charNum) {
        myLineNum = lineNum;
        myCharNum = charNum;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("true");
    }

    public void nameAnalyze(SymTable st) {
    }

    private int myLineNum;
    private int myCharNum;
}

class FalseNode extends ExpNode {
    public FalseNode(int lineNum, int charNum) {
        myLineNum = lineNum;
        myCharNum = charNum;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("false");
    }

    public void nameAnalyze(SymTable st) {
    }

    private int myLineNum;
    private int myCharNum;
}

class IdNode extends ExpNode {
    public IdNode(int lineNum, int charNum, String strVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myStrVal = strVal;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print(myStrVal);
        if (this.s != null) {
            p.print("(");
            p.print(s.toString());
            p.print(")");
        }
    }

    public Sym getSym() {
        return this.s;
    }

    public String getStrVal() {
        return myStrVal;
    }

    public int getCharNum() {
        return myCharNum;
    }

    public int getLineNum() {
        return myLineNum;
    }

    public void nameAnalyze(SymTable st) {
        try {
            s = st.lookupLocal(myStrVal);
            if(s != null)
                return;
            s = st.lookupGlobal(myStrVal);
            if(s == null)
                ErrMsg.fatal(myLineNum, myCharNum, "Undeclared identifier");
        } catch (EmptySymTableException e) {
            e.printStackTrace();
        }
    }

    public void setForDot(Sym k) {
        s = k;
    }

    private int myLineNum;
    private int myCharNum;
    private String myStrVal;
    private Sym s;
}

class DotAccessExpNode extends ExpNode {
    public DotAccessExpNode(ExpNode loc, IdNode id) {
        myLoc = loc;
        myId = id;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myLoc.unparse(p, 0);
        p.print(").");
        myId.unparse(p, 0);
    }

    public void nameAnalyze(SymTable st) {
        badAccess = false;
        myLoc.nameAnalyze(st);

        if (myLoc instanceof IdNode) {
            if (((IdNode) myLoc).getSym() == null) {
                ErrMsg.fatal(((IdNode) myId).getLineNum(), ((IdNode) myId).getCharNum(), "Dot-access of non-struct type");
                badAccess = true;
                return;
            } else {
                if (!(((IdNode) myLoc).getSym() instanceof StructDeclSym)) {
                    ErrMsg.fatal(((IdNode) myId).getLineNum(), ((IdNode) myId).getCharNum(),
                            "Dot-access of non-struct type");
                    badAccess = true;
                    return;
                }

                StructDefSym sdsym = ((StructDeclSym) ((IdNode) myLoc).getSym()).getStructDefSym();
                SymTable structTable = sdsym.getStructSymTable();
                try {
                    if (structTable.lookupLocal(myId.getStrVal()) == null) {
                        ErrMsg.fatal(myId.getLineNum(), myId.getCharNum(), "Invalid struct field name");
                        badAccess = true;
                        return;
                    }

                    if (structTable.lookupLocal(myId.getStrVal()) instanceof Sym)
                        myId.setForDot(structTable.lookupLocal(myId.getStrVal()));

                    if (structTable.lookupLocal(myId.getStrVal()) instanceof StructDeclSym) {
                        this.prev = (StructDeclSym) structTable.lookupLocal(myId.getStrVal());
                        myId.setForDot((StructDeclSym) structTable.lookupLocal(myId.getStrVal()));
                    }
                } catch (EmptySymTableException e) {
                    e.printStackTrace();
                }
            }
        }

        if (myLoc instanceof DotAccessExpNode) {
            if (((DotAccessExpNode) myLoc).badAccess || ((DotAccessExpNode) myLoc).prev == null) {
                badAccess = true;
                return;
            }
            StructDefSym sdsym = (((DotAccessExpNode) myLoc).prev).getStructDefSym();
            SymTable structTable = sdsym.getStructSymTable();
            try {
                if (structTable.lookupLocal(myId.getStrVal()) == null) {
                    ErrMsg.fatal(myId.getLineNum(), myId.getCharNum(), "Invalid struct field name");
                    badAccess = true;
                    return;
                }
                if(structTable.lookupLocal(myId.getStrVal()) instanceof Sym)
                    myId.setForDot(structTable.lookupLocal(myId.getStrVal()));
    
                if(structTable.lookupLocal(myId.getStrVal()) instanceof StructDeclSym) {
                    this.prev = (StructDeclSym)structTable.lookupLocal(myId.getStrVal());
                    myId.setForDot((StructDeclSym)structTable.lookupLocal(myId.getStrVal()));
                }
            } catch (EmptySymTableException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean badAccess;
    private StructDeclSym prev;  
    private ExpNode myLoc;
    private IdNode myId;
}

class AssignNode extends ExpNode {
    public AssignNode(ExpNode lhs, ExpNode exp) {
        myLhs = lhs;
        myExp = exp;
    }

    public void unparse(PrintWriter p, int indent) {
        if (indent != -1)  p.print("(");
        myLhs.unparse(p, 0);
        p.print(" = ");
        myExp.unparse(p, 0);
        if (indent != -1)  p.print(")");
    }

    public void nameAnalyze(SymTable st) {
        myLhs.nameAnalyze(st);
        myExp.nameAnalyze(st);
    }
    
    private ExpNode myLhs;
    private ExpNode myExp;
}

class CallExpNode extends ExpNode {
    public CallExpNode(IdNode name, ExpListNode elist) {
        myId = name;
        myExpList = elist;
    }

    public CallExpNode(IdNode name) {
        myId = name;
        myExpList = new ExpListNode(new LinkedList<ExpNode>());
    }

    public void unparse(PrintWriter p, int indent) {
        myId.unparse(p, 0);
        p.print("(");
        if (myExpList != null) {
            myExpList.unparse(p, 0);
        }
        p.print(")");
    }

    public void nameAnalyze(SymTable st) {
        myId.nameAnalyze(st);
        if(myExpList != null)
            myExpList.nameAnalyze(st);
    }
    
    private IdNode myId;
    private ExpListNode myExpList;  // possibly null
}

abstract class UnaryExpNode extends ExpNode {
    public UnaryExpNode(ExpNode exp) {
        myExp = exp;
    }

    public void nameAnalyze(SymTable st) {
        myExp.nameAnalyze(st);
    }
    
    protected ExpNode myExp;
}

abstract class BinaryExpNode extends ExpNode {
    public BinaryExpNode(ExpNode exp1, ExpNode exp2) {
        this.myExp1 = exp1;
        this.myExp2 = exp2;
    }

    public void nameAnalyze(SymTable st) {
        myExp1.nameAnalyze(st);
        myExp2.nameAnalyze(st);
    }

    protected ExpNode myExp1;
    protected ExpNode myExp2;
}

// **********************************************************************
// Subclasses of UnaryExpNode
// **********************************************************************

class UnaryMinusNode extends UnaryExpNode {
    public UnaryMinusNode(ExpNode exp) {
        super(exp);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(-");
        myExp.unparse(p, 0);
        p.print(")");
    }
    public void nameAnalyze(SymTable st) {
        myExp.nameAnalyze(st);
    }
}

class NotNode extends UnaryExpNode {
    public NotNode(ExpNode exp) {
        super(exp);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(!");
        myExp.unparse(p, 0);
        p.print(")");
    }
    public void nameAnalyze(SymTable st) {
        myExp.nameAnalyze(st);
    }
}

// **********************************************************************
// Subclasses of BinaryExpNode
// **********************************************************************

class PlusNode extends BinaryExpNode {
    public PlusNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" + ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
    public void nameAnalyze(SymTable st) {
        myExp1.nameAnalyze(st);
        myExp2.nameAnalyze(st);
    }
}

class MinusNode extends BinaryExpNode {
    public MinusNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" - ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
    public void nameAnalyze(SymTable st) {
        myExp1.nameAnalyze(st);
        myExp2.nameAnalyze(st);
    }
}

class TimesNode extends BinaryExpNode {
    public TimesNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" * ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
    public void nameAnalyze(SymTable st) {
        myExp1.nameAnalyze(st);
        myExp2.nameAnalyze(st);
    }
}

class DivideNode extends BinaryExpNode {
    public DivideNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" / ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
    public void nameAnalyze(SymTable st) {
        myExp1.nameAnalyze(st);
        myExp2.nameAnalyze(st);
    }
}

class AndNode extends BinaryExpNode {
    public AndNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" && ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
    public void nameAnalyze(SymTable st) {
    }
}

class OrNode extends BinaryExpNode {
    public OrNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" || ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
    public void nameAnalyze(SymTable st) {
        myExp1.nameAnalyze(st);
        myExp2.nameAnalyze(st);
    }
}

class EqualsNode extends BinaryExpNode {
    public EqualsNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" == ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
    public void nameAnalyze(SymTable st) {
        myExp1.nameAnalyze(st);
        myExp2.nameAnalyze(st);
    }
}

class NotEqualsNode extends BinaryExpNode {
    public NotEqualsNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" != ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
    public void nameAnalyze(SymTable st) {
        myExp1.nameAnalyze(st);
        myExp2.nameAnalyze(st);
    }
}

class LessNode extends BinaryExpNode {
    public LessNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" < ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
    public void nameAnalyze(SymTable st) {
        myExp1.nameAnalyze(st);
        myExp2.nameAnalyze(st);
    }
}

class GreaterNode extends BinaryExpNode {
    public GreaterNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" > ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
    public void nameAnalyze(SymTable st) {
        myExp1.nameAnalyze(st);
        myExp2.nameAnalyze(st);
    }
}

class LessEqNode extends BinaryExpNode {
    public LessEqNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" <= ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
    public void nameAnalyze(SymTable st) {
        myExp1.nameAnalyze(st);
        myExp2.nameAnalyze(st);
    }
}

class GreaterEqNode extends BinaryExpNode {
    public GreaterEqNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" >= ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
    public void nameAnalyze(SymTable st) {
        myExp1.nameAnalyze(st);
        myExp2.nameAnalyze(st);
    }
}
