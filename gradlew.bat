@rem Gradle startup script for Windows
@if "%DEBUG%"=="" @echo off

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

rem Resolve any "." and ".." in APP_HOME
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

set JAVA_EXE=java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

:findJavaFromPath
where java >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute
goto failNoJava

:findJavaFromJavaHome
set "JAVA_HOME=%JAVA_HOME:"=%"
set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if exist "%JAVA_EXE%" goto execute

echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
goto fail

:failNoJava
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in PATH.
echo.
echo Set JAVA_HOME to a JDK 17+ install, or install Android Studio.
goto fail

:fail
exit /b 1

:execute
set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%
