#!/bin/bash

javac -d bin src/*.java

echo "Choisissez l'option à exécuter:"
echo "1. Client"
echo "2. Serveur"
read -p "Entrez votre choix (1 ou 2): " choix

case $choix in
    1)
        java -cp bin Client
        ;;
    2)
        java -cp bin Server
        ;;
    *)
        echo "Choix invalide. Veuillez entrer 1 ou 2."
        ;;
esac