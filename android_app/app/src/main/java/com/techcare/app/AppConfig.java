package com.techcare.app;

public class AppConfig {
    // 10.0.2.2:5000 is the standard emulator address mapping to your PC's localhost:5000.
    // If you run the app on a physical Android phone, change this to your computer's local IP (e.g. "http://192.168.1.50:5000").
    public static final String BASE_URL = "http://10.0.2.2:5000";
    
    public static final String API_LOGIN = BASE_URL + "/api/login";
    public static final String API_REGISTER = BASE_URL + "/api/user_register";
    public static final String API_COMPANIES = BASE_URL + "/api/view_companies";
    public static final String API_PRODUCTS = BASE_URL + "/api/view_products";
    public static final String API_SEND_COMPLAINT = BASE_URL + "/api/send_complaint";
    public static final String API_VIEW_COMPLAINTS = BASE_URL + "/api/view_complaints";
    public static final String API_APPROVE_ESTIMATE = BASE_URL + "/api/approve_estimate";
    public static final String API_GIVE_RATING = BASE_URL + "/api/give_rating";
    public static final String API_SEND_FEEDBACK = BASE_URL + "/api/send_feedback";
    
    // Tech APIs
    public static final String API_TECH_ASSIGNED = BASE_URL + "/api/technician/view_assigned";
    public static final String API_TECH_UPDATE_STATUS = BASE_URL + "/api/technician/update_status";
    public static final String API_TECH_FEEDBACK = BASE_URL + "/api/technician/view_feedback";
    public static final String API_REQUEST_PART = BASE_URL + "/api/technician/request_part";
    
    // Chat APIs
    public static final String API_CHAT_SEND = BASE_URL + "/api/chat/send";
    public static final String API_CHAT_GET = BASE_URL + "/api/chat/get";
    public static final String API_CHAT_BLOCK = BASE_URL + "/api/chat/block";

    // Bidding & Matching APIs
    public static final String API_TECH_AVAILABLE_JOBS = BASE_URL + "/api/technician/available_jobs";
    public static final String API_TECH_OFFER_JOB = BASE_URL + "/api/technician/offer_job";
    public static final String API_USER_COMPLAINT_OFFERS = BASE_URL + "/api/user/complaint_offers";
    public static final String API_USER_ACCEPT_OFFER = BASE_URL + "/api/user/accept_offer";
    public static final String API_TECH_CONFIRM_JOB = BASE_URL + "/api/technician/confirm_job";
    public static final String API_TECH_CANCEL_JOB = BASE_URL + "/api/technician/cancel_job";
}
