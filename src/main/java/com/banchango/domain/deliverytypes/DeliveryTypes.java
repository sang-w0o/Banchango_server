package com.banchango.domain.deliverytypes;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@NoArgsConstructor
@Getter
@Entity
@Table(name = "delivery_types")
public class DeliveryTypes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 30, nullable = false)
    private String name;

    @Column
    private Integer warehouseId;

    @Builder
    public DeliveryTypes(String name, Integer warehouseId) {
        this.name = name;
        this.warehouseId = warehouseId;
    }
}
