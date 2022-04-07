java_args=""
jruby_args=""
default_optimize=""
overwrite_optimize=""

echo "" 1>&2
echo "================================== [ NOTICE ] ==================================" 1>&2
echo " Embulk will not be executable as a single command, such as 'embulk run'." 1>&2
echo " It will happen at some point in v0.11.*." 1>&2
echo "" 1>&2
echo " Get ready for the removal by running Embulk with your own 'java' command line." 1>&2
echo " Running Embulk with your own 'java' command line has already been available." 1>&2
echo "" 1>&2
echo " For instance in Java 1.8 :" 1>&2
echo "  java -XX:+AggressiveOpts -XX:+UseConcMarkSweepGC -jar embulk-X.Y.Z.jar run ..." 1>&2
echo "  java -XX:+AggressiveOpts -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Xverify:none -jar embulk-X.Y.Z.jar guess ..." 1>&2
echo "" 1>&2
echo " See https://github.com/embulk/embulk/issues/1496 for the details." 1>&2
echo "================================================================================" 1>&2
echo "" 1>&2

while true; do
    case "$1" in
        -E*)
            external_script="${1#-E}"
            echo "Running an external shell script: $external_script"
            . $external_script
            shift
            ;;
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

java_fullversion=`java -fullversion 2>&1`

case "$java_fullversion" in
    [a-z]*\ full\ version\ \"1.7*\")
        echo "[ERROR] Embulk no longer supports Java 1.7." 1>&2
        exit 1
        ;;
    [a-z]*\ full\ version\ \"1.8*\")
        ;;
    *)
        echo "[ERROR] The Java version is not recognized by the self-executable single 'embulk' command." 1>&2
        echo "[ERROR]   $java_fullversion" 1>&2
        echo "[ERROR]" 1>&2
        echo "[ERROR] Build your own 'java' command line instead of running Embulk as a single command." 1>&2
        echo "[ERROR]" 1>&2
        echo "[ERROR] See https://github.com/embulk/embulk/issues/1496 for the details." 1>&2
        exit 1
esac

if test "$overwrite_optimize" = "true" -o "$default_optimize" -a "$overwrite_optimize" != "false"; then
    java_args="-XX:+AggressiveOpts -XX:+UseConcMarkSweepGC $java_args"
else
    java_args="-XX:+AggressiveOpts -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Xverify:none $java_args"
fi

exec java $java_args -jar "$0" $jruby_args "$@"
exit 127
