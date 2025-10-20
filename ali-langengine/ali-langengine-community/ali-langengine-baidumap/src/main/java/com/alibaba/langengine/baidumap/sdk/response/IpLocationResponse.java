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

package com.alibaba.langengine.baidumap.sdk.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class IpLocationResponse {

	/**
	 * Result status code
	 */
	@JsonProperty("status")
	private Integer status;

	/**
	 * Detailed address information and confidence level, format:
	 * CN|province|city|district|street|confidence1|confidence2|unknown
	 */
	@JsonProperty("address")
	private String address;

	/**
	 * Address content details
	 */
	@JsonProperty("content")
	private Content content;

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public Content getContent() {
		return content;
	}

	public void setContent(Content content) {
		this.content = content;
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Content {

		/**
		 * Brief address information
		 */
		@JsonProperty("address")
		private String address;

		/**
		 * Detailed address information
		 */
		@JsonProperty("address_detail")
		private AddressDetail addressDetail;

		/**
		 * Coordinate point information
		 */
		@JsonProperty("point")
		private Point point;

		public String getAddress() {
			return address;
		}

		public void setAddress(String address) {
			this.address = address;
		}

		public AddressDetail getAddressDetail() {
			return addressDetail;
		}

		public void setAddressDetail(AddressDetail addressDetail) {
			this.addressDetail = addressDetail;
		}

		public Point getPoint() {
			return point;
		}

		public void setPoint(Point point) {
			this.point = point;
		}

		@JsonInclude(JsonInclude.Include.NON_NULL)
		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class AddressDetail {

			/**
			 * Administrative division code
			 */
			@JsonProperty("adcode")
			private String adcode;

			/**
			 * City
			 */
			@JsonProperty("city")
			private String city;

			/**
			 * Baidu city code
			 */
			@JsonProperty("city_code")
			private Integer cityCode;

			/**
			 * District/county
			 */
			@JsonProperty("district")
			private String district;

			/**
			 * Province
			 */
			@JsonProperty("province")
			private String province;

			/**
			 * Road name
			 */
			@JsonProperty("street")
			private String street;

			/**
			 * Road number
			 */
			@JsonProperty("street_number")
			private String streetNumber;

			public String getAdcode() {
				return adcode;
			}

			public void setAdcode(String adcode) {
				this.adcode = adcode;
			}

			public String getCity() {
				return city;
			}

			public void setCity(String city) {
				this.city = city;
			}

			public Integer getCityCode() {
				return cityCode;
			}

			public void setCityCode(Integer cityCode) {
				this.cityCode = cityCode;
			}

			public String getDistrict() {
				return district;
			}

			public void setDistrict(String district) {
				this.district = district;
			}

			public String getProvince() {
				return province;
			}

			public void setProvince(String province) {
				this.province = province;
			}

			public String getStreet() {
				return street;
			}

			public void setStreet(String street) {
				this.street = street;
			}

			public String getStreetNumber() {
				return streetNumber;
			}

			public void setStreetNumber(String streetNumber) {
				this.streetNumber = streetNumber;
			}

		}

		@JsonInclude(JsonInclude.Include.NON_NULL)
		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class Point {

			/**
			 * Longitude of the city center point
			 */
			@JsonProperty("x")
			private String x;

			/**
			 * Latitude of the city center point
			 */
			@JsonProperty("y")
			private String y;

			public String getX() {
				return x;
			}

			public void setX(String x) {
				this.x = x;
			}

			public String getY() {
				return y;
			}

			public void setY(String y) {
				this.y = y;
			}

		}

	}

}
