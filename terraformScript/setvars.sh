#!/bin/sh
#    Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
#    Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.



#Tenancy specifics
TF_VAR_tenancy_ocid=""; export TF_VAR_tenancy_ocid
TF_VAR_user_ocid=""; export TF_VAR_user_ocid
TF_VAR_fingerprint=""; export TF_VAR_fingerprint
TF_VAR_private_key_path=""; export TF_VAR_private_key_path
#OCI Region, such as us-phoenix-1
TF_VAR_region=""; export TF_VAR_region

#Comparment OCID for all created entities
TF_VAR_compartment_ocid=""; export TF_VAR_compartment_ocid

#Name prefix for all created entites
TF_VAR_name_prefix="as"; export TF_VAR_name_prefix

#Registry specifics
TF_VAR_region_code=""; export TF_VAR_region_code
TF_VAR_tenancy_namespace=""; export TF_VAR_tenancy_namespace
TF_VAR_repos_name=""; export TF_VAR_repos_name

#Function configuration parameters
TF_VAR_debug_level="INFO"; export TF_VAR_debug_level
TF_VAR_debug_jwt=""; export TF_VAR_debug_jwt
TF_VAR_gtw_uri_base="/cloudnativesaas/opportunities"; export TF_VAR_gtw_uri_base
TF_VAR_fusion_hostname=""; export TF_VAR_fusion_hostname
TF_VAR_idcs_app_secret=""; export TF_VAR_idcs_app_secret
TF_VAR_idcs_app_clientid=""; export TF_VAR_idcs_app_clientid
TF_VAR_idcs_app_scopeid=""; export TF_VAR_idcs_app_scopeid
TF_VAR_idcs_app_url=""; export TF_VAR_idcs_app_url

