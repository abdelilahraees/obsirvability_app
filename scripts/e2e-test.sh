#!/usr/bin/env bash
#
# End-to-end smoke test for observability_app.
# Registers a user, adds two products to the cart, checks out.
#
# Usage:
#   ./e2e-test.sh <email>              # register + full scenario
#   ./e2e-test.sh <email> <password>   # password default = "pass1234"
#
# Requires: curl, jq

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8079}"
EMAIL="${1:-}"
PASSWORD="${2:-pass1234}"

if [[ -z "$EMAIL" ]]; then
    echo "usage: $0 <email> [password]" >&2
    exit 1
fi

if ! command -v jq >/dev/null 2>&1; then
    echo "error: jq is required (sudo apt install jq)" >&2
    exit 1
fi

# --- helpers ---
c_cyan="\033[1;36m"; c_green="\033[1;32m"; c_red="\033[1;31m"; c_dim="\033[2m"; c_off="\033[0m"

step() { echo -e "\n${c_cyan}==> $*${c_off}"; }
ok()   { echo -e "${c_green}ok${c_off} $*"; }
die()  { echo -e "${c_red}fail${c_off} $*" >&2; exit 1; }

HTTP_CODE=""
BODY=""

http() {
    # http METHOD PATH [DATA] -> populates $BODY and $HTTP_CODE (globals)
    local method="$1" path="$2" data="${3:-}"
    local args=(-sS -o /tmp/e2e.body -w "%{http_code}" -X "$method" "$BASE_URL$path"
                -H 'Content-Type: application/json')
    [[ -n "${TOKEN:-}" ]] && args+=(-H "Authorization: Bearer $TOKEN")
    [[ -n "$data"      ]] && args+=(-d "$data")
    HTTP_CODE=$(curl "${args[@]}")
    BODY=$(cat /tmp/e2e.body)
}

# --- 1. register (or login if user already exists) ---
step "register $EMAIL"
http POST /auth/register "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}"
if [[ "$HTTP_CODE" == "200" ]]; then
    TOKEN=$(echo "$BODY" | jq -r .token)
    ok "registered, token acquired"
elif [[ "$HTTP_CODE" == "409" ]]; then
    echo -e "${c_dim}already registered, logging in instead${c_off}"
    http POST /auth/login "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}"
    [[ "$HTTP_CODE" == "200" ]] || die "login failed ($HTTP_CODE): $BODY"
    TOKEN=$(echo "$BODY" | jq -r .token)
    ok "logged in, token acquired"
else
    die "register failed ($HTTP_CODE): $BODY"
fi

echo -e "${c_dim}token: ${TOKEN:0:60}...${c_off}"

# --- 2. list products ---
step "GET /products"
http GET /products
[[ "$HTTP_CODE" == "200" ]] || die "products list failed ($HTTP_CODE): $BODY"
PRODUCT_COUNT=$(echo "$BODY" | jq 'length')
ok "$PRODUCT_COUNT products available"
echo "$BODY" | jq -r '.[] | "  - \(.name)  (\(.price)€, stock=\(.stock), id=\(.id))"'

[[ "$PRODUCT_COUNT" -ge 2 ]] || die "need at least 2 products to run the test"

PID1=$(echo "$BODY" | jq -r '.[0].id')
PNAME1=$(echo "$BODY" | jq -r '.[0].name')
PID2=$(echo "$BODY" | jq -r '.[1].id')
PNAME2=$(echo "$BODY" | jq -r '.[1].name')

# --- 3. add two items to cart ---
step "POST /cart/items  ($PNAME1 x2)"
http POST /cart/items "{\"productId\":\"$PID1\",\"quantity\":2}"
[[ "$HTTP_CODE" == "200" ]] || die "add item 1 failed ($HTTP_CODE): $BODY"
ok "added"

step "POST /cart/items  ($PNAME2 x1)"
http POST /cart/items "{\"productId\":\"$PID2\",\"quantity\":1}"
[[ "$HTTP_CODE" == "200" ]] || die "add item 2 failed ($HTTP_CODE): $BODY"
ok "added"

# --- 4. view cart ---
step "GET /cart"
http GET /cart
[[ "$HTTP_CODE" == "200" ]] || die "get cart failed ($HTTP_CODE): $BODY"
echo "$BODY" | jq '{cartId: .id, user: .user.email, items: [.items[] | {name: .product.name, price: .product.price, qty: .quantity}]}'

# --- 5. checkout ---
step "POST /cart/checkout"
http POST /cart/checkout
[[ "$HTTP_CODE" == "200" ]] || die "checkout failed ($HTTP_CODE): $BODY"
ok "checkout success"
echo "$BODY" | jq '{cartId: .id, remainingItems: (.items | length)}'

# --- 6. verify stock was decremented ---
step "verify stocks decremented"
http GET /products
echo "$BODY" | jq -r --arg id1 "$PID1" --arg id2 "$PID2" \
    '.[] | select(.id == $id1 or .id == $id2) | "  - \(.name)  stock=\(.stock)"'

echo -e "\n${c_green}=== e2e test completed successfully ===${c_off}"
echo -e "${c_dim}now check Grafana:${c_off}"
echo -e "  ${c_dim}Tempo:  { resource.service.name = \"observability_app\" && span.user.email = \"$EMAIL\" }${c_off}"
echo -e "  ${c_dim}Loki:   {service_name=\"observability_app\"} |= \"$EMAIL\"${c_off}"