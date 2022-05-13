#!/bin/bash

path="$1"
test_folder_name="$2"
# $1 se refere ao primeiro parâmetro passado na command line
# $2 se refere ao segundo parâmetro passado na command line

fullpath=$path$test_folder_name

mkdir $fullpath
echo "Nome da pasta criada para o teste: $test_folder_name"

mv ${path}answerbasicOp* $fullpath 
echo "Copiando answerbasicOp files..."
mv ${path}operatorbasicOp* $fullpath
echo "Copiando operatorbasicOp files..."
mv ${path}target/classes/SOpbasicOp* $fullpath
echo "Copiando SOpbasicOp files..."
mv ${path}target/classes/Agg_node* $fullpath 
echo "Copiando Agg_node files..."

