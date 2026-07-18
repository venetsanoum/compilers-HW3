declare i32 @printf(i8*)
declare i8* @calloc(i32, i32)
declare void @throw_oob()

define i32 @main(i32 %argc, i8** %argv) {
ret i32 0
}
define i32 @bar () {
%x = alloca i32
%y = alloca i32
store i32 5, i32* %x
%_0 = load i32, i32* %x
%_1 = icmp slt i32 %_0, 6
br i1 %_1, label %if0, label %if1
if0:
store i32 8, i32* %y
br label %if2
if1:
store i32 0, i32* %y
br label %if2
if2:
%_2 = load i32, i32* %x
ret i32 %_2
}
