package me.isaiah.mods.chestshop;

import me.isaiah.mods.chestshop.interfaces.ISign;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.regex.Pattern;

public class ChestShopSign {

    public static final byte NAME_LINE = 0;
    public static final byte QUANTITY_LINE = 1;
    public static final byte PRICE_LINE = 2;
    public static final byte ITEM_LINE = 3;

    public static final Pattern[] SHOP_SIGN_PATTERN = {
            Pattern.compile("^?[\\w -.:]*$"),
            Pattern.compile("^[1-9][0-9]{0,5}$"),
            Pattern.compile("(?i)^[\\d.bs(free) :]+$"),
            Pattern.compile("^[\\w? #:-]+$")
    };
    public static final String AUTOFILL_CODE = "?";

    public static String[] readText(ISign sign) {
        Text[] text = sign.chestshop_getText();
        return new String[] {text[0].asString(), text[1].asString(), text[2].asString(), text[3].asString()};
    }

    public static boolean isValid(ISign sign) {
        Text[] text = sign.chestshop_getText();
        String[] txt = new String[] {text[0].asString(), text[1].asString(), text[2].asString(), text[3].asString()};
        return isValid(txt);
    }

    public static boolean isValid(String[] line) {
        return isValidPreparedSign(line) && (line[PRICE_LINE].toUpperCase(Locale.ROOT).contains("B") || line[PRICE_LINE].toUpperCase(Locale.ROOT).contains("S")) && !line[NAME_LINE].isEmpty();
    }

    public static double getBuyPrice(ISign sign) {
        String[] txt = readText(sign);
        String priceLine = txt[PRICE_LINE];
        if (!priceLine.toUpperCase(Locale.ROOT).contains("B"))
            return -1;

        String priceString = priceLine.toUpperCase(Locale.ROOT).substring(priceLine.indexOf("B")+1).trim().split(":")[0];
        if (priceString.equalsIgnoreCase("free"))
            return 0;
        return Double.valueOf(priceString);
    }

    public static double getSellPrice(ISign sign) {
        return 0.0;
    }

    public static boolean isValidPreparedSign(String[] lines) {
        for (int i = 0; i < 4; i++)
            if (!SHOP_SIGN_PATTERN[i].matcher(lines[i]).matches())
                return false;

        return lines[PRICE_LINE].indexOf(':') == lines[PRICE_LINE].lastIndexOf(':');
    }

}