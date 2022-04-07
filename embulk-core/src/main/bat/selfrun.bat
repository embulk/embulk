@echo off

rem NOTE: Do not use backquotes in this bat file because backquotes are unintentionally recognized by sh.
rem NOTE: Just quotes are available for [ for /f "delims=" %%w ('...') ].

setlocal

echo. 1>&2
echo ================================== [ NOTICE ] ================================== 1>&2
echo  Embulk will not be executable as a single command, such as 'embulk run'. 1>&2
echo  It will happen at some point in v0.11.*. 1>&2
echo. 1>&2
echo  Get ready for the removal by running Embulk with your own 'java' command line. 1>&2
echo  Running Embulk with your own 'java' command line has already been available. 1>&2
echo. 1>&2
echo  For instance in Java 1.8 : 1>&2
echo   java -XX:+AggressiveOpts -XX:+UseConcMarkSweepGC -jar embulk-X.Y.Z.jar run ... 1>&2
echo   java -XX:+AggressiveOpts -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Xverify:none -jar embulk-X.Y.Z.jar guess ... 1>&2
echo. 1>&2
echo  See https://github.com/embulk/embulk/issues/1496 for the details. 1>&2
echo ================================================================================ 1>&2
echo. 1>&2

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

for /f "delims=" %%w in ('java -fullversion 2^>^&1') do set java_fullversion=%%w
echo %java_fullversion% | find " full version ""1.7" > NUL
if not ERRORLEVEL 1 (set java_version=7)
echo %java_fullversion% | find " full version ""1.8" > NUL
if not ERRORLEVEL 1 (set java_version=8)
if not defined java_version (set java_version=0)

if %java_version% EQU 7 (
    echo [ERROR] Embulk no longer supports Java 1.7. 1>&2
    exit 1

) else if %java_version% EQU 8 (
    rem Do nothing for Java 8

) else (
    echo [ERROR] The Java version is not recognized by the self-executable single 'embulk' command. 1>&2
    echo [ERROR]   %java_fullversion% 1>&2
    echo [ERROR] 1>&2
    echo [ERROR] Build your own 'java' command line instead of running Embulk as a single command. 1>&2
    echo [ERROR] 1>&2
    echo [ERROR] See https://github.com/embulk/embulk/issues/1496 for the details. 1>&2
    exit 1

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
