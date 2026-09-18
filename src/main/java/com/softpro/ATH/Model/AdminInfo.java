package com.softpro.ATH.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "admininfo")
public class AdminInfo {
	
	@Id
	@Column(length = 60)
	private String userid;
	
	@Column(length = 100)
	private String name;
	
	@Column(length = 60)
	private String password;
	
	@Column(length = 1000, nullable = true)
	private String profilepic;

	public String getUserid() {
		return userid;
	}

	public void setUserid(String userid) {
		this.userid = userid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getProfilepic() {
		return profilepic;
	}

	public void setProfilepic(String profilepic) {
		this.profilepic = profilepic;
	}
	
}
