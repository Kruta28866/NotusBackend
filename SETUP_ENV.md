# Konfiguracja Zmiennych Środowiskowych (Environment Variables)

Aby aplikacja backendowa mogła połączyć się z bazą danych Supabase oraz obsługiwać tokeny QR, musisz skonfigurować zmienne środowiskowe na swoim komputerze.

## Wymagane Zmienne
- **`DB_PASSWORD`** (Wymagane): Hasło do Twojej bazy danych Supabase.
- **`DB_HOST`** (Opcjonalne): Host bazy danych (domyślnie: `db.videmeamentkjybkyoeq.supabase.co`).
- **`DB_USER`** (Opcjonalne): Użytkownik bazy danych (domyślnie: `postgres`).
- **`QR_HMAC_SECRET`** (Opcjonalne): Dowolny, długi ciąg znaków do podpisywania kodów QR.

---

## 1. Konfiguracja w IntelliJ IDEA (Zalecane)

1. W górnym menu wybierz listę rozwijalną obok przycisku "Run" (zielony trójkąt).
2. Wybierz **`Edit Configurations...`**.
3. Znajdź swoją główną konfigurację (zazwyczaj **`NotusBackendApplication`**).
4. W sekcji **`Environment variables`** kliknij ikonę folderu/dokumentu po prawej stronie pola.
5. Dodaj nową zmienną:
   - **Name**: `DB_PASSWORD`
   - **Value**: `[Twoje_Haslo_Tu]`
6. Kliknij **`OK`** i **`Apply`**.

---

## 2. Konfiguracja w Visual Studio Code

1. Otwórz folder projektu w VS Code.
2. Jeśli nie masz folderu `.vscode`, utwórz go w głównym katalogu.
3. Wewnątrz utwórz plik **`launch.json`** z następującą zawartością:
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
        "DB_PASSWORD": "Twoje_Haslo_Tu"
      }
    }
  ]
}
```
4. Uruchom aplikację przez zakładkę **`Run and Debug`** (Ctrl+Shift+D).

---

## 3. Uruchamianie z Terminala (PowerShell)

Zanim wpiszesz polecenie uruchomienia (np. `mvnw spring-boot:run`), musisz ustawić zmienną w danej sesji terminala:
```powershell
$env:DB_PASSWORD="Twoje_Haslo_Tu"
./mvnw.cmd spring-boot:run
```

---

> [!IMPORTANT]
> **Nigdy** nie dodawaj pliku `.env` ani haseł do Gita! Jeśli planujesz użyć pliku `.env`, upewnij się, że jest on wpisany w `.gitignore`.
