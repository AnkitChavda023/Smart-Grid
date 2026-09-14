package com.smartgrid.vendorservice.search;

import com.smartgrid.vendorservice.domain.Vendor;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class VendorSearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final VendorDocumentRepository vendorDocumentRepository;

    public VendorSearchService(ElasticsearchOperations elasticsearchOperations, VendorDocumentRepository vendorDocumentRepository) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.vendorDocumentRepository = vendorDocumentRepository;
    }

    public void index(Vendor vendor) {
        GeoPoint location = (vendor.getLatitude() != null && vendor.getLongitude() != null)
                ? new GeoPoint(vendor.getLatitude(), vendor.getLongitude())
                : null;
        vendorDocumentRepository.save(new VendorDocument(
                vendor.getId().toString(),
                vendor.getName(),
                vendor.getRegion(),
                vendor.getCapabilities(),
                vendor.getContact(),
                vendor.getCategory(),
                vendor.getCertifications(),
                location));
    }

    public List<VendorDocument> search(String query, Double lat, Double lon, Double radiusKm) {
        return search(query, null, null, null, lat, lon, radiusKm);
    }

    public List<VendorDocument> search(String query, String category, String region, String certification,
                                       Double lat, Double lon, Double radiusKm) {
        boolean hasQuery = query != null && !query.isBlank();
        boolean hasCategory = category != null && !category.isBlank() && !"all".equalsIgnoreCase(category);
        boolean hasRegion = region != null && !region.isBlank() && !"all".equalsIgnoreCase(region);
        boolean hasCert = certification != null && !certification.isBlank();
        boolean hasGeo = lat != null && lon != null && radiusKm != null;

        if (!hasQuery && !hasCategory && !hasRegion && !hasCert && !hasGeo) {
            List<VendorDocument> all = new ArrayList<>();
            vendorDocumentRepository.findAll().forEach(all::add);
            return all;
        }

        Criteria criteria = null;

        if (hasQuery) {
            String q = query.trim();
            criteria = Criteria.where("name").contains(q)
                    .or(Criteria.where("capabilities").contains(q))
                    .or(Criteria.where("region").contains(q))
                    .or(Criteria.where("category").contains(q))
                    .or(Criteria.where("certifications").contains(q));
        }

        if (hasCategory) {
            Criteria catCriteria = Criteria.where("category").is(category.trim());
            criteria = criteria == null ? catCriteria : criteria.and(catCriteria);
        }

        if (hasRegion) {
            Criteria regCriteria = Criteria.where("region").is(region.trim());
            criteria = criteria == null ? regCriteria : criteria.and(regCriteria);
        }

        if (hasCert) {
            Criteria certCriteria = Criteria.where("certifications").contains(certification.trim());
            criteria = criteria == null ? certCriteria : criteria.and(certCriteria);
        }

        if (hasGeo) {
            Criteria geoCriteria = Criteria.where("location").within(new GeoPoint(lat, lon), radiusKm + "km");
            criteria = criteria == null ? geoCriteria : criteria.and(geoCriteria);
        }

        CriteriaQuery criteriaQuery = new CriteriaQuery(criteria);
        return elasticsearchOperations.search(criteriaQuery, VendorDocument.class)
                .stream()
                .map(SearchHit::getContent)
                .toList();
    }
}
