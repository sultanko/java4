cp ./manifest.txt ./out/production/hw4
cd ./out/production/hw4
jar cfm Implementor.jar manifest.txt ru/ifmo/ctddev/shah/implementor/*.class info/kgeorgiy/java/advanced/implementor/*.class
mv Implementor.jar ../../../
