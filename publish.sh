echo "Publishing to OSSRH"

echo "Make sure the following properties are set in ~/.gradle/gradle.properties"
echo "OSSRH_USERNAME"
echo "OSSRH_PASSWORD"
echo "signing.keyId"
echo "signing.password"
echo "signing.secretKeyRingFile"

./gradlew clean build publishAllPublicationsToOSSRHRepository
