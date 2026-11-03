package com.ripple.model;

/**
 * A donation type, like Zakat. Tax-deductible types add a tax section to the receipt (CR4).
 */
public class DonationType extends LookupEntry {

    private boolean taxDeductible;

    /** Creates a donation type. */
    public DonationType(int id, String name, String description, boolean taxDeductible) {
        super(id, name, description);
        this.taxDeductible = taxDeductible;
    }

    public boolean isTaxDeductible() {
        return taxDeductible;
    }

    public void setTaxDeductible(boolean taxDeductible) {
        this.taxDeductible = taxDeductible;
    }
}
