#!/usr/bin/env python3
import json
import logging
import os
import sys
from dataclasses import dataclass
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent"
DEFAULT_MODEL = os.environ.get("GEMINI_MODEL", "gemini-3.5-flash")
PORT = int(os.environ.get("PORT", "8000"))
LOGGER = logging.getLogger("AI_ASSISTANT")

logging.basicConfig(
    level=os.environ.get("LOG_LEVEL", "INFO").upper(),
    format="%(asctime)s %(levelname)s %(name)s %(message)s",
)

RESPONSE_SCHEMA = {
    "type": "object",
    "properties": {
        "offers": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "source": {"type": "string"},
                    "price": {"type": "number"},
                    "currency": {"type": "string"},
                    "url": {"type": "string"},
                    "note": {"type": "string"},
                },
                "required": ["source", "price", "currency", "url", "note"],
            },
        },
        "best_price": {
            "type": "object",
            "properties": {
                "source": {"type": "string"},
                "price": {"type": "number"},
            },
            "required": ["source", "price"],
        },
        "best_safe_choice": {
            "type": "object",
            "properties": {
                "source": {"type": "string"},
                "reason": {"type": "string"},
            },
            "required": ["source", "reason"],
        },
        "saving": {
            "type": "object",
            "properties": {
                "amount": {"type": "number"},
                "percentage": {"type": "number"},
            },
            "required": ["amount", "percentage"],
        },
        "recommendation": {"type": "string"},
    },
    "required": ["offers", "best_price", "best_safe_choice", "saving", "recommendation"],
}


@dataclass
class PartCompareRequest:
    oem: str
    part_name: str
    vehicle: str | None
    partner_price: float | None

    @classmethod
    def from_dict(cls, payload: dict) -> "PartCompareRequest":
        part_name = str(payload.get("part_name", "")).strip()
        if not part_name:
            raise ValueError("Pole 'part_name' jest wymagane.")

        raw_partner_price = payload.get("partner_price")
        partner_price = None
        if raw_partner_price not in (None, ""):
            try:
                partner_price = float(raw_partner_price)
            except (TypeError, ValueError) as exc:
                raise ValueError("Pole 'partner_price' musi byc liczba.") from exc

        vehicle = payload.get("vehicle")
        vehicle_value = str(vehicle).strip() if vehicle not in (None, "") else None

        return cls(
            oem=str(payload.get("oem", "")).strip(),
            part_name=part_name,
            vehicle=vehicle_value,
            partner_price=partner_price,
        )


class GeminiBackendError(RuntimeError):
    pass


def build_prompt(request: PartCompareRequest) -> str:
    oem_value = request.oem or "brak pewnego numeru OEM"
    partner_price = (
        f"{request.partner_price:.2f} PLN" if request.partner_price is not None else "brak ceny partnera"
    )
    vehicle = request.vehicle or "brak danych o aucie"

    return f"""
Jestes asystentem zakupowym dla prywatnej aplikacji Garage Assistant.

Cel:
1. Traktuj sklep partnerski OEM jako glowny, zaufany punkt odniesienia.
2. Znajdz publicznie dostepne oferty alternatywne dla tej czesci.
3. Porownaj ceny i opisz ryzyko zakupu.
4. Zwroc tylko JSON zgodny ze schematem.

Wazne zasady:
- Najpierw szukaj po numerze OEM.
- Jesli OEM nie daje wynikow, uzyj nazwy czesci oraz kontekstu auta.
- Priorytetowo sprawdzaj Allegro, Ceneo i iParts.
- Mozesz uwzglednic inne publiczne sklepy motoryzacyjne, jesli znajdziesz sensowna oferte.
- Nie wymyslaj ofert. Jesli nie masz wystarczajacego potwierdzenia, pomin te oferte.
- "best_safe_choice.source" ma zwykle pozostac "OEM Partner", jesli partner OEM ma potwierdzona kompatybilnosc.
- W "best_safe_choice.reason" wyjasnij krotko, czemu to najbezpieczniejszy wybor.
- W "saving.amount" i "saving.percentage" policz oszczednosc wzgledem ceny partnera OEM. Jesli brak ceny partnera, zwroc 0.
- W polu note krotko napisz, po czym znaleziono oferte lub jakie jest ograniczenie.
- W "best_price" podaj najtansza oferte znalezionej listy.
- W "recommendation" napisz zrozumiala porade zakupowa dla uzytkownika.

Dane wejsciowe:
- OEM: {oem_value}
- Nazwa czesci: {request.part_name}
- Auto: {vehicle}
- Cena partnera OEM: {partner_price}
""".strip()


