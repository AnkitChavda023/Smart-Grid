package com.smartgrid.vendorservice.dto;

import com.smartgrid.vendorservice.domain.VendorSku;

public record VendorSkuResponse(String skuId, double price, int leadTimeDays) {

    public static VendorSkuResponse from(VendorSku sku) {
        return new VendorSkuResponse(sku.getSkuId(), sku.getPrice(), sku.getLeadTimeDays());
    }
}
