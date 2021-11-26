docker build -f ./protocol-stack/Dockerfile --target app --tag protocol-stack:latest . &&

cd ./protocol-stack && docker save -o protocol-stack.tar protocol-stack:latest &&

cd ../ && rsync -arzP -e 'ssh -p 12034' ./ v.menino@cluster.di.fct.unl.pt:~/