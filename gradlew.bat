@echo off
setlocal

set APP_HOME=%~dp0

set CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar

if not exist "%CLASSPATH%" (
  echo Missing gradle-wrapper.jar in %CLASSPATH%.
  echo Please ensure gradle/wrapper/gradle-wrapper.jar exists.
  exit /b 1
)

set JAVA=%JAVA_HOME%\bin\java.exe
if not exist "%JAVA%" set JAVA=java

"%JAVA%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
endlocal

