package apidez.com.databinding.utils;

import java.util.Random;

/**
 * Created by nongdenchet on 10/21/15.
 */
public class StringUtils {
    public static String generateString(String characters, int length) {
        Random rand = new Random();
        char[] text = new char[length];
        for (int i = 0; i < length; i++) {
            text[i] = characters.charAt(rand.nextInt(characters.length()));
        }
        return new String(text);
    }

    public static boolean isEmpty(String string) {
        return string == null || string.isEmpty();
    }
}
