# Gemini Backend

Lekki backend do porownywania ofert czesci przez Gemini.

## Co robi

- odbiera zadanie z aplikacji Android pod `POST /parts/compare`,
- wysyla zapytanie do Gemini po stronie serwera,
- trzyma klucz API poza aplikacja mobilna,
- wymusza strukture odpowiedzi JSON,
- zwraca gotowy wynik do ekranu `Zapytaj AI`.

## Wymagane zmienne srodowiskowe

- `GEMINI_API_KEY` - klucz Gemini API z Google AI Studio
- `GEMINI_MODEL` - opcjonalnie, domyslnie `gemini-3.5-flash`
- `PORT` - opcjonalnie, domyslnie `8000`

## Uruchomienie lokalne

```bash
export GEMINI_API_KEY="wklej_tutaj_klucz"
export GEMINI_MODEL="gemini-3.5-flash"
python3 backend/gemini_backend.py
```

Serwer uruchomi sie pod:

```text
http://localhost:8000
```

## Endpointy

### Health

```http
GET /health
```

Przykladowa odpowiedz:

```json
{
  "status": "ok",
  "gemini_configured": true
}
```

### Porownanie czesci

```http
POST /parts/compare
Content-Type: application/json
```

Przykladowy request:

```json
{
  "oem": "11130139259",
  "part_name": "Uszczelka, miska olejowa ELRING 071.340",
  "vehicle": "BMW E61 520d M47N2",
  "partner_price": 67.13
}
```

Przykladowa odpowiedz:

```json
{
  "offers": [],
  "best_price": null,
  "best_safe_choice": null,
  "saving": null,
  "recommendation": "Najtańsza oferta została znaleziona na Allegro..."
}
```

## Konfiguracja aplikacji Android

Backend jest wpiety przez `AI_ASSISTANT_BASE_URL` w module `app`.

Przyklad:

```bash
./gradlew assembleDebug -PAI_ASSISTANT_BASE_URL=http://10.0.2.2:8000
```

Na fizycznym telefonie wpisz adres komputera w tej samej sieci, na przyklad:

```bash
./gradlew assembleDebug -PAI_ASSISTANT_BASE_URL=http://192.168.1.50:8000
```

## Debug Checklist

### Test 1 – Backend health

```bash
curl http://localhost:8000/health
```

Expected:

```json
{
  "status": "ok",
  "gemini_configured": true
}
```

Jesli klucz Gemini nie jest ustawiony:

```json
{
  "status": "ok",
  "gemini_configured": false
}
```

### Test 2 – Compare endpoint

```bash
curl -X POST http://localhost:8000/parts/compare \
  -H "Content-Type: application/json" \
  -d '{
    "oem": "11130139259",
    "part_name": "Uszczelka, miska olejowa ELRING 071.340",
    "vehicle": "BMW E61 520d M47N2",
    "partner_price": 67.13
  }'
```

Expected:

Backend powinien zwrocic poprawny JSON Variant C:

```json
{
  "offers": [],
  "best_price": null,
  "best_safe_choice": null,
  "saving": null,
  "recommendation": ""
}
```

albo te same pola wypelnione prawdziwymi danymi, jesli Gemini znalazlo oferty.

### Test 3 – Android logcat

Filtruj logi po:

```text
AI_ASSISTANT
```

Expected:

- Base URL jest widoczny
- request payload jest widoczny
- status backendu jest widoczny
- raw backend response jest widoczny

## Uwagi

- Android nie przechowuje klucza Gemini.
- Backend uzywa oficjalnego endpointu `generateContent`.
- Backend prosi Gemini o odpowiedz `application/json` zgodna ze schematem.
- Prompt utrzymuje sklep partnerski OEM jako glowny, bezpieczny punkt odniesienia.
- Backend nigdy nie loguje pelnego klucza Gemini.
- Gdy Gemini zawiedzie, backend nadal zwraca poprawna strukture Variant C.
