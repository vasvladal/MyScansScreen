# Clipboard Monitor - Manual de utilizare

## Cuprins
1. [Introducere](#introducere)
2. [Funcționalități](#funcționalități)
3. [Începutul utilizării](#începutul-utilizării)
4. [Utilizarea aplicației](#utilizarea-aplicației)
   - [Ecranul principal](#ecranul-principal)
   - [Istoricul clipboardului](#istoricul-clipboardului)
   - [Controlul serviciului](#controlul-serviciului)
5. [Funcții avansate](#funcții-avansate)
   - [Selectarea limbii](#selectarea-limbii)
   - [Gestionarea imaginilor](#gestionarea-imaginilor)
   - [Gestionarea URI-urilor](#gestionarea-uri-urilor)
6. [Configurare](#configurare)
7. [Depanare](#depanare)
8. [Despre](#despre)

## Introducere
Clipboard Monitor este o aplicație Android care monitorizează tot ce copiați în clipboard, inclusiv text, imagini și referințe la fișiere. Funcționează în fundal și păstrează un istoric al elementelor copiate, la care puteți accesa oricând.

## Funcționalități
- Monitorizează modificările clipboardului în timp real
- Stochează istoricul textelor, imaginilor și URI-urilor copiate
- Suportă mai multe limbi (engleză, rusă, ucraineană, română)
- Compresie imagine și previzualizare
- Partajare sau copiere a elementelor din istoric
- Serviciu în fundal cu notificări

## Începutul utilizării
1. **Instalare**: Descărcați și instalați aplicația din RuStore
2. **Permisiuni**: Acordați permisiunile necesare la solicitare:
   - Acces la clipboard
   - Acces la stocare (pentru imagini)
   - Permisiune pentru notificări (Android 13+)
3. **Prima utilizare**: Aplicația va începe să monitorizeze clipboardul automat dacă este configurată astfel

## Utilizarea aplicației

### Ecranul principal
Ecranul principal oferă:
- Indicator de stare a serviciului (activ/oprit)
- Buton pentru vizualizarea istoricului clipboardului
- Buton pentru pornire/oprire a serviciului de monitorizare
- Buton pentru ștergerea întregului istoric
- Butonul meniu (în dreapta sus) pentru selectarea limbii și informații despre aplicație

### Istoricul clipboardului
Accesați istoricul clipboardului apăsând "Vizualizare clipboard":
- Elementele sunt afișate în ordine cronologică (cele mai recente primele)
- Fiecare intrare afișează:
   - Previzualizare conținut (text sau miniatură imagine)
   - Indicator de tip (codat pe culori)
   - Marcaj temporal
   - Butoane de acțiune (partajare, ștergere)

**Acțiuni:**
- **Apăsare scurtă**: Copierea elementului înapoi în clipboard
- **Apăsare lungă**: Afișare meniu contextual cu opțiuni suplimentare
- **Partajare**: Partajarea elementului cu alte aplicații
- **Ștergere**: Eliminarea elementului din istoric

### Controlul serviciului
- **Pornire serviciu**: Începe monitorizarea clipboardului (funcționează în fundal)
- **Oprire serviciu**: Întrerupe monitorizarea (istoricul rămâne)
- Starea este afișată în partea de sus a ecranului principal

## Funcții avansate

### Selectarea limbii
Schimbați limba aplicației prin meniu:
1. Apăsați butonul meniu (în dreapta sus)
2. Selectați "Limbă"
3. Alegeți dintre opțiunile disponibile
4. Aplicația se va reporni cu noua limbă

### Gestionarea imaginilor
- Imaginile sunt comprimate pentru a economisi spațiu
- Miniaturile sunt afișate în istoric
- Imaginile complete pot fi copiate înapoi sau partajate
- Pe Android 15+, în mod implicit sunt stocate doar referințele la imagini

### Gestionarea URI-urilor
- Fișierele și URI-urile sunt stocate ca referințe
- Pentru imagini, puteți alege să importați imaginea completă
- URI-urile pot fi copiate sau partajate ca elemente obișnuite

## Configurare
Aplicația poate fi configurată prin `config.toml` (pentru utilizatori avansați):
- Numărul maxim de intrări de păstrat (implicit: 100)
- Curățare automată (implicit: activată)
- Calitatea compresiei imaginilor (implicit: 80%)
- Setări notificări
- Numele și versiunea bazei de date
- Temă interfață și setări previzualizare

## Depanare
**Probleme comune:**
1. **Clipboardul nu este monitorizat**:
   - Asigurați-vă că serviciul funcționează (verificați starea pe ecranul principal)
   - Acordați toate permisiunile necesare
   - Reporniți aplicația dacă este necesar

2. **Imaginile nu apar**:
   - Verificați permisiunile de stocare
   - Pe Android 15+, imaginile pot fi stocate doar ca referințe

3. **Serviciul se oprește neașteptat**:
   - Dezactivați optimizarea bateriei pentru aplicație
   - Asigurați-vă că sistemul nu închide aplicația

## Despre
- Versiune: [afișată în antetul aplicației]
- Descriere: Utilitar de monitorizare clipboard
- Informații despre dezvoltator disponibile în dialogul Despre

Pentru suport suplimentar, vă rugăm să contactați dezvoltatorul prin pagina aplicației în magazin.

---

*Notă: Acest manual se bazează pe versiunea ${AppInfo.getFormattedVersion(context)} a Clipboard Monitor. Unele funcționalități pot varia în funcție de dispozitivul și versiunea de Android.*