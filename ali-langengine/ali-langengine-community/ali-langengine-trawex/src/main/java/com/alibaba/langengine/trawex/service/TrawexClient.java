package com.alibaba.langengine.trawex.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.langengine.trawex.TrawexConfiguration;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Trawex API 客户端
 * 负责与 Trawex API 的所有通信
 * 
 * @author AIDC-AI
 */
@Slf4j
public class TrawexClient {
    
    private final OkHttpClient httpClient;
    private final String apiKey;
    private final String apiSecret;
    private final String baseUrl;
    private final int maxRetries;
    private final int retryInterval;
    
    public TrawexClient() {
        this(TrawexConfiguration.TRAWEX_API_KEY,
             TrawexConfiguration.TRAWEX_API_SECRET,
             TrawexConfiguration.TRAWEX_API_BASE_URL);
    }
    
    public TrawexClient(String apiKey, String apiSecret, String baseUrl) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.baseUrl = baseUrl;
        this.maxRetries = TrawexConfiguration.TRAWEX_MAX_RETRIES;
        this.retryInterval = TrawexConfiguration.TRAWEX_RETRY_INTERVAL;
        
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(TrawexConfiguration.TRAWEX_REQUEST_TIMEOUT))
            .readTimeout(Duration.ofSeconds(TrawexConfiguration.TRAWEX_REQUEST_TIMEOUT))
            .writeTimeout(Duration.ofSeconds(TrawexConfiguration.TRAWEX_REQUEST_TIMEOUT))
            .build();
    }
    
    /**
     * 搜索酒店
     */
    public String searchHotels(String destination, String checkIn, String checkOut, 
                              Integer adults, Integer rooms, Integer starRating, Double maxPrice) {
        JSONObject params = new JSONObject();
        params.put("destination", destination);
        params.put("check_in", checkIn);
        params.put("check_out", checkOut);
        params.put("adults", adults != null ? adults : 2);
        params.put("rooms", rooms != null ? rooms : 1);
        if (starRating != null) {
            params.put("star_rating", starRating);
        }
        if (maxPrice != null) {
            params.put("max_price", maxPrice);
        }
        params.put("currency", TrawexConfiguration.TRAWEX_DEFAULT_CURRENCY);
        params.put("language", TrawexConfiguration.TRAWEX_DEFAULT_LANGUAGE);
        
        return post("/hotels/search", params);
    }
    
    /**
     * 获取酒店详情
     */
    public String getHotelDetails(String hotelId, String checkIn, String checkOut, Integer adults, Integer rooms) {
        JSONObject params = new JSONObject();
        params.put("hotel_id", hotelId);
        params.put("check_in", checkIn);
        params.put("check_out", checkOut);
        params.put("adults", adults != null ? adults : 2);
        params.put("rooms", rooms != null ? rooms : 1);
        params.put("currency", TrawexConfiguration.TRAWEX_DEFAULT_CURRENCY);
        params.put("language", TrawexConfiguration.TRAWEX_DEFAULT_LANGUAGE);
        
        return post("/hotels/details", params);
    }
    
    /**
     * 预订酒店
     */
    public String bookHotel(String hotelId, String checkIn, String checkOut, 
                           Map<String, Object> guestInfo, Map<String, Object> paymentInfo) {
        JSONObject params = new JSONObject();
        params.put("hotel_id", hotelId);
        params.put("check_in", checkIn);
        params.put("check_out", checkOut);
        params.put("guest_info", guestInfo);
        params.put("payment_info", paymentInfo);
        params.put("currency", TrawexConfiguration.TRAWEX_DEFAULT_CURRENCY);
        
        return post("/hotels/book", params);
    }
    
    /**
     * 搜索航班
     */
    public String searchFlights(String origin, String destination, String departureDate, 
                               String returnDate, Integer adults, String cabinClass) {
        JSONObject params = new JSONObject();
        params.put("origin", origin);
        params.put("destination", destination);
        params.put("departure_date", departureDate);
        if (returnDate != null && !returnDate.isEmpty()) {
            params.put("return_date", returnDate);
        }
        params.put("adults", adults != null ? adults : 1);
        params.put("cabin_class", cabinClass != null ? cabinClass : "economy");
        params.put("currency", TrawexConfiguration.TRAWEX_DEFAULT_CURRENCY);
        params.put("language", TrawexConfiguration.TRAWEX_DEFAULT_LANGUAGE);
        
        return post("/flights/search", params);
    }
    
    /**
     * 获取航班详情
     */
    public String getFlightDetails(String flightId) {
        JSONObject params = new JSONObject();
        params.put("flight_id", flightId);
        params.put("currency", TrawexConfiguration.TRAWEX_DEFAULT_CURRENCY);
        params.put("language", TrawexConfiguration.TRAWEX_DEFAULT_LANGUAGE);
        
        return post("/flights/details", params);
    }
    
    /**
     * 预订航班
     */
    public String bookFlight(String flightId, Map<String, Object> passengerInfo, 
                            Map<String, Object> paymentInfo) {
        JSONObject params = new JSONObject();
        params.put("flight_id", flightId);
        params.put("passenger_info", passengerInfo);
        params.put("payment_info", paymentInfo);
        params.put("currency", TrawexConfiguration.TRAWEX_DEFAULT_CURRENCY);
        
        return post("/flights/book", params);
    }
    
    /**
     * 搜索旅游套餐
     */
    public String searchPackages(String destination, String departureDate, String returnDate, 
                                Integer adults, String packageType) {
        JSONObject params = new JSONObject();
        params.put("destination", destination);
        params.put("departure_date", departureDate);
        params.put("return_date", returnDate);
        params.put("adults", adults != null ? adults : 2);
        if (packageType != null) {
            params.put("package_type", packageType);
        }
        params.put("currency", TrawexConfiguration.TRAWEX_DEFAULT_CURRENCY);
        params.put("language", TrawexConfiguration.TRAWEX_DEFAULT_LANGUAGE);
        
        return post("/packages/search", params);
    }
    
    /**
     * 获取套餐详情
     */
    public String getPackageDetails(String packageId) {
        JSONObject params = new JSONObject();
        params.put("package_id", packageId);
        params.put("currency", TrawexConfiguration.TRAWEX_DEFAULT_CURRENCY);
        params.put("language", TrawexConfiguration.TRAWEX_DEFAULT_LANGUAGE);
        
        return post("/packages/details", params);
    }
    
    /**
     * 搜索活动
     */
    public String searchActivities(String destination, String date, String category) {
        JSONObject params = new JSONObject();
        params.put("destination", destination);
        if (date != null) {
            params.put("date", date);
        }
        if (category != null) {
            params.put("category", category);
        }
        params.put("currency", TrawexConfiguration.TRAWEX_DEFAULT_CURRENCY);
        params.put("language", TrawexConfiguration.TRAWEX_DEFAULT_LANGUAGE);
        
        return post("/activities/search", params);
    }
    
    /**
     * 预订活动
     */
    public String bookActivity(String activityId, String date, Integer participants, 
                              Map<String, Object> participantInfo, Map<String, Object> paymentInfo) {
        JSONObject params = new JSONObject();
        params.put("activity_id", activityId);
        params.put("date", date);
        params.put("participants", participants);
        params.put("participant_info", participantInfo);
        params.put("payment_info", paymentInfo);
        params.put("currency", TrawexConfiguration.TRAWEX_DEFAULT_CURRENCY);
        
        return post("/activities/book", params);
    }
    
    /**
     * 搜索租车
     */
    public String searchCarRentals(String pickupLocation, String pickupDate, String dropoffDate, 
                                  String dropoffLocation, String carType) {
        JSONObject params = new JSONObject();
        params.put("pickup_location", pickupLocation);
        params.put("pickup_date", pickupDate);
        params.put("dropoff_date", dropoffDate);
        if (dropoffLocation != null) {
            params.put("dropoff_location", dropoffLocation);
        }
        if (carType != null) {
            params.put("car_type", carType);
        }
        params.put("currency", TrawexConfiguration.TRAWEX_DEFAULT_CURRENCY);
        params.put("language", TrawexConfiguration.TRAWEX_DEFAULT_LANGUAGE);
        
        return post("/cars/search", params);
    }
    
    /**
     * 预订租车
     */
    public String bookCarRental(String carId, Map<String, Object> driverInfo, 
                               Map<String, Object> paymentInfo) {
        JSONObject params = new JSONObject();
        params.put("car_id", carId);
        params.put("driver_info", driverInfo);
        params.put("payment_info", paymentInfo);
        params.put("currency", TrawexConfiguration.TRAWEX_DEFAULT_CURRENCY);
        
        return post("/cars/book", params);
    }
    
    /**
     * 发送 POST 请求
     */
    private String post(String endpoint, JSONObject params) {
        int attempt = 0;
        Exception lastException = null;
        
        while (attempt < maxRetries) {
            try {
                String url = baseUrl + endpoint;
                String timestamp = String.valueOf(System.currentTimeMillis());
                String signature = generateSignature(endpoint, timestamp, params.toJSONString());
                
                RequestBody body = RequestBody.create(
                    params.toJSONString(),
                    MediaType.parse("application/json; charset=utf-8")
                );
                
                Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-Api-Key", apiKey)
                    .addHeader("X-Timestamp", timestamp)
                    .addHeader("X-Signature", signature)
                    .addHeader("User-Agent", TrawexConfiguration.TRAWEX_USER_AGENT)
                    .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    
                    if (!response.isSuccessful()) {
                        JSONObject error = JSON.parseObject(responseBody);
                        String errorCode = error.getString("error_code");
                        String errorMessage = error.getString("message");
                        throw new TrawexException(response.code(), errorCode, 
                            errorMessage != null ? errorMessage : "API request failed");
                    }
                    
                    return responseBody;
                }
                
            } catch (IOException e) {
                lastException = e;
                attempt++;
                log.warn("Trawex API request failed (attempt {}/{}): {}", 
                    attempt, maxRetries, e.getMessage());
                
                if (attempt < maxRetries) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(retryInterval);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new TrawexException("Request interrupted", ie);
                    }
                }
            } catch (TrawexException e) {
                // 不重试业务异常
                throw e;
            }
        }
        
        throw new TrawexException("Failed after " + maxRetries + " attempts", lastException);
    }
    
    /**
     * 生成请求签名
     */
    private String generateSignature(String endpoint, String timestamp, String body) {
        try {
            String data = endpoint + timestamp + body + apiSecret;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new TrawexException("Failed to generate signature", e);
        }
    }
}
