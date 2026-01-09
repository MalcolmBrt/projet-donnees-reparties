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

CORE_FILES="platform/Server.class platform/Server\$1.class platform/AgentLoader.class common/Agent.class common/AgentImpl.class common/MoveException.class common/Node.class"
CLIENT_FILES="$CORE_FILES platform/Client.class"
SERVER_FILES="$CORE_FILES"

case $AGENT_NAME in
    "GourmetAgent")
        JAR_CONTENT="agents/$AGENT_NAME.class"
        CLIENT_FILES="$CLIENT_FILES services/restaurants/Restaurant.class"
        SERVER_FILES="$SERVER_FILES services/restaurants/ServiceGuideImpl.class services/restaurants/ServiceTarifImpl.class services/restaurants/Restaurant.class services/restaurants/ServiceGuide.class services/restaurants/ServiceTarif.class"
        ;;
    "CompressAgent")
        JAR_CONTENT="agents/$AGENT_NAME.class"
        SERVER_FILES="$SERVER_FILES services/files/ServiceFileImpl.class services/files/ServiceFile.class"
        ;;
    "TestAgent")
        JAR_CONTENT="agents/$AGENT_NAME.class"
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
    mkdir -p "runtime/client/$(dirname $file)"
    cp "bin/$file" "runtime/client/$file"
done

# Déploiement sur les SERVEURS (Core + Services + Implémentations)
echo "-> Serveurs (srv1 & srv2)..."

for file in $SERVER_FILES; do
    mkdir -p "runtime/srv1/$(dirname $file)"
    cp "bin/$file" "runtime/srv1/$file"
    mkdir -p "runtime/srv2/$(dirname $file)"
    cp "bin/$file" "runtime/srv2/$file"
done

if [ "$AGENT_NAME" == "CompressAgent" ]; then
    cp "bigdata.log" "runtime/srv1/"
fi

echo "TERMINÉ !"
echo "=========================================="
echo "Lancement pour le scénario : $AGENT_NAME"
echo "=========================================="

if [ "$AGENT_NAME" == "GourmetAgent" ]; then
    echo "1. Srv 1 : cd runtime/srv1 && java platform.Server IP:PORT ServiceGuide"
    echo "2. Srv 2 : cd runtime/srv2 && java platform.Server IP:PORT ServiceTarif"
    echo "3. Client: cd runtime/client && java platform.Client IP:PORT IP:PORT IP:PORT $JAR_NAME"
elif [ "$AGENT_NAME" == "CompressAgent" ]; then
    echo "1. Srv 1 : cd runtime/srv1 && java platform.Server IP:PORT ServiceFile"
    echo "3. Client: cd runtime/client && java platform.Client IP:PORT IP:PORT $JAR_NAME"
fi