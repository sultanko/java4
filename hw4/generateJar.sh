cp ./manifest.txt ./out/production/hw4
mkdir out/production/hw4/artifacts
cp ../java-advanced-2015/artifacts/ImplementorTest.jar ./out/production/hw4/artifacts
mkdir out/production/hw4/lib
cp ../java-advanced-2015/lib/*.jar ./out/production/hw4/lib
cd ./out/production/hw4
jar cfm Implementor.jar manifest.txt ru/ifmo/ctddev/shah/implementor/*.class artifacts/ImplementorTest.jar lib/*.jar
mv Implementor.jar ../../../
