@echo off
java -classpath "%~dp0\jars\commandproxy-cli.jar" commandproxy.cli.Main %*
exit /b %ERRORLEVEL%