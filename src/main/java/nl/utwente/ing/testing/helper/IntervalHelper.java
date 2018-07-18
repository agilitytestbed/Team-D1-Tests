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

    /**
     * Method used to convert a date String object in the format that the DPA uses to a month identifier using the
     * following formula: (currentYear - 1970) * 12 + (monthValue - 1).
     * This method helps in figuring out how many months have elapsed between events (such as Transactions).
     * @param dateString The String object representing a date in the format the DPA uses from which the month
     *                   identifier should be retrieved.
     * @return The month identifier of the String object representing a date in the format the DPA uses.
     */
    public static int getMonthIdentifier(String dateString) {
        LocalDateTime date = LocalDateTime.parse(dateString.split("Z")[0]);
        return (date.getYear() - 1970) * 12 + (date.getMonthValue() - 1);
    }

}
