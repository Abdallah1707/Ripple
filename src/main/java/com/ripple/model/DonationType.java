package com.ripple.model;

/**
 * Lookup value that classifies a donation, e.g. "Zakat" or "Food Supplies".
 *
 * <p>The tax-deductible flag decides whether a receipt gets a tax section (CR4).
 */
public class DonationType extends LookupEntry {

    private boolean taxDeductible;

    /**
     * Creates a donation type.
     *
     * @param id            database id, or 0 if not saved yet
     * @param name          unique name
     * @param description   optional description
     * @param taxDeductible whether donations of this type are tax-deductible
     */
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
