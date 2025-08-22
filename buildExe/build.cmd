@echo off
setlocal enabledelayedexpansion

:: 要执行的批处理脚本
set bat.file=C:\development\repository\rule-validator\buildExe\tools\package.bat
:: 被执行的批处理文件的输出信息文件
set bat.log.out=C:\development\repository\rule-validator\buildExe\tools\package.out

set common.function=%~dp0tools\common.bat

:: 删除标志文件
if exist done.flag del done.flag
:: 动画字符
set "chars=\|/-"
set index=0
set line.out=line.out

:: 启动后台命令，重定向输出到空设备，并在完成后创建标志
start /B "" cmd /c "%bat.file% >nul 2>&1 & echo . > done.flag"
echo please waiting....

if not exist %line.out% echo 0 > %line.out% && if exist %bat.log.out% del %bat.log.out%
if not exist %bat.log.out% type nul > %bat.log.out%

:loop
:: 获取当前行数
set /p last_line=<"%line.out%" 2>nul || set last_line=0
:: 读取文件新内容
set line_num=0
if exist %bat.log.out% (
    @REM 检查文件大小，文件为空时，读取文件报错
    for %%F in (%bat.log.out%) do set size= %%~zF
    if !size! gtr 0 (
        if %last_line% == 0 (
            for /f "usebackq delims=" %%a in (%bat.log.out%) do (
                    call :readline %%a
            )
        ) else (
            for /f "usebackq skip=%last_line% delims=" %%a in (%bat.log.out%) do (
                  call :readline %%a
            )
        )
    )

    :: 更新行号计数
    if !line_num! gtr 0 (
        set /a new_line=last_line + line_num
        echo !new_line! > "%line.out%"
    )
)

if exist done.flag (
    del done.flag
    del line.out
    del %bat.log.out%
    echo complete!
    pause
    exit /b
)
:: 更新动画
set /a "index=(index+1) %% 4"
call set "char=!chars:~%index%,1!"
<nul set /p "=!char!"
ping -n 2 127.0.0.1 >nul
<nul set /p "="
goto :loop

:readline
    set /a line_num+=1
    set line=%*
    call %common.function% :datetime datetimestr
    <nul set /p "=[!datetimestr!] "
    echo !line!
    exit /b