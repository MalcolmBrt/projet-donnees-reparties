#!/bin/bash

if [ $# -ne 1 ]; then
    echo "Usage: $0 <AgentName>"
    echo "Exemple: $0 TestAgent"
    echo "Exemple: $0 GourmetAgent"
    echo "Exemple: $0 CompressAgent"
    exit 1
fi

AGENT_NAME=$1
JAR_NAME="${AGENT_NAME}.jar"

# On supprime les anciens fichiers pour éviter les conflits
echo "--- Nettoyage ---"
rm -rf bin runtime
mkdir -p bin
mkdir -p runtime/client runtime/srv1 runtime/srv2

echo "--- Compilation ---"
javac -d bin \
    src/common/*.java \
    src/platform/*.java \
    src/agents/*.java \
    src/services/restaurants/*.java \
    src/services/files/*.java

if [ $? -ne 0 ]; then
    echo "ERREUR DE COMPILATION"
    exit 1
fi

echo "--- Configuration pour $AGENT_NAME ---"

CORE_FILES="Server.class Server\$1.class AgentLoader.class Agent.class AgentImpl.class MoveException.class Node.class"
CLIENT_FILES="$CORE_FILES"

case $AGENT_NAME in
    "GourmetAgent")
        JAR_CONTENT="$AGENT_NAME.class"
        CLIENT_FILES="$CLIENT_FILES Restaurant.class"
        SERVER_FILES="ServiceGuideImpl.class ServiceTarifImpl.class Restaurant.class ServiceGuide.class ServiceTarif.class"
        ;;
    "CompressAgent")
        JAR_CONTENT="$AGENT_NAME.class"
        SERVER_FILES="ServiceFileImpl.class ServiceFile.class"
        ;;
    "TestAgent")
        JAR_CONTENT="$AGENT_NAME.class"
        SERVER_FILES=""
        ;;
    *)
        echo "Agent inconnu : $AGENT_NAME"
        exit 1
        ;;
esac

echo "--- Création du JAR ($JAR_NAME) ---"
cd bin
jar cf ../runtime/client/${JAR_NAME} $JAR_CONTENT
cd ..

echo "--- Déploiement ---"

# Déploiement sur le CLIENT (Uniquement le Core)
echo "-> Client..."
for file in $CLIENT_FILES; do
    cp "bin/$file" "runtime/client/"
done

# Déploiement sur les SERVEURS (Core + Services + Implémentations)
echo "-> Serveurs (srv1 & srv2)..."

ALL_SERVER_FILES="$CORE_FILES $SERVER_FILES"

for file in $ALL_SERVER_FILES; do
    cp "bin/$file" "runtime/srv1/"
    cp "bin/$file" "runtime/srv2/"
done

if [ "$AGENT_NAME" == "CompressAgent" ]; then
    cp "bigdata.log" "runtime/srv1/"
fi

echo "TERMINÉ !"
echo "=========================================="
echo "Lancement pour le scénario : $AGENT_NAME"
echo "=========================================="

if [ "$AGENT_NAME" == "GourmetAgent" ]; then
    echo "1. Srv 1 (Guide) : cd runtime/srv1 && java Server 8081 ServiceGuide"
    echo "2. Srv 2 (Tarif) : cd runtime/srv2 && java Server 8082 ServiceTarif"
    echo "3. Client        : cd runtime/client && java Server 8080 $JAR_NAME"
elif [ "$AGENT_NAME" == "CompressAgent" ]; then
    echo "1. Srv 1 (File)  : cd runtime/srv1 && java Server 8081 ServiceFile"
    echo "3. Client        : cd runtime/client && java Server 8080 $JAR_NAME"
fi