package lol.unic.gytags.cache;

import java.text.Normalizer;
import java.util.Locale;

public final class NicknameNormalizer {
    private NicknameNormalizer() {
    }

    public static String normalize(String nickname) {
        if (nickname == null) {
            return "";
        }
        return Normalizer.normalize(nickname.strip(), Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
    }
}
