#!/usr/bin/env bash
# ----------- DEPLOY CORDA NODES
./scripts/deploy.sh

echo "🍀 🍀 🍀 🍀 🍀 🍀 🍀 done deploying Corda nodes"
# ------------ NOTARY NODE
ttab ./scripts/nnotary.sh

# ------------ REGULATOR NODE
echo 🕗 Sleeping for 15 seconds
sleep 15s
echo "🔆 Woke up, opening terminal for Regulator Corda Node"
ttab ./scripts/nregulator.sh

# ------------ partyA NODE
echo 🕗  Sleeping for 15 seconds
sleep 15s
echo "🔆 Woke up, 🔆 🔆 🔆  opening terminal for PartyA"
ttab ./scripts/npartyA.sh

# ------------ partyB NODE
echo 🕗 Sleeping for 15 seconds
sleep 15s
echo "🔆 Woke up, 🔆 🔆 🔆  opening terminal for partyB"
ttab ./scripts/npartyB.sh

# ------------ partyC NODE
echo 🕗  Sleeping for 15 seconds ........
sleep 15s
echo "🔆 Woke up, 🔆 🔆 🔆 opening terminal for partyC"
ttab ./scripts/npartyC.sh

echo "🔵 SLEEPING 🍎 10 🍎 seconds to let corda nodes finish booting up 🔵 🔵 🔵 🔵 🔵 🔵 🔵 🔵 🔵 🔵 🔵"
sleep 10s

echo "🔆 Woke up, 🧩 🧩 🧩 🧩 opening Regulator webserver"
ttab ./scripts/wregulator.sh
sleep 15s

echo "🔆 Woke up, 🧩 🧩 🧩 🧩  opening PartyA webserver"
ttab ./scripts/wpartyA.sh
sleep 15s

echo "🔆 Woke up, 🧩 🧩 🧩 🧩  opening PartyB webserver"
ttab ./scripts/wpartyB.sh

sleep 15s
echo "🔆 Woke up, 🧩 🧩 🧩 🧩  opening PartyC webserver"
ttab ./scripts/wpartyC.sh

echo "🍀 🍀 🍀 🍀 🍀 🍀 🍀 done deploying Corda nodes and associated webservers"



