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

IF EXIST .\temp rmdir /Q /S .\temp
::pause 暂停
:: --win-console 运行时是否开启控制台
@REM %jpackage% --type app-image --name %app-name%  ^
@REM     --input ..\target\exec\ --main-jar app.jar ^
@REM     --icon ..\src\main\resources\lizi.png ^
@REM     --win-console ^
@REM     --dest .\ --temp .\temp\rule-validator

@REM IF EXIST %~dp0temp rmdir /Q /S %~dp0temp
@REM IF EXIST %app-name% (
@REM     echo build new exe success >> %log-file%
@REM ) ELSE (
@REM     echo build new exe fail >> %log-file%
@REM )
%jpackage% --type msi ^
    --name %app-name%  ^
    --input %~dp0..\..\target\exec\ --main-jar app.jar ^
    --icon %~dp0..\..\src\main\resources\lizi.ico ^
    --dest .\ --temp .\temp\rule-validator ^
    --win-dir-chooser ^
    --win-shortcut ^
    --win-per-user-install

IF EXIST .\temp rmdir /Q /S .\temp

IF EXIST %app-name%-1.0.msi (
    echo build new exe success >> %log-file%
) ELSE (
    echo build new exe fail >> %log-file%
)