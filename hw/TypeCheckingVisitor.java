import syntaxtree.*;
import visitor.*;
import symtbl.*;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
// Ο visitor δέχεται String και επιστρέφει String. Το ότι δέχεται string
// είναι για να ξέρει τι θέλω να μου επιστρέψει ανάλογα το argu στον identifier node
class TypeCheckingVisitor extends GJDepthFirst <String, String>{
    String currClass, currMethod;
    MethodInfo currMethInfo;
    ClassInfo currClassInfo;
    SymbolTable symtbl;
    // ο συγκεκριμένος visitor πρέπει να παίρνει το symbol table 
    public TypeCheckingVisitor(SymbolTable symtbl){
        this.symtbl = symtbl;
    }
    public String CheckVariable(String name) throws Exception{
        // έλεγχος στις τοπικές μεταβλητές
        LocalVarInfo local = currMethInfo.RetrieveLocalVar(name);
        if(local != null){
            return local.GetType();
        }
        // έλεγχος στις παραμέτρους τις μεθόδου
        List<LocalVarInfo> param = currMethInfo.RetrieveParameters();
        for(LocalVarInfo l : param){
            if(l.GetName().equals(name)){
                return l.GetType();
            }
        }
        // έλεγχος στα fields της κλάσης
        FieldInfo field = currClassInfo.RetrieveField(name);
        if(field != null){
            return field.GetType();
        }

        // έλεγχος σε fields γονεικών κλάσεων
        String parent;
        ClassInfo tempClass = currClassInfo;
        while((parent = tempClass.RetrieveParent()) != null){
            ClassInfo parentClass = symtbl.RetrieveClass(parent);
            FieldInfo parentField = parentClass.RetrieveField(name);
            if(parentField != null){
                return parentField.GetType();
            }
            tempClass = parentClass;
        }
        throw new Exception("Curr Class: " + currClass + " Undefined Variable: " + name);
    }
    public boolean isBoolean(String s){
        return s.equals("boolean");
    }
    // ελέγχει αν ο rightType είναι subclass του leftType
    // δηλαδή αν υπάρχει γονέας του rightType που είναι ίδιος με το leftType
    public boolean isSubClass(String rightType, String leftType){
        String parent;
        ClassInfo tempClass = symtbl.RetrieveClass(rightType);
        while((parent = tempClass.RetrieveParent()) != null){
            ClassInfo parentClass = symtbl.RetrieveClass(parent);
            if(parent.equals(leftType))
                return true;
            tempClass = parentClass;
        }
        return false;
    }
    public boolean isPrimary(String type){
        return (type.equals("int") || type.equals("boolean") || type.equals("int[]"));
    }
    public boolean isCompatible(String right, String left){
        // αν εχω primary type και non primary type δεν είναι compatible
        if((isPrimary(left) && !isPrimary(right)) || (!isPrimary(left) && isPrimary(right)))
            return false;
        // αν έχω 2 primary type αν δεν είναι ίδια δεν είναι compatible
        if(isPrimary(left) && isPrimary(right) && !right.equals(left))
            return false;
        // αν έχουν σχέση type, subtype είναι compatible. Εκεί που περιμένω left
        // μπορώ να βάλω right αν right subclass left.
        if(left.equals(right) || isSubClass(right, left)){
            return true;
        }
        return false;
    }
    /*  εκφώνηση: 
        it is an error — unless all argument types are exactly the same and the two methods are 
        defined in different classes (one a superclass, the other a subclass), in which case the 
        name match is treated as an override and the subclass method must have the same return type 
        as in the ancestor class.
     */
    public void AllowedOverride(MethodInfo m) throws Exception{
        String parent;
        ClassInfo parClass = null;
        ClassInfo currClass = currClassInfo;
        List<LocalVarInfo> currSign= m.RetrieveParameters();
        // πάω σε όλες τις γονεικές κλάσεις και παίρνω τη λίστα μεθόδων με το ίδιο όνομα με την m
        while((parent = currClass.RetrieveParent()) != null){
            parClass = symtbl.RetrieveClass(parent);
            List<MethodInfo> methods = parClass.RetrieveMethod(m.GetMethodName());
            if(methods == null){ // αν δεν υπάρχει τέτοια μέθοδος στον 
            // τρέχοντα γονέα, πάω σε πιο "πάνω" γονεική κλάση
                currClass = parClass;
                continue;
            }
            // για όλες τις μεθόδους του γονέα που ταιριάζουν στο όνομα με την τρέχουσα μέθοδο m
            for(MethodInfo pm : methods){
                boolean flag = true;
                List<LocalVarInfo> parSign = pm.RetrieveParameters(); // λίστα παραμέτρων
                if(parSign.size() != currSign.size()) continue; // αν έχουν διαφορετικό αριθμό ορισμάτων, τότε δεν πρόκειται καν για overriding
                                                                // οπότε συνεχίζω τον έλεγχο σε επόμενες μεθόδους αν υπάρχουν
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
                        // οπότε αν έχουν διαφορετικό τύπο επιστροφής τότε είναι λάθος overriding
                        if(!pm.GetRetVal().equals(m.GetRetVal())){
                            throw new Exception("In class: " + currClass.GetName() + " method: " + pm.GetMethodName() + "(" + pm.RetrieveParamSign() + ")" + " cannot override method: " + 
                            m.GetMethodName() + "(" + m.RetrieveParamSign() + ") of parent class " + parent + ". Return type " + m.GetRetVal() +  
                            " is incompatible with " + pm.GetRetVal());
                        }
                    return; // όλες οι παράμετροι ταιρίαζουν και έχω και ίδιο τύπο επιστροφής, άρα valid overriding
                    }
                }
            }
            // προχωράω στην πιο "πάνω" γονεική κλάση με flag == false
            currClass = parClass;
        }
    }
    public void AllowedMethod(MethodInfo m, String parentClass) throws Exception{
        if(m == currMethInfo) // δεν ελέγχω τη μέθοδο με τον εαυτό της
            return;
        boolean overrideFlag =  true;
        // παράμετροι τρέχουσας μεθόδου
        List<LocalVarInfo> currSign = currMethInfo.RetrieveParameters();
        // παράμετροι μεθόδου προς εξέταση - νέας μεθόδου
        List<LocalVarInfo> checkSign = m.RetrieveParameters();
        // αν εχουν διαφορετικό αριθμό παραμέτρων είναι εντάξει
        if(currSign.size() != checkSign.size()) return;
        // αν έστω και ένα ζευγάρι παραμέτρων δεν έχει type/subtype σχέση είναι εντάξει
        if(currSign.size() == checkSign.size()){
            for(int i = 0; i < currSign.size(); i++){
                String typeA = currSign.get(i).GetType();
                String typeB = checkSign.get(i).GetType();
                // πανομοιότυπες υπογραφές παραμέτρων έχουν ήδη ελεγχθεί από override έλεγχο
                if(!typeA.equals(typeB))
                    overrideFlag = false;
                if(!isCompatible(typeA, typeB) && !isCompatible(typeB, typeA)){
                    return;
                }
            }
        if(overrideFlag) return; // εχω όλες τις παραμέτρους πανομοιότυπες, οπότε έχει
                                // ήδη ελεγχθεί στο AllowedOverride
        // σε αυτό το σημείο έχω invalid overload, δηλαδη τουλάχιστον ένα ζευγάρι παραμέτρων με type/subtype σχέση
        throw new Exception("Overload error: " + currMethInfo.GetMethodName() + "(" + currMethInfo.RetrieveParamSign() + ") in class " + currClass + " and " + 
        currMethInfo.GetMethodName() + "(" + m.RetrieveParamSign() + ") in class " + parentClass + " are ambiguous");
        }
    }
    /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> "public"
    * f4 -> "static"
    * f5 -> "void"
    * f6 -> "main"
    * f7 -> "("
    * f8 -> "String"
    * f9 -> "["
    * f10 -> "]"
    * f11 -> Identifier()
    * f12 -> ")"
    * f13 -> "{"
    * f14 -> ( VarDeclaration() )*
    * f15 -> ( Statement() )*
    * f16 -> "}"
    * f17 -> "}"
    */
    @Override
    public String visit(MainClass n, String argu) throws Exception {
        String classname = n.f1.accept(this, "name");
        currClass = classname;
        currClassInfo = symtbl.RetrieveClass(classname);
        currMethInfo = currClassInfo.RetrieveMethod("main").get(0);
        currMethod = "main";
        super.visit(n, argu);
        currMethInfo = null;
        currMethod = null;
        return null;
    }
    /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> ( VarDeclaration() )*
    * f4 -> ( MethodDeclaration() )*
    * f5 -> "}"
    */
    @Override
    public String visit(ClassDeclaration n, String argu) throws Exception {
        n.f0.accept(this, argu);
        String classname = n.f1.accept(this, "name");
        currClass = classname;
        currClassInfo = symtbl.RetrieveClass(classname);
        super.visit(n,argu);
        return null;
    }
    /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "extends"
    * f3 -> Identifier()
    * f4 -> "{"
    * f5 -> ( VarDeclaration() )*
    * f6 -> ( MethodDeclaration() )*
    * f7 -> "}"
    */
    @Override
    public String visit(ClassExtendsDeclaration n, String argu) throws Exception{
        n.f0.accept(this, argu);
        String classname = n.f1.accept(this, "name");
        currClass = classname;
        currClassInfo = symtbl.RetrieveClass(classname);
        super.visit(n,argu);
        return null;
    }
    /**
    * f0 -> "public"
    * f1 -> Type()
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( FormalParameterList() )?
    * f5 -> ")"
    * f6 -> "{"
    * f7 -> ( VarDeclaration() )*
    * f8 -> ( Statement() )*
    * f9 -> "return"
    * f10 -> Expression()
    * f11 -> ";"
    * f12 -> "}"
    */
    @Override
    public String visit(MethodDeclaration n, String argu) throws Exception{
        ClassInfo curr = symtbl.RetrieveClass(currClass);
        String name = n.f2.accept(this, "name");
        currMethod = name;
        List<MethodInfo>listOfMethods = curr.RetrieveMethod(name);
        String listOfParameters = n.f4.present() ? n.f4.accept(this, "name") : "";

        // από όλες τις μεθόδους με το ίδιο όνομα (αν τυχόν έχω overloading)
        // κρατάω τις πληροφορίες για αυτή με την ίδια υπογραφή με αυτή της τρέχουσας μεθόδου
        for(MethodInfo m : listOfMethods){
            String params = m.RetrieveParamSign();
            if(listOfParameters.equals(params)){
                currMethInfo = m;
                break;
            }
        }
        // ελέγχος για τυχόν invalid overriding της μεθόδου (θα ελεγχθούν μόνο μέθοδοι γονεικών κλάσεων)
        AllowedOverride(currMethInfo);
        
        // ελέγχος για τυχόν invalid overloading. Πρώτα θα ελεγθούν όλες οι μέθοδοι με κοινό όνομα εντός της ίδιας κλάσης (της currClass)
        for(MethodInfo m : listOfMethods){
            AllowedMethod(m, currClass);
        }
        // και μετά πρέπει να ελεγχθούν και οι μέθοδοι με το ίδιο όνομα που ανήκουν σε γονεικές κλάσεις
        String parent;
        ClassInfo tempClass = currClassInfo;
        ClassInfo parentClass;
        while((parent = tempClass.RetrieveParent()) != null){
            parentClass = symtbl.RetrieveClass(parent);
            List<MethodInfo> methods = parentClass.RetrieveMethod(currMethod);
            if(methods == null){
                tempClass = parentClass;
                continue;
            }
            for(MethodInfo pm : methods){
                AllowedMethod(pm, parent);
            }
            tempClass = parentClass;
        }
        String returnType = n.f10.accept(this, "type");
        String methodsType = currMethInfo.GetRetVal();
        if(!returnType.equals(methodsType))
            throw new Exception("In class " + currClass + ", in method " + currMethod + ". Incompatible types: " + returnType + " cannot be converted to " + methodsType);
        super.visit(n, argu);
        // αφού επισκεφτεί τα παιδιά της μεθόδου (παραμέτρους και τοπικές μεταβλητές)
        // πρέπει το currMethod να "αρχικοποιηθεί" ξανά ώστε να μην έχουμε λανθασμένη
        // πληροφορία για τη τρέχουσα μέθοδο, πχ σε επόμενη κλάση τα πεδία τα μην θεωρηθούν τοπικές
        // μεταβλητές μιας παλαιότερης currMethod
        currMethod = null;
        currMethInfo = null;
        return null;
    }
    /**
    * f0 -> FormalParameter()
    * f1 -> FormalParameterTail()
    */
    @Override
    public String visit(FormalParameterList n, String argu) throws Exception {
        String ret = n.f0.accept(this, argu);
        if (n.f1 != null) {
            ret += n.f1.accept(this, argu); // φτιάχνεται ένα string με τις παραμέτρους
        }
        return ret;
    }
    /**
    * f0 -> ","
    * f1 -> FormalParameter()
    */
    @Override
    public String visit(FormalParameterTerm n, String argu) throws Exception {
        return n.f1.accept(this, argu);
    }
    /**
    * f0 -> ( FormalParameterTerm() )*
    */
    @Override
    public String visit(FormalParameterTail n, String argu) throws Exception {
        String ret = "";
        for ( Node node: n.f0.nodes) {
            ret += "," + node.accept(this, argu); // οι υπόλοιποι τύποι μετά τον πρώτο
        }

        return ret;
    }
    /**
    * f0 -> Type()
    * f1 -> Identifier()
    */
    @Override
    public String visit(FormalParameter n, String argu) throws Exception{
        String type = n.f0.accept(this, argu);
        super.visit(n, argu);
        return type;
    }
    /* Statements */
       /**
    * f0 -> Block()
    *       | AssignmentStatement()
    *       | ArrayAssignmentStatement()
    *       | IfStatement()
    *       | WhileStatement()
    *       | PrintStatement()
    */

   /**
    * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
    @Override
    public String visit(AssignmentStatement n, String argu) throws Exception{
        String leftType = n.f0.accept(this, "type"); // θέλω τον τύπο του αριστερού μέρους
        String exprType = n.f2.accept(this, "type"); // θέλω τον τύπο του δεξιού μέρους
        if(!isCompatible(exprType, leftType)){
            throw new Exception("In class " + currClass + ", in method " + currMethod + ". Bad Assignment Statement. Incompatible types: " + exprType + " cannot be coverted to " + leftType);
        }
        return null;
    }
    /**
    * f0 -> Identifier()
    * f1 -> "["
    * f2 -> Expression()
    * f3 -> "]"
    * f4 -> "="
    * f5 -> Expression()
    * f6 -> ";"
    */
    @Override
    public String visit(ArrayAssignmentStatement n, String argu) throws Exception{
        String type = n.f0.accept(this, "type");
        if(!type.equals("int[]"))
            throw new Exception("In class " + currClass + " ,in method " + currMethod + ". Array required but " + type + " found");
        String exprType = n.f2.accept(this, "type");
        String rightType = n.f5.accept(this, "type");
        if(!exprType.equals("int"))
            throw new Exception("In class " + currClass + " ,in method " + currMethod + ". Bad Array Assignment. Incompatible types: " +exprType + " cannot be converted to int");
        if(!rightType.equals("int"))
            throw new Exception("In class " + currClass + " ,in method " + currMethod + ". Bad Array Assignment. Incompatible types: " + rightType + " cannot be converted to int");
        return "int";
    }
    /**
    * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
    @Override
    public String visit(WhileStatement n, String argu) throws Exception{
        String type = n.f2.accept(this, "type");
        if(!isBoolean(type))
            throw new Exception("In class " + currClass + ", in method " + currMethod + ". Bad While Statement. Incompatible types: " + type + " cannot be converted to boolean");
        super.visit(n, argu);
        return null;
    }
    /**
    * f0 -> "if"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    * f5 -> "else"
    * f6 -> Statement()
    */
    @Override
    public String visit(IfStatement n, String argu) throws Exception{
        String type = n.f2.accept(this, "type");
        if(!isBoolean(type))
            throw new Exception("In class " + currClass + ", in method " + currMethod + ". Bad If Statement. Incompatible types: " + type + " cannot be converted to boolean");
        super.visit(n, argu);
        return null;
    }
    /**
    * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
    @Override
    public String visit(PrintStatement n, String argu) throws Exception{
        String type = n.f2.accept(this, "type");
        if(!type.equals("int"))
            throw new Exception("In class " + currClass + ", in method " + currMethod + ". Bad Print Statement. Incompatible types: " + type + " cannot be converted to int");
        super.visit(n, argu);
        return null;
    }
    /**
    * f0 -> ArrayType()
    *       | BooleanType()
    *       | IntegerType()
    *       | Identifier()
    */
    // άλλες φορές θέλω το όνομα του identifier και άλλες μόνο τον τύπο του
    @Override
    public String visit(Identifier n, String argu) throws Exception{
        // πρέπει να πάρω τη μεταβλητή από τη σωστή ιεραρχία
        if("type".equals(argu))
            return CheckVariable(n.f0.toString());
        return n.f0.toString(); // αυτό τις περιπτώσεις που θέλω απλά το όνομα ενός identifier
        // χωρίς να ψάξω μέσα στην ιεραρχία των μεταβλητών
    }
    @Override
    public String visit(ArrayType n, String argu) {
        return "int[]";
    }

    @Override
    public String visit(BooleanType n, String argu) {
        return "boolean";
    }

    @Override
    public String visit(IntegerType n, String argu) {
        return "int";
    }

    /* Primary Expressions */
    /**
    * f0 -> IntegerLiteral()
    *       | TrueLiteral()
    *       | FalseLiteral()
    *       | Identifier()
    *       | ThisExpression()
    *       | ArrayAllocationExpression()
    *       | AllocationExpression()
    *       | NotExpression()
    *       | BracketExpression()
    */
    @Override
    public String visit(IntegerLiteral n, String argu){
        return "int";
    }
    @Override
    public String visit(TrueLiteral n, String argu) throws Exception {
      return "boolean";
   }
   @Override
   public String visit(FalseLiteral n, String argu) throws Exception {
      return "boolean";
   }
   /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
    @Override 
    public String visit(AllocationExpression n, String argu)throws Exception{
        // πχ = new A(), θελω το όνομα Α, το οποίο θα χειριστώ ως τύπο
        String classname = n.f1.accept(this, "name");
        if(symtbl.RetrieveClass(classname) == null)
            throw new Exception("Class: " + classname + " is not declared");
        return classname;
    }
    @Override
    public String visit(ThisExpression n, String argu) throws Exception{
        // this: πρόκειται για τον τύπο της τρέχουσας κλάσης
        return currClass;
    }
    /**
    * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
    @Override
    public String visit(ArrayAllocationExpression n, String argu) throws Exception{
        String exprType = n.f3.accept(this, argu);
        if(!exprType.equals("int")){
            throw new Exception("In class " + currClass + ", in method " + currMethod + ". Bad Array Allocation. Incompatible types: " + exprType + " cannot be converded to int");
        }
        return "int[]";
    }
    @Override
    public String visit(BracketExpression n, String argu)throws Exception{
        // ο τύπος του expression
        return n.f1.accept(this, argu);
    }
    @Override
    public String visit(NotExpression n, String argu) throws Exception{
        String type = n.f1.accept(this, "type");
        if(isBoolean(type)){
            return "boolean";
        }
        throw new Exception("In class " + currClass + ", in method " + currMethod + ". Bad operand type " + type + " for unary operator !");
    }
    /* Expressions */
    /**
    * f0 -> AndExpression()
    *       | CompareExpression()
    *       | PlusExpression()
    *       | MinusExpression()
    *       | TimesExpression()
    *       | ArrayLookup()
    *       | ArrayLength()
    *       | MessageSend()
    *       | PrimaryExpression()
    */

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( ExpressionList() )?
    * f5 -> ")"
    */
    @Override
    public String visit(MessageSend n, String argu) throws Exception{
        String type = n.f0.accept(this, "type");    // θέλω τον τύπο, πχ a.foo θέλω τον τύπο του a
        // δηλαδή να ελεγχθεί στην ιεραρχία των μεταβλητών
        if(isPrimary(type))
            throw new Exception("In class " + currClass + ": " +type + " cannot be dereferenced");
        String method = n.f2.accept(this, "name"); // θέλω το όνομα της μεθόδου
        ClassInfo c;
        c = symtbl.RetrieveClass(type);
        
        ClassInfo curr = c; // η curr θα είναι η κλάση πάνω στην οποία γίνεται η κλήση της μεθόδου
        // ξεκινάει από τη κλάση του τύπου του primaryexpression
        List<MethodInfo> meth = null; // εδω θα κρατηθεί η λίστα των μεθόδων της curr με
        // το όνομα method
        // όσο δεν έχω βρει την μέθοδο σε μια κλάση, πηγαίνω στις γονεικές της
        // μέχρι να βρεθεί, αν δεν βρεθεί πουθενά σημαίνει ότι δεν έχει δηλωθεί
        while(curr != null){
            // αν υπάρχει η μέθοδος στην κλάση curr σταματάω τον έλεγχο
            if(curr.MethodExists(method)){
                meth = curr.RetrieveMethod(method);
                break;
            }
            // αλλιώς πηγαίνω στον γονέα της τρέχουσας κλάσης αν υπάρχει
            String parent = curr.RetrieveParent();
            curr = (parent == null) ? null : symtbl.RetrieveClass(parent);
        }
        // σε αυτό το σημείο δεν έχει βρεθεί πουθενά αυτή η μέθοδος, άρα δεν έχει δηλωθεί
        if(meth == null)
            throw new Exception("No such method: " + method);

        // Κοιτάω για matching
        for(MethodInfo m : meth){
            boolean flag;
            // παράμετροι της τρέχουσας μεθόδου 
            List<LocalVarInfo> params = m.RetrieveParameters();
            // expression list, παιρνω τους τύπου κάθε expression
            String given =n.f4.present() ? n.f4.accept(this, "type") : "";
            List<String> givenParam = given.equals("") ? new ArrayList<>() : List.of(given.split(","));
            // συνεχίζω στον έλεγχο μόνο αν έχω ίσο αριθμό παραμέτρων
            if(params.size() == givenParam.size()){
                flag = true;
                for(int i=0; i<params.size(); i++){
                    // αν έχω mismatch σε μια θέση πάω στην επόμενη μέθοδο αν υπάρχει
                    if(!isCompatible(givenParam.get(i), params.get(i).GetType())){
                        flag = false;
                        break;
                    }
                }
                // αν βρω match επιστρέφω τον τύπο της μεθόδου που κλήθηκε
                if(flag == true){
                    return m.GetRetVal();
                }
            }
        }              
        throw new Exception("In class " + currClass + ". Mismatch of parameters in method: " + method);
    }
    /**
    * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
    @Override
    public String visit(ExpressionList n, String argu) throws Exception{
        String ret = n.f0.accept(this, argu);
        if(n.f1 != null){
            ret += n.f1.accept(this, argu);
        }
        return ret;
    }
    /**
    * f0 -> ( ExpressionTerm() )*
    */
    @Override 
    public String visit(ExpressionTail n, String argu) throws Exception{
        String ret = "";
        for(Node node : n.f0.nodes){
            ret += "," + node.accept(this, argu);
        }
        return ret;
    }
    /**
    * f0 -> ","
    * f1 -> Expression()
    */
    @Override 
    public String visit(ExpressionTerm n, String argu) throws Exception{
        return n.f1.accept(this, argu);
    }
    /**
    * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
    @Override
    public String visit(CompareExpression n, String argu) throws Exception{
        String firstType = n.f0.accept(this, "type");
        String secondType = n.f2.accept(this, "type");
        if(!firstType.equals("int") || !secondType.equals("int")){
            throw new Exception("In class " + currClass + ", in method " + currMethod + ". Bad operand types for binary operator < (" + firstType + " < " + secondType + ") is not allowed");
        }
        return "boolean";
    }
    /**
    * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
    @Override
    public String visit(PlusExpression n, String argu) throws Exception{
        String firstType = n.f0.accept(this, "type");
        String secondType = n.f2.accept(this, "type");
        if(!firstType.equals("int") || !secondType.equals("int")){
            throw new Exception("In class " + currClass + ", in method " + currMethod + ". Bad operand types for binary operator + (" + firstType + " + " + secondType + ") is not allowed");
        }
        return firstType;
    }
    /**
    * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
    @Override
    public String visit(MinusExpression n, String argu) throws Exception{
        String firstType = n.f0.accept(this, "type");
        String secondType = n.f2.accept(this, "type");
        if(!firstType.equals("int") || !secondType.equals("int")){
            throw new Exception("In class " + currClass + ", in method " + currMethod + ". Bad operand types for binary operator - (" + firstType + " - " + secondType + ") is not allowed");
        }
        return firstType;
    }
    /**
    * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
    @Override
    public String visit(TimesExpression n, String argu) throws Exception{
        String firstType = n.f0.accept(this, "type");
        String secondType = n.f2.accept(this, "type");
        if(!firstType.equals("int") || !secondType.equals("int")){
            throw new Exception("In class " + currClass + ", in method " + currMethod + ". Bad operand types for binary operator * (" + firstType + " * " + secondType + ") is not allowed");
        }
        return firstType;
    }
    /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
    @Override
    public String visit(ArrayLength n, String argu) throws Exception{
        String primary = n.f0.accept(this, "type");
        if(!primary.equals("int[]"))
            throw new Exception("In class " + currClass + ", in method " + currMethod + ". Bad length. " + primary + " cannot be dereferenced");
        return "int";
    }
    /**
    * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
    @Override 
    public String visit(ArrayLookup n, String argu)throws Exception{
        String type = n.f0.accept(this, "type");
        if(!type.equals("int[]"))
            throw new Exception("In class " + currClass + ", in method " + currMethod + ". Array required but " + type + " found");
        type = n.f2.accept(this, "type");
        if(!type.equals("int"))
            throw new Exception("In class " + currClass + ", in method " + currMethod + ". Array Lookup. incompatible types: boolean cannot be converted to int");
        return "int";
    }
    /**
    * f0 -> PrimaryExpression()
    * f1 -> "&&"
    * f2 -> PrimaryExpression()
    */
    @Override
    public String visit(AndExpression n, String argu) throws Exception{
        String firstType, secondType;
        firstType = n.f0.accept(this, "type");
        if(!isBoolean(firstType))
            throw new Exception("In class " + currClass + ", in method " + currMethod + ". Bad operand type for binary operator &&. Got type: " + firstType);
        secondType = n.f2.accept(this, "type");
        if(!isBoolean(secondType))
            throw new Exception("In class " + currClass + ", in method " + currMethod + ". Bad operand type for binary operator &&. Got type: " + secondType);
        if(firstType.equals("boolean"))
            return "boolean";
        return secondType;
    }
}