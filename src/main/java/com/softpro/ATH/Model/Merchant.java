package com.softpro.ATH.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "merchant", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class Merchant {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	private String name;
	
	@Column(nullable = false, length = 150, unique = true)
	private String email;
	
	@Column(nullable = false, length = 60)
	private String password;
	
	@Column(nullable = false, length = 13)
	private String contactno;
	
	@Column(nullable = false, length = 12)
	private String aadharno;
	
	@Column(nullable = false, length = 18)
	private String pancardno;
	
	
	@Column(nullable = false, length = 500)
	private String address;
	
	@Column(length = 1000, nullable = true)
	private String profilepic;
	
	@Column(nullable = false, length = 20)
	private String status;
	
	@Column(length = 30, nullable = false)
	private String regdate;

	@Column(nullable = false)
	private boolean demoEnabled = false;

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

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getContactno() {
		return contactno;
	}

	public void setContactno(String contactno) {
		this.contactno = contactno;
	}

	public String getAadharno() {
		return aadharno;
	}

	public void setAadharno(String aadharno) {
		this.aadharno = aadharno;
	}

	public String getPancardno() {
		return pancardno;
	}

	public void setPancardno(String pancardno) {
		this.pancardno = pancardno;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public	String getProfilepic() {
		return profilepic;
	}

	public void setProfilepic(String profilepic) {
		this.profilepic = profilepic;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getRegdate() {
		return regdate;
	}

	public void setRegdate(String regdate) {
		this.regdate = regdate;
	}
	
	public boolean isDemoEnabled() { return demoEnabled; }

	public void setDemoEnabled(boolean demoEnabled) { this.demoEnabled = demoEnabled; }

}
