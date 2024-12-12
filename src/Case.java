public class Case {
    private enum etat {
        ROUGE,
        JAUNE,
        VIDE
    }

    private etat etat;

    public Case() {
        etat = etat.VIDE;
    }

    public void afficher() {
        switch (etat) {
            case ROUGE:
                System.out.print("R ");
                break;
            case JAUNE:
                System.out.print("J ");
                break;
            case VIDE:
                System.out.print("[] ");
                break;
        }
    }

    public void jouer(char couleur){
        switch (couleur) {
            case 'R':
                etat = etat.ROUGE;
                break;
            case 'J':
                etat = etat.JAUNE;
                break;
        }
    }


    public char getEtat() {
        switch (this.etat) {
            case ROUGE:
                return 'R';
            case JAUNE:
                return 'J';
            default:
                break;
        }
        return 'V';
    }
}
