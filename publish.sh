echo "Publishing to OSSRH"

echo "Make sure the following properties are set in ~/.gradle/gradle.properties"
echo "sonatypeUsername"
echo "sonatypePassword"
echo "signing.keyId"
echo "signing.password"
echo "signing.secretKeyRingFile"

./gradlew publishToSonatype closeSonatypeStagingRepository
