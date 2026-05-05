# Lokalny workflow Git

Ten projekt moze byc prowadzony calkowicie offline. Git przechowuje cala historie lokalnie na Macu, a branche dzialaja bez GitHuba, GitLaba ani internetu.

## Glowne galezie

- `main` - stabilny stan aplikacji, ktory powinien sie otwierac w Android Studio.
- `feature/...` - nowe funkcje, np. `feature/vehicle-profiles`.
- `fix/...` - poprawki bledow, np. `fix/gradle-sync`.
- `notes/...` - wieksze zmiany dokumentacji lub planu.

## Typowy rytm pracy

1. Zaczynamy z aktualnego `main`.
2. Tworzymy branch dla jednej funkcji.
3. Wprowadzamy zmiany.
4. Sprawdzamy aplikacje w Android Studio.
5. Robimy commit.
6. Scalanie do `main`, kiedy zmiana jest gotowa.

## Przykladowe komendy

```bash
git status
git switch main
git switch -c feature/vehicle-profiles
git add .
git commit -m "Add vehicle profiles screen"
git switch main
git merge feature/vehicle-profiles
```

## Kopia zapasowa offline

Repo mozna skopiowac na dysk zewnetrzny albo pendrive. Wystarczy skopiowac caly katalog `BmwGarageAssistant`, razem z ukrytym katalogiem `.git`.

