package com.softpro.ATH.Model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

@Entity
@Table(name = "fpos", uniqueConstraints = {
		@UniqueConstraint(columnNames = "email"),
		@UniqueConstraint(columnNames = "registrationNumber")
})
public class FPO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(nullable = false, length = 150)
	private String name;

	@Column(nullable = false, length = 100)
	private String registrationNumber;

	@Column(nullable = false, length = 120)
	private String contactPerson;

	@Column(nullable = false, length = 15)
	private String phone;

	@Column(nullable = false, length = 150)
	private String email;

	@Column(nullable = false, length = 60)
	private String password;

	@Column(nullable = false, length = 500)
	private String address;

	@Column(length = 100)
	private String state;

	@Column(length = 100)
	private String district;

	@Column(length = 1000)
	private String description;

	@Column(nullable = false, length = 20)
	private String status = "Pending";

	@Column(length = 30, nullable = false)
	private String regdate;

	@Column(nullable = false)
	private boolean demoEnabled = false;

	@ManyToMany
	@JoinTable(name = "fpo_members",
			joinColumns = @JoinColumn(name = "fpo_id"),
			inverseJoinColumns = @JoinColumn(name = "farmer_id"),
			uniqueConstraints = @UniqueConstraint(columnNames = {"fpo_id", "farmer_id"}))
	private List<Formers> members = new ArrayList<>();

	public long getId() { return id; }
	public void setId(long id) { this.id = id; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public String getRegistrationNumber() { return registrationNumber; }
	public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }
	public String getContactPerson() { return contactPerson; }
	public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
	public String getPhone() { return phone; }
	public void setPhone(String phone) { this.phone = phone; }
	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }
	public String getPassword() { return password; }
	public void setPassword(String password) { this.password = password; }
	public String getAddress() { return address; }
	public void setAddress(String address) { this.address = address; }
	public String getState() { return state; }
	public void setState(String state) { this.state = state; }
	public String getDistrict() { return district; }
	public void setDistrict(String district) { this.district = district; }
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }
	public String getRegdate() { return regdate; }
	public void setRegdate(String regdate) { this.regdate = regdate; }
	public List<Formers> getMembers() { return members; }
	public void setMembers(List<Formers> members) { this.members = members; }
	public boolean isDemoEnabled() { return demoEnabled; }

	public void setDemoEnabled(boolean demoEnabled) { this.demoEnabled = demoEnabled; }

}
