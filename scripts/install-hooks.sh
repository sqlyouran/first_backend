#!/bin/bash
# Install git hooks for JaCoCo coverage gate
# Run this script once after cloning: ./scripts/install-hooks.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(dirname "$SCRIPT_DIR")"
HOOKS_SRC="$BACKEND_DIR/hooks"

# Detect git hooks directory (works for submodules too)
GIT_DIR=$(git rev-parse --git-dir)
HOOKS_DEST="$GIT_DIR/hooks"

# Create hooks directory if it doesn't exist
mkdir -p "$HOOKS_DEST"

echo "🔧 Installing git hooks..."
echo "   Source: $HOOKS_SRC"
echo "   Target: $HOOKS_DEST"
echo ""

# Copy pre-commit hook
if [ -f "$HOOKS_SRC/pre-commit" ]; then
    cp "$HOOKS_SRC/pre-commit" "$HOOKS_DEST/pre-commit"
    chmod +x "$HOOKS_DEST/pre-commit"
    echo "✅ pre-commit hook installed"
else
    echo "⚠️  pre-commit hook not found in $HOOKS_SRC"
fi

# Copy pre-push hook
if [ -f "$HOOKS_SRC/pre-push" ]; then
    cp "$HOOKS_SRC/pre-push" "$HOOKS_DEST/pre-push"
    chmod +x "$HOOKS_DEST/pre-push"
    echo "✅ pre-push hook installed"
else
    echo "⚠️  pre-push hook not found in $HOOKS_SRC"
fi

echo ""
echo "✅ Git hooks installed successfully!"
echo ""
echo "What these hooks do:"
echo "  • pre-commit: Runs fast tests (excludes @Tag slow)"
echo "  • pre-push:   Runs full test suite + JaCoCo check"
echo ""
echo "To bypass hooks temporarily: git commit --no-verify"
