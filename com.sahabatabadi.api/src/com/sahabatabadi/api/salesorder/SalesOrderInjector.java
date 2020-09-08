package com.sahabatabadi.api.salesorder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.sahabatabadi.api.DocumentInjector;
import com.sahabatabadi.api.ThreadPoolManager;

/**
 * Class to inject {@link BizzySalesOrder} into iDempiere. This class receives a
 * single {@link BizzySalesOrder} with potentially mixed principals and
 * discounts and splits the Bizzy SO into multiple sales orders based on
 * principal and discount. Next, they this class converts them into
 * {@link SASSalesOrder} objects, then inserts the SAS SO objects using
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
    
    /**
     * Injects the specified Bizzy Sales Order into iDempiere
     * 
     * @param bizzySo Bizzy Sales Order object to be injected
     * @return Document numbers of the documents successfully inserted
     */
    public String injectSalesOrder(BizzySalesOrder bizzySo) {
        for (BizzySalesOrderLine soLine : bizzySo.orderLines) {
            String principal = SalesOrderUtils.getProductPrincipal(soLine.productId);
            soLine.principalId = principal;
            soLine.discount = SalesOrderUtils.getProductDiscount(soLine.productId, bizzySo.bpHoldingNo, principal);
        }

        ArrayList<BizzySalesOrderLine[]> groupedSoLines = splitSoLines(bizzySo.orderLines);

        ArrayList<Future<String>> pendingResults = new ArrayList<>();
        for (BizzySalesOrderLine[] soLineGroup : groupedSoLines) {
            BizzySalesOrder splitBizzySo = new BizzySalesOrder(bizzySo);
            splitBizzySo.orderLines = soLineGroup;

            SASSalesOrder sasSo = new SASSalesOrder(splitBizzySo);
            SalesOrderInjectorThread task = new SalesOrderInjectorThread(sasSo);
            Future<String> res = ThreadPoolManager.getExecutor().submit(task);
            pendingResults.add(res);
        }

        ArrayList<String> insertedDocNums = new ArrayList<>();
        for (Future<String> result : pendingResults) {
            try {
                String docNum = result.get();
                if (docNum != null) {
                    insertedDocNums.add(docNum);
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        return insertedDocNums.toString();
    }

    /**
     * Wrapper class for the injector thread
     */
    class SalesOrderInjectorThread implements Callable<String> {
        /**
         * SAS SO object to be inserted by this class
         */
        private SASSalesOrder sasSo;

        /**
         * Default constructor
         * 
         * @param sasSo SAS SO object to be inserted in {@link #call()}
         */
        public SalesOrderInjectorThread(SASSalesOrder sasSo) {
            this.sasSo = sasSo;
        }

        /**
         * @return document number of the injected, or null if inject failed
         */
        public String call() {
            DocumentInjector inj = new DocumentInjector(SALES_ORDER_WINDOW_ID, SALES_ORDER_MENU_ID);
            boolean injectSuccess = inj.injectDocument(sasSo);
            return injectSuccess ? sasSo.documentNo : null;
        }
    }

    /**
     * Helper method to split Bizzy SO lines based on principal and discount
     * 
     * @param bizzySoLines array of Bizzy SO lines to be split
     * @return ArrayList of SO lines split by principal and discount
     */
    private ArrayList<BizzySalesOrderLine[]> splitSoLines(BizzySalesOrderLine[] bizzySoLines) {
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
