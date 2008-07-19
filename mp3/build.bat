@echo off
if "%JAVA_HOME%"=="" echo Error: JAVA_HOME is not defined.
if exist bin/org/mp3transform/build/Build.class goto buildOK
if not exist bin mkdir bin
javac -sourcepath src/tools -d bin src/tools/org/mp3transform/build/*.java
:buildOK
java -cp "bin;%JAVA_HOME%/lib/tools.jar;temp" org.mp3transform.build.Build %1 %2 %3 %4 %5