#!/usr/bin/env bash
# Wrapper to invoke PowerShell seed script on Windows environments
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
powershell.exe -ExecutionPolicy Bypass -File "$SCRIPT_DIR/seed-demo-data.ps1" "$@"
