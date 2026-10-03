# 📻 LAYA Origin — Webradio Serverless GitOps

Bienvenue sur le projet officiel de **LAYA Origin**, une webradio avec architecture 100 % Serverless et gratuite hébergée sur GitHub.

---

## 🌟 Architecture du Projet

1. **Base de données (`playlist.json`)** :
   - Fichier JSON hébergé à la racine du dépôt.
   - Contient la liste des morceaux (titre, artiste, URL directe du flux audio).
   - Aucune base de données payante requise.

2. **Le Mix de Minuit (Algorithme Déterministe)** :
   - Chaque jour à minuit pile, la playlist est réordonnée côté client grâce à un algorithme de mélange pseudo-aléatoire déterministe (Fisher-Yates) basé sur la date du jour (`YYYYMMDD` en graine/seed).
   - Tous les auditeurs dans le monde écoutent la même programmation de façon parfaitement synchronisée.

3. **Lecteur Mobile Android & React Native** :
   - **Android Natif (Jetpack Compose / Kotlin)** :
     - Foreground Service avec notification multimédia pour écoute continue écran éteint.
     - Cache audio intelligent pré-chargeant la piste suivante pour éliminer toute latence.
     - Design pastel épuré, bouton central START/PAUSE, indicateur clignotant **● LIVE**, minuterie de sommeil (Sleep Timer) et console GitOps intégrée.
   - **React Native (Expo)** :
     - Code complet prêt à l'emploi situé dans `react-native-expo/`.

4. **Interface d'Administration Web (`admin/index.html`)** :
   - Interface web autonome (HTML5/JS) permettant de gérer les morceaux via l'API REST de GitHub.
   - Importation en un clic depuis vos **GitHub Releases**.
   - Commit direct (GitOps) via requêtes `PUT` avec Personal Access Token (PAT).

---

## 🎵 Hébergement Gratuit des Morceaux (GitHub Releases)

Pour héberger vos fichiers `.mp3` gratuitement et sans limite de bande passante :
1. Rendez-vous sur l'onglet **Releases** de ce dépôt.
2. Créez ou éditez une release (ex: `Musique_laya_origin_initial`).
3. Glissez-déposez vos fichiers `.mp3`.
4. Dans l'interface d'administration (`admin/index.html`), cliquez sur **"✨ Importer depuis les GitHub Releases"** ou copiez l'URL directe du fichier.
5. Cliquez sur **"Commiter vers GitHub"** : la playlist est mise à jour instantanément !

---

## 🚀 Démarrer le Lecteur React Native (Expo)

```bash
cd react-native-expo
npm install
npx expo start
```

---

## 🛠️ Interface d'Administration Web

Ouvrez simplement le fichier `admin/index.html` dans un navigateur ou activez **GitHub Pages** sur la branche `main` pour y accéder en ligne gratuitement !
