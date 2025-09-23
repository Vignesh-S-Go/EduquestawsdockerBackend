package com.vip;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = {"http://ec2-13-201-34-207.ap-south-1.compute.amazonaws.com" })
@RequestMapping("/api")
public class Controller {

    @Autowired
    private Service userService;
    
    @Autowired
    private ExamService examService;
    
    @Autowired
    private ExamRegistrationService examRegistrationService;
    
    @Autowired
    private PaymentService paymentService;

    @Autowired
    private JwtUtil jwtUtil; 
    
    // ==================== USER ENDPOINTS ====================
    
    @PostMapping("/users/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        try {
            User registeredUser = userService.registerUser(user);
            return ResponseEntity.ok(registeredUser);
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }
    
    @PostMapping("/users/login")
    public ResponseEntity<?> loginUser(@RequestBody User user) {
        try {
            User loggedInUser = userService.loginUser(user.getEmail(), user.getPassword());
            if (loggedInUser != null) {
                String token = jwtUtil.generateToken(loggedInUser.getEmail());
                return ResponseEntity.ok(new LoginResponse("Login Successful", token));
            } else {
                return ResponseEntity.status(401).body("Invalid email or password");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/users/profile")
    public ResponseEntity<?> getUserProfile(@RequestHeader("Authorization") String authHeader) {
        try {
            User user = getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body("Invalid or expired token");
            }
            
            UserProfileResponse userProfile = new UserProfileResponse(
                user.getId(), user.getName(), user.getEmail(), user.getCreatedAt(), user.getUpdatedAt()
            );
            return ResponseEntity.ok(userProfile);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
 // 2. Update the createPayment endpoint to use the new field
    @PostMapping("/payments/create")
    public ResponseEntity<?> createPayment(
        @RequestBody PaymentRequest paymentRequest, 
        @RequestHeader("Authorization") String authHeader) {
        
        try {
            User user = getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body("Invalid or expired token");
            }
            
            Payment payment = paymentService.createPayment(
                user, 
                paymentRequest.getExamId(), 
                paymentRequest.getAmount(),
                paymentRequest.getPaymentMethod() // <-- PASS THE NEW FIELD HERE
            );
            
            if (payment != null) {
                paymentService.updatePaymentStatus(payment.getId(), "Paid");
                return ResponseEntity.ok(payment);
            } else {
                return ResponseEntity.status(400).body("Failed to create payment");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
    
    // ==================== DASHBOARD ENDPOINTS ====================
    
    @GetMapping("/dashboard/stats")
    public ResponseEntity<?> getDashboardStats(@RequestHeader("Authorization") String authHeader) {
        try {
            User user = getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body("Invalid or expired token");
            }
            
            Long totalExams = examService.getTotalExamsCount();
            Long totalStudents = examRegistrationService.getTotalStudentsCount();
            
            DashboardStats stats = new DashboardStats(totalExams, totalStudents);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
    
    // ==================== EXAM ENDPOINTS ====================
    
    @GetMapping("/exams/available")
    public ResponseEntity<?> getAvailableExams(@RequestHeader("Authorization") String authHeader) {
        try {
            User user = getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body("Invalid or expired token");
            }
            
            List<Exam> exams = examService.getAvailableExams();
            return ResponseEntity.ok(exams);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
    

    @GetMapping("/exams/registered")
    public ResponseEntity<?> getUserRegisteredExams(@RequestHeader("Authorization") String authHeader) {
        try {
            User user = getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body("Invalid or expired token");
            }
            
            List<ExamRegistration> registrations = examRegistrationService.getUserRegistrations(user);
            
            // --- THIS IS THE FIX ---
            // Pass the score from the registration object to the response object
            List<UserExamResponse> userExams = registrations.stream().map(reg -> 
                new UserExamResponse(
                    reg.getExam().getId(),
                    reg.getExam().getExamName(),
                    reg.getExam().getSubject(),
                    reg.getExam().getExamDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    reg.getExam().getDuration(),
                    reg.getStatus(),
                    reg.getId(),
                    reg.getScore() // <-- PASS THE SCORE HERE
                )
            ).collect(Collectors.toList());
            
            return ResponseEntity.ok(userExams);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
    
    @PostMapping("/exams/{examId}/register")
    public ResponseEntity<?> registerForExam(@PathVariable Long examId, @RequestHeader("Authorization") String authHeader) {
        try {
            User user = getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body("Invalid or expired token");
            }
            
            ExamRegistration registration = examRegistrationService.registerUserForExam(user, examId);
            if (registration != null) {
                return ResponseEntity.ok(new SuccessResponse("Successfully registered for exam"));
            } else {
                return ResponseEntity.status(400).body("Failed to register for exam");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
    
    @PostMapping("/exams/start/{registrationId}")
    public ResponseEntity<?> startExam(@PathVariable Long registrationId, @RequestHeader("Authorization") String authHeader) {
        try {
            User user = getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body("Invalid or expired token");
            }
            
            ExamRegistration registration = examRegistrationService.startExam(registrationId);
            if (registration != null) {
                return ResponseEntity.ok(new SuccessResponse("Exam started successfully"));
            } else {
                return ResponseEntity.status(400).body("Failed to start exam");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
    
    // ==================== PAYMENT ENDPOINTS ====================
    
 // In your Controller.java file

    @GetMapping("/payments")
    public ResponseEntity<?> getUserPayments(@RequestHeader("Authorization") String authHeader) {
        try {
            User user = getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body("Invalid or expired token");
            }
            
            List<Payment> payments = paymentService.getUserPayments(user);
            List<PaymentResponse> paymentResponses = payments.stream().map(payment -> 
                new PaymentResponse(
                    payment.getUser().getName(), // Get the name from the User object
                    payment.getAmount(),
                    payment.getPaymentDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), // Format the date
                    payment.getStatus()
                )
            ).collect(Collectors.toList());
            
            return ResponseEntity.ok(paymentResponses); // Return the transformed list

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
    
    @GetMapping("/payments/stats")
    public ResponseEntity<?> getPaymentStats(@RequestHeader("Authorization") String authHeader) {
        try {
            User user = getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body("Invalid or expired token");
            }
            
            Double totalRevenue = paymentService.getTotalRevenue();
            Long paidInvoices = paymentService.getPaidInvoicesCount();
            Long pendingInvoices = paymentService.getPendingInvoicesCount();
            
            PaymentStatsResponse stats = new PaymentStatsResponse(totalRevenue, paidInvoices, pendingInvoices);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
    
    @GetMapping("/payments/all")
    public ResponseEntity<?> getAllPayments(@RequestHeader("Authorization") String authHeader) {
        try {
            User user = getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body("Invalid or expired token");
            }
            
            List<Payment> payments = paymentService.getAllPayments();
            List<PaymentResponse> paymentResponses = payments.stream().map(payment -> 
                new PaymentResponse(
                    payment.getUser().getName(),
                    payment.getAmount(),
                    payment.getPaymentDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    payment.getStatus()
                )
            ).collect(Collectors.toList());
            
            return ResponseEntity.ok(paymentResponses);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
    
    // ==================== REPORTS ENDPOINTS ====================
    
    @GetMapping("/reports/monthly-stats")
    public ResponseEntity<?> getMonthlyStats(@RequestHeader("Authorization") String authHeader) {
        try {
            User user = getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body("Invalid or expired token");
            }
            
            List<Object[]> monthlyRegistrations = examRegistrationService.getMonthlyRegistrationStats(2024);
            
            // Convert to proper format for frontend charts
            List<MonthlyStatsResponse> stats = new ArrayList<>();
            String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
            
            for (int i = 1; i <= 12; i++) {
                String month = months[i-1];
                Long count = 0L;
                
                for (Object[] row : monthlyRegistrations) {
                    if (row[0].equals(i)) {
                        count = ((Number) row[1]).longValue();
                        break;
                    }
                }
                
                stats.add(new MonthlyStatsResponse(month, count));
            }
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
    
    // ==================== HELPER METHODS ====================
    
    private User getUserFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        
        String token = authHeader.substring(7);
        String email = jwtUtil.extractUsername(token);
        
        if (email == null || jwtUtil.isTokenExpired(token)) {
            return null;
        }
        
        return userService.getUserByEmail(email);
    }
 // In Controller.java

 // Find your existing completeExam endpoint and update it like this:
	 @PostMapping("/exams/complete/{registrationId}")
	 public ResponseEntity<?> completeExam(
	     @PathVariable Long registrationId, 
	     @RequestBody Map<String, Double> payload,
	     @RequestHeader("Authorization") String authHeader) {
	     
	     User user = getUserFromToken(authHeader);
	     if (user == null) {
	         return ResponseEntity.status(401).body("Invalid token");
	     }
	
	     Double score = payload.get("score");
	     // Pass the user object to the service method
	     ExamRegistration registration = examRegistrationService.completeExam(registrationId, score, user);
	     
	     if (registration != null) {
	         return ResponseEntity.ok(new SuccessResponse("Exam completed successfully"));
	     } else {
	         // This will now correctly return an error if the user doesn't own the registration
	         return ResponseEntity.status(400).body("Failed to complete exam. Registration not found for this user.");
	     }
	 }
    // ==================== RESPONSE CLASSES ====================
    
    public static class LoginResponse {
        private String message;
        private String token;
        
        public LoginResponse(String message, String token) {
            this.message = message;
            this.token = token;
        }
        
        public String getMessage() { return message; }
        public String getToken() { return token; }
    }

    public static class UserProfileResponse {
        private Long id;
        private String name;
        private String email;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public UserProfileResponse(Long id, String name, String email, 
                                 LocalDateTime createdAt, LocalDateTime updatedAt) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
    }

    public static class DashboardStats {
        private Long totalExams;
        private Long totalStudents;

        public DashboardStats(Long totalExams, Long totalStudents) {
            this.totalExams = totalExams;
            this.totalStudents = totalStudents;
        }

        public Long getTotalExams() { return totalExams; }
        public Long getTotalStudents() { return totalStudents; }
    }

 // In Controller.java

    public static class UserExamResponse {
        private Long examId;
        private String examName;
        private String subject;
        private String date;
        private String duration;
        private String status;
        private Long registrationId;
        private Double score; // <-- ADD THIS FIELD

        public UserExamResponse(Long examId, String examName, String subject, String date, 
                              String duration, String status, Long registrationId, Double score) { // <-- ADD score
            this.examId = examId;
            this.examName = examName;
            this.subject = subject;
            this.date = date;
            this.duration = duration;
            this.status = status;
            this.registrationId = registrationId;
            this.score = score; // <-- ADD THIS LINE
        }

        public Long getExamId() { return examId; }
        public String getExamName() { return examName; }
        public String getSubject() { return subject; }
        public String getDate() { return date; }
        public String getDuration() { return duration; }
        public String getStatus() { return status; }
        public Long getRegistrationId() { return registrationId; }
        public Double getScore() { return score; } // <-- ADD THIS GETTER
    }

    public static class PaymentStatsResponse {
        private Double totalRevenue;
        private Long paidInvoices;
        private Long pendingInvoices;

        public PaymentStatsResponse(Double totalRevenue, Long paidInvoices, Long pendingInvoices) {
            this.totalRevenue = totalRevenue;
            this.paidInvoices = paidInvoices;
            this.pendingInvoices = pendingInvoices;
        }

        public Double getTotalRevenue() { return totalRevenue; }
        public Long getPaidInvoices() { return paidInvoices; }
        public Long getPendingInvoices() { return pendingInvoices; }
    }

    public static class PaymentResponse {
        private String studentName;
        private Double amount;
        private String date;
        private String status;

        public PaymentResponse(String studentName, Double amount, String date, String status) {
            this.studentName = studentName;
            this.amount = amount;
            this.date = date;
            this.status = status;
        }

        public String getStudentName() { return studentName; }
        public Double getAmount() { return amount; }
        public String getDate() { return date; }
        public String getStatus() { return status; }
    }

    public static class MonthlyStatsResponse {
        private String month;
        private Long count;

        public MonthlyStatsResponse(String month, Long count) {
            this.month = month;
            this.count = count;
        }

        public String getMonth() { return month; }
        public Long getCount() { return count; }
    }

    public static class SuccessResponse {
        private String message;

        public SuccessResponse(String message) {
            this.message = message;
        }

        public String getMessage() { return message; }
    }
 // Inside Controller.java
    public static class PaymentRequest {
        private Long examId;
        private Double amount;
        private String paymentMethod; // <-- ADD THIS LINE

        // Getters and Setters
        public Long getExamId() { return examId; }
        public void setExamId(Long examId) { this.examId = examId; }
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        public String getPaymentMethod() { return paymentMethod; } // <-- ADD THIS GETTER
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; } // <-- ADD THIS SETTER
    }
}
