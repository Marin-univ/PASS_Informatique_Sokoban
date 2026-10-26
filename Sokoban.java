import java.util.Scanner;

/**
 * Projet Java PASS 2026-2027 -- Sokoban : jeu et resolution algorithmique.
 *
 * Tout le projet tient dans cette unique classe (contrainte du sujet).
 * Aucune bibliotheque externe, aucune structure de donnees toute faite :
 * la file (BFS) et l'ensemble des etats visites (table de hachage) sont
 * implementes a la main avec des tableaux.
 *
 * Compilation / execution :
 *     javac Sokoban.java
 *     java Sokoban
 */
public class Sokoban {

    // ==================================================================
    //  SYMBOLES ET CONSTANTES
    // ==================================================================

    static final char MUR                 = '#';
    static final char LIBRE               = ' ';
    static final char JOUEUR              = '@';
    static final char CAISSE              = '$';
    static final char OBJECTIF            = '.';
    static final char CAISSE_SUR_OBJECTIF = '*';
    static final char JOUEUR_SUR_OBJECTIF = '+';

    /** Les 4 directions, dans l'ordre : haut, bas, gauche, droite. */
    static final char[]   TOUCHES  = { 'z',    's',   'q',      'd'      };
    static final int[]    DL       = {  -1,     1,     0,        0       };
    static final int[]    DC       = {   0,     0,    -1,        1       };
    static final String[] NOMS_DIR = { "haut", "bas", "gauche", "droite" };

    /** Les niveaux sont stockes directement dans le programme. */
    static final String[][] NIVEAUX = {
        {   // Niveau 1 : une seule caisse, une seule poussee
            "########",
            "#      #",
            "#  .   #",
            "#  $   #",
            "#  @   #",
            "#      #",
            "########"
        },
        {   // Niveau 2 : l'objectif est du mauvais cote, il faut contourner
            "#######",
            "#     #",
            "#  $  #",
            "#  .  #",
            "#  @  #",
            "#     #",
            "#######"
        },
        {   // Niveau 3 : deux caisses, deux objectifs
            "########",
            "#      #",
            "#  $$  #",
            "#  ..  #",
            "#  @   #",
            "#      #",
            "########"
        }
    };

    // ==================================================================
    //  ETAT GLOBAL
    // ==================================================================

    // Partie statique du niveau : ne change JAMAIS pendant la partie.
    static boolean[][] mur;        // mur[l][c] == true      -> case infranchissable
    static boolean[][] objectif;   // objectif[l][c] == true -> case but
    static int hauteur, largeur;

    // Partie dynamique : modifiee a chaque coup.
    static boolean[][] caisse;     // caisse[l][c] == true   -> une caisse est ici
    static int joueurL, joueurC;
    static int nbCaisses;
    static int nbMouvements;

    /** Niveau actuellement charge (pour la touche "recommencer"). */
    static int niveauCourant;

    // Historique des coups, pour l'annulation (touche 'a').
    static char[] historique = new char[10000];
    static int    nbCoupsJoues;

    static final Scanner clavier = new Scanner(System.in);

    // ==================================================================
    //  PROGRAMME PRINCIPAL -- MENU
    // ==================================================================

    public static void main(String[] args) {
        int choix = -1;
        while (choix != 6) {
            System.out.println();
            System.out.println("================================");
            System.out.println("          SOKOBAN JAVA");
            System.out.println("================================");
            System.out.println("1. Nouvelle partie");
            System.out.println("2. Choisir un niveau");
            System.out.println("3. Resoudre automatiquement");
            System.out.println("4. Comparer DFS / BFS");
            System.out.println("5. Afficher les regles");
            System.out.println("6. Quitter");
            System.out.print("Votre choix : ");

            choix = lireEntier();

            if (choix == 1) {
                chargerNiveau(0);
                jouer();
            } else if (choix == 2) {
                int n = menuNiveaux();
                if (n >= 0) {
                    chargerNiveau(n);
                    jouer();
                }
            } else if (choix == 3) {
                int n = menuNiveaux();
                if (n >= 0) {
                    chargerNiveau(n);
                    menuResolution();
                }
            } else if (choix == 4) {
                int n = menuNiveaux();
                if (n >= 0) {
                    chargerNiveau(n);
                    comparerAlgorithmes();
                }
            } else if (choix == 5) {
                afficherRegles();
            } else if (choix == 6) {
                System.out.println("Au revoir !");
            } else {
                System.out.println("Choix invalide.");
            }
        }
    }

