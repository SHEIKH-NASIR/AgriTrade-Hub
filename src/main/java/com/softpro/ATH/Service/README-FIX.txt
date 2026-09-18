ROUTE OPTIMIZATION FIX

1. Replace GeocodingService.java with the supplied version.
2. Replace RouteOptimizationService.java with the supplied version.
3. FPOShipment.java and FPOShipmentRepo.java do not need changes for this fix.
4. Do NOT create new demo orders just because the old shipments were created earlier.
   The new RouteOptimizationService backfills missing coordinates from the addresses
   already stored on those FPOShipment records.
5. Make sure each merchant/shipment destination contains at least:
   locality/area, city, state and preferably the 6-digit Indian PIN code.
6. Replace the example User-Agent contact email in GeocodingService.java with your
   real project/contact email before deployment.
7. Restart Spring Boot and click Route Optimization again on the existing 3 shipments.

If the existing shipment's destinationAddress is blank/wrong, editing only the Merchant
address will NOT automatically rewrite an already-created shipment. In that case update
the shipment destination (or add a synchronization step when the merchant address changes).
