package com.tsg.employeeapi.service;



import com.tsg.employeeapi.domain.Employee;
import com.tsg.employeeapi.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.tsg.employeeapi.constant.Constant.PHOTO_DIRECTORY;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmployeeService {

    private final SequenceGenerator sequenceGenerator;

    private final EmployeeRepository repository;

    public Page<Employee> getAllContacts(int page, int size) {
        return repository.findAll(PageRequest.of(page, size, Sort.by("name")));
    }

    public Employee getContact(String id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Contact not found"));
    }

    public Employee createContact(Employee employee) {
        employee.setId(sequenceGenerator.generateNextId());
        return repository.save(employee);
    }

    public Employee createOrUpdateContact(Employee employee) {
        // Check if employee exists
        Optional<Employee> existingEmployee = repository.findByEmail(employee.getEmail());
        if (existingEmployee.isPresent()) {
            Employee updatedEmployee = existingEmployee.get();
            updatedEmployee.setName(employee.getName());
            updatedEmployee.setTitle(employee.getTitle());
            updatedEmployee.setPhone(employee.getPhone());
            updatedEmployee.setAddress(employee.getAddress());
            updatedEmployee.setStatus(employee.getStatus());
            updatedEmployee.setPhotoUrl(employee.getPhotoUrl());
            return repository.save(updatedEmployee);
        } else {
            employee.setId(sequenceGenerator.generateNextId());
            return repository.save(employee);
        }
    }



    public void deleteContact(Employee contact) {
        repository.deleteById(contact.getId());
    }

    public String uploadPhoto(String id, MultipartFile file) {
        log.info("Saving picture for user id: {}", id);
        Employee employee = getContact(id);
        String photoUrl = photoFunction.apply(id, file);

        employee.setPhotoUrl(photoUrl);
        repository.save(employee);

        return photoUrl;
    }
    private final Function<String, String> fileExtension = filename -> Optional.of(filename).filter(name -> name.contains(".")).map(name -> "." + name.substring(filename.lastIndexOf(".") + 1)).orElse(".png");

    private final BiFunction<String, MultipartFile, String>  photoFunction = (id, image) -> {
        String filename = id + fileExtension.apply(image.getOriginalFilename());
        try {
            Path fileStorageLocation = Paths.get(PHOTO_DIRECTORY).toAbsolutePath().normalize();

            if(!Files.exists(fileStorageLocation)) {
                Files.createDirectories(fileStorageLocation);
            }
            Files.copy(image.getInputStream(), fileStorageLocation.resolve(filename), REPLACE_EXISTING);
            return ServletUriComponentsBuilder.fromCurrentContextPath().path("/employees/image/" + filename).toUriString();
        }catch (Exception exception) {
            throw  new RuntimeException("Unable to save image");
        }
    };
}

