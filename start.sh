#!/bin/bash

# Compilation des fichiers Java
javac -d bin src/*.java

# Lancement du serveur dans un nouveau terminal
gnome-terminal -- bash -c "java -cp bin Server; exec bash" &

# Attente pour que le serveur démarre
sleep 2

# Lancement des deux clients dans de nouveaux terminaux
gnome-terminal -- bash -c "java -cp bin Client; exec bash" &
gnome-terminal -- bash -c "java -cp bin Client; exec bash" &

echo "Serveur et clients lancés dans des terminaux séparés."
