@echo off

REM @TODO: Upload to Maven Central

SET NOTION2JEKYLL_VERSION=v0.2
set HOME=%HOMEDRIVE%%HOMEPATH%
set NOTION2JEKYLL_FOLDER=%HOME%\.notion2jekyll
set NOTION2JEKYLL_JAR=%NOTION2JEKYLL_FOLDER%\notion2jekyll-%NOTION2JEKYLL_VERSION%.jar
MKDIR "%NOTION2JEKYLL_FOLDER%" > NUL 2> NUL
REM echo %NOTION2JEKYLL_JAR%
if not exist "%NOTION2JEKYLL_JAR%" (
    powershell -Command "(New-Object Net.WebClient).DownloadFile('https://github.com/soywiz/notion2jekyll/releases/download/%NOTION2JEKYLL_VERSION%/notion2jekyll.jar', '%NOTION2JEKYLL_JAR%')"
) else (
    rem file exists
)
java -jar "%NOTION2JEKYLL_JAR%" %*
