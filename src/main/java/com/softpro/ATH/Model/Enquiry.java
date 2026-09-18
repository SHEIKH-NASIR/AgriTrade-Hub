package com.softpro.ATH.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "enquiries")
public class Enquiry {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@Column(length = 100, nullable = false)
	private String name;
	
	@Column(length = 13, nullable = false)
	private String contactno;
	
	@Column(length = 100, nullable = false)
	private String email;
	
	@Column(length = 500)
	private String address;
	
	@Column(length = 1000, nullable = false)
	private String message;
	
	@Column(length = 50, nullable = false)
	private String enquirydate;
	
	 @Column(nullable = true, length = 1000)
	 private String adminResponse; // Admin's response to the enquiry (nullable)
	
	@Column(nullable = false)
    private String status = "PENDING"; // Status of the enquiry (PENDING/RESOLVED)

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getContactno() {
		return contactno;
	}

	public void setContactno(String contactno) {
		this.contactno = contactno;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getEnquirydate() {
		return enquirydate;
	}

	public void setEnquirydate(String enquirydate) {
		this.enquirydate = enquirydate;
	}

	public String getAdminResponse() {
		return adminResponse;
	}

	public void setAdminResponse(String adminResponse) {
		this.adminResponse = adminResponse;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
	
}
