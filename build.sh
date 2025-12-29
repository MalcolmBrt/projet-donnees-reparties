#!/bin/bash

if [ $# -ne 1 ]; then
    echo "Usage: $0 <AgentName>"
    echo "Exemple: $0 TestAgent"
    exit 1
fi

AGENT_NAME=$1
JAR_NAME="${AGENT_NAME}.jar"

# 1. NETTOYAGE
# On supprime les anciens fichiers pour éviter les conflits
echo "--- Nettoyage ---"
rm -rf bin runtime
mkdir -p bin
mkdir -p runtime/client runtime/srv1 runtime/srv2

# 2. COMPILATION
# On compile tout d'un coup. 
# -d bin : demande à Java de générer l'arborescence des packages dans le dossier bin
echo "--- Compilation ---"
javac -d bin \
    src/common/*.java \
    src/platform/*.java \
    src/agents/*.java \
    src/services/restaurants/*.java

# Vérification si la compilation a marché
if [ $? -ne 0 ]; then
    echo "ERREUR DE COMPILATION. Vérifiez vos packages et imports."
    exit 1
fi

# 3. CRÉATION DU JAR DE L'AGENT
echo "--- Création du JAR ---"
cd bin
jar cf ../runtime/client/${JAR_NAME} ${AGENT_NAME}.class
cd ..

# 4. DÉPLOIEMENT (COPIE)
# On copie TOUTE la structure de packages (common, platform) vers les serveurs
# C'est important de garder les dossiers, sinon Java ne trouve pas les classes.
echo "--- Déploiement ---"

# Copie des classes (.class)
cd bin
cp *.class ../runtime/client/
cp *.class ../runtime/srv1/
rm ../runtime/srv1/${AGENT_NAME}.class
cp *.class ../runtime/srv2/
rm ../runtime/srv2/${AGENT_NAME}.class


echo "TERMINÉ !"
echo "Pour lancer les serveurs :"
echo "Serveur 1 :"
echo "cd runtime/srv1"
echo "java Server 8081"
echo "Serveur 2 :"
echo "cd runtime/srv2"
echo "java Server 8082"
echo "Serveur initial :"
echo "cd runtime/client"
echo "java Server 8080 ${JAR_NAME}"