def call_gemini(request: PartCompareRequest) -> dict:
    api_key = os.environ.get("GEMINI_API_KEY", "").strip()
    LOGGER.info("GEMINI_API_KEY configured: %s", bool(api_key))
    if not api_key:
        raise GeminiBackendError("Brakuje zmiennej srodowiskowej GEMINI_API_KEY.")

    model = os.environ.get("GEMINI_MODEL", DEFAULT_MODEL).strip() or DEFAULT_MODEL
    prompt = build_prompt(request)

    payload = {
        "contents": [
            {
                "parts": [
                    {"text": prompt},
                ]
            }
        ],
        "tools": [
            {"googleSearch": {}},
        ],
        "generationConfig": {
            "responseFormat": {
                "text": {
                    "mimeType": "application/json",
                    "schema": RESPONSE_SCHEMA,
                }
            }
        },
    }
    LOGGER.info("Gemini request model: %s", model)
    LOGGER.debug("Gemini request payload: %s", json.dumps(payload, ensure_ascii=False))

    body = json.dumps(payload).encode("utf-8")
    http_request = Request(
        GEMINI_API_URL.format(model=model),
        data=body,
        method="POST",
        headers={
            "Content-Type": "application/json",
            "x-goog-api-key": api_key,
        },
    )

    try:
        with urlopen(http_request, timeout=45) as response:
            LOGGER.info("Gemini HTTP status: %s", response.status)
            raw = response.read().decode("utf-8")
    except HTTPError as exc:
        detail = exc.read().decode("utf-8", errors="replace")
        LOGGER.error("Gemini HTTP error status: %s", exc.code)
        LOGGER.error("Raw Gemini error response: %s", detail)
        raise GeminiBackendError(f"Gemini zwrocil blad HTTP {exc.code}: {detail}") from exc
    except URLError as exc:
        LOGGER.exception("Gemini connection error")
        raise GeminiBackendError(f"Nie udalo sie polaczyc z Gemini: {exc.reason}") from exc

    LOGGER.debug("Raw Gemini response: %s", raw)
    parsed = json.loads(raw)
    LOGGER.debug("Parsed Gemini response: %s", json.dumps(parsed, ensure_ascii=False))
    text = extract_response_text(parsed)
    if not text:
        raise GeminiBackendError("Gemini nie zwrocilo tekstowej odpowiedzi JSON.")

    result = json.loads(text)
    LOGGER.debug("Parsed Gemini JSON content: %s", json.dumps(result, ensure_ascii=False))
    return normalize_ai_response(result, request.partner_price)


def extract_response_text(payload: dict) -> str:
    candidates = payload.get("candidates") or []
    for candidate in candidates:
        content = candidate.get("content") or {}
        for part in content.get("parts") or []:
            text = part.get("text")
            if isinstance(text, str) and text.strip():
                return text
    return ""


def normalize_ai_response(payload: dict, partner_price: float | None) -> dict:
    offers = []
    for offer in payload.get("offers", []):
        try:
            price = float(offer.get("price", 0))
        except (TypeError, ValueError):
            continue

        offers.append(
            {
                "source": str(offer.get("source", "Rynek")).strip() or "Rynek",
                "price": round(price, 2),
                "currency": str(offer.get("currency", "PLN")).strip() or "PLN",
                "url": str(offer.get("url", "")).strip(),
                "note": str(offer.get("note", "Oferta znaleziona przez Gemini.")).strip()
                or "Oferta znaleziona przez Gemini.",
            }
        )

    best_price = payload.get("best_price") or {}
    best_safe_choice = payload.get("best_safe_choice") or {}
    saving = payload.get("saving") or {}

    cheapest_offer = min(offers, key=lambda offer: offer["price"], default=None)
    best_price_source = str(best_price.get("source", "")).strip() or (cheapest_offer["source"] if cheapest_offer else "Brak danych")
    best_price_value = parse_float(best_price.get("price"))
    if best_price_value is None and cheapest_offer is not None:
        best_price_value = cheapest_offer["price"]

    saving_amount = parse_float(saving.get("amount")) or 0.0
    saving_percentage = parse_float(saving.get("percentage")) or 0.0

    if partner_price is not None and cheapest_offer is not None:
        computed_amount = round(max(partner_price - cheapest_offer["price"], 0.0), 2)
        computed_percentage = round((computed_amount / partner_price) * 100, 2) if partner_price > 0 else 0.0
        if saving_amount <= 0:
            saving_amount = computed_amount
        if saving_percentage <= 0:
            saving_percentage = computed_percentage

    return {
        "offers": offers,
        "best_price": (
            {
                "source": best_price_source,
                "price": round(best_price_value or 0.0, 2),
            }
            if offers
            else None
        ),
        "best_safe_choice": (
            {
                "source": str(best_safe_choice.get("source", "OEM Partner")).strip() or "OEM Partner",
                "reason": str(best_safe_choice.get("reason", "")).strip()
                or "Potwierdzona kompatybilnosc i schematy OEM.",
            }
            if offers
            else None
        ),
        "saving": (
            {
                "amount": round(saving_amount, 2),
                "percentage": round(saving_percentage, 2),
            }
            if offers and partner_price is not None
            else None
        ),
        "recommendation": str(payload.get("recommendation", "")).strip()
        or "Najbezpieczniejszym punktem odniesienia pozostaje sklep partnerski OEM.",
    }

