package com.banchango.admin;

import com.banchango.ApiTestContext;
import com.banchango.admin.dto.EstimateStatusUpdateRequestDto;
import com.banchango.admin.dto.WarehouseAdminDetailResponseDto;
import com.banchango.admin.dto.WarehouseAdminUpdateRequestDto;
import com.banchango.admin.dto.WarehouseInsertRequestResponseListDto;
import com.banchango.auth.token.JwtTokenUtil;
import com.banchango.common.dto.BasicMessageResponseDto;
import com.banchango.domain.estimates.EstimateStatus;
import com.banchango.domain.estimates.Estimates;
import com.banchango.domain.estimates.EstimatesRepository;
import com.banchango.domain.mainitemtypes.MainItemType;
import com.banchango.domain.users.UserRole;
import com.banchango.domain.users.Users;
import com.banchango.domain.users.UsersRepository;
import com.banchango.domain.warehouseconditions.WarehouseCondition;
import com.banchango.domain.warehouses.*;
import com.banchango.estimateitems.dto.EstimateItemSearchResponseDto;
import com.banchango.estimates.dto.EstimateSearchResponseDto;
import com.banchango.factory.entity.EstimateEntityFactory;
import com.banchango.factory.entity.EstimateItemEntityFactory;
import com.banchango.factory.entity.UserEntityFactory;
import com.banchango.factory.entity.WarehouseEntityFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URI;
import java.util.Arrays;


