package com.banchango.estimates.dto;

import com.banchango.domain.estimates.EstimateStatus;
import com.banchango.domain.estimates.Estimates;
import com.banchango.warehouses.dto.WarehouseSummaryDto;
import lombok.Getter;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@Getter
public class EstimateSearchDto {
    private Integer id;
    private WarehouseSummaryDto warehouse;
    private EstimateStatus status;

    public EstimateSearchDto(Estimates estimate) {
        this.id = estimate.getId();
        this.warehouse = null;
        this.status = estimate.getStatus();
    }

    public void updateWarehouse(WarehouseSummaryDto warehouse) {
        this.warehouse = warehouse;
    }
}
