package com.banchango.estimates;

import com.banchango.ApiTestContext;
import com.banchango.auth.token.JwtTokenUtil;
import com.banchango.common.dto.BasicMessageResponseDto;
import com.banchango.domain.estimates.EstimateStatus;
import com.banchango.domain.estimates.Estimates;
import com.banchango.domain.estimates.EstimatesRepository;
import com.banchango.domain.users.UserRole;
import com.banchango.domain.users.Users;
import com.banchango.domain.users.UsersRepository;
import com.banchango.domain.warehouses.Warehouses;
import com.banchango.domain.warehouses.WarehousesRepository;
import com.banchango.estimates.dto.EstimateInsertRequestDto;
import com.banchango.estimates.dto.EstimateSearchResponseDto;
import com.banchango.factory.entity.EstimateEntityFactory;
import com.banchango.factory.entity.UserEntityFactory;
import com.banchango.factory.entity.WarehouseEntityFactory;
import com.banchango.factory.request.EstimatesInsertRequestFactory;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class EstimateApiTests extends ApiTestContext {
    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private WarehousesRepository warehouseRepository;

    @Autowired
    private EstimatesRepository estimatesRepository;

    @Autowired
    private UserEntityFactory userEntityFactory;

    @Autowired
    private WarehouseEntityFactory warehouseEntityFactory;

    @Autowired
    private EstimateEntityFactory estimateEntityFactory;

    String accessToken = null;
    Users user = null;





    @Test
    public void get_estimateByUserId_responseIsOk_IfAllConditionsAreRight() {
        Warehouses warehouse = warehouseEntityFactory.createViewableWithNoMainItemTypes(accessToken);
        Estimates estimate1 = estimateEntityFactory.createReceptedWithEstimateItems(warehouse.getId(), user.getUserId());
        Estimates estimate2 = estimateEntityFactory.createReceptedWithEstimateItems(warehouse.getId(), user.getUserId());
        Estimates estimate3 = estimateEntityFactory.createReceptedWithEstimateItems(warehouse.getId(), user.getUserId());

        RequestEntity<Void> request = RequestEntity.get(URI.create("/v3/users/"+user.getUserId()+"/estimates"))
            .header("Authorization", "Bearer " + accessToken)
            .build();

        ResponseEntity<EstimateSearchResponseDto> response = restTemplate.exchange(request, EstimateSearchResponseDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getEstimates());

        response.getBody().getEstimates().stream()
            .forEach(estimateSearchDto -> {
                assertNotNull(estimateSearchDto.getId());
                assertNotNull(estimateSearchDto.getWarehouse());
                assertEquals(estimateSearchDto.getWarehouse().getWarehouseId(), warehouse.getId());
                assertEquals(estimateSearchDto.getWarehouse().getAddress(), warehouse.getAddress());
                assertEquals(estimateSearchDto.getWarehouse().getName(), warehouse.getName());
                assertEquals(estimateSearchDto.getStatus(), EstimateStatus.RECEPTED);
                assertEquals(EstimateEntityFactory.MONTHLY_AVERAGE_RELEASE, estimateSearchDto.getMontlyAverageRelease());
            });
    }

    @Test
    public void get_estimateByUserId_responseIsNotFound_IfEstimatesNotExist() {
        RequestEntity<Void> request = RequestEntity.get(URI.create("/v3/users/"+user.getUserId()+"/estimates"))
            .header("Authorization", "Bearer " + accessToken)
            .build();

        ResponseEntity<EstimateSearchResponseDto> response = restTemplate.exchange(request, EstimateSearchResponseDto.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void get_estimateByUserId_responseIsUnAuthorized_IfAccessTokenNotGiven() {
        Warehouses warehouse = warehouseEntityFactory.createViewableWithNoMainItemTypes(accessToken);
        Estimates estimate1 = estimateEntityFactory.createReceptedWithEstimateItems(warehouse.getId(), user.getUserId());
        Estimates estimate2 = estimateEntityFactory.createReceptedWithEstimateItems(warehouse.getId(), user.getUserId());
        Estimates estimate3 = estimateEntityFactory.createReceptedWithEstimateItems(warehouse.getId(), user.getUserId());

        RequestEntity<Void> request = RequestEntity.get(URI.create("/v3/users/"+user.getUserId()+"/estimates"))
            .build();

        ResponseEntity<EstimateSearchResponseDto> response = restTemplate.exchange(request, EstimateSearchResponseDto.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }


    @Test
    public void get_estimateByUserId_responseIsForbidden_IfOtherUserId() {
        Warehouses warehouse = warehouseEntityFactory.createViewableWithNoMainItemTypes(accessToken);
        Estimates estimate1 = estimateEntityFactory.createReceptedWithEstimateItems(warehouse.getId(), user.getUserId());
        Estimates estimate2 = estimateEntityFactory.createReceptedWithEstimateItems(warehouse.getId(), user.getUserId());
        Estimates estimate3 = estimateEntityFactory.createReceptedWithEstimateItems(warehouse.getId(), user.getUserId());

        String otherUsersAccessToken = JwtTokenUtil.generateAccessToken(0, UserRole.USER);

        RequestEntity<Void> request = RequestEntity.get(URI.create("/v3/users/"+user.getUserId()+"/estimates"))
            .header("Authorization", "Bearer " + otherUsersAccessToken)
            .build();

        ResponseEntity<EstimateSearchResponseDto> response = restTemplate.exchange(request, EstimateSearchResponseDto.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}