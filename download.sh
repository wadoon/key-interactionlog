#!/bin/sh
                
TARGET=$(readlink -f "key-interactionlog-1.0.0")
                
mkdir -p $TARGET
function download() {
    # Use curl or wget
    if command -v wget >/dev/null 2>&1; then
        wget -timestamping -O    "$2" "$1"
    elif command -v curl >/dev/null 2>&1; then
        curl -L -o "$2" "$1"
    else
        echo "Error: Neither curl nor wget is installed."
        exit 1
    fi
}
            
download 'https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-serialization-json/1.9.0/kotlinx-serialization-json-1.9.0.jar'\
         "$TARGET/kotlinx-serialization-json-1.9.0.jar"
download 'https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-stdlib/2.2.10/kotlin-stdlib-2.2.10.jar'\
         "$TARGET/kotlin-stdlib-2.2.10.jar"
download 'https://repo.maven.apache.org/maven2/com/atlassian/commonmark/commonmark/0.17.0/commonmark-0.17.0.jar'\
         "$TARGET/commonmark-0.17.0.jar"
download 'https://repo.maven.apache.org/maven2/com/atlassian/commonmark/commonmark-ext-gfm-tables/0.17.0/commonmark-ext-gfm-tables-0.17.0.jar'\
         "$TARGET/commonmark-ext-gfm-tables-0.17.0.jar"
download 'https://repo.maven.apache.org/maven2/org/ocpsoft/prettytime/prettytime/5.0.9.Final/prettytime-5.0.9.Final.jar'\
         "$TARGET/prettytime-5.0.9.Final.jar"
download 'https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-datetime/0.7.1-0.6.x-compat/kotlinx-datetime-0.7.1-0.6.x-compat.jar'\
         "$TARGET/kotlinx-datetime-0.7.1-0.6.x-compat.jar"
download 'https://repo.maven.apache.org/maven2/io/github/wadoon/key/key-interactionlog/1.0.0/key-interactionlog-1.0.0.jar'\
         "$TARGET/key-interactionlog-1.0.0.jar"

                 
echo "Extend your classpath by following Jars, either using the whole folder, or by single Jars."
echo "java -cp key-2.14.4-dev.jar:$TARGET/*"
echo "or: java -cp key-2.14.4-dev.jar:$TARGET/kotlinx-serialization-json-1.9.0.jar:$TARGET/kotlin-stdlib-2.2.10.jar:$TARGET/commonmark-0.17.0.jar:$TARGET/commonmark-ext-gfm-tables-0.17.0.jar:$TARGET/prettytime-5.0.9.Final.jar:$TARGET/kotlinx-datetime-0.7.1-0.6.x-compat.jar:$TARGET/key-interactionlog-1.0.0.jar"                
