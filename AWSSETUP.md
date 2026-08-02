# AWS Setup

This service stores uploaded statement files in an S3 bucket. To run it locally (via `docker compose`) you need an S3 bucket and an IAM user with programmatic access to it.

## 1. Create an S3 bucket

1. Sign in to the [AWS Console](https://console.aws.amazon.com/) -> S3 -> **Create bucket**.
2. Choose a globally-unique bucket name and a region (the project defaults to `af-south-1`; any region works as long as it matches the `REGION` value you configure).
3. Keep **Block all public access** enabled — the app accesses objects via the AWS SDK and pre-signed URLs, so the bucket itself does not need to be public.
4. Leave the other defaults and create the bucket.

## 2. Create an IAM user with access to the bucket

1. Go to IAM -> Users -> **Create user**.
2. Give it a descriptive name, e.g. `customer-statements-local`.
3. Skip console access — this user only needs programmatic (API) access.
4. Attach a permissions policy scoped to the bucket you created. You can attach an inline policy like:

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

   The app uploads (`PutObject`), generates pre-signed download URLs (`GetObject`), and deletes objects on failed uploads (`DeleteObject`) — no other S3 permissions are required.

5. After creating the user, go to its **Security credentials** tab -> **Create access key** -> choose *Application running outside AWS* (or *Local code*) -> create it, and note the **Access key ID** and **Secret access key**. The secret is only shown once.

## 3. Configure the project

Set the following in your project's `.env` file (see the main [README](README.md)):

```env
ACCESS_KEY=<the access key ID from step 2>
SECRET_KEY=<the secret access key from step 2>
REGION=<the bucket's region, e.g. af-south-1>
BUCKET_NAME=<the bucket name from step 1>
```

## Notes

- These credentials are only used locally by the app to sign requests to S3 — nothing else in this AWS account is touched.
- Do not commit `.env` or credentials to git (`.env` is already git-ignored).
- If you rotate or revoke the access key, update `.env` and restart `docker compose`.

## Tearing it down

When you're done and want to remove the AWS resources created above (to avoid ongoing storage costs and unused credentials lying around):

1. **Empty the bucket.** S3 buckets can't be deleted while they contain objects.
   - Console: S3 -> your bucket -> **Empty** -> confirm by typing the bucket name.
   - CLI: `aws s3 rm s3://YOUR_BUCKET_NAME --recursive`
2. **Delete the bucket.**
   - Console: S3 -> your bucket -> **Delete** -> confirm by typing the bucket name.
   - CLI: `aws s3api delete-bucket --bucket YOUR_BUCKET_NAME --region YOUR_REGION`
3. **Delete the access key.**
   - IAM -> Users -> your user -> **Security credentials** -> find the access key -> **Actions** -> **Delete**.
4. **Delete the inline policy and the IAM user.**
   - IAM -> Users -> your user -> **Permissions** tab -> remove the inline policy (if not removed automatically with the user).
   - IAM -> Users -> select your user -> **Delete**.
