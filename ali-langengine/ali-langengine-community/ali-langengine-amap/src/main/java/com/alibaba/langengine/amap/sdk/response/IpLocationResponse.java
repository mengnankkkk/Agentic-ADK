/**
 * Copyright (C) 2024 AIDC-AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.langengine.amap.sdk.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Gaode Map IP Location API Response POJO Class
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class IpLocationResponse {

	/**
	 * Return result status value 0 or 1, 0 means failure; 1 means success
	 */
	@JsonProperty("status")
	private String status;

	/**
	 * Return status description When status is 0, info returns the error reason,
	 * otherwise returns "OK"
	 */
	@JsonProperty("info")
	private String info;

	/**
	 * Status code 10000 represents correct, see info status table for details
	 */
	@JsonProperty("infocode")
	private String infocode;

	/**
	 * Province name For municipalities, the municipality name is displayed; For local
	 * network IP segments, returns "局域网"; For illegal IPs and foreign IPs, returns empty
	 */
	private String province;

	/**
	 * City name For municipalities, the municipality name is displayed; For local network
	 * IP segments, illegal IPs or foreign IPs, returns empty
	 */
	private String city;

	/**
	 * City's adcode Adcode information can be referenced from the city code table
	 */
	private String adcode;

	/**
	 * Rectangular area range of the city The lower left and upper right coordinates of
	 * the city range
	 */
	private String rectangle;

	private boolean isEmpty(Object obj) {
		if (obj instanceof List<?>) {
            List<?> list = (List<?>) obj;
			return list.isEmpty();
		}
		return obj == null || obj.toString().isEmpty();
	}

	// Getters and Setters

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	public String getInfocode() {
		return infocode;
	}

	public void setInfocode(String infocode) {
		this.infocode = infocode;
	}

	public String getProvince() {
		return province;
	}

	@JsonProperty("province")
	public void setProvince(Object province) {
		this.province = isEmpty(province) ? "" : province.toString();
	}

	public String getCity() {
		return city;
	}

	@JsonProperty("city")
	public void setCity(Object city) {
		this.city = isEmpty(city) ? "" : city.toString();
	}

	public String getAdcode() {
		return adcode;
	}

	@JsonProperty("adcode")
	public void setAdcode(Object adcode) {
		this.adcode = isEmpty(adcode) ? "" : adcode.toString();
	}

	public String getRectangle() {
		return rectangle;
	}

	@JsonProperty("rectangle")
	public void setRectangle(Object rectangle) {
		this.rectangle = isEmpty(rectangle) ? "" : rectangle.toString();
	}

}
