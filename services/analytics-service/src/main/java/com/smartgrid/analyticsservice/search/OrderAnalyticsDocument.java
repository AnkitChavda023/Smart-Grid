package com.smartgrid.analyticsservice.search;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Map;

@Document(indexName = "analytics_orders")
public class OrderAnalyticsDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String metricType;

    @Field(type = FieldType.Keyword)
    private String windowType;

    @Field(type = FieldType.Long)
    private long windowStart;

    @Field(type = FieldType.Long)
    private long windowEnd;

    @Field(type = FieldType.Double)
    private double value;

    @Field(type = FieldType.Object)
    private Map<String, String> dimensions;

    protected OrderAnalyticsDocument() {
    }

    public OrderAnalyticsDocument(String id, String metricType, String windowType, long windowStart,
            long windowEnd, double value, Map<String, String> dimensions) {
        this.id = id;
        this.metricType = metricType;
        this.windowType = windowType;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.value = value;
        this.dimensions = dimensions;
    }

    public String getId() {
        return id;
    }

    public String getMetricType() {
        return metricType;
    }

    public String getWindowType() {
        return windowType;
    }

    public long getWindowStart() {
        return windowStart;
    }

    public long getWindowEnd() {
        return windowEnd;
    }

    public double getValue() {
        return value;
    }

    public Map<String, String> getDimensions() {
        return dimensions;
    }
}
