import syntaxtree.*;
import visitor.*;
import symtbl.*;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
/* Σκοπός αυτού του visitor είναι να "γεμίσει" το symbol table, δηλάδη να εισάγει όλα τα δεδομένα του προγράμματος 
στη δομή του symbol table ελέγχοντας ταυτόχρονα για διπλότυπα. */ 

class SymbolTableVisitor extends GJDepthFirst <String, Void>{
    SymbolTable symtbl = new SymbolTable();
    String currClass, currMethod;
    // κρατάω και currMethInfo επειδή έχω λίστα από μεθόδους στο symbol table
    MethodInfo currMethInfo;
    // έλεγχος για διπλή δήλωση μεθόδου
    public boolean isDuplicate(ClassInfo c, MethodInfo m){
        List<MethodInfo> method = c.RetrieveMethod(m.GetMethodName());
        Set<String> set = new HashSet<>();
        for(MethodInfo i : method){
            String parameters = i.RetrieveParamSign();
            if(set.contains(parameters)){
                return true;
            }else{
                set.add(parameters);
            }
        }
        return false;
    }
    @Override
    public String visit(MainClass n, Void argu) throws Exception {
        String classname = n.f1.accept(this, argu);
        currClass = classname;
        // Δημιουργια της κλάσης
        ClassInfo tempinfo = new ClassInfo(classname);
        // Μέθοδος - main, τύπος επιστροφής void
        MethodInfo mainMethod = new MethodInfo("main", "void");
        tempinfo.AddMethod("main", mainMethod);
        tempinfo.setIsMain(true);
        currMethInfo = mainMethod;
        currMethod = "main";
        String argName = n.f11.accept(this, null);
        // variable τύπου String[] πάντα
        LocalVarInfo l = new LocalVarInfo(argName, "String[]");
        currMethInfo.AddParam(l);
        // εισαγωγή της main στην αρχική κλάση
        symtbl.AddClass(classname, tempinfo);
        super.visit(n, argu);
        currMethInfo = null;
        currMethod = null;
        return null;
    }
    @Override
    public String visit(ClassDeclaration n, Void argu) throws Exception {
        n.f0.accept(this, argu);
        String classname = n.f1.accept(this, argu);
        currClass = classname;
        // αν υπάρχει ήδη η κλάση τότε error
        if(symtbl.ContainsClass(classname)){
            throw new Exception("Duplicate class: " + classname);
        }
        ClassInfo tempinfo = new ClassInfo(classname);
        symtbl.AddClass(classname, tempinfo);
        super.visit(n,argu);
        return null;
    }
    @Override
    public String visit(ClassExtendsDeclaration n, Void argu) throws Exception{
        n.f0.accept(this, argu);
        String classname = n.f1.accept(this, argu);
        currClass = classname;
        // αν υπάρχει ήδη η κλάση τότε error
        if(symtbl.ContainsClass(classname)){
            throw new Exception("Duplicate class: " + classname);
        }
        n.f2.accept(this, argu);
        String parent = n.f3.accept(this, argu);
        // αν δεν έχει δηλωθεί η κλάση γονέας προγουμένως, τότε error
        if(!symtbl.ContainsClass(parent)){
            throw new Exception("In class " + currClass + ". Parent class has not been declared: " + parent);
        }
        ClassInfo tempinfo = new ClassInfo(classname);
        tempinfo.SetParent(parent);

        symtbl.AddClass(classname, tempinfo);
        super.visit(n,argu);
        return null;
    }
    @Override 
    public String visit(VarDeclaration n, Void argu) throws Exception {
        String type = n.f0.accept(this, argu);
        String name = n.f1.accept(this, argu);

        if(currMethod == null){
            // τότε πρόκειται για field κλάσης, συγκεκριμένα της currClass
            FieldInfo tempinfo = new FieldInfo(name, type);
            ClassInfo curr = symtbl.RetrieveClass(currClass);
            /* δεν γίνεται να επαναλμβάνονται ονόματα στα πεδία της ίδιας κλάσης
             (A name cannot be repeated in fields (of the same class)).*/
            if(curr.ContainsField(name)){
                throw new Exception("In class " + currClass + ". Variable has already been declared: " + name);
            }
            curr.AddField(name, tempinfo);
        }else if (currMethod != null){
            /* δεν γίνεται να επαναλμβάνονται ονόματα στις τοπικές μεταβλητές της ίδιας μεθόδου,
            (A name cannot be repeated in local variables (of the same method))
            ούτε να υπάρχουν ίδια ονόματα σε παραμέτρους και τοπικές μεταβλητές */
            if(currMethInfo.ContainsLocalVar(name) || currMethInfo.ContainsParam(name)){
                throw new Exception("In class " + currClass + ". Variable has already been declared: " + name);
            }
            // αλλιώς πρόκειται για δηλώσεις μεταβλητών μιας μεθόδου
            LocalVarInfo tempinfo = new LocalVarInfo(name, type);
            currMethInfo.AddLocalVar(tempinfo);
        }
        super.visit(n, argu);
        return null;
    }
    @Override
    public String visit(MethodDeclaration n, Void argu) throws Exception{
        ClassInfo curr = symtbl.RetrieveClass(currClass);
        String name = n.f2.accept(this, argu);
        String type = n.f1.accept(this, argu);
        currMethod = name;
        MethodInfo method = new MethodInfo(name, type);
        currMethInfo = method;
        curr.AddMethod(name, method);
        super.visit(n, argu);
        // αφου ολοκληρωθεί η δήλωση της μεθόδου, δηλαδή μετά το supervisit ελέγχω αν
        // η μέθοδος είναι duplicate
        if(isDuplicate(curr, method)){
            throw new Exception("Method " + currMethInfo.GetMethodName() + "() is already declared in class: " + curr.GetName());
        }
        // αφού επισκεφτεί τα παιδιά της μεθόδου (παραμέτρους και τοπικές μεταβλητές)
        // πρέπει το currMethod να "αρχικοποιηθεί" ξανά ώστε οι επόμενες παράμετροι 
        // και τοπικές μεταβλητές να μην θεωρηθεί ότι ανήκουν στη τρέχουσα μέθοδο
        currMethod = null;
        currMethInfo = null;
        return null;
    }
    @Override
    public String visit(FormalParameter n, Void argu) throws Exception{
        String type = n.f0.accept(this, null);
        String name = n.f1.accept(this, null);
        LocalVarInfo par = new LocalVarInfo(name, type);
        // δεν γίνεται να επαναλμβάνονται ονόματα στη λίστα παραμέτρων
        if(currMethInfo.ContainsParam(name)){
            throw new Exception("In class " + currClass + ". Parameter has already been declared: " + name);
        }
        currMethInfo.AddParam(par);
        return null;
    }
    @Override
    public String visit(Identifier n, Void argu) {
        return n.f0.toString();
    }
    @Override
    public String visit(ArrayType n, Void argu) {
        return "int[]";
    }

    @Override
    public String visit(BooleanType n, Void argu) {
        return "boolean";
    }

    @Override
    public String visit(IntegerType n, Void argu) {
        return "int";
    }

}