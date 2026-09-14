package com.smartgrid.contractservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "sla_terms")
public class SlaTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Column(name = "metric_name", nullable = false)
    private String metricName;

    @Column(name = "threshold_value", nullable = false)
    private double thresholdValue;

    @Column(name = "penalty_per_breach", nullable = false)
    private double penaltyPerBreach;

    protected SlaTerm() {
    }

    public SlaTerm(String metricName, double thresholdValue, double penaltyPerBreach) {
        this.metricName = metricName;
        this.thresholdValue = thresholdValue;
        this.penaltyPerBreach = penaltyPerBreach;
    }

    public void setContract(Contract contract) {
        this.contract = contract;
    }

    public UUID getId() {
        return id;
    }

    public Contract getContract() {
        return contract;
    }

    public String getMetricName() {
        return metricName;
    }

    public double getThresholdValue() {
        return thresholdValue;
    }

    public double getPenaltyPerBreach() {
        return penaltyPerBreach;
    }
}
