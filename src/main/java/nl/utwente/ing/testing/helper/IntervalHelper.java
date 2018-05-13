package nl.utwente.ing.testing.helper;

import java.time.LocalDateTime;

/**
 * The IntervalHelper class.
 * Used to generate intervals and compare dates.
 *
 * @author Daan Kooij
 */
public class IntervalHelper {

    /**
     * Method used to convert a LocalDateTime object to a String, in the format that the DPA uses.
     *
     * @param localDateTime The LocalDateTime object that should be converted to a String.
     * @return A String object in the format that the DPA uses that reflects the converted LocalDateTime object.
     */
    public static String dateToString(LocalDateTime localDateTime) {
        return localDateTime.toString() + "Z";
    }

}
