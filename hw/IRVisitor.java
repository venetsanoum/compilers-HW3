import syntaxtree.*;
import visitor.*;
import symtbl.*;
import java.io.FileWriter;

class IRVisitor extends GJDepthFirst <void, String>{
    String currClass, currMethod;
    MethodInfo currMethInfo;
    ClassInfo currClassInfo;
    SymbolTable symtbl;
    FileWriter fw;
    public IRVisitor(SymbolTable st, FileWriter fw){
        this.symtbl = st;
        this.fw = fw;
    }
    int regCount = 0;
    int ifCount = 0;
    int whileCount = 0;

    String newTemp(){
        return "%_" + regCount++;
    }
    String newIfLabel(){
        return "if" + ifCount++;
    }
    String newWhileLabel(){
        return "while" + whileCount;
    }
    void emit(String s) throws Exception{
        try{
            fw.write(s);
        }catch (Exception e){
            System.err.println(e.getMessage());
        }
    }
}