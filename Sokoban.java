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

    static final String[][] NIVEAUX = {
        {
            "########",
            "#      #",
            "#  .   #",
            "#  $   #",
            "#  @   #",
            "#      #",
            "########"
        },
        {
            "#######",
            "#     #",
            "#  $  #",
            "#  .  #",
            "#  @  #",
            "#     #",
            "#######"
        },
        {
            "########",
            "#      #",
            "#  $$  #",
            "#  ..  #",
            "#  @   #",
            "#      #",
            "########"
        },
        {
            "########",
            "#      #",
            "#  .$  #",
            "#      #",
            "#  $.  #",
            "#  @   #",
            "########"
        },
        {
            "##########",
            "#        #",
            "#  $  $  #",
            "#  .  .  #",
            "#        #",
            "#  .  .  #",
            "#  $  $  #",
            "#   @    #",
            "##########"
        },
        {
            "#######",
            "#  .  #",
            "# $*$ #",
            "#  +  #",
            "#  $  #",
            "#  .  #",
            "#######"
        },
        {
            "#########",
            "#       #",
            "#  ###  #",
            "#  $ $  #",
            "#  . .  #",
            "#   @   #",
            "#########"
        },
        {
            "##########",
            "#  #     #",
            "#  #  .  #",
            "#  $     #",
            "#  @  .  #",
            "#  $     #",
            "#  #     #",
            "##########"
        },
        {
            "#########",
            "#.      #",
            "#.      #",
            "#. $$$  #",
            "#   @   #",
            "#########"
        },
        {
            "##########",
            "#..#     #",
            "#.. $ $  #",
            "#        #",
            "#   $ $  #",
            "#       @#",
            "##########"
        }
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
        boolean enCours = true;
        while (enCours) {
            int choix = menuPrincipal();
            if (choix == 1) {
                chargerNiveau(0);
                jouer();
            } else if (choix == 2) {
                menuNiveaux();
            } else {
                enCours = false;
            }
        }
        System.out.println("Au revoir !");
    }

    static int menuPrincipal() {
        while (true) {
            System.out.println();
            System.out.println("===== SOKOBAN =====");
            System.out.println("1. Jouer");
            System.out.println("2. Choix du niveau");
            System.out.println("3. Quitter");
            System.out.print("Votre choix : ");

            int choix = lireChoix(3);
            if (choix != -1) {
                return choix;
            }
            System.out.println("Choix invalide.");
        }
    }

    static void menuNiveaux() {
        while (true) {
            System.out.println();
            System.out.println("===== CHOIX DU NIVEAU =====");
            for (int i = 0; i < NIVEAUX.length; i++) {
                System.out.println((i + 1) + ". Niveau " + (i + 1));
            }
            System.out.println((NIVEAUX.length + 1) + ". Retour");
            System.out.print("Votre choix : ");

            int choix = lireChoix(NIVEAUX.length + 1);
            if (choix == -1) {
                System.out.println("Choix invalide.");
            } else if (choix == NIVEAUX.length + 1) {
                return;
            } else {
                chargerNiveau(choix - 1);
                jouer();
                return;
            }
        }
    }

    static int lireChoix(int max) {
        String saisie = clavier.hasNextLine() ? clavier.nextLine().trim() : "";
        if (saisie.isEmpty()) {
            return max;
        }

        try {
            int choix = Integer.parseInt(saisie);
            if (choix >= 1 && choix <= max) {
                return choix;
            }
        } catch (NumberFormatException e) {
        }
        return -1;
    }

    static void chargerNiveau(int indice) {
        String[] plan = NIVEAUX[indice];

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
        joueurL = -1;
        joueurC = -1;

        for (int l = 0; l < hauteur; l++) {
            for (int c = 0; c < largeur; c++) {
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
        while (true) {
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
                return;
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
