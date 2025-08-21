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
IF not EXIST ..\target\rule-validator-1.0-SNAPSHOT.jar (
    echo "please use idea to package first" >> %log-file%
    exit /b
)
::pause 暂停
:: --win-console 运行时是否开启控制台
%jpackage% --type app-image --name %app-name%  ^
    --input ..\target\ --main-jar rule-validator-1.0-SNAPSHOT.jar ^
    --icon ..\src\main\resources\lizi.png ^
    --win-console ^
    --dest .\ --temp ~/temp/rule-validator

if exist %app-name% (
    echo build new exe success >> %log-file%
) else (
    echo build new exe fail >> %log-file%
)