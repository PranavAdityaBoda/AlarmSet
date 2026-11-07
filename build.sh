#!/bin/sh
# No Gradle: aapt2 -> javac -> Check -> d8 -> zipalign -> apksigner
set -e
cd "$(dirname "$0")"
export JAVA_HOME=$HOME/Android/jdk PATH=$HOME/Android/jdk/bin:$PATH
BT=$HOME/Android/Sdk/build-tools/35.0.0 JAR=$HOME/Android/Sdk/platforms/android-35/android.jar
rm -rf out && mkdir -p out/classes out/gen
[ -f release.keystore ] || keytool -genkeypair -keystore release.keystore -storepass android -keypass android \
    -alias spread -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Alarm Set" >/dev/null 2>&1
$BT/aapt2 compile --dir res -o out/res.zip
$BT/aapt2 link -I $JAR --manifest AndroidManifest.xml --java out/gen -o out/base.apk out/res.zip
javac -nowarn --release 11 -cp $JAR -d out/classes $(find src out/gen -name "*.java")
javac -nowarn -cp out/classes -d out/test test/Check.java
java -cp out/test:out/classes Check
$BT/d8 --release --lib $JAR --min-api 31 --output out $(find out/classes -name '*.class')
cp out/base.apk out/unsigned.apk && (cd out && zip -q unsigned.apk classes.dex)
$BT/zipalign -f 4 out/unsigned.apk out/aligned.apk
$BT/apksigner sign --ks release.keystore --ks-pass pass:android --out AlarmSet.apk out/aligned.apk
rm -f AlarmSet.apk.idsig
echo "built $(pwd)/AlarmSet.apk"
