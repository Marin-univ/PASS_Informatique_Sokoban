import java.util.Scanner;

public class Sokoban {

    static final char MUR                 = '#';
    static final char LIBRE               = ' ';
    static final char JOUEUR              = '@';
    static final char CAISSE              = '$';
    static final char OBJECTIF            = '.';
    static final char CAISSE_SUR_OBJECTIF = '*';
    static final char JOUEUR_SUR_OBJECTIF = '+';

    static final char[]   TOUCHES  = { 'z',    's',   'q',      'd'      };
    static final int[]    DL       = {  -1,     1,     0,        0       };
    static final int[]    DC       = {   0,     0,    -1,        1       };
    static final String[] NOMS_DIR = { "haut", "bas", "gauche", "droite" };

    static final String[] NIVEAU = {
        "########",
        "#      #",
        "#  .   #",
        "#  $   #",
        "#  @   #",
        "#      #",
        "########"
    };

    static boolean[][] mur;
    static boolean[][] objectif;
    static boolean[][] caisse;
    static int hauteur, largeur;
    static int joueurL, joueurC;
    static int nbCaisses;
    static int nbMouvements;

    static final Scanner clavier = new Scanner(System.in);

    public static void main(String[] args) {
        chargerNiveau();
        jouer();
    }

    static void chargerNiveau() {
        hauteur = NIVEAU.length;
        largeur = 0;
        for (int l = 0; l < hauteur; l++) {
            if (NIVEAU[l].length() > largeur) {
                largeur = NIVEAU[l].length();
            }
        }

        mur      = new boolean[hauteur][largeur];
        objectif = new boolean[hauteur][largeur];
        caisse   = new boolean[hauteur][largeur];
        nbCaisses    = 0;
        nbMouvements = 0;
        joueurL = -1;
        joueurC = -1;

        for (int l = 0; l < hauteur; l++) {
            for (int c = 0; c < largeur; c++) {
                char symbole = (c < NIVEAU[l].length()) ? NIVEAU[l].charAt(c) : LIBRE;

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

    static boolean deplacementValide(int l, int c) {
        return l >= 0 && l < hauteur && c >= 0 && c < largeur && !mur[l][c];
    }

    static boolean peutPousser(int l, int c, int dir) {
        int derriereL = l + DL[dir];
        int derriereC = c + DC[dir];
        return deplacementValide(derriereL, derriereC) && !caisse[derriereL][derriereC];
    }

    static void deplacerCaisse(int l, int c, int dir) {
        caisse[l][c] = false;
        caisse[l + DL[dir]][c + DC[dir]] = true;
    }

    static boolean deplacerJoueur(int dir) {
        int cibleL = joueurL + DL[dir];
        int cibleC = joueurC + DC[dir];

        if (!deplacementValide(cibleL, cibleC)) {
            return false;
        }
        if (caisse[cibleL][cibleC]) {
            if (!peutPousser(cibleL, cibleC, dir)) {
                return false;
            }
            deplacerCaisse(cibleL, cibleC, dir);
        }
        joueurL = cibleL;
        joueurC = cibleC;
        nbMouvements++;
        return true;
    }

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

            System.out.print("Commande (z/s/q/d, x=quitter) : ");
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

            if (!deplacerJoueur(dir)) {
                System.out.println("Deplacement impossible vers le " + NOMS_DIR[dir] + ".");
            }
        }
    }
}
