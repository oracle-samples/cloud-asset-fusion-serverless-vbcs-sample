#
# Copyright © 2020, Oracle and/or its affiliates. All rights reserved.
# Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
#
# NOTES :  Modify to suit your environment
ENDPOINT=<KMS_ENDPOINT>
KEY=<CLOUDNATIVEFUSION_OAUTH_SECRET_OCID>

PLAIN_TEXT=<OAUTH_CLIENT_SECRET>>
# Output of script is the encrypted key
oci kms crypto encrypt --key-id "$KEY" --endpoint "$ENDPOINT" --plaintext "$( echo $PLAIN_TEXT | base64 -w0 )"