    /** Sous-menu de selection d'un niveau. Renvoie l'indice, ou -1 si annule. */
    static int menuNiveaux() {
        System.out.println();
        System.out.println("===== SOKOBAN =====");
        for (int i = 0; i < NIVEAUX.length; i++) {
            System.out.println((i + 1) + ". Niveau " + (i + 1));
        }
        System.out.println((NIVEAUX.length + 1) + ". Retour");
        System.out.print("Votre choix : ");

        int c = lireEntier();
        if (c >= 1 && c <= NIVEAUX.length) {
            return c - 1;
        }
        if (c != NIVEAUX.length + 1) {
            System.out.println("Choix invalide.");
        }
        return -1;
    }

    static void menuResolution() {
        System.out.println();
        System.out.println("1. Resolution par DFS (profondeur)");
        System.out.println("2. Resolution par BFS (largeur)");
        System.out.print("Votre choix : ");
        int c = lireEntier();
        if (c == 1) {
            resoudreDFS();
            rejouerSolution(solutionDFS, longueurDFS);
        } else if (c == 2) {
            resoudreBFS();
            rejouerSolution(solutionBFS, longueurBFS);
        } else {
            System.out.println("Choix invalide.");
        }
    }

    static void afficherRegles() {
        System.out.println();
        System.out.println("===== REGLES =====");
        System.out.println("  " + MUR + " : mur");
        System.out.println("  " + LIBRE + " : case libre");
        System.out.println("  " + JOUEUR + " : joueur");
        System.out.println("  " + CAISSE + " : caisse");
        System.out.println("  " + OBJECTIF + " : objectif");
        System.out.println("  " + CAISSE_SUR_OBJECTIF + " : caisse placee sur un objectif");
        System.out.println();
        System.out.println("Deplacez toutes les caisses sur les objectifs.");
        System.out.println("Une caisse ne peut etre que POUSSEE, jamais tiree,");
        System.out.println("et seulement si la case derriere elle est libre.");
        System.out.println();
        System.out.println("Commandes : z=haut  s=bas  q=gauche  d=droite");
        System.out.println("            a=annuler  r=recommencer  x=quitter");
    }

    /** Lecture robuste d'un entier : ne plante pas sur une saisie texte. */
    static int lireEntier() {
        String ligne = clavier.hasNextLine() ? clavier.nextLine().trim() : "";
        if (ligne.length() == 0) {
            return -1;
        }
        int valeur = 0;
        for (int i = 0; i < ligne.length(); i++) {
            char ch = ligne.charAt(i);
            if (ch < '0' || ch > '9') {
                return -1;
            }
            valeur = valeur * 10 + (ch - '0');
        }
        return valeur;
    }

    // ==================================================================
    //  PARTIE A / B / C  --  LE JEU
    // ==================================================================

