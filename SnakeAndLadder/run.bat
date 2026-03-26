@echo off
echo Compiling...
javac SnakeAndLadder.java
if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b 1
)
echo Running...
java SnakeAndLadder
pause
