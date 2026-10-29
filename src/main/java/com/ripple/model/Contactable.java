package com.ripple.model;

/**
 * Anything Ripple can send a message to: donors, volunteers, attendees and beneficiaries.
 *
 * <p>Thanks to this interface the communication code treats all of them the same way
 * (polymorphism). Email or phone may be empty if the person did not give one.
 */
public interface Contactable {

    /**
     * Returns the person's display name.
     *
     * @return the name
     */
    String getName();

    /**
     * Returns the email address.
     *
     * @return the email address, or an empty string if there is none
     */
    String getEmail();

    /**
     * Returns the phone number.
     *
     * @return the phone number, or an empty string if there is none
     */
    String getPhone();
}