def parse_float(value):
    try:
        return float(value)
    except (TypeError, ValueError):
        return None


def empty_variant_c_result(recommendation: str = "") -> dict:
    return {
        "offers": [],
        "best_price": None,
        "best_safe_choice": None,
        "saving": None,
        "recommendation": recommendation,
    }


class Handler(BaseHTTPRequestHandler):
    server_version = "BmwGarageGeminiBackend/0.1"

    def do_OPTIONS(self):
        self.send_response(HTTPStatus.NO_CONTENT)
        self._send_cors_headers()
        self.end_headers()

    def do_GET(self):
        if self.path == "/health":
            self._send_json(
                HTTPStatus.OK,
                {
                    "status": "ok",
                    "gemini_configured": bool(os.environ.get("GEMINI_API_KEY", "").strip()),
                },
            )
            return
        self._send_json(HTTPStatus.NOT_FOUND, {"message": "Nie znaleziono endpointu."})

    def do_POST(self):
        if self.path != "/parts/compare":
            self._send_json(HTTPStatus.NOT_FOUND, {"message": "Nie znaleziono endpointu."})
            return

        try:
            length = int(self.headers.get("Content-Length", "0"))
            raw_body = self.rfile.read(length).decode("utf-8") if length > 0 else "{}"
            LOGGER.info("Incoming request payload: %s", raw_body)
            payload = json.loads(raw_body)
            request = PartCompareRequest.from_dict(payload)
            result = call_gemini(request)
            LOGGER.debug("Parsed backend result: %s", json.dumps(result, ensure_ascii=False))
        except json.JSONDecodeError:
            LOGGER.exception("Invalid JSON received by backend")
            self._send_json(HTTPStatus.BAD_REQUEST, {"message": "Niepoprawny JSON w zadaniu."})
            return
        except ValueError as exc:
            LOGGER.exception("Validation error in backend request")
            self._send_json(HTTPStatus.BAD_REQUEST, {"message": str(exc)})
            return
        except GeminiBackendError as exc:
            LOGGER.exception("Gemini processing failed")
            self._send_json(
                HTTPStatus.OK,
                empty_variant_c_result(
                    "AI nie znalazło jeszcze ofert dla tej części. Spróbuj ponownie później lub użyj przycisków Allegro / Ceneo / iParts."
                ),
            )
            return
        except Exception as exc:  # pragma: no cover
            LOGGER.exception("Unexpected backend error")
            self._send_json(
                HTTPStatus.OK,
                empty_variant_c_result(
                    "AI nie znalazło jeszcze ofert dla tej części. Spróbuj ponownie później lub użyj przycisków Allegro / Ceneo / iParts."
                ),
            )
            return

        self._send_json(HTTPStatus.OK, result)

    def log_message(self, format, *args):
        sys.stderr.write("%s - - [%s] %s\n" % (self.address_string(), self.log_date_time_string(), format % args))

    def _send_json(self, status: HTTPStatus, payload: dict):
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self._send_cors_headers()
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _send_cors_headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")


def main():
    server = ThreadingHTTPServer(("0.0.0.0", PORT), Handler)
    print(f"Gemini backend listening on http://0.0.0.0:{PORT}")
    server.serve_forever()


if __name__ == "__main__":
    main()
