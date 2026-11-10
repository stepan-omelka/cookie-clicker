# Deploying to Firebase Hosting + Cloud Run

```
browser ──► Firebase Hosting (Angular files on a CDN)
               └── /api/** ──► Cloud Run: cookie-clicker-api (Spring Boot, europe-west1)
                                   └──► Neon PostgreSQL (free plan, no card)
```

Hosting forwards `/api/**` to Cloud Run, so the browser talks to a single origin and no CORS is needed.

## What it costs

Normal use costs nothing. Every part stays inside a free tier:

| Service | Free per month |
|---|---|
| Cloud Run | 180,000 vCPU-seconds, 360,000 GiB-seconds, 2M requests |
| Firebase Hosting | 10 GB storage, 360 MB transfer per day |
| Artifact Registry | 0.5 GB storage |
| Neon | 1 GB storage, 100 compute hours (separate company, no card) |

Cloud Run needs the Blaze plan, which means a card on file. Google Cloud has no switch that guarantees exactly 0 at every moment: Google says spend caps are not instant and that usage during the delay is billed. This setup therefore stacks four layers, so that reaching a bill at all needs deliberate abuse, and even then it stays at a few cents:

| Layer | What it covers | Reacts |
|---|---|---|
| 1. Hard limits in the config | At most 1 Cloud Run instance with 512 MiB, billed only while handling requests, 30 s timeout. Images are built on your machine (no Cloud Build). The registry keeps only the 2 newest images. | always |
| 2. Cloud Run spend cap | Native Google cap on Cloud Run, set to 1 USD. New requests are paused once it's reached. | within minutes |
| 3. Kill switch | Everything in the project (Hosting, registry, network). A budget sends the net cost to a small function, which unlinks billing once it reaches 0.01. | hours (billing data lags) |
| 4. Budget emails | You get an email at 50, 90 and 100 % of 1 USD. | hours |

The spend cap counts gross cost before credits, and 1 USD of Cloud Run usage is far below its free tier. If Google counts the free tier as a credit, Cloud Run pauses while it is still free. If it doesn't, the kill switch catches the first cent.

When the kill switch fires, billing is unlinked from the project:
- Cloud Run stops, so the game is unavailable.
- Firebase falls back to the Spark plan, whose limits simply stop instead of charging.
- The game data lives in Neon, so nothing is lost.
- Google warns that resources in a project without billing may be deleted later. Here that only means the container image, which `deploy.sh` rebuilds.

The spend cap resets on the 1st of every month.

## One-time setup

1. **Tools.** Install the CLIs below, keep Docker Desktop running, then log in with `gcloud auth login` and `firebase login`.

   ```bash
   brew install --cask google-cloud-sdk
   ```

   ```bash
   npm install -g firebase-tools
   ```

2. **Neon.** Create a project in the AWS Europe Central (Frankfurt) region. Open *Connect*, turn **off** connection pooling and copy host, database, user and password into `deploy/cloudrun.env.yaml`:

   ```bash
   cp deploy/cloudrun.env.example.yaml deploy/cloudrun.env.yaml
   ```

   The URL has the form `jdbc:postgresql://<host>/<database>?sslmode=require`. The file is ignored by git.

3. **Firebase.** In the [Firebase console](https://console.firebase.google.com) create a project and upgrade it to **Blaze**. Then export its project ID:

   ```bash
   export PROJECT_ID=your-project-id
   ```

4. **Cloud Run spend cap.** It can only be created in the console:
   1. In the [Google Cloud console](https://console.cloud.google.com/billing/budgets), open *Billing → Budgets & alerts* and click *Create budget*.
   2. Choose **Spend cap enforcement** and name it `cloud run cap`.
   3. For scope, pick your project and the service **Cloud Run**.
   4. Set the amount to **1 USD**, or the same amount in your billing currency, and click *Finish*.

5. **Infrastructure and kill switch.** This enables the APIs, creates the image registry with its cleanup policy, deploys the kill switch and creates the 1 USD budget that feeds it. It takes a few minutes.

   ```bash
   ./deploy/setup.sh
   ```

6. **Check the kill switch is wired** without triggering it. Publish a fake notification with zero cost:

   ```bash
   gcloud pubsub topics publish billing-kill-switch --project "$PROJECT_ID" --message '{"costAmount": 0, "budgetAmount": 1, "currencyCode": "USD"}'
   ```

   ```bash
   gcloud functions logs read billing-kill-switch --region europe-west1 --project "$PROJECT_ID" --limit 5
   ```

   The log should say `billing stays on`. A message with a cost of 0.01 or more really disables billing.

## Deploy

```bash
./deploy/deploy.sh
```

The script builds the backend image on your machine and pushes it to the registry. It then deploys Cloud Run with the limits above, builds Angular and deploys Firebase Hosting. The game runs at `https://<project-id>.web.app`.

The first request after a quiet period starts Cloud Run, which takes about 10 seconds.

## After the kill switch fired

1. Check what cost money in *Billing → Reports*.
2. Fix the cause.
3. Link billing again and redeploy:

   ```bash
   gcloud billing projects link "$PROJECT_ID" --billing-account <account-id>
   ```

   ```bash
   ./deploy/deploy.sh
   ```

## Things to know

- There is one shared save and the API is public, so anyone can reset the game. Clicks are rate limited on the server.
- To remove everything on Google's side, delete the project. Neon is deleted separately in its console.

  ```bash
  gcloud projects delete "$PROJECT_ID"
  ```
