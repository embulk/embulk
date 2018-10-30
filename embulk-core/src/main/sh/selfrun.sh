java_args=""
jruby_args=""
default_optimize=""
overwrite_optimize=""

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
    [a-z]*\ full\ version\ \"9*\")
        echo "[WARN] Embulk does not guarantee running with Java 9." 1>&2
        echo "[WARN] Executing Java with: \"--add-modules java.xml.bind --add-modules=java.se.ee\"" 1>&2
        echo "" 1>&2
        java_args="--add-modules java.xml.bind --add-modules=java.se.ee $java_args"
        ;;
    [a-z]*\ full\ version\ \"10*\")
        echo "[WARN] Embulk does not guarantee running with Java 10." 1>&2
        echo "[WARN] Executing Java with: \"--add-modules java.xml.bind --add-modules=java.se.ee\"" 1>&2
        echo "" 1>&2
        java_args="--add-modules java.xml.bind --add-modules=java.se.ee $java_args"
        ;;
    [a-z]*\ full\ version\ \"11*\")
        echo "[WARN] Embulk does not guarantee running with Java 11." 1>&2
        echo "[WARN] Executing Java with: \"--add-modules java.xml.bind --add-modules=java.se.ee\"" 1>&2
        echo "" 1>&2
        java_args="--add-modules java.xml.bind --add-modules=java.se.ee $java_args"
        ;;
    *)
        echo "[WARN] Unrecognized Java version: $java_fullversion" 1>&2
        echo "" 1>&2
esac

if test "$overwrite_optimize" = "true" -o "$default_optimize" -a "$overwrite_optimize" != "false"; then
    java_args="-XX:+AggressiveOpts -XX:+UseConcMarkSweepGC $java_args"
else
    java_args="-XX:+AggressiveOpts -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Xverify:none $java_args"
fi

exec java $java_args -jar "$0" $jruby_args "$@"
exit 127
