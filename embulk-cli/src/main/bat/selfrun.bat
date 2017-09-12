@echo off

setlocal

rem Do not use %0 to identify the JAR (bat) file.
rem %0 is just "embulk" when run by just "> embulk" while %0 is "embulk.bat" when run by "> embulk.bat".
set this=%~f0

set java_args=
set jruby_args=
set default_optimize=
set overwrite_optimize=
set status=
set error=
set args=

rem In jar file, cannot goto ahread for some reason.

for %%a in ( %* ) do (
    call :check_arg %%a
)

if "%error%" == "true" exit /b 1

set optimize=false
if "%overwrite_optimize%" == "true" (
    set optimize=true
) else (
    if "%default_optimize%" == "true" (
        if not "%overwrite_optimize%" == "false" (
            set optimize=true
        )
    )
)

if "%optimize%" == "true" (
    set java_args=-XX:+AggressiveOpts -XX:+UseConcMarkSweepGC %java_args%
) else (
    set java_args=-XX:+AggressiveOpts -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Xverify:none %java_args%
)

java %java_args% -jar %this% %jruby_args% %args%

endlocal

exit /b %ERRORLEVEL%

:check_arg
set arg=%*

rem Remove double quotations
set p1=%arg:~0,1%
set p1=%p1:"=%
set p2=%arg:~-1,1%
set p2=%p2:"=%
set arg=%p1%%arg:~1,-1%%p2%

if "%status%" == "rest" (
    set args=%args% %arg%

) else if "%status%" == "read" (
    call :read_file %arg%

) else if "%arg:~0,2%" == "-E" (
    if not "%arg:~2%" == "" (
        echo Running an external batch file: %arg:~2%
        call %arg:~2%
    )

) else if "%arg%" == "-J+O" (
    set overwrite_optimize=true
    set status=rest

) else if "%arg%" == "-J-O" (
    set overwrite_optimize=false
    set status=rest

) else if "%arg:~0,2%" == "-J" (
    if not "%arg:~2%" == "" (
        set java_args=%java_args% %arg:~2%
    ) else (
        set status=read
    )

) else if "%arg:~0,2%" == "-R" (
    set jruby_args=%jruby_args% %arg%

) else if "%arg%" == "run" (
    set default_optimize=true
    set args=%args% %arg%
    set status=rest

) else (
    set args=%args% %arg%
    set status=rest
)
exit /b

:read_file
if not exist "%~1" (
    echo "failed to load java argument file."
    set error=true
) else (
    for /f "delims=" %%i in (%~1) do set java_args=%java_args% %%i
)
set status=
exit /b
