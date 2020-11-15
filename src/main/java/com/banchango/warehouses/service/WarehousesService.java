package com.banchango.warehouses.service;

import com.banchango.auth.exception.AuthenticateException;
import com.banchango.auth.token.JwtTokenUtil;
import com.banchango.domain.agencymainitemtypes.AgencyMainItemTypesRepository;
import com.banchango.domain.agencywarehousedetails.AgencyWarehouseDetails;
import com.banchango.domain.agencywarehousedetails.AgencyWarehouseDetailsRepository;
import com.banchango.domain.agencywarehousepayments.AgencyWarehousePaymentsRepository;
import com.banchango.domain.deliverytypes.DeliveryTypes;
import com.banchango.domain.deliverytypes.DeliveryTypesRepository;
import com.banchango.domain.generalwarehousedetails.GeneralWarehouseDetailsRepository;
import com.banchango.domain.insurances.Insurances;
import com.banchango.domain.insurances.InsurancesRepository;
import com.banchango.domain.warehouseattachments.WarehouseAttachmentsRepository;
import com.banchango.domain.warehouselocations.WarehouseLocationsRepository;
import com.banchango.domain.warehouses.ServiceType;
import com.banchango.domain.warehouses.Warehouses;
import com.banchango.domain.warehouses.WarehousesRepository;
import com.banchango.domain.warehousetypes.WarehouseTypes;
import com.banchango.domain.warehousetypes.WarehouseTypesRepository;
import com.banchango.tools.ObjectMaker;
import com.banchango.warehouses.dto.*;
import com.banchango.warehouses.exception.WarehouseAlreadyRegisteredException;
import com.banchango.warehouses.exception.WarehouseIdNotFoundException;
import com.banchango.warehouses.exception.WarehouseInvalidAccessException;
import com.banchango.warehouses.exception.WarehouseSearchException;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class WarehousesService {

    private final WarehousesRepository warehousesRepository;
    private final DeliveryTypesRepository deliveryTypesRepository;
    private final WarehouseLocationsRepository warehouseLocationsRepository;
    private final WarehouseTypesRepository warehouseTypesRepository;
    private final WarehouseAttachmentsRepository warehouseAttachmentsRepository;
    private final InsurancesRepository insurancesRepository;
    private final AgencyWarehouseDetailsRepository agencyWarehouseDetailsRepository;
    private final AgencyMainItemTypesRepository agencyMainItemTypesRepository;
    private final AgencyWarehousePaymentsRepository agencyWarehousePaymentsRepository;
    private final GeneralWarehouseDetailsRepository generalWarehouseDetailsRepository;

    @Transactional
    public JSONObject saveAgencyWarehouse(AgencyWarehouseInsertRequestDto wrapperDto, String token) throws Exception{
       if(!JwtTokenUtil.isTokenValidated(JwtTokenUtil.getToken(token))) {
           throw new AuthenticateException();
       }
       int userId = Integer.parseInt(JwtTokenUtil.extractUserId(JwtTokenUtil.getToken(token)));
       if(warehousesRepository.findByUserId(userId).isPresent()) {
           throw new WarehouseAlreadyRegisteredException();
       }
       if(wrapperDto.getInsurance() != null) {
           int insuranceId = getSavedInsuranceId(wrapperDto.getInsurance().toEntity());
           Warehouses warehouse = toWarehouseEntityWithInsurance(wrapperDto, insuranceId, userId);
           int warehouseId = warehousesRepository.save(warehouse).getWarehouseId();
           saveWarehouseType(wrapperDto.getWarehouseType(), warehouseId);
           saveWarehouseLocation(wrapperDto.getLocation(), warehouseId);
           saveAgencyWarehouseDetailInformations(wrapperDto.getAgencyDetail(), warehouseId);
       } else {
           Warehouses warehouse = toWarehouseEntityWithoutInsurance(wrapperDto, userId);
           int warehouseId = warehousesRepository.save(warehouse).getWarehouseId();
           saveWarehouseType(wrapperDto.getWarehouseType(), warehouseId);
           saveWarehouseLocation(wrapperDto.getLocation(), warehouseId);
           saveAgencyWarehouseDetailInformations(wrapperDto.getAgencyDetail(), warehouseId);
       }
       JSONObject jsonObject = ObjectMaker.getJSONObject();
       jsonObject.put("message", "창고가 정상적으로 등록 되었습니다.");
       return jsonObject;
    }

    @Transactional
    public JSONObject saveGeneralWarehouse(GeneralWarehouseInsertRequestDto wrapperDto, String token) throws Exception {
        if(!JwtTokenUtil.isTokenValidated(JwtTokenUtil.getToken(token))) {
            throw new AuthenticateException();
        }
        int userId = Integer.parseInt(JwtTokenUtil.extractUserId(JwtTokenUtil.getToken(token)));
        if(warehousesRepository.findByUserId(userId).isPresent()) {
            throw new WarehouseAlreadyRegisteredException();
        }
        if(wrapperDto.getInsurance() != null) {
            int insuranceId = getSavedInsuranceId(wrapperDto.getInsurance().toEntity());
            Warehouses warehouse = toWarehouseEntityWithInsurance(wrapperDto, insuranceId, userId);
            int warehouseId = warehousesRepository.save(warehouse).getWarehouseId();
            saveGeneralWarehouseDetailInformations(wrapperDto.getGeneralDetail(), warehouseId);
            saveWarehouseType(wrapperDto.getWarehouseType(), warehouseId);
            saveWarehouseLocation(wrapperDto.getLocation(), warehouseId);
        } else {
            Warehouses warehouse = toWarehouseEntityWithoutInsurance(wrapperDto, userId);
            int warehouseId = warehousesRepository.save(warehouse).getWarehouseId();
            saveGeneralWarehouseDetailInformations(wrapperDto.getGeneralDetail(), warehouseId);
            saveWarehouseType(wrapperDto.getWarehouseType(), warehouseId);
            saveWarehouseLocation(wrapperDto.getLocation(), warehouseId);
        }
        JSONObject jsonObject = ObjectMaker.getJSONObject();
        jsonObject.put("message", "창고가 정상적으로 등록되었습니다.");
        return jsonObject;
    }

    private Warehouses toWarehouseEntityWithInsurance(WarehouseInsertRequestDto wrapperDto, Integer insuranceId, Integer userId) {
        return Warehouses.builder()
                .canUse(wrapperDto.getCanUse()).name(wrapperDto.getName())
                .insuranceId(insuranceId).serviceType(wrapperDto.getServiceType())
                .landArea(wrapperDto.getLandArea()).totalArea(wrapperDto.getTotalArea())
                .address(wrapperDto.getAddress()).addressDetail(wrapperDto.getAddressDetail())
                .description(wrapperDto.getDescription()).availableWeekdays(wrapperDto.getAvailableWeekdays())
                .openAt(wrapperDto.getOpenAt()).closeAt(wrapperDto.getCloseAt())
                .availableTimeDetail(wrapperDto.getAvailableTimeDetail()).cctvExist(wrapperDto.getCctvExist())
                .securityCompanyExist(wrapperDto.getSecurityCompanyExist()).securityCompanyName(wrapperDto.getSecurityCompanyName())
                .doorLockExist(wrapperDto.getDoorLockExist()).airConditioningType(wrapperDto.getAirConditioningType())
                .workerExist(wrapperDto.getWorkerExist()).canPickup(wrapperDto.getCanPickup())
                .canPark(wrapperDto.getCanPark()).parkingScale(wrapperDto.getParkingScale())
                .userId(userId).build();
    }

    private Warehouses toWarehouseEntityWithoutInsurance(WarehouseInsertRequestDto wrapperDto, Integer userId) {
        return Warehouses.builder()
                .canUse(wrapperDto.getCanUse()).name(wrapperDto.getName())
                .serviceType(wrapperDto.getServiceType())
                .landArea(wrapperDto.getLandArea()).totalArea(wrapperDto.getTotalArea())
                .address(wrapperDto.getAddress()).addressDetail(wrapperDto.getAddressDetail())
                .description(wrapperDto.getDescription()).availableWeekdays(wrapperDto.getAvailableWeekdays())
                .openAt(wrapperDto.getOpenAt()).closeAt(wrapperDto.getCloseAt())
                .availableTimeDetail(wrapperDto.getAvailableTimeDetail()).cctvExist(wrapperDto.getCctvExist())
                .securityCompanyExist(wrapperDto.getSecurityCompanyExist()).securityCompanyName(wrapperDto.getSecurityCompanyName())
                .doorLockExist(wrapperDto.getDoorLockExist()).airConditioningType(wrapperDto.getAirConditioningType())
                .workerExist(wrapperDto.getWorkerExist()).canPickup(wrapperDto.getCanPickup())
                .canPark(wrapperDto.getCanPark()).parkingScale(wrapperDto.getParkingScale())
                .userId(userId).build();
    }

    private void saveWarehouseType(String warehouseType, Integer warehouseId) {
        warehouseTypesRepository.save(WarehouseTypes.builder().name(warehouseType).warehouseId(warehouseId).build());
    }
    private void saveWarehouseLocation(WarehouseLocationDto locationDto, Integer warehouseId) {
        warehouseLocationsRepository.save(locationDto.toEntity(warehouseId));
    }

    private void saveGeneralWarehouseDetailInformations(GeneralWarehouseDetailInsertRequestDto requestDto, Integer warehouseId) {
        generalWarehouseDetailsRepository.save(requestDto.toEntity(warehouseId));
    }

    private void saveAgencyWarehouseDetailInformations(AgencyWarehouseDetailInsertRequestDto requestDto, Integer warehouseId) {
        int agencyWarehouseDetailId = getSavedAgencyWarehouseDetailId(requestDto.toAgencyWarehouseDetailEntity(warehouseId));
        agencyMainItemTypesRepository.save(requestDto.toAgencyMainItemsEntity(agencyWarehouseDetailId));
        saveWarehousePayments(requestDto.getPayments(), agencyWarehouseDetailId);
        saveDeliveryTypes(requestDto.getDeliveryTypes(), agencyWarehouseDetailId);
    }

    private void saveWarehousePayments(AgencyWarehousePaymentInsertRequestDto[] payments, Integer agencyWarehouseDetailId) {
        for(AgencyWarehousePaymentInsertRequestDto dto : payments) {
            agencyWarehousePaymentsRepository.save(dto.toEntity(agencyWarehouseDetailId));
        }
    }

    private void saveDeliveryTypes(String[] names, Integer agencyWarehouseDetailId) {
        for(String name : names) {
            deliveryTypesRepository.save(DeliveryTypes.builder().name(name).agencyWarehouseDetailId(agencyWarehouseDetailId).build());
        }
    }

    private Integer getSavedAgencyWarehouseDetailId(AgencyWarehouseDetails detail) {
        return agencyWarehouseDetailsRepository.save(detail).getAgencyWarehouseDetailId();
    }

    private Integer getSavedInsuranceId(Insurances insurance) {
        return insurancesRepository.save(insurance).getInsuranceId();
    }

    @Transactional(readOnly = true)
    public JSONObject search(String address, Integer limit, Integer offset) throws WarehouseSearchException{
        JSONObject jsonObject = ObjectMaker.getJSONObject();
        JSONArray jsonArray = ObjectMaker.getJSONArray();
        PageRequest request = PageRequest.of(limit, offset);
        List<WarehouseSearchResponseDto> warehouses = warehousesRepository.findByAddressContaining(address, request).stream().map(WarehouseSearchResponseDto::new).collect(Collectors.toList());
        if(warehouses.size() == 0) throw new WarehouseSearchException();
        for(WarehouseSearchResponseDto searchResponseDto : warehouses) {
            WarehouseLocationDto locationDto = new WarehouseLocationDto(warehouseLocationsRepository.findByWarehouseId(searchResponseDto.getWarehouseId()));
            WarehouseTypesDto typesDto = new WarehouseTypesDto(warehouseTypesRepository.findByWarehouseId(searchResponseDto.getWarehouseId()));
            List<WarehouseAttachmentDto> attachmentsList = warehouseAttachmentsRepository.findByWarehouseId(searchResponseDto.getWarehouseId()).stream().map(WarehouseAttachmentDto::new).collect(Collectors.toList());
            if(attachmentsList.size() != 0) {
                jsonArray.put(searchResponseDto.toJSONObjectWithLocationAndAttachmentAndType(locationDto, attachmentsList.get(0), typesDto));
            } else {
                jsonArray.put(searchResponseDto.toJSONObjectWithLocationAndType(locationDto, typesDto));
            }
        }
        jsonObject.put("warehouses", jsonArray);
        return jsonObject;
    }

    @Transactional(readOnly = true)
    public JSONObject getAgencyWarehouseList(String token) throws Exception{
        if(!JwtTokenUtil.isTokenValidated(JwtTokenUtil.getToken(token))) {
            throw new AuthenticateException();
        }
        JSONObject jsonObject = ObjectMaker.getJSONObject();
        JSONArray jsonArray = ObjectMaker.getJSONArray();
        List<AgencyWarehouseListResponseDto> warehousesList = warehousesRepository.findByServiceType(ServiceType.AGENCY).stream().map(AgencyWarehouseListResponseDto::new).collect(Collectors.toList());
        for(AgencyWarehouseListResponseDto dto : warehousesList) {
            dto.setWarehouseType(agencyWarehouseDetailsRepository.findByWarehouseId(dto.getWarehouseId()).orElseThrow(WarehouseIdNotFoundException::new).getType());
            dto.setWarehouseCondition(warehouseTypesRepository.findByWarehouseId(dto.getWarehouseId()).getName());
            JSONObject listObject = dto.toJSONObject();
            jsonArray.put(listObject);
        }
        jsonObject.put("warehouses", jsonArray);
        return jsonObject;
    }

    @Transactional
    public JSONObject delete(Integer warehouseId, String token) throws Exception {
        if(!JwtTokenUtil.isTokenValidated(JwtTokenUtil.getToken(token))) {
            throw new AuthenticateException();
        }
        Warehouses warehouse = warehousesRepository.findByWarehouseId(warehouseId).orElseThrow(WarehouseIdNotFoundException::new);
        if(!warehouse.getUserId().equals(Integer.parseInt(JwtTokenUtil.extractUserId(JwtTokenUtil.getToken(token))))) {
            throw new WarehouseInvalidAccessException();
        }
        if(warehouse.getInsuranceId() != null) {
            insurancesRepository.deleteByInsuranceId(warehouse.getInsuranceId());
        }
        warehousesRepository.delete_(warehouseId);
        JSONObject jsonObject = ObjectMaker.getJSONObject();
        jsonObject.put("message", "창고가 정상적으로 삭제되었습니다.");
        return jsonObject;
    }

    @Transactional(readOnly = true)
    public JSONObject getSpecificWarehouseInfo(Integer warehouseId, String token) throws Exception {
        if(!JwtTokenUtil.isTokenValidated(JwtTokenUtil.getToken(token))) {
            throw new AuthenticateException();
        }
        return createJSONObjectOfSpecificWarehouseInfo(warehouseId);
    }

    private JSONObject createJSONObjectOfSpecificWarehouseInfo(Integer warehouseId) throws WarehouseIdNotFoundException {
        WarehouseResponseDto warehouseResponseDto = new WarehouseResponseDto(warehousesRepository.findByWarehouseId(warehouseId).orElseThrow(WarehouseIdNotFoundException::new));
        JSONObject jsonObject = warehouseResponseDto.toJSONObject();
        WarehouseLocationDto locationDto = new WarehouseLocationDto(warehouseLocationsRepository.findByWarehouseId(warehouseId));
        jsonObject.put("location", locationDto.toJSONObject());
        jsonObject.put("warehouseCondition", warehouseTypesRepository.findByWarehouseId(warehouseId).getName());
        Integer agencyWarehouseDetailId = getAgencyWarehouseDetailId(warehouseId);
        if(warehouseResponseDto.getInsuranceId() != null) {
            jsonObject.put("insuranceName", insurancesRepository.findByInsuranceId(warehouseResponseDto.getInsuranceId()).getName());
        }
        jsonObject.put("agencyDetails", createJSONObjectOfAgencyDetails(warehouseId, agencyWarehouseDetailId));
        return jsonObject;
    }

    private Integer getAgencyWarehouseDetailId(Integer warehouseId) throws WarehouseIdNotFoundException{
        return agencyWarehouseDetailsRepository.findByWarehouseId(warehouseId).orElseThrow(WarehouseIdNotFoundException::new).getAgencyWarehouseDetailId();
    }

    private JSONArray createJSONArrayOfDeliveryTypes(Integer agencyWarehouseDetailId) {
        JSONArray jsonArray = ObjectMaker.getJSONArray();
        List<DeliveryTypes> deliveryTypes = deliveryTypesRepository.findByAgencyWarehouseDetailId(agencyWarehouseDetailId);
        for(DeliveryTypes type : deliveryTypes) {
            jsonArray.put(type.getName());
        }
        return jsonArray;
    }

    private JSONArray createJSONArrayOfPayments(Integer agencyWarehouseDetailId) {
        JSONArray jsonArray = ObjectMaker.getJSONArray();
        List<AgencyWarehousePaymentResponseDto> paymentList = agencyWarehousePaymentsRepository.findByAgencyWarehouseDetailId(agencyWarehouseDetailId).stream().map(AgencyWarehousePaymentResponseDto::new).collect(Collectors.toList());
        for(AgencyWarehousePaymentResponseDto dto : paymentList) {
            jsonArray.put(dto.toJSONObject());
        }
        return jsonArray;
    }

    private JSONObject createJSONObjectOfAgencyDetails(Integer warehouseId, Integer agencyWarehouseDetailId) throws WarehouseIdNotFoundException{
        JSONObject jsonObject = ObjectMaker.getJSONObject();
        jsonObject.put("agencyWarehouseDetailId", agencyWarehouseDetailId);
        jsonObject.put("warehouseType", agencyWarehouseDetailsRepository.findByWarehouseId(warehouseId).orElseThrow(WarehouseIdNotFoundException::new).getType());
        jsonObject.put("mainItemType", agencyMainItemTypesRepository.findByAgencyWarehouseDetailId(agencyWarehouseDetailId).getName());
        jsonObject.put("deliveryTypes", createJSONArrayOfDeliveryTypes(agencyWarehouseDetailId));
        jsonObject.put("payments", createJSONArrayOfPayments(agencyWarehouseDetailId));
        return jsonObject;
    }
}