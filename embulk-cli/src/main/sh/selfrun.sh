
: <<BAT
@echo off
setlocal enabledelayedexpansion

rem �ϐ��錾
SET JAVA_ARGS=
SET JRUBY_ARGS=
SET DEFAULT_OPTIMIZE=0
SET OVERWRITE_OPTIMIZE=0
SET file_args=
SET JAVA_FILE_FLAG=0
SET JRUBY_FILE_FLAG=0
SET OTHER_FLAG=

rem :SHIFT�ɂ���shift�R�}���h�ɂĕϐ����V�t�g����
rem (%2 -> %1)�̂ŁA%1���`�F�b�N����Ȃ炱��ȏ�I�v�V�����������Ƃ���
if "%1" == "" goto :OPT_END

rem �I�v�V�����p�[�T�J�n
:OPT_START
rem �������擾
rem %~n�Ń_�u���N�I�e�[�V������W�J����
SET ARGS=%~1
rem ��������Ȃ�I�v�V�����p�[�T���I��
if "%ARGS%"== "" goto :OPT_END

rem run�I�v�V����
rem �f�t�H���g�œK���I�v�V�����t���O��L����
if /i "%ARGS%" == "run" (
    SET DEFAULT_OPTIMIZE=1
    rem ���̈������p�[�X����
    goto SHIFT
)

rem -J filename �̑Ή�
rem Win32 BAT�ł�-J�̂��Ƃ̃I�v�V���������Ȃ��̂�
rem �t���O�𗧂ĂĎ��̈������t�@�C���ƌ��Ȃ�
if %JAVA_FILE_FLAG% == 1 (
  rem �t�@�C�������݂��Ȃ��ꍇ�ɂ�
  rem �����ƌ��Ȃ�
  if NOT EXIST "%ARGS%" (
    SET JAVA_ARGS=%JAVA_ARGS% %ARGS%
    goto ARGS_FILE_READ_RESUME
  )
  rem �t�@�C������I�v�V������ǂݍ���
  rem Win32 BAT�ł�IF����FOR�Ȃǂ��g���ēǂݍ���ł�
  rem ����ɓǂݍ��߂Ȃ��̂ŁA��Ugoto�ŋ[���I�Ɋ֐����ۂ��ǂݏo��
  goto ARGS_FILE_READ
  rem ARGS_FILE_READ���炱���ɖ߂�
  :ARGS_FILE_READ_RESUME
  rem �ǂݍ��񂾓��e�𔽉f����
  SET JAVA_ARGS=%JAVA_ARGS% %file_args%
  rem �t�@�C���ǂݍ��݃t���O��܂�
  rem �������Ȃ��Ɖ��X�ƃt�@�C���Ƃ��Ĉ����Ă��܂�
  SET JAVA_FILE_FLAG=0
  goto SHIFT
)

if %JRUBY_FILE_FLAG% == 1 (
  rem �t�@�C�������݂��Ȃ��ꍇ�ɂ�
  rem �����ƌ��Ȃ�
  if NOT EXIST "%ARGS%" (
    SET JRUBY_ARGS=%JRUBY_ARGS% %ARGS%
    goto ARGS_FILE_READ_RESUME
  )
  rem �t�@�C������I�v�V������ǂݍ���
  rem Win32 BAT�ł�IF����FOR�Ȃǂ��g���ēǂݍ���ł�
  rem ����ɓǂݍ��߂Ȃ��̂ŁA��Ugoto�ŋ[���I�Ɋ֐����ۂ��ǂݏo��
  rem JRUBY_ARGS�t���O�𗧂Ă邱�Ƃ�ARGS_FILE_READ����߂�goto��𐧌䂵�Ă���
  SET JRUBY_ARGS=1
  goto ARGS_FILE_READ
  rem ARGS_FILE_READ����߂��Ă���
  :JRUBY_ARGS_FILE_READ_RESUME
  rem �ǂݍ��񂾓��e�𔽉f����
  SET JRUBY_ARGS=%JRUBY_ARGS% %file_args%
  rem �t�@�C���ǂݍ��݃t���O��܂�
  rem �������Ȃ��Ɖ��X�ƃt�@�C���Ƃ��Ĉ����Ă��܂�
  SET JRUBY_FILE_FLAG=0
  goto SHIFT
)

rem �I�v�V�����p�[�T�R�A
rem ��������2�����؂�o��
rem ��: -J+O => -J
SET OPTION=%ARGS:~0,2%
rem ����ȊO��؂�o��
rem ARGS����OPTION�̕��������ɒu�����邱�Ƃōs���Ă���B
rem ��: -J+O => +O
SET OPTION_ARG=!ARGS:%OPTION%=!
rem -J�I�v�V����
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
  rem �t�@�C���̈����w��
  if "%OPTION_ARG%" == "" (
    SET JAVA_FILE_FLAG=1
    goto SHIFT
  )
  rem -Jjavaoption
  SET JAVA_ARGS=%JAVA_ARGS% %OPTION_ARG%
  goto SHIFT
)

rem -R�I�v�V����
if /i "%OPTION%" == "-R" (
  rem �t�@�C���̈����w��
  if "%OPTION_ARG%" == "" (
    SET JRUBY_FILE_FLAG=1
    goto SHIFT
  )
  rem -Rjrubyoption
  SET JRUBY_ARGS=%JRUBY_ARGS% %OPTION_ARG%
  goto SHIFT
)

rem ���̑��S�ẴI�v�V����
SET OTHER_FLAG=%OTHER_FLAG% %ARGS%

rem �o�b�`�t�@�C�������V�t�g
rem %2��%1�Ɉړ�����
rem ���̃I�v�V�������Ȃ���������Ȃ��̂ł����ōēx�p�[�X����
:SHIFT
shift /1
goto :OPT_START

rem �t�@�C����ǂݍ���
:ARGS_FILE_READ
rem usebackq���g��Ȃ���in�̌�̃t�@�C�����ɂă_�u���N�I�e�[�V�����𗘗p�o���Ȃ�
rem delims�ɉ����w�肵�Ȃ��̂�1�s�S�Ă�ǂݍ���
for /f "usebackq  delims=" %%a in ("%ARGS%") do (
    SET file_args=%%a
)
rem JRUBY��JAVA���𔻒f����goto���ύX����
if %JRUBY_ARGS% == 1 (
  goto JRUBY_ARGS_FILE_READ_RESUME
) else (
  goto ARGS_FILE_READ_RESUME
)

rem �I�v�V�����p�[�T�R�A�I��
:OPT_END

rem Win32 BAT�ł�OR���g���Ȃ��̂ŋ�����OR�����o��
SET FLAG=FALSE
if %OVERWRITE_OPTIMIZE% == 1 SET FLAG=TRUE
if %DEFAULT_OPTIMIZE% == 1 SET FLAG=TRUE
rem ������ if OVERWRITE_OPTIMIZE OR DEFAULT_OPTIMIZE then �ƂȂ�
if %FLAG% == TRUE (
  SET JAVA_ARGS=-XX:+AggressiveOpts -XX:+UseConcMarkSweepGC %JAVA_ARGS%
) ELSE (
  SET JAVA_ARGS=-XX:+AggressiveOpts -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Xverify:none %JAVA_ARGS%
)

rem ���ۂɎ��s����
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
