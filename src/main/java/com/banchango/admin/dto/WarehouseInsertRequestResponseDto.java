package com.banchango.admin.dto;

import com.banchango.domain.warehouses.Warehouses;
import com.banchango.tools.DateConverter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class WarehouseInsertRequestResponseDto {

    private Integer warehouseId;
    private String name;
    private String createdAt;

    public WarehouseInsertRequestResponseDto(Warehouses warehouse) {
        this.warehouseId = warehouse.getId();
        this.name = warehouse.getName();
        this.createdAt = DateConverter.convertDateWithTime(warehouse.getCreatedAt());
    }
}
