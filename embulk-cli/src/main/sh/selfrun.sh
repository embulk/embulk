
: <<BAT
@echo off
setlocal enabledelayedexpansion

SET java_args=
SET jruby_args=
SET default_optimize=0
SET overwrite_optimize=0
SET file_args=
SET JAVA_FILE_FLAG=0
SET JRUBY_FILE_FLAG=0
SET OTHER_FLAG=

if "%1" == "" goto :OPT_END

:OPT_START
rem Get args
SET ARGS=%~1
rem Argument is to empty if OPT_END
if "%ARGS%"== "" goto :OPT_END

rem java wo/optimization
if /i "%ARGS%" == "run" (
    SET default_optimize=1
    goto SHIFT
)

if %JAVA_FILE_FLAG% == 1 (
  rem hmm...
  if NOT EXIST "%ARGS%" (
    SET java_args=%java_args% %ARGS%
    goto ARGS_FILE_READ_RESUME
  )
  goto ARGS_FILE_READ
  :ARGS_FILE_READ_RESUME
  SET java_args=%java_args% %file_args%
  SET JAVA_FILE_FLAG=0
  goto SHIFT
)

if %JRUBY_FILE_FLAG% == 1 (
  rem hmm...
  if NOT EXIST "%ARGS%" (
    SET jruby_args=%jruby_args% %ARGS%
    goto ARGS_FILE_READ_RESUME
  )
  goto ARGS_FILE_READ
  :ARGS_FILE_READ_RESUME
  SET jruby_args=%jruby_args% %file_args%
  SET JRUBY_FILE_FLAG=0
  goto SHIFT
)

SET OPTION=%ARGS:~0,2%
SET OPTION_ARG=!ARGS:%OPTION%=!
if /i "%OPTION%" == "-J" (
  if /i "%OPTION_ARG%" == "+O" (
    SET overwrite_optimize=1
    goto SHIFT
  )
  if /i "%OPTION_ARG%" == "-O" (
    SET overwrite_optimize=0
    goto SHIFT
  )
  rem java flag(file)
  if "%OPTION_ARG%" == "" (
    SET JAVA_FILE_FLAG=1
    goto SHIFT
  )
  
  SET java_args=%OPTION_ARG%
  goto SHIFT
)

if /i "%OPTION%" == "-R" (
  rem jruby flag(file)
  if "%OPTION_ARG%" == "" (
    SET JRUBY_FILE_FLAG=1
    goto SHIFT
  )
  
  SET java_args=%OPTION_ARG%
  goto SHIFT
)

rem Other *)
SET OTHER_FLAG=%OTHER_FLAG% %ARGS%

:SHIFT
shift /1
goto :OPT_START

:ARGS_FILE_READ
for /f "usebackq  delims=" %%a in ("%ARGS%") do (
    SET file_args=%%a
)
goto ARGS_FILE_READ_RESUME

:OPT_END

SET FLAG=FALSE
if %overwrite_optimize% == 1 SET FLAG=TRUE
if %default_optimize% == 1 SET FLAG=TRUE
if %FLAG% == TRUE (
  SET java_args=-XX:+AggressiveOpts -XX:+UseConcMarkSweepGC %java_args%
) ELSE (
  SET java_args=-XX:+AggressiveOpts -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Xverify:none %java_args%
)

java %java_args% -jar %~0 %jruby_args% %OTHER_FLAG%

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
