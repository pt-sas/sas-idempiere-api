package com.sahabatabadi.api.salesorder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.compiere.util.CLogger;

import com.sahabatabadi.api.DocumentInjector;

/**
 * Class to inject Bizzy Sales Order objects into iDempiere. This class receives
 * a single bizzy SO with potentially mixed principals and discounts, splits the
 * bizzy SO into multiple sales orders based on principal and discount, converts 
 * them into a SAS SO objects, then inserts the SAS SO objects using 
 * {@link DocumentInjector}.
 * 
 * @author Nicholas Alexander Limit
 * @version 1.0
 */
public class SalesOrderInjector {
    /**
     * Window ID for Sales Order window in iDempiere
     */
    public static final int SALES_ORDER_WINDOW_ID = 143;

    /**
     * Menu ID for Sales Order menu in iDempiere
     */
    public static final int SALES_ORDER_MENU_ID = 129;

    protected static CLogger log = CLogger.getCLogger(SalesOrderInjector.class);
    
    /**
     * Injects the specified Bizzy Sales Order into iDempiere
     * 
     * @param bizzySo Bizzy Sales Order object to be injected
     * @return Document numbers of the documents successfully inserted
     */
    public static String injectSalesOrder(BizzySalesOrder bizzySo) {
        for (BizzySalesOrderLine soLine : bizzySo.orderLines) {
            String principal = SalesOrderUtils.getProductPrincipal(soLine.productId);
            soLine.principalId = principal;
            soLine.discount = SalesOrderUtils.getProductDiscount(soLine.productId, bizzySo.bpHoldingNo, principal); // setting double as an int
        }

        ArrayList<String> insertedDocNums = new ArrayList<>();

        ArrayList<BizzySalesOrderLine[]> groupedSoLines = splitSoLines(bizzySo.orderLines);
        for (BizzySalesOrderLine[] soLineGroup : groupedSoLines) {
            BizzySalesOrder splitBizzySo = new BizzySalesOrder(bizzySo);
            splitBizzySo.orderLines = soLineGroup;

            SASSalesOrder sasSo = new SASSalesOrder(splitBizzySo);
            DocumentInjector inj = new DocumentInjector(SALES_ORDER_WINDOW_ID, SALES_ORDER_MENU_ID);
            boolean injectSuccess = inj.injectDocument(sasSo);

            if (injectSuccess) {
                insertedDocNums.add(sasSo.documentNo);
            }
        }

        return insertedDocNums.toString();
    }

    /**
     * Helper method to split Bizzy SO lines based on principal and discount
     * 
     * @param bizzySoLines array of Bizzy SO lines to be split
     * @return ArrayList of SO lines split by principal and discount
     */
    private static ArrayList<BizzySalesOrderLine[]> splitSoLines(BizzySalesOrderLine[] bizzySoLines) {
        HashMap<String, HashMap<Double, ArrayList<BizzySalesOrderLine>>> principalGrouping = new HashMap<>();

        for (int i = 0; i < bizzySoLines.length; i++) {
            String principal = bizzySoLines[i].principalId;
            double discount = bizzySoLines[i].discount;

            if (!principalGrouping.containsKey(principal)) {
                principalGrouping.put(principal, new HashMap<Double, ArrayList<BizzySalesOrderLine>>());
            }

            HashMap<Double, ArrayList<BizzySalesOrderLine>> discountGrouping = principalGrouping.get(principal);

            if (!discountGrouping.containsKey(discount)) {
                discountGrouping.put(discount, new ArrayList<BizzySalesOrderLine>());
            }

            ArrayList<BizzySalesOrderLine> groupedLines = discountGrouping.get(discount);

            groupedLines.add(bizzySoLines[i]);
        }

        ArrayList<BizzySalesOrderLine[]> toReturn = new ArrayList<>();

        for (Map.Entry<String, HashMap<Double, ArrayList<BizzySalesOrderLine>>> mapElement : principalGrouping.entrySet()) {
            HashMap<Double, ArrayList<BizzySalesOrderLine>> discountGrouping = mapElement.getValue();

            for (Map.Entry<Double, ArrayList<BizzySalesOrderLine>> innerMapElement : discountGrouping.entrySet()) {
                ArrayList<BizzySalesOrderLine> groupedLines = innerMapElement.getValue();
                toReturn.add(groupedLines.toArray(new BizzySalesOrderLine[groupedLines.size()]));
            }
        }
        
        return toReturn;
    }    
}
