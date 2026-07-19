package symtbl;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
public class ClassInfo{
    // θέλω λίστα απο methodinfo για την υποστηριξη του overloading
    // θα ειναι κάτι της μορφής foo -> foo(int), foo(int, A) κλπ
    LinkedHashMap <String, List<MethodInfo>> methods = new LinkedHashMap<String, List<MethodInfo>>();
    // linked hashmap γιατί τα θέλω με την σειρά τα fields για το Offset
    LinkedHashMap <String, FieldInfo> fields = new LinkedHashMap<>();  
    LinkedHashMap<Integer, MethodInfo> vtableMeth = new LinkedHashMap<Integer, MethodInfo>(); 
    String name;
    String parentname;
    int nextField;
    int nextMethod;
    boolean ismain; // αν ειναι η κλάση που περιέχει τη main θα γίνει true
    public ClassInfo(String name){
        this.name = name;
        this.ismain = false;
    }
    public void AddMethod(String name, MethodInfo methinfo){
        // αν δεν υπάρχει λίστα με αυτό το key, δηλαδή όνομα μεθόδου
        // εισήγαγε το με άδεια λίστα
        if(!methods.containsKey(name)){
            this.methods.put(name, new ArrayList<MethodInfo>());
        }
        List <MethodInfo> list = methods.get(name);
        list.add(methinfo);
    }
    public void AddField(String name, FieldInfo fieldinfo){
        this.fields.put(name, fieldinfo);
    }
    public void SetParent(String parentname){
        this.parentname = parentname;
    }
    public void SetNextField(int offs){
        this.nextField = offs;
    }
    public void SetNextMethod(int offs){
        this.nextMethod = offs;
    }
    public List<MethodInfo> RetrieveMethod(String name){
        return this.methods.get(name);
    }
    public FieldInfo RetrieveField(String name){
        return this.fields.get(name);
    }
    public boolean ContainsField(String name){
        return this.fields.containsKey(name);
    }
    public String RetrieveParent(){
        return this.parentname;
    }
    public String GetName(){
        return this.name;
    }
    public boolean MethodExists(String name){
        return this.methods.containsKey(name);
    }
    public int getNextField(){
        return this.nextField;
    }
    public int getNextMethod(){
        return this.nextMethod;
    }
    public LinkedHashMap<String, FieldInfo> RetrieveFields(){
        return this.fields;
    }
    public LinkedHashMap<String, List<MethodInfo>> RetrieveMethods(){
        return this.methods;
    }
    public boolean isMainClass(){
        return this.ismain;
    }
    public void setIsMain(boolean b){
        this.ismain = b;
    }
    public void addvtableMeth(Integer off,  MethodInfo m){
        this.vtableMeth.put(off, m);
    }
    public LinkedHashMap<Integer, MethodInfo>RetrieveVtableMeth(){
        return this.vtableMeth;
    }
    public void copyVtableMeth(ClassInfo p){
        this.vtableMeth.putAll(p.RetrieveVtableMeth());
    }
}
