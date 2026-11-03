package com.ripple.model;

/**
 * A person or company that gives donations. The preferred channel decides how the donor
 * receives the automatic thank-you message (client requirement CR5): by email, SMS or a
 * social media message to their handle.
 */
public class Donor extends Person {

    private final String socialHandle;
    private final ContactChannel preferredChannel;

    /**
     * Creates a donor without a social media handle.
     *
     * @param id               database id, or 0 if not saved yet
     * @param name             display name
     * @param email            email address, or an empty string
     * @param phone            phone number, or an empty string
     * @param preferredChannel how the donor wants to be contacted
     */
    public Donor(int id, String name, String email, String phone, ContactChannel preferredChannel) {
        this(id, name, email, phone, "", preferredChannel);
    }

    /**
     * Creates a donor.
     *
     * @param id               database id, or 0 if not saved yet
     * @param name             display name
     * @param email            email address, or an empty string
     * @param phone            phone number, or an empty string
     * @param socialHandle     social media handle such as {@code "@mona_ali"}, or an empty string
     * @param preferredChannel how the donor wants to be contacted
     */
    public Donor(int id, String name, String email, String phone, String socialHandle,
                 ContactChannel preferredChannel) {
        super(id, name, email, phone);
        this.socialHandle = socialHandle == null ? "" : socialHandle;
        this.preferredChannel = preferredChannel;
    }

    @Override
    public String getSocialHandle() {
        return socialHandle;
    }

    public ContactChannel getPreferredChannel() {
        return preferredChannel;
    }
}
