package symtbl;
// Τοπικές μεταβλητές μιας μεθόδου
public class LocalVarInfo{
    String name;
    String type;
    public LocalVarInfo(String name, String type){
        this.name = name;
        this.type = type;
    }
    public String GetType(){
        return this.type;
    }
    public String GetName(){
        return this.name;
    }
}  