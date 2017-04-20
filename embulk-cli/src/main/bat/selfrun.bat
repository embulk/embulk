@echo off

setlocal

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

for %%a in (%*) do (
    if %%a == "-b" (
        set found_bundle_option=true
    ) else if %%a == "--bundle" (
        set found_bundle_option=true
    ) else if %found_bundle_option% == "true"
        set EMBULK_BUNDLE_PATH=%%a
        set found_bundle_option=
    )
)

if not defined EMBULK_BUNDLE_PATH (
    set EMBULK_BUNDLE_PATH=
    set BUNDLE_GEMFILE=
    FOR /F usebackq IN (`java -cp %0 org.jruby.Main -e 'print RbConfig::CONFIG["ruby_version"]'`) DO SET rb_version=%%w
    set GEM_HOME="%USERPROFILE%/.embulk/jruby/%rb_version%"
    set GEM_PATH=""
) else (
    if not exist "%EMBULK_BUNDLE_PATH%\" (
        echo Directory not found: "%EMBULK_BUNDLE_PATH%"
        exit /b 1
    )
    call :absolute_path %EMBULK_BUNDLE_PATH%
    set BUNDLE_GEMFILE="%absolute_path%/Gemfile"
    if not exist "%BUNDLE_GEMFILE%" (
        echo Gemfile not found: "%BUNDLE_GEMFILE%"
        exit /b 1
    )
    set GEM_HOME=
    set GEM_PATH=
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

:absolute_path
set absolute_path=%~dp1
exit /b
