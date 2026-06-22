import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import symtbl.*;

public class OffsetsCalculator{
   SymbolTable symtbl;

    public OffsetsCalculator(SymbolTable symtbl){
        this.symtbl = symtbl;
    }
    public void calculate(){
        for(Map.Entry<String, ClassInfo> entry : symtbl.RetrieveClasses().entrySet()){
            ClassInfo c = entry.getValue();
            if(c.isMainClass()) continue; // η main δεν εχει offset
            classOffsets(c);
        }
    }
    public void classOffsets(ClassInfo c){
        int field = 0;
        int method = 0;
        String parent;
        System.out.println("-----------Class " + c.GetName() + "-----------");
        // μια subclass συνεχίζει τα offsets της από εκεί που σταμάτησαν τα Offsets στην superclass
        if((parent = c.RetrieveParent()) != null){
            ClassInfo parentClass = symtbl.RetrieveClass(parent);
            field = parentClass.getNextField();
            method = parentClass.getNextMethod();
        }
        System.out.println("---Variables---");
        // για κάθε πεδίο
        for(Map.Entry<String, FieldInfo> entry : c.RetrieveFields().entrySet()){
            FieldInfo f = entry.getValue();
            f.SetOffset(field);
            System.out.println(c.GetName() + "." + f.GetName() + " : " + field);
            int size = mySize(f.GetType());
            // ανανεώνω το επόμενο offset με βάση τον τύπο του πεδίου
            field += size;

        }
        c.SetNextField(field);
        System.out.println("---Methods---");
        for(Map.Entry<String, List<MethodInfo>> entry : c.RetrieveMethods().entrySet()){
            List<MethodInfo> methods = entry.getValue();
            String methodname = entry.getKey();
            // η main δεν εχει offset
            if(methodname.equals("main")) continue;
            // για κάθε μέθοδο στη λίστα των μεθόδων
            for(MethodInfo m : methods){
                // αν είναι override η μέθοδος δεν τυπώνεται offset
                if(isOverride(m, c)) continue;
                m.SetOffset(method);
                System.out.println(c.GetName() + "." + m.GetMethodName() + " : " + method);
                method += 8;
                c.SetNextMethod(method);
            }
        }
    }

    public int mySize(String t){
        if(t.equals("int"))
            return 4;
        else if(t.equals("boolean"))
            return 1;
        return 8;
    }
    // ίδια συνάρτηση με την AllowedOverride στον TypeCheckingVisitor
    public boolean isOverride(MethodInfo m, ClassInfo currClassInfo){
        String parent;
        ClassInfo parClass = null;
        ClassInfo currClass = currClassInfo;
        List<LocalVarInfo> currSign= m.RetrieveParameters();
        // πάω σε όλες τις γονεικές κλάσεις και παίρνω τη λίστα μεθόδων με το ίδιο όνομα με την m
        while((parent = currClass.RetrieveParent()) != null){
            parClass = symtbl.RetrieveClass(parent);
            List<MethodInfo> methods = parClass.RetrieveMethod(m.GetMethodName());
            if(methods == null){ // πάω σε πιο "πάνω" γονεική κλάση
                currClass = parClass;
                continue;
            }
            for(MethodInfo pm : methods){
                boolean flag = true;
                List<LocalVarInfo> parSign = pm.RetrieveParameters();
                if(parSign.size() != currSign.size()) return false;
                if(parSign.size() == currSign.size()){
                    for(int i = 0; i < parSign.size(); i++){
                        String typeA = parSign.get(i).GetType();
                        String typeB = currSign.get(i).GetType();
                        // αν έστω και ένα τύπος είναι διαφορετικός θέτω το flag false
                        if(!typeA.equals(typeB))
                            flag = false;
                    }
                    // flag == true, σημαίνει ότι όλες οι παράμετροι ταιριάζουν
                    if(flag){
                        // οπότε αν έχουν διαφορετικό τύπο επιστροφής τότε δεν ειναι override
                        if(!pm.GetRetVal().equals(m.GetRetVal()))
                            return false;
                    return true; // όλες οι παράμετροι ταιρίαζουν και έχω και ίδιο τύπο επιστροφής, άρα valid overriding
                    }
                }
            }
            // προχωράω στην πιο "πάνω" γονεική κλάση
            currClass = parClass;
        }
        return false;
    }
}

