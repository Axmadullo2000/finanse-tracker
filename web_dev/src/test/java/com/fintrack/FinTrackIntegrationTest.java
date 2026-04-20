package com.fintrack;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.dto.*;
import com.fintrack.entity.Role;
import com.fintrack.entity.User;
import com.fintrack.repository.UserRepository;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FinTrackIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        if (userRepository.findByUsername("testuser").isEmpty()) {
            User user = new User();
            user.setUsername("testuser");
            user.setPassword(passwordEncoder.encode("password123"));
            user.setEmail("test@fintrack.com");
            user.setRole(Role.USER);
            userRepository.save(user);
        }

        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEmail("admin@fintrack.com");
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
        }

        if (userRepository.findByUsername("otheruser").isEmpty()) {
            User other = new User();
            other.setUsername("otheruser");
            other.setPassword(passwordEncoder.encode("other123"));
            other.setEmail("other@fintrack.com");
            other.setRole(Role.USER);
            userRepository.save(other);
        }
    }

    // ==================== AUTH TESTS ====================

    @Test
    @DisplayName("Register — should return 201 for valid request")
    void registerShouldSucceed() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("newpass123");
        request.setEmail("new@fintrack.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Register — should return 400 for duplicate username")
    void registerShouldFailForDuplicateUsername() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setEmail("another@fintrack.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Register — should return 400 for invalid email")
    void registerShouldFailForInvalidEmail() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("validuser");
        request.setPassword("password123");
        request.setEmail("not-an-email");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Protected endpoint — should return 401 when not authenticated")
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("Auth /me — should return current username")
    void meShouldReturnUsername() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(content().string("Logged in as: testuser"));
    }

    // ==================== CATEGORY TESTS ====================

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("Create category — should return 201")
    void createCategoryShouldSucceed() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setName("Food");

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Food"))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("Create category — should return 400 for blank name")
    void createCategoryShouldFailForBlankName() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setName("");

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("List categories — should return user's categories")
    void listCategoriesShouldReturnUserCategories() throws Exception {
        createCategory("Food");
        createCategory("Transport");

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("Update category — should return updated category")
    void updateCategoryShouldSucceed() throws Exception {
        Long id = createCategoryAndGetId("Food");

        CategoryRequest request = new CategoryRequest();
        request.setName("Groceries");

        mockMvc.perform(put("/api/categories/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Groceries"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("Delete category — should return 204")
    void deleteCategoryShouldSucceed() throws Exception {
        Long id = createCategoryAndGetId("ToDelete");

        mockMvc.perform(delete("/api/categories/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/categories/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "otheruser")
    @DisplayName("Category isolation — user cannot see another user's categories")
    void categoryShouldBeIsolatedPerUser() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ==================== EXPENSE TESTS ====================

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("Create expense — should return 201")
    void createExpenseShouldSucceed() throws Exception {
        Long categoryId = createCategoryAndGetId("Food");

        ExpenseRequest request = new ExpenseRequest();
        request.setAmount(new BigDecimal("25.50"));
        request.setDescription("Lunch");
        request.setDate(java.time.LocalDate.of(2026, 4, 19));
        request.setCategoryId(categoryId);

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(25.50))
                .andExpect(jsonPath("$.description").value("Lunch"))
                .andExpect(jsonPath("$.categoryName").value("Food"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("Create expense — should return 404 for invalid category")
    void createExpenseShouldFailForInvalidCategory() throws Exception {
        ExpenseRequest request = new ExpenseRequest();
        request.setAmount(new BigDecimal("10.00"));
        request.setDescription("Test");
        request.setDate(java.time.LocalDate.of(2026, 4, 19));
        request.setCategoryId(9999L);

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("List expenses — should return paginated results")
    void listExpensesShouldReturnPaginated() throws Exception {
        Long categoryId = createCategoryAndGetId("Food");
        createExpense("25.50", "Lunch", "2026-04-19", categoryId);
        createExpense("15.00", "Coffee", "2026-04-19", categoryId);

        mockMvc.perform(get("/api/expenses")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("Filter expenses — should filter by category")
    void filterExpensesByCategoryShouldWork() throws Exception {
        Long foodId = createCategoryAndGetId("Food");
        Long transportId = createCategoryAndGetId("Transport");

        createExpense("25.50", "Lunch", "2026-04-19", foodId);
        createExpense("15.00", "Bus", "2026-04-19", transportId);

        mockMvc.perform(get("/api/expenses")
                        .param("categoryId", foodId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].categoryName").value("Food"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("Filter expenses — should filter by date range")
    void filterExpensesByDateRangeShouldWork() throws Exception {
        Long categoryId = createCategoryAndGetId("Food");
        createExpense("25.50", "Monday lunch", "2026-04-13", categoryId);
        createExpense("15.00", "Saturday lunch", "2026-04-18", categoryId);
        createExpense("30.00", "Sunday dinner", "2026-04-19", categoryId);

        mockMvc.perform(get("/api/expenses")
                        .param("from", "2026-04-18")
                        .param("to", "2026-04-19"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    @WithMockUser(username = "otheruser")
    @DisplayName("Expense isolation — user cannot see another user's expenses")
    void expenseShouldBeIsolatedPerUser() throws Exception {
        mockMvc.perform(get("/api/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    // ==================== BUDGET TESTS ====================

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("Create budget — should return 201")
    void createBudgetShouldSucceed() throws Exception {
        Long categoryId = createCategoryAndGetId("Food");

        BudgetRequest request = new BudgetRequest();
        request.setMonthlyLimit(new BigDecimal(300));
        request.setMonth(4);
        request.setYear(2026);
        request.setCategoryId(categoryId);

        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.monthlyLimit").value(300))
                .andExpect(jsonPath("$.categoryName").value("Food"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("Create budget — should return 400 for duplicate")
    void createDuplicateBudgetShouldFail() throws Exception {
        Long categoryId = createCategoryAndGetId("Food");
        createBudget(300, 4, 2026, categoryId);

        BudgetRequest request = new BudgetRequest();
        request.setMonthlyLimit(new BigDecimal(500));
        request.setMonth(4);
        request.setYear(2026);
        request.setCategoryId(categoryId);

        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== SUMMARY TESTS ====================

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("Monthly summary — should show spending vs budget")
    void monthlySummaryShouldWork() throws Exception {
        Long categoryId = createCategoryAndGetId("Food");
        createBudget(300, 4, 2026, categoryId);
        createExpense("25.50", "Lunch", "2026-04-19", categoryId);
        createExpense("15.00", "Coffee", "2026-04-18", categoryId);

        mockMvc.perform(get("/api/summary/monthly")
                        .param("month", "4")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoryName").value("Food"))
                .andExpect(jsonPath("$[0].spent").value(40.50))
                .andExpect(jsonPath("$[0].budgetLimit").value(300))
                .andExpect(jsonPath("$[0].remaining").value(259.50));
    }

    // ==================== ADMIN TESTS ====================

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Admin endpoint — should return 403 for regular user")
    void adminEndpointShouldReturn403ForRegularUser() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("Admin endpoint — should return 200 for admin")
    void adminEndpointShouldReturn200ForAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").isNumber());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("Admin stats — should return user count")
    void adminStatsShouldReturnUserCount() throws Exception {
        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").isNumber());
    }

    // ==================== HELPER METHODS ====================

    private void createCategory(String name) throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setName(name);

        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private Long createCategoryAndGetId(String name) throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setName(name);

        MvcResult result = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        CategoryResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CategoryResponse.class);
        return response.getId();
    }

    private void createExpense(String amount, String description,
                               String date, Long categoryId) throws Exception {
        String json = """
                {
                    "amount": %s,
                    "description": "%s",
                    "date": "%s",
                    "categoryId": %d
                }
                """.formatted(amount, description, date, categoryId);

        mockMvc.perform(post("/api/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private void createBudget(double limit, int month, int year,
                              Long categoryId) throws Exception {
        String json = """
                {
                    "monthlyLimit": %s,
                    "month": %d,
                    "year": %d,
                    "categoryId": %d
                }
                """.formatted(limit, month, year, categoryId);

        mockMvc.perform(post("/api/budgets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }
}
