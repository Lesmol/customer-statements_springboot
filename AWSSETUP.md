# AWS Setup

This service stores uploaded statement files in an S3 bucket. To run it locally (via `docker compose`) you need an S3
bucket and an IAM user with programmatic access to it.

## Quick start: one-click deploy

Skip steps 1-3 below by launching this CloudFormation stack, which creates the S3 bucket, a scoped IAM policy, and an
IAM user with an access key for you (region `af-south-1`):

[Launch Stack](https://af-south-1.console.aws.amazon.com/cloudformation/home?region=af-south-1#/stacks/quickcreate?templateURL=https%3A%2F%2Fcf-templates-h7u79i6ytuzc-af-south-1.s3.af-south-1.amazonaws.com%2F2026-07-30T173505.362Zlpi-springboot-local-dev-s3.yaml&stackName=springboot-s3-stack)

After the stack finishes creating, get the bucket name, access key ID, and secret access key from the stack's
**Outputs** tab, then skip ahead to [step 4](#4-configure-the-project) to configure the project.

## 1. Create an S3 bucket

1. Sign in to the [AWS Console](https://console.aws.amazon.com/) -> S3 -> **Create bucket**.
2. Choose a globally-unique bucket name and a region (the project defaults to `af-south-1`; any region works as long as
   it matches the `REGION` value you configure).
3. Keep **Block all public access** enabled; the app accesses objects via the AWS SDK and pre-signed URLs, so the bucket
   itself does not need to be public.
4. Leave the other defaults and create the bucket.

## 2. Create a policy scoped to the bucket

1. Go to IAM -> Policies -> **Create policy**.
2. Select the **JSON** policy editor and paste in the following, replacing `YOUR_BUCKET_NAME` with the bucket you
   created above:

   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Effect": "Allow",
         "Action": [
           "s3:PutObject",
           "s3:GetObject",
           "s3:DeleteObject"
         ],
         "Resource": "arn:aws:s3:::YOUR_BUCKET_NAME/*"
       }
     ]
   }
   ```

   The app uploads (`PutObject`), generates pre-signed download URLs (`GetObject`), and deletes objects on failed
   uploads (`DeleteObject`). No other S3 permissions are required.

3. Give it a descriptive name, e.g. `customer-statements-local-policy`, and select **Create policy**.

## 3. Create an IAM user with access to the bucket

1. Go to IAM -> IAM Users -> **Create user**.
2. Give it a descriptive name, e.g. `customer-statements-local`.
3. Skip console access - this user only needs programmatic (API) access.
4. Select **Attach policies directly**, find the policy you created above, and select it.
5. Select **Create user**.
6. Go to the new user's **Security credentials** tab -> **Create access key** -> choose *Application running outside
   AWS* -> create it, and note the **Access key ID** and **Secret access key**. The secret is only shown once.

## 4. Configure the project

Set the following in your project's `.env` file (see the main [README](README.md)):

```env
ACCESS_KEY=<the access key ID from step 3>
SECRET_KEY=<the secret access key from step 3>
REGION=<the bucket's region, e.g. af-south-1>
BUCKET_NAME=<the bucket name from step 1>
```

## Notes

- These credentials are only used locally by the app to sign requests to S3; nothing else in this AWS account is
  touched.
- Do not commit `.env` (`.env` is already git-ignored).
- If you rotate or revoke the access key, update `.env` and run `docker compose up --build`.

## Tearing it down

When you're done and want to remove the AWS resources created above (to avoid ongoing storage costs and unused
credentials lying around):

1. **Empty the bucket.** S3 buckets can't be deleted while they contain objects.
    - Console: S3 -> your bucket -> **Empty** -> confirm by typing the `permanently delete`.
    - CLI: `aws s3 rm s3://YOUR_BUCKET_NAME --recursive`
2. **Delete the bucket.**
    - Console: S3 -> your bucket -> **Delete** -> confirm by typing the bucket name.
    - CLI: `aws s3api delete-bucket --bucket YOUR_BUCKET_NAME --region YOUR_REGION`
3. **Delete the access key.**
    - IAM -> Users -> your user -> **Security credentials** -> find the access key -> **Actions** -> **Deactivate** -> **Delete**.
4. **Delete the IAM user and policy.**
    - IAM -> Users -> select your user -> **Delete**.
    - IAM -> Policies -> find the policy you created -> **Delete**.