    /**
     * Partie C : charge le niveau d'indice donne dans les tableaux globaux.
     * Separe le decor fixe (murs, objectifs) du contenu mobile (caisses, joueur).
     */
    static void chargerNiveau(int indice) {
        String[] plan = NIVEAUX[indice];
        niveauCourant = indice;

        hauteur = plan.length;
        largeur = 0;
        for (int l = 0; l < hauteur; l++) {
            if (plan[l].length() > largeur) {
                largeur = plan[l].length();
            }
        }

        mur      = new boolean[hauteur][largeur];
        objectif = new boolean[hauteur][largeur];
        caisse   = new boolean[hauteur][largeur];
        nbCaisses    = 0;
        nbMouvements = 0;
        nbCoupsJoues = 0;
        joueurL = -1;
        joueurC = -1;

        for (int l = 0; l < hauteur; l++) {
            for (int c = 0; c < largeur; c++) {
                // les lignes plus courtes que la grille sont completees par du vide
                char symbole = (c < plan[l].length()) ? plan[l].charAt(c) : LIBRE;

                if (symbole == MUR) {
                    mur[l][c] = true;
                } else if (symbole == OBJECTIF) {
                    objectif[l][c] = true;
                } else if (symbole == CAISSE) {
                    caisse[l][c] = true;
                    nbCaisses++;
                } else if (symbole == CAISSE_SUR_OBJECTIF) {
                    caisse[l][c]   = true;
                    objectif[l][c] = true;
                    nbCaisses++;
                } else if (symbole == JOUEUR) {
                    joueurL = l;
                    joueurC = c;
                } else if (symbole == JOUEUR_SUR_OBJECTIF) {
                    joueurL = l;
                    joueurC = c;
                    objectif[l][c] = true;
                }
            }
        }
    }

    /** Partie A : reconstruit et affiche la grille a partir des tableaux. */
    static void afficherGrille() {
        for (int l = 0; l < hauteur; l++) {
            StringBuilder ligne = new StringBuilder();
            for (int c = 0; c < largeur; c++) {
                char symbole;
                if (mur[l][c]) {
                    symbole = MUR;
                } else if (caisse[l][c]) {
                    symbole = objectif[l][c] ? CAISSE_SUR_OBJECTIF : CAISSE;
                } else if (l == joueurL && c == joueurC) {
                    symbole = objectif[l][c] ? JOUEUR_SUR_OBJECTIF : JOUEUR;
                } else {
                    symbole = objectif[l][c] ? OBJECTIF : LIBRE;
                }
                ligne.append(symbole);
            }
            System.out.println(ligne.toString());
        }
    }

    /** Une case est utilisable si elle est dans la grille et n'est pas un mur. */
    static boolean deplacementValide(int l, int c) {
        return l >= 0 && l < hauteur && c >= 0 && c < largeur && !mur[l][c];
    }

    /**
     * Partie A : la caisse en (l,c) peut-elle etre poussee dans la direction dir ?
     * Il faut que la case derriere la caisse soit dans la grille, sans mur,
     * et sans autre caisse.
     */
    static boolean peutPousser(int l, int c, int dir) {
        int derriereL = l + DL[dir];
        int derriereC = c + DC[dir];
        return deplacementValide(derriereL, derriereC) && !caisse[derriereL][derriereC];
    }

    /** Partie A : avance physiquement la caisse d'une case dans la direction dir. */
    static void deplacerCaisse(int l, int c, int dir) {
        caisse[l][c] = false;
        caisse[l + DL[dir]][c + DC[dir]] = true;
    }

    /**
     * Partie A : tente de deplacer le joueur dans la direction dir.
     * @return true si le coup a reellement eu lieu.
     */
    static boolean deplacerJoueur(int dir) {
        int cibleL = joueurL + DL[dir];
        int cibleC = joueurC + DC[dir];

        if (!deplacementValide(cibleL, cibleC)) {
            return false;                       // mur ou bord de grille
        }
        if (caisse[cibleL][cibleC]) {
            if (!peutPousser(cibleL, cibleC, dir)) {
                return false;                   // caisse bloquee : on ne tire jamais
            }
            deplacerCaisse(cibleL, cibleC, dir);
        }
        joueurL = cibleL;
        joueurC = cibleC;
        nbMouvements++;
        return true;
    }

