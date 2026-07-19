@.A_vtable = global [2 x i8*] [
i8* bitcast (i32 (i8*)* @A.foo to i8*),
 i8* bitcast (i32 (i8*)* @A.bar to i8*)]
@.B_vtable = global [3 x i8*] [
i8* bitcast (i32 (i8*)* @A.foo to i8*),
 i8* bitcast (i32 (i8*)* @A.bar to i8*),
 i8* bitcast (i32 (i8*)* @B.baz to i8*)]
declare i32 @printf(i8*)
declare i8* @calloc(i32, i32)
declare void @throw_oob()
define i32 @main(i32 %argc, i8** %argv) {

ret i32 0
}
define i32 @foo () {

ret i32 1
}
define i32 @bar () {

ret i32 2
}
