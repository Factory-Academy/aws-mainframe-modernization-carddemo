#!/bin/bash
cobc -I ../cpy/ -T test.txt -tsymbols --std=ibm-strict sample/cobol/SIZING.cbl 