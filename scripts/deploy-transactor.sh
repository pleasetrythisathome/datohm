set -e
while [[ $# -gt 1 ]]
do
key="$1"

case $key in
    -v|--version)
    VERSION="$2"
    shift # past argument
    ;;
    -p|--project)
    PROJECT="$2"
    shift # past argument
    ;;
    -e|--env)
    ENV="$2"
    shift # past argument
    ;;
    -r|--region)
    REGION="$2"
    shift # past argument
    ;;
esac
shift # past argument or value
done

chmod 777 target/datomic/cf.properties;
chmod 777 target/datomic/ddb-transactor.properties;

NAME=$(tr '[:lower:]' '[:upper:]' <<< ${PROJECT:0:1})${PROJECT:1}$(tr '[:lower:]' '[:upper:]' <<< ${ENV:0:1})${ENV:1}

echo "deploying datomic transactor" ${NAME} "to" ${REGION}

set -x

~/.datomic/datomic-pro-${VERSION}/bin/datomic ensure-transactor ${PWD}/target/datomic/ddb-transactor.properties ${PWD}/target/datomic/ddb-transactor.properties;
~/.datomic/datomic-pro-${VERSION}/bin/datomic ensure-cf ${PWD}/target/datomic/cf.properties ${PWD}/target/datomic/cf.properties;
~/.datomic/datomic-pro-${VERSION}/bin/datomic create-cf-template ${PWD}/target/datomic/ddb-transactor.properties ${PWD}/target/datomic/cf.properties ${PWD}/target/datomic/cf-template.json;
~/.datomic/datomic-pro-${VERSION}/bin/datomic create-cf-stack ${REGION} ${NAME}  ${PWD}/target/datomic/cf-template.json
