#!/bin/bash

# COBOL Program Analysis Script
# Analyzes COBOL source files to identify dependencies, file I/O, and complexity

if [ $# -eq 0 ]; then
    echo "Usage: $0 <cobol-file>"
    echo "Example: $0 app/cbl/COSGN00C.cbl"
    exit 1
fi

COBOL_FILE="$1"

if [ ! -f "$COBOL_FILE" ]; then
    echo "Error: File '$COBOL_FILE' not found"
    exit 1
fi

echo "========================================="
echo "COBOL Program Analysis"
echo "File: $COBOL_FILE"
echo "========================================="
echo ""

# Extract PROGRAM-ID
echo "Program ID:"
grep -i "PROGRAM-ID" "$COBOL_FILE" | head -1
echo ""

# Find COPY statements (copybooks)
echo "Copybooks (COPY statements):"
grep -i "COPY" "$COBOL_FILE" | grep -v "^\*" | sed 's/^[ \t]*//'
echo ""

# Find CALL statements
echo "External Calls (CALL statements):"
grep -i "CALL" "$COBOL_FILE" | grep -v "^\*" | sed 's/^[ \t]*//'
echo ""

# Find CICS commands
echo "CICS Commands:"
grep -i "EXEC CICS" "$COBOL_FILE" | grep -v "^\*" | sed 's/^[ \t]*//' | sort | uniq -c
echo ""

# Find file operations
echo "File I/O Operations:"
grep -iE "EXEC CICS (READ|WRITE|REWRITE|DELETE|STARTBR|READNEXT|ENDBR)" "$COBOL_FILE" | \
    grep -v "^\*" | sed 's/^[ \t]*//' | cut -d' ' -f3 | sort | uniq -c
echo ""

# Find SELECT statements (batch files)
echo "File Control (SELECT statements):"
grep -i "SELECT" "$COBOL_FILE" | grep -i "ASSIGN TO" | grep -v "^\*" | sed 's/^[ \t]*//'
echo ""

# Count PERFORM statements (complexity indicator)
PERFORM_COUNT=$(grep -i "PERFORM" "$COBOL_FILE" | grep -v "^\*" | wc -l)
echo "Complexity Indicators:"
echo "  PERFORM statements: $PERFORM_COUNT"

# Count IF statements
IF_COUNT=$(grep -iE "^\s*IF " "$COBOL_FILE" | grep -v "^\*" | wc -l)
echo "  IF statements: $IF_COUNT"

# Count EVALUATE statements
EVALUATE_COUNT=$(grep -i "EVALUATE" "$COBOL_FILE" | grep -v "^\*" | wc -l)
echo "  EVALUATE statements: $EVALUATE_COUNT"
echo ""

# Find REDEFINES (data structure complexity)
echo "Data Structure Complexity:"
REDEFINES_COUNT=$(grep -i "REDEFINES" "$COBOL_FILE" | grep -v "^\*" | wc -l)
echo "  REDEFINES clauses: $REDEFINES_COUNT"

OCCURS_COUNT=$(grep -i "OCCURS" "$COBOL_FILE" | grep -v "^\*" | wc -l)
echo "  OCCURS clauses: $OCCURS_COUNT"

COMP3_COUNT=$(grep -i "COMP-3" "$COBOL_FILE" | grep -v "^\*" | wc -l)
echo "  COMP-3 fields: $COMP3_COUNT"
echo ""

# Identify SQL (DB2) usage
SQL_COUNT=$(grep -i "EXEC SQL" "$COBOL_FILE" | grep -v "^\*" | wc -l)
if [ $SQL_COUNT -gt 0 ]; then
    echo "Database Access (DB2):"
    echo "  SQL statements: $SQL_COUNT"
    echo ""
fi

# Count lines of code (excluding comments and blank lines)
LOC=$(grep -v "^\*" "$COBOL_FILE" | grep -v "^$" | wc -l)
echo "Lines of Code (excluding comments): $LOC"
echo ""

echo "========================================="
echo "Conversion Recommendations:"
echo "========================================="

# Provide recommendations based on analysis
if grep -qi "EXEC CICS" "$COBOL_FILE"; then
    echo "✓ CICS Program detected → Convert to Spring Boot REST Controller"
    echo "  See: mappings/cics-to-spring.md"
fi

if grep -qi "SELECT.*ASSIGN TO" "$COBOL_FILE"; then
    echo "✓ Batch Program detected → Convert to Spring Batch Job"
    echo "  See: examples/batch-conversion-example.md"
fi

if [ $COMP3_COUNT -gt 0 ]; then
    echo "⚠ COMP-3 fields detected → Use BigDecimal in Java"
    echo "  See: mappings/data-types.md"
fi

if [ $REDEFINES_COUNT -gt 0 ]; then
    echo "⚠ REDEFINES detected → Choose table inheritance or JSON strategy"
    echo "  See: mappings/vsam-to-sql.md"
fi

if [ $SQL_COUNT -gt 0 ]; then
    echo "✓ DB2 SQL detected → Convert to JPA/JDBC"
fi

if [ $OCCURS_COUNT -gt 0 ]; then
    echo "⚠ OCCURS (arrays) detected → Convert to child tables or collections"
fi

echo ""
echo "Analysis complete!"
