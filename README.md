# compilers-HW3

```make```

```java Main [file1] [file2] ... [fileN]```

*Τα αποτελέσματα αποθηκεύονται στα αρχεία file1.ll, file2.ll ... fileN.ll.*

Το project αναπτύχθηκε στο πλαίσιο του μαθήματος Μεταγλωττιστές (Κ31 - https://cgi.di.uoa.gr/~compilers/index.html).

Η υλοποίηση περιλαμβάνει:

* Δημιουργία και διαχείριση Abstract Syntax Tree (AST) μέσω του JTB.
* Δημιουργία Symbol Table για την καταγραφή κλάσεων, μεθόδων, μεταβλητών και τύπων.
* Type checking και έλεγχο σημασιολογικών περιορισμών του προγράμματος.
* Υλοποίηση των παραπάνω λειτουργιών με χρήση του Visitor Pattern.
* Μετατροπή του MiniJava προγράμματος σε LLVM Intermediate Representation (LLVM IR) μέσω visitor.

* Διαφορετικούς visitors για τα επιμέρους στάδια της σημασιολογικής ανάλυσης, όπως:

    * **SymbolTableVisitor** – κατασκευή του Symbol Table.
    * **TypeCheckingVisitor** – έλεγχος τύπων και σημασιολογικών περιορισμών.
    * **IRVisitor** – παραγωγή LLVM Intermediate Representation.

