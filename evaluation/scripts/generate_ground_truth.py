#!/usr/bin/env python3
"""
Generates 100 frozen synthetic benchmark evaluation queries for ResolveIQ.
Covers 20 knowledge articles, various intents, difficulty levels, and edge cases.
"""

import json
import os

TEMPLATES = [
    # KB-101: Password reset & 2FA
    ("How do I reset my password if I lost my phone authenticator?", "ACCOUNT", "password_reset", "HIGH", ["KB-101"]),
    ("Locked out of account because MFA code is not working", "ACCOUNT", "password_reset", "HIGH", ["KB-101"]),
    ("Need 2FA reset link for locked agent account", "ACCOUNT", "password_reset", "MEDIUM", ["KB-101"]),
    ("Authenticator app deleted, how to bypass two factor authentication?", "ACCOUNT", "password_reset", "HIGH", ["KB-101"]),
    ("Forgot account password and recovery email not arriving", "ACCOUNT", "password_reset", "MEDIUM", ["KB-101"]),

    # KB-102: API Authentication & JWT
    ("API returning 401 Unauthorized for valid JWT bearer token", "TECHNICAL", "api_troubleshooting", "HIGH", ["KB-102"]),
    ("How often do JWT access tokens expire in ResolveIQ?", "TECHNICAL", "api_troubleshooting", "LOW", ["KB-102"]),
    ("Where can I generate a machine to machine API key?", "TECHNICAL", "api_troubleshooting", "MEDIUM", ["KB-102"]),
    ("Rotating refresh token failed with invalid signature", "TECHNICAL", "api_troubleshooting", "HIGH", ["KB-102"]),
    ("API key permission denied when calling v1 endpoints", "TECHNICAL", "api_troubleshooting", "MEDIUM", ["KB-102"]),

    # KB-103: Subscription & Seat Limits
    ("Reached maximum seat limit on our enterprise plan, how to add 5 more?", "BILLING", "subscription_change", "MEDIUM", ["KB-103"]),
    ("How are pro-rated charges calculated when upgrading agent seats?", "BILLING", "subscription_change", "LOW", ["KB-103"]),
    ("Want to downgrade subscription tier at the end of billing cycle", "BILLING", "subscription_change", "LOW", ["KB-103"]),
    ("Error adding new team member due to seat quota limit", "BILLING", "subscription_change", "MEDIUM", ["KB-103"]),
    ("Need quote for additional 20 agent licenses on Pro plan", "BILLING", "subscription_change", "LOW", ["KB-103"]),

    # KB-104: Payment Reconciliation & Duplicate Charges
    ("Noticed duplicate charge on credit card for invoice INV-9812", "BILLING", "billing_dispute", "HIGH", ["KB-104"]),
    ("Charged twice for monthly subscription due to timeout", "BILLING", "billing_dispute", "HIGH", ["KB-104"]),
    ("Credit card shows two pending charges for the same transaction", "BILLING", "billing_dispute", "HIGH", ["KB-104"]),
    ("Requesting refund for double billing on Visa card", "BILLING", "billing_dispute", "HIGH", ["KB-104"]),
    ("Payment gateway timeout caused duplicate payment capture", "BILLING", "billing_dispute", "HIGH", ["KB-104"]),

    # KB-105: Enterprise SSO & SAML
    ("Okta SAML 2.0 login failing with 401 signature validation error", "TECHNICAL", "sso_troubleshooting", "CRITICAL", ["KB-105"]),
    ("Azure AD SSO certificate expired, users cannot login", "TECHNICAL", "sso_troubleshooting", "CRITICAL", ["KB-105"]),
    ("Mismatched Entity ID error in PingFederate SAML assertion", "TECHNICAL", "sso_troubleshooting", "HIGH", ["KB-105"]),
    ("How to update SP metadata certificate in tenant settings?", "TECHNICAL", "sso_troubleshooting", "MEDIUM", ["KB-105"]),
    ("Single sign on redirection loop after IdP certificate update", "TECHNICAL", "sso_troubleshooting", "CRITICAL", ["KB-105"]),

    # KB-106: International Wire & SWIFT
    ("Need SWIFT routing code and field 70 reference for international wire", "BILLING", "payment_inquiry", "MEDIUM", ["KB-106"]),
    ("Sent international wire transfer 5 days ago but invoice still unpaid", "BILLING", "payment_inquiry", "HIGH", ["KB-106"]),
    ("Where to find MT103 wire transfer instructions for invoice payment?", "BILLING", "payment_inquiry", "LOW", ["KB-106"]),
    ("Wire payment returned by beneficiary bank due to missing reference", "BILLING", "payment_inquiry", "HIGH", ["KB-106"]),
    ("How long does international wire payment take to reconcile?", "BILLING", "payment_inquiry", "LOW", ["KB-106"]),

    # KB-107: GDPR & Privacy
    ("Customer requesting complete GDPR Article 17 account data erasure", "ACCOUNT", "privacy_request", "MEDIUM", ["KB-107"]),
    ("How do I submit a right to be forgotten privacy request for user?", "ACCOUNT", "privacy_request", "LOW", ["KB-107"]),
    ("Are vector embeddings purged when a customer requests PII deletion?", "ACCOUNT", "privacy_request", "LOW", ["KB-107"]),
    ("Export all personal ticket history and user data under GDPR", "ACCOUNT", "privacy_request", "LOW", ["KB-107"]),
    ("Verify redaction status of customer personal identification records", "ACCOUNT", "privacy_request", "LOW", ["KB-107"]),

    # KB-108: Updating Tax IDs & Invoicing Entities
    ("Need to change EU VAT number and company address on future invoices", "ACCOUNT", "tax_update", "MEDIUM", ["KB-108"]),
    ("How to update corporate tax ID (EIN) in billing settings?", "ACCOUNT", "tax_update", "LOW", ["KB-108"]),
    ("Can I change the legal entity name on an already finalized invoice?", "ACCOUNT", "tax_update", "LOW", ["KB-108"]),
    ("Invoice shows incorrect UK VAT number after corporate restructuring", "ACCOUNT", "tax_update", "MEDIUM", ["KB-108"]),
    ("Where to enter reverse charge VAT exemption certificate?", "ACCOUNT", "tax_update", "LOW", ["KB-108"]),

    # KB-109: RBAC & Permissions
    ("How to assign Team Lead permissions to support supervisor?", "ACCOUNT", "access_control", "MEDIUM", ["KB-109"]),
    ("What are the 6 standard user roles available in ResolveIQ?", "ACCOUNT", "access_control", "LOW", ["KB-109"]),
    ("Auditor role cannot view governance logs, need permission grant", "ACCOUNT", "access_control", "MEDIUM", ["KB-109"]),
    ("Agent cannot see tickets assigned to other teams in queue", "ACCOUNT", "access_control", "LOW", ["KB-109"]),
    ("Restricting Knowledge Manager to draft authoring without publish rights", "ACCOUNT", "access_control", "MEDIUM", ["KB-109"]),

    # KB-110: Custom Domain & SSL
    ("Setting up custom branded domain support.mycompany.com with CNAME", "TECHNICAL", "domain_setup", "MEDIUM", ["KB-110"]),
    ("SSL certificate failed to provision automatically for custom portal", "TECHNICAL", "domain_setup", "HIGH", ["KB-110"]),
    ("How long does Let's Encrypt SSL issuance take after DNS change?", "TECHNICAL", "domain_setup", "LOW", ["KB-110"]),
    ("Custom portal domain showing invalid SSL certificate error", "TECHNICAL", "domain_setup", "HIGH", ["KB-110"]),
    ("CNAME verification failing for branded customer support subdomain", "TECHNICAL", "domain_setup", "MEDIUM", ["KB-110"]),

    # KB-111: Order Cancellation & RMA
    ("Cancel order placed 20 minutes ago before warehouse dispatch", "DELIVERY", "order_cancellation", "HIGH", ["KB-111"]),
    ("How to generate an RMA return shipping label for damaged item?", "DELIVERY", "order_cancellation", "MEDIUM", ["KB-111"]),
    ("Customer wants to return opened merchandise for full refund", "DELIVERY", "order_cancellation", "MEDIUM", ["KB-111"]),
    ("How many days after delivery can a return RMA be authorized?", "DELIVERY", "order_cancellation", "LOW", ["KB-111"]),
    ("Warehouse received RMA return, when will refund be processed?", "DELIVERY", "order_cancellation", "MEDIUM", ["KB-111"]),

    # KB-112: Missing Shipment & Carrier Investigation
    ("Tracking says package delivered today but it is missing from porch", "DELIVERY", "missing_shipment", "HIGH", ["KB-112"]),
    ("FedEx marked package as delivered with no signature or photo proof", "DELIVERY", "missing_shipment", "HIGH", ["KB-112"]),
    ("How to file a lost-in-transit carrier claim for order #ORD-4491?", "DELIVERY", "missing_shipment", "HIGH", ["KB-112"]),
    ("Package scan shows delivered to wrong address and coordinates", "DELIVERY", "missing_shipment", "HIGH", ["KB-112"]),
    ("Need urgent replacement sent for lost shipment marked delivered", "DELIVERY", "missing_shipment", "HIGH", ["KB-112"]),

    # KB-113: Freight & Customs
    ("Commercial freight shipment delayed at customs due to missing HS code", "DELIVERY", "customs_inquiry", "HIGH", ["KB-113"]),
    ("Who is responsible for paying import VAT and duties on international order?", "DELIVERY", "customs_inquiry", "MEDIUM", ["KB-113"]),
    ("Customs broker requesting commercial invoice documentation for parcel", "DELIVERY", "customs_inquiry", "HIGH", ["KB-113"]),
    ("International pallet stuck at port customs clearance checkpoint", "DELIVERY", "customs_inquiry", "HIGH", ["KB-113"]),
    ("Tariff code discrepancy holding up cross-border logistics delivery", "DELIVERY", "customs_inquiry", "HIGH", ["KB-113"]),

    # KB-114: Warehouse Fulfillment & Backorders
    ("Order fulfillment delayed due to out of stock warehouse inventory", "DELIVERY", "fulfillment_delay", "MEDIUM", ["KB-114"]),
    ("Can I request split shipment for items that are currently in stock?", "DELIVERY", "fulfillment_delay", "MEDIUM", ["KB-114"]),
    ("When will backordered items be replenished and shipped?", "DELIVERY", "fulfillment_delay", "LOW", ["KB-114"]),
    ("Order has been in processing state at warehouse for 4 business days", "DELIVERY", "fulfillment_delay", "HIGH", ["KB-114"]),
    ("Cancel backordered portion of order and refund to credit card", "DELIVERY", "fulfillment_delay", "MEDIUM", ["KB-114"]),

    # KB-115: Address Change & Rerouting
    ("Need to change delivery address on package that was shipped yesterday", "DELIVERY", "address_change", "HIGH", ["KB-115"]),
    ("How to submit an in-flight carrier intercept package reroute?", "DELIVERY", "address_change", "HIGH", ["KB-115"]),
    ("Typo in shipping address apartment number, package already on truck", "DELIVERY", "address_change", "HIGH", ["KB-115"]),
    ("Carrier intercept fee for rerouting shipment to new destination", "DELIVERY", "address_change", "LOW", ["KB-115"]),
    ("Can FedEx hold package at facility because customer moved?", "DELIVERY", "address_change", "MEDIUM", ["KB-115"]),

    # KB-116: Audit Logs & SIEM
    ("How to stream security audit events to Datadog or Splunk SIEM?", "TECHNICAL", "security_audit", "LOW", ["KB-116"]),
    ("What is the retention period for security audit logs in PostgreSQL?", "TECHNICAL", "security_audit", "LOW", ["KB-116"]),
    ("Export audit trail of all role privilege escalations for Q3 compliance", "TECHNICAL", "security_audit", "MEDIUM", ["KB-116"]),
    ("Webhook endpoint for streaming immutable security audit events", "TECHNICAL", "security_audit", "LOW", ["KB-116"]),
    ("Search audit logs for IP addresses associated with failed logins", "TECHNICAL", "security_audit", "MEDIUM", ["KB-116"]),

    # KB-117: IP Whitelisting
    ("How to configure CIDR block IP whitelisting for employee portal?", "TECHNICAL", "network_security", "MEDIUM", ["KB-117"]),
    ("Blocked from accessing portal due to IP whitelist restrictions", "TECHNICAL", "network_security", "HIGH", ["KB-117"]),
    ("Adding VPN subnet to authorized IP access list in security settings", "TECHNICAL", "network_security", "MEDIUM", ["KB-117"]),
    ("Emergency access procedure if administrator IP is accidentally blocked", "TECHNICAL", "network_security", "CRITICAL", ["KB-117"]),
    ("API returning 403 Forbidden for corporate office gateway IP", "TECHNICAL", "network_security", "HIGH", ["KB-117"]),

    # KB-118: SCIM Provisioning
    ("Setting up Okta SCIM 2.0 automated employee provisioning", "TECHNICAL", "scim_integration", "MEDIUM", ["KB-118"]),
    ("Deactivated user in Microsoft Entra ID still has active session", "TECHNICAL", "scim_integration", "HIGH", ["KB-118"]),
    ("SCIM endpoint returning error when syncing user department groups", "TECHNICAL", "scim_integration", "MEDIUM", ["KB-118"]),
    ("How does SCIM handle automatic token revocation upon user termination?", "TECHNICAL", "scim_integration", "LOW", ["KB-118"]),
    ("OneLogin SCIM synchronization configuration and bearer token setup", "TECHNICAL", "scim_integration", "MEDIUM", ["KB-118"]),

    # KB-119: Webhooks & Rate Limits
    ("Webhook receiver receiving HTTP 429 Too Many Requests rate limits", "TECHNICAL", "webhook_management", "HIGH", ["KB-119"]),
    ("What is the exponential backoff retry schedule for failed webhooks?", "TECHNICAL", "webhook_management", "LOW", ["KB-119"]),
    ("Webhook endpoint timed out after 5000ms and was placed in retry queue", "TECHNICAL", "webhook_management", "MEDIUM", ["KB-119"]),
    ("How to handle Retry-After header when receiving webhook bursts?", "TECHNICAL", "webhook_management", "LOW", ["KB-119"]),
    ("Webhook delivery failed with SSL handshake error on receiver", "TECHNICAL", "webhook_management", "HIGH", ["KB-119"]),

    # KB-120: Sandbox Environment
    ("How to create an isolated staging sandbox environment for testing?", "TECHNICAL", "sandbox_setup", "LOW", ["KB-120"]),
    ("Simulating ticket triage webhooks and mock AI responses in sandbox", "TECHNICAL", "sandbox_setup", "LOW", ["KB-120"]),
    ("Resetting test data in developer sandbox without affecting production", "TECHNICAL", "sandbox_setup", "LOW", ["KB-120"]),
    ("Can sandbox environments send real emails or trigger real charges?", "TECHNICAL", "sandbox_setup", "LOW", ["KB-120"]),
    ("Testing Stripe payment reconciliation webhook failure in sandbox", "TECHNICAL", "sandbox_setup", "LOW", ["KB-120"])
]

def generate_ground_truth():
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    output_path = os.path.join(base_dir, 'datasets', 'eval_ground_truth.json')

    dataset = []
    for query, category, intent, urgency, citations in TEMPLATES:
        dataset.append({
            "query": query,
            "category": category,
            "expected_intent": intent,
            "expected_urgency": urgency,
            "relevant_article_ids": citations,
            "min_expected_recall_at_5": 1.0
        })

    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(dataset, f, indent=2)

    print(f"Successfully generated {len(dataset)} benchmark evaluation samples in {output_path}")

if __name__ == "__main__":
    generate_ground_truth()
