package symtbl;
// Πληροφορίες για τα πεδία μια κλάσης. Θέλουμε πληροφορίες όπως
// όνομα, τύπος και offset
public class FieldInfo{
    String name;
    String type;
    int offset;
    public FieldInfo(String name, String type){
        this.name = name;
        this.type = type;
    }
    public String GetType(){
        return this.type;
    }
    public String GetName(){
        return this.name;
    }
    public int GetOffset(){
        return this.offset;
    }
    public void SetOffset(int offs){
        this.offset = offs;
    }
}