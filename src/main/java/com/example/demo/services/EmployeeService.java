package com.example.demo.services;

import com.example.demo.models.Employee;
import com.example.demo.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public List<HashMap<String, String>> processFileForPreview(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(0);

            HashMap<String, Integer> headers = findHeaders(sheet);
            if (headers.isEmpty()) {
                throw new RuntimeException("No headers found in the Excel sheet.");
            }

            List<HashMap<String, String>> rows = extractRowsToMap(sheet, headers);
            List<HashMap<String, String>> previewData = new ArrayList<>();

            for (HashMap<String, String> row : rows) {
                String id = row.get("id");
                if (!employeeRepository.existsById(id)) {
                    previewData.add(row);
                }
            }

            workbook.close();
            return previewData;
        } catch (IOException e) {
            throw new RuntimeException("Error processing file: " + e.getMessage());
        }
    }

    private HashMap<String, Integer> findHeaders(Sheet sheet) {
        HashMap<String, Integer> headers = new HashMap<>();

        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell.getCellType() == CellType.STRING) {
                    String cellValue = cell.getStringCellValue();
                    if (cellValue != null && !cellValue.isEmpty()) {
                        headers.put(cellValue.toLowerCase().trim(), cell.getColumnIndex());
                    }
                }
            }
        }
        return headers;
    }

    private List<HashMap<String, String>> extractRowsToMap(Sheet sheet, HashMap<String, Integer> headers) {
        List<HashMap<String, String>> rows = new ArrayList<>();
        Iterator<Row> rowIterator = sheet.iterator();
        boolean headerRowPassed = false;

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();

            if (!headerRowPassed) {
                headerRowPassed = true;
                continue;  // Skip the header row
            }

            HashMap<String, String> rowData = new HashMap<>();
            for (String header : headers.keySet()) {
                int columnIndex = headers.get(header);
                Cell cell = row.getCell(columnIndex);

                // Handle null or blank cells
                String cellValue = "";
                if (cell != null) {
                    cellValue = switch (cell.getCellType()) {
                        case STRING -> cell.getStringCellValue().trim(); // Trim cell value
                        case NUMERIC -> String.valueOf(cell.getNumericCellValue());
                        default -> "";
                    };
                }

                // Replace null or blank cell values with ""
                if (cellValue == null || cellValue.isEmpty()) {
                    cellValue = "";
                }

                // Store the cell value in rowData
                rowData.put(header, cellValue);
            }

            // Add rowData to rows if not empty
            if (!rowData.isEmpty()) {
                rows.add(rowData);
            }
        }
        return rows;
    }

    public void saveRows(List<HashMap<String, String>> rows) {
        for (HashMap<String, String> row : rows) {
            Employee employee = new Employee();
            employee.setId(row.getOrDefault("id", "").trim());
            employee.setName(row.getOrDefault("name", "").trim());
            employee.setDepartment(row.getOrDefault("department", "").trim());
            String salaryStr = row.get("salary");
            employee.setSalary(salaryStr != null && !salaryStr.isEmpty() ? new BigDecimal(salaryStr) : BigDecimal.ZERO);

            employeeRepository.save(employee);
        }
    }

    public Page<Employee> getEmployees(int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        return employeeRepository.findAll(pageable);
    }

    public Employee getEmployeeById(String id) {
        Optional<Employee> optionalEmployee = employeeRepository.findById(id);
        return optionalEmployee.orElse(null);
    }

    public void updateEmployee(Employee updatedEmployee) {
        Optional<Employee> optionalEmployee = employeeRepository.findById(updatedEmployee.getId());
        if (optionalEmployee.isPresent()) {
            Employee employee = optionalEmployee.get();
            employee.setName(updatedEmployee.getName().trim());
            employee.setDepartment(updatedEmployee.getDepartment().trim());
            employee.setSalary(updatedEmployee.getSalary());
            employeeRepository.save(employee);
        } else {
            throw new RuntimeException("Employee not found with id: " + updatedEmployee.getId());
        }
    }

    public void deleteEmployee(String id) {
        employeeRepository.deleteById(id);
    }

    public Page<Employee> getEmployeesSortedByName(int page, int size) {
        return employeeRepository.findAll(PageRequest.of(page, size, Sort.by("name")));
    }

    public Page<Employee> getEmployeesSortedByDepartment(int page, int size) {
        return employeeRepository.findAll(PageRequest.of(page, size, Sort.by("department")));
    }
}


