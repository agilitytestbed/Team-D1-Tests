package nl.utwente.ing.testing.bean;

/**
 * The UserMessage class.
 * Used to store information about a UserMessage.
 *
 * @author Daan Kooij
 */
public class UserMessage {

    private String message;
    private String date;
    private boolean read;
    private String type;

    /**
     * An empty constructor of PaymentRequest.
     * Used by the Spring framework.
     */
    public UserMessage() {

    }

    /**
     * A constructor of UserMessage.
     *
     * @param message The message of the to be created UserMessage.
     * @param date    The date of the to be created UserMessage.
     * @param read    The boolean indicating whether the to be created UserMessage is read.
     * @param type    The type of the to be created UserMessage.
     */
    public UserMessage(String message, String date, boolean read, String type) {
        this.message = message;
        this.date = date;
        this.read = read;
        this.type = type;
    }

    /**
     * Method used to retrieve the message of UserMessage.
     *
     * @return The message of UserMessage.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Method used to update the message of UserMessage.
     *
     * @param message The new message of UserMessage.
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Method used to retrieve the date of UserMessage.
     *
     * @return The date of UserMessage.
     */
    public String getDate() {
        return date;
    }

    /**
     * Method used to update the date of UserMessage.
     *
     * @param date The new date of UserMessage.
     */
    public void setDate(String date) {
        this.date = date;
    }

    /**
     * Method used to retrieve the boolean indicating whether UserMessage is read.
     *
     * @return The boolean indicating whether UserMessage is read.
     */
    public boolean getRead() {
        return read;
    }

    /**
     * Method used to update the boolean indicating whether UserMessage is read.
     *
     * @param read The new boolean indicating whether UserMessage is read.
     */
    public void setRead(boolean read) {
        this.read = read;
    }

    /**
     * Method used to retrieve the type of UserMessage.
     *
     * @return The ID of UserMessage.
     */
    public String getType() {
        return type;
    }

    /**
     * Method used to update the type of UserMessage.
     *
     * @param type The new type of UserMessage.
     */
    public void setType(String type) {
        this.type = type;
    }

}
