import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import syntaxtree.*;

public class Main {
    public static void main(String[] args) throws Exception {
        for(int i = 0; i < args.length; i++){
            FileInputStream fis = null;
            try{
                fis = new FileInputStream(args[i]);
                MiniJavaParser parser = new MiniJavaParser(fis);

                Goal root = parser.Goal();
                System.out.println("---- " + args[i] + " ----");
                SymbolTableVisitor st = new SymbolTableVisitor();
                root.accept(st, null);
                TypeCheckingVisitor tcv = new TypeCheckingVisitor(st.symtbl);
                root.accept(tcv, null);
                System.err.println("Program parsed successfully.");
                OffsetsCalculator ofc = new OffsetsCalculator(st.symtbl);
                ofc.calculate();
            }
            catch(ParseException ex){
                System.out.println(ex.getMessage());
            }
            catch(FileNotFoundException ex){
                System.err.println(ex.getMessage());
            }
            catch(Exception e){
                System.err.println(e.getMessage());
                System.err.println();
            }
            finally{
                try{
                    if(fis != null) fis.close();
                }
                catch(IOException ex){
                    System.err.println(ex.getMessage());
                }
            }
        }
    }
}