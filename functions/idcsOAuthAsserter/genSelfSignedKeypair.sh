#
# Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
# Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
#
#!/bin/bash

basedir=$(dirname "$0")

export WORKDIR="${basedir}/genCertsDir"
unset DEBUG_M

function help(){
	cat << EOF
#
#
#### Self Sign Certificates generator helper Script #####
#
#
# This script generates a keystore with a self signed certificate that can be imported to IDCS Apps for JWT Assertion grant type
# The keystore expected from this script can be used in the implementacion of the backed service that will invoke IDCS App to retrieve access token via Assertion
#
# Prerequisites:
#   JAVA_HOME env variable.
#
# Expected output:
#   * Kesytore file
#   * Public Self Signed certificate
#   * Private Key in PKCS8 format
#
# Usage:
#	 cd \${PATH_TO_SCRIPT}
#    ./genSelfSignedKeypair.sh [--tenant|-t] <tenantValue> [--keyalias|-ka] <keyaliasValue> [--output|-o] <outputDirValue> [--debug|-d] [--help|-h]
#
#        --tenant|-t:      Tenant name to identiy the  cert
#
#        --keyalias|-ka:   Unique Key alias for certificate
#
#        --output|-o:      Output dir were the generated files will be placed. If not specified, the default dir will be the <PATH_TO_SCRIPT>/genCertsDir. THe dir should be empty.
#
#        --debug|-d:       To show debug messages. In particular will print in console the passwords used for Keystore and Private key. Use with discretion.
#
#        --help|-h:        Display this message
EOF
exit 1
}

function prepareVars() {
    ## To be generated Files
    export KEYSTORE_FILE=${WORKDIR}/${TENANT}-keystore.p12
    export CERTIFICATE_FILE=${WORKDIR}/${TENANT}-${KEY_ALIAS}-cert.pem
    export PKCS8_KEYFILE=${WORKDIR}/${TENANT}-${KEY_ALIAS}-pkcs8-key.pem

    ## KeyTool Properties.
    ## You can personalize the values based on your needs
    export STORE_TYPE=PKCS12
    export KEY_ALG=RSA
    export KEY_SIZE=2048
    export KEY_VALIDITY_DAYS=1825
    export SIG_ALG=SHA256withRSA
    export DNAME="CN=${TENANT} FN Assertion, O=Oracle Corporation,L=Redwood Shores,ST=California,C=US"
}


function error() {
    local code=$1
    local msg=$2
    echo "[ERROR]: ${msg}"
    exit ${code}
}

function validateEnv() {
    if [ -z "${JAVA_HOME}" ] ; then
       error "1" "JAVA_HOME env variable is not defined. Please set JAVA_HOME to use genSelfSignedKeypair.sh script"
    fi
}

function handleParams() {
    shopt -s nocasematch
	while [ $# -gt 0 ]; do
        case "${1}" in
	        --tenant | -t)
                shift
                export TENANT=$1
                ;;

            --keyalias | -ka)
	            shift
       	        export KEY_ALIAS=$1
       	        ;;

       	    --workdir | -wd)
	            shift
       	        export WORKDIR=$1
       	        ;;

       	    --debug | -d)
       	        export DEBUG_M="TRUE"
       	        ;;

            --help | -h)
	            help
       	        ;;

            *)
	            echo "Unrecognized argument: '${1}'."
	            usage
	            ;;
        esac
	    shift
	done

	if [ -z "${TENANT}" ] ; then
	    error 1 "Missing argument: --tenant | -t"
	fi

	if [ -z "${KEY_ALIAS}" ] ; then
	    error 1 "Missing argument: --keyalias | -ka"
	fi

	#if [ ! $(ls ${WORKDIR}) ] ; then
	#    error 1 "The output dir [${WORKDIR}] should be empty"
	#fi
}

echo "=============================================="
echo "Generating Self Signed Certificates in Kesytore"
echo ""
validateEnv
handleParams $*
prepareVars
echo ""
echo "General Properties: "
echo "-------------------------------"
echo "==> Workdir: ${WORKDIR}"
echo "==> Tenant Name: ${TENANT}"
echo "==> Key Alias: ${KEY_ALIAS}"
echo ""
echo "Keystore Properties: "
echo "-------------------------------"
echo "==> Store Type: ${STORE_TYPE}"
echo "==> Key Algorithm: ${KEY_ALG}"
echo "==> Key Size: ${KEY_SIZE}"
echo "==> Validity time(days): ${KEY_VALIDITY_DAYS}"
echo "==> Sign Algorithm: ${SIG_ALG}"
echo "==> dname: ${DNAME}"
echo ""

echo "Passwords: "
read -s -p "[*] Keystore Password: " KEYSTORE_PASSWORD
echo ""
read -s -p "[*] PrivateKey Password: " KEY_PASSWORD

echo ""
if [ "${DEBUG_M}" == "TRUE" ] ; then
    echo ""
    echo "--------------------------"
    echo "[DEBUG] KEYSTORE_PASSWORD [${KEYSTORE_PASSWORD}] "
    echo "[DEBUG] KEY_PASSWORD [${KEY_PASSWORD}] "
    echo "--------------------------"
fi
echo ""

mkdir -pv ${WORKDIR} || error 6 "Failure while creating output dir ${WORKDIR}"

# Delete files if exists
rm -f ${KEYSTORE_FILE}
rm -f ${CERTIFICATE_FILE}
rm -f ${PKCS8_KEYFILE}

echo "Generate keypair in keystore ${KEYSTORE_FILE} ..."
${JAVA_HOME}/bin/keytool -genkeypair -v -keystore "${KEYSTORE_FILE}" \
    -storetype ${STORE_TYPE} -storepass "${KEYSTORE_PASSWORD}" \
    -keyalg ${KEY_ALG} -keysize ${KEY_SIZE} -sigalg ${SIG_ALG} -validity ${KEY_VALIDITY_DAYS} \
    -alias "${KEY_ALIAS}" -keypass "${KEY_PASSWORD}" -dname "${DNAME}" || error 2 "Failure while creating keystore"

echo "Export public certificate with alias ${KEY_ALIAS} from generated keystore (openssl) ... "
${JAVA_HOME}/bin/keytool -exportcert -v -keystore "${KEYSTORE_FILE}" \
                         -storetype ${STORE_TYPE} -storepass "${KEYSTORE_PASSWORD}" \
                          -alias "${KEY_ALIAS}" -file "${CERTIFICATE_FILE}" -rfc  || error 3 "Failure while exporting keystore"

echo "Extract Private Key in PKCS8 format from generated keystore (openssl) ... "
openssl pkcs12 -in "${KEYSTORE_FILE}" -passin pass:"${KEYSTORE_PASSWORD}" \
    -nodes -nocerts -nomacver | sed -n  '/BEGIN PRIVATE KEY/,$p' > ${PKCS8_KEYFILE}   || error 4 "Failure while exporting keystore"

echo "Self Signed certificate generated successfully"
echo "=============================================="
echo "Files generated: "
echo "==> ${KEYSTORE_FILE}"
echo "==> ${CERTIFICATE_FILE} [Stored in Keystore]"
echo "==> ${PKCS8_KEYFILE} [Stored in Keystore]"
echo "=============================================="
echo ""