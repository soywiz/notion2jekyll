@echo off

REM @TODO: Upload to Maven Central

SET NOTION2JEKYLL_VERSION=v0.3
SET NOTION2JEKYLL_SHA1=740e236f17e158eb0a357c4abe679adb4d41d1e9
set HOME=%HOMEDRIVE%%HOMEPATH%
set NOTION2JEKYLL_FOLDER=%HOME%\.notion2jekyll
set NOTION2JEKYLL_JAR=%NOTION2JEKYLL_FOLDER%\notion2jekyll-%NOTION2JEKYLL_VERSION%.jar
MKDIR "%NOTION2JEKYLL_FOLDER%" > NUL 2> NUL
if not exist "%NOTION2JEKYLL_JAR%" (
    powershell -Command "(New-Object Net.WebClient).DownloadFile('https://github.com/soywiz/notion2jekyll/releases/download/%NOTION2JEKYLL_VERSION%/notion2jekyll.jar', '%NOTION2JEKYLL_JAR%.temp')"
    CertUtil -hashfile "%NOTION2JEKYLL_JAR%.temp" SHA1 | find /i /v "sha1" | find /i /v "certutil" > "%NOTION2JEKYLL_JAR%.temp.sha1"
    FOR /F "tokens=*" %%g IN ('type %NOTION2JEKYLL_JAR%.temp.sha1') do (SET SHA1=%%g)
    if "%SHA1%"=="%NOTION2JEKYLL_SHA1%" (
        COPY /Y "%NOTION2JEKYLL_JAR%.temp" "%NOTION2JEKYLL_JAR%"
        echo DONE
    ) else (
        echo "Error downloading file expected %NOTION2JEKYLL_SHA1% but found %SHA1%"
        exit /b
    )
) else (
    rem file exists
)

java -jar "%NOTION2JEKYLL_JAR%" %*