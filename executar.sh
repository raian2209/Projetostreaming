#!/usr/bin/env bash
#
# Compila e executa o Projetostreaming.
# Uso: ./executar.sh
#
set -e

# Garante que roda a partir da pasta do projeto, mesmo se chamado de outro lugar.
cd "$(dirname "$0")"

echo "==> Compilando o codigo (src -> out)..."
rm -rf out
javac -d out $(find src -name "*.java")

echo "==> Executando (streaming.Main)..."
echo
java -cp out streaming.Main
