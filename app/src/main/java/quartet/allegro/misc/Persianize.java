package quartet.allegro.misc;

/**
 * Created by akbar on 5/25/15.
 */
public class Persianize {

    public static String persianizeNumber(int num){

        String str = String.valueOf(num);
        StringBuilder sb = new StringBuilder();

        for (int i=0; i<str.length(); i++) {

            char c ;

            switch (str.charAt(i)) {
                case '1':
                    c = '۱';
                    break;
                case '2':
                    c = '۲';
                    break;
                case '3':
                    c = '۳';
                    break;
                case '4':
                    c = '۴';
                    break;
                case '5':
                    c = '۵';
                    break;
                case '6':
                    c = '۶';
                    break;
                case '7':
                    c = '۷';
                    break;
                case '8':
                    c = '۸';
                    break;
                case '9':
                    c = '۹';
                    break;
                case '0':
                    c = '۰';
                    break;
                default:
                    c = 'x';
                    break;
            }

            if (c != 'x')
                sb.append(c);
        }

        return sb.toString();
    }
}
