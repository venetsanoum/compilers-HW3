package symtbl;
import java.util.LinkedHashMap;

public class SymbolTable {
    // το symbol table είναι ένα hashmap με κλειδί το όνομα κάθε κλάσης
    // και πληροφορίες για κάθε κλάση
    LinkedHashMap<String, ClassInfo> symbtable = new LinkedHashMap<>();
    public void AddClass(String name, ClassInfo classinfo){
        this.symbtable.put(name, classinfo);
    }
    public ClassInfo RetrieveClass(String name){
        return this.symbtable.get(name);
    }
    public boolean ContainsClass(String name){
        return symbtable.containsKey(name);
    }
    public LinkedHashMap<String, ClassInfo> RetrieveClasses(){
        return this.symbtable;
    }
}


