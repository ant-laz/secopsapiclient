## Objective

Google SecOps has a REST API called the Chronicle API access here:

https://cloud.google.com/chronicle/docs/reference/rest

This repo offers examples calling this API using Java.

The examples here can be used on their own.

The examples here can be used on Google Cloud, e.g. on Dataflow.

## Background on authentication for REST requests to a Google API.

https://cloud.google.com/docs/authentication/rest#rest-request

You need to supply credentials to authenticate a REST request to a Google API.

The credentials provided to Application Default Credentials (ADC) are the
preferred option for authenticating a REST call in a production environment.

Some Google Cloud services, such as Compute Engine, App Engine,
Cloud Run functions & Dataflow support attaching a user-managed service account
to some types of resources.

When you attach a service account to a resource, the code running on the
resource can use that service account as its identity.

For example it is possible to specify a user-managed service account to a
Dataflow [Job](https://cloud.google.com/dataflow/docs/concepts/security-and-permissions#specifying_a_user-managed_controller_service_account)

Attaching a user-managed service account is the preferred way to provide
credentials to ADC for production code running on Google Cloud.

## 1. Using your own account to request a Chronicle API using curl

### 1.A Method: feeds.get

As an example, let's get details of a single Feed

https://cloud.google.com/chronicle/docs/reference/rest/v1alpha/projects.locations.instances.feeds/get

Check who the current gcloud user is (we'll get their auth token later)
```shell
export CURRENT_USER=$(gcloud config list account --format "value(core.account)")
echo ${CURRENT_USER}
```

Make sure your user has ``chronicle.feeds.get`` permission on the GCP Project
bound to Google Sec Ops. As per this [guide](https://cloud.google.com/chronicle/docs/onboard/configure-feature-access)
there are several pre-defined IAM roles which have this permission. For
example ```roles/chronicle.admin```.

And finally make the request to Chronicle API using your gcloud CLI credentials
```shell
curl -X GET \
    -H "Authorization: Bearer $(gcloud auth print-access-token)" \
    "https://${LOCATION}-chronicle.googleapis.com/v1alpha/projects/${BYOP_GCP_PROJECT}/locations/${LOCATION}/instances/${GSECOPS_CUSTOMER_ID}/feeds/${FEED_ID}"
```

### 1.B Method: logs.import

As an example let's import some logs into Google SecOps

https://cloud.google.com/chronicle/docs/reference/rest/v1alpha/projects.locations.instances.logTypes.logs/import

Check who the current gcloud user is (we'll get their auth token later)
```shell
export CURRENT_USER=$(gcloud config list account --format "value(core.account)")
echo ${CURRENT_USER}
```

Make sure your user has ``chronicle.logs.import`` permission on the GCP Project
bound to Google Sec Ops. As per this [guide](https://cloud.google.com/chronicle/docs/onboard/configure-feature-access)
there are several pre-defined IAM roles which have this permission. For
example ```roles/chronicle.admin```.

Make sure you have a forwarder created in Google SecOps

Let's build the body of the POST request in in a file ```config/logs.json```

The template of the file is like this.

```shell
{
  "hint": "",
  "inline_source": {
    "forwarder": "projects/${BYOP_GCP_PROJECT}/locations/${LOCATION}/instances/${GSECOPS_CUSTOMER_ID}/forwarders/${GSECOPS_FORWARDER_ID}",
      "logs": [
        {
          "data": "${BASE_64_ENCOCED_STRING}",
          "log_entry_time":  "2014-11-01T15:01:23Z",
          "collection_time": "2014-11-01T15:02:23Z"
        }
      ]
  }
}
```

And finally make the request to Chronicle API using your gcloud CLI credentials
```shell
curl -i -X POST \
    -H "Authorization: Bearer $(gcloud auth print-access-token)" \
    -H 'Content-Type: application/json' \
    -d @config/logs.json \
    "https://${LOCATION}-chronicle.googleapis.com/v1alpha/projects/${BYOP_GCP_PROJECT}/locations/${LOCATION}/instances/${GSECOPS_CUSTOMER_ID}/logTypes/${LOG_TYPE}/logs:import"
```

## 2. Using your own account in a locally executing Java program

Execute the following as your user. It will create a credentials file locally.

```shell
gcloud auth application-default login
```

This Java library https://github.com/googleapis/google-auth-library-java
will discover and parse this credentials file and use it to generate an access
token that can be used in the Authorization header on HTTP requests to
the Chronicle API.

Execute the program like so

```shell
./gradlew run --args="\
--location ${LOCATION} \
--project ${BYOP_GCP_PROJECT} \
--customerid ${GSECOPS_CUSTOMER_ID} \
--feedid ${FEED_ID} \
--forwarderid ${GSECOPS_FORWARDER_ID} \
--logtype ${LOG_TYPE}"
```



## 3. Creating a user-managed service account

Instead of using your gcloud CLI credentials, let's create a service account.

```shell
gcloud iam service-accounts create ${SERVICE_ACCT} \
    --description="service acct for requests to chronicle API" \
    --display-name="${SERVICE_ACCT}"
```

Allow yourself the permission to impersonate the new service account.
https://cloud.google.com/docs/authentication/rest#impersonated-sa

```shell
gcloud iam service-accounts add-iam-policy-binding \
    ${SERVICE_ACCT_FULL} \
    --member="user:${CURRENT_USER}" \
    --role="roles/iam.serviceAccountTokenCreator"
```

Following the principle of least privilege, create a custom IAM role with the
minimum permissions needed for the service account to carry out API operations.
Here the custom role simply has the ```chronicle.feeds.get``` permission.


```shell
gcloud iam roles create ${IAM_CUSTOM_ROLE_ID} \
  --project=${PROJECT_ID} \
  --title="${IAM_CUSTOM_ROLE_ID}" \
  --description="${IAM_CUSTOM_ROLE_ID}" \
  --permissions="chronicle.feeds.get" \
  --stage=GA
```

TODO::Grant the service account the new custom IAM role

```shell
gcloud projects add-iam-policy-binding ${PROJECT_ID} \
  --member="serviceAccount:${SERVICE_ACCT_FULL}" \
  --role="projects/${PROJECT_ID}/roles/${IAM_CUSTOM_ROLE_ID}"
```

TODO:: Grant the service account a pre-defined IAM roles from Google [SecOps](https://cloud.google.com/chronicle/docs/onboard/configure-feature-access)

```shell
gcloud projects add-iam-policy-binding ${PROJECT_ID} \
    --member="serviceAccount:${SERVICE_ACCT_FULL}" \
    --role="roles/chronicle.viewer"
```

## 4. Impersonating a service account to request a Chronicle API using curl

TODO::


## 5. Using the service account in a locally executing Java program

TODO::

## 6. Using the service account in a Java program running on Google Cloud services

TODO:: 





