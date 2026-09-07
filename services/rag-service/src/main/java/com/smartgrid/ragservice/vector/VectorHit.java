package com.smartgrid.ragservice.vector;

import java.util.UUID;

public record VectorHit(UUID chunkId, double cosineDistance) {
}
