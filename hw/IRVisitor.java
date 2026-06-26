import syntaxtree.*;
import visitor.*;
import symtbl.*;
import java.io.FileWriter;
import java.util.List;

class IRVisitor extends GJDepthFirst <String, String>{
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
        return "while" + whileCount++;
    }
    void emit(String s) throws Exception{
        try{
            fw.write(s + "\n");
        }catch (Exception e){
            System.err.println(e.getMessage());
        }
    }
    String llvmType(String type){
        if(type.equals("int"))
            return "i32";
        else if(type.equals("boolean"))
            return "i1";
        else if(type.equals("int[]"))
            return "i32*";
        else
            return "i8*";
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
        emit("declare i32 @printf(i8*)");
        emit("declare i8* @calloc(i32, i32)");
        emit("declare void @throw_oob()");
        emit("");
        // TODO: more declerations
        String str = "define i32 @main(i32 %argc, i8** %argv) {";
        for (Node node : n.f14.nodes){
            node.accept(this, null);
        }
        for(Node node : n.f15.nodes){
            node.accept(this, null);
        }
        emit(str);
        emit("ret i32 0");
        emit("}");

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
        String classname = n.f1.accept(this, "name");
        currClass = classname;
        currClassInfo = symtbl.RetrieveClass(classname);
        
        n.f4.accept(this, null);
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
        String classname = n.f1.accept(this, "name");
        currClass = classname;
        currClassInfo = symtbl.RetrieveClass(classname);
        n.f4.accept(this, null);
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
        String type = n.f1.accept(this, null);
        String llvmtype = llvmType(type);
        currMethod = n.f2.accept(this, "name");
        List<MethodInfo>listOfMethods = currClassInfo.RetrieveMethod(currMethod);
        String params = n.f4.present() ? n.f4.accept(this, null) : "";
        String paramSign = n.f4.present() ? n.f4.accept(this, "signature") : "";
        // από όλες τις μεθόδους με το ίδιο όνομα (αν τυχόν έχω overloading)
        // κρατάω τις πληροφορίες για αυτή με την ίδια υπογραφή με αυτή της τρέχουσας μεθόδου
        for(MethodInfo m : listOfMethods){
            String checkparams = m.RetrieveParamSign();
            if(paramSign.equals(checkparams)){
                currMethInfo = m;
                break;
            }
        }
        emit("define " + llvmtype + " @" + currMethod + " (" + params + ") {");
        //n.f4.accept(this, "llvm");
        for(Node node : n.f7.nodes){
            node.accept(this, null);
        }
        for(Node node : n.f8.nodes){
            node.accept(this, null);
        }
        String reg = n.f10.accept(this, "load");

        emit("ret " + llvmtype + " " + reg);
        emit("}");
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
   /* Θέλω τη μορφη i32 %a, i1 %b, ... -> για το define της μεθοδου : null/default
   ή την μορφη int, boolean , ...  -> για την ευρεση της σωστης currMethod : signature*/
    public String visit(FormalParameter n, String argu) throws Exception {
        String id  = n.f1.accept(this, "name");
        String type = llvmType(n.f0.accept(this, null));
        if(argu.equals("signature")){ // paremeters signature
            return n.f0.toString();
        }
        id = "%" + id;                // declerations
        return type + " " + id;
   }
    /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
    @Override
    public String visit(VarDeclaration n, String argu) throws Exception{
        String type = n.f0.accept(this, null);
        String name = n.f1.accept(this, "name");
        String llvmtype = llvmType(type);
        emit("%" + name + " = alloca " + llvmtype);
        return null;
    }
    @Override
    public String visit(Identifier n, String argu) throws Exception{
        // πρέπει να πάρω τη μεταβλητή από τη σωστή ιεραρχία, θέλω τον τύπο της
        if("type".equals(argu))
            return CheckVariable(n.f0.toString());
        else if("load".equals(argu)){ // χρηση μεταβλητής - πρέπει να γινει load. Κάνει emit και επιστρέφει τον προσωρινό καταχωρητή
            String type = llvmType(CheckVariable(n.f0.toString()));
            String temp = newTemp();
            emit(temp + " = load " + type + ", " + type + "* %" + n.f0.toString());
            return temp;
        }
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
    *       | ThisExpression() TODO
    *       | ArrayAllocationExpression() TODO
    *       | AllocationExpression() TODO
    *       | NotExpression() TODO
    *       | BracketExpression() TODO
    */
    @Override
    public String visit(IntegerLiteral n, String argu){
        return n.f0.toString();
    }
    @Override
    public String visit(TrueLiteral n, String argu) throws Exception {
      return "1";
   }
   @Override
   public String visit(FalseLiteral n, String argu) throws Exception {
      return "0";
   }
   /* Statements */
       /**
    * f0 -> Block() TODO
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
   public String visit(AssignmentStatement n, String argu) throws Exception {
    String id = n.f0.accept(this, "name");
    String llvmtype = llvmType(CheckVariable(id));
    String reg = n.f2.accept(this, "load");
    emit("store " + llvmtype + " " + reg + ", " + llvmtype + "* %" + id);
    // store το αποτέλεσμα του expression στο identifier
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
   /* ptr_idx = &ptr[idx] 
   %ptr_idx = getelementptr i8, i8* %ptr, i32 %idx */
    @Override
    public String visit(ArrayAssignmentStatement n, String argu) throws Exception{
        String arrayindx = n.f0.accept(this, "load");
        String indx = n.f2.accept(this, "load");
        String reg = n.f5.accept(this, "load");
        String tmp = newTemp(); // αποτελεσμα του getelementptr
        String str = tmp + " = getelementptr i32, i32* " + arrayindx + ", i32 " + indx;
        emit(str);
        emit("store i32 " + reg + ", i32* " + tmp);
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
   public String visit(IfStatement n, String argu) throws Exception {
        String ifLabel = newIfLabel();
        String elseLabel = newIfLabel();
        String endLabel = newIfLabel();
        String condition = n.f2.accept(this, "load");
        emit("br i1 " + condition + ", label %" + ifLabel + ", label %" + elseLabel);
        emit(ifLabel + ":");
        n.f4.accept(this, null);
        emit("br label %" + endLabel); // μετα το if πηγαινε στο τελος
        emit(elseLabel + ":");
        n.f6.accept(this, null);
        emit("br label %" + endLabel); // μετα το else πηγαινε στο τελος
        emit(endLabel + ":");
        return null;
    }

   /**
    * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
   public String visit(WhileStatement n, String argu) throws Exception {
        String whileLabel = newWhileLabel();
        emit("br label %" + whileLabel);
        emit(whileLabel + ":");
        String condition = n.f2.accept(this, "load");
        String trueLabel = newWhileLabel();
        String falseLabel = newWhileLabel();
        emit("br i1 " + condition + ", label %" + trueLabel + ", label %" + falseLabel);
        emit(trueLabel + ":");
        n.f4.accept(this, null);
        emit("br label %" + whileLabel);
        emit(falseLabel + ":");
        return null;
    }

   /**
    * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
   public String visit(PrintStatement n, String argu) throws Exception {
        String reg = n.f2.accept(this, "load");
        emit("call void @printint(i32 " + reg + ")");
        return null;
   }
    /* Expressions */
    /**
    * f0 -> AndExpression() 
    *       | CompareExpression()
    *       | PlusExpression()
    *       | MinusExpression()
    *       | TimesExpression()
    *       | ArrayLookup() TODO
    *       | ArrayLength() TODO
    *       | MessageSend() TODO
    *       | PrimaryExpression()
    */
    /**
    * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
    @Override
    public String visit(CompareExpression n, String argu) throws Exception{
        String left = n.f0.accept(this, "load");
        String right = n.f2.accept(this, "load");
        String temp = newTemp();
        emit(temp + " = icmp slt i32 " + left + ", " + right);
        return temp;
    }
    /**
    * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
    @Override
    public String visit(PlusExpression n, String argu) throws Exception{
        String left = n.f0.accept(this, "load");
        String right = n.f2.accept(this, "load");
        String temp = newTemp();
        emit(temp + " = add i32 " + left + ", " + right);
        return temp;
    }
    /**
    * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
    @Override
    public String visit(MinusExpression n, String argu) throws Exception{
        String left = n.f0.accept(this, "load");
        String right = n.f2.accept(this, "load");
        String temp = newTemp();
        emit(temp + " = sub i32 " + left + ", " + right);
        return temp;
    }
    /**
    * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
    @Override
    public String visit(TimesExpression n, String argu) throws Exception{
        String left = n.f0.accept(this, "load");
        String right = n.f2.accept(this, "load");
        String temp = newTemp();
        emit(temp + " = mul i32 " + left + ", " + right);
        return temp;
    }
    /**
    * f0 -> PrimaryExpression()
    * f1 -> "&&"
    * f2 -> PrimaryExpression()
    */
    @Override
    public String visit(AndExpression n, String argu) throws Exception{
        String left = n.f0.accept(this, "load");
        String right = n.f2.accept(this, "load");
        String temp = newTemp();
        emit(temp + " = and i1 " + left + ", " + right);
        return temp;
    }
}
