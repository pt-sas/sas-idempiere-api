package com.sahabatabadi.api.salesorder;

public class SASSalesOrderLine {
	private SASSalesOrder header;

    public int lineNo;              // Line: 10
    public String productId;        // M_Product_ID[Value]: AB0301485
    public int quantity;            // QtyEntered: 19
    // public String uom;              // C_UOM_ID[Name]: PCS
    // public String tax;              // C_Tax_ID[Name]: PPN
    // public int freightAmt;          // FreightAmt: 0
    // public char processed;          // Processed: Y
    public String documentNo;       // OPTIONAL: C_Order_ID[DocumentNo]: ATR1-OPN-1001-0018
    public String datePromised;     // OPTIONAL: DatePromised: 2020-01-02
    // public int setInstance;         // M_AttributeSetInstance_ID: 0
    // public int discountListId;      // SAS_DiscountList_ID[Value]: 500266
    // public char isAffectPRomo;      // IsAffectPromo: N

    public SASSalesOrderLine(BizzySalesOrderLine orderLine, SASSalesOrder header) {
        /* parsing values from Bizzy SO Line */
        this.header = header;
        this.productId = orderLine.productId;
        this.quantity = orderLine.quantity; // TODO ask about natura (- 1)

        /* calculating values */
        this.lineNo = header.getNextLineNumber();
        this.documentNo = this.header.documentNo;
        this.datePromised = this.header.datePromised;
    }
}
