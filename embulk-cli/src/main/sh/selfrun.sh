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

embulk_args="$@"

while [ $# -gt 0 ] ; do
    case "$1" in
        "-b" | "--bundle")
            shift
            export EMBULK_BUNDLE_PATH="$1"
            shift
            break
            ;;
        *)
            shift
            ;;
    esac
done

if test -z ${EMBULK_BUNDLE_PATH}; then
    unset EMBULK_BUNDLE_PATH
    unset BUNDLE_GEMFILE
    export GEM_HOME="$(cd && pwd)/.embulk/jruby/$(java -cp $0 org.jruby.Main -e 'print RbConfig::CONFIG["ruby_version"]')"
    export GEM_PATH=""
else
    export BUNDLE_GEMFILE="$(cd ${EMBULK_BUNDLE_PATH} && pwd)/Gemfile"
    unset GEM_HOME
    unset GEM_PATH
fi

if test "$overwrite_optimize" = "true" -o "$default_optimize" -a "$overwrite_optimize" != "false"; then
    java_args="-XX:+AggressiveOpts -XX:+UseConcMarkSweepGC $java_args"
else
    java_args="-XX:+AggressiveOpts -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Xverify:none $java_args"
fi

exec java $java_args -jar "$0" $jruby_args "$embulk_args"
exit 127
