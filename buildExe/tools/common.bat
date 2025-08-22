@echo off

goto %~1
exit /b

:log
    setlocal
        if not exist %2 type nul > %2
        set mes=%3
        echo mes: %mes%
        call :datetime datetime
        echo !datetime! !mes! >> %2
    endlocal
    exit /b

:datetime
    setlocal
        for /F "tokens=1" %%i in ('date /t') do set date=%%i
        for /f "tokens=1-3 delims=:.," %%t in ("!time!") do set "ts=%%t:%%u:%%v"
        :: set result to input param
        set "datestr=%date%"
        set "timestr=%ts%"
    endlocal & set "%~2=%datestr% %timestr%"
    exit /b