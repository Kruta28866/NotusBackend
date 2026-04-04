# Uruchamianie Backendu

## Wymagane Zmienne Środowiskowe

- **`DB_PASSWORD`** (Wymagane): Hasło do bazy danych Supabase.
- **`GEMINI_API_KEY`** (Wymagane): Klucz API do Gemini.
- **`QR_HMAC_SECRET`** (Opcjonalne): Dowolny, długi ciąg znaków do podpisywania kodów QR (ma wartość domyślną).

---

## 1. Docker (Zalecane)

Najłatwiejszy sposób — nie wymaga instalacji Javy ani Mavena.

**Wymagania:** [Docker Desktop](https://www.docker.com/products/docker-desktop/)

1. Skopiuj plik z przykładowymi zmiennymi:
   ```bash
   cp .env.example .env
   ```
2. Otwórz `.env` i uzupełnij swoje wartości:
   ```
   DB_PASSWORD=twoje_haslo
   GEMINI_API_KEY=twoj_klucz_api
   ```
3. Uruchom backend:
   ```bash
   docker compose up
   ```

Backend będzie dostępny pod adresem `http://localhost:8080`.

Aby zatrzymać: `docker compose down`

> [!IMPORTANT]
> Nigdy nie commituj pliku `.env` do Gita — zawiera Twoje prywatne klucze.

---

## 2. IntelliJ IDEA (bez Dockera)

1. W górnym menu wybierz listę rozwijalną obok przycisku "Run" (zielony trójkąt).
2. Wybierz **`Edit Configurations...`**.
3. Znajdź konfigurację **`NotusBackendApplication`**.
4. W sekcji **`Environment variables`** dodaj:
   - `DB_PASSWORD` = `twoje_haslo`
   - `GEMINI_API_KEY` = `twoj_klucz_api`
5. Kliknij **`OK`** i **`Apply`**.

---

## 3. Visual Studio Code (bez Dockera)

Utwórz plik `.vscode/launch.json`:
```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Run NotusBackend",
      "request": "launch",
      "mainClass": "com.notus.backend.NotusBackendApplication",
      "env": {
        "DB_PASSWORD": "twoje_haslo",
        "GEMINI_API_KEY": "twoj_klucz_api"
      }
    }
  ]
}
```
Uruchom przez zakładkę **`Run and Debug`** (Ctrl+Shift+D).

---

## 4. Terminal — PowerShell (bez Dockera)

```powershell
$env:DB_PASSWORD="twoje_haslo"
$env:GEMINI_API_KEY="twoj_klucz_api"
./mvnw.cmd spring-boot:run
```
