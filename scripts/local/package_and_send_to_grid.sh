# To be run on ".../code" directory, inside Msc-Thesis

# docker build -f ./Dockerfile --target app --tag protocol-stack:latest . &&

# cd ./protocol-stack && docker save -o protocol-stack.tar protocol-stack:latest &&
mvn clean &&

mvn package &&

rsync -arzP -e 'ssh' ./ nancy.g5k:~/protocol-stack-java