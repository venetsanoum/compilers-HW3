package symtbl;
import java.util.HashMap;
import java.util.ArrayList;
// Πληροφορίες για μια μέθοδο: hashmap τοπικών μεταβλητών,
// λίστα παραμέτρων, όνομα, τύπος επιστροφής και offset.
public class MethodInfo{
    HashMap <String, LocalVarInfo> localvars = new HashMap<>();
    ArrayList <LocalVarInfo> parameters = new ArrayList<>();
    String name;
    String retval;
    int offset; 
    public MethodInfo(String name, String retval){
        this.name = name;
        this.retval = retval;
    }
    public void AddParam(LocalVarInfo par){
        this.parameters.add(par);
    }
    public void AddLocalVar(LocalVarInfo var){
        this.localvars.put(var.GetName(), var);
    }
    public void SetOffset(int offs){
        this.offset = offs;
    } 
    public int GetOffset(){
        return this.offset;
    }
    public LocalVarInfo RetrieveLocalVar(String name){
        return this.localvars.get(name);
    }
    public boolean ContainsLocalVar(String name){
        return this.localvars.containsKey(name);
    }
    public String GetName(LocalVarInfo p){
        return p.name;
    }
    public boolean ContainsParam(String name){
        for(LocalVarInfo p: this.parameters){
            if(p.GetName().equals(name)) return true;
        }
        return false;
    }
    public String RetrieveParamSign(){
        String res = "";
        for(LocalVarInfo p : this.parameters){
            if(!res.equals(""))
                res += ",";
            res += p.GetType();
        }
        return res;
    }
    public ArrayList<LocalVarInfo> RetrieveParameters(){
        return this.parameters;
    }
    public String GetMethodName(){
        return this.name;
    }
    public String GetRetVal(){
        return this.retval;
    }
}