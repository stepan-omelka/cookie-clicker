import base64
import json
import os

import functions_framework
from google.cloud import billing_v1

PROJECT_NAME = f"projects/{os.environ['TARGET_PROJECT']}"
KILL_AT = float(os.environ.get("KILL_AT", "0.01"))


@functions_framework.cloud_event
def stop_billing(cloud_event):
    budget = json.loads(base64.b64decode(cloud_event.data["message"]["data"]))
    cost = float(budget["costAmount"])
    if cost < KILL_AT:
        print(f"Cost {cost} {budget.get('currencyCode', '')} is below {KILL_AT}, billing stays on")
        return

    client = billing_v1.CloudBillingClient()
    if not client.get_project_billing_info(name=PROJECT_NAME).billing_enabled:
        print(f"Billing for {PROJECT_NAME} is already disabled")
        return

    # unlinking the billing account stops every paid service in the project
    client.update_project_billing_info(
        name=PROJECT_NAME,
        project_billing_info=billing_v1.ProjectBillingInfo(billing_account_name=""),
    )
    print(f"Cost {cost} reached {KILL_AT}, billing disabled for {PROJECT_NAME}")
