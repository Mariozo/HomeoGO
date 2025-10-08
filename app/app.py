
# File: app/app.py
# Project: HomeoGO / Elza backend
# Created: 06.okt.2025 09:55 (Rīga)
# ver. 1.3
# Purpose:
#   Flask-based lightweight API for Elza conversational logic.


# app/app.py
from flask import Flask, request, jsonify
from dotenv import load_dotenv, find_dotenv
from openai import OpenAI
import os

# ---- .env ielāde (vienu līmeni augstāk) ----
env_path = find_dotenv()
if env_path:
    load_dotenv(env_path, override=True)
    print(f"[ENV] .env: {env_path}")
else:
    print("[ENV] ⚠ .env nav atrasts")

OPENAI_API_KEY  = os.getenv("OPENAI_API_KEY", "").strip()
OPENAI_MODEL    = os.getenv("OPENAI_MODEL", "gpt-4o-mini").strip()
SERVER_TOKEN    = os.getenv("SERVER_TOKEN", "").strip()
TIMEOUT_SECONDS = int(os.getenv("TIMEOUT", "15"))

print(f"[ENV] KEY={'OK' if OPENAI_API_KEY else 'MISSING'} MODEL={OPENAI_MODEL}")

client = OpenAI(api_key=OPENAI_API_KEY)
app = Flask(__name__)

# 1) Pirms-katra-pieprasījuma logs (redzēsi IP un ceļu)
@app.before_request
def _log_req():
    try:
        print(f"[REQ] {request.remote_addr} {request.method} {request.path}")
    except:
        pass

# 2) Saknes ceļš "/" — lai var testēt ļoti vienkārši pārlūkā
@app.get("/")
def root():
    return jsonify({"ok": True, "hint": "Try GET /health or POST /elza/reply"}), 200

# 3) (pēc izvēles) Draudzīgs GET /elza/reply skaidrojums
@app.get("/elza/reply")
def elza_reply_get_info():
    return jsonify({
        "ok": False,
        "hint": 'Use POST /elza/reply with JSON body {"prompt":"...","lang":"lv-LV"}'
    }), 405

@app.get("/health")
def health():
    return jsonify({"ok": True, "model": OPENAI_MODEL, "key_loaded": bool(OPENAI_API_KEY)})

@app.post("/elza/reply")
def elza_reply():
    # (neobligāti) Bearer token pārbaude
    auth = request.headers.get("Authorization", "")
    token = auth.replace("Bearer ", "").strip()
    if SERVER_TOKEN and token != SERVER_TOKEN:
        return jsonify({"error":"Unauthorized"}), 401

    data = request.get_json(silent=True) or {}
    prompt = (data.get("prompt") or "").strip()
    lang   = (data.get("lang") or "lv-LV").strip()
    if not prompt:
        return jsonify({"error":"Missing prompt"}), 400

    try:
        # Jaunais SDK: client.chat.completions.create(...)
        resp = client.chat.completions.create(
            model=OPENAI_MODEL,
            messages=[
                {"role":"system","content":"Tu esi Elza — draudzīga un gudra balss asistente latviešu valodā."},
                {"role":"user","content":prompt}
            ],
            timeout=TIMEOUT_SECONDS,
        )
        reply_text = (resp.choices[0].message.content or "").strip()
        return jsonify({"reply": reply_text})
    except Exception as e:
        print(f"[AI ERROR] {e}")
        # Draudzīgs 200-atgriežams fallback, lai Android nedomā, ka tā ir kļūme
        return jsonify({"reply": "Pašlaik nevaru iegūt gudru atbildi tiešsaistē. Pastāsti sīkāk: " + prompt})

if __name__ == "__main__":
    # pieejams no localhost un LAN
    app.run(host="0.0.0.0", port=5000)
