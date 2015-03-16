
: <<BAT
@echo off
setlocal enabledelayedexpansion

rem 変数宣言
SET JAVA_ARGS=
SET JRUBY_ARGS=
SET DEFAULT_OPTIMIZE=0
SET OVERWRITE_OPTIMIZE=0
SET file_args=
SET JAVA_FILE_FLAG=0
SET JRUBY_FILE_FLAG=0
SET OTHER_FLAG=

rem :SHIFTにあるshiftコマンドにて変数がシフトする
rem (%2 -> %1)ので、%1をチェックし空ならこれ以上オプションが無いとする
if "%1" == "" goto :OPT_END

rem オプションパーサ開始
:OPT_START
rem 引数を取得
rem %~nでダブルクオテーションを展開する
SET ARGS=%~1
rem 引数が空ならオプションパーサを終了
if "%ARGS%"== "" goto :OPT_END

rem runオプション
rem デフォルト最適化オプションフラグを有効化
if /i "%ARGS%" == "run" (
    SET DEFAULT_OPTIMIZE=1
    rem 次の引数をパースする
    goto SHIFT
)

rem -J filename の対応
rem Win32 BATでは-Jのあとのオプションを取れないので
rem フラグを立てて次の引数をファイルと見なす
if %JAVA_FILE_FLAG% == 1 (
  rem ファイルが存在しない場合には
  rem 引数と見なす
  if NOT EXIST "%ARGS%" (
    SET JAVA_ARGS=%JAVA_ARGS% %ARGS%
    goto ARGS_FILE_READ_RESUME
  )
  rem ファイルからオプションを読み込む
  rem Win32 BATではIF内でFORなどを使って読み込んでも
  rem 正常に読み込めないので、一旦gotoで擬似的に関数っぽく読み出す
  goto ARGS_FILE_READ
  rem ARGS_FILE_READからここに戻る
  :ARGS_FILE_READ_RESUME
  rem 読み込んだ内容を反映する
  SET JAVA_ARGS=%JAVA_ARGS% %file_args%
  rem ファイル読み込みフラグを折る
  rem こうしないと延々とファイルとして扱ってしまう
  SET JAVA_FILE_FLAG=0
  goto SHIFT
)

if %JRUBY_FILE_FLAG% == 1 (
  rem ファイルが存在しない場合には
  rem 引数と見なす
  if NOT EXIST "%ARGS%" (
    SET JRUBY_ARGS=%JRUBY_ARGS% %ARGS%
    goto ARGS_FILE_READ_RESUME
  )
  rem ファイルからオプションを読み込む
  rem Win32 BATではIF内でFORなどを使って読み込んでも
  rem 正常に読み込めないので、一旦gotoで擬似的に関数っぽく読み出す
  rem JRUBY_ARGSフラグを立てることでARGS_FILE_READから戻るgoto先を制御している
  SET JRUBY_ARGS=1
  goto ARGS_FILE_READ
  rem ARGS_FILE_READから戻ってくる
  :JRUBY_ARGS_FILE_READ_RESUME
  rem 読み込んだ内容を反映する
  SET JRUBY_ARGS=%JRUBY_ARGS% %file_args%
  rem ファイル読み込みフラグを折る
  rem こうしないと延々とファイルとして扱ってしまう
  SET JRUBY_FILE_FLAG=0
  goto SHIFT
)

rem オプションパーサコア
rem 引数から2文字切り出す
rem 例: -J+O => -J
SET OPTION=%ARGS:~0,2%
rem それ以外を切り出す
rem ARGSからOPTIONの文字列を空に置換することで行っている。
rem 例: -J+O => +O
SET OPTION_ARG=!ARGS:%OPTION%=!
rem -Jオプション
if /i "%OPTION%" == "-J" (
  rem -J+O
  if /i "%OPTION_ARG%" == "+O" (
    SET OVERWRITE_OPTIMIZE=1
    goto SHIFT
  )
  rem -J-O
  if /i "%OPTION_ARG%" == "-O" (
    SET OVERWRITE_OPTIMIZE=0
    goto SHIFT
  )
  rem ファイルの引数指定
  if "%OPTION_ARG%" == "" (
    SET JAVA_FILE_FLAG=1
    goto SHIFT
  )
  rem -Jjavaoption
  SET JAVA_ARGS=%JAVA_ARGS% %OPTION_ARG%
  goto SHIFT
)

rem -Rオプション
if /i "%OPTION%" == "-R" (
  rem ファイルの引数指定
  if "%OPTION_ARG%" == "" (
    SET JRUBY_FILE_FLAG=1
    goto SHIFT
  )
  rem -Rjrubyoption
  SET JRUBY_ARGS=%JRUBY_ARGS% %OPTION_ARG%
  goto SHIFT
)

rem その他全てのオプション
SET OTHER_FLAG=%OTHER_FLAG% %ARGS%

rem バッチファイル引数シフト
rem %2が%1に移動する
rem 次のオプションがないか分からないのでここで再度パースする
:SHIFT
shift /1
goto :OPT_START

rem ファイルを読み込む
:ARGS_FILE_READ
rem usebackqを使わないとinの後のファイル名にてダブルクオテーションを利用出来ない
rem delimsに何も指定しないので1行全てを読み込む
for /f "usebackq  delims=" %%a in ("%ARGS%") do (
    SET file_args=%%a
)
rem JRUBYかJAVAかを判断してgoto先を変更する
if %JRUBY_ARGS% == 1 (
  goto JRUBY_ARGS_FILE_READ_RESUME
) else (
  goto ARGS_FILE_READ_RESUME
)

rem オプションパーサコア終了
:OPT_END

rem Win32 BATではORが使えないので強引にORを作り出す
SET FLAG=FALSE
if %OVERWRITE_OPTIMIZE% == 1 SET FLAG=TRUE
if %DEFAULT_OPTIMIZE% == 1 SET FLAG=TRUE
rem ここは if OVERWRITE_OPTIMIZE OR DEFAULT_OPTIMIZE then となる
if %FLAG% == TRUE (
  SET JAVA_ARGS=-XX:+AggressiveOpts -XX:+UseConcMarkSweepGC %JAVA_ARGS%
) ELSE (
  SET JAVA_ARGS=-XX:+AggressiveOpts -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Xverify:none %JAVA_ARGS%
)

rem 実際に実行する
java %JAVA_ARGS% -jar %~0 %JRUBY_ARGS% %OTHER_FLAG%

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
