package com.smartgrid.shipmentservice.dto;

public record LiveLocationMessage(double latitude, double longitude, Double etaMinutes, String status) {
}
