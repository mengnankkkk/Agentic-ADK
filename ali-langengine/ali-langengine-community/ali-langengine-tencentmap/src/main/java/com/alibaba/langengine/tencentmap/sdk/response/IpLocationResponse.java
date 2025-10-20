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

package com.alibaba.langengine.tencentmap.sdk.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class IpLocationResponse {

	/**
	 * Status code, 0 indicates normal, others indicate exceptions. Refer to status code
	 * description for details
	 */
	@JsonProperty("status")
	private Integer status;

	/**
	 * Status description
	 */
	@JsonProperty("message")
	private String message;

	/**
	 * IP location result
	 */
	@JsonProperty("result")
	private Result result;

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Result getResult() {
		return result;
	}

	public void setResult(Result result) {
		this.result = result;
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Result {

		/**
		 * IP address used for location
		 */
		@JsonProperty("ip")
		private String ip;

		/**
		 * Location coordinates. Note: IP location service is accurate to the city level,
		 * this position is the coordinate of the administrative division government to
		 * which the IP address belongs.
		 */
		@JsonProperty("location")
		private Location location;

		/**
		 * Location administrative division information
		 */
		@JsonProperty("ad_info")
		private AdInfo adInfo;

		public String getIp() {
			return ip;
		}

		public void setIp(String ip) {
			this.ip = ip;
		}

		public Location getLocation() {
			return location;
		}

		public void setLocation(Location location) {
			this.location = location;
		}

		public AdInfo getAdInfo() {
			return adInfo;
		}

		public void setAdInfo(AdInfo adInfo) {
			this.adInfo = adInfo;
		}

		@JsonInclude(JsonInclude.Include.NON_NULL)
		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class Location {

			/**
			 * Latitude
			 */
			@JsonProperty("lat")
			private Double lat;

			/**
			 * Longitude
			 */
			@JsonProperty("lng")
			private Double lng;

			public Double getLat() {
				return lat;
			}

			public void setLat(Double lat) {
				this.lat = lat;
			}

			public Double getLng() {
				return lng;
			}

			public void setLng(Double lng) {
				this.lng = lng;
			}

		}

		@JsonInclude(JsonInclude.Include.NON_NULL)
		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class AdInfo {

			/**
			 * Country
			 */
			@JsonProperty("nation")
			private String nation;

			/**
			 * Country code (ISO3166 standard 3-digit code)
			 */
			@JsonProperty("nation_code")
			private Integer nationCode;

			/**
			 * Province
			 */
			@JsonProperty("province")
			private String province;

			/**
			 * City
			 */
			@JsonProperty("city")
			private String city;

			/**
			 * District
			 */
			@JsonProperty("district")
			private String district;

			/**
			 * Administrative division code
			 */
			@JsonProperty("adcode")
			private Integer adcode;

			public String getNation() {
				return nation;
			}

			public void setNation(String nation) {
				this.nation = nation;
			}

			public Integer getNationCode() {
				return nationCode;
			}

			public void setNationCode(Integer nationCode) {
				this.nationCode = nationCode;
			}

			public String getProvince() {
				return province;
			}

			public void setProvince(String province) {
				this.province = province;
			}

			public String getCity() {
				return city;
			}

			public void setCity(String city) {
				this.city = city;
			}

			public String getDistrict() {
				return district;
			}

			public void setDistrict(String district) {
				this.district = district;
			}

			public Integer getAdcode() {
				return adcode;
			}

			public void setAdcode(Integer adcode) {
				this.adcode = adcode;
			}

		}

	}

}
