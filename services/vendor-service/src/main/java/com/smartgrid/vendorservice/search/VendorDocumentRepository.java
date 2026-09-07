package com.smartgrid.vendorservice.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface VendorDocumentRepository extends ElasticsearchRepository<VendorDocument, String> {
}