    /** Partie B : toutes les caisses sont-elles sur un objectif ? */
    static boolean partieTerminee() {
        for (int l = 0; l < hauteur; l++) {
            for (int c = 0; c < largeur; c++) {
                if (caisse[l][c] && !objectif[l][c]) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Bonus : annule le dernier coup (y compris la poussee eventuelle). */
    static boolean annulerDernierCoup() {
        if (nbCoupsJoues == 0) {
            return false;
        }
        nbCoupsJoues--;
        int dir         = historique[nbCoupsJoues] % 10;    // direction jouee
        boolean poussee = historique[nbCoupsJoues] >= 10;   // y avait-il une caisse ?

        if (poussee) {
            // la caisse poussee est juste devant le joueur : on la ramene sur lui
            int caisseL = joueurL + DL[dir];
            int caisseC = joueurC + DC[dir];
            caisse[caisseL][caisseC] = false;
            caisse[joueurL][joueurC] = true;
        }
        joueurL = joueurL - DL[dir];
        joueurC = joueurC - DC[dir];
        nbMouvements--;
        return true;
    }

    /** Partie A + B : boucle de jeu interactive. */
    static void jouer() {
        boolean enCours = true;

        while (enCours) {
            System.out.println();
            afficherGrille();
            System.out.println("Mouvements : " + nbMouvements);

            if (partieTerminee()) {
                System.out.println();
                System.out.println("Victoire !");
                System.out.println("Nombre de mouvements : " + nbMouvements);
                return;
            }

            System.out.print("Commande (z/s/q/d, a=annuler, r=recommencer, x=quitter) : ");
            String saisie = clavier.hasNextLine() ? clavier.nextLine().trim().toLowerCase() : "x";

            if (saisie.length() != 1) {
                System.out.println("Commande invalide.");
                continue;
            }
            char commande = saisie.charAt(0);

            if (commande == 'x') {
                System.out.println("Partie abandonnee.");
                enCours = false;
                continue;
            }
            if (commande == 'a') {
                if (!annulerDernierCoup()) {
                    System.out.println("Rien a annuler.");
                }
                continue;
            }
            if (commande == 'r') {
                chargerNiveau(niveauCourant);
                System.out.println("Niveau redemarre.");
                continue;
            }

            int dir = -1;
            for (int d = 0; d < TOUCHES.length; d++) {
                if (TOUCHES[d] == commande) {
                    dir = d;
                }
            }
            if (dir == -1) {
                System.out.println("Commande invalide.");
                continue;
            }

            // on retient s'il y avait une caisse devant, pour pouvoir annuler
            int cibleL = joueurL + DL[dir];
            int cibleC = joueurC + DC[dir];
            boolean pousseePrevue = deplacementValide(cibleL, cibleC) && caisse[cibleL][cibleC];

            if (deplacerJoueur(dir)) {
                historique[nbCoupsJoues] = (char) (dir + (pousseePrevue ? 10 : 0));
                nbCoupsJoues++;
            } else {
                System.out.println("Deplacement impossible vers le " + NOMS_DIR[dir] + ".");
            }
        }
    }

    // ==================================================================
    //  PARTIE D  --  REPRESENTATION D'UN ETAT
    // ==================================================================
    //
    //  Un etat de jeu = position du joueur + positions des caisses.
    //  Les murs et les objectifs ne bougent pas : inutile de les stocker.
    //
    //  On encode l'etat dans une String :
    //      caractere 0      -> index du joueur   (index = ligne * largeur + colonne)
    //      caracteres 1..n  -> index des caisses, TRIES par ordre croissant
    //
    //  Le tri est essentiel : deux configurations ou les memes cases portent une
    //  caisse sont le MEME etat, meme si les caisses y sont arrivees dans un
    //  ordre different. Sans tri, on explorerait plusieurs fois la meme chose.

    static String encoderEtatCourant() {
        char[] code = new char[nbCaisses + 1];
        code[0] = (char) (joueurL * largeur + joueurC);

        int k = 1;
        for (int l = 0; l < hauteur; l++) {
            for (int c = 0; c < largeur; c++) {
                if (caisse[l][c]) {
                    code[k] = (char) (l * largeur + c);
                    k++;
                }
            }
        }
        // le parcours ligne par ligne produit deja des index croissants
        return new String(code);
    }

    /** Partie D : l'etat correspond-il a une victoire ? */
    static boolean etatGagnant(String etat) {
        for (int i = 1; i < etat.length(); i++) {
            int index = etat.charAt(i);
            if (!objectif[index / largeur][index % largeur]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Partie D : calcule l'etat obtenu en jouant la direction dir.
     * @return le nouvel etat, ou null si le mouvement est impossible.
     */
    static String appliquerMouvement(String etat, int dir) {
        int index         = etat.charAt(0);
        int joueurLigne   = index / largeur;
        int joueurColonne = index % largeur;

        int cibleL = joueurLigne   + DL[dir];
        int cibleC = joueurColonne + DC[dir];
        if (!deplacementValide(cibleL, cibleC)) {
            return null;                           // mur ou bord
        }
        int cibleIndex = cibleL * largeur + cibleC;

        // y a-t-il une caisse sur la case cible ?
        int positionCaisse = -1;
        for (int i = 1; i < etat.length(); i++) {
            if (etat.charAt(i) == cibleIndex) {
                positionCaisse = i;
            }
        }

        char[] nouveau = etat.toCharArray();

        if (positionCaisse != -1) {
            int derriereL = cibleL + DL[dir];
            int derriereC = cibleC + DC[dir];
            if (!deplacementValide(derriereL, derriereC)) {
                return null;                       // mur derriere la caisse
            }
            int derriereIndex = derriereL * largeur + derriereC;
            for (int i = 1; i < etat.length(); i++) {
                if (etat.charAt(i) == derriereIndex) {
                    return null;                   // deux caisses a la suite
                }
            }
            if (caisseBloquee(derriereL, derriereC)) {
                return null;                       // bonus : coup qui rend le niveau insoluble
            }
            nouveau[positionCaisse] = (char) derriereIndex;
        }

        nouveau[0] = (char) cibleIndex;
        trierCaisses(nouveau);
        return new String(nouveau);
    }

    /** Tri par insertion des index de caisses (indices 1..n du tableau). */
    static void trierCaisses(char[] code) {
        for (int i = 2; i < code.length; i++) {
            char valeur = code[i];
            int j = i - 1;
            while (j >= 1 && code[j] > valeur) {
                code[j + 1] = code[j];
                j--;
            }
            code[j + 1] = valeur;
        }
    }

    /**
     * Bonus : detection d'un blocage evident. Une caisse poussee dans un coin
     * (deux murs perpendiculaires) ne pourra plus jamais etre deplacee. Si ce
     * coin n'est pas un objectif, le niveau devient insoluble : on coupe la
     * branche de recherche immediatement.
     */
    static boolean caisseBloquee(int l, int c) {
        if (objectif[l][c]) {
            return false;
        }
        boolean murHaut   = !deplacementValide(l - 1, c);
        boolean murBas    = !deplacementValide(l + 1, c);
        boolean murGauche = !deplacementValide(l, c - 1);
        boolean murDroite = !deplacementValide(l, c + 1);

        return (murHaut || murBas) && (murGauche || murDroite);
    }

    // ==================================================================
    //  ENSEMBLE DES ETATS VISITES  --  table de hachage maison
    // ==================================================================
    //
    //  HashMap/HashSet sont interdits : on implemente une table de hachage a
    //  chainage separe avec trois tableaux paralleles.
    //
    //      tetes[b]    -> indice du premier etat du seau b, ou -1 si vide
    //      cles[i]     -> l'etat stocke a l'indice i
    //      suivants[i] -> indice de l'etat suivant du meme seau, ou -1
    //
    //  Cout moyen d'une recherche ou d'une insertion : O(1).

    static final int MAX_ETATS = 400000;
    static final int NB_SEAUX  = 1 << 19;      // puissance de 2 -> masque binaire

    static String[] cles;
    static int[]    suivants;
    static int[]    tetes;
    static int      nbEtatsStockes;
    static boolean  memoireSaturee;

    static void ensembleInitialiser() {
        cles     = new String[MAX_ETATS];
        suivants = new int[MAX_ETATS];
        tetes    = new int[NB_SEAUX];
        for (int b = 0; b < NB_SEAUX; b++) {
            tetes[b] = -1;
        }
        nbEtatsStockes = 0;
        memoireSaturee = false;
    }

    /** Fonction de hachage polynomiale (methode de Horner, base 31). */
    static int hacher(String etat) {
        int h = 17;
        for (int i = 0; i < etat.length(); i++) {
            h = h * 31 + etat.charAt(i);
        }
        return (h & 0x7fffffff) & (NB_SEAUX - 1);
    }

    static boolean ensembleContient(String etat) {
        int seau = hacher(etat);
        for (int i = tetes[seau]; i != -1; i = suivants[i]) {
            if (cles[i].equals(etat)) {
                return true;
            }
        }
        return false;
    }

    static boolean ensembleAjouter(String etat) {
        if (nbEtatsStockes >= MAX_ETATS) {
            memoireSaturee = true;
            return false;
        }
        int seau = hacher(etat);
        cles[nbEtatsStockes]     = etat;
        suivants[nbEtatsStockes] = tetes[seau];
        tetes[seau]              = nbEtatsStockes;
        nbEtatsStockes++;
        return true;
    }

    // ==================================================================
    //  PARTIE E  --  RECHERCHE EN PROFONDEUR (DFS, recursive)
    // ==================================================================

    static final int PROFONDEUR_MAX = 100;

    static char[]  solutionDFS = new char[0];
    static int     longueurDFS;
    static int     etatsExploresDFS;
    static boolean dfsTrouve;
    static long    dureeDFS;

    /** Partie E : lance la recherche en profondeur sur le niveau charge. */
    static boolean resoudreDFS() {
        long debut = System.nanoTime();

        ensembleInitialiser();
        solutionDFS      = new char[PROFONDEUR_MAX];
        longueurDFS      = 0;
        etatsExploresDFS = 0;

        String depart = encoderEtatCourant();
        ensembleAjouter(depart);
        dfsTrouve = explorerProfondeur(depart, 0);

        dureeDFS = (System.nanoTime() - debut) / 1000000;

        System.out.println();
        System.out.println("---------- DFS ----------");
        afficherResultat(dfsTrouve, solutionDFS, longueurDFS, etatsExploresDFS, dureeDFS);
        return dfsTrouve;
    }

    /**
     * Coeur recursif du DFS.
     *
     *  Cas de base       : l'etat est gagnant -> on remonte true.
     *  Condition d'arret : profondeur maximale atteinte, memoire saturee, ou
     *                      plus aucun successeur non visite -> on remonte false.
     *  Cas recursif      : pour chaque direction jouable, on calcule l'etat
     *                      suivant et on s'appelle avec profondeur + 1.
     *
     * Le tableau solutionDFS joue le role de chemin courant : solutionDFS[p]
     * contient le coup joue a la profondeur p.
     */
    static boolean explorerProfondeur(String etat, int profondeur) {
        etatsExploresDFS++;

        if (etatGagnant(etat)) {                 // --- cas de base ---
            longueurDFS = profondeur;
            return true;
        }
        if (profondeur >= PROFONDEUR_MAX || memoireSaturee) {
            return false;                        // --- condition d'arret ---
        }

        for (int dir = 0; dir < 4; dir++) {      // --- cas recursif ---
            String suivant = appliquerMouvement(etat, dir);
            if (suivant == null || ensembleContient(suivant)) {
                continue;
            }
            ensembleAjouter(suivant);
            solutionDFS[profondeur] = TOUCHES[dir];
            if (explorerProfondeur(suivant, profondeur + 1)) {
                return true;
            }
        }
        return false;
    }

    // ==================================================================
    //  PARTIE F  --  RECHERCHE EN LARGEUR (BFS, file maison)
    // ==================================================================
    //
    //  La file est un tableau circulaire d'indices. Pour chaque etat decouvert
    //  on memorise son parent et le coup qui y mene, ce qui permet de
    //  reconstruire le chemin a la fin en remontant les parents.

    static String[] decouverts;      // etats decouverts, dans l'ordre de decouverte
    static int[]    parents;         // indice du parent de chaque etat decouvert
    static char[]   coups;           // coup menant du parent a l'etat
    static int[]    file;            // file circulaire d'indices
    static int      debutFile, finFile, tailleFile;

    static char[]  solutionBFS = new char[0];
    static int     longueurBFS;
    static int     etatsExploresBFS;
    static boolean bfsTrouve;
    static long    dureeBFS;

    static void fileInitialiser() {
        file       = new int[MAX_ETATS];
        debutFile  = 0;
        finFile    = 0;
        tailleFile = 0;
    }

    static void enfiler(int indiceEtat) {
        file[finFile] = indiceEtat;
        finFile = (finFile + 1) % MAX_ETATS;
        tailleFile++;
    }

    static int defiler() {
        int indiceEtat = file[debutFile];
        debutFile = (debutFile + 1) % MAX_ETATS;
        tailleFile--;
        return indiceEtat;
    }

    static boolean fileVide() {
        return tailleFile == 0;
    }

    /**
     * Partie F : recherche en largeur. On explore les etats par distance
     * croissante depuis l'etat initial, donc la premiere solution trouvee est
     * forcement l'une des plus courtes.
     */
    static boolean resoudreBFS() {
        long debut = System.nanoTime();

        ensembleInitialiser();
        fileInitialiser();
        decouverts = new String[MAX_ETATS];
        parents    = new int[MAX_ETATS];
        coups      = new char[MAX_ETATS];

        etatsExploresBFS = 0;
        longueurBFS      = 0;
        bfsTrouve        = false;

        String depart = encoderEtatCourant();
        decouverts[0] = depart;
        parents[0]    = -1;
        ensembleAjouter(depart);
        enfiler(0);

        int nbDecouverts  = 1;
        int indiceArrivee = -1;

        while (!fileVide() && !memoireSaturee) {
            int courant = defiler();
            etatsExploresBFS++;

            if (etatGagnant(decouverts[courant])) {
                indiceArrivee = courant;
                bfsTrouve = true;
                break;
            }

            for (int dir = 0; dir < 4; dir++) {
                String suivant = appliquerMouvement(decouverts[courant], dir);
                if (suivant == null || ensembleContient(suivant)) {
                    continue;
                }
                if (nbDecouverts >= MAX_ETATS) {
                    memoireSaturee = true;
                    break;
                }
                ensembleAjouter(suivant);
                decouverts[nbDecouverts] = suivant;
                parents[nbDecouverts]    = courant;
                coups[nbDecouverts]      = TOUCHES[dir];
                enfiler(nbDecouverts);
                nbDecouverts++;
            }
        }

        // Reconstruction du chemin : on remonte les parents, puis on inverse.
        if (bfsTrouve) {
            int longueur = 0;
            for (int i = indiceArrivee; parents[i] != -1; i = parents[i]) {
                longueur++;
            }
            solutionBFS = new char[longueur];
            longueurBFS = longueur;
            int position = longueur - 1;
            for (int i = indiceArrivee; parents[i] != -1; i = parents[i]) {
                solutionBFS[position] = coups[i];
                position--;
            }
        } else {
            solutionBFS = new char[0];
            longueurBFS = 0;
        }

        dureeBFS = (System.nanoTime() - debut) / 1000000;

        System.out.println();
        System.out.println("---------- BFS ----------");
        afficherResultat(bfsTrouve, solutionBFS, longueurBFS, etatsExploresBFS, dureeBFS);
        return bfsTrouve;
    }

    // ==================================================================
    //  AFFICHAGE DES RESULTATS ET COMPARAISON
    // ==================================================================

    static void afficherResultat(boolean trouve, char[] solution, int longueur,
                                 int etatsExplores, long duree) {
        System.out.println("Solution trouvee : " + (trouve ? "oui" : "non"));
        System.out.println("Mouvements       : " + (trouve ? longueur : 0));
        System.out.println("Etats explores   : " + etatsExplores);
        System.out.println("Temps            : " + duree + " ms");
        if (memoireSaturee) {
            System.out.println("(limite d'etats atteinte : recherche interrompue)");
        }
        if (trouve) {
            StringBuilder chaine = new StringBuilder();
            for (int i = 0; i < longueur; i++) {
                chaine.append(solution[i]);
            }
            System.out.println("Solution         : " + chaine.toString());
        }
    }

    /** Rejoue la solution trouvee sur la grille, coup par coup. */
    static void rejouerSolution(char[] solution, int longueur) {
        if (longueur == 0) {
            return;
        }
        System.out.print("Afficher la solution pas a pas ? (o/n) : ");
        String reponse = clavier.hasNextLine() ? clavier.nextLine().trim().toLowerCase() : "n";
        if (!reponse.equals("o")) {
            return;
        }
        for (int i = 0; i < longueur; i++) {
            for (int dir = 0; dir < TOUCHES.length; dir++) {
                if (TOUCHES[dir] == solution[i]) {
                    deplacerJoueur(dir);
                }
            }
            System.out.println();
            System.out.println("Coup " + (i + 1) + "/" + longueur + " : " + solution[i]);
            afficherGrille();
        }
        System.out.println();
        System.out.println("Victoire ! Nombre de mouvements : " + nbMouvements);
    }

    /** Section 9 du sujet : comparaison des deux algorithmes sur la meme grille. */
    static void comparerAlgorithmes() {
        // On sauvegarde l'etat initial : chaque solveur doit partir de la meme grille.
        String initial = encoderEtatCourant();

        resoudreDFS();
        restaurerEtat(initial);
        resoudreBFS();
        restaurerEtat(initial);

        System.out.println();
        System.out.println("========== COMPARAISON ==========");
        System.out.println("DFS");
        System.out.println("Solution trouvee : " + (dfsTrouve ? "oui" : "non"));
        System.out.println("Mouvements : " + longueurDFS);
        System.out.println("Etats explores : " + etatsExploresDFS);
        System.out.println("Temps : " + dureeDFS + " ms");
        System.out.println("BFS");
        System.out.println("Solution trouvee : " + (bfsTrouve ? "oui" : "non"));
        System.out.println("Mouvements : " + longueurBFS);
        System.out.println("Etats explores : " + etatsExploresBFS);
        System.out.println("Temps : " + dureeBFS + " ms");
        System.out.println("=================================");
        System.out.println("Le BFS explore les etats par distance croissante : sa");
        System.out.println("solution est donc toujours de longueur minimale, au prix");
        System.out.println("d'une file pouvant contenir enormement d'etats.");
        System.out.println("Le DFS descend au plus profond d'abord et s'arrete a la");
        System.out.println("premiere solution rencontree, souvent bien plus longue,");
        System.out.println("mais n'occupe en memoire que la branche courante.");
    }

    /** Replace la grille dynamique dans l'etat encode donne. */
    static void restaurerEtat(String etat) {
        for (int l = 0; l < hauteur; l++) {
            for (int c = 0; c < largeur; c++) {
                caisse[l][c] = false;
            }
        }
        int index = etat.charAt(0);
        joueurL = index / largeur;
        joueurC = index % largeur;
        for (int i = 1; i < etat.length(); i++) {
            int p = etat.charAt(i);
            caisse[p / largeur][p % largeur] = true;
        }
        nbMouvements = 0;
        nbCoupsJoues = 0;
    }
}
