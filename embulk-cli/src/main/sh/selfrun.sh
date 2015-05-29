
: <<BAT
@echo off

setlocal

set this=%0
set java_args=
set jruby_args=
set default_optimize=
set overwrite_optimize=

:loop
    set temp=%~1
    if "%temp%" == "-J+O" (
        set overwrite_optimize=true
        shift
        goto end
        
    ) else if "%temp%" == "-J-O" (
        set overwrite_optimize=false
        shift
        goto end
        
    ) else if "%temp:~0,2%" == "-J" (
        if not "%temp:~2%" == "" (
            set java_args=%java_args% %temp:~2%
        ) else (
            if not exist "%~2" (
                echo "failed to load java argument file."
                exit /b 1
            )
            goto read
        )
        shift
        goto loop
        
    ) else if "%temp:~0,2%" == "-R" (
        set jruby_args=%jruby_args% %temp:~2%
        shift
        goto loop
        
    ) else if "%temp%" == "run" (
        set default_optimize=true
        goto end
        
    ) else (
        goto end
    )
    
:read

for /f "delims=" %%i in (%~2) do set java_args=%java_args% %%i
shift
shift

goto loop

:end

rem "%*" is not changed by 'shift'
set args=
:loop2
    if "%~1" == "" goto end2
    set args=%args% %~1
    shift
    goto loop2
:end2

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

exit /B
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
            v="${1#-R}"
            jruby_args="$jruby_args $v"
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
