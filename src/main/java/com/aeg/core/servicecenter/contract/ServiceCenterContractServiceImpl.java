package com.aeg.core.servicecenter.contract;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.servicecenter.ServiceCenterRepository;
import com.aeg.core.servicecenter.contract.dto.ServiceCenterContractRequest;
import com.aeg.core.servicecenter.contract.dto.ServiceCenterContractResponse;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional
public class ServiceCenterContractServiceImpl implements ServiceCenterContractService {

	private final ServiceCenterContractRepository repository;
	private final ServiceCenterRepository serviceCenterRepository;

	public ServiceCenterContractServiceImpl(
			ServiceCenterContractRepository repository,
			ServiceCenterRepository serviceCenterRepository) {
		this.repository = repository;
		this.serviceCenterRepository = serviceCenterRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<ServiceCenterContractResponse> findAll() {
		return repository.findAll().stream().map(this::toResponse).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public ServiceCenterContractResponse findById(Long id) {
		return toResponse(findEntity(id));
	}

	@Override
	public ServiceCenterContractResponse create(ServiceCenterContractRequest request) {
		ServiceCenterContract e = new ServiceCenterContract();
		applyRequest(e, request);
		return toResponse(repository.save(e));
	}

	@Override
	public ServiceCenterContractResponse update(Long id, ServiceCenterContractRequest request) {
		ServiceCenterContract e = findEntity(id);
		applyRequest(e, request);
		return toResponse(repository.save(e));
	}

	@Override
	public void delete(Long id) {
		repository.delete(findEntity(id));
	}

	private ServiceCenterContract findEntity(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Service center contract not found with id: " + id));
	}

	private void applyRequest(ServiceCenterContract e, ServiceCenterContractRequest request) {
		e.setServiceCenter(serviceCenterRepository.getReferenceById(request.serviceCenterId()));
		e.setStartDate(request.startDate());
		e.setEndDate(request.endDate());
		e.setPhotoUrls(request.photoUrls().toArray(String[]::new));
	}

	private ServiceCenterContractResponse toResponse(ServiceCenterContract e) {
		return new ServiceCenterContractResponse(
				e.getId(),
				e.getServiceCenterId(),
				e.getStartDate(),
				e.getEndDate(),
				e.getCreatedAt(),
				e.getPhotoUrls() == null ? List.of() : Arrays.asList(e.getPhotoUrls()));
	}
}
