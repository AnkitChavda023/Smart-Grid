package com.smartgrid.contractservice.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface ContractDocumentRepository extends ElasticsearchRepository<ContractDocument, String> {

    List<ContractDocument> findByTermsContaining(String term);
}
