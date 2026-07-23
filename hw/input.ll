@.A_vtable = global [2 x i8*] [
i8* bitcast (i32 (i8*)* @A.foo to i8*),
 i8* bitcast (i32 (i8*)* @A.bar to i8*)]
@.B_vtable = global [3 x i8*] [
i8* bitcast (i32 (i8*)* @A.foo to i8*),
 i8* bitcast (i32 (i8*)* @A.bar to i8*),
 i8* bitcast (i32 (i8*)* @B.baz to i8*)]
declare i8* @calloc(i32, i32)
declare i32 @printf(i8*, ...)
declare void @exit(i32)
@_cint = constant [4 x i8] c"%d\0a\00"
@_cOOB = constant [15 x i8] c"Out of bounds\0a\00"
define void @print_int(i32 %i) {
	%_str = bitcast [4 x i8]* @_cint to i8*
  call i32 (i8*, ...) @printf(i8* %_str, i32 %i)
  tret void
}
define void @throw_oob() {
  %_str = bitcast [15 x i8]* @_cOOB to i8*
  call i32 (i8*, ...) @printf(i8* %_str)
  call void @exit(i32 1)
  ret void
}
define i32 @main(i32 %argc, i8** %argv) {

ret i32 0
}
define i32 @foo () {

ret i32 1
}
define i32 @bar () {

ret i32 2
}
