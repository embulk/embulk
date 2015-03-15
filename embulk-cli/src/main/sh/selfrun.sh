@echo on
setlocal enabledelayedexpansion

SET java_args=
SET jruby_args=
SET default_optimize=0
SET overwrite_optimize=0
SET file_args=
SET FILE_FLAG=0
SET OTHER_FLAG=

if "%1" == "" goto :OPT_END

:OPT_START
rem Get args
SET ARGS=%1
rem Argument is to empty if OPT_END
if "%ARGS%"== "" goto :OPT_END

rem java wo/optimization
if /i "%ARGS%" == "run" (
    SET default_optimize=1
    goto SHIFT
)

echo %FILE_FLAG%
if %FILE_FLAG% == 1 (
  rem for /f %%i in ("%ARGS%") do SET file_args=%%i
  SET /P file_args=<"%ARGS%"
  SET java_args=%java_args% %file_args%
  SET FILE_FLAG=0
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
    SET FILE_FLAG=1
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

:OPT_END

SET FLAG=FALSE
if %overwrite_optimize% == 1 SET FLAG=TRUE
if %default_optimize% == 1 SET FLAG=TRUE
if %FLAG% == TRUE (
  SET java_args=-XX:+AggressiveOpts -XX:+UseConcMarkSweepGC %java_args%
) ELSE (
  SET java_args=-XX:+AggressiveOpts -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Xverify:none %java_args%
)

echo java %java_args% -jar %~0 %jruby_args% %OTHER_FLAG%

exit /B