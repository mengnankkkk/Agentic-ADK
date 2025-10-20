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

package com.alibaba.langengine.baidumap.sdk.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class IpLocationRequest {

	/**
	 * User's IP address. If not provided or empty, the system will locate based on the
	 * requesting IP. To obtain location information via IPv6, please submit a ticket for
	 * application.
	 */
	@JsonProperty("ip")
	private String ip;

	/**
	 * Required when the AK verification method is SN verification. Not required for other
	 * AK verification methods.
	 */
	@JsonProperty("sn")
	private String sn;

	/**
	 * Sets the coordinate type for returned location information: - If coor is not
	 * provided or empty: Baidu Mercator coordinates (Baidu meter coordinates) - coor =
	 * bd09ll: Baidu latitude and longitude coordinates (encrypted from GCJ-02) - coor =
	 * gcj02: GCJ-02 coordinates (encrypted from original GPS coordinates) Note: Baidu
	 * Maps uses bd09ll coordinates. Please select the appropriate coordinate type
	 * accordingly.
	 */
	@JsonProperty("coor")
	private String coor;

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getSn() {
		return sn;
	}

	public void setSn(String sn) {
		this.sn = sn;
	}

	public String getCoor() {
		return coor;
	}

	public void setCoor(String coor) {
		this.coor = coor;
	}

}
