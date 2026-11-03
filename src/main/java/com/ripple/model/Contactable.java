package com.ripple.model;

/**
 * Anyone we can send a message to. Email or phone can be empty.
 */
public interface Contactable {

    /** The person's name. */
    String getName();

    /** Email, or empty. */
    String getEmail();

    /** Phone, or empty. */
    String getPhone();

    /** Social media handle, or empty. Only donors have one. */
    default String getSocialHandle() {
        return "";
    }
}
