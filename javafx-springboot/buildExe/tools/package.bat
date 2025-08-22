@echo off
SET "app-name=validator-tools"
SET "log-file=%~dp0package.out"
::>=jdk 14
SET "jpackage=C:\development\enviorment\jdk-17.0.12\bin\jpackage"

echo check jpackage version >> %log-file%
%jpackage% --version >> %log-file%
if not %ERRORLEVEL% EQU 0 (
    echo  jpackage check error %ERRORLEVEL% >> %log-file%
    echo "please check jpackage config(which in %~dp0package.bat): %jpackage%" >> %log-file%
    exit /b 1001
)

echo remove existed exe >> %log-file%
IF EXIST %app-name% rmdir /Q /S %app-name%
IF not EXIST %~dp0..\..\target\exec\app.jar (
    echo packaging... >> %log-file%
    call mvn clean -D"maven.test.skip"=true package -f %~dp0..\..\pom.xml
    IF not EXIST %~dp0..\..\target\exec\app.jar (
        echo mvn clean -D"maven.test.skip"=true  package: maven package failed >> %log-file%
        exit /b
    )
)
::pause 暂停
:: --win-console 运行时是否开启控制台
IF EXIST .\temp rmdir /Q /S .\temp
@REM %jpackage% --type app-image --name %app-name%  ^
@REM     --input %~dp0..\..\target\exec\ --main-jar app.jar ^
@REM     --icon %~dp0..\..\src\main\resources\lizi.png ^
@REM     --dest .\ --temp .\temp\rule-validator
@REM IF EXIST .\temp rmdir /Q /S .\temp
@REM
@REM if exist %app-name% (
@REM     echo build new exe success >> %log-file%
@REM ) else (
@REM     echo build new exe fail >> %log-file%
@REM )
%jpackage% --type msi ^
    --name %app-name%  ^
    --input %~dp0..\..\target\exec\ --main-jar app.jar ^
    --icon %~dp0..\..\src\main\resources\lizi.png ^
    --win-console ^
    --dest .\ --temp .\temp\rule-validator ^
    --win-dir-chooser ^
    --win-per-user-install

IF EXIST .\temp rmdir /Q /S .\temp

if exist %app-name%-1.0.msi (
    echo build new exe success >> %log-file%
) else (
    echo build new exe fail >> %log-file%
)