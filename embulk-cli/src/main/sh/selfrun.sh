
: <<BAT
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

BAT

java_args=""
jruby_args=""
default_optimize=""
overwrite_optimize=""

while true; do
    case "$1" in
        "-J+O")
            overwrite_optimize="true"
            shift
            break;
            ;;
        "-J-O")
            overwrite_optimize="false"
            shift
            break;
            ;;
        -J*)
            v="${1#-J}"
            if test "$v"; then
                java_args="$java_args $v"
            else
                shift
                file_args=`cat "$1"`
                if test $? -ne 0; then
                    echo "Failed to load java argument file."
                    exit 1
                fi
                java_args="$java_args $file_args"
            fi
            shift
            ;;
        -R*)
            jruby_args="$jruby_args $1"
            shift
            ;;
        run)
            default_optimize="true"
            break
            ;;
        *)
            break
            ;;
    esac
done

if test "$overwrite_optimize" = "true" -o "$default_optimize" -a "$overwrite_optimize" != "false"; then
    java_args="-XX:+AggressiveOpts -XX:+UseConcMarkSweepGC $java_args"
else
    java_args="-XX:+AggressiveOpts -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Xverify:none $java_args"
fi

exec java $java_args -jar "$0" $jruby_args "$@"
exit 127
