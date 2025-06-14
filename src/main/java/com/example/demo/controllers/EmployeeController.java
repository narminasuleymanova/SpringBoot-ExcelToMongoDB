package com.example.demo.controllers;

import com.example.demo.models.Employee;
import com.example.demo.services.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;

@Controller
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private List<HashMap<String, String>> previewData; // To hold previewed data temporarily

    // Mapping to display upload form
    @GetMapping("/upload-form")
    public String showUploadForm() {
        return "upload-form"; // Thymeleaf template name (upload-form.html)
    }

    // Handling file upload and preview
    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes,
                                   Model model) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a file to upload.");
            return "redirect:/employees/upload-form";
        }

        try {
            previewData = employeeService.processFileForPreview(file);
            model.addAttribute("previewData", previewData);
            return "preview"; // Thymeleaf template name (preview.html)
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error processing file: " + e.getMessage());
            return "redirect:/employees/upload-form";
        }
    }

    // Mapping to confirm upload
    @PostMapping("/confirm")
    public String confirmUpload(RedirectAttributes redirectAttributes) {
        if (previewData != null && !previewData.isEmpty()) {
            employeeService.saveRows(previewData);
            previewData = null; // Clear preview data after saving
            redirectAttributes.addFlashAttribute("message", "Data successfully uploaded.");
        } else {
            redirectAttributes.addFlashAttribute("error", "No data to upload.");
        }
        return "redirect:/employees/show";
    }

    // Mapping to decline upload
    @PostMapping("/decline")
    public String declineUpload(RedirectAttributes redirectAttributes) {
        previewData = null; // Clear preview data
        redirectAttributes.addFlashAttribute("message", "Data upload cancelled.");
        return "redirect:/employees/upload-form";
    }

    // Mapping to display all employees
    @GetMapping("/show")
    public String showEmployees(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size,
                                @RequestParam(name = "sortBy", defaultValue = "name") String sortBy,
                                Model model) {
        Page<Employee> employeePage;

        if ("name".equals(sortBy)) {
            employeePage = employeeService.getEmployeesSortedByName(page, size);
        } else if ("department".equals(sortBy)) {
            employeePage = employeeService.getEmployeesSortedByDepartment(page, size);
        } else {
            employeePage = employeeService.getEmployees(page, size);
        }

        model.addAttribute("employeePage", employeePage);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("sortBy", sortBy); // Pass sortBy to the view to maintain state

        return "show-employees";
    }

    // Mapping to handle update employee form
    @GetMapping("/update/{id}")
    public String showUpdateForm(@PathVariable String id, Model model) {
        Employee employee = employeeService.getEmployeeById(id);
        model.addAttribute("employee", employee);
        return "update-employee"; // Thymeleaf template name (update-employee.html)
    }

    // Processing updated employee data
    @PostMapping("/update/{id}")
    public String updateEmployee(@PathVariable String id,
                                 @ModelAttribute("employee") Employee updatedEmployee,
                                 RedirectAttributes redirectAttributes) {
        try {
            updatedEmployee.setId(id);

            employeeService.updateEmployee(updatedEmployee);

            redirectAttributes.addFlashAttribute("message", "Employee updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating employee: " + e.getMessage());
        }

        return "redirect:/employees/show";
    }

    // Mapping to handle delete employee
    @PostMapping("/{id}")
    public String deleteEmployee(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            employeeService.deleteEmployee(id);
            redirectAttributes.addFlashAttribute("message", "Employee deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting employee: " + e.getMessage());
        }
        return "redirect:/employees/show";
    }
}
