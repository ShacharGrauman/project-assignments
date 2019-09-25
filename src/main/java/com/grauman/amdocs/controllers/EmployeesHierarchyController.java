package com.grauman.amdocs.controllers;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.grauman.amdocs.dao.EmployeeDataDAO;
import com.grauman.amdocs.models.EmployeeData;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/hierarchy")
@RestController
@CrossOrigin(origins = "*" , allowCredentials = "true")
public class EmployeesHierarchyController {

	@Autowired
	private EmployeeDataDAO employeeDataDAO;

	@GetMapping("")
	public ResponseEntity<Map<EmployeeData, List<EmployeeData>>> hierarchy() throws SQLException {
		Map<EmployeeData, List<EmployeeData>> map = employeeDataDAO.findEmployeesHierarchy();
		return ResponseEntity.ok().body(map);
	}
}
