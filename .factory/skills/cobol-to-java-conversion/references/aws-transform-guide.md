# AWS Transform Guide

Quick reference for using AWS Transform for COBOL modernization.

## What is AWS Transform?

AWS Transform is an AI-powered service that automates mainframe modernization by:
- Analyzing legacy code and dependencies
- Generating documentation
- Providing refactoring suggestions
- Creating test cases
- Accelerating migration from COBOL to Java

## Key Features

1. **Automated Analysis:** Dependency mapping, complexity analysis
2. **Documentation Generation:** AI-powered code documentation
3. **Refactoring:** Automated COBOL to Java conversion
4. **Testing:** Test case generation and validation
5. **Reimagine:** Modern architecture recommendations

## Getting Started

### Prerequisites

- AWS Account
- AWS CLI configured
- COBOL source code
- S3 bucket for artifacts

### Setup

```bash
# Install/update AWS CLI
pip install --upgrade awscli

# Configure credentials
aws configure
```

## Workflow

### 1. Upload Source Code

```bash
# Create S3 bucket
aws s3 mb s3://my-carddemo-modernization

# Upload COBOL sources
aws s3 sync app/cbl/ s3://my-carddemo-modernization/source/ \
  --exclude "*.md" --exclude "*.txt"

# Upload copybooks
aws s3 sync app/cpy/ s3://my-carddemo-modernization/copybooks/
```

### 2. Create Transform Project

```bash
# Create project
aws transform create-project \
  --name carddemo-modernization \
  --source-location s3://my-carddemo-modernization/source/ \
  --target-language Java \
  --framework SpringBoot
```

### 3. Run Analysis

```bash
# Get project ID from previous command
PROJECT_ID="12345678-1234-1234-1234-123456789012"

# Start analysis
aws transform start-analysis \
  --project-id $PROJECT_ID

# Check status
aws transform get-analysis-status \
  --project-id $PROJECT_ID
```

### 4. Review Analysis Results

```bash
# Get analysis results
aws transform get-analysis \
  --project-id $PROJECT_ID \
  --output json > analysis-results.json

# Download dependency graph
aws transform get-dependency-graph \
  --project-id $PROJECT_ID \
  --output-file dependency-graph.png
```

### 5. Start Refactoring

```bash
# Start automated refactoring
aws transform start-refactoring \
  --project-id $PROJECT_ID \
  --programs "COSGN00C,COMEN01C,COACT01C"

# Monitor progress
aws transform get-refactoring-status \
  --project-id $PROJECT_ID
```

### 6. Download Converted Code

```bash
# Download Java code
aws s3 sync s3://my-carddemo-modernization/output/java/ ./java-output/

# Review generated code
ls -la ./java-output/
```

## Analysis Output

AWS Transform provides:

### Dependency Map
- COPY relationships
- CALL relationships
- File dependencies
- Database access patterns

### Complexity Metrics
- Cyclomatic complexity
- Lines of code
- Number of branches
- Estimated effort

### Modernization Plan
- Recommended migration order
- Risk assessment
- Effort estimates
- Technology recommendations

## Best Practices

### 1. Organize Source Code

```
s3://bucket/
├── source/
│   ├── cics/          # Online programs
│   ├── batch/         # Batch programs
│   └── common/        # Shared modules
├── copybooks/         # Data structures
├── jcl/              # Job control
└── schemas/          # Database schemas
```

### 2. Run Incremental Analysis

Start with a small subset to validate:

```bash
# Analyze one program first
aws transform start-analysis \
  --project-id $PROJECT_ID \
  --programs "COSGN00C"
```

### 3. Review Before Full Conversion

- Check dependency map for accuracy
- Validate complexity metrics
- Review suggested architecture
- Verify test case coverage

### 4. Use Tags for Organization

```bash
aws transform tag-resource \
  --resource-arn arn:aws:transform:us-east-1:123456789012:project/$PROJECT_ID \
  --tags Key=Application,Value=CardDemo Key=Environment,Value=Dev
```

## Integration with CI/CD

### GitHub Actions Example

```yaml
name: COBOL Modernization

on:
  push:
    paths:
      - 'app/cbl/**'

jobs:
  modernize:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      
      - name: Configure AWS
        uses: aws-actions/configure-aws-credentials@v1
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: us-east-1
      
      - name: Upload to S3
        run: |
          aws s3 sync app/cbl/ s3://${{ secrets.S3_BUCKET }}/source/
      
      - name: Run Analysis
        run: |
          aws transform start-analysis --project-id ${{ secrets.PROJECT_ID }}
```

## Cost Optimization

- Use incremental analysis (analyze only changed files)
- Delete old projects when done
- Use S3 lifecycle policies to archive outputs
- Tag resources for cost tracking

## Limitations

- Not all COBOL constructs may be supported
- Manual review still required
- Generated code may need refactoring
- Performance tuning needed post-conversion

## Resources

- [AWS Transform Documentation](https://docs.aws.amazon.com/transform/)
- [AWS Transform CLI Reference](https://docs.aws.amazon.com/cli/latest/reference/transform/)
- [AWS Mainframe Modernization](https://aws.amazon.com/mainframe-modernization/)
- [AWS Blu Age](https://aws.amazon.com/mainframe-modernization/capabilities/refactor/)

## Support

- AWS Support Center
- AWS Forums: re:Post
- AWS Account Team

## Example: Complete Project Workflow

```bash
#!/bin/bash

# Complete AWS Transform workflow
PROJECT_NAME="carddemo-modernization"
BUCKET="s3://my-modernization-bucket"
REGION="us-east-1"

# 1. Upload source
echo "Uploading source code..."
aws s3 sync app/cbl/ $BUCKET/source/

# 2. Create project
echo "Creating Transform project..."
PROJECT_ID=$(aws transform create-project \
  --name $PROJECT_NAME \
  --source-location $BUCKET/source/ \
  --target-language Java \
  --framework SpringBoot \
  --region $REGION \
  --query 'ProjectId' \
  --output text)

echo "Project ID: $PROJECT_ID"

# 3. Start analysis
echo "Starting analysis..."
aws transform start-analysis --project-id $PROJECT_ID

# 4. Wait for completion
echo "Waiting for analysis to complete..."
while true; do
  STATUS=$(aws transform get-analysis-status \
    --project-id $PROJECT_ID \
    --query 'Status' \
    --output text)
  
  echo "Status: $STATUS"
  
  if [ "$STATUS" == "COMPLETED" ]; then
    break
  fi
  
  sleep 30
done

# 5. Download results
echo "Downloading results..."
aws s3 sync $BUCKET/output/ ./transform-output/

echo "Complete! Check ./transform-output/ for results."
```
