class MoreThan4{
    public static void main(String[] a){
        B b;
        b = new B();
        System.out.println(b.foo(true));
    }
}
class A {
    public int foo(int x) { return 1; }
}

class B extends A {
    public int foo(boolean b) { 
        return 1;
    }
}