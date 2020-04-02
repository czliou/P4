import java.util.*;

public class Sym {
	private String type;

	public Sym(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public String toString() {
		return type;
	}

}

class FunctionSym extends Sym {
	private String type;
	private Sym returnType;
    private List<String> formals;

	public FunctionSym(String type, Sym returnType) {
		super(type);
		this.type = type;
        this.returnType = returnType;
        formals = new ArrayList<>();
	}  

    public void addFormal(String type) {
        formals.add(type);
    }

	public Sym getReturn() {
		return returnType;
	}         

	public String getType() {
		return type;
	}

	public String toString() {
        if(formals.size() == 0)
            return "->" + returnType.getType();
        String str = "";
        for(int i = 0; i < formals.size() - 1; i++) {
            str += formals.get(i) + ",";
        }
        return str + formals.get(formals.size() - 1) + "->" + returnType.getType();
	}  
}

class StructDefSym extends Sym {
	private String type;
	private SymTable structst;

	public StructDefSym(SymTable s, String type) {
		super(type);
		this.type = type;
        this.structst = s;
	}     

	public SymTable getStructSymTable() {
		return this.structst;
	}   

	public String getType() {
		return this.type;
	}

	public String toString() {
		return this.type;
	}
}


class StructDeclSym extends Sym {
	private String structName;
	private StructDefSym sdsym;

	public StructDeclSym(StructDefSym body, String name) {
		super(name);
		this.structName = name;
		this.sdsym = body;
	}        

	public String getStructName() {
		return this.structName;
	}

	public StructDefSym getStructDefSym() {
		return this.sdsym;
	}
}
