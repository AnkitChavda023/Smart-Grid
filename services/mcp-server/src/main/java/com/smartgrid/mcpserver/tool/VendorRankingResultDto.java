package com.smartgrid.mcpserver.tool;

public record VendorRankingResultDto(String vendorId, String vendorName, double compositeScore,
                                      double price, int leadTimeDays, double reliabilityScore) {
}
