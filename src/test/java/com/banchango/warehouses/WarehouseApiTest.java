package com.banchango.warehouses;

import com.banchango.ApiTestContext;
import com.banchango.auth.token.JwtTokenUtil;
import com.banchango.common.dto.BasicMessageResponseDto;
import com.banchango.domain.mainitemtypes.MainItemType;
import com.banchango.domain.mainitemtypes.MainItemTypes;
import com.banchango.domain.users.UserRole;
import com.banchango.domain.users.UserType;
import com.banchango.domain.users.Users;
import com.banchango.domain.users.UsersRepository;
import com.banchango.domain.warehouseconditions.WarehouseCondition;
import com.banchango.domain.warehouses.*;
import com.banchango.factory.entity.UserEntityFactory;
import com.banchango.factory.entity.WarehouseEntityFactory;
import com.banchango.factory.request.WarehouseInsertRequestFactory;
import com.banchango.users.exception.UserEmailNotFoundException;
import com.banchango.warehouses.dto.*;
import com.sun.mail.iap.Response;
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
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class WarehouseApiTest extends ApiTestContext {
    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private UserEntityFactory userEntityFactory;

    @Autowired
    private WarehousesRepository warehouseRepository;

    @Autowired
    private WarehouseEntityFactory warehouseEntityFactory;

    // POST API에 이메일 전송 기능이 추가되어, 아래 테스트 코드는 일단 주석 처리 하겠습니다.
//    @Test
//    public void post_warehouse_responseIsOk_IfAllConditionsAreRight() {
//        List<MainItemType> mainItemTypes = new ArrayList<>();
//        mainItemTypes.add(MainItemType.CLOTH);
//        mainItemTypes.add(MainItemType.BOOK);
//
//        List<WarehouseCondition> warehouseConditions = new ArrayList<>();
//        warehouseConditions.add(WarehouseCondition.LOW_TEMPERATURE);
//        warehouseConditions.add(WarehouseCondition.ROOM_TEMPERATURE);
//
//        List<String> strings = new ArrayList<>();
//        strings.add("one");
//        strings.add("two");
//        strings.add("three");
//
//        WarehouseInsertRequestDto requestBody = WarehouseInsertRequestDto.builder()
//            .name("TEST_NAME")
//            .space(123)
//            .address("address")
//            .addressDetail("addressDetail")
//            .description("description")
//            .availableWeekdays(1)
//            .openAt("06:00")
//            .closeAt("18:00")
//            .availableTimeDetail("availableTimeDetail")
//            .insurance("insurance")
//            .cctvExist(true)
//            .securityCompanyName("name")
//            .doorLockExist(true)
//            .airConditioningType(AirConditioningType.BOTH)
//            .workerExist(true)
//            .canPark(true)
//            .mainItemTypes(mainItemTypes)
//            .warehouseType(WarehouseType.THREEPL)
//            .minReleasePerMonth(23)
//            .latitude(22.2)
//            .longitude(22.2)
//            .deliveryTypes(strings)
//            .warehouseCondition(warehouseConditions)
//            .warehouseCondition(warehouseConditions)
//            .warehouseFacilityUsages(strings)
//            .warehouseUsageCautions(strings)
//            .build();
//
//
//        RequestEntity<WarehouseInsertRequestDto> request = RequestEntity.post(URI.create("/v3/warehouses"))
//            .contentType(MediaType.APPLICATION_JSON)
//            .header("Authorization", "Bearer " + accessToken)
//            .body(requestBody);
//
//        ResponseEntity<String> response = restTemplate.exchange(request, String.class);
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertTrue(response.getBody().contains("message"));
//    }

    @Test
    public void post_warehouse_responseIsForbidden_IfUserIsShipper() {
        Users shipper = userEntityFactory.createUserWithShipperType();
        String accessToken = JwtTokenUtil.generateAccessToken(shipper.getUserId(), UserRole.USER);
        WarehouseInsertRequestDto warehouseInsertRequestDto = WarehouseInsertRequestFactory.create();

        RequestEntity<WarehouseInsertRequestDto> request = RequestEntity.post("/v3/warehouses")
                .header("Authorization", "Bearer "+accessToken)
                .body(warehouseInsertRequestDto);

        ResponseEntity<BasicMessageResponseDto> response = restTemplate.exchange(request, BasicMessageResponseDto.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    public void delete_warehouse_responseIsOk_IfWarehouseStatusIsViewable() {
        Users owner = userEntityFactory.createUserWithOwnerType();
        String accessToken = JwtTokenUtil.generateAccessToken(owner.getUserId(), UserRole.USER, UserType.OWNER);
        Warehouses warehouse = warehouseEntityFactory.createViewableWithNoMainItemTypes(accessToken);
        String url = "/v3/warehouses/"+warehouse.getId();

        RequestEntity<Void> request = RequestEntity.delete(URI.create(url))
                .header("Authorization", "Bearer "+accessToken)
                .build();

        ResponseEntity<BasicMessageResponseDto> response = restTemplate.exchange(request, BasicMessageResponseDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getMessage());

        warehouseRepository.delete(warehouse);
    }

    @Test
    public void delete_warehouse_responseIsOk_IfWarehouseStatusIsInProgress() {
        Users owner = userEntityFactory.createUserWithOwnerType();
        String accessToken = JwtTokenUtil.generateAccessToken(owner.getUserId(), UserRole.USER, UserType.OWNER);
        Warehouses warehouse = warehouseEntityFactory.createInProgressWithNoMainItemTypes(accessToken);
        String url = "/v3/warehouses/"+warehouse.getId();

        RequestEntity<Void> request = RequestEntity.delete(URI.create(url))
                .header("Authorization", "Bearer "+accessToken)
                .build();

        ResponseEntity<BasicMessageResponseDto> response = restTemplate.exchange(request, BasicMessageResponseDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getMessage());

        warehouseRepository.delete(warehouse);
    }

    @Test
    public void delete_warehouse_responseIsOk_IfWarehouseStatusIsRejected() {
        Users owner = userEntityFactory.createUserWithOwnerType();
        String accessToken = JwtTokenUtil.generateAccessToken(owner.getUserId(), UserRole.USER, UserType.OWNER);
        Warehouses warehouse = warehouseEntityFactory.createdRejectedWithNoMainItemTypes(accessToken);
        String url = "/v3/warehouses/"+warehouse.getId();

        RequestEntity<Void> request = RequestEntity.delete(URI.create(url))
                .header("Authorization", "Bearer "+accessToken)
                .build();

        ResponseEntity<BasicMessageResponseDto> response = restTemplate.exchange(request, BasicMessageResponseDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getMessage());

        warehouseRepository.delete(warehouse);
    }

    @Test
    public void delete_warehouse_responseIsUnAuthorized_IfAuthorizationIsEmpty() {
        RequestEntity<Void> request = RequestEntity.delete(URI.create("/v3/warehouses/99999"))
                .build();

        ResponseEntity<BasicMessageResponseDto> response = restTemplate.exchange(request, BasicMessageResponseDto.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody().getMessage());
    }


    @Test
    public void delete_warehouse_responseIsForbidden_IfNotMine() {
        Users actualOwner = userEntityFactory.createUserWithOwnerType();
        Users owner = userEntityFactory.createUserWithOwnerType();
        String acutalOwnerAccessToken = JwtTokenUtil.generateAccessToken(actualOwner.getUserId(), UserRole.ADMIN, UserType.OWNER);
        String accessToken = JwtTokenUtil.generateAccessToken(owner.getUserId(), UserRole.ADMIN, UserType.OWNER);
        Warehouses warehouse = warehouseEntityFactory.createDeletedWithNoMainItemTypes(acutalOwnerAccessToken);

        RequestEntity<Void> request = RequestEntity.delete(URI.create("/v3/warehouses/"+warehouse.getId()))
                .header("Authorization", "Bearer "+accessToken)
                .build();

        ResponseEntity<BasicMessageResponseDto> response = restTemplate.exchange(request, BasicMessageResponseDto.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void delete_warehouse_responseIsNotFound_IfWarehouseStatusIsDeleted() {
        Users owner = userEntityFactory.createUserWithOwnerType();
        String accessToken = JwtTokenUtil.generateAccessToken(owner.getUserId(), UserRole.ADMIN, UserType.OWNER)
        Warehouses warehouse = warehouseEntityFactory.createDeletedWithNoMainItemTypes(accessToken);

        RequestEntity<Void> request = RequestEntity.delete(URI.create("/v3/warehouses/"+warehouse.getId()))
                .header("Authorization", "Bearer "+accessToken)
                .build();

        ResponseEntity<BasicMessageResponseDto> response = restTemplate.exchange(request, BasicMessageResponseDto.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void delete_warehouse_responseIsNotFound_IfWarehouseNotExist() {
        Users owner = userEntityFactory.createUserWithOwnerType();
        String accessToken = JwtTokenUtil.generateAccessToken(owner.getUserId(), UserRole.ADMIN, UserType.OWNER)
        RequestEntity<Void> request = RequestEntity.delete(URI.create("/v3/warehouses/0"))
                .header("Authorization", "Bearer "+accessToken)
                .build();

        ResponseEntity<BasicMessageResponseDto> response = restTemplate.exchange(request, BasicMessageResponseDto.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
    
    @Test
    public void get_warehouseByAddress_responseIsOk_IfAllConditionsAreRight() {
        Warehouses tempWarehouse = saveWarehouse(WarehouseStatus.VIEWABLE, new MainItemType[] { MainItemType.CLOTH });

        String addressQuery = "addr";
        String url = String.format("/v3/warehouses?address=%s&page=0&size=4", addressQuery);
        RequestEntity<Void> request = RequestEntity.get(URI.create(url))
                .build();

        ResponseEntity<WarehouseSearchResponseDto> response = restTemplate.exchange(request, WarehouseSearchResponseDto.class);

        List<WarehouseSearchDto> warehouses = response.getBody().getWarehouses();
        assertTrue(warehouses.size() > 0);

        WarehouseSearchDto warehouse = warehouses.get(0);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertNotNull(warehouse.getAddress());
        assertNotNull(warehouse.getWarehouseId());
        assertNotNull(warehouse.getWarehouseCondition());
        assertNotNull(warehouse.getMinReleasePerMonth());
        assertNotNull(warehouse.getName());
        assertNotNull(warehouse.getWarehouseType());
        assertNotNull(warehouse.getCloseAt());
        assertNotNull(warehouse.getMainImageUrl());
        assertNotNull(warehouse.getOpenAt());
        assertNotNull(warehouse.getSpace());
        assertNotNull(warehouse.getDeliveryTypes());
        assertNotNull(warehouse.getMainItemTypes());

        for(WarehouseSearchDto _warehouse : warehouses) {
            String address = _warehouse.getAddress().toLowerCase();
            assertTrue(address.contains(addressQuery.toLowerCase()));
        }

        warehouseRepository.delete(tempWarehouse);
    }

    @Test
    public void get_warehouseForMain_responseIsOk_IfAllConditionsAreRight() {
        Warehouses tempWarehouse = saveWarehouse(WarehouseStatus.VIEWABLE, new MainItemType[] { MainItemType.CLOTH });
        RequestEntity<Void> request = RequestEntity.get(URI.create("/v3/warehouses?page=0&size=4"))
                .build();

        ResponseEntity<WarehouseSearchResponseDto> response = restTemplate.exchange(request, WarehouseSearchResponseDto.class);

        List<WarehouseSearchDto> warehouses = response.getBody().getWarehouses();
        assertTrue(warehouses.size() > 0);

        WarehouseSearchDto warehouse = warehouses.get(0);
        assertEquals(HttpStatus.OK, response.getStatusCode());


        assertNotNull(warehouse.getAddress());
        assertNotNull(warehouse.getWarehouseId());
        assertNotNull(warehouse.getWarehouseCondition());
        assertNotNull(warehouse.getMinReleasePerMonth());
        assertNotNull(warehouse.getName());
        assertNotNull(warehouse.getWarehouseType());
        assertNotNull(warehouse.getCloseAt());
        assertNotNull(warehouse.getMainImageUrl());
        assertNotNull(warehouse.getOpenAt());
        assertNotNull(warehouse.getSpace());
        assertNotNull(warehouse.getDeliveryTypes());
        assertNotNull(warehouse.getMainItemTypes());

        warehouseRepository.delete(tempWarehouse);
    }

    @Test
    public void get_warehouseForMain_responseIsNotFound_IfIsViewableIsFalse() {
        Warehouses tempWarehouse = saveWarehouse(WarehouseStatus.IN_PROGRESS, new MainItemType[] { MainItemType.CLOTH });
        RequestEntity<Void> request = RequestEntity.get(URI.create("/v3/warehouses?page=0&size=4"))
                .build();

        ResponseEntity<WarehouseSearchResponseDto> response = restTemplate.exchange(request, WarehouseSearchResponseDto.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        warehouseRepository.delete(tempWarehouse);
    }

    @Test
    public void get_warehouseForMain_responseIsNotFound_IfWarehouseNotExist() {
        warehouseRepository.deleteAll();
        RequestEntity<Void> request = RequestEntity.get(URI.create("/v3/warehouses?page=0&size=4"))
                .build();

        ResponseEntity<WarehouseSearchResponseDto> response = restTemplate.exchange(request, WarehouseSearchResponseDto.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void get_warehouseByMainItemType_responseIsOk_IfAllConditionsAreRight() {
        Warehouses warehouse1 = saveWarehouse(WarehouseStatus.VIEWABLE, new MainItemType[] { MainItemType.CLOTH, MainItemType.COSMETIC });
        Warehouses warehouse2 = saveWarehouse(WarehouseStatus.VIEWABLE, new MainItemType[] { MainItemType.CLOTH, MainItemType.ACCESSORY });
        Warehouses warehouse3 = saveWarehouse(WarehouseStatus.VIEWABLE, new MainItemType[] { MainItemType.CLOTH, MainItemType.BOOK });

        String url = "/v3/warehouses?page=0&size=5&mainItemTypes=CLOTH,COSMETIC";

        RequestEntity<Void> request = RequestEntity.get(URI.create(url))
                .build();

        ResponseEntity<WarehouseSearchResponseDto> response = restTemplate.exchange(request, WarehouseSearchResponseDto.class);

        List<WarehouseSearchDto> warehouses = response.getBody().getWarehouses();
        assertTrue(warehouses.size() > 0);

        WarehouseSearchDto warehouseSearchDto = warehouses.get(0);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertNotNull(warehouseSearchDto.getAddress());
        assertNotNull(warehouseSearchDto.getWarehouseId());
        assertNotNull(warehouseSearchDto.getWarehouseCondition());
        assertNotNull(warehouseSearchDto.getMinReleasePerMonth());
        assertNotNull(warehouseSearchDto.getName());
        assertNotNull(warehouseSearchDto.getWarehouseType());
        assertNotNull(warehouseSearchDto.getCloseAt());
        assertNotNull(warehouseSearchDto.getMainImageUrl());
        assertNotNull(warehouseSearchDto.getOpenAt());
        assertNotNull(warehouseSearchDto.getSpace());
        assertNotNull(warehouseSearchDto.getDeliveryTypes());
        assertNotNull(warehouseSearchDto.getMainItemTypes());

        for (WarehouseSearchDto _warehouse : warehouses) {
            _warehouse.getMainItemTypes().stream()
                .filter(mainItemTypeMatchDto -> mainItemTypeMatchDto.getName() == MainItemType.CLOTH || mainItemTypeMatchDto.getName() == MainItemType.COSMETIC)
                .forEach(mainItemTypeMatchDto -> assertTrue(mainItemTypeMatchDto.getMatch()));

            _warehouse.getMainItemTypes().stream()
                .filter(mainItemTypeMatchDto -> mainItemTypeMatchDto.getName() != MainItemType.CLOTH && mainItemTypeMatchDto.getName() != MainItemType.COSMETIC)
                .forEach(mainItemTypeMatchDto -> assertFalse(mainItemTypeMatchDto.getMatch()));
        }

        warehouseRepository.delete(warehouse1);
        warehouseRepository.delete(warehouse2);
        warehouseRepository.delete(warehouse3);
    }

    @Test
    public void get_warehouseByMainItemType_responseIsNotFound_IfIsViewableIsFalse() {
        Warehouses warehouse = saveWarehouse(WarehouseStatus.IN_PROGRESS, new MainItemType[] { MainItemType.CLOTH });

        String mainItemType = MainItemType.CLOTH.toString();
        String url = String.format("/v3/warehouses?mainItemTypes=%s&page=0&size=5", mainItemType);

        RequestEntity<Void> request = RequestEntity.get(URI.create(url))
                .build();

        ResponseEntity<WarehouseSearchResponseDto> response = restTemplate.exchange(request, WarehouseSearchResponseDto.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        warehouseRepository.delete(warehouse);
    }

    @Test
    public void get_warehouseByMainItemType_responseIsNotFound_IfWarehouseNotExist() {
        warehouseRepository.deleteAll();

        String mainItemType = MainItemType.CLOTH.toString();
        String url = String.format("/v3/warehouses?mainItemTypes=%s&page=0&size=5", mainItemType);

        RequestEntity<Void> request = RequestEntity.get(URI.create(url))
                .build();

        ResponseEntity<WarehouseSearchResponseDto> response = restTemplate.exchange(request, WarehouseSearchResponseDto.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void get_warehouse_responseIsBadRequest_IfAddressAndMainItemTypeBothGiven() {
        warehouseRepository.deleteAll();

        String mainItemType = MainItemType.CLOTH.toString();
        String addressQuery = "addr";
        String url = String.format("/v3/warehouses?mainItemTypes=%s&address=%s&page=0&offset=5", mainItemType, addressQuery);

        RequestEntity<Void> request = RequestEntity.get(URI.create(url))
            .build();

        ResponseEntity<WarehouseSearchResponseDto> response = restTemplate.exchange(request, WarehouseSearchResponseDto.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void get_warehouseDetail_responseIsOk_IfAllConditionsAreRight() {
        Warehouses _warehouse = saveWarehouse(WarehouseStatus.VIEWABLE, new MainItemType[] { MainItemType.CLOTH });
        String url = String.format("/v3/warehouses/%d", _warehouse.getId());

        RequestEntity<Void> request = RequestEntity.get(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .build();

        ResponseEntity<WarehouseDetailResponseDto> response = restTemplate.exchange(request, WarehouseDetailResponseDto.class);

        WarehouseDetailResponseDto warehouse = response.getBody();
        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertNotNull(warehouse.getWarehouseId());
        assertNotNull(warehouse.getOwnerId());
        assertNotNull(warehouse.getName());
        assertNotNull(warehouse.getSpace());
        assertNotNull(warehouse.getAddress());
        assertNotNull(warehouse.getAddressDetail());
        assertNotNull(warehouse.getDescription());
        assertNotNull(warehouse.getAvailableWeekdays());
        assertNotNull(warehouse.getOpenAt());
        assertNotNull(warehouse.getCloseAt());
        assertNotNull(warehouse.getAvailableTimeDetail());
        assertNotNull(warehouse.getCctvExist());
        assertNotNull(warehouse.getDoorLockExist());
        assertNotNull(warehouse.getAirConditioningType());
        assertNotNull(warehouse.getWorkerExist());
        assertNotNull(warehouse.getCanPark());
        assertNotNull(warehouse.getMainItemTypes());
        assertNotNull(warehouse.getWarehouseType());
        assertNotNull(warehouse.getMinReleasePerMonth());
        assertNotNull(warehouse.getLatitude());
        assertNotNull(warehouse.getLongitude());
        assertNull(warehouse.getBlogUrl());
        assertNotNull(warehouse.getMainImageUrl());
        assertNotNull(warehouse.getDeliveryTypes());
        assertNotNull(warehouse.getWarehouseCondition());
        assertNotNull(warehouse.getWarehouseFacilityUsages());
        assertNotNull(warehouse.getWarehouseUsageCautions());
        assertNotNull(warehouse.getImages());

        warehouseRepository.delete(_warehouse);
    }

    @Test
    public void get_warehouseDetail_responseIsNotFound_IfIsViewableIsFalse() {
        Warehouses _warehouse = saveWarehouse(WarehouseStatus.IN_PROGRESS, new MainItemType[]{MainItemType.CLOTH});
        String url = String.format("/v3/warehouses/%d", _warehouse.getId());

        RequestEntity<Void> request = RequestEntity.get(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .build();

        ResponseEntity<WarehouseDetailResponseDto> response = restTemplate.exchange(request, WarehouseDetailResponseDto.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        warehouseRepository.delete(_warehouse);
    }

    @Test
    public void get_warehouseDetail_responseIsNotFound_IfWarehouseNotExist() {
        String url = String.format("/v3/warehouses/%d", 0);

        RequestEntity<Void> request = RequestEntity.get(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .build();

        ResponseEntity<WarehouseDetailResponseDto> response = restTemplate.exchange(request, WarehouseDetailResponseDto.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    private Warehouses saveWarehouse(WarehouseStatus status, MainItemType[] mainItemTypes) {
        int userId = JwtTokenUtil.extractUserId(accessToken);

        Warehouses warehouse = Warehouses.builder()
                .userId(userId)
                .name("NAME")
                .space(123)
                .address("address")
                .addressDetail("addressDetail")
                .description("description")
                .availableWeekdays(1)
                .openAt("06:00")
                .closeAt("18:00")
                .availableTimeDetail("availableTimeDetail")
                .cctvExist(true)
                .doorLockExist(true)
                .airConditioningType(AirConditioningType.HEATING)
                .workerExist(true)
                .canPark(true)
                .warehouseType(WarehouseType.THREEPL)
                .minReleasePerMonth(2)
                .latitude(22.2)
                .longitude(22.2)
                .status(status)
                .build();

        List<MainItemTypes> m = Arrays.stream(mainItemTypes)
            .map(mainItemType -> new MainItemTypes(mainItemType, warehouse))
            .collect(Collectors.toList());

        warehouse.getMainItemTypes().addAll(m);

        return warehouseRepository.save(warehouse);
    }

    @Test
    public void put_WarehouseInfoIsUpdated_ifAllConditionsAreRight() {
        Warehouses warehouse = warehouseEntityFactory.createViewableWithMainItemTypes(accessToken, new MainItemType[]{MainItemType.BOOK, MainItemType.FOOD, MainItemType.CLOTH});
        Integer warehouseId = warehouse.getId();
        String url = String.format("/v3/warehouses/%d", warehouseId);
        WarehouseUpdateRequestDto body = WarehouseUpdateRequestDto.builder()
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
            .warehouseFacilityUsages(Arrays.asList(new String[]{"WH_FACILITY_USAGE"}))
            .build();

        RequestEntity<WarehouseUpdateRequestDto> putRequest = RequestEntity.put(URI.create(url))
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + accessToken)
            .body(body);

        ResponseEntity<WarehouseDetailResponseDto> firstResponse = restTemplate.exchange(putRequest, WarehouseDetailResponseDto.class);
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
        assertTrue(firstResponse.getBody().getWarehouseFacilityUsages().contains("WH_FACILITY_USAGE"));

        RequestEntity<Void> getRequest = RequestEntity.get(URI.create(url)).
            header("Authorization", "Bearer " + accessToken).build();
        ResponseEntity<WarehouseDetailResponseDto> secondResponse = restTemplate.exchange(getRequest, WarehouseDetailResponseDto.class);
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
    }

    @Test
    public void put_WarehouseInfoIsUpdated_responseIsNotFound_ifWarehouseNotExist() {
        Integer warehouseId = 0;
        String url = String.format("/v3/warehouses/%d", warehouseId);
        WarehouseUpdateRequestDto body = WarehouseUpdateRequestDto.builder()
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
            .warehouseFacilityUsages(Arrays.asList(new String[]{"WH_FACILITY_USAGE"}))
            .build();

        RequestEntity<WarehouseUpdateRequestDto> putRequest = RequestEntity.put(URI.create(url))
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + accessToken)
            .body(body);

        ResponseEntity<WarehouseDetailResponseDto> response = restTemplate.exchange(putRequest, WarehouseDetailResponseDto.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void put_WarehouseInfoIsUpdated_responseIsNotFound_ifWarehouseStatusIsDeleted() {
        Warehouses warehouse = warehouseEntityFactory.createDeletedWithMainItemTypes(accessToken, new MainItemType[]{MainItemType.BOOK, MainItemType.FOOD, MainItemType.CLOTH});
        Integer warehouseId = warehouse.getId();

        String url = String.format("/v3/warehouses/%d", warehouseId);
        WarehouseUpdateRequestDto body = WarehouseUpdateRequestDto.builder()
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
            .warehouseFacilityUsages(Arrays.asList(new String[]{"WH_FACILITY_USAGE"}))
            .build();

        RequestEntity<WarehouseUpdateRequestDto> putRequest = RequestEntity.put(URI.create(url))
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + accessToken)
            .body(body);

        ResponseEntity<WarehouseDetailResponseDto> response = restTemplate.exchange(putRequest, WarehouseDetailResponseDto.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void put_WarehouseInfoIsUpdated_responseIsUnAuthorized_ifTokenNotGiven() {
        Warehouses warehouse = warehouseEntityFactory.createViewableWithMainItemTypes(accessToken, new MainItemType[]{MainItemType.BOOK, MainItemType.FOOD, MainItemType.CLOTH});
        Integer warehouseId = warehouse.getId();
        String url = String.format("/v3/warehouses/%d", warehouseId);
        WarehouseUpdateRequestDto body = WarehouseUpdateRequestDto.builder()
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
            .warehouseFacilityUsages(Arrays.asList(new String[]{"WH_FACILITY_USAGE"}))
            .build();

        RequestEntity<WarehouseUpdateRequestDto> putRequest = RequestEntity.put(URI.create(url))
            .contentType(MediaType.APPLICATION_JSON)
            .body(body);

        ResponseEntity<WarehouseDetailResponseDto> response = restTemplate.exchange(putRequest, WarehouseDetailResponseDto.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    public void put_WarehouseInfoIsUpdated_responseIsForbidden_ifOtherUsersTokenGiven() {
        Warehouses warehouse = warehouseEntityFactory.createViewableWithMainItemTypes(accessToken, new MainItemType[]{MainItemType.BOOK, MainItemType.FOOD, MainItemType.CLOTH});
        Integer warehouseId = warehouse.getId();
        String otherUserAccessToken = JwtTokenUtil.generateAccessToken(0, UserRole.USER);

        String url = String.format("/v3/warehouses/%d", warehouseId);
        WarehouseUpdateRequestDto body = WarehouseUpdateRequestDto.builder()
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
            .warehouseFacilityUsages(Arrays.asList(new String[]{"WH_FACILITY_USAGE"}))
            .build();

        RequestEntity<WarehouseUpdateRequestDto> putRequest = RequestEntity.put(URI.create(url))
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer" + otherUserAccessToken)
            .body(body);

        ResponseEntity<WarehouseDetailResponseDto> response = restTemplate.exchange(putRequest, WarehouseDetailResponseDto.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    public void put_WarehouseInfoIsUpdated_responseIsForbidden_ifWarehouseStatusIsInProgress() {
        Warehouses warehouse = warehouseEntityFactory.createInProgressWithMainItemTypes(accessToken, new MainItemType[]{MainItemType.BOOK, MainItemType.FOOD, MainItemType.CLOTH});
        Integer warehouseId = warehouse.getId();

        String url = String.format("/v3/warehouses/%d", warehouseId);
        WarehouseUpdateRequestDto body = WarehouseUpdateRequestDto.builder()
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
            .warehouseFacilityUsages(Arrays.asList(new String[]{"WH_FACILITY_USAGE"}))
            .build();

        RequestEntity<WarehouseUpdateRequestDto> putRequest = RequestEntity.put(URI.create(url))
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + accessToken)
            .body(body);

        ResponseEntity<WarehouseDetailResponseDto> response = restTemplate.exchange(putRequest, WarehouseDetailResponseDto.class);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    public void put_WarehouseInfoIsUpdated_responseIsForbidden_ifWarehouseStatusIsRejected() {
        Warehouses warehouse = warehouseEntityFactory.createRejectedWithMainItemTypes(accessToken, new MainItemType[]{MainItemType.BOOK, MainItemType.FOOD, MainItemType.CLOTH});
        Integer warehouseId = warehouse.getId();

        String url = String.format("/v3/warehouses/%d", warehouseId);
        WarehouseUpdateRequestDto body = WarehouseUpdateRequestDto.builder()
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
            .warehouseFacilityUsages(Arrays.asList(new String[]{"WH_FACILITY_USAGE"}))
            .build();

        RequestEntity<WarehouseUpdateRequestDto> putRequest = RequestEntity.put(URI.create(url))
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + accessToken)
            .body(body);

        ResponseEntity<WarehouseDetailResponseDto> response = restTemplate.exchange(putRequest, WarehouseDetailResponseDto.class);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    public void get_myWarehouses_responseIsOk_IfWarehouseStatusViewable() {
        Warehouses warehouse = warehouseEntityFactory.createViewableWithNoMainItemTypes(accessToken);

        RequestEntity<Void> request = RequestEntity.get(URI.create("/v3/users/"+user.getUserId()+"/warehouses"))
            .header("Authorization", "Bearer "+accessToken)
            .build();

        ResponseEntity<MyWarehousesResponseDto> response = restTemplate.exchange(request, MyWarehousesResponseDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getWarehouses());

        response.getBody().getWarehouses().stream()
            .forEach(myWarehouse -> {
                assertNotNull(myWarehouse.getId());
                assertEquals(WarehouseEntityFactory.NAME, myWarehouse.getName());
                assertEquals(WarehouseEntityFactory.ADDRESS, myWarehouse.getAddress());
                assertEquals(WarehouseEntityFactory.ADDRESS_DETAIL, myWarehouse.getAddressDetail());
                assertEquals(WarehouseStatus.VIEWABLE, myWarehouse.getStatus());
                assertNotNull(myWarehouse.getMainImageUrl());
            });

        warehouseRepository.delete(warehouse);
    }


    @Test
    public void get_myWarehouses_responseIsOk_IfWarehouseStatusInProgress() {
        Warehouses warehouse = warehouseEntityFactory.createInProgressWithNoMainItemTypes(accessToken);

        RequestEntity<Void> request = RequestEntity.get(URI.create("/v3/users/"+user.getUserId()+"/warehouses"))
            .header("Authorization", "Bearer "+accessToken)
            .build();

        ResponseEntity<MyWarehousesResponseDto> response = restTemplate.exchange(request, MyWarehousesResponseDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getWarehouses());

        response.getBody().getWarehouses().stream()
            .forEach(myWarehouse -> {
                assertNotNull(myWarehouse.getId());
                assertEquals(WarehouseEntityFactory.NAME, myWarehouse.getName());
                assertEquals(WarehouseEntityFactory.ADDRESS, myWarehouse.getAddress());
                assertEquals(WarehouseEntityFactory.ADDRESS_DETAIL, myWarehouse.getAddressDetail());
                assertEquals(WarehouseStatus.IN_PROGRESS, myWarehouse.getStatus());
                assertNotNull(myWarehouse.getMainImageUrl());
            });

        warehouseRepository.delete(warehouse);
    }

    @Test
    public void get_myWarehouses_responseIsOk_IfWarehouseStatusRejected() {
        Warehouses warehouse = warehouseEntityFactory.createdRejectedWithNoMainItemTypes(accessToken);

        RequestEntity<Void> request = RequestEntity.get(URI.create("/v3/users/"+user.getUserId()+"/warehouses"))
            .header("Authorization", "Bearer "+accessToken)
            .build();

        ResponseEntity<MyWarehousesResponseDto> response = restTemplate.exchange(request, MyWarehousesResponseDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getWarehouses());

        response.getBody().getWarehouses().stream()
            .forEach(myWarehouse -> {
                assertNotNull(myWarehouse.getId());
                assertEquals(WarehouseEntityFactory.NAME, myWarehouse.getName());
                assertEquals(WarehouseEntityFactory.ADDRESS, myWarehouse.getAddress());
                assertEquals(WarehouseEntityFactory.ADDRESS_DETAIL, myWarehouse.getAddressDetail());
                assertEquals(WarehouseStatus.REJECTED, myWarehouse.getStatus());
                assertNotNull(myWarehouse.getMainImageUrl());
            });

        warehouseRepository.delete(warehouse);
    }

    @Test
    public void get_myWarehouses_responseIsUnauthorized_IfAccessTokenNotGiven() {
        Warehouses warehouse = warehouseEntityFactory.createViewableWithNoMainItemTypes(accessToken);

        RequestEntity<Void> request = RequestEntity.get(URI.create("/v3/users/"+user.getUserId()+"/warehouses"))
            .build();

        ResponseEntity<MyWarehousesResponseDto> response = restTemplate.exchange(request, MyWarehousesResponseDto.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        warehouseRepository.delete(warehouse);
    }

    @Test
    public void get_myWarehouses_responseIsForbidden_IfGivenOtherUserId() {
        Warehouses warehouse = warehouseEntityFactory.createViewableWithNoMainItemTypes(accessToken);
        Users otherUser = userEntityFactory.createUser();

        RequestEntity<Void> request = RequestEntity.get(URI.create("/v3/users/"+otherUser.getUserId()+"/warehouses"))
            .header("Authorization", "Bearer "+accessToken)
            .build();

        ResponseEntity<MyWarehousesResponseDto> response = restTemplate.exchange(request, MyWarehousesResponseDto.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

        warehouseRepository.delete(warehouse);
    }

    @Test
    public void get_myWarehouses_responseIsNotFound_IfWarehouseNotExist() {
        warehouseRepository.deleteAll();

        RequestEntity<Void> request = RequestEntity.get(URI.create("/v3/users/"+user.getUserId()+"/warehouses"))
            .header("Authorization", "Bearer "+accessToken)
            .build();

        ResponseEntity<MyWarehousesResponseDto> response = restTemplate.exchange(request, MyWarehousesResponseDto.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void get_myWarehouses_responseIsNotFound_IfWarehouseStatusDeleted() {
        Warehouses warehouse = warehouseEntityFactory.createDeletedWithNoMainItemTypes(accessToken);

        RequestEntity<Void> request = RequestEntity.get(URI.create("/v3/users/"+user.getUserId()+"/warehouses"))
            .header("Authorization", "Bearer "+accessToken)
            .build();

        ResponseEntity<MyWarehousesResponseDto> response = restTemplate.exchange(request, MyWarehousesResponseDto.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        warehouseRepository.delete(warehouse);
    }

    @Test
    public void get_myWarehouses_responseIsNotFound_IfUserNotExist() {
        int invalidUserId = 0;
        String invalidUserAccessToken = JwtTokenUtil.generateAccessToken(invalidUserId, UserRole.USER);

        RequestEntity<Void> request = RequestEntity.get(URI.create("/v3/users/"+invalidUserId+"/warehouses"))
            .header("Authorization", "Bearer "+invalidUserAccessToken)
            .build();

        ResponseEntity<MyWarehousesResponseDto> response = restTemplate.exchange(request, MyWarehousesResponseDto.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}