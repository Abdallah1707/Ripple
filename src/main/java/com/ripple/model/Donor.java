package com.ripple.model;

/**
 * A donor. The preferred channel is used for thank-you messages (CR5).
 */
public class Donor extends Person {

    private final String socialHandle;
    private final ContactChannel preferredChannel;

    /** Creates a donor without a social handle. */
    public Donor(int id, String name, String email, String phone, ContactChannel preferredChannel) {
        this(id, name, email, phone, "", preferredChannel);
    }

    /** Creates a donor. */
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
