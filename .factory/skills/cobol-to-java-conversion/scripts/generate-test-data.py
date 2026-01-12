#!/usr/bin/env python3
"""
Test Data Generation Script for COBOL to Java Conversion Validation

This script generates test data based on COBOL copybook structures
to help validate conversion correctness.
"""

import argparse
import json
import random
import string
from datetime import datetime, timedelta
from decimal import Decimal

def generate_account_data(num_records=100):
    """Generate sample account data"""
    accounts = []
    
    for i in range(num_records):
        account = {
            "accountId": f"{random.randint(1000000000000000, 9999999999999999)}",
            "accountName": f"Test Account {i+1}",
            "status": random.choice(["A", "I", "C"]),
            "balance": str(Decimal(random.uniform(0, 50000)).quantize(Decimal('0.01'))),
            "creditLimit": str(Decimal(random.uniform(1000, 25000)).quantize(Decimal('0.01'))),
            "openDate": (datetime.now() - timedelta(days=random.randint(1, 3650))).strftime("%Y-%m-%d")
        }
        accounts.append(account)
    
    return accounts

def generate_transaction_data(num_records=500):
    """Generate sample transaction data"""
    transactions = []
    
    for i in range(num_records):
        transaction = {
            "transactionId": f"TXN{datetime.now().strftime('%Y%m%d')}{i:06d}",
            "accountId": f"{random.randint(1000000000000000, 9999999999999999)}",
            "transactionType": random.choice(["DB", "CR", "PY", "FE"]),
            "amount": str(Decimal(random.uniform(1, 5000)).quantize(Decimal('0.01'))),
            "transactionDate": (datetime.now() - timedelta(days=random.randint(0, 90))).strftime("%Y-%m-%d"),
            "description": f"Test transaction {i+1}"
        }
        transactions.append(transaction)
    
    return transactions

def generate_customer_data(num_records=100):
    """Generate sample customer data"""
    customers = []
    
    for i in range(num_records):
        customer = {
            "custId": f"{i+1:09d}",
            "custName": f"Customer {i+1}",
            "firstName": random.choice(["John", "Jane", "Bob", "Alice", "Charlie"]),
            "lastName": random.choice(["Smith", "Johnson", "Williams", "Brown", "Jones"]),
            "address": f"{random.randint(1, 9999)} Main St",
            "city": random.choice(["New York", "Los Angeles", "Chicago", "Houston", "Phoenix"]),
            "state": random.choice(["NY", "CA", "IL", "TX", "AZ"]),
            "zipCode": f"{random.randint(10000, 99999):05d}",
            "phoneNumber": f"{random.randint(200, 999)}-{random.randint(200, 999)}-{random.randint(1000, 9999)}"
        }
        customers.append(customer)
    
    return customers

def save_to_json(data, filename):
    """Save data to JSON file"""
    with open(filename, 'w') as f:
        json.dump(data, f, indent=2)
    print(f"Generated {len(data)} records → {filename}")

def save_to_csv(data, filename):
    """Save data to CSV file"""
    import csv
    
    if not data:
        return
    
    with open(filename, 'w', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=data[0].keys())
        writer.writeheader()
        writer.writerows(data)
    
    print(f"Generated {len(data)} records → {filename}")

def main():
    parser = argparse.ArgumentParser(
        description='Generate test data for COBOL to Java conversion validation'
    )
    parser.add_argument('--output', '-o', default='test-data',
                       help='Output directory (default: test-data)')
    parser.add_argument('--format', '-f', choices=['json', 'csv'], default='json',
                       help='Output format (default: json)')
    parser.add_argument('--accounts', type=int, default=100,
                       help='Number of account records (default: 100)')
    parser.add_argument('--transactions', type=int, default=500,
                       help='Number of transaction records (default: 500)')
    parser.add_argument('--customers', type=int, default=100,
                       help='Number of customer records (default: 100)')
    
    args = parser.parse_args()
    
    import os
    os.makedirs(args.output, exist_ok=True)
    
    print("Generating test data...")
    print("=" * 50)
    
    # Generate data
    accounts = generate_account_data(args.accounts)
    transactions = generate_transaction_data(args.transactions)
    customers = generate_customer_data(args.customers)
    
    # Save data
    if args.format == 'json':
        save_to_json(accounts, f"{args.output}/accounts.json")
        save_to_json(transactions, f"{args.output}/transactions.json")
        save_to_json(customers, f"{args.output}/customers.json")
    else:
        save_to_csv(accounts, f"{args.output}/accounts.csv")
        save_to_csv(transactions, f"{args.output}/transactions.csv")
        save_to_csv(customers, f"{args.output}/customers.csv")
    
    print("=" * 50)
    print(f"Test data generation complete!")
    print(f"Total records: {args.accounts + args.transactions + args.customers}")

if __name__ == '__main__':
    main()