import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class AdminApiTest extends ApiTestContext {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private WarehousesRepository warehousesRepository;

    @Autowired
    private EstimatesRepository estimatesRepository;

    @Autowired
    private WarehouseEntityFactory warehouseEntityFactory;

    @Autowired
    private UserEntityFactory userEntityFactory;

    @Autowired
    private EstimateEntityFactory estimateEntityFactory;

    Users user = null;
    String accessToken = null;
    Users admin = null;
    String adminAccessToken = null;



    @Test
    public void put_WarehouseInfoIsUpdatedWithoutBlogUrl_ifAllConditionsAreRight() {
        Warehouses warehouse = warehouseEntityFactory.createViewableWithMainItemTypes(accessToken, new MainItemType[]{MainItemType.BOOK, MainItemType.FOOD, MainItemType.CLOTH});
        Integer warehouseId = warehouse.getId();
        String url = String.format("/v3/admin/warehouses/%d", warehouseId);
        WarehouseAdminUpdateRequestDto body = WarehouseAdminUpdateRequestDto.builder()
                .name("NEW NAME")
                .space(999)
                .address("NEW ADDRESS")
                .addressDetail("NEW ADDR_DETAIL")
                .description("NEW DESC")
                .availableWeekdays(101010)
                .openAt("08:00")
                .closeAt("23:30")
                .availableTimeDetail("NEW AVAIL_TIME_DETAIL")
                .cctvExist(false)
                .doorLockExist(false)
                .airConditioningType(AirConditioningType.NONE)
                .workerExist(false)
                .canPark(false)
                .mainItemTypes(Arrays.asList(new MainItemType[]{MainItemType.COSMETIC, MainItemType.COLD_STORAGE, MainItemType.ELECTRONICS}))
                .warehouseType(WarehouseType.FULFILLMENT)
                .warehouseCondition(Arrays.asList(new WarehouseCondition[]{WarehouseCondition.BONDED, WarehouseCondition.HAZARDOUS}))
                .minReleasePerMonth(101)
                .latitude(11.11)
                .longitude(33.33)
                .insurances(Arrays.asList(new String[]{"NEW_INSURANCE_1", "NEW_INSURANCE_2"}))
                .securityCompanies(Arrays.asList(new String[]{"NEW_SEC_COMP_1", "NEW_SEC_COMP_2"}))
                .deliveryTypes(Arrays.asList(new String[]{"NEW_DELIVERY_1", "NEW_DELIVERY_2"}))
                .status(WarehouseStatus.REJECTED)
                .warehouseFacilityUsages(Arrays.asList(new String[]{"WH_FACILITY_USAGE"}))
                .build();
        RequestEntity<WarehouseAdminUpdateRequestDto> putRequest = RequestEntity.put(URI.create(url))
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + adminAccessToken)
                .body(body);

        ResponseEntity<WarehouseAdminDetailResponseDto> firstResponse = restTemplate.exchange(putRequest, WarehouseAdminDetailResponseDto.class);
        assertEquals(HttpStatus.OK, firstResponse.getStatusCode());
        assertEquals("NEW NAME", firstResponse.getBody().getName());
        assertEquals(Integer.valueOf(999), firstResponse.getBody().getSpace());
        assertEquals("NEW ADDRESS", firstResponse.getBody().getAddress());
        assertEquals("NEW ADDR_DETAIL", firstResponse.getBody().getAddressDetail());
        assertEquals("NEW DESC", firstResponse.getBody().getDescription());
        assertEquals(Integer.valueOf(101010), firstResponse.getBody().getAvailableWeekdays());
        assertEquals("08:00", firstResponse.getBody().getOpenAt());
        assertEquals("23:30", firstResponse.getBody().getCloseAt());
        assertEquals("NEW AVAIL_TIME_DETAIL", firstResponse.getBody().getAvailableTimeDetail());
        assertFalse(firstResponse.getBody().getCctvExist());
        assertFalse(firstResponse.getBody().getDoorLockExist());
        assertFalse(firstResponse.getBody().getWorkerExist());
        assertFalse(firstResponse.getBody().getCanPark());
        assertEquals(AirConditioningType.NONE, firstResponse.getBody().getAirConditioningType());
        assertEquals(firstResponse.getBody().getMainItemTypes(), Arrays.asList(new MainItemType[]{MainItemType.COSMETIC, MainItemType.COLD_STORAGE, MainItemType.ELECTRONICS}));
        assertEquals(WarehouseType.FULFILLMENT, firstResponse.getBody().getWarehouseType());
        assertEquals(Integer.valueOf(101), firstResponse.getBody().getMinReleasePerMonth());
        assertEquals(Double.valueOf(11.11), firstResponse.getBody().getLatitude());
        assertEquals(Double.valueOf(33.33), firstResponse.getBody().getLongitude());
        assertNull(firstResponse.getBody().getBlogUrl());
        assertTrue(firstResponse.getBody().getInsurances().containsAll(Arrays.asList(new String[]{"NEW_INSURANCE_1", "NEW_INSURANCE_2"})));
        assertTrue(firstResponse.getBody().getSecurityCompanies().containsAll(Arrays.asList(new String[]{"NEW_SEC_COMP_1", "NEW_SEC_COMP_2"})));
        assertTrue(firstResponse.getBody().getDeliveryTypes().containsAll(Arrays.asList(new String[]{"NEW_DELIVERY_1", "NEW_DELIVERY_2"})));
        assertTrue(firstResponse.getBody().getWarehouseCondition().containsAll(Arrays.asList(new WarehouseCondition[]{WarehouseCondition.BONDED, WarehouseCondition.HAZARDOUS})));
        assertEquals(WarehouseStatus.REJECTED, firstResponse.getBody().getStatus());
        assertTrue(firstResponse.getBody().getWarehouseFacilityUsages().contains("WH_FACILITY_USAGE"));

        RequestEntity<Void> getRequest = RequestEntity.get(URI.create(url)).
                header("Authorization", "Bearer " + adminAccessToken).build();
        ResponseEntity<WarehouseAdminDetailResponseDto> secondResponse = restTemplate.exchange(getRequest, WarehouseAdminDetailResponseDto.class);
        assertEquals(HttpStatus.OK, secondResponse.getStatusCode());
        assertEquals("NEW NAME", secondResponse.getBody().getName());
        assertEquals(Integer.valueOf(999), secondResponse.getBody().getSpace());
        assertEquals("NEW ADDRESS", secondResponse.getBody().getAddress());
        assertEquals("NEW ADDR_DETAIL", secondResponse.getBody().getAddressDetail());
        assertEquals("NEW DESC", secondResponse.getBody().getDescription());
        assertEquals(Integer.valueOf(101010), secondResponse.getBody().getAvailableWeekdays());
        assertEquals("08:00", secondResponse.getBody().getOpenAt());
        assertEquals("23:30", secondResponse.getBody().getCloseAt());
        assertEquals("NEW AVAIL_TIME_DETAIL", secondResponse.getBody().getAvailableTimeDetail());
        assertFalse(secondResponse.getBody().getCctvExist());
        assertFalse(secondResponse.getBody().getDoorLockExist());
        assertFalse(secondResponse.getBody().getWorkerExist());
        assertFalse(secondResponse.getBody().getCanPark());
        assertEquals(AirConditioningType.NONE, secondResponse.getBody().getAirConditioningType());
        assertEquals(secondResponse.getBody().getMainItemTypes(), Arrays.asList(new MainItemType[]{MainItemType.COSMETIC, MainItemType.COLD_STORAGE, MainItemType.ELECTRONICS}));
        assertEquals(WarehouseType.FULFILLMENT, secondResponse.getBody().getWarehouseType());
        assertEquals(Integer.valueOf(101), secondResponse.getBody().getMinReleasePerMonth());
        assertEquals(Double.valueOf(11.11), secondResponse.getBody().getLatitude());
        assertEquals(Double.valueOf(33.33), secondResponse.getBody().getLongitude());
        assertNull(secondResponse.getBody().getBlogUrl());
        assertTrue(secondResponse.getBody().getInsurances().containsAll(Arrays.asList(new String[]{"NEW_INSURANCE_1", "NEW_INSURANCE_2"})));
        assertTrue(secondResponse.getBody().getSecurityCompanies().containsAll(Arrays.asList(new String[]{"NEW_SEC_COMP_1", "NEW_SEC_COMP_2"})));
        assertTrue(secondResponse.getBody().getDeliveryTypes().containsAll(Arrays.asList(new String[]{"NEW_DELIVERY_1", "NEW_DELIVERY_2"})));
        assertTrue(secondResponse.getBody().getWarehouseCondition().containsAll(Arrays.asList(new WarehouseCondition[]{WarehouseCondition.BONDED, WarehouseCondition.HAZARDOUS})));
        assertTrue(secondResponse.getBody().getWarehouseFacilityUsages().contains("WH_FACILITY_USAGE"));
        assertEquals(WarehouseStatus.REJECTED, secondResponse.getBody().getStatus());
    }

    @Test
    public void put_WarehouseInfoIsUpdatedWithBlogUrl_ifAllConditionsAreRight() {
        Warehouses warehouse = warehouseEntityFactory.createViewableWithMainItemTypes(accessToken, new MainItemType[]{MainItemType.BOOK, MainItemType.FOOD, MainItemType.CLOTH});
        Integer warehouseId = warehouse.getId();
        String url = String.format("/v3/admin/warehouses/%d", warehouseId);
        String BLOG_URL = "BLOG_URL";

        WarehouseAdminUpdateRequestDto body = WarehouseAdminUpdateRequestDto.builder()
            .name("NEW NAME")
            .space(999)
            .address("NEW ADDRESS")
            .addressDetail("NEW ADDR_DETAIL")
            .description("NEW DESC")
            .availableWeekdays(101010)
            .openAt("08:00")
            .closeAt("23:30")
            .availableTimeDetail("NEW AVAIL_TIME_DETAIL")
            .cctvExist(false)
            .doorLockExist(false)
            .airConditioningType(AirConditioningType.NONE)
            .workerExist(false)
            .canPark(false)
            .mainItemTypes(Arrays.asList(new MainItemType[]{MainItemType.COSMETIC, MainItemType.COLD_STORAGE, MainItemType.ELECTRONICS}))
            .warehouseType(WarehouseType.FULFILLMENT)
            .warehouseCondition(Arrays.asList(new WarehouseCondition[]{WarehouseCondition.BONDED, WarehouseCondition.HAZARDOUS}))
            .minReleasePerMonth(101)
            .latitude(11.11)
            .longitude(33.33)
            .blogUrl(BLOG_URL)
            .insurances(Arrays.asList(new String[]{"NEW_INSURANCE_1", "NEW_INSURANCE_2"}))
            .securityCompanies(Arrays.asList(new String[]{"NEW_SEC_COMP_1", "NEW_SEC_COMP_2"}))
            .deliveryTypes(Arrays.asList(new String[]{"NEW_DELIVERY_1", "NEW_DELIVERY_2"}))
            .status(WarehouseStatus.REJECTED)
            .warehouseFacilityUsages(Arrays.asList(new String[]{"WH_FACILITY_USAGE"}))
            .build();

        RequestEntity<WarehouseAdminUpdateRequestDto> putRequest = RequestEntity.put(URI.create(url))
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + adminAccessToken)
            .body(body);

        ResponseEntity<WarehouseAdminDetailResponseDto> firstResponse = restTemplate.exchange(putRequest, WarehouseAdminDetailResponseDto.class);
        assertEquals(HttpStatus.OK, firstResponse.getStatusCode());
        assertEquals("NEW NAME", firstResponse.getBody().getName());
        assertEquals(Integer.valueOf(999), firstResponse.getBody().getSpace());
        assertEquals("NEW ADDRESS", firstResponse.getBody().getAddress());
        assertEquals("NEW ADDR_DETAIL", firstResponse.getBody().getAddressDetail());
        assertEquals("NEW DESC", firstResponse.getBody().getDescription());
        assertEquals(Integer.valueOf(101010), firstResponse.getBody().getAvailableWeekdays());
        assertEquals("08:00", firstResponse.getBody().getOpenAt());
        assertEquals("23:30", firstResponse.getBody().getCloseAt());
        assertEquals("NEW AVAIL_TIME_DETAIL", firstResponse.getBody().getAvailableTimeDetail());
        assertFalse(firstResponse.getBody().getCctvExist());
        assertFalse(firstResponse.getBody().getDoorLockExist());
        assertFalse(firstResponse.getBody().getWorkerExist());
        assertFalse(firstResponse.getBody().getCanPark());
        assertEquals(AirConditioningType.NONE, firstResponse.getBody().getAirConditioningType());
        assertEquals(firstResponse.getBody().getMainItemTypes(), Arrays.asList(new MainItemType[]{MainItemType.COSMETIC, MainItemType.COLD_STORAGE, MainItemType.ELECTRONICS}));
        assertEquals(WarehouseType.FULFILLMENT, firstResponse.getBody().getWarehouseType());
        assertEquals(Integer.valueOf(101), firstResponse.getBody().getMinReleasePerMonth());
        assertEquals(Double.valueOf(11.11), firstResponse.getBody().getLatitude());
        assertEquals(Double.valueOf(33.33), firstResponse.getBody().getLongitude());
        assertEquals(BLOG_URL, firstResponse.getBody().getBlogUrl());
        assertTrue(firstResponse.getBody().getInsurances().containsAll(Arrays.asList(new String[]{"NEW_INSURANCE_1", "NEW_INSURANCE_2"})));
        assertTrue(firstResponse.getBody().getSecurityCompanies().containsAll(Arrays.asList(new String[]{"NEW_SEC_COMP_1", "NEW_SEC_COMP_2"})));
        assertTrue(firstResponse.getBody().getDeliveryTypes().containsAll(Arrays.asList(new String[]{"NEW_DELIVERY_1", "NEW_DELIVERY_2"})));
        assertTrue(firstResponse.getBody().getWarehouseCondition().containsAll(Arrays.asList(new WarehouseCondition[]{WarehouseCondition.BONDED, WarehouseCondition.HAZARDOUS})));
        assertEquals(WarehouseStatus.REJECTED, firstResponse.getBody().getStatus());
        assertTrue(firstResponse.getBody().getWarehouseFacilityUsages().contains("WH_FACILITY_USAGE"));

        RequestEntity<Void> getRequest = RequestEntity.get(URI.create(url)).
            header("Authorization", "Bearer " + adminAccessToken).build();
        ResponseEntity<WarehouseAdminDetailResponseDto> secondResponse = restTemplate.exchange(getRequest, WarehouseAdminDetailResponseDto.class);
        assertEquals(HttpStatus.OK, secondResponse.getStatusCode());
        assertEquals("NEW NAME", secondResponse.getBody().getName());
        assertEquals(Integer.valueOf(999), secondResponse.getBody().getSpace());
        assertEquals("NEW ADDRESS", secondResponse.getBody().getAddress());
        assertEquals("NEW ADDR_DETAIL", secondResponse.getBody().getAddressDetail());
        assertEquals("NEW DESC", secondResponse.getBody().getDescription());
        assertEquals(Integer.valueOf(101010), secondResponse.getBody().getAvailableWeekdays());
        assertEquals("08:00", secondResponse.getBody().getOpenAt());
        assertEquals("23:30", secondResponse.getBody().getCloseAt());
        assertEquals("NEW AVAIL_TIME_DETAIL", secondResponse.getBody().getAvailableTimeDetail());
        assertFalse(secondResponse.getBody().getCctvExist());
        assertFalse(secondResponse.getBody().getDoorLockExist());
        assertFalse(secondResponse.getBody().getWorkerExist());
        assertFalse(secondResponse.getBody().getCanPark());
        assertEquals(AirConditioningType.NONE, secondResponse.getBody().getAirConditioningType());
        assertEquals(secondResponse.getBody().getMainItemTypes(), Arrays.asList(new MainItemType[]{MainItemType.COSMETIC, MainItemType.COLD_STORAGE, MainItemType.ELECTRONICS}));
        assertEquals(WarehouseType.FULFILLMENT, secondResponse.getBody().getWarehouseType());
        assertEquals(Integer.valueOf(101), secondResponse.getBody().getMinReleasePerMonth());
        assertEquals(Double.valueOf(11.11), secondResponse.getBody().getLatitude());
        assertEquals(Double.valueOf(33.33), secondResponse.getBody().getLongitude());
        assertEquals(BLOG_URL, secondResponse.getBody().getBlogUrl());
        assertTrue(secondResponse.getBody().getInsurances().containsAll(Arrays.asList(new String[]{"NEW_INSURANCE_1", "NEW_INSURANCE_2"})));
        assertTrue(secondResponse.getBody().getSecurityCompanies().containsAll(Arrays.asList(new String[]{"NEW_SEC_COMP_1", "NEW_SEC_COMP_2"})));
        assertTrue(secondResponse.getBody().getDeliveryTypes().containsAll(Arrays.asList(new String[]{"NEW_DELIVERY_1", "NEW_DELIVERY_2"})));
        assertTrue(secondResponse.getBody().getWarehouseCondition().containsAll(Arrays.asList(new WarehouseCondition[]{WarehouseCondition.BONDED, WarehouseCondition.HAZARDOUS})));
        assertTrue(secondResponse.getBody().getWarehouseFacilityUsages().contains("WH_FACILITY_USAGE"));
        assertEquals(WarehouseStatus.REJECTED, secondResponse.getBody().getStatus());
    }
}
