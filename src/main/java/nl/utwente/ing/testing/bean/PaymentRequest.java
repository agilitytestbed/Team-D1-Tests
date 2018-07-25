package nl.utwente.ing.testing.bean;

import java.util.List;

/**
 * The PaymentRequest class.
 * Used to store information about a PaymentRequest.
 *
 * @author Daan Kooij
 */
public class PaymentRequest {

    private String description;
    private String due_date;
    private float amount;
    private long number_of_requests;

    /**
     * An empty constructor of PaymentRequest.
     * Used by the Spring framework.
     */
    public PaymentRequest() {

    }

    /**
     * A constructor of PaymentRequest.
     *
     * @param description        The description of the to be created PaymentRequest.
     * @param due_date           The due date of the to be created PaymentRequest.
     * @param amount             The amount of the to be created PaymentRequest.
     * @param number_of_requests The number of requests of the to be created PaymentRequest.
     */
    public PaymentRequest(String description, String due_date, float amount, long number_of_requests) {
        this.description = description;
        this.due_date = due_date;
        this.amount = amount;
        this.number_of_requests = number_of_requests;
    }

    /**
     * Method used to retrieve the description of PaymentRequest.
     *
     * @return The description of PaymentRequest.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Method used to update the description of PaymentRequest.
     *
     * @param description The new description of PaymentRequest.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Method used to retrieve the due date of PaymentRequest.
     *
     * @return The due date of PaymentRequest.
     */
    public String getDue_date() {
        return due_date;
    }

    /**
     * Method used to update the due date of PaymentRequest.
     *
     * @param due_date The new due date of PaymentRequest.
     */
    public void setDue_date(String due_date) {
        this.due_date = due_date;
    }

    /**
     * Method used to retrieve the amount of PaymentRequest.
     *
     * @return The amount of PaymentRequest.
     */
    public float getAmount() {
        return amount;
    }

    /**
     * Method used to update the amount of PaymentRequest.
     *
     * @param amount The new amount of PaymentRequest.
     */
    public void setAmount(float amount) {
        this.amount = amount;
    }

    /**
     * Method used to retrieve the number of requests of PaymentRequest.
     *
     * @return The number of requests of PaymentRequest.
     */
    public long getNumber_of_requests() {
        return number_of_requests;
    }

    /**
     * Method used to update the number of requests of PaymentRequest.
     *
     * @param number_of_requests The new number of requests of PaymentRequest.
     */
    public void setNumber_of_requests(long number_of_requests) {
        this.number_of_requests = number_of_requests;
    }

}